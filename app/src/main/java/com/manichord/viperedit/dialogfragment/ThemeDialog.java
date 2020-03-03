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
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.manichord.viperedit.R;
import com.manichord.viperedit.preferences.PreferenceHelper;

public class ThemeDialog extends DialogFragment implements AdapterView.OnItemClickListener {

    private ListView list;

    public static ThemeDialog newInstance() {
        final ThemeDialog f = new ThemeDialog();
        return f;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        final View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_theme_list, null);
        list = (ListView) view.findViewById(android.R.id.list);

        String[] themes = {
                getString(R.string.theme_dark), getString(R.string.light_theme), getString(R.string.theme_black)
        };

        list.setAdapter(new ArrayAdapter<>(getActivity(), R.layout.item_single_choice, themes));
        list.setOnItemClickListener(this);

        int currentTheme = PreferenceHelper.getTheme(getActivity());

        for (int i = 0; i < themes.length; i++) {
            if (i == currentTheme)
                list.setItemChecked(i, true);
        }

        return new AlertDialog.Builder(getActivity())
                .setView(view)
                .create();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        DialogListener target = (DialogListener) getTargetFragment();
        if (target == null) {
            target = (DialogListener) getActivity();
        }
        target.onThemeSelected(position);
        this.dismiss();
    }

    public interface DialogListener {
        void onThemeSelected(int result);
    }
}
