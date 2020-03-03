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

package com.manichord.viperedit.explorer;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MenuItemCompat;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;

import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Filter;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;

import com.manichord.viperedit.R;
import com.manichord.viperedit.adapter.AdapterDetailedList;
import com.manichord.viperedit.dialogfragment.EditTextDialog;
import com.manichord.viperedit.preferences.PreferenceHelper;
import com.manichord.viperedit.util.AlphanumComparator;
import com.manichord.viperedit.util.Build;
import com.manichord.viperedit.util.ThemeUtils;

public class SelectFileActivity extends AppCompatActivity implements SearchView.OnQueryTextListener, AdapterView.OnItemClickListener, EditTextDialog.EditDialogListener {

    private static final String ActionKey = "action";

    private String currentFolder;
    private ListView listView;
    private boolean wantAFile = true;
    private MenuItem mSearchViewMenuItem;
    private SearchView mSearchView;
    private Filter filter;

    public static Intent CreateSelectFileActivityIntnet(Context context, Action action) {
        Intent intent = new Intent(context, SelectFileActivity.class);
        intent.putExtra(ActionKey, action.value);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        currentFolder = PreferenceHelper.defaultFolder(this);

        ThemeUtils.setTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_file);

        Toolbar toolbar = findViewById(R.id.my_awesome_toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final Action action = Action.getActionByValue(getIntent().getExtras().getInt(ActionKey));
        wantAFile = action == Action.SelectFile;

        listView = findViewById(android.R.id.list);
        listView.setOnItemClickListener(this);
        listView.setTextFilterEnabled(true);

        String lastNavigatedPath = PreferenceHelper.getWorkingFolder(this);

        File file = new File(lastNavigatedPath);

        if (!file.exists()) {
            PreferenceHelper.setWorkingFolder(this, PreferenceHelper.defaultFolder(this));
            file = new File(PreferenceHelper.defaultFolder(this));
        }

        new UpdateList().execute(file.getAbsolutePath());
    }

    @Override
    public void onBackPressed() {
        if (currentFolder.isEmpty() || currentFolder.equals("/")) {
            finish();
        } else {
            File file = new File(currentFolder);
            String parentFolder = file.getParent();
            new UpdateList().execute(parentFolder);
        }
    }

    public boolean onQueryTextChange(String newText) {
        if (filter == null)
            return true;

        if (TextUtils.isEmpty(newText)) {
            filter.filter(null);
        } else {
            filter.filter(newText);
        }
        return true;
    }

    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    /**
     * Finish this Activity with a result code and URI of the selected file.
     *
     * @param file The file selected.
     */
    private void finishWithResult(File file) {
        if (file != null) {
            Uri uri = Uri.fromFile(file);
            setResult(RESULT_OK, new Intent().setData(uri));
            finish();
        } else {
            setResult(RESULT_CANCELED);
            finish();
        }
    }


    @Override
    public void onItemClick(AdapterView<?> parent,
                            View view, int position, long id) {
        final String name = ((TextView) view.findViewById(android.R.id.text1)).getText().toString();
        if (name.equals("..")) {
            if (currentFolder.equals("/")) {
                new UpdateList().execute(PreferenceHelper.getWorkingFolder(this));
            } else {
                File tempFile = new File(currentFolder);
                if (tempFile.isFile()) {
                    tempFile = tempFile.getParentFile()
                            .getParentFile();
                } else {
                    tempFile = tempFile.getParentFile();
                }
                new UpdateList().execute(tempFile.getAbsolutePath());
            }
            return;
        } else if (name.equals(getString(R.string.home))) {
            new UpdateList().execute(PreferenceHelper.getWorkingFolder(this));
            return;
        }

        final File selectedFile = new File(currentFolder, name);

        if (selectedFile.isFile() && wantAFile) {
            finishWithResult(selectedFile);
        } else if (selectedFile.isDirectory()) {
            new UpdateList().execute(selectedFile.getAbsolutePath());
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_select_file, menu);
        mSearchViewMenuItem = menu.findItem(R.id.im_search);
        mSearchView = (SearchView) MenuItemCompat.getActionView(mSearchViewMenuItem);
        mSearchView.setIconifiedByDefault(true);
        mSearchView.setOnQueryTextListener(this);
        mSearchView.setSubmitButtonEnabled(false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // menu items
        MenuItem imSetAsWorkingFolder = menu.findItem(R.id.im_set_as_working_folder);
        MenuItem imIsWorkingFolder = menu.findItem(R.id.im_is_working_folder);
        MenuItem imSelectFolder = menu.findItem(R.id.im_select_folder);
        if (imSetAsWorkingFolder != null) {
            // set the imSetAsWorkingFolder visible only if the two folder dont concide
            imSetAsWorkingFolder.setVisible(!currentFolder.equals(PreferenceHelper.getWorkingFolder(SelectFileActivity.this)));
        }
        if (imIsWorkingFolder != null) {
            // set visible is the other is invisible
            imIsWorkingFolder.setVisible(!imSetAsWorkingFolder.isVisible());
        }
        if (imSelectFolder != null) {
            imSelectFolder.setVisible(!wantAFile);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int i = item.getItemId();
        if (i == android.R.id.home) {
            finish();
            return true;
        } else if (i == R.id.im_add_file_or_folder) {
            ShowAddFileOrFolderPopup(findViewById(R.id.im_add_file_or_folder));
        } else if (i == R.id.im_set_as_working_folder) {
            PreferenceHelper.setWorkingFolder(SelectFileActivity.this, currentFolder);
            invalidateOptionsMenu();
            return true;
        } else if (i == R.id.im_is_working_folder) {
            Toast.makeText(getBaseContext(), R.string.is_the_working_folder, Toast.LENGTH_SHORT).show();
            return true;
        } else if (i == R.id.im_select_folder) {
            finishWithResult(new File(currentFolder));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onEdittextDialogEnded(final String inputText, final String hint, final EditTextDialog.Actions actions) {
        if (actions == EditTextDialog.Actions.NewFile && !TextUtils.isEmpty(inputText)) {
            File file = new File(currentFolder, inputText);
            try {
                file.createNewFile();
                finishWithResult(file);
            } catch (IOException e) {
                Toast.makeText(SelectFileActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else if (actions == EditTextDialog.Actions.NewFolder && !TextUtils.isEmpty(inputText)) {
            File file = new File(currentFolder, inputText);
            file.mkdirs();
            new UpdateList().execute(currentFolder);
        }
    }

    public enum Action {
        SelectFile(0), SelectFolder(1);

        private final int value;

        Action(int value) {
            this.value = value;
        }

        public static Action getActionByValue(int value) {
            for (Action action : Action.values()) {
                if (action.value == value) {
                    return action;
                }
            }

            return null;
        }
    }

    private void ShowAddFileOrFolderPopup(View anchor) {
        PopupMenu popup = new PopupMenu(SelectFileActivity.this, anchor);

        popup.getMenuInflater().inflate(R.menu.popup_new_file, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int i = item.getItemId();
            if (i == R.id.im_new_file) {
                EditTextDialog.newInstance(EditTextDialog.Actions.NewFile).show(getFragmentManager().beginTransaction(), "dialog");
                return true;
            } else if (i == R.id.im_new_folder) {
                EditTextDialog.newInstance(EditTextDialog.Actions.NewFolder).show(getFragmentManager().beginTransaction(), "dialog");
                return true;
            } else {
                return false;
            }
        });

        popup.show();
    }

    private class UpdateList extends AsyncTask<String, Void, LinkedList<AdapterDetailedList.FileDetail>> {

        String exceptionMessage;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (mSearchView != null) {
                mSearchView.setIconified(true);
                MenuItemCompat.collapseActionView(mSearchViewMenuItem);
                mSearchView.setQuery("", false);
            }

        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected LinkedList<AdapterDetailedList.FileDetail> doInBackground(final String... params) {
            try {

                final String path = params[0];
                if (TextUtils.isEmpty(path)) {
                    return null;
                }

                File tempFolder = new File(path);
                if (tempFolder.isFile()) {
                    tempFolder = tempFolder.getParentFile();
                }

                String[] unopenableExtensions = {"apk", "mp3", "mp4", "png", "jpg", "jpeg"};

                final LinkedList<AdapterDetailedList.FileDetail> fileDetails = new LinkedList<>();
                final LinkedList<AdapterDetailedList.FileDetail> folderDetails = new LinkedList<>();
                currentFolder = tempFolder.getAbsolutePath();

                if (!tempFolder.canRead()) {
                    throw new RuntimeException("cannot read temp folder");
                } else {
                    File[] files = tempFolder.listFiles();

                    Arrays.sort(files, getFileNameComparator());

                    if (files != null) {
                        for (final File f : files) {
                            if (f.isDirectory()) {
                                folderDetails.add(new AdapterDetailedList.FileDetail(f.getName(),
                                        getString(R.string.folder),
                                        ""));
                            } else if (f.isFile()
                                    && !FilenameUtils.isExtension(f.getName().toLowerCase(), unopenableExtensions)
                                    && FileUtils.sizeOf(f) <= Build.MAX_FILE_SIZE * FileUtils.ONE_KB) {
                                final long fileSize = f.length();
                                SimpleDateFormat format = new SimpleDateFormat("MMM dd, yyyy  hh:mm a");
                                String date = format.format(f.lastModified());
                                fileDetails.add(new AdapterDetailedList.FileDetail(f.getName(),
                                        FileUtils.byteCountToDisplaySize(fileSize), date));
                            }
                        }
                    }
                }

                folderDetails.addAll(fileDetails);
                return folderDetails;
            } catch (Exception e) {
                exceptionMessage = e.getMessage();
                return null;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void onPostExecute(final LinkedList<AdapterDetailedList.FileDetail> names) {
            if (names != null) {
                boolean isRoot = currentFolder.equals("/");
                AdapterDetailedList mAdapter = new AdapterDetailedList(getBaseContext(), names, isRoot);
                listView.setAdapter(mAdapter);
                filter = mAdapter.getFilter();
            }
            if (exceptionMessage != null) {
                Toast.makeText(SelectFileActivity.this, exceptionMessage, Toast.LENGTH_SHORT).show();
            }
            invalidateOptionsMenu();
            super.onPostExecute(names);
        }

        public final Comparator<File> getFileNameComparator() {
            return new AlphanumComparator() {
                /**
                 * {@inheritDoc}
                 */
                @Override
                public String getTheString(Object obj) {
                    return ((File) obj).getName()
                            .toLowerCase();
                }
            };
        }
    }
}
