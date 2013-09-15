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

package com.vmihalachi.turboeditor;

import java.util.regex.Pattern;

/**
 * User: Vlad Date: 29/07/13 Time: 14.33
 */
public class Patterns {
    public static final int COLOR_NUMBER = 0xffff6600;
    public static final int COLOR_KEYWORD = 0xff2f6f9f;
    public static final int COLOR_ATTR = 0xff4f9fcf;
    public static final int COLOR_ATTR_VALUE = 0xffd44950;
    public static final int COLOR_STRING = 0xffd44950;
    public static final int COLOR_COMMENT = 0xff999999;

    // Strings
    public static final Pattern GENERAL_STRINGS = Pattern.compile("\"(.*?)\"|'(.*?)'");

    public static final Pattern HTML_OPEN_TAGS = Pattern.compile(
            "<([A-Za-z][A-Za-z0-9]*)\\b[^>]*>");
    public static final Pattern HTML_CLOSE_TAGS = Pattern.compile(
            "</([A-Za-z][A-Za-z0-9]*)\\b[^>]*>");
    public static final Pattern HTML_ATTRS = Pattern.compile(
            "(\\S+)=[\"']?((?:.(?![\"']?\\s+(?:\\S+)=|[>\"']))+.)[\"']?");

    //static final Pattern CSS_STYLE_NAME= Pattern.compile(
    //    "[ \\t\\n\\r\\f](.+?)\\{([^\\)]+)\\}");
    public static final Pattern CSS_ATTRS = Pattern.compile(
            "(.+?):(.+?);");
    public static final Pattern CSS_ATTR_VALUE = Pattern.compile(
            ":[ \t](.+?);");

    public static final Pattern NUMBERS = Pattern.compile(
            "\\b(\\d*[.]?\\d+)\\b");
    public static final Pattern CSS_NUMBERS = Pattern.compile(
            "/^auto$|^[+-]?[0-9]+\\.?([0-9]+)?(px|em|ex|%|in|cm|mm|pt|pc)?$/ig");
    public static final Pattern GENERAL_KEYWORDS = Pattern.compile(
            "\\b(alignas|alignof|and|and_eq|asm|auto|bitand|bitorbool|break|case|catch|char|"
                    + "char16_t|char32_t|class|compl|const|constexpr|const_cast|continue|decltype"
                    + "|default|delete|do|double|dynamic_cast|else|enum|explicit|export|extern|"
                    + "false|float|for|friend|function|goto|if|inline|int|mutable|namespace|new|noexcept|"
                    + "not|not_eq|nullptr|operator|or|or_eq|private|protected|public|register|"
                    + "reinterpret_cast|return|short|signed|sizeof|static|static_assert|static_cast"
                    + "|struct|switch|template|this|thread_local|throw|true|try|typedef|typeid|typename"
                    + "|union|unsigned|using|var|virtual|void|volatile|wchar_t|while|xor|xor_eq)\\b");
    // Comments
    public static final Pattern XML_COMMENTS = Pattern.compile("(?s)<!--.*?-->");
    public static final Pattern GENERAL_COMMENTS = Pattern.compile(
            "/\\*(?:.|[\\n\\r])*?\\*/|//.*");
}
