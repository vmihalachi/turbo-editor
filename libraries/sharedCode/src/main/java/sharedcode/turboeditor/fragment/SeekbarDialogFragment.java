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

package sharedcode.turboeditor.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.NumberPicker;

import sharedcode.turboeditor.R;

// ...
public class SeekbarDialogFragment extends DialogFragment {

    private NumberPicker mSeekBar;

    public static SeekbarDialogFragment newInstance(final Actions action) {
        return SeekbarDialogFragment.newInstance(action, 0, 50, 100);
    }

    public static SeekbarDialogFragment newInstance(final Actions action, final int min, final int current, final int max) {
        final SeekbarDialogFragment f = new SeekbarDialogFragment();
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

        final Actions action = (Actions) getArguments().getSerializable("action");

        final View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_fragment_seekbar, null);
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
        onSeekbarDialogDismissed target = (onSeekbarDialogDismissed) getTargetFragment();
        if (target == null) {
            target = (onSeekbarDialogDismissed) getActivity();
        }
        target.onSeekbarDialogDismissed(
                (Actions) getArguments().getSerializable("action"),
                mSeekBar.getValue()
        );
        this.dismiss();
    }

    public enum Actions {
        FileSize, SelectPage, GoToLine
    }

    public interface onSeekbarDialogDismissed {
        void onSeekbarDialogDismissed(Actions action, int value);
    }
}