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

import android.net.Uri;
import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.widget.Toast;

import com.spazedog.lib.rootfw4.RootFW;
import com.spazedog.lib.rootfw4.Shell;
import com.spazedog.lib.rootfw4.utils.File;
import com.spazedog.lib.rootfw4.utils.Filesystem;

import org.apache.commons.io.FileUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import sharedcode.turboeditor.R;
import sharedcode.turboeditor.activity.MainActivity;
import sharedcode.turboeditor.util.Device;
import sharedcode.turboeditor.util.GreatUri;

public class SaveFileTask extends AsyncTask<Void, Void, Void> {

    private final MainActivity activity;
    private final GreatUri uri;
    private final String newContent;
    private final String encoding;
    private String message;
    private String positiveMessage, negativeMessage;
    private SaveFileInterface mCompletionHandler;

    public SaveFileTask(MainActivity activity, GreatUri uri, String newContent, String encoding, SaveFileInterface mCompletionHandler) {
        this.activity = activity;
        this.uri = uri;
        this.newContent = newContent;
        this.encoding = encoding;
        this.mCompletionHandler = mCompletionHandler;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        positiveMessage = String.format(activity.getString(R.string.file_saved_with_success), uri.getFileName());
        negativeMessage = activity.getString(R.string.err_occured);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Void doInBackground(final Void... voids) {

        boolean isRootNeeded = false;
        Shell.Result resultRoot = null;

        try {
            String filePath = uri.getFilePath();
            // if the uri has no path
            if (TextUtils.isEmpty(filePath)) {
               writeUri(uri.getUri(), newContent, encoding);
            } else {
                isRootNeeded = !uri.isWritable();
                if (isRootNeeded == false) {
                    if (Device.hasKitKatApi())
                        writeUri(uri.getUri(), newContent, encoding);
                    else {
                        FileUtils.write(new java.io.File(filePath),
                                newContent,
                                encoding);
                    }
                }
                // if we can read the file associated with the uri
                else {

                    if (RootFW.connect()) {
                        Filesystem.Disk systemPart = RootFW.getDisk(uri.getParentFolder());
                        systemPart.mount(new String[]{"rw"});

                        File file = RootFW.getFile(uri.getFilePath());
                        resultRoot = file.writeResult(newContent);

                        RootFW.disconnect();
                    }

                }

            }


            if (isRootNeeded) {
                if (resultRoot != null && resultRoot.wasSuccessful()) {
                    message = positiveMessage;
                }
                else if (resultRoot != null) {
                    message = negativeMessage + " command number: " + resultRoot.getCommandNumber() + " result code: " + resultRoot.getResultCode() + " error lines: " + resultRoot.getString();
                }
                else
                    message = negativeMessage;
            }
            else
                message = positiveMessage;
        } catch (Exception e) {
            e.printStackTrace();
            message = e.getMessage();
        }
        return null;
    }

    private void writeUri(Uri uri, String newContent, String encoding) throws IOException {
        ParcelFileDescriptor pfd = activity.getContentResolver().openFileDescriptor(uri, "w");
        FileOutputStream fileOutputStream = new FileOutputStream(pfd.getFileDescriptor());
        fileOutputStream.write(newContent.getBytes(Charset.forName(encoding)));
        fileOutputStream.close();
        pfd.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onPostExecute(final Void aVoid) {
        super.onPostExecute(aVoid);
        Toast.makeText(activity, message, Toast.LENGTH_LONG).show();

        /*android.content.ClipboardManager clipboard = (android.content.ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("Clip",message);
        clipboard.setPrimaryClip(clip);*/

        if (mCompletionHandler != null)
            mCompletionHandler.fileSaved(message.equals(positiveMessage));
    }

    public interface SaveFileInterface {
        void fileSaved(Boolean success);
    }
}