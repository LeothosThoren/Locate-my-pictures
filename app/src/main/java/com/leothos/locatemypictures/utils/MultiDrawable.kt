package com.leothos.locatemypictures.utils

/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

/**
 * Draws up to four other drawables.
 */
class MultiDrawable(private val mDrawables: List<Drawable>) : Drawable() {

    override fun draw(canvas: Canvas) {
        if (mDrawables.size == 1) {
            mDrawables[0].draw(canvas)
            return
        }
        val width = bounds.width()
        val height = bounds.height()

        canvas.save()
        canvas.clipRect(0, 0, width, height)

        if (mDrawables.size == 2 || mDrawables.size == 3) {
            // Paint left half
            canvas.save()
            canvas.clipRect(0, 0, width / 2, height)
            canvas.translate((-width / 4).toFloat(), 0f)
            mDrawables[0].draw(canvas)
            canvas.restore()
        }
        if (mDrawables.size == 2) {
            // Paint right half
            canvas.save()
            canvas.clipRect(width / 2, 0, width, height)
            canvas.translate((width / 4).toFloat(), 0f)
            mDrawables[1].draw(canvas)
            canvas.restore()
        } else {
            // Paint top right
            canvas.save()
            canvas.scale(.5f, .5f)
            canvas.translate(width.toFloat(), 0f)
            mDrawables[1].draw(canvas)

            // Paint bottom right
            canvas.translate(0f, height.toFloat())
            mDrawables[2].draw(canvas)
            canvas.restore()
        }

        if (mDrawables.size >= 4) {
            // Paint top left
            canvas.save()
            canvas.scale(.5f, .5f)
            mDrawables[0].draw(canvas)

            // Paint bottom left
            canvas.translate(0f, height.toFloat())
            mDrawables[3].draw(canvas)
            canvas.restore()
        }

        canvas.restore()
    }

    override fun setAlpha(i: Int) {

    }

    override fun setColorFilter(colorFilter: ColorFilter?) {

    }

    override fun getOpacity(): Int {
        return PixelFormat.UNKNOWN
    }
}