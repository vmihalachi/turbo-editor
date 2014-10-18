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

package sharedcode.turboeditor.preferences;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import de.greenrobot.event.EventBus;
import sharedcode.turboeditor.R;
import sharedcode.turboeditor.fragment.SeekbarDialog;
import sharedcode.turboeditor.util.AnimationUtils;
import sharedcode.turboeditor.util.ProCheckUtils;
import sharedcode.turboeditor.util.ViewUtils;
import sharedcode.turboeditor.views.DialogHelper;

import static sharedcode.turboeditor.util.EventBusEvents.APreferenceValueWasChanged;
import static sharedcode.turboeditor.util.EventBusEvents.APreferenceValueWasChanged.Type.FONT_SIZE;
import static sharedcode.turboeditor.util.EventBusEvents.APreferenceValueWasChanged.Type.LINE_NUMERS;
import static sharedcode.turboeditor.util.EventBusEvents.APreferenceValueWasChanged.Type.MONOSPACE;
import static sharedcode.turboeditor.util.EventBusEvents.APreferenceValueWasChanged.Type.READ_ONLY;
import static sharedcode.turboeditor.util.EventBusEvents.APreferenceValueWasChanged.Type.SYNTAX;
import static sharedcode.turboeditor.util.EventBusEvents.APreferenceValueWasChanged.Type.WRAP_CONTENT;

public class SettingsFragment extends Fragment implements SeekbarDialog.ISeekbarDialog {

    // Editor Variables
    private boolean sLineNumbers;
    private boolean sColorSyntax;
    private boolean sWrapContent;
    private boolean sUseMonospace;
    private boolean sReadOnly;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sUseMonospace = PreferenceHelper.getUseMonospace(getActivity());
        sColorSyntax = PreferenceHelper.getSyntaxHiglight(getActivity());
        sWrapContent = PreferenceHelper.getWrapContent(getActivity());
        sLineNumbers = PreferenceHelper.getLineNumbers(getActivity());
        sReadOnly = PreferenceHelper.getReadOnly(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Our custom layout
        View rootView = inflater.inflate(R.layout.fragment_settings, container, false);
        final CheckBox switchLineNumbers, switchSyntax, switchWrapContent, switchMonospace, switchReadOnly;
        switchLineNumbers = (CheckBox) rootView.findViewById(R.id.switch_line_numbers);
        switchSyntax = (CheckBox) rootView.findViewById(R.id.switch_syntax);
        switchWrapContent = (CheckBox) rootView.findViewById(R.id.switch_wrap_content);
        switchMonospace = (CheckBox) rootView.findViewById(R.id.switch_monospace);
        switchReadOnly = (CheckBox) rootView.findViewById(R.id.switch_read_only);

        switchLineNumbers.setChecked(sLineNumbers);
        switchSyntax.setChecked(sColorSyntax);
        switchWrapContent.setChecked(sWrapContent);
        switchMonospace.setChecked(sUseMonospace);
        switchReadOnly.setChecked(sReadOnly);

        TextView fontSizeView, donateView, extraOptionsView;
        fontSizeView = (TextView) rootView.findViewById(R.id.drawer_button_font_size);
        extraOptionsView = (TextView) rootView.findViewById(R.id.drawer_button_extra_options);
        donateView = (TextView) rootView.findViewById(R.id.drawer_button_go_pro);

        if(ProCheckUtils.isPro(getActivity(), false))
            ViewUtils.setVisible(donateView, false);

        switchLineNumbers.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                sLineNumbers = isChecked;
                PreferenceHelper.setLineNumbers(getActivity(), isChecked);
                EventBus.getDefault().post(new APreferenceValueWasChanged(LINE_NUMERS));
            }
        });

        switchSyntax.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                sColorSyntax = isChecked;
                PreferenceHelper.setSyntaxHiglight(getActivity(), isChecked);
                EventBus.getDefault().post(new APreferenceValueWasChanged(SYNTAX));

            }
        });

        switchWrapContent.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                sWrapContent = isChecked;
                PreferenceHelper.setWrapContent(getActivity(), isChecked);
                EventBus.getDefault().post(new APreferenceValueWasChanged(WRAP_CONTENT));
            }
        });

        switchMonospace.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                sUseMonospace = isChecked;
                PreferenceHelper.setUseMonospace(getActivity(), isChecked);
                EventBus.getDefault().post(new APreferenceValueWasChanged(MONOSPACE));

            }
        });

        switchReadOnly.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                sReadOnly = isChecked;
                PreferenceHelper.setReadOnly(getActivity(), isChecked);
                EventBus.getDefault().post(new APreferenceValueWasChanged(READ_ONLY));
            }
        });

        fontSizeView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int fontMax = 36;
                int fontCurrent = //(int) (mEditor.getTextSize() / scaledDensity);
                        //fontMax / 2;
                        PreferenceHelper.getFontSize(getActivity());
                SeekbarDialog dialogFrag = SeekbarDialog.newInstance(SeekbarDialog.Actions
                        .FontSize, 1, fontCurrent, fontMax);
                dialogFrag.setTargetFragment(SettingsFragment.this, 0);
                dialogFrag.show(getFragmentManager().beginTransaction(), "dialog");
            }
        });

        extraOptionsView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AnimationUtils.startActivityWithScale(getActivity(), new Intent(getActivity(),
                        ExtraSettingsActivity.class), false, 0, v);
            }
        });

        donateView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogHelper.showDonateDialog(getActivity());
            }
        });

        return rootView;
    }

    @Override
    public void onSeekbarDialogDismissed(SeekbarDialog.Actions action, int value) {
        PreferenceHelper.setFontSize(getActivity(), value);
        EventBus.getDefault().post(new APreferenceValueWasChanged(FONT_SIZE));

    }
}
