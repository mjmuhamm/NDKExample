//
// Created by Malik Muhammad on 3/11/26.
//
#include <jni.h>
#include <android/bitmap.h>
#include <android/log.h>

#define LOG_TAG "NativeLib"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" JNIEXPORT void JNICALL
Java_com_example_ndkexample_data_NativeProcessor_convertToGray(
        JNIEnv *env,
        jobject thiz,
        jobject bitmap
) {

    AndroidBitmapInfo info;
    void *pixels;
    int ret;

    if ((ret = AndroidBitmap_lockPixels(env, bitmap, &pixels)) < 0) {
        LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
        return;
    }

    //process pixels
    for (int y = 0; y < info.height; y++) {
        auto *line = (uint32_t *) pixels;
        for (int x = 0; x < info.width; x++) {
            uint32_t rgba = line[x];

            //extract channels
            uint8_t r = rgba & 0xFF;
            uint8_t g = (rgba >> 8) & 0xFF;
            uint8_t b = (rgba >> 16) & 0xFF;
            uint8_t a = (rgba >> 24) & 0xFF;

            //simple grayscale average
            uint8_t gray = (r + g + b) / 3;

            // reassemble
            line[x] = (a << 24) | (gray << 16) | (gray << 8);
        }
        pixels = (char *) pixels + info.stride;
    }
    AndroidBitmap_unlockPixels(env, bitmap);

}