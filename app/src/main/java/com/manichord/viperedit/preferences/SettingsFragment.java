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

package com.manichord.viperedit.preferences;

import android.Manifest;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.manichord.viperedit.R;
import com.manichord.viperedit.home.MainActivity;
import com.manichord.viperedit.dialogfragment.EncodingDialog;
import com.manichord.viperedit.dialogfragment.NumberPickerDialog;
import com.manichord.viperedit.dialogfragment.ThemeDialog;
import com.manichord.viperedit.util.Device;
import com.manichord.viperedit.util.ProCheckUtils;
import com.manichord.viperedit.util.ViewUtils;

public class SettingsFragment extends Fragment implements NumberPickerDialog.INumberPickerDialog, EncodingDialog.DialogListener, ThemeDialog.DialogListener {

    // Editor Variables
    private boolean sLineNumbers;
    private boolean sColorSyntax;
    private boolean sWrapContent;
    private boolean sUseMonospace;
    private boolean sReadOnly;
    private boolean sAccessoryView;
    private boolean sStorageAccessFramework;
    private boolean sSuggestions;
    private boolean sAutoSave;
    private boolean sIgnoreBackButton;
    private boolean sSplitText;
    private boolean sErrorReports;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context context = getActivity();
        sUseMonospace = PreferenceHelper.getUseMonospace(context);
        sColorSyntax = PreferenceHelper.getSyntaxHighlight(context);
        sWrapContent = PreferenceHelper.getWrapContent(context);
        sLineNumbers = PreferenceHelper.getLineNumbers(context);
        sReadOnly = PreferenceHelper.getReadOnly(context);
        sAccessoryView = PreferenceHelper.getUseAccessoryView(context);
        sStorageAccessFramework = PreferenceHelper.getUseStorageAccessFramework(context);
        sSuggestions = PreferenceHelper.getSuggestionActive(context);
        sAutoSave = PreferenceHelper.getAutoSave(context);
        sIgnoreBackButton = PreferenceHelper.getIgnoreBackButton(context);
        sSplitText = PreferenceHelper.getSplitText(context);
        sErrorReports = PreferenceHelper.getSendErrorReports(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Our custom layout
        final View rootView = inflater.inflate(R.layout.fragment_settings, container, false);
        final SwitchCompat swLineNumbers, swSyntax, swWrapContent, swMonospace, swReadOnly;
        final SwitchCompat swSuggestions, swAccessoryView, swStorageAccessFramework, swAutoSave, swIgnoreBackButton, swSplitText, swErrorReports;
        
        swLineNumbers = (SwitchCompat) rootView.findViewById(R.id.switch_line_numbers);
        swSyntax = (SwitchCompat) rootView.findViewById(R.id.switch_syntax);
        swWrapContent = (SwitchCompat) rootView.findViewById(R.id.switch_wrap_content);
        swMonospace = (SwitchCompat) rootView.findViewById(R.id.switch_monospace);
        swReadOnly = (SwitchCompat) rootView.findViewById(R.id.switch_read_only);

        swSuggestions = (SwitchCompat) rootView.findViewById(R.id.switch_suggestions_active);
        swAccessoryView = (SwitchCompat) rootView.findViewById(R.id.switch_accessory_view);
        swStorageAccessFramework = (SwitchCompat) rootView.findViewById(R.id.switch_storage_access_framework);
        swAutoSave = (SwitchCompat) rootView.findViewById(R.id.switch_auto_save);
        swIgnoreBackButton = (SwitchCompat) rootView.findViewById(R.id.switch_ignore_backbutton);
        swSplitText = (SwitchCompat) rootView.findViewById(R.id.switch_page_system);
        swErrorReports = (SwitchCompat) rootView.findViewById(R.id.switch_send_error_reports);

        swLineNumbers.setChecked(sLineNumbers);
        swSyntax.setChecked(sColorSyntax);
        swWrapContent.setChecked(sWrapContent);
        swMonospace.setChecked(sUseMonospace);
        swReadOnly.setChecked(sReadOnly);

        swSuggestions.setChecked(sSuggestions);
        swAccessoryView.setChecked(sAccessoryView);
        swStorageAccessFramework.setChecked(sStorageAccessFramework);
        swAutoSave.setChecked(sAutoSave);
        swIgnoreBackButton.setChecked(sIgnoreBackButton);
        swSplitText.setChecked(sSplitText);
        swErrorReports.setChecked(sErrorReports);

        TextView fontSizeView, encodingView, extraOptionsView, themeView, goPro;
        goPro = (TextView) rootView.findViewById(R.id.drawer_button_go_pro);
        fontSizeView = (TextView) rootView.findViewById(R.id.drawer_button_font_size);
        encodingView = (TextView) rootView.findViewById(R.id.drawer_button_encoding);
        extraOptionsView = (TextView) rootView.findViewById(R.id.drawer_button_extra_options);
        themeView = (TextView) rootView.findViewById(R.id.drawer_button_theme);

        ViewUtils.setVisible(goPro, !ProCheckUtils.isPro(getActivity()));
        ViewUtils.setVisible(swStorageAccessFramework, Device.hasKitKatApi());

        goPro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String appPackageName = "com.maskyn.fileeditorpro";
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                } catch (android.content.ActivityNotFoundException anfe) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                }
            }
        });

        swLineNumbers.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                PreferenceHelper.setLineNumbers(getActivity(), isChecked);
                ((MainActivity) getActivity()).aPreferenceValueWasChanged(PreferenceChangeType.LINE_NUMERS);
            }
        });

        swSyntax.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                sColorSyntax = isChecked;
                PreferenceHelper.setSyntaxHighlight(getActivity(), isChecked);
                ((MainActivity) getActivity()).aPreferenceValueWasChanged(PreferenceChangeType.SYNTAX);

            }
        });

        swWrapContent.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                PreferenceHelper.setWrapContent(getActivity(), isChecked);
                ((MainActivity) getActivity()).aPreferenceValueWasChanged(PreferenceChangeType.WRAP_CONTENT);
            }
        });

        swMonospace.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                sUseMonospace = isChecked;
                PreferenceHelper.setUseMonospace(getActivity(), isChecked);
                ((MainActivity) getActivity()).aPreferenceValueWasChanged(PreferenceChangeType.MONOSPACE);

            }
        });

        swReadOnly.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                PreferenceHelper.setReadOnly(getActivity(), isChecked);
                ((MainActivity) getActivity()).aPreferenceValueWasChanged(PreferenceChangeType.READ_ONLY);
            }
        });

        fontSizeView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int fontMax = 36;
                int fontCurrent = //(int) (mEditor.getTextSize() / scaledDensity);
                        //fontMax / 2;
                        PreferenceHelper.getFontSize(getActivity());
                NumberPickerDialog dialogFrag = NumberPickerDialog.newInstance(NumberPickerDialog
                        .Actions
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

        themeView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ThemeDialog dialogFrag = ThemeDialog.newInstance();
                dialogFrag.setTargetFragment(SettingsFragment.this, 0);
                dialogFrag.show(getFragmentManager().beginTransaction(), "dialog");
            }
        });

        swSuggestions.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                PreferenceHelper.setSuggestionsActive(getActivity(), isChecked);
                ((MainActivity) getActivity()).aPreferenceValueWasChanged(PreferenceChangeType.TEXT_SUGGESTIONS);
            }
        });

        swAccessoryView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                PreferenceHelper.setUseAccessoryView(getActivity(), isChecked);
                ((MainActivity) getActivity()).aPreferenceValueWasChanged(PreferenceChangeType.ACCESSORY_VIEW);
            }
        });

        swStorageAccessFramework.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    PreferenceHelper.setUseStorageAccessFramework(getActivity(), true);
                    return;
                }
                if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED) {
                    PreferenceHelper.setUseStorageAccessFramework(getActivity(), false);
                    return;
                }
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MainActivity.REQUEST_WRITE_STORAGE_PERMISSION);
            }
        });

        swAutoSave.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                PreferenceHelper.setAutoSave(getActivity(), isChecked);
            }
        });

        swIgnoreBackButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                PreferenceHelper.setIgnoreBackButton(getActivity(), isChecked);
            }
        });

        swSplitText.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                PreferenceHelper.setSplitText(getActivity(), isChecked);
            }
        });

        swErrorReports.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                PreferenceHelper.setSendErrorReport(getActivity(), isChecked);
            }
        });

        return rootView;
    }

    @Override
    public void onNumberPickerDialogDismissed(NumberPickerDialog.Actions action, int value) {
        PreferenceHelper.setFontSize(getActivity(), value);
        ((MainActivity) getActivity()).aPreferenceValueWasChanged(PreferenceChangeType.FONT_SIZE);

    }

    @Override
    public void onEncodingSelected(String result) {
        PreferenceHelper.setEncoding(getActivity(), result);
        ((MainActivity) getActivity()).aPreferenceValueWasChanged(PreferenceChangeType.ENCODING);
    }

    @Override
    public void onThemeSelected(int result) {
        PreferenceHelper.setTheme(getActivity(), result);
        ((MainActivity) getActivity()).aPreferenceValueWasChanged(PreferenceChangeType.THEME_CHANGE);
    }
}
