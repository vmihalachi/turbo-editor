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

package sharedcode.turboeditor.task;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import org.sufficientlysecure.rootcommands.Shell;
import org.sufficientlysecure.rootcommands.Toolbox;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

import de.greenrobot.event.EventBus;
import sharedcode.turboeditor.R;
import sharedcode.turboeditor.root.RootUtils;
import sharedcode.turboeditor.util.EventBusEvents;

public class SaveFileTask extends AsyncTask<Void, Void, Void> {

    private final Context context;
    private final String filePath;
    private final String text;
    private final String encoding;
    private File file;
    private String message;
    private String positiveMessage;

    public SaveFileTask(Context context, String filePath, String text, String encoding) {
        this.context = context;
        this.filePath = filePath;
        this.text = text;
        this.encoding = encoding;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        file = new File(filePath);
        positiveMessage = String.format(context.getString(R.string.file_saved_with_success), file.getName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Void doInBackground(final Void... voids) {

        try {

            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }

            boolean isRoot = false;
            if (!file.canWrite()) {
                try {
                    Shell shell = null;
                    shell = Shell.startRootShell();
                    Toolbox tb = new Toolbox(shell);
                    isRoot = tb.isRootAccessGiven();
                } catch (IOException | TimeoutException e) {
                    e.printStackTrace();
                    isRoot = false;
                }
            }

            RootUtils.writeFile(context, file.getAbsolutePath(), text, encoding, isRoot);

            message = positiveMessage;
        } catch (Exception e) {
            message = e.getMessage();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onPostExecute(final Void aVoid) {
        super.onPostExecute(aVoid);
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        if (message.equals(positiveMessage))
            EventBus.getDefault().post(new EventBusEvents.SavedAFile(filePath));
    }
}