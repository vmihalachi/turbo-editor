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

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.DialogFragment

import com.manichord.viperedit.R
import com.manichord.viperedit.util.FragmentArgumentDelegate
import com.manichord.viperedit.util.GreatUri
import com.manichord.viperedit.views.DialogHelper

class SaveFileDialog : DialogFragment() {

    private var uri: GreatUri by FragmentArgumentDelegate()
    private var text: String by FragmentArgumentDelegate()
    private var encoding: String by FragmentArgumentDelegate()
    private var openNewFileAfter: Boolean by FragmentArgumentDelegate()
    private var newUri: GreatUri by FragmentArgumentDelegate()

    private var saveDialogCallback: ISaveDialog? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)

        saveDialogCallback = context as ISaveDialog
    }

    override fun onDetach() {
        super.onDetach()

        saveDialogCallback = null
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val view = DialogHelper.Builder(activity)
                .setIcon(ResourcesCompat.getDrawable(resources, R.drawable.ic_action_save, activity?.theme))
                .setTitle(R.string.salva)
                .setMessage(String.format(getString(R.string.save_changes), uri.fileName))
                .createCommonView()

        return AlertDialog.Builder(activity)
                .setView(view)
                .setPositiveButton(R.string.salva
                ) { _, _ ->
                    if (uri.fileName?.isEmpty() == true) {
                        val dialogFrag = NewFileDetailsDialog(uri, text, encoding)
                        dialogFrag.show(fragmentManager!!, "dialog")
                    } else {
                        saveDialogCallback?.startSavingFile(uri, text, encoding)
                    }
                }
                .setNeutralButton(android.R.string.cancel, null)
                .setNegativeButton(R.string.no
                ) { _, _ ->
                    saveDialogCallback?.userDoesNotWantToSave(
                            openNewFileAfter, newUri
                    )
                }
                .create()
    }

    interface ISaveDialog {
        fun userDoesNotWantToSave(openNewFile: Boolean, newUri: GreatUri)

        fun startSavingFile(uri: GreatUri, text: String, encoding: String)
    }

    companion object {

        fun create(uri: GreatUri?, text: String?, encoding: String?, openNewFileAfter: Boolean = false,
                   newUri: GreatUri = GreatUri(Uri.EMPTY, "", "")) =

            SaveFileDialog().apply {
                if (uri != null)
                    this.uri = uri
                if (text != null)
                    this.text = text
                if (encoding != null)
                    this.encoding = encoding
                this.openNewFileAfter = openNewFileAfter
                this.newUri = newUri
            }
    }
}