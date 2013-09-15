/*******************************************************************************
 * Copyright (c) 2012, 2013 Vlad Mihalachi
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.vmihalachi.turboeditor.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * User: Vlad Date: 21/07/13 Time: 14.09
 */
public final class PreferenceHelper {

    private PreferenceHelper() {
    }

    /**
     * Getter Methods
     */
    public static SharedPreferences getPrefs(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static SharedPreferences.Editor getEditor(Context context) {
        return getPrefs(context).edit();
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
    /**
     * Setter Methods
     */

    public static void setWrapText(Context context, boolean value) {
        getEditor(context).putBoolean("editor_wrap_text", value).commit();
    }

    public static void setSyntaxHiglight(Context context, boolean value) {
        getEditor(context).putBoolean("editor_syntax_highlight", value).commit();
    }

    public static void setEncoding(Context context, String value) {
        getEditor(context).putString("editor_encoding", value).commit();
    }
}
