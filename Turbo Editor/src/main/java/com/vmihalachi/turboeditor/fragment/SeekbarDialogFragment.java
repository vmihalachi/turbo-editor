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

package com.vmihalachi.turboeditor.fragment;

import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import com.vmihalachi.turboeditor.R;

// ...
public class SeekbarDialogFragment extends DialogFragment {

    SeekBar mSeekBar;

    public static SeekbarDialogFragment newInstance(final Actions action) {
        return SeekbarDialogFragment.newInstance(action, 50, 100);
    }

    public static SeekbarDialogFragment newInstance(final Actions action, final int current, final int max) {
        final SeekbarDialogFragment f = new SeekbarDialogFragment();
        final Bundle args = new Bundle();
        args.putSerializable("action", action);
        args.putInt("current", current);
        args.putInt("max", max);
        f.setArguments(args);
        return f;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {

        final Dialog dialog = getDialog();
        final Actions action = (Actions) getArguments().getSerializable("action");
        final String title;
        switch (action) {
            case FileSize:
                title = getString(R.string.text_size);
                break;
            default:
                title = null;
                break;
        }
        dialog.setTitle(title);

        final View view = inflater.inflate(R.layout.dialog_fragment_seekbar, container);
        this.mSeekBar = (SeekBar) view.findViewById(android.R.id.input);
        this.mSeekBar.setProgress(getArguments().getInt("current"));
        this.mSeekBar.setMax(getArguments().getInt("max"));

        view.findViewById(android.R.id.button1)
            .setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                returnData();
            }
        });
        return view;
    }

    void returnData(){
        try {
            ((onSeekbarDialogDismissed) getTargetFragment()).onSeekbarDialogDismissed(
                    (Actions) getArguments().getSerializable("action"),
                    mSeekBar.getProgress()
            );
        } catch (Exception e){
            try {
                ((onSeekbarDialogDismissed) getActivity()).onSeekbarDialogDismissed(
                        (Actions) getArguments().getSerializable("action"),
                        mSeekBar.getProgress()
                );
            } catch (Exception e2){
            }
        }
        this.dismiss();
    }

    public enum Actions {
        FileSize
    }

    public interface onSeekbarDialogDismissed {
        void onSeekbarDialogDismissed(Actions action, int value);
    }
}