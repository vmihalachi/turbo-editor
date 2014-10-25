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
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import de.greenrobot.event.EventBus;
import sharedcode.turboeditor.R;
import sharedcode.turboeditor.fragment.EncodingDialog;
import sharedcode.turboeditor.fragment.SeekbarDialog;
import sharedcode.turboeditor.util.ProCheckUtils;
import sharedcode.turboeditor.util.ViewUtils;
import sharedcode.turboeditor.views.DialogHelper;

import static sharedcode.turboeditor.util.EventBusEvents.APreferenceValueWasChanged;
import static sharedcode.turboeditor.util.EventBusEvents.APreferenceValueWasChanged.Type.ENCODING;
import static sharedcode.turboeditor.util.EventBusEvents.APreferenceValueWasChanged.Type.FONT_SIZE;
import static sharedcode.turboeditor.util.EventBusEvents.APreferenceValueWasChanged.Type.LINE_NUMERS;
import static sharedcode.turboeditor.util.EventBusEvents.APreferenceValueWasChanged.Type.MONOSPACE;
import static sharedcode.turboeditor.util.EventBusEvents.APreferenceValueWasChanged.Type.READ_ONLY;
import static sharedcode.turboeditor.util.EventBusEvents.APreferenceValueWasChanged.Type.SYNTAX;
import static sharedcode.turboeditor.util.EventBusEvents.APreferenceValueWasChanged.Type.TEXT_SUGGESTIONS;
import static sharedcode.turboeditor.util.EventBusEvents.APreferenceValueWasChanged.Type.THEME_CHANGE;
import static sharedcode.turboeditor.util.EventBusEvents.APreferenceValueWasChanged.Type.WRAP_CONTENT;

public class SettingsFragment extends Fragment implements SeekbarDialog.ISeekbarDialog, EncodingDialog.DialogListener {

    // Editor Variables
    private boolean sLineNumbers;
    private boolean sColorSyntax;
    private boolean sWrapContent;
    private boolean sUseMonospace;
    private boolean sReadOnly;

    private boolean sLightTheme;
    private boolean sSuggestions;
    private boolean sAutoSave;
    private boolean sIgnoreBackButton;
    private boolean sSplitText;
    private boolean sErrorReports;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sUseMonospace = PreferenceHelper.getUseMonospace(getActivity());
        sColorSyntax = PreferenceHelper.getSyntaxHighlight(getActivity());
        sWrapContent = PreferenceHelper.getWrapContent(getActivity());
        sLineNumbers = PreferenceHelper.getLineNumbers(getActivity());
        sReadOnly = PreferenceHelper.getReadOnly(getActivity());

        sLightTheme = PreferenceHelper.getLightTheme(getActivity());
        sSuggestions = PreferenceHelper.getSuggestionActive(getActivity());
        sAutoSave = PreferenceHelper.getAutoSave(getActivity());
        sIgnoreBackButton = PreferenceHelper.getIgnoreBackButton(getActivity());
        sSplitText = PreferenceHelper.getSplitText(getActivity());
        sErrorReports = PreferenceHelper.getSendErrorReports(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Our custom layout
        final View rootView = inflater.inflate(R.layout.fragment_settings, container, false);
        final SwitchCompat swLineNumbers, swSyntax, swWrapContent, swMonospace, swReadOnly;
        final SwitchCompat swLightTheme, swSuggestions, swAutoSave, swIgnoreBackButton, swSplitText, swErrorReports;
        
        swLineNumbers = (SwitchCompat) rootView.findViewById(R.id.switch_line_numbers);
        swSyntax = (SwitchCompat) rootView.findViewById(R.id.switch_syntax);
        swWrapContent = (SwitchCompat) rootView.findViewById(R.id.switch_wrap_content);
        swMonospace = (SwitchCompat) rootView.findViewById(R.id.switch_monospace);
        swReadOnly = (SwitchCompat) rootView.findViewById(R.id.switch_read_only);

        swLightTheme = (SwitchCompat) rootView.findViewById(R.id.switch_light_theme);
        swSuggestions = (SwitchCompat) rootView.findViewById(R.id.switch_suggestions_active);
        swAutoSave = (SwitchCompat) rootView.findViewById(R.id.switch_auto_save);
        swIgnoreBackButton = (SwitchCompat) rootView.findViewById(R.id.switch_ignore_backbutton);
        swSplitText = (SwitchCompat) rootView.findViewById(R.id.switch_page_system);
        swErrorReports = (SwitchCompat) rootView.findViewById(R.id.switch_send_error_reports);

        swLineNumbers.setChecked(sLineNumbers);
        swSyntax.setChecked(sColorSyntax);
        swWrapContent.setChecked(sWrapContent);
        swMonospace.setChecked(sUseMonospace);
        swReadOnly.setChecked(sReadOnly);

        swLightTheme.setChecked(sLightTheme);
        swSuggestions.setChecked(sSuggestions);
        swAutoSave.setChecked(sAutoSave);
        swIgnoreBackButton.setChecked(sIgnoreBackButton);
        swSplitText.setChecked(sSplitText);
        swErrorReports.setChecked(sErrorReports);

        TextView fontSizeView, encodingView, donateView, extraOptionsView;
        fontSizeView = (TextView) rootView.findViewById(R.id.drawer_button_font_size);
        encodingView = (TextView) rootView.findViewById(R.id.drawer_button_encoding);
        extraOptionsView = (TextView) rootView.findViewById(R.id.drawer_button_extra_options);
        donateView = (TextView) rootView.findViewById(R.id.drawer_button_go_pro);

        if(ProCheckUtils.isPro(getActivity(), false))
            ViewUtils.setVisible(donateView, false);

        swLineNumbers.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                PreferenceHelper.setLineNumbers(getActivity(), isChecked);
                EventBus.getDefault().post(new APreferenceValueWasChanged(LINE_NUMERS));
            }
        });

        swSyntax.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                sColorSyntax = isChecked;
                PreferenceHelper.setSyntaxHighlight(getActivity(), isChecked);
                EventBus.getDefault().post(new APreferenceValueWasChanged(SYNTAX));

            }
        });

        swWrapContent.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                PreferenceHelper.setWrapContent(getActivity(), isChecked);
                EventBus.getDefault().post(new APreferenceValueWasChanged(WRAP_CONTENT));
            }
        });

        swMonospace.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                sUseMonospace = isChecked;
                PreferenceHelper.setUseMonospace(getActivity(), isChecked);
                EventBus.getDefault().post(new APreferenceValueWasChanged(MONOSPACE));

            }
        });

        swReadOnly.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
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

        encodingView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EncodingDialog dialogFrag = EncodingDialog.newInstance();
                dialogFrag.setTargetFragment(SettingsFragment.this, 0);
                dialogFrag.show(getFragmentManager().beginTransaction(), "dialog");
            }
        });

        extraOptionsView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View otherOptions = rootView.findViewById(R.id.other_options);
                boolean isVisible = otherOptions.getVisibility() == View.VISIBLE;
                ViewUtils.setVisible(otherOptions, !isVisible);
            }
        });

        donateView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogHelper.showDonateDialog(getActivity());
            }
        });

        swLightTheme.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                PreferenceHelper.setLightTheme(getActivity(), isChecked);
                EventBus.getDefault().post(new APreferenceValueWasChanged(THEME_CHANGE));
            }
        });

        swSuggestions.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                PreferenceHelper.setSuggestionsActive(getActivity(), isChecked);
                EventBus.getDefault().post(new APreferenceValueWasChanged(TEXT_SUGGESTIONS));
            }
        });

        return rootView;
    }

    @Override
    public void onSeekbarDialogDismissed(SeekbarDialog.Actions action, int value) {
        PreferenceHelper.setFontSize(getActivity(), value);
        EventBus.getDefault().post(new APreferenceValueWasChanged(FONT_SIZE));

    }

    @Override
    public void onEncodingSelected(String result) {
        PreferenceHelper.setEncoding(getActivity(), result);
        EventBus.getDefault().post(new APreferenceValueWasChanged(ENCODING));
    }
}
