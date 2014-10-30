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

import android.os.Build;

/**
 * Contains params of current device. This is nice because we can override
 * some here to test compatibility with old API.
 *
 * @author Artem Chepurnoy
 */
public class Device {

    /**
     * @return {@code true} if device is device supports given API version,
     * {@code false} otherwise.
     */
    public static boolean hasTargetApi(int api) {
        return Build.VERSION.SDK_INT >= api;
    }

    /**
     * @return {@code true} if device is running
     * {@link android.os.Build.VERSION_CODES#L Lemon Cake} or higher, {@code false} otherwise.
     */
    public static boolean hasLemonCakeApi() {
        return Build.VERSION.SDK_INT >= 20; // Build.VERSION_CODES.L;
    }

    /**
     * @return {@code true} if device is running
     * {@link android.os.Build.VERSION_CODES#KITKAT KitKat} or higher, {@code false} otherwise.
     */
    public static boolean hasKitKatApi() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    }

    /**
     * @return {@code true} if device is running
     * {@link android.os.Build.VERSION_CODES#JELLY_BEAN_MR2 Jelly Bean 4.3} or higher, {@code false} otherwise.
     */
    public static boolean hasJellyBeanMR2Api() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2;
    }

    /**
     * @return {@code true} if device is running
     * {@link android.os.Build.VERSION_CODES#JELLY_BEAN_MR1 Jelly Bean 4.2} or higher, {@code false} otherwise.
     */
    public static boolean hasJellyBeanMR1Api() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1;
    }

}
