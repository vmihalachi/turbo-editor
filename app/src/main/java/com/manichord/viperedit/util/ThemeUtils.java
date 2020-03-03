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

package com.manichord.viperedit.util;

import android.app.Activity;

import com.manichord.viperedit.R;
import com.manichord.viperedit.preferences.PreferenceHelper;

public class ThemeUtils {

    public static void setTheme(Activity activity){
        boolean light = PreferenceHelper.getLightTheme(activity);
        if (light) {
            activity.setTheme(R.style.AppThemeBaseLight);
        } else {
            activity.setTheme(R.style.AppThemeBaseDark);
        }
    }

    public static void setWindowsBackground(Activity activity) {
        boolean whiteTheme = PreferenceHelper.getLightTheme(activity);
        boolean darkTheme = PreferenceHelper.getDarkTheme(activity);
        boolean blackTheme = PreferenceHelper.getBlackTheme(activity);
        if (whiteTheme) {
            activity.getWindow().setBackgroundDrawableResource(R.color.window_background_light);
        } else if (darkTheme) {
            activity.getWindow().setBackgroundDrawableResource(R.color.window_background);
        } else {
            activity.getWindow().setBackgroundDrawableResource(android.R.color.black);
        }
    }
}
