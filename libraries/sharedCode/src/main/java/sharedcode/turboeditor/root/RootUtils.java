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

/**
 *   920 Text Editor is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   920 Text Editor is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with 920 Text Editor.  If not, see <http://www.gnu.org/licenses/>.
 */

package sharedcode.turboeditor.root;

import android.content.Context;

import org.apache.commons.io.FileUtils;
import org.sufficientlysecure.rootcommands.Shell;
import org.sufficientlysecure.rootcommands.Toolbox;

import java.io.BufferedReader;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;


public class RootUtils {

    public static void writeFile(Context context, String path, String text, String encoding, boolean isRoot) throws Exception {
        File file = new File(path);
        if (!file.canWrite() && isRoot) {
            File appFolder = context.getFilesDir();
            File tempFile = new File(appFolder, "temp.root.file");
            if (!tempFile.exists())
                tempFile.createNewFile();
            FileUtils.write(tempFile, text, encoding);
            Shell shell = Shell.startRootShell();
            Toolbox tb = new Toolbox(shell);
            String mount = tb.getFilePermissions(path);
            tb.copyFile(tempFile.getAbsolutePath(), path, true, false);
            tb.setFilePermissions(path, mount);
            tempFile.delete();
        } else {
            FileUtils.write(file,
                    text,
                    encoding);
        }
    }

    public static ArrayList<File> getFileList(String path, boolean runAtRoot) {
        ArrayList<File> filesList = new ArrayList<File>();
        if (runAtRoot == false) {
            File base = new File(path);
            File[] files = base.listFiles();
            if (files == null)
                return null;
            Collections.addAll(filesList, files);
        } else {
            BufferedReader reader = null; //errReader = null;
            try {
                LinuxShell.execute("ls -a " + LinuxShell.getCommandLineString(path));
                if (reader == null)
                    return null;

                File f;
                String line;
                while ((line = reader.readLine()) != null) {
                    f = new File(line.substring(2));
                    filesList.add(f);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return filesList;
    }

}