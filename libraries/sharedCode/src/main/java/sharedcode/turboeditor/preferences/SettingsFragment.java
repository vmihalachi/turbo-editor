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
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import sharedcode.turboeditor.R;
import sharedcode.turboeditor.fragment.EncodingDialogFragment;
import sharedcode.turboeditor.fragment.SeekbarDialogFragment;

import de.greenrobot.event.EventBus;
import sharedcode.turboeditor.util.ProCheckUtils;

import static sharedcode.turboeditor.util.EventBusEvents.APreferenceValueWasChanged;
import static sharedcode.turboeditor.util.EventBusEvents.APreferenceValueWasChanged.Type.AUTO_SAVE;
import static sharedcode.turboeditor.util.EventBusEvents.APreferenceValueWasChanged.Type.ENCODING;
import static sharedcode.turboeditor.util.EventBusEvents.APreferenceValueWasChanged.Type.FONT_SIZE;
import static sharedcode.turboeditor.util.EventBusEvents.APreferenceValueWasChanged.Type.LINE_NUMERS;
import static sharedcode.turboeditor.util.EventBusEvents.APreferenceValueWasChanged.Type.MONOSPACE;
import static sharedcode.turboeditor.util.EventBusEvents.APreferenceValueWasChanged.Type.READ_ONLY;
import static sharedcode.turboeditor.util.EventBusEvents.APreferenceValueWasChanged.Type.SYNTAX;
import static sharedcode.turboeditor.util.EventBusEvents.APreferenceValueWasChanged.Type.TEXT_SUGGESTIONS;
import static sharedcode.turboeditor.util.EventBusEvents.APreferenceValueWasChanged.Type.THEME_CHANGE;
import static sharedcode.turboeditor.util.EventBusEvents.APreferenceValueWasChanged.Type.WRAP_CONTENT;

public class SettingsFragment extends Fragment implements EncodingDialogFragment.DialogListener, SeekbarDialogFragment.onSeekbarDialogDismissed {

    public static String sCurrentEncoding;
    // Editor Variables
    public static boolean sLineNumbers;
    public static boolean sColorSyntax;
    public static boolean sWrapContent;
    public static int sFontSize;
    public static boolean sUseMonospace;
    public static boolean sLightTheme;
    public static boolean sSuggestionsActive;
    public static boolean sAutoSave;
    public static boolean sReadOnly;
    public static boolean sSendErrorReports;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SettingsFragment.sCurrentEncoding = PreferenceHelper.getEncoding(getActivity());
        SettingsFragment.sUseMonospace = PreferenceHelper.getUseMonospace(getActivity());
        SettingsFragment.sColorSyntax = PreferenceHelper.getSyntaxHiglight(getActivity());
        SettingsFragment.sWrapContent = PreferenceHelper.getWrapContent(getActivity());
        SettingsFragment.sLineNumbers = PreferenceHelper.getLineNumbers(getActivity());
        SettingsFragment.sFontSize = PreferenceHelper.getFontSize(getActivity());
        SettingsFragment.sSuggestionsActive = PreferenceHelper.getSuggestionActive(getActivity());
        SettingsFragment.sLightTheme = PreferenceHelper.getLightTheme(getActivity());
        SettingsFragment.sAutoSave = PreferenceHelper.getAutoSave(getActivity());
        SettingsFragment.sReadOnly = PreferenceHelper.getReadOnly(getActivity());
        SettingsFragment.sSendErrorReports = PreferenceHelper.getReadOnly(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Our custom layout
        View rootView = inflater.inflate(R.layout.fragment_settings, container, false);
        final CheckBox switchLineNumbers, switchSyntax, switchWrapContent, switchMonospace, switchLightTheme, switchSuggestionsActive, switchAutoSave, switchReadOnly, switchSendErrorReports;
        switchLineNumbers = (CheckBox) rootView.findViewById(R.id.switch_line_numbers);
        switchSyntax = (CheckBox) rootView.findViewById(R.id.switch_syntax);
        switchWrapContent = (CheckBox) rootView.findViewById(R.id.switch_wrap_content);
        switchMonospace = (CheckBox) rootView.findViewById(R.id.switch_monospace);
        switchLightTheme = (CheckBox) rootView.findViewById(R.id.switch_light_theme);
        switchSuggestionsActive = (CheckBox) rootView.findViewById(R.id.switch_suggestions_active);
        switchAutoSave = (CheckBox) rootView.findViewById(R.id.switch_auto_save);
        switchReadOnly = (CheckBox) rootView.findViewById(R.id.switch_read_only);
        switchSendErrorReports = (CheckBox) rootView.findViewById(R.id.switch_send_error_reports);

        switchLineNumbers.setChecked(sLineNumbers);
        switchSyntax.setChecked(sColorSyntax);
        switchWrapContent.setChecked(sWrapContent);
        switchMonospace.setChecked(sUseMonospace);
        switchLightTheme.setChecked(sLightTheme);
        switchSuggestionsActive.setChecked(sSuggestionsActive);
        switchAutoSave.setChecked(sAutoSave);
        switchReadOnly.setChecked(sReadOnly);


        TextView encodingView, fontSizeView, goProView;
        encodingView = (TextView) rootView.findViewById(R.id.drawer_button_encoding);
        fontSizeView = (TextView) rootView.findViewById(R.id.drawer_button_font_size);
        goProView = (TextView) rootView.findViewById(R.id.drawer_button_go_pro);

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

        switchLightTheme.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                sLightTheme = isChecked;
                PreferenceHelper.setLightTheme(getActivity(), isChecked);
                EventBus.getDefault().post(new APreferenceValueWasChanged(THEME_CHANGE));
            }
        });

        switchSuggestionsActive.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                sSuggestionsActive = isChecked;
                PreferenceHelper.setSuggestionActive(getActivity(), isChecked);
                EventBus.getDefault().post(new APreferenceValueWasChanged(TEXT_SUGGESTIONS));
            }
        });

        switchAutoSave.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                sAutoSave = isChecked;
                PreferenceHelper.setAutoSave(getActivity(), isChecked);
                EventBus.getDefault().post(new APreferenceValueWasChanged(AUTO_SAVE));
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

        switchSendErrorReports.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                sSendErrorReports = isChecked;
                PreferenceHelper.setSendErrorReports(getActivity(), isChecked);
            }
        });

        encodingView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EncodingDialogFragment dialogFrag = EncodingDialogFragment.newInstance();
                dialogFrag.setTargetFragment(SettingsFragment.this, 0);
                dialogFrag.show(getFragmentManager().beginTransaction(), "dialog");
            }
        });

        fontSizeView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int fontMax = 36;
                int fontCurrent = //(int) (mEditor.getTextSize() / scaledDensity);
                        //fontMax / 2;
                        PreferenceHelper.getFontSize(getActivity());
                SeekbarDialogFragment dialogFrag = SeekbarDialogFragment.newInstance(SeekbarDialogFragment.Actions.FileSize, 1, fontCurrent, fontMax);
                dialogFrag.setTargetFragment(SettingsFragment.this, 0);
                dialogFrag.show(getFragmentManager().beginTransaction(), "dialog");
            }
        });

        goProView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.maskyn.fileeditorpro"))
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                } catch (Exception e) {
                }
            }
        });

        goProView.setVisibility(ProCheckUtils.isPro(getActivity()) ? View.GONE : View.VISIBLE);
        return rootView;
    }


    @Override
    public void onEncodingSelected(String value) {
        PreferenceHelper.setEncoding(getActivity(), value);
        EventBus.getDefault().post(new APreferenceValueWasChanged(ENCODING));
    }

    @Override
    public void onSeekbarDialogDismissed(SeekbarDialogFragment.Actions action, int value) {
        sFontSize = value;
        PreferenceHelper.setFontSize(getActivity(), value);
        EventBus.getDefault().post(new APreferenceValueWasChanged(FONT_SIZE));

    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(Uri uri);
    }
}
