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

import android.app.Activity;

import sharedcode.turboeditor.R;
import sharedcode.turboeditor.preferences.PreferenceHelper;

public class ThemeHelper {

    public static void setWindowsBackground(Activity activity) {
        boolean whiteTheme = PreferenceHelper.getLightTheme(activity);
        if (whiteTheme) {
            activity.getWindow().setBackgroundDrawableResource(R.color.window_background_light);
        } else {
            activity.getWindow().setBackgroundDrawableResource(R.color.window_background);
        }
    }
}
