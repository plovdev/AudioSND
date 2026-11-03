#include <jni.h>
#include <AudioToolbox/AudioToolbox.h>

#include <atomic>
#include <vector>
#include <mutex>
#include <condition_variable>
#include <cstring>
#include <unordered_map>
#include <memory>

#include "../h/org_plovdev_audioengine_devices_NativeInputAudioDevice.h"
// ================= GLOBALS =================

struct AudioDeviceContext {
    AudioUnit audioUnit{};
    AudioStreamBasicDescription format{};

    std::atomic<bool> isRunning{false};
    bool isInitialized{false};

    std::vector<uint8_t> ring;
    size_t ringSize{0};

    std::atomic<size_t> writePos{0};
    std::atomic<size_t> readPos{0};

    std::mutex mtx;
    std::condition_variable cv;

    std::string deviceId;
};

std::unordered_map<long, std::unique_ptr<AudioDeviceContext>> ctxs;
std::mutex ctxMutex;
long nextId = 1;

AudioDeviceContext* getCtx(long h) {
    std::lock_guard<std::mutex> l(ctxMutex);
    auto it = ctxs.find(h);
    return it == ctxs.end() ? nullptr : it->second.get();
}

// ================= FORMAT =================

static AudioStreamBasicDescription javaToASBD(JNIEnv* env, jobject fmt) {
    AudioStreamBasicDescription asbd{};
    jclass cls = env->GetObjectClass(fmt);

    auto getInt = [&](const char* name) {
        return env->GetIntField(fmt, env->GetFieldID(cls, name, "I"));
    };

    int sr = getInt("sampleRate");
    int ch = getInt("channels");

    asbd.mSampleRate = sr;
    asbd.mFormatID = kAudioFormatLinearPCM;
    asbd.mFormatFlags |= kAudioFormatFlagIsFloat;
    asbd.mChannelsPerFrame = ch;
    asbd.mBitsPerChannel = 32;
    asbd.mFramesPerPacket = 1;
    asbd.mFormatFlags = static_cast<UInt32>(kAudioFormatFlagIsPacked) |
        static_cast<UInt32>(kAudioFormatFlagsNativeEndian) |
        static_cast<UInt32>(kAudioFormatFlagIsFloat);

    asbd.mBytesPerFrame = 4 * ch;
    asbd.mBytesPerPacket = asbd.mBytesPerFrame;

    return asbd;
}

// ================= CALLBACK =================

static OSStatus audioCallback(
        void* ref,
        AudioUnitRenderActionFlags*,
        const AudioTimeStamp* ts,
        UInt32 bus,
        UInt32 frames,
        AudioBufferList*)
{
    auto* c = (AudioDeviceContext*)ref;

    if (!c->isRunning.load(std::memory_order_relaxed))
        return noErr;

    UInt32 bytes = frames * c->format.mBytesPerFrame;

    uint8_t temp[65536];

    AudioBufferList list{};
    list.mNumberBuffers = 1;
    list.mBuffers[0].mNumberChannels = c->format.mChannelsPerFrame;
    list.mBuffers[0].mDataByteSize = bytes;
    list.mBuffers[0].mData = temp;

    AudioUnitRender(c->audioUnit, nullptr, ts, bus, frames, &list);

    size_t write = c->writePos.load(std::memory_order_relaxed);
    size_t read  = c->readPos.load(std::memory_order_acquire);

    size_t free = c->ringSize - (write - read);

    if (free < bytes) {
        // drop old audio (НЕ блокируем realtime)
        c->readPos.store(read + (bytes - free), std::memory_order_release);
    }

    size_t idx = write % c->ringSize;
    size_t first = std::min((size_t)bytes, c->ringSize - idx);

    memcpy(&c->ring[idx], temp, first);
    memcpy(&c->ring[0], temp + first, bytes - first);

    c->writePos.store(write + bytes, std::memory_order_release);

    c->cv.notify_one();
    return noErr;
}

// ================= OPEN =================

extern "C"
JNIEXPORT jlong JNICALL
Java_org_plovdev_audioengine_devices_NativeInputAudioDevice__1open(
        JNIEnv* env, jobject,
        jobject format,
        jobject deviceInfo)
{
    auto ctx = std::make_unique<AudioDeviceContext>();

    ctx->format = javaToASBD(env, format);

    jclass cls = env->GetObjectClass(deviceInfo);
    jfieldID idField = env->GetFieldID(cls, "id", "Ljava/lang/String;");
    jstring idStr = (jstring)env->GetObjectField(deviceInfo, idField);

    const char* cstr = env->GetStringUTFChars(idStr, nullptr);
    ctx->deviceId = cstr;
    env->ReleaseStringUTFChars(idStr, cstr);

    AudioComponentDescription desc{};
    desc.componentType = kAudioUnitType_Output;
    desc.componentSubType = kAudioUnitSubType_HALOutput;
    desc.componentManufacturer = kAudioUnitManufacturer_Apple;

    AudioComponent comp = AudioComponentFindNext(nullptr, &desc);
    AudioComponentInstanceNew(comp, &ctx->audioUnit);

    UInt32 enable = 1;
    UInt32 disable = 0;

    AudioUnitSetProperty(ctx->audioUnit,
                          kAudioOutputUnitProperty_EnableIO,
                          kAudioUnitScope_Input,
                          1,
                          &enable,
                          sizeof(enable));

    AudioUnitSetProperty(ctx->audioUnit,
                          kAudioOutputUnitProperty_EnableIO,
                          kAudioUnitScope_Output,
                          0,
                          &disable,
                          sizeof(disable));

    AudioDeviceID dev =
        (AudioDeviceID)strtoul(ctx->deviceId.c_str(), nullptr, 10);

    AudioUnitSetProperty(ctx->audioUnit,
                          kAudioOutputUnitProperty_CurrentDevice,
                          kAudioUnitScope_Global,
                          0,
                          &dev,
                          sizeof(dev));

    AudioUnitSetProperty(ctx->audioUnit,
                          kAudioUnitProperty_StreamFormat,
                          kAudioUnitScope_Output,
                          1,
                          &ctx->format,
                          sizeof(ctx->format));


    AURenderCallbackStruct cb{};
    cb.inputProc = audioCallback;
    cb.inputProcRefCon = ctx.get();

    AudioUnitSetProperty(ctx->audioUnit,
                          kAudioOutputUnitProperty_SetInputCallback,
                          kAudioUnitScope_Global,
                          1,
                          &cb,
                          sizeof(cb));

    AudioUnitInitialize(ctx->audioUnit);

    ctx->ringSize =
        ctx->format.mSampleRate *
        ctx->format.mBytesPerFrame * 0.06;

    ctx->ring.resize(ctx->ringSize);

    ctx->isRunning = true;

    AudioOutputUnitStart(ctx->audioUnit);

    long id;
    {
        std::lock_guard<std::mutex> l(ctxMutex);
        id = nextId++;
        ctxs[id] = std::move(ctx);
    }

    return id;
}

// ================= READ =================

extern "C"
JNIEXPORT jint JNICALL
Java_org_plovdev_audioengine_devices_NativeInputAudioDevice__1read(
        JNIEnv* env, jobject,
        jobject buffer,
        jlong handle)
{
    auto* c = getCtx(handle);
    if (!c) return -1;

    void* dst = env->GetDirectBufferAddress(buffer);
    jlong cap = env->GetDirectBufferCapacity(buffer);

    if (!dst || cap <= 0)
        return -1;

    if (!c->isRunning)
    {
        AudioOutputUnitStart(c->audioUnit);
        c->isRunning = true;
    }

    uint8_t* out = (uint8_t*)dst;
    size_t copied = 0;

    while (copied < (size_t)cap)
    {
        std::unique_lock lk(c->mtx);

        c->cv.wait(lk, [&]{
            return (c->writePos.load() - c->readPos.load()) > 0;
        });

        size_t avail = c->writePos.load() - c->readPos.load();

        size_t r = c->readPos.load();
        size_t idx = r % c->ringSize;

        size_t take = std::min(avail, (size_t)cap - copied);
        size_t first = std::min(take, c->ringSize - idx);

        memcpy(out + copied, &c->ring[idx], first);
        memcpy(out + copied + first, &c->ring[0], take - first);

        c->readPos.store(r + take);
        copied += take;
    }

    return copied;
}

// ================= CLOSE =================

extern "C"
JNIEXPORT void JNICALL
Java_org_plovdev_audioengine_devices_NativeInputAudioDevice__1close(
        JNIEnv*, jobject, jlong handle)
{
    std::unique_ptr<AudioDeviceContext> ctx;

    {
        std::lock_guard<std::mutex> l(ctxMutex);
        auto it = ctxs.find(handle);
        if (it == ctxs.end()) return;

        ctx = std::move(it->second);
        ctxs.erase(it);
    }

    ctx->isRunning = false;

    AudioOutputUnitStop(ctx->audioUnit);
    AudioUnitUninitialize(ctx->audioUnit);
    AudioComponentInstanceDispose(ctx->audioUnit);

    ctx->cv.notify_all();
}