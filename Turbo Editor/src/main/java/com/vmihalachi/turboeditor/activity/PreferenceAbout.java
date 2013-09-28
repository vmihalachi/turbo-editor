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

package com.vmihalachi.turboeditor.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;

import com.vmihalachi.turboeditor.R;
import com.vmihalachi.turboeditor.fragment.ChangelogDialogFragment;
import com.vmihalachi.turboeditor.helper.AppInfoHelper;

public class PreferenceAbout extends PreferenceActivity {
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.about);
        setupClickablePreferences();
    }

    public void setupClickablePreferences() {
        final Preference email = findPreference("aboutactivity_authoremail"),
                changelog = findPreference("aboutactivity_changelog"),
                open_source_licenses = findPreference("aboutactivity_open_source_licenses"),
                market = findPreference("aboutactivity_authormarket");
        if (email != null) {
            email.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(final Preference preference) {
                    Intent i = new Intent(Intent.ACTION_SEND);
                    i.setType("message/rfc822");
                    i.putExtra(Intent.EXTRA_EMAIL, new String[]{"app.feedback.mail@gmail.com"});
                    i.putExtra(Intent.EXTRA_SUBJECT, AppInfoHelper.getApplicationName(getBaseContext()) + " " + AppInfoHelper.getCurrentVersion(getBaseContext()));
                    i.putExtra(Intent.EXTRA_TEXT, "");
                    try {
                        startActivity(Intent.createChooser(i, getString(R.string.aboutactivity_authoremail_summary)));
                    } catch (android.content.ActivityNotFoundException ex) {
                    }
                    return false;
                }
            });
        }
        if (changelog != null) {
            changelog.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(final Preference preference) {
                    ChangelogDialogFragment.showChangeLogDialog(getFragmentManager());
                    return false;
                }
            });
        }
        if (open_source_licenses != null) {
            open_source_licenses.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(final Preference preference) {
                    startActivity(new Intent(PreferenceAbout.this, LicensesActivity.class));
                    return false;
                }
            });
        }
        if (market != null) {
            market.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(final Preference preference) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=pub:Vlad+Mihalachi"))
                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    } catch (Exception e) {
                    }
                    return false;
                }
            });
        }
    }
}
