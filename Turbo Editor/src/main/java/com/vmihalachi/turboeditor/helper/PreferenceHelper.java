/*
 * Copyright (C) 2013 Vlad Mihalachi
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
 * along with Turbo Editor. If not, see <http://www.gnu.org/licenses/>.
 */

package com.vmihalachi.turboeditor.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;

public final class PreferenceHelper {

    private static final String SD_CARD_ROOT = Environment.getExternalStorageDirectory().getAbsolutePath();

    private PreferenceHelper() {
    }

    // Getter Methods

    public static SharedPreferences getPrefs(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static SharedPreferences.Editor getEditor(Context context) {
        return getPrefs(context).edit();
    }

    public static boolean getUseMonospace(Context context) {
        return getPrefs(context).getBoolean("use_monospace", false);
    }

    public static boolean getWrapText(Context context) {
        return getPrefs(context).getBoolean("editor_wrap_text", true);
    }

    public static boolean getSyntaxHiglight(Context context) {
        return getPrefs(context).getBoolean("editor_syntax_highlight", true);
    }

    public static String getEncoding(Context context) {
        return getPrefs(context).getString("editor_encoding", "UTF-8");
    }

    public static int getFontSize(Context context) {
        return getPrefs(context).getInt("font_size", 18);
    }

    public static String getLastNavigatedFolder(Context context) {
        return getPrefs(context).getString("last_navigated_folder", SD_CARD_ROOT);
    }

    public static String[] getSavedPaths(Context context) {
        return getPrefs(context).getString("savedPaths", "").split(",");
    }

    // Setter methods

    public static void setUseMonospace(Context context, boolean value) {
        getEditor(context).putBoolean("use_monospace", value).commit();
    }

    public static void setWrapText(Context context, boolean value) {
        getEditor(context).putBoolean("editor_wrap_text", value).commit();
    }

    public static void setSyntaxHiglight(Context context, boolean value) {
        getEditor(context).putBoolean("editor_syntax_highlight", value).commit();
    }

    public static void setEncoding(Context context, String value) {
        getEditor(context).putString("editor_encoding", value).commit();
    }

    public static void setFontSize(Context context, int value) {
        getEditor(context).putInt("font_size", value).commit();
    }

    public static void setLastNavigatedFolder(Context context, String value) {
        getEditor(context).putString("last_navigated_folder", value).commit();
    }

    public static void setSavedPaths(Context context, StringBuilder stringBuilder) {
        getEditor(context).putString("savedPaths", stringBuilder.toString()).commit();
    }
}
