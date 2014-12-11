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

public class MimeTypes {
    public static final String[] MIME_TEXT = {
            "ajx", "am", "asa", "asc", "asp", "aspx", "awk", "bat", "c", "cdf", "cf", "cfg", "cfm", "cgi", "cnf", "conf",
            "cc", "cpp", "css", "csv", "ctl", "dat", "dhtml", "diz", "file", "forward", "grp", "h", "hh", "hpp", "hqx", "hta", "htaccess",
            "htc", "htm", "html", "htpasswd", "htt", "htx", "in", "inc", "info", "ini", "ink", "java", "js", "jsp", "key", "latex", "log",
            "logfile", "m3u", "m4", "m4a", "mak", "map", "md", "markdown", "model", "msg", "nfo", "nsi", "info", "old", "pas", "patch", "perl",
            "php", "php2", "php3", "php4", "php5", "php6", "phtml", "pix", "pl", "pm", "po", "pwd", "py", "qmail", "rb", "rbl", "rbw",
            "readme", "reg", "rss", "rtf", "ruby", "session", "setup", "sh", "shtm", "shtml", "sql", "ssh", "stm", "style", "svg", "tcl",
            "tex", "text", "threads", "tmpl", "tpl", "txt", "ubb", "vbs", "xhtml", "xml", "xrc", "xsl"
    };
    public static final String[] MIME_CODE = {
    	    "cs", "php", "js", "java", "py", "rb", "aspx", "cshtml", "vbhtml", "go", "c", "h", "cc", "cpp", "hh", "hpp", "pl", "pm", "t", "pod",
            "m", "f", "for", "f90", "f95", "asp", "json", "wiki", "lua", "r"
    };
    public static final String[] MIME_HTML = {
            "htm", "html", "xhtml"
    };
    public static final String[] MIME_PICTURE = {
    		"bmp", "eps", "png", "jpeg", "jpg", "ico", "gif", "tiff", "webp"
    };
    public static final String[] MIME_MUSIC = {
    		"aac", "flac", "mp3", "mpga", "oga", "ogg", "opus", "webma", "wav"
    };
    public static final String[] MIME_VIDEO = {
            "avi", "mp4", "mkv", "wmw", "ogv", "webm"
    };
    public static final String[] MIME_ARCHIVE = {
    		"7z", "arj", "bz2", "gz", "rar", "tar", "tgz", "zip", "xz"
    };
    public static final String[] MIME_SQL = {
            "sql", "mdf", "ndf", "ldf"
    };
    public static final String[] MIME_MARKDOWN = {
            "md", "mdown", "markdown",
    };
}
