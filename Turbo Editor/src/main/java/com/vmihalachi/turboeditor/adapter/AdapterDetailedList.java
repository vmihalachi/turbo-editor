/*
 * Copyright (C) 2013 Vlad Mihalachi
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
 * along with Turbo Editor. If not, see <http://www.gnu.org/licenses/>.
 */

package com.vmihalachi.turboeditor.adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.vmihalachi.turboeditor.R;
import com.vmihalachi.turboeditor.helper.FileHelper;
import com.vmihalachi.turboeditor.util.MimeTypes;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

public class AdapterDetailedList extends
        ArrayAdapter<AdapterDetailedList.FileDetail> {

    // Layout Inflater
    final LayoutInflater inflater;

    // The Context to get drawables from resources
    private final Context context;

    // The list of names
    final LinkedList<FileDetail> fileDetails;

    // Change HashMap<Integer, Boolean>  to SparseBooleanArray
    private HashMap<String, Boolean> mSelection =
            new HashMap<String, Boolean>();

    private final int default_text_color;
    private final int highlight_text_color;

    public static class ViewHolder {

        // Text view with the name of the file
        public TextView label;

        // Text view with the name of the file
        public TextView sizeLabel;

        public TextView dateLabel;

        // The icon of the file
        public ImageView icon;
    }

    public AdapterDetailedList(final Context context,
                               final LinkedList<FileDetail> fileDetails,
                               final boolean isRoot) {
        // super
        super(context,
                R.layout.item_file_list,
                fileDetails);
        this.context = context;
        this.fileDetails = fileDetails;
        // Cache the LayoutInflate to avoid asking for a new one each time.
        this.inflater = LayoutInflater.from(context);
        this.default_text_color = context.getResources().getColor(android.R.color.primary_text_dark);
        this.highlight_text_color = context.getResources().getColor(android.R.color.holo_blue_dark);
        if (!isRoot) {
            this.fileDetails.addFirst(new FileDetail("..",
                    context.getString(R.string.folder), ""));
        } else {
            this.fileDetails.addFirst(new FileDetail(context.getString(R.string.home),
                    context.getString(R.string.folder),
                    ""));
        }
    }

    @Override
    public View getView(final int position,
                        View convertView, final ViewGroup parent) {
        if (convertView == null) {
            convertView = this.inflater
                    .inflate(R.layout.item_file_list,
                            null);
            final ViewHolder hold = new ViewHolder();
            hold.label = (TextView) convertView.findViewById(android.R.id.title);
            hold.sizeLabel = (TextView) convertView.findViewById(android.R.id.text1);
            hold.dateLabel = (TextView) convertView.findViewById(android.R.id.text2);
            hold.icon = (ImageView) convertView.findViewById(android.R.id.icon);
            convertView.setTag(hold);
            final FileDetail fileDetail = fileDetails.get(position);
            final String fileName = fileDetail.getName();
            setIcon(hold, fileDetail);
            hold.label
                    .setText(fileName);
            hold.sizeLabel
                    .setText(fileDetail.getSize());
            hold.dateLabel
                    .setText(fileDetail.getDateModified());
            if (isPositionChecked(fileName)) {
                hold.label
                        .setTextColor(this.highlight_text_color);
                hold.label
                        .setTypeface(null, Typeface.ITALIC);
            } else {
                hold.label
                        .setTextColor(this.default_text_color);
                hold.label
                        .setTypeface(null, Typeface.NORMAL);
            }
        } else {
            final ViewHolder hold = ((ViewHolder) convertView.getTag());
            final FileDetail fileDetail = fileDetails.get(position);
            final String fileName = fileDetail.getName();
            setIcon(hold, fileDetail);
            hold.label
                    .setText(fileName);
            hold.sizeLabel
                    .setText(fileDetail.getSize());
            hold.dateLabel
                    .setText(fileDetail.getDateModified());
            if (isPositionChecked(fileName)) {
                hold.label
                        .setTextColor(this.highlight_text_color);
                hold.label
                        .setTypeface(null, Typeface.ITALIC);
            } else {
                hold.label
                        .setTextColor(this.default_text_color);
                hold.label
                        .setTypeface(null, Typeface.NORMAL);
            }
        }
        return convertView;
    }

    private void setIcon(final ViewHolder viewHolder,
                         final FileDetail fileDetail) {
        final String fileName = fileDetail.getName();
        final String ext = FileHelper.getExtension(fileName);
        if (fileDetail.isFolder()) {
            viewHolder.icon
                    .setImageResource(R.color.file_folder);
        } else if (Arrays.asList(MimeTypes.MIME_HTML)
                .contains(ext) || ext.endsWith("html")) {
            viewHolder.icon
                    .setImageResource(R.color.file_html);
        } else if (Arrays.asList(MimeTypes.MIME_CODE)
                .contains(ext)
                || fileName.endsWith("css")
                || fileName.endsWith("js")) {
            viewHolder.icon
                    .setImageResource(R.color.file_code);
        } else if (Arrays.asList(MimeTypes.MIME_ARCHIVE).contains(ext)) {
            viewHolder.icon
                    .setImageResource(R.color.file_archive);
        } else if (Arrays.asList(MimeTypes.MIME_MUSIC)
                .contains(ext)) {
            viewHolder.icon
                    .setImageResource(R.color.file_media_music);
        } else if (Arrays.asList(MimeTypes.MIME_PICTURE).contains(ext)) {
            viewHolder.icon
                    .setImageResource(R.color.file_media_picture);
        } else if (Arrays.asList(MimeTypes.MIME_VIDEO)
                .contains(ext)) {
            viewHolder.icon
                    .setImageResource(R.color.file_media_video);
        } else {
            viewHolder.icon
                    .setImageResource(R.color.file_text);
        }
    }

    public void checkPosition(final String name) {
        if (isPositionChecked(name)) {
            removeSelection(name);
        } else {
            setNewSelection(name, true);
        }
    }

    void setNewSelection(final String name,
                         final boolean value) {
        this.mSelection.put(name, value);
        notifyDataSetChanged();
    }

    boolean isPositionChecked(final String name) {
        final Boolean result = this.mSelection.get(name);
        return (result == null) ? false : result;
    }

    public Set<String> getCurrentCheckedPosition() {
        return this.mSelection.keySet();
    }

    private void removeSelection(final String name) {
        this.mSelection.remove(name);
        notifyDataSetChanged();
    }

    public void clearSelection() {
        this.mSelection = new HashMap<String, Boolean>();
        notifyDataSetChanged();
    }

    public static class FileDetail {
        private String name;
        private String size;
        private String dateModified;
        private boolean isFolder;

        public FileDetail(String name, String size,
                          String dateModified) {
            this.name = name;
            this.size = size;
            this.dateModified = dateModified;
            if (TextUtils.isEmpty(dateModified)) {
                isFolder = true;
            } else {
                isFolder = false;
            }
        }

        public String getDateModified() {
            return dateModified;
        }

        public String getSize() {
            return size;
        }

        public String getName() {
            return name;
        }

        public boolean isFolder() {
            return isFolder;
        }
    }
}
