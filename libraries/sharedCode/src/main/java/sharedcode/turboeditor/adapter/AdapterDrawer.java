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

package sharedcode.turboeditor.adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.List;

import sharedcode.turboeditor.R;

public class AdapterDrawer extends
        ArrayAdapter<File> {

    private final Callbacks callbacks;
    // Layout Inflater
    private final LayoutInflater inflater;
    // List of file details
    private final List<File> files;
    private String selectedPath = "";

    public AdapterDrawer(Context context,
                         List<File> files,
                         Callbacks callbacks) {
        super(context, R.layout.item_file_list, files);
        this.files = files;
        this.inflater = LayoutInflater.from(context);
        this.callbacks = callbacks;
    }


    @Override
    public View getView(final int position,
                        View convertView, final ViewGroup parent) {
        if (convertView == null) {
            convertView = this.inflater
                    .inflate(R.layout.item_drawer_list,
                            parent, false);
            final ViewHolder hold = new ViewHolder();
            hold.nameLabel = (TextView) convertView.findViewById(android.R.id.text1);
            hold.cancelButton = (ImageView) convertView.findViewById(R.id.button_remove_from_list);
            convertView.setTag(hold);

            final String fileName = files.get(position).getName();
            hold.nameLabel.setText(fileName);
            hold.cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean closeOpenedFile = TextUtils.equals(selectedPath, files.get(position).getAbsolutePath());
                    callbacks.CancelItem(position, closeOpenedFile);
                    if (closeOpenedFile)
                        selectedPath = "";

                }
            });

            if (TextUtils.equals(selectedPath, files.get(position).getAbsolutePath())) {
                hold.nameLabel.setTypeface(null, Typeface.BOLD);
            } else {
                hold.nameLabel.setTypeface(null, Typeface.NORMAL);
            }

        } else {
            final ViewHolder hold = ((ViewHolder) convertView.getTag());
            final String fileName = files.get(position).getName();
            hold.nameLabel.setText(fileName);
            hold.cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean closeOpenedFile = TextUtils.equals(selectedPath, files.get(position).getAbsolutePath());
                    callbacks.CancelItem(position, closeOpenedFile);
                    if (closeOpenedFile)
                        selectedPath = "";
                }
            });

            if (TextUtils.equals(selectedPath, files.get(position).getAbsolutePath())) {
                hold.nameLabel.setTypeface(null, Typeface.BOLD);
            } else {
                hold.nameLabel.setTypeface(null, Typeface.NORMAL);
            }
        }
        return convertView;
    }

    public void selectView(String selectedPath) {
        //callbacks.ItemSelected(selectedPath);
        this.selectedPath = selectedPath;
        notifyDataSetChanged();
    }

    public interface Callbacks {
        void CancelItem(int position, boolean andCloseOpenedFile);

        //void ItemSelected(String path);
    }

    public static class ViewHolder {

        // Name of the file
        public TextView nameLabel;

        public ImageView cancelButton;
    }
}
