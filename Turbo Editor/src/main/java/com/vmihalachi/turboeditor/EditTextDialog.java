/*******************************************************************************
 * Copyright (c) 2012, 2013 Vlad Mihalachi
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.vmihalachi.turboeditor;

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

import com.example.strings.R;

// ...
public class EditTextDialog extends DialogFragment implements TextView.OnEditorActionListener {

    private EditText mEditText;

    public static EditTextDialog newInstance(final String hint) {
        final EditTextDialog f = new EditTextDialog();

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

        final View view = inflater.inflate(R.layout.edittext_dialog, container);
        this.mEditText = (EditText) view.findViewById(R.id.editText);

        // Show soft keyboard automatically
        this.mEditText.setText(getArguments().getString("hint"));
        this.mEditText.requestFocus();
        dialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        this.mEditText.setOnEditorActionListener(this);

        final Button button = (Button) view.findViewById(R.id.ok_button);
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
        target.onFinishEditDialog(this.mEditText.getText().toString(),
                (Actions) getArguments().getSerializable("action"), getArguments().getString("hint"));
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

    public enum Actions {
        NewRemoteFolder, NewRemoteFile, NewLocalFolder, Rename, Move, EditEncoding
    }

    public interface EditDialogListener {
        void onFinishEditDialog(String inputText, Actions action, String hint);
    }
}


