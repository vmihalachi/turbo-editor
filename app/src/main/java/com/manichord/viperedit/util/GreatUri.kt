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

package com.manichord.viperedit.util

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable

import java.io.File
import java.util.Objects

/**
 * Created by mac on 19/03/15.
 */
data class GreatUri(var uri: Uri?, var filePath: String?, var fileName: String?) : Parcelable {

    val parentFolder: String?
        get() = File(filePath!!).parent

    val isReadable: Boolean
        get() = File(filePath!!).canRead()

    val isWritable: Boolean
        get() = File(filePath!!).canWrite()

    constructor(parcel: Parcel) : this(
            parcel.readParcelable(Uri::class.java.classLoader),
            parcel.readString(),
            parcel.readString())

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val greatUri = other as GreatUri?
        return uri == greatUri!!.uri
    }

    override fun hashCode(): Int {
        return Objects.hash(uri)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(uri, flags)
        parcel.writeString(filePath)
        parcel.writeString(fileName)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<GreatUri> {
        override fun createFromParcel(parcel: Parcel): GreatUri {
            return GreatUri(parcel)
        }

        override fun newArray(size: Int): Array<GreatUri?> {
            return arrayOfNulls(size)
        }
    }
}
