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
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.vmihalachi.turboeditor.R;
import com.vmihalachi.turboeditor.util.MimeTypes;

import org.apache.commons.io.FilenameUtils;

import java.util.Arrays;
import java.util.LinkedList;

public class AdapterDetailedList extends
        ArrayAdapter<AdapterDetailedList.FileDetail> {

    // Layout Inflater
    private final LayoutInflater inflater;

    // List of file details
    private final LinkedList<FileDetail> fileDetails;

    public static class ViewHolder {

        // Name of the file
        public TextView nameLabel;

        // Size of the file
        public TextView sizeLabel;

        // Date of the file
        public TextView dateLabel;

        // Icon of the file
        public ImageView icon;
    }

    public AdapterDetailedList(final Context context,
                               final LinkedList<FileDetail> fileDetails,
                               final boolean isRoot) {
        super(context, R.layout.item_file_list, fileDetails);
        this.fileDetails = fileDetails;
        this.inflater = LayoutInflater.from(context);
        if (!isRoot) {
            this.fileDetails.addFirst(new FileDetail("..", context.getString(R.string.folder), ""));
        } else {
            this.fileDetails.addFirst(new FileDetail(context.getString(R.string.home), context.getString(R.string.folder), ""));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View getView(final int position,
                        View convertView, final ViewGroup parent) {
        if (convertView == null) {
            convertView = this.inflater
                    .inflate(R.layout.item_file_list,
                            null);
            final ViewHolder hold = new ViewHolder();
            hold.nameLabel = (TextView) convertView.findViewById(android.R.id.title);
            hold.sizeLabel = (TextView) convertView.findViewById(android.R.id.text1);
            hold.dateLabel = (TextView) convertView.findViewById(android.R.id.text2);
            hold.icon = (ImageView) convertView.findViewById(android.R.id.icon);
            convertView.setTag(hold);
            final FileDetail fileDetail = fileDetails.get(position);
            final String fileName = fileDetail.getName();
            setIcon(hold, fileDetail);
            hold.nameLabel.setText(fileName);
            hold.sizeLabel.setText(fileDetail.getSize());
            hold.dateLabel.setText(fileDetail.getDateModified());
        } else {
            final ViewHolder hold = ((ViewHolder) convertView.getTag());
            final FileDetail fileDetail = fileDetails.get(position);
            final String fileName = fileDetail.getName();
            setIcon(hold, fileDetail);
            hold.nameLabel.setText(fileName);
            hold.sizeLabel.setText(fileDetail.getSize());
            hold.dateLabel.setText(fileDetail.getDateModified());
        }
        return convertView;
    }

    private void setIcon(final ViewHolder viewHolder, final FileDetail fileDetail) {
        final String fileName = fileDetail.getName();
        final String ext = FilenameUtils.getExtension(fileName);
        if (fileDetail.isFolder()) {
            viewHolder.icon.setImageResource(R.color.file_folder);
        } else if (Arrays.asList(MimeTypes.MIME_HTML).contains(ext) || ext.endsWith("html")) {
            viewHolder.icon.setImageResource(R.color.file_html);
        } else if (Arrays.asList(MimeTypes.MIME_CODE).contains(ext)
                || fileName.endsWith("css")
                || fileName.endsWith("js")) {
            viewHolder.icon.setImageResource(R.color.file_code);
        } else if (Arrays.asList(MimeTypes.MIME_ARCHIVE).contains(ext)) {
            viewHolder.icon.setImageResource(R.color.file_archive);
        } else if (Arrays.asList(MimeTypes.MIME_MUSIC)
                .contains(ext)) {
            viewHolder.icon.setImageResource(R.color.file_media_music);
        } else if (Arrays.asList(MimeTypes.MIME_PICTURE).contains(ext)) {
            viewHolder.icon.setImageResource(R.color.file_media_picture);
        } else if (Arrays.asList(MimeTypes.MIME_VIDEO).contains(ext)) {
            viewHolder.icon.setImageResource(R.color.file_media_video);
        } else {
            viewHolder.icon.setImageResource(R.color.file_text);
        }
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
