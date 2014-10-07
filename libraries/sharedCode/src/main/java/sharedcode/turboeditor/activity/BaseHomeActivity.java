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

import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.ActivityOptionsCompat;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import org.apache.commons.io.FilenameUtils;
import org.sufficientlysecure.rootcommands.Shell;
import org.sufficientlysecure.rootcommands.Toolbox;

import java.io.File;

import de.greenrobot.event.EventBus;
import sharedcode.turboeditor.R;
import sharedcode.turboeditor.fragment.ChangelogDialogFragment;
import sharedcode.turboeditor.fragment.EditorFragment;
import sharedcode.turboeditor.fragment.NoFileOpenedFragment;
import sharedcode.turboeditor.preferences.PreferenceHelper;
import sharedcode.turboeditor.util.AppInfoHelper;
import sharedcode.turboeditor.util.EventBusEvents;
import sharedcode.turboeditor.util.ProCheckUtils;
import sharedcode.turboeditor.util.ThemeHelper;
import sharedcode.turboeditor.views.CustomDrawerLayout;

public abstract class BaseHomeActivity extends Activity {

    private static final int SELECT_FILE_CODE = 121;
    private EditText editor;

    /*
    * This class provides a handy way to tie together the functionality of
    * {@link DrawerLayout} and the framework <code>ActionBar</code> to implement the recommended
    * design for navigation drawers.
    */
    private ActionBarDrawerToggle mDrawerToggle;
    /*
    * The Drawer Layout
    */
    private CustomDrawerLayout mDrawerLayout;

    //region Activity facts
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // set the windows background
        ThemeHelper.setWindowsBackground(this);
        // super!!
        super.onCreate(savedInstanceState);
        // setup the layout
        setContentView(R.layout.activity_home);
        // setup the navigation drawer
        setupNavigationDrawer();
        // Replace fragment
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_editor, new NoFileOpenedFragment())
                .commit();
        /* First Time we open this activity */
        if (savedInstanceState == null) {
            // Open
            mDrawerLayout.openDrawer(Gravity.START);
            // Set the default title
            getActionBar().setTitle(getString(R.string.nome_app_turbo_editor));
        }
        // parse the intent
        parseIntent(getIntent());
        // show a dialog with the changelog
        showChangeLog();
    }


    @Override
    protected final void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }


    @Override
    public void onResume() {
        super.onResume();
        // Register the Event Bus for events
        EventBus.getDefault().registerSticky(this);
    }


    @Override
    public void onPause() {
        super.onPause();
        // Unregister the Event Bus
        EventBus.getDefault().unregister(this);
    }


    @Override
    protected void onDestroy() {
        try {
            closeKeyBoard();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {

        // if we should ignore the back button
        if(PreferenceHelper.getIgnoreBackButton(this))
            return;


        boolean fileOpened = getFragmentManager().findFragmentById(R.id.fragment_editor) instanceof EditorFragment;
        if (mDrawerLayout.isDrawerOpen(Gravity.START) && fileOpened) {
            mDrawerLayout.closeDrawer(Gravity.START);
        } else if (mDrawerLayout.isDrawerOpen(Gravity.END) && fileOpened) {
            mDrawerLayout.closeDrawer(Gravity.END);
        } else if (fileOpened) {

            // remove editor fragment
            getFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_editor, new NoFileOpenedFragment())
                    .commit();
            // Set the default title
            getActionBar().setTitle(getString(R.string.nome_app_turbo_editor));

            EventBus.getDefault().post(new EventBusEvents.ClosedAFile());

            mDrawerLayout.openDrawer(Gravity.START);
            mDrawerLayout.closeDrawer(Gravity.END);
        } else {
            displayInterstitial();
            super.onBackPressed();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            onBackPressed();
            return true;
        }
        else if(keyCode == KeyEvent.KEYCODE_MENU){
            return false;
        }
        else {
            if (editor == null)
                editor = (EditText) findViewById(R.id.editor);
            // this will happen on first key pressed on hard-keyboard only. Once myInputField
            // gets the focus again, it will automatically receive further key presses.

            try {
                if (editor != null && !editor.hasFocus()) {
                    editor.requestFocus();
                    editor.onKeyDown(keyCode, event);
                    return true;
                }
            } catch (NullPointerException ex) {

            }
        }


        return false;
    }

    @Override
    public final void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == SELECT_FILE_CODE) {
            String path = data.getStringExtra("path");
            File file = new File(path);
            if (file.isFile() && file.exists()) {
                EventBus.getDefault().postSticky(new EventBusEvents.NewFileToOpen(new File(path)));
            } else if (file.isDirectory()) {

            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        /* If we clicked on the Navigation Drawer Menu item */
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            mDrawerLayout.closeDrawer(Gravity.RIGHT);
            return true;
        } else
            return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        parseIntent(intent);
    }
    //endregion

    //region Calls from the layout
    public void OpenFile(View view) {
        Intent subActivity = new Intent(BaseHomeActivity.this, SelectFileActivity.class);
        subActivity.putExtra("action", SelectFileActivity.Actions.SelectFile);
        Bundle scaleBundle = ActivityOptionsCompat.makeScaleUpAnimation(
                view, 0, 0, view.getWidth(), view.getHeight()).toBundle();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            startActivityForResult(subActivity, SELECT_FILE_CODE, scaleBundle);
        else
            startActivityForResult(subActivity, SELECT_FILE_CODE);
    }

    public void CreateFile(View view) {
        onEvent(new EventBusEvents.NewFileToOpen(""));
        onEvent(new EventBusEvents.AFileIsSelected("")); // simulate click on the list
    }

    public void OpenInfo(View view) {
        Intent subActivity = new Intent(BaseHomeActivity.this, PreferenceAbout.class);
        Bundle scaleBundle = ActivityOptionsCompat.makeScaleUpAnimation(
                view, 0, 0, view.getWidth(), view.getHeight()).toBundle();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            startActivity(subActivity, scaleBundle);
        else
            startActivity(subActivity);
    }

    public void OpenSettings(View view) {
        mDrawerLayout.closeDrawer(Gravity.START);
        mDrawerLayout.openDrawer(Gravity.END);
    }
    //endregion

    //region Eventbus
    public void onEvent(final EventBusEvents.NewFileToOpen event) {

        new AsyncTask<Void, Void, Void>() {

            File file;
            String message;
            String fileText;
            String encoding;
            ProgressDialog progressDialog;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                // Close the drawer
                mDrawerLayout.closeDrawer(Gravity.START);
                file = event.getFile();
                message = "";
                progressDialog = new ProgressDialog(BaseHomeActivity.this);
                progressDialog.setMessage(getString(R.string.please_wait));
                progressDialog.show();

            }

            @Override
            protected Void doInBackground(Void... params) {
                try {
                    boolean isRoot = false;

                    if (!file.exists()) {
                        fileText = event.getFileText();
                        return null;
                    }

                    if (!file.canRead()) {
                        Shell shell = null;
                        shell = Shell.startRootShell();
                        Toolbox tb = new Toolbox(shell);
                        isRoot = tb.isRootAccessGiven();
                    }

                    if (isRoot) {
                        File tempFile = new File(getFilesDir(), "temp.root.file");
                        if (!tempFile.exists())
                            tempFile.createNewFile();
                        Shell shell = Shell.startRootShell();
                        Toolbox tb = new Toolbox(shell);
                        tb.copyFile(event.getFile().getAbsolutePath(), tempFile.getAbsolutePath(), false, false);
                        file = new File(tempFile.getAbsolutePath());
                    }

                    boolean autoencoding = PreferenceHelper.getAutoEncoding(BaseHomeActivity.this);
                    if (autoencoding) {

                        encoding = sharedcode.turboeditor.util.FileUtils.getDetectedEncoding(file);
                        if (encoding.isEmpty()) {
                            encoding = PreferenceHelper.getEncoding(BaseHomeActivity.this);
                        }
                    } else {
                        encoding = PreferenceHelper.getEncoding(BaseHomeActivity.this);
                    }

                    fileText = org.apache.commons.io.FileUtils.readFileToString(file, encoding);
                } catch (Exception e) {
                    message = e.getMessage();
                    fileText = "";
                }
                while (mDrawerLayout.isDrawerOpen(Gravity.START)) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                super.onPostExecute(result);
                progressDialog.hide();

                if (!message.isEmpty()) {
                    Toast.makeText(BaseHomeActivity.this, message, Toast.LENGTH_LONG).show();
                    EventBus.getDefault().post(new EventBusEvents.CannotOpenAFile());
                } else {
                    getFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_editor, EditorFragment.newInstance(event.getFile().getAbsolutePath(), fileText, encoding))
                            .commit();

                }

            }
        }.execute();

    }

    public void onEvent(EventBusEvents.SavedAFile event) {
        try {
            closeKeyBoard();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        // Get intent, action and MIME type
        final Intent intent = getIntent();
        final String action = intent.getAction();
        final String type = intent.getType();

        if (Intent.ACTION_VIEW.equals(action)
                || Intent.ACTION_EDIT.equals(action)
                || Intent.ACTION_PICK.equals(action)
                && type != null) {
            //This Activity was called by startActivityForResult
            final Intent returnIntent = new Intent();
            setResult(Activity.RESULT_OK, returnIntent);
            // finish the activity
            finish();
        }
        if (!ProCheckUtils.isPro(getApplicationContext()))
            displayInterstitial();
    }

    public void onEvent(EventBusEvents.AFileIsSelected event) {
        String name = FilenameUtils.getName(event.getPath());
        if (name.isEmpty())
            getActionBar().setTitle(R.string.nome_app_turbo_editor);
        else
            getActionBar().setTitle(name);
    }

    /**
     * When a file can't be opened
     * Invoked by the EditorFragment
     *
     * @param event The event called
     */
    public void onEvent(EventBusEvents.CannotOpenAFile event) {
        //
        mDrawerLayout.openDrawer(Gravity.LEFT);
        //
        getActionBar().setTitle(getString(R.string.nome_app_turbo_editor));
        // Replace fragment
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_editor, new NoFileOpenedFragment())
                .commit();
    }

    public void onEvent(EventBusEvents.APreferenceValueWasChanged event) {

        if (event.hasType(EventBusEvents.APreferenceValueWasChanged.Type.THEME_CHANGE)) {
            ThemeHelper.setWindowsBackground(this);
        }
    }
    //endregion

    // closes the soft keyboard
    private void closeKeyBoard() throws NullPointerException {
        // Central system API to the overall input method framework (IMF) architecture
        InputMethodManager inputManager =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        // Base interface for a remotable object
        IBinder windowToken = getCurrentFocus().getWindowToken();

        // Hide type
        int hideType = InputMethodManager.HIDE_NOT_ALWAYS;

        // Hide the KeyBoard
        inputManager.hideSoftInputFromWindow(windowToken, hideType);
    }

    /**
     * Setup the navigation drawer
     */
    private void setupNavigationDrawer() {
        mDrawerLayout = (CustomDrawerLayout) findViewById(R.id.drawer_layout);
        /* Action Bar */
        final ActionBar ab = getActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setHomeButtonEnabled(true);
        /* Navigation drawer */
        mDrawerToggle =
                new ActionBarDrawerToggle(
                        this,
                        mDrawerLayout,
                        R.drawable.ic_drawer,
                        R.string.nome_app_turbo_editor,
                        R.string.nome_app_turbo_editor) {

                    /**
                     * {@inheritDoc}
                     */
                    @Override
                    public void onDrawerClosed(View view) {
                        invalidateOptionsMenu();
                    }

                    /**
                     * {@inheritDoc}
                     */
                    @Override
                    public void onDrawerOpened(View drawerView) {
                        invalidateOptionsMenu();
                        try {
                            closeKeyBoard();
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }
                    }
                };
        /* link the mDrawerToggle to the Drawer Layout */
        mDrawerLayout.setDrawerListener(mDrawerToggle);
//mDrawerLayout.setFocusableInTouchMode(false);
    }

    /**
     * Show a dialog with the changelog
     */
    private void showChangeLog() {
        final String currentVersion = AppInfoHelper.getCurrentVersion(this);
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        final String lastVersion = preferences.getString("last_version", currentVersion);
        preferences.edit().putString("last_version", currentVersion).apply();
        if (!lastVersion.equals(currentVersion)) {
            ChangelogDialogFragment.showChangeLogDialog(getFragmentManager());
        }
    }

    /**
     * Parses the intent
     */
    private void parseIntent(Intent intent) {
        final String action = intent.getAction();
        final String type = intent.getType();

        if (Intent.ACTION_VIEW.equals(action)
                || Intent.ACTION_EDIT.equals(action)
                || Intent.ACTION_PICK.equals(action)
                && type != null) {
            // Post event
            EventBus.getDefault().postSticky(new EventBusEvents.NewFileToOpen(new File(intent.getData().getPath())));
        } else if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                onEvent(new EventBusEvents.NewFileToOpen(intent.getStringExtra(Intent.EXTRA_TEXT)));
                onEvent(new EventBusEvents.AFileIsSelected("")); // simulate click on the list
            }
        }
    }

    public abstract void displayInterstitial();
}
