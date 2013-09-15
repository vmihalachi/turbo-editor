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

/**
 * User: Vlad Date: 06/06/13 Time: 20.39
 */
public final class StringHelper {

    private StringHelper() {
    }

    public static String join(final String... strings) {
        final StringBuffer buffer = new StringBuffer();
        for (String string : strings) {
            if (!string.endsWith("/")) {
                string += "/";
            }
            buffer.append(string);
        }
        String result = buffer.toString();
        if (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
