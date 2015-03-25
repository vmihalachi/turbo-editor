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

package sharedcode.turboeditor.util;

import android.net.Uri;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.File;

/**
 * Created by mac on 19/03/15.
 */
public class GreatUri {
    private Uri uri;
    private String filePath;
    private String fileName;

    public GreatUri(Uri uri, String filePath, String fileName) {
        this.uri = uri;
        this.filePath = filePath;
        this.fileName = fileName;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 31). // two randomly chosen prime numbers
                // if deriving: appendSuper(super.hashCode()).
                append(uri).
                toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof GreatUri))
            return false;
        if (obj == this)
            return true;

        GreatUri rhs = (GreatUri) obj;
        return new EqualsBuilder().
                // if deriving: appendSuper(super.equals(obj)).
                append(uri, rhs.uri).
                isEquals();
    }

    public Uri getUri() {
        return uri;
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getParentFolder() {
        return new File(filePath).getParent();
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public boolean isReadable() {
        return new File(getFilePath()).canRead();
    }

    public boolean isWritable() {
        return new File(getFilePath()).canWrite();
    }
}
