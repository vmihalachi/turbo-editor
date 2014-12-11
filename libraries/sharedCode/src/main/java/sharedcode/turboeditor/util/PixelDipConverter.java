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
import android.content.res.Resources;
import android.util.DisplayMetrics;

public final class PixelDipConverter {

    private PixelDipConverter() {
    }

    /**
     * This method convets dp unit to equivalent device specific value in pixels.
     *
     * @param dp      A value in dp(Device independent pixels) unit. Which we need to convert into pixels
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent Pixels equivalent to dp according to device
     */
    public static float convertDpToPixel(final float dp, final Context context) {
        final Resources resources = context.getResources();
        final DisplayMetrics metrics = resources.getDisplayMetrics();
        return dp * metrics.densityDpi / 160f;
    }

    /**
     * This method converts device specific pixels to device independent pixels.
     *
     * @param px      A value in px (pixels) unit. Which we need to convert into db
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent db equivalent to px value
     */
    public static float convertPixelsToDp(final float px, final Context context) {
        final Resources resources = context.getResources();
        final DisplayMetrics metrics = resources.getDisplayMetrics();
        return px / (metrics.densityDpi / 160f);
    }
}
