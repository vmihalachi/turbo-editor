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
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import sharedcode.turboeditor.R;
import sharedcode.turboeditor.preferences.PreferenceHelper;

public class LicensesActivity extends Activity implements AdapterView.OnItemClickListener {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        boolean light = PreferenceHelper.getLightTheme(this);
        if (light) {
            setTheme(R.style.AppTheme_Light);
        } else {
            setTheme(R.style.AppTheme_Dark);
        }
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_licenses);
        ListView listView = (ListView) findViewById(android.R.id.list);
        listView.setOnItemClickListener(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, getResources().getStringArray(R.array.open_source_libs));
        listView.setAdapter(adapter);
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String openSourceLib = ((TextView) view.findViewById(android.R.id.text1)).getText().toString();
        Intent browserIntent = null;
        switch (openSourceLib) {
            case "ChangeLog Library":
                browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/gabrielemariotti/changeloglib?source=c#license"));
                break;
            case "EventBus":
                browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/greenrobot/EventBus?source=c#license"));
                break;
            case "commons-io":
                browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://commons.apache.org/proper/commons-io/"));
                break;
            case "RootCommands":
                browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/dschuermann/superuser-commands"));
                break;
            case "Floating Action Button":
                browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/makovkastar/FloatingActionButton"));
                break;
        }
        if (browserIntent != null) {
            startActivity(browserIntent);
        }
    }
}
