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

package sharedcode.turboeditor.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import sharedcode.turboeditor.R;
import sharedcode.turboeditor.util.AppInfoHelper;
import sharedcode.turboeditor.util.Constants;
import sharedcode.turboeditor.util.ProCheckUtils;

public class PreferenceAbout extends Activity {

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        setTheme(R.style.AppTheme_Dark);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        TextView changeLogText = (TextView) findViewById(R.id.changelog_text);
        TextView proVersionText = (TextView) findViewById(R.id.pro_version_text);

        try {
            changeLogText.setText(String.format(getString(R.string.app_version), getPackageManager().getPackageInfo(getPackageName(), 0).versionName));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        proVersionText.setVisibility(ProCheckUtils.isPro(getBaseContext()) ? View.GONE : View.VISIBLE);

    }

    @Override
    protected void onDestroy() {
        //checkout.stop();
        super.onDestroy();
    }

    public void OpenPlayStore(View view) {
        try {
            if (Constants.FOR_AMAZON) {
                String url = "amzn://apps/android?p=com.maskyn.fileeditor";
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } else {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=pub:Maskyn"))
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            }
        } catch (Exception e) {
        }
    }

    public void GoToProVersion(View view) {
        try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.maskyn.fileeditorpro"))
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception e) {
        }
    }

    public void OpenGithub(View view) {
        String url = "https://github.com/vmihalachi/TurboEditor";
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    public void SendFeedback(View view) {
        String url = "http://forum.xda-developers.com/android/apps-games/app-turbo-editor-text-editor-t2832016";
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    public void SendMail(View view) {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("message/rfc822");
        i.putExtra(Intent.EXTRA_EMAIL, new String[]{"maskyngames@gmail.com"});
        i.putExtra(Intent.EXTRA_SUBJECT, AppInfoHelper.getApplicationName(getBaseContext()) + " " + AppInfoHelper.getCurrentVersion(getBaseContext()));
        i.putExtra(Intent.EXTRA_TEXT, "");
        try {
            startActivity(Intent.createChooser(i, getString(R.string.nome_app_turbo_editor)));
        } catch (android.content.ActivityNotFoundException ex) {
        }
    }

    public void OpenTranslatePage(View view) {
        String url = "http://crowdin.net/project/turbo-client";
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    public void OpenGooglePlusCommunity(View view) {
        String url = "https://plus.google.com/u/0/communities/111974095419108178946";
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    /*void setupClickablePreferences() {
        final Preference email = findPreference("aboutactivity_authoremail"),
                changelog = findPreference("aboutactivity_changelog"),
                open_source_licenses = findPreference("aboutactivity_open_source_licenses"),
                market = findPreference("aboutactivity_authormarket");
        if (email != null) {
            email.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(final Preference preference) {

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

                    return false;
                }
            });
        }
    }*/
}
