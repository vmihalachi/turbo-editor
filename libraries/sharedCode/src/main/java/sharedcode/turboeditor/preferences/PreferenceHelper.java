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

package sharedcode.turboeditor.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;

public final class PreferenceHelper {

    public static final String SD_CARD_ROOT = Environment.getExternalStorageDirectory().getAbsolutePath();

    private PreferenceHelper() {
    }

    // Getter Methods

    private static SharedPreferences getPrefs(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    private static SharedPreferences.Editor getEditor(Context context) {
        return getPrefs(context).edit();
    }

    public static boolean getUseMonospace(Context context) {
        return getPrefs(context).getBoolean("use_monospace", false);
    }

    public static boolean getLineNumbers(Context context) {
        return getPrefs(context).getBoolean("editor_line_numbers", true);
    }

    public static boolean getSyntaxHiglight(Context context) {
        return getPrefs(context).getBoolean("editor_syntax_highlight", false);
    }

    public static boolean getWrapContent(Context context) {
        return getPrefs(context).getBoolean("editor_wrap_content", true);
    }

    public static boolean getLightTheme(Context context) {
        return getPrefs(context).getBoolean("light_theme", false);
    }

    public static boolean getSuggestionActive(Context context) {
        return getPrefs(context).getBoolean("suggestion_active", false);
    }

    public static boolean getAutoEncoding(Context context) {
        return getPrefs(context).getBoolean("autoencoding", true);
    }

    public static boolean getSendErrorReports(Context context) {
        return getPrefs(context).getBoolean("send_error_reports", true);
    }

    public static String getEncoding(Context context) {
        return getPrefs(context).getString("editor_encoding", "UTF-8");
    }

    public static int getFontSize(Context context) {
        return getPrefs(context).getInt("font_size", 16);
    }

    public static String getWorkingFolder(Context context) {
        return getPrefs(context).getString("working_folder", SD_CARD_ROOT);
    }

    public static String[] getSavedPaths(Context context) {
        return getPrefs(context).getString("savedPaths", "").split(",");
    }

    public static boolean getPageSystemButtonsPopupShown(Context context) {
        return getPrefs(context).getBoolean("page_system_button_popup_shown", false);
    }

    public static boolean getAutoSave(Context context) {
        return getPrefs(context).getBoolean("auto_save", false);
    }

    public static boolean getReadOnly(Context context) {
        return getPrefs(context).getBoolean("read_only", false);
    }

    public static boolean getIgnoreBackButton(Context context) {
        return getPrefs(context).getBoolean("ignore_back_button", false);
    }

    public static boolean getPageSystemEnabled(Context context) {
        return getPrefs(context).getBoolean("page_system_active", true);
    }

    public static boolean hasDonated(Context context) {
        return getPrefs(context).getBoolean("has_donated", false);
    }
    // Setter methods

    public static void setUseMonospace(Context context, boolean value) {
        getEditor(context).putBoolean("use_monospace", value).commit();
    }

    public static void setLineNumbers(Context context, boolean value) {
        getEditor(context).putBoolean("editor_line_numbers", value).commit();
    }

    public static void setSyntaxHiglight(Context context, boolean value) {
        getEditor(context).putBoolean("editor_syntax_highlight", value).commit();
    }

    public static void setWrapContent(Context context, boolean value) {
        getEditor(context).putBoolean("editor_wrap_content", value).commit();
    }

    public static void setAutoencoding(Context context, boolean value) {
        getEditor(context).putBoolean("autoencoding", value).commit();
    }

    public static void setFontSize(Context context, int value) {
        getEditor(context).putInt("font_size", value).commit();
    }

    public static void setWorkingFolder(Context context, String value) {
        getEditor(context).putString("working_folder", value).commit();
    }

    public static void setSavedPaths(Context context, StringBuilder stringBuilder) {
        getEditor(context).putString("savedPaths", stringBuilder.toString()).commit();
    }

    public static void setPageSystemButtonsPopupShown(Context context, boolean value) {
        getEditor(context).putBoolean("page_system_button_popup_shown", value).commit();
    }

    public static void setReadOnly(Context context, boolean value) {
        getEditor(context).putBoolean("read_only", value).commit();
    }

    public static void setHasDonated(Context context, boolean value) {
        getEditor(context).putBoolean("has_donated", value);
    }

}
