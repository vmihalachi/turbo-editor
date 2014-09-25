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

package sharedcode.turboeditor.util;

import android.content.Context;

public class EdittextPadding {

    public static int getPaddingWithoutLineNumbers(Context context) {
        return (int) PixelDipConverter.convertDpToPixel(5, context);
    }

    public static int getPaddingWithLineNumbers(Context context, float fontSize) {
        return (int) PixelDipConverter.convertDpToPixel(fontSize * 1.85f, context);
    }

    public static int getPaddingTop(Context context) {
        return getPaddingWithoutLineNumbers(context);
    }
}
