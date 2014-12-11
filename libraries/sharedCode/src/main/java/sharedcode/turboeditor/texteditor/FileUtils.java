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

import org.mozilla.universalchardet.UniversalDetector;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

public class FileUtils {
    public static String getDetectedEncoding(File file) {
        InputStream is = null;
        String encoding = null;
        try {
            is = new FileInputStream(file);
            UniversalDetector detector = new UniversalDetector(null);
            byte[] buf = new byte[4096];
            int nread;
            while ((nread = is.read(buf)) > 0 && !detector.isDone()) {
                detector.handleData(buf, 0, nread);
            }
            detector.dataEnd();
            encoding = detector.getDetectedCharset();
        } catch (IOException e) {
            // nothing to do
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (encoding == null) {
                return Charset.defaultCharset().name();
            }
        }
        return encoding;
    }
}
