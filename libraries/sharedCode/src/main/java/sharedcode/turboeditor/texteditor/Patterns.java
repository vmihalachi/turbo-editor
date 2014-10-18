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

package sharedcode.turboeditor.texteditor;

import java.util.regex.Pattern;

public class Patterns {

    /*
    public static final int COLOR_NUMBER = 0xffff6600;
    public static final int COLOR_KEYWORD = 0xff2f6f9f;
    public static final int COLOR_ATTR = 0xff4f9fcf;
    public static final int COLOR_ATTR_VALUE = 0xffd44950;
    public static final int COLOR_STRING = 0xffd44950;
    public static final int COLOR_COMMENT = 0xff999999;
     */

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
    public static final Pattern SYMBOLS = Pattern.compile(
            "(!|,|\\(|\\)|\\+|\\-|\\*|<|>|=|\\.|\\?|;|\\{|\\}|\\[|\\])");
    public static final Pattern GENERAL_KEYWORDS = Pattern.compile(
            "\\b(alignas|alignof|and|and_eq|asm|auto|bitand|bitorbool|break|case|catch|char|"
                    + "char16_t|char32_t|class|compl|const|constexpr|const_cast|continue|decltype"
                    + "|default|delete|do|double|dynamic_cast|echo|else|enum|explicit|export|extern|"
                    + "false|float|for|friend|function|goto|if|inline|int|mutable|namespace|new|noexcept|"
                    + "not|not_eq|null|nullptr|operator|or|or_eq|private|protected|public|register|"
                    + "reinterpret_cast|return|short|signed|sizeof|static|static_assert|static_cast"
                    + "|struct|switch|template|this|thread_local|throw|true|try|typedef|typeid|typename|undefined"
                    + "|union|unsigned|using|var|virtual|void|volatile|wchar_t|while|xor|xor_eq|)\\b", Pattern.CASE_INSENSITIVE);

    public static final Pattern PY_KEYWORDS = Pattern.compile(
            "\\b(int|float|long|complex|str|unicode|list|tuple|bytearray|buffer|xrange|set|frozenset|dict|bool)|(True|False|None|self|NotImplemented|Ellipsis|__debug__|__file__)|(and|del|from|not|while|as|elif|global|or|with|assert|else|if|pass|yield|break|except|import|print|class|exec|in|raise|continue|finally|is|return|def|for|lambda|try)|(ArithmeticError|AssertionError|AttributeError|BaseException|DeprecationWarning|EnvironmentError|EOFError|Exception|FloatingPointError|FutureWarning|GeneratorExit|IOError|ImportError|ImportWarning|IndexError|KeyError|KeyboardInterrupt|LookupError|MemoryError|NameError|NotImplementedError|OSError|OverflowError|PendingDeprecationWarning|ReferenceError|RuntimeError|RuntimeWarning|StandardError|StopIteration|SyntaxError|SyntaxWarning|SystemError|SystemExit|TypeError|UnboundLocalError|UserWarning|UnicodeError|UnicodeWarning|UnicodeEncodeError|UnicodeDecodeError|UnicodeTranslateError|ValueError|Warning|WindowsError|ZeroDivisionError)\\b", Pattern.CASE_INSENSITIVE);

    public static final Pattern LUA_KEYWORDS = Pattern.compile(
            "@[A-Za-z0-9_\\.]*|\\b(local|global|boolean|number|userdata)\\b|\\b(true|false|nil)\\b|\\b(return|then|while|and|break|do|else|elseif|end|for|function|if|in|not|or|repeat|until|thread|table)\\b" +
                    "|(?i)\\b(editsetText|editText|inkey|touch|system.exit|system.expCall|system.getAppPath|system.getCardMnt|system.getSec|system.impCallActionSend|system.impCallActionView|system.setrun|system.setScreen|system.version|El_Psy_Congroo|canvas.drawCircle|canvas.drawCls|canvas.drawLine|canvas.drawRect|canvas.getBmpSize|canvas.getColor|canvas.getg|canvas.getviewSize|canvas.loadBmp|canvas.putCircle|canvas.putCls|canvas.putflush|canvas.putg|canvas.putLine|canvas.putRect|canvas.putrotg|canvas.putWork|canvas.saveBmp|canvas.setMainBmp|canvas.setWorkBmp|canvas.workCls|canvas.workflush|color|canvas.drawText|canvas.drawTextBox|canvas.drawTextCenter|canvas.drawTextRotate|canvas.putText|canvas.putTextBox|canvas.putTextRotate|http.addHeader|http.addParam|http.clrHeader|http.clrParam|http.get|http.post|http.setContentType|http.setPostFile|http.status|dialog|item.add|item.check|item.clear|item.list|item.radio|toast|sensor.getAccel|sensor.setdevAccel|sensor.setdevMagnet|sensor.setdevOrient|sensor.getGdirection|sensor.getMagnet|sensor.getOrient|sound.beep|sound.isPlay|sound.pause|sound.restart|sound.setSoundFile|sound.start|sound.stop|zip.addFile|zip.exec|zip.status|sock.close|sock.connectOpen|sock.getAddress|sock.listenOpen|sock.recv|sock.send|sprite.clear|sprite.define|sprite.init|sprite.move|sprite.put)\\b" +
                    "|(?i)\\b(assert|collectgarbage|coroutine.create|coroutine.resume|coroutine.running|coroutine.status|coroutine.wrap|coroutine.yield|debug.debug|debug.getfenv|debug.gethook|debug.getinfo|debug.getlocal|debug.getmetatable|debug.getregistry|debug.getupvalue|debug.setfenv|debug.sethook|debug.setlocal|debug.setmetatable|debug.setupvalue|debug.traceback|dofile|error|file:close|file:flush|file:lines|file:read|file:seek|file:setvbuf|file:write|getfenv|getmetatable|io.close|io.flush|io.input|io.lines|io.open|io.output|io.popen|io.read|io.tmpfile|io.type|io.write|ipairs|load|loadfile|loadstring|math.abs|math.acos|math.asin|math.atan2|math.atan|math.ceil|math.cosh|math.cos|math.deg|math.exp|math.floor|math.fmod|math.frexp|math.ldexp|math.log10|math.log|math.max|math.min|math.modf|math.pow|math.rad|math.random|math.randomseed|math.sinh|math.sin|math.sqrt|math.tanh|math.tan|module|next|os.clock|os.date|os.difftime|os.execute|os.exit|os.getenv|os.remove|os.rename|os.setlocale|os.time|os.tmpname|package.cpath|package.loaded|package.loadlib|package.path|package.preload|package.seeal|pairs|pcall|print|rawequal|rawget|rawset|require|select|setfenv|setmetatable|string.byte|string.char|string.dump|string.find|string.format|string.gmatch|string.gsub|string.len|string.lower|string.match|string.rep|string.reverse|string.sub|string.upper|table.concat|table.insert|table.maxn|table.remove|table.sort|tonumber|tostring|type|unpack|xpcall)\\b"
    );

    public static final Pattern PHP_VARIABLES = Pattern.compile("\\$\\s*(\\w+)");
    // Comments
    public static final Pattern XML_COMMENTS = Pattern.compile("(?s)<!--.*?-->");
    public static final Pattern GENERAL_COMMENTS = Pattern.compile(
            "/\\*(?:.|[\\n\\r])*?\\*/|//.*|#.*");
    public static final Pattern GENERAL_COMMENTS_NO_SLASH = Pattern.compile(
            "/\\*(?:.|[\\n\\r])*?\\*/|#.*");
    public static final Pattern SQL_KEYWORDS = Pattern.compile(
            "\\b(ADD|EXCEPT|PERCENT|ALL|EXEC|PLAN|ALTER|EXECUTE|PRECISION|AND|EXISTS|PRIMARY|ANY|EXIT|PRINT|AS|FETCH|PROC|ASC|FILE|PROCEDURE|AUTHORIZATION|FILLFACTOR|PUBLIC|BACKUP|FOR|RAISERROR|BEGIN|FOREIGN|READ|BETWEEN|FREETEXT|READTEXT|BREAK|FREETEXTTABLE|RECONFIGURE|BROWSE|FROM|REFERENCES|BULK|FULL|REPLICATION|BY|FUNCTION|RESTORE|CASCADE|GOTO|RESTRICT|CASE|GRANT|RETURN|CHECK|GROUP|REVOKE|CHECKPOINT|HAVING|RIGHT|CLOSE|HOLDLOCK|ROLLBACK|CLUSTERED|IDENTITY|ROWCOUNT|COALESCE|IDENTITY_INSERT|ROWGUIDCOL|COLLATE|IDENTITYCOL|RULE|COLUMN|IF|SAVE|COMMIT|IN|SCHEMA|COMPUTE|INDEX|SELECT|CONSTRAINT|INNER|SESSION_USER|CONTAINS|INSERT|SET|CONTAINSTABLE|INTERSECT|SETUSER|CONTINUE|INTO|SHUTDOWN|CONVERT|IS|SOME|CREATE|JOIN|STATISTICS|CROSS|KEY|SYSTEM_USER|CURRENT|KILL|TABLE|CURRENT_DATE|LEFT|TEXTSIZE|CURRENT_TIME|LIKE|THEN|CURRENT_TIMESTAMP|LINENO|TO|CURRENT_USER|LOAD|TOP|CURSOR|NATIONAL|TRAN|DATABASE|NOCHECK|TRANSACTION|DBCC|NONCLUSTERED|TRIGGER|DEALLOCATE|NOT|TRUNCATE|DECLARE|NULL|TSEQUAL|DEFAULT|NULLIF|UNION|DELETE|OF|UNIQUE|DENY|OFF|UPDATE|DESC|OFFSETS|UPDATETEXT|DISK|ON|USE|DISTINCT|OPEN|USER|DISTRIBUTED|OPENDATASOURCE|VALUES|DOUBLE|OPENQUERY|VARYING|DROP|OPENROWSET|VIEW|DUMMY|OPENXML|WAITFOR|DUMP|OPTION|WHEN|ELSE|OR|WHERE|END|ORDER|WHILE|ERRLVL|OUTER|WITH|ESCAPE|OVER|WRITETEXT)\\b", Pattern.CASE_INSENSITIVE);


    public static final Pattern LINK = android.util.Patterns.WEB_URL;

}
