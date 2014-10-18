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
import android.view.View;
import android.widget.ListView;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.Date;

import sharedcode.turboeditor.R;
import sharedcode.turboeditor.adapter.AdapterTwoItem;

// ...
public class FileInfoDialog extends DialogFragment {

    public static FileInfoDialog newInstance(String filePath) {
        final FileInfoDialog f = new FileInfoDialog();
        final Bundle args = new Bundle();
        args.putString("filePath", filePath);
        f.setArguments(args);
        return f;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        final View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_fragment_file_info, null);
        ListView list = (ListView) view.findViewById(android.R.id.list);

        File file = new File(getArguments().getString("filePath"));

        // Get the last modification information.
        Long lastModified = file.lastModified();

        // Create a new date object and pass last modified information
        // to the date object.
        Date date = new Date(lastModified);

        String[] lines1 = {
                getString(R.string.name),
                getString(R.string.folder),
                getString(R.string.size),
                getString(R.string.modification_date)
        };
        String[] lines2 = {
                file.getName(),
                file.getParent(),
                FileUtils.byteCountToDisplaySize(file.length()),
                date.toString()
        };

        list.setAdapter(new AdapterTwoItem(getActivity(), lines1, lines2));

        return new AlertDialog.Builder(getActivity())
                //.setTitle(title)
                .setView(view)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        }
                )
                .create();
    }
}