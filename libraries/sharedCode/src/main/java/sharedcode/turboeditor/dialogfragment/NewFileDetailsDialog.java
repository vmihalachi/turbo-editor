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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;

import org.apache.commons.io.FileUtils;

import java.io.File;

import sharedcode.turboeditor.R;
import sharedcode.turboeditor.activity.MainActivity;
import sharedcode.turboeditor.preferences.PreferenceHelper;
import sharedcode.turboeditor.task.SaveFileTask;
import sharedcode.turboeditor.util.ViewUtils;
import sharedcode.turboeditor.views.DialogHelper;

// ...
public class NewFileDetailsDialog extends DialogFragment {

    private EditText mName;
    private EditText mFolder;
    private CheckBox mDeleteCurrentFile;

    public static NewFileDetailsDialog newInstance(String currentPath, String currentName, String fileText, String fileEncoding) {
        final NewFileDetailsDialog f = new NewFileDetailsDialog();
        final Bundle args = new Bundle();
        args.putString("path", currentPath);
        args.putString("name", currentName);
        args.putString("fileText", fileText);
        args.putString("fileEncoding", fileEncoding);
        f.setArguments(args);
        return f;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        View view = new DialogHelper.Builder(getActivity())
                .setTitle(R.string.save_as)
                .setView(R.layout.dialog_fragment_new_file_details)
                .createSkeletonView();

        this.mName = (EditText) view.findViewById(android.R.id.text1);
        this.mFolder = (EditText) view.findViewById(android.R.id.text2);

        boolean noName = TextUtils.isEmpty(getArguments().getString("name"));
        boolean noPath = TextUtils.isEmpty(getArguments().getString("path"));

        if (noName) {
            this.mName.setText(".txt");
        } else {
            this.mName.setText(getArguments().getString("name"));
        }
        if (noPath) {
            this.mFolder.setText(PreferenceHelper.getWorkingFolder(getActivity()));
        } else {
            this.mFolder.setText(getArguments().getString("path"));
        }

        this.mDeleteCurrentFile = (CheckBox) view.findViewById(R.id.delete_current_file);
        ViewUtils.setVisible(mDeleteCurrentFile, !noName);

        // Show soft keyboard automatically
        this.mName.requestFocus();
        this.mName.setSelection(0);
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        return new AlertDialog.Builder(getActivity())
                .setView(view)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                if (mDeleteCurrentFile.isChecked()) {
                                    FileUtils.deleteQuietly(new File(getArguments().getString("path"), getArguments().getString("name")));
                                }

                                if (!mName.getText().toString().isEmpty() && !mFolder.getText().toString().isEmpty()) {
                                    File file = new File(mFolder.getText().toString(), mName.getText().toString());
                                    new SaveFileTask((MainActivity) getActivity(), file.getPath(), getArguments().getString("fileText"), getArguments().getString("fileEncoding")).execute();
                                    PreferenceHelper.setWorkingFolder(getActivity(), file.getParent());
                                }
                            }
                        }
                )
                .setNegativeButton(android.R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        }
                )
                .create();
    }

}