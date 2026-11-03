#include <jni.h>
#include "h/org_plovdev_audioengine_NativeAudioEngine.h"

// Глобальные структуры движка (например, карты устройств)
static bool engineInitialized = false;

extern "C" {

/*
 * Class:     org_plovdev_audioengine_NativeAudioEngine
 * Method:    _init
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_plovdev_audioengine_NativeAudioEngine__1init
  (JNIEnv* env, jobject obj) {
    if (engineInitialized) return;

    engineInitialized = true;
}

/*
 * Class:     org_plovdev_audioengine_NativeAudioEngine
 * Method:    _cleanup
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_plovdev_audioengine_NativeAudioEngine__1cleanup
  (JNIEnv* env, jobject obj) {
    if (!engineInitialized) return;

    engineInitialized = false;
}
}