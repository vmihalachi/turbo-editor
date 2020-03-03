/*
 * Copyright (C) 2014 Vlad Mihalachi
 *
 * This file is part of Turbo Editor.
 *
 * Turbo Editor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Turbo Editor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.manichord.viperedit.home.texteditor

import android.text.Layout
import android.text.TextUtils
import android.widget.ScrollView

class LineUtils {
    lateinit var goodLines: BooleanArray
        private set
    lateinit var realLines: IntArray
        private set

    fun updateHasNewLineArray(startingLine: Int, lineCount: Int, layout: Layout, text: String) {

        val hasNewLineArray = BooleanArray(lineCount)
        goodLines = BooleanArray(lineCount)
        realLines = IntArray(lineCount)

        if (TextUtils.isEmpty(text)) {
            goodLines[0] = false
            realLines[0] = 1
            return
        }

        var i = 0

        // for every line on the edittext
        while (i < lineCount) {
            // check if this line contains "\n"
            //hasNewLineArray[i] = text.substring(layout.getLineStart(i), layout.getLineEnd(i)).endsWith("\n");
            hasNewLineArray[i] = text[layout.getLineEnd(i) - 1] == '\n'
            // if true
            if (hasNewLineArray[i]) {
                var j = i - 1
                while (j > -1 && !hasNewLineArray[j]) {
                    j--
                }
                goodLines[j + 1] = true

            }
            i++
        }

        goodLines[lineCount - 1] = true

        var realLine = startingLine // the first line is not 0, is 1. We start counting from 1

        i = 0
        while (i < goodLines.size) {
            if (goodLines[i]) {
                realLine++
            }
            realLines[i] = realLine
            i++
        }
    }

    fun firstReadLine(): Int {
        return realLines[0]
    }

    fun lastReadLine(): Int {
        return realLines[realLines.size - 1]
    }

    fun fakeLineFromRealLine(realLine: Int): Int {
        var i: Int
        var fakeLine = 0
        i = 0
        while (i < realLines.size) {
            if (realLine == realLines[i]) {
                fakeLine = i
                break
            }
            i++
        }
        return fakeLine
    }

    companion object {

        fun getYAtLine(scrollView: ScrollView, lineCount: Int, line: Int): Int {
            return scrollView.getChildAt(0).height / lineCount * line
        }

        @Throws(ArithmeticException::class)
        fun getFirstVisibleLine(scrollView: ScrollView, childHeight: Int, lineCount: Int): Int {
            var line = scrollView.scrollY * lineCount / childHeight
            if (line < 0) line = 0
            return line
        }

        fun getLastVisibleLine(scrollView: ScrollView, childHeight: Int, lineCount: Int, deviceHeight: Int): Int {
            var line = (scrollView.scrollY + deviceHeight) * lineCount / childHeight
            if (line > lineCount) line = lineCount
            return line
        }

        /**
         * Gets the line from the index of the letter in the text
         *
         * @param index
         * @param lineCount
         * @param layout
         * @return
         */
        fun getLineFromIndex(index: Int, lineCount: Int, layout: Layout): Int {
            var line = 0
            var currentIndex = 0

            while (line < lineCount) {
                currentIndex += layout.getLineEnd(line) - layout.getLineStart(line)
                if (currentIndex > index) {
                    break
                }
                line++
            }

            return line
        }
    }

}