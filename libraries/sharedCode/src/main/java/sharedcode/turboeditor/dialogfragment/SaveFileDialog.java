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
import android.view.View;

import org.apache.commons.io.FilenameUtils;

import java.io.File;

import sharedcode.turboeditor.R;
import sharedcode.turboeditor.activity.MainActivity;
import sharedcode.turboeditor.task.SaveFileTask;
import sharedcode.turboeditor.views.DialogHelper;

public class SaveFileDialog extends DialogFragment {

    public static SaveFileDialog newInstance(String filePath, String text, String encoding) {
        return newInstance(filePath, text, encoding, false, "");
    }

    public static SaveFileDialog newInstance(String filePath, String text, String encoding, boolean openNewFileAfter, String pathOfNewFile) {
        SaveFileDialog frag = new SaveFileDialog();
        Bundle args = new Bundle();
        args.putString("filePath", filePath);
        args.putString("text", text);
        args.putString("encoding", encoding);
        args.putBoolean("openNewFileAfter", openNewFileAfter);
        args.putString("pathOfNewFile", pathOfNewFile);
        frag.setArguments(args);
        return frag;
    }



    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final String filePath = getArguments().getString("filePath");
        final String text = getArguments().getString("text");
        final String encoding = getArguments().getString("encoding");
        final String fileName = FilenameUtils.getName(filePath);
        final File file = new File(filePath);

        View view = new DialogHelper.Builder(getActivity())
                .setIcon(getResources().getDrawable(R.drawable.ic_action_save))
                .setTitle(R.string.salva)
                .setMessage(String.format(getString(R.string.save_changes), fileName))
                .createCommonView();

        return new AlertDialog.Builder(getActivity())
                .setView(view)
                .setPositiveButton(R.string.salva,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (!fileName.isEmpty())
                                    new SaveFileTask((MainActivity) getActivity(), filePath, text,
                                            encoding).execute();
                                else {
                                    NewFileDetailsDialog dialogFrag =
                                            NewFileDetailsDialog.newInstance("","",text,
                                                    encoding);
                                    dialogFrag.show(getFragmentManager().beginTransaction(),
                                            "dialog");
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
                                        getArguments().getBoolean("openNewFileAfter"),
                                        getArguments().getString("pathOfNewFile")
                                );
                            }
                        }
                )
                .create();
    }

    public interface ISaveDialog {
        void userDoesntWantToSave(boolean openNewFile, String pathOfNewFile);
    }
}