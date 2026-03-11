package com.example.ndkexample.data

import android.graphics.Bitmap

object NativeProcessor {
    init {
        System.loadLibrary("native_lib")
    }
    external fun convertToGray(bitmap: Bitmap)

    fun convertToGrayKotlin(bitmap: Bitmap) {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)

        // copy pixels into an array
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (i in pixels.indices) {
            val color = pixels[i]

            //color channels
            val a = (color shr 24) and 0xFF
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF

            val gray = (r + g + b) / 3

            //rebuild with new color
            pixels[i] = (a shl 24) or (gray shl 16) or (gray shl 8) or gray
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)

    }
}