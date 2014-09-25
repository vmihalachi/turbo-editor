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

import java.io.File;

public class EventBusEvents {
    public static class CannotOpenAFile {

    }

    public static class NewFileToOpen {

        private final File file;
        private final String fileText;

        public NewFileToOpen(File file) {
            this.file = file;
            this.fileText = "";
        }

        public NewFileToOpen(String fileText) {
            this.file = new File("");
            this.fileText = fileText;
        }

        public File getFile() {
            return file;
        }

        public String getFileText() {
            return fileText;
        }
    }

    public static class AFileIsSelected {

        private final String path;

        public AFileIsSelected(String path) {
            this.path = path;
        }

        public String getPath() {
            return path;
        }
    }

    public static class APreferenceValueWasChanged {
        private Type type;

        public APreferenceValueWasChanged(Type type) {
            this.type = type;
        }

        public Type getType() {
            return type;
        }

        public void setType(Type type) {
            this.type = type;
        }

        public enum Type {
            FONT_SIZE, ENCODING, SYNTAX, WRAP_CONTENT, MONOSPACE, LINE_NUMERS, THEME_CHANGE, TEXT_SUGGESTIONS, AUTO_SAVE, READ_ONLY
        }
    }

    public static class SaveAFile {
    }

    public static class SavedAFile {
        private final String path;

        public SavedAFile(String path) {
            this.path = path;
        }

        public String getPath() {
            return path;
        }
    }

    public static class ClosedAFile {
    }

    public static class InvalideTheMenu {
    }

}
