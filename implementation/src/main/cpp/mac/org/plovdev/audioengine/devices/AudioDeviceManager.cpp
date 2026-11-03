#include <iostream>
#include <jni.h>
#include <CoreAudio/CoreAudio.h>
#include <vector>
#include <string>
#include <algorithm>
#include <sstream>
#include <iomanip>

#include "../h/org_plovdev_audioengine_devices_AudioDeviceManager.h"

// =====================
// Utils
// =====================
std::string CFStringToStdString(CFStringRef cfStr) {
    if (!cfStr) return "";
    char buffer[512];
    if (CFStringGetCString(cfStr, buffer, sizeof(buffer), kCFStringEncodingUTF8)) {
        return buffer;
    }
    return "";
}

// =====================
// JNI cache
// =====================
JavaVM* gJvm = nullptr;
jclass clsAudioDeviceInfo = nullptr;
jmethodID ctorAudioDeviceInfo = nullptr;
jclass clsAudioDeviceType = nullptr;
jfieldID fidInputType = nullptr;
jfieldID fidOutputType = nullptr;
jfieldID fidDuplexType = nullptr;
jclass clsArrayList = nullptr;
jmethodID ctorArrayList = nullptr;
jmethodID arrayListAdd = nullptr;
jclass clsTrackFormat = nullptr;
jmethodID ctorTrackFormat = nullptr;

jclass clsAudioDeviceManager = nullptr;
jobject audioDeviceManagerObject = nullptr;
jmethodID notifyConnectedMethod = nullptr;
jmethodID notifyDisconnectedMethod = nullptr;

std::vector<AudioObjectID> lastDeviceList;

// =====================
// Init JNI cache
// =====================
bool initJNICommon(JNIEnv* env) {
    if (clsAudioDeviceInfo) return true;

    env->GetJavaVM(&gJvm);
    // 1. AudioDeviceInfo class
    clsAudioDeviceInfo = (jclass)env->NewGlobalRef(
        env->FindClass("org/plovdev/audioengine/devices/AudioDeviceInfo")
    );
    if (!clsAudioDeviceInfo) return false;

    ctorAudioDeviceInfo = env->GetMethodID(
        clsAudioDeviceInfo,
        "<init>",
        "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lorg/plovdev/audioengine/devices/AudioDeviceInfo$AudioDeviceType;Ljava/util/List;)V"
    );
    if (!ctorAudioDeviceInfo) return false;

    // 2. AudioDeviceType enum
    clsAudioDeviceType = (jclass)env->NewGlobalRef(
        env->FindClass("org/plovdev/audioengine/devices/AudioDeviceInfo$AudioDeviceType")
    );
    if (!clsAudioDeviceType) return false;

    fidInputType = env->GetStaticFieldID(clsAudioDeviceType, "INPUT",
        "Lorg/plovdev/audioengine/devices/AudioDeviceInfo$AudioDeviceType;");
    fidOutputType = env->GetStaticFieldID(clsAudioDeviceType, "OUTPUT",
        "Lorg/plovdev/audioengine/devices/AudioDeviceInfo$AudioDeviceType;");
    fidDuplexType = env->GetStaticFieldID(clsAudioDeviceType, "DUPLEX",
        "Lorg/plovdev/audioengine/devices/AudioDeviceInfo$AudioDeviceType;");

    if (!fidInputType || !fidOutputType || !fidDuplexType) return false;

    // 3. ArrayList for List<TrackFormat>
    clsArrayList = (jclass)env->NewGlobalRef(
        env->FindClass("java/util/ArrayList")
    );
    if (!clsArrayList) return false;

    ctorArrayList = env->GetMethodID(clsArrayList, "<init>", "()V");
    arrayListAdd = env->GetMethodID(clsArrayList, "add", "(Ljava/lang/Object;)Z");

    if (!ctorArrayList || !arrayListAdd) return false;

    return true;
}


// =====================
// Get device channels
// =====================
int getDeviceChannels(AudioDeviceID devId, bool isInput) {
    AudioObjectPropertyScope scope = isInput ?
        kAudioObjectPropertyScopeInput : kAudioObjectPropertyScopeOutput;

    AudioObjectPropertyAddress streamAddr{
        kAudioDevicePropertyStreamConfiguration,
        scope,
        kAudioObjectPropertyElementMaster
    };

    UInt32 streamSize = 0;
    if (AudioObjectGetPropertyDataSize(devId, &streamAddr, 0, nullptr, &streamSize) != noErr || streamSize == 0) {
        return 0;
    }

    AudioBufferList* bufferList = (AudioBufferList*)malloc(streamSize);
    if (!bufferList) return 0;

    if (AudioObjectGetPropertyData(devId, &streamAddr, 0, nullptr, &streamSize, bufferList) != noErr) {
        free(bufferList);
        return 0;
    }

    int channels = 0;
    for (UInt32 i = 0; i < bufferList->mNumberBuffers; i++) {
        channels += bufferList->mBuffers[i].mNumberChannels;
    }

    free(bufferList);
    return channels;
}

// =====================
// Determine device type
// =====================
jobject getDeviceType(JNIEnv* env, AudioDeviceID devId) {
    int inputChannels = getDeviceChannels(devId, true);
    int outputChannels = getDeviceChannels(devId, false);

    if (inputChannels > 0 && outputChannels > 0) {
        // Duplex device
        return env->GetStaticObjectField(clsAudioDeviceType, fidDuplexType);
    } else if (inputChannels > 0) {
        // Input only
        return env->GetStaticObjectField(clsAudioDeviceType, fidInputType);
    } else {
        // Output only
        return env->GetStaticObjectField(clsAudioDeviceType, fidOutputType);
    }
}

// =====================
// Get device name
// =====================
std::string getDeviceName(AudioDeviceID devId, bool isInput) {
    CFStringRef nameRef = nullptr;
    UInt32 propSize = sizeof(nameRef);

    AudioObjectPropertyAddress nameAddr{
        kAudioObjectPropertyName,
        isInput ? kAudioObjectPropertyScopeInput : kAudioObjectPropertyScopeOutput,
        kAudioObjectPropertyElementMaster
    };

    if (AudioObjectGetPropertyData(devId, &nameAddr, 0, nullptr, &propSize, &nameRef) != noErr || !nameRef) {
        return "";
    }

    std::string name = CFStringToStdString(nameRef);
    CFRelease(nameRef);
    return name;
}

// =====================
// Get device name global
// =====================
std::string getDeviceName(AudioDeviceID devId) {
    CFStringRef nameRef = nullptr;
    UInt32 propSize = sizeof(nameRef);

    AudioObjectPropertyAddress nameAddr{
        kAudioObjectPropertyName,
        kAudioObjectPropertyScopeGlobal,
        kAudioObjectPropertyElementMaster
    };

    if (AudioObjectGetPropertyData(devId, &nameAddr, 0, nullptr, &propSize, &nameRef) != noErr || !nameRef) {
        return "";
    }

    std::string name = CFStringToStdString(nameRef);
    CFRelease(nameRef);
    return name;
}

void initTrackFormatClass(JNIEnv* env) {
    if (clsTrackFormat) return;

    clsTrackFormat = (jclass)env->NewGlobalRef(
        env->FindClass("org/plovdev/audioengine/format/TrackFormat")
    );

    if (clsTrackFormat) {
        ctorTrackFormat = env->GetMethodID(
            clsTrackFormat,
            "<init>",
            "(IIIZLjava/nio/ByteOrder;Lorg/plovdev/audioengine/format/TrackFormat$AudioCodec;)V"
        );
    }
}

jobject getAudioCodecForASBD(JNIEnv* env, const AudioStreamBasicDescription& asbd) {
    jclass clsAudioCodec = env->FindClass("org/plovdev/audioengine/format/TrackFormat$AudioCodec");
    if (!clsAudioCodec) return nullptr;

    bool isFloat  = (asbd.mFormatFlags & kAudioFormatFlagIsFloat) != 0;
    bool isSigned = (asbd.mFormatFlags & kAudioFormatFlagIsSignedInteger) != 0;

    jfieldID fidCodec = nullptr;

    if (isFloat) {
        if (asbd.mBitsPerChannel == 32) {
            fidCodec = env->GetStaticFieldID(clsAudioCodec, "FLOAT32",
                "Lorg/plovdev/audioengine/format/TrackFormat$AudioCodec;");
        } else if (asbd.mBitsPerChannel == 64) {
            fidCodec = env->GetStaticFieldID(clsAudioCodec, "FLOAT64",
                "Lorg/plovdev/audioengine/format/TrackFormat$AudioCodec;");
        }
    } else if (isSigned) {
        switch(asbd.mBitsPerChannel) {
            case 8:  fidCodec = env->GetStaticFieldID(clsAudioCodec, "PCM8",
                "Lorg/plovdev/audioengine/format/TrackFormat$AudioCodec;"); break;
            case 16: fidCodec = env->GetStaticFieldID(clsAudioCodec, "PCM16",
                "Lorg/plovdev/audioengine/format/TrackFormat$AudioCodec;"); break;
            case 20: fidCodec = env->GetStaticFieldID(clsAudioCodec, "PCM24",
                "Lorg/plovdev/audioengine/format/TrackFormat$AudioCodec;"); break;
            case 24: fidCodec = env->GetStaticFieldID(clsAudioCodec, "PCM24",
                "Lorg/plovdev/audioengine/format/TrackFormat$AudioCodec;"); break;
            case 32: fidCodec = env->GetStaticFieldID(clsAudioCodec, "PCM32",
                "Lorg/plovdev/audioengine/format/TrackFormat$AudioCodec;"); break;
        }
    }

    if (!fidCodec) {
        fidCodec = env->GetStaticFieldID(clsAudioCodec, "PCM16",
            "Lorg/plovdev/audioengine/format/TrackFormat$AudioCodec;");
    }

    if (!fidCodec) return nullptr;

    return env->GetStaticObjectField(clsAudioCodec, fidCodec);
}

// =====================
// Get supported formats
// =====================
jobject getDeviceSupportedFormats(JNIEnv* env, AudioDeviceID devId, bool isInput) {
    initTrackFormatClass(env);
    if (!clsTrackFormat || !ctorTrackFormat) {
        return env->NewObject(clsArrayList, ctorArrayList);
    }

    jobject formatList = env->NewObject(clsArrayList, ctorArrayList);
    if (!formatList) return nullptr;

    jclass clsByteOrder = env->FindClass("java/nio/ByteOrder");
    jfieldID fidLE = env->GetStaticFieldID(clsByteOrder, "LITTLE_ENDIAN", "Ljava/nio/ByteOrder;");
    jobject byteOrderLE = fidLE ? env->GetStaticObjectField(clsByteOrder, fidLE) : nullptr;

    AudioObjectPropertyScope scope = isInput ?
        kAudioObjectPropertyScopeInput : kAudioObjectPropertyScopeOutput;

    AudioObjectPropertyAddress addrStreams{
        kAudioDevicePropertyStreams,
        scope,
        kAudioObjectPropertyElementMaster
    };

    UInt32 size = 0;
    if (AudioObjectGetPropertyDataSize(devId, &addrStreams, 0, nullptr, &size) != noErr) {
        return formatList;
    }

    UInt32 streamCount = size / sizeof(AudioStreamID);
    std::vector<AudioStreamID> streams(streamCount);

    if (AudioObjectGetPropertyData(devId, &addrStreams, 0, nullptr, &size, streams.data()) != noErr) {
        return formatList;
    }

    for (UInt32 i = 0; i < streamCount; i++) {
        AudioStreamID streamID = streams[i];

        UInt32 availableFormatsSize = 0;
        AudioObjectPropertyAddress addrFormats{
            kAudioStreamPropertyPhysicalFormats,
            scope,
            kAudioObjectPropertyElementMaster
        };

        if (AudioObjectGetPropertyDataSize(streamID, &addrFormats, 0, nullptr, &availableFormatsSize) != noErr) {
            continue;
        }

        UInt32 formatCount = availableFormatsSize / sizeof(AudioStreamBasicDescription);
        std::vector<AudioStreamBasicDescription> availableFormats(formatCount);

        if (AudioObjectGetPropertyData(streamID, &addrFormats, 0, nullptr,
                                        &availableFormatsSize, availableFormats.data()) != noErr) {
            continue;
        }

        for (const auto& asbd : availableFormats) {
            bool isSigned = (asbd.mFormatFlags & kAudioFormatFlagIsSignedInteger) != 0;
            bool isFloat  = (asbd.mFormatFlags & kAudioFormatFlagIsFloat) != 0;
            jboolean signedFlag = isSigned || isFloat;

            jobject codecEnum = getAudioCodecForASBD(env, asbd);

            jobject tf = env->NewObject(
                clsTrackFormat, ctorTrackFormat,
                (jint)asbd.mChannelsPerFrame,
                (jint)asbd.mBitsPerChannel,
                (jint)asbd.mSampleRate,
                signedFlag,
                byteOrderLE,
                codecEnum
            );

            if (tf) {
                env->CallBooleanMethod(formatList, arrayListAdd, tf);
                env->DeleteLocalRef(tf);
            }
        }
    }

    return formatList;
}

// =====================
// Create AudioDeviceInfo
// =====================
jobject createAudioDeviceInfo(
        JNIEnv* env,
        AudioDeviceID devId,
        const std::string& name
) {
    jobject deviceType = getDeviceType(env, devId);
    if (!deviceType) return nullptr;

    bool isInputDevice = false;
    if (env->IsSameObject(deviceType, env->GetStaticObjectField(clsAudioDeviceType, fidInputType))) {
        isInputDevice = true;
    } else if (env->IsSameObject(deviceType, env->GetStaticObjectField(clsAudioDeviceType, fidDuplexType))) {
        isInputDevice = true;
    }

    jobject formatsList = getDeviceSupportedFormats(env, devId, isInputDevice);
    if (!formatsList) {
        formatsList = env->NewObject(clsArrayList, ctorArrayList);
    }

    std::string vendorStr = "Unknown";
    CFStringRef vendorRef = nullptr;
    UInt32 dataSize = sizeof(vendorRef);

    AudioObjectPropertyAddress propAddrVendor {
        kAudioDevicePropertyDeviceManufacturerCFString,
        kAudioObjectPropertyScopeGlobal,
        kAudioObjectPropertyElementMaster
    };

    OSStatus status = AudioObjectGetPropertyData(devId, &propAddrVendor, 0, nullptr, &dataSize, &vendorRef);

    if (status == noErr && vendorRef != nullptr) {
        char buffer[256];
        if (CFStringGetCString(vendorRef, buffer, sizeof(buffer), kCFStringEncodingUTF8)) {
            vendorStr = buffer;
        }
        CFRelease(vendorRef);
    } else {
        UInt32 vendorCode = 0;
        dataSize = sizeof(vendorCode);

        AudioObjectPropertyAddress propAddrVendorCode {
            kAudioDevicePropertyDeviceManufacturer,
            kAudioObjectPropertyScopeGlobal,
            kAudioObjectPropertyElementMaster
        };

        if (AudioObjectGetPropertyData(devId, &propAddrVendorCode, 0, nullptr, &dataSize, &vendorCode) == noErr) {
            std::ostringstream oss;
            oss << std::hex << std::uppercase << std::setfill('0')
                << std::setw(2) << ((vendorCode >> 24) & 0xFF)
                << std::setw(2) << ((vendorCode >> 16) & 0xFF)
                << std::setw(2) << ((vendorCode >> 8) & 0xFF)
                << std::setw(2) << (vendorCode & 0xFF);
            vendorStr = oss.str();
        }
    }

    // Создаем AudioDeviceInfo
    return env->NewObject(
        clsAudioDeviceInfo,
        ctorAudioDeviceInfo,
        env->NewStringUTF(std::to_string(devId).c_str()), // id
        env->NewStringUTF(name.c_str()),                  // name
        env->NewStringUTF(vendorStr.c_str()),             // vendor
        deviceType,                                       // AudioDeviceType enum
        formatsList                                       // List<TrackFormat>
    );
}

// =====================
// Get all devices
// =====================
jobject getDevicesByScope(JNIEnv* env, bool wantInputDevices) {
    // Get all devices
    AudioObjectPropertyAddress addrAllDevices{
        kAudioHardwarePropertyDevices,
        kAudioObjectPropertyScopeGlobal,
        kAudioObjectPropertyElementMaster
    };

    UInt32 size = 0;
    if (AudioObjectGetPropertyDataSize(kAudioObjectSystemObject, &addrAllDevices, 0, nullptr, &size) != noErr) {
        return env->NewObject(clsArrayList, ctorArrayList);
    }

    int count = size / sizeof(AudioDeviceID);
    if (count == 0) {
        return env->NewObject(clsArrayList, ctorArrayList);
    }

    std::vector<AudioDeviceID> allDevices(count);
    if (AudioObjectGetPropertyData(kAudioObjectSystemObject, &addrAllDevices, 0, nullptr,
                                   &size, allDevices.data()) != noErr) {
        return env->NewObject(clsArrayList, ctorArrayList);
    }

    jobject deviceList = env->NewObject(clsArrayList, ctorArrayList);


    for (AudioDeviceID devId : allDevices) {
        int inputChannels = getDeviceChannels(devId, true);
        int outputChannels = getDeviceChannels(devId, false);

        if (wantInputDevices && inputChannels == 0) continue;
        if (!wantInputDevices && outputChannels == 0) continue;

        std::string name = getDeviceName(devId, wantInputDevices);
        if (name.empty()) {
            name = getDeviceName(devId, !wantInputDevices);
            if (name.empty()) continue;
        }

        jobject deviceInfo = createAudioDeviceInfo(env, devId, name);
        if (!deviceInfo) continue;

        env->CallBooleanMethod(deviceList, arrayListAdd, deviceInfo);
        env->DeleteLocalRef(deviceInfo);
    }

    return deviceList;
}

// =====================
// Get default device by scope
// =====================
jobject getDefaultDeviceByScope(JNIEnv* env, bool isInput) {
    AudioDeviceID deviceId;
    UInt32 size = sizeof(AudioDeviceID);

    AudioObjectPropertyAddress addrDefault;

    if (isInput) {
        addrDefault = {
            kAudioHardwarePropertyDefaultInputDevice,
            kAudioObjectPropertyScopeGlobal,
            kAudioObjectPropertyElementMaster
        };
    } else {
        addrDefault = {
            kAudioHardwarePropertyDefaultOutputDevice,
            kAudioObjectPropertyScopeGlobal,
            kAudioObjectPropertyElementMaster
        };
    }

    if (AudioObjectGetPropertyData(kAudioObjectSystemObject, &addrDefault, 0, nullptr,
                                   &size, &deviceId) != noErr) {
        return nullptr;
    }

    std::string name = getDeviceName(deviceId, isInput);
    if (name.empty()) return nullptr;

    return createAudioDeviceInfo(env, deviceId, name);
}


std::vector<AudioObjectID> getCurrentAudioDeviceList() {
    AudioObjectPropertyAddress addr{
        kAudioHardwarePropertyDevices,
        kAudioObjectPropertyScopeGlobal,
        kAudioObjectPropertyElementMaster
    };

    UInt32 size = 0;
    if (AudioObjectGetPropertyDataSize(kAudioObjectSystemObject, &addr, 0, nullptr, &size) != noErr) {
        return {};
    }

    int count = size / sizeof(AudioDeviceID);
    std::vector<AudioDeviceID> devices(count);
    if (AudioObjectGetPropertyData(kAudioObjectSystemObject, &addr, 0, nullptr, &size, devices.data()) != noErr) {
        return {};
    }

    return devices;
}

OSStatus audioDeviceChangedCallback(AudioObjectID inObjectID, UInt32 inNumberAddresses, const AudioObjectPropertyAddress inAddresses[], void* inClientData) {
    JNIEnv* env;
    bool attached = false;
    if (gJvm->GetEnv((void**)&env, JNI_VERSION_1_8) != JNI_OK) {
        gJvm->AttachCurrentThread((void**)&env, nullptr);
        attached = true;
    }

    std::vector<AudioObjectID> currentDevices = getCurrentAudioDeviceList();

    for (AudioObjectID dev : currentDevices) {
        if (std::find(lastDeviceList.begin(), lastDeviceList.end(), dev) == lastDeviceList.end()) {
            std::string name = getDeviceName(dev);
            jobject deviceInfo = createAudioDeviceInfo(env, dev, name);

            env->CallVoidMethod(audioDeviceManagerObject, notifyConnectedMethod, deviceInfo);
            env->DeleteLocalRef(deviceInfo);
        }
    }

    for (AudioObjectID dev : lastDeviceList) {
        if (std::find(currentDevices.begin(), currentDevices.end(), dev) == currentDevices.end()) {
            std::string name = getDeviceName(dev);
            jobject deviceInfo = createAudioDeviceInfo(env, dev, name);

            env->CallVoidMethod(audioDeviceManagerObject, notifyDisconnectedMethod, deviceInfo);
            env->DeleteLocalRef(deviceInfo);
        }
    }

    lastDeviceList = currentDevices;

    if (attached) {
        gJvm->DetachCurrentThread();
    }
    return noErr;
}

void subscribeToNativeDeviceEvents() {
    AudioObjectPropertyAddress addr = {
        kAudioHardwarePropertyDevices,
        kAudioObjectPropertyScopeGlobal,
        kAudioObjectPropertyElementMaster
    };
    AudioObjectAddPropertyListener(kAudioObjectSystemObject, &addr, &audioDeviceChangedCallback, nullptr);
}

// =====================
// JNI Methods
// =====================
extern "C" {
    /*
     * Class:     org_plovdev_audioengine_devices_AudioDeviceManager
     * Method:    _initCallback
     * Signature: ()V
     */
    JNIEXPORT void JNICALL Java_org_plovdev_audioengine_devices_AudioDeviceManager__1initManager(JNIEnv* env, jobject obj) {
        initJNICommon(env);
        getDevicesByScope(env, true);
        getDevicesByScope(env, false);

        jclass local = env->GetObjectClass(obj);
        clsAudioDeviceManager = (jclass) env->NewGlobalRef(local);

        audioDeviceManagerObject = env->NewGlobalRef(obj);

        notifyConnectedMethod = env->GetMethodID(clsAudioDeviceManager, "notifyConnected", "(Lorg/plovdev/audioengine/devices/AudioDeviceInfo;)V");
        notifyDisconnectedMethod = env->GetMethodID(clsAudioDeviceManager, "notifyDisconnected", "(Lorg/plovdev/audioengine/devices/AudioDeviceInfo;)V");

        lastDeviceList = getCurrentAudioDeviceList();
        subscribeToNativeDeviceEvents();
    }


    JNIEXPORT jobject JNICALL Java_org_plovdev_audioengine_devices_AudioDeviceManager__1getInputAudioDevices(JNIEnv *env, jobject obj) {
        return getDevicesByScope(env, true);
    }

    JNIEXPORT jobject JNICALL Java_org_plovdev_audioengine_devices_AudioDeviceManager__1getOutputAudioDevices(JNIEnv *env, jobject obj) {
        return getDevicesByScope(env, false);
    }

    JNIEXPORT jobject JNICALL Java_org_plovdev_audioengine_devices_AudioDeviceManager__1getDefaultInputAudioDevice(JNIEnv *env, jobject obj) {
        return getDefaultDeviceByScope(env, true);
    }

    JNIEXPORT jobject JNICALL Java_org_plovdev_audioengine_devices_AudioDeviceManager__1getDefaultOutputAudioDevice(JNIEnv *env, jobject obj) {
        return getDefaultDeviceByScope(env, false);
    }
}