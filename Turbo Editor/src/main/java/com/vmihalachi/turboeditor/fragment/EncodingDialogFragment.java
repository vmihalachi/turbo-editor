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
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.vmihalachi.turboeditor.R;

// ...
public class EncodingDialogFragment extends DialogFragment implements TextView.OnEditorActionListener {

    private EditText mEditText;

    public static EncodingDialogFragment newInstance(final String hint) {
        final EncodingDialogFragment f = new EncodingDialogFragment();
        // Supply num input as an argument.
        final Bundle args = new Bundle();
        args.putString("hint", hint);
        f.setArguments(args);
        return f;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {

        final Dialog dialog = getDialog();
        final String title = getString(R.string.codifica);
        dialog.setTitle(title);

        final View view = inflater.inflate(R.layout.dialog_fragment_edittext, container);
        this.mEditText = (EditText) view.findViewById(android.R.id.edit);

        // Show soft keyboard automatically
        this.mEditText.setText(getArguments().getString("hint"));
        this.mEditText.requestFocus();
        dialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        this.mEditText.setOnEditorActionListener(this);

        final Button button = (Button) view.findViewById(android.R.id.button1);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                returnData();
            }
        });

        return view;
    }

    void returnData() {
        EditDialogListener target = (EditDialogListener) getTargetFragment();
        if (target == null) {
            target = (EditDialogListener) getActivity();
        }
        target.onFinishEditDialog(this.mEditText.getText().toString(), getArguments().getString("hint"));
        this.dismiss();
    }

    @Override
    public boolean onEditorAction(final TextView v, final int actionId, final KeyEvent event) {
        if (EditorInfo.IME_ACTION_DONE == actionId) {
            returnData();
            return true;
        }
        return false;
    }

    public interface EditDialogListener {
        void onFinishEditDialog(String inputText, String hint);
    }
}


