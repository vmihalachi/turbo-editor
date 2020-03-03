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

package com.manichord.viperedit.dialogfragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.NumberPicker;

import com.manichord.viperedit.R;
import com.manichord.viperedit.views.DialogHelper;

// ...
public class NumberPickerDialog extends DialogFragment {

    private NumberPicker mSeekBar;

    public static NumberPickerDialog newInstance(final Actions action) {
        return NumberPickerDialog.newInstance(action, 0, 50, 100);
    }

    public static NumberPickerDialog newInstance(final Actions action, final int min, final int current, final int max) {
        final NumberPickerDialog f = new NumberPickerDialog();
        final Bundle args = new Bundle();
        args.putSerializable("action", action);
        args.putInt("min", min);
        args.putInt("current", current);
        args.putInt("max", max);
        f.setArguments(args);
        return f;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        Actions action = (Actions) getArguments().getSerializable("action");
        int title;
        switch (action){
            case FontSize:
                title = R.string.font_size;
                break;
            case SelectPage:
                title = R.string.goto_page;
                break;
            case GoToLine:
                title = R.string.goto_line;
                break;
            default:
                title = R.string.nome_app_turbo_editor;
                break;
        }

        View view = new DialogHelper.Builder(getActivity())
                .setTitle(title)
                .setView(R.layout.dialog_fragment_seekbar)
                .createSkeletonView();

        this.mSeekBar = (NumberPicker) view.findViewById(android.R.id.input);
        this.mSeekBar.setMaxValue(getArguments().getInt("max"));
        this.mSeekBar.setMinValue(getArguments().getInt("min"));
        this.mSeekBar.setValue(getArguments().getInt("current"));
        return new AlertDialog.Builder(getActivity())
                //.setTitle(title)
                .setView(view)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                returnData();
                            }
                        }
                )
                .setNegativeButton(android.R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        }
                )
                .create();
    }

    void returnData() {
        INumberPickerDialog target = (INumberPickerDialog) getTargetFragment();
        if (target == null) {
            target = (INumberPickerDialog) getActivity();
        }
        target.onNumberPickerDialogDismissed(
                (Actions) getArguments().getSerializable("action"),
                mSeekBar.getValue()
        );
        this.dismiss();
    }

    public enum Actions {
        FontSize, SelectPage, GoToLine
    }

    public interface INumberPickerDialog {
        void onNumberPickerDialogDismissed(Actions action, int value);
    }
}