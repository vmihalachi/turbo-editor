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

package sharedcode.turboeditor.dialogfragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;

import it.gmariotti.changelibs.library.view.ChangeLogListView;
import sharedcode.turboeditor.R;
import sharedcode.turboeditor.util.Build;

public class ChangelogDialog extends DialogFragment {

    public static void showChangeLogDialog(FragmentManager fragmentManager) {
        ChangelogDialog changelogDialog = new ChangelogDialog();
        FragmentTransaction ft = fragmentManager.beginTransaction();
        Fragment prev = fragmentManager.findFragmentByTag("changelogdemo_dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        changelogDialog.show(ft, "changelogdemo_dialog");
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        LayoutInflater layoutInflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ChangeLogListView chgList = (ChangeLogListView) layoutInflater.inflate(R.layout.demo_changelog_fragment_dialogstandard, null);

        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.changelog)
                .setView(chgList)
                .setNegativeButton(android.R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialog.dismiss();
                            }
                        }
                )
                .setPositiveButton(R.string.vota, new DialogInterface.OnClickListener() {
                    /**
                     * {@inheritDoc}
                     */
                    @Override
                    public void onClick(final DialogInterface dialog, final int which) {
                        try {
                            if (Build.FOR_AMAZON) {
                                String url = "amzn://apps/android?p=com.maskyn.fileeditor";
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                            } else {
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.maskyn.fileeditor"))
                                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                            }
                        } catch (Exception e) {
                        }
                    }
                })
                .create();

    }
}
