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

package com.manichord.viperedit.dialogfragment

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.WindowManager
import android.widget.EditText

import androidx.fragment.app.DialogFragment
import com.manichord.viperedit.R
import com.manichord.viperedit.home.MainActivity
import com.manichord.viperedit.preferences.PreferenceHelper
import com.manichord.viperedit.util.GreatUri
import com.manichord.viperedit.views.DialogHelper

import java.io.File
import java.io.IOException


// ...
@SuppressLint("ValidFragment")
class NewFileDetailsDialog(
    var currentUri: GreatUri,
    var fileText: String,
    var fileEncoding: String) : DialogFragment() {

    private var mName: EditText? = null
    private var mFolder: EditText? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val view = DialogHelper.Builder(activity)
                .setTitle(R.string.save_as)
                .setView(R.layout.dialog_fragment_new_file_details)
                .createSkeletonView()

        this.mName = view.findViewById(android.R.id.text1)
        this.mFolder = view.findViewById(android.R.id.text2)

        val noName = TextUtils.isEmpty(currentUri.fileName)
        val noPath = TextUtils.isEmpty(currentUri.filePath)

        if (noName) {
            this.mName!!.setText(".txt")
        } else {
            this.mName!!.setText(currentUri.fileName)
        }
        if (noPath) {
            this.mFolder!!.setText(PreferenceHelper.getWorkingFolder(activity))
        } else {
            this.mFolder!!.setText(currentUri.parentFolder)
        }

        // Show soft keyboard automatically
        this.mName!!.requestFocus()
        this.mName!!.setSelection(0)
        activity!!.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)

        return AlertDialog.Builder(activity)
                .setView(view)
                .setPositiveButton(android.R.string.ok
                ) { dialog, which ->

                    if (!mName!!.text.toString().isEmpty() && !mFolder!!.text.toString().isEmpty()) {

                        val file = File(mFolder!!.text.toString(), mName!!.text.toString())
                        try {
                            file.parentFile!!.mkdirs()
                            file.createNewFile()
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }

                        val newUri = GreatUri(Uri.fromFile(file), file.absolutePath, file.name)
                        if (activity != null) {
                            (activity as MainActivity).startSavingFile(newUri, fileText, fileEncoding)
                        }
                    }
                }
                .setNegativeButton(android.R.string.cancel
                ) { dialog, which ->

                }
                .create()
    }

}