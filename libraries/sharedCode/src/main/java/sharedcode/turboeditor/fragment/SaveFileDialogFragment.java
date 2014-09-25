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

package sharedcode.turboeditor.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import sharedcode.turboeditor.R;
import sharedcode.turboeditor.preferences.SettingsFragment;
import sharedcode.turboeditor.util.SaveFileTask;

import org.apache.commons.io.FilenameUtils;

import java.io.File;

public class SaveFileDialogFragment extends DialogFragment {

    public static SaveFileDialogFragment newInstance(String filePath, String text) {
        SaveFileDialogFragment frag = new SaveFileDialogFragment();
        Bundle args = new Bundle();
        args.putString("filePath", filePath);
        args.putString("text", text);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final String filePath = getArguments().getString("filePath");
        final String text = getArguments().getString("text");
        final String fileName = FilenameUtils.getName(filePath);
        final File file = new File(filePath);

        return new AlertDialog.Builder(getActivity())
                .setMessage(String.format(getString(R.string.save_changes), fileName))
                .setPositiveButton(android.R.string.yes,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if(!fileName.isEmpty())
                                    new SaveFileTask(getActivity(), filePath, text, SettingsFragment.sCurrentEncoding).execute();
                                else {
                                    NewFileDetailsDialogFragment dialogFrag = NewFileDetailsDialogFragment.newInstance(text, SettingsFragment.sCurrentEncoding);
                                    dialogFrag.show(getFragmentManager().beginTransaction(), "dialog");
                                }
                            }
                        }
                )
                .setNegativeButton(android.R.string.no,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        }
                )
                .create();
    }

    public static enum Action {
        SaveAFile
    }
}