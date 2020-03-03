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

package com.manichord.viperedit.adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.LinkedList;

import com.manichord.viperedit.R;
import com.manichord.viperedit.util.GreatUri;

public class AdapterDrawer extends ArrayAdapter<GreatUri> {

    private final Callbacks callbacks;
    // Layout Inflater
    private final LayoutInflater inflater;
    // List of file details
    private final LinkedList<GreatUri> greatUris;
    private GreatUri selectedGreatUri = new GreatUri(Uri.EMPTY, "", "");

    public AdapterDrawer(Context context,
                         LinkedList<GreatUri> greatUris,
                         Callbacks callbacks) {
        super(context, R.layout.item_file_list, greatUris);
        this.greatUris = greatUris;
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

            final GreatUri greatUri = greatUris.get(position);
            final String fileName = greatUri.getFileName();
            hold.nameLabel.setText(fileName);
            hold.cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean closeOpenedFile = selectedGreatUri.getUri().equals(greatUri.getUri());
                    callbacks.CancelItem(position, closeOpenedFile);
                    if (closeOpenedFile)
                        selectPosition(new GreatUri(Uri.EMPTY, "", ""));

                }
            });

            if (selectedGreatUri.getUri().equals(greatUri.getUri())) {
                hold.nameLabel.setTypeface(null, Typeface.BOLD);
            } else {
                hold.nameLabel.setTypeface(null, Typeface.NORMAL);
            }

        } else {
            final ViewHolder hold = ((ViewHolder) convertView.getTag());
            final GreatUri greatUri = greatUris.get(position);
            final String fileName = greatUri.getFileName();
            hold.nameLabel.setText(fileName);
            hold.cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean closeOpenedFile = selectedGreatUri.getUri().equals(greatUri.getUri());
                    callbacks.CancelItem(position, closeOpenedFile);
                    if (closeOpenedFile)
                        selectPosition(new GreatUri(Uri.EMPTY, "", ""));

                }
            });

            if (selectedGreatUri.getUri().equals(greatUri.getUri())) {
                hold.nameLabel.setTypeface(null, Typeface.BOLD);
            } else {
                hold.nameLabel.setTypeface(null, Typeface.NORMAL);
            }

        }
        return convertView;
    }

    public void selectPosition(GreatUri greatUri) {
        //callbacks.ItemSelected(selectedPath);
        this.selectedGreatUri = greatUri;
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
