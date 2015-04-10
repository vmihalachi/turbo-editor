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

package sharedcode.turboeditor.dialogfragment;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import sharedcode.turboeditor.R;
import sharedcode.turboeditor.activity.MainActivity;
import sharedcode.turboeditor.task.SaveFileTask;
import sharedcode.turboeditor.util.GreatUri;
import sharedcode.turboeditor.views.DialogHelper;

@SuppressLint("ValidFragment")
public class SaveFileDialog extends DialogFragment {

    GreatUri uri;
    String text;
    String encoding;
    boolean openNewFileAfter;
    GreatUri newUri;

    @SuppressLint("ValidFragment")
    public SaveFileDialog(GreatUri uri, String text, String encoding) {
        this.uri = uri;
        this.text = text;
        this.encoding = encoding;
        this.openNewFileAfter = false;
        this.newUri = new GreatUri(Uri.EMPTY, "", "");
    }

    @SuppressLint("ValidFragment")
    public SaveFileDialog(GreatUri uri, String text, String encoding, boolean openNewFileAfter, GreatUri newUri) {
        this.uri = uri;
        this.text = text;
        this.encoding = encoding;
        this.openNewFileAfter = openNewFileAfter;
        this.newUri = newUri;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        View view = new DialogHelper.Builder(getActivity())
                .setIcon(getResources().getDrawable(R.drawable.ic_action_save))
                .setTitle(R.string.salva)
                .setMessage(String.format(getString(R.string.save_changes), uri.getFileName()))
                .createCommonView();

        return new AlertDialog.Builder(getActivity())
                .setView(view)
                .setPositiveButton(R.string.salva,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (uri.getFileName().isEmpty()) {
                                    NewFileDetailsDialog dialogFrag =
                                            new NewFileDetailsDialog(uri,text,
                                                    encoding);
                                    dialogFrag.show(getFragmentManager().beginTransaction(),
                                            "dialog");
                                } else {
                                    new SaveFileTask((MainActivity) getActivity(), uri, text,
                                            encoding, new SaveFileTask.SaveFileInterface() {
                                        @Override
                                        public void fileSaved(Boolean success) {
                                            if (getActivity() != null) {
                                                ((MainActivity) getActivity()).savedAFile(uri, true);
                                            }
                                        }
                                    }).execute();
                                }
                            }
                        }
                )
                .setNeutralButton(android.R.string.cancel, null)
                .setNegativeButton(R.string.no,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ISaveDialog target = (ISaveDialog) getTargetFragment();
                                if (target == null) {
                                    target = (ISaveDialog) getActivity();
                                }
                                target.userDoesntWantToSave(
                                        openNewFileAfter, newUri
                                );
                            }
                        }
                )
                .create();
    }

    public interface ISaveDialog {
        void userDoesntWantToSave(boolean openNewFile, GreatUri newUri);
    }
}