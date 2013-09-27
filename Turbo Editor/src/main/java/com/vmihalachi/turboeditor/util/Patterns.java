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

package com.vmihalachi.turboeditor.util;

import java.util.regex.Pattern;

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
