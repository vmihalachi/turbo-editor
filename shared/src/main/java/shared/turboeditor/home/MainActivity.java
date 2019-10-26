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

package shared.turboeditor.home;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.core.view.MenuItemCompat;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.ShareActionProvider;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import android.text.Editable;
import android.text.InputType;
import android.text.Selection;
import android.text.Spannable;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.KeyListener;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ListView;
import android.widget.Toast;

import com.spazedog.lib.rootfw4.RootFW;
import com.spazedog.lib.rootfw4.utils.io.FileReader;

import org.apache.commons.io.FilenameUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import shared.turboeditor.R;
import shared.turboeditor.markdown.MarkdownActivity;
import shared.turboeditor.explorer.SelectFileActivity;
import shared.turboeditor.adapter.AdapterDrawer;
import shared.turboeditor.dialogfragment.ChangelogDialog;
import shared.turboeditor.dialogfragment.EditTextDialog;
import shared.turboeditor.dialogfragment.FileInfoDialog;
import shared.turboeditor.dialogfragment.FindTextDialog;
import shared.turboeditor.dialogfragment.NewFileDetailsDialog;
import shared.turboeditor.dialogfragment.NumberPickerDialog;
import shared.turboeditor.dialogfragment.SaveFileDialog;
import shared.turboeditor.preferences.PreferenceChangeType;
import shared.turboeditor.preferences.PreferenceHelper;
import shared.turboeditor.task.SaveFileTask;
import shared.turboeditor.home.texteditor.EditTextPadding;
import shared.turboeditor.home.texteditor.FileUtils;
import shared.turboeditor.home.texteditor.LineUtils;
import shared.turboeditor.home.texteditor.PageSystem;
import shared.turboeditor.home.texteditor.PageSystemButtons;
import shared.turboeditor.home.texteditor.Patterns;
import shared.turboeditor.home.texteditor.SearchResult;
import shared.turboeditor.util.AccessStorageApi;
import shared.turboeditor.util.AccessoryView;
import shared.turboeditor.util.AnimationUtils;
import shared.turboeditor.util.AppInfoHelper;
import shared.turboeditor.util.Device;
import shared.turboeditor.util.GreatUri;
import shared.turboeditor.util.IHomeActivity;
import shared.turboeditor.util.MimeTypes;
import shared.turboeditor.util.ProCheckUtils;
import shared.turboeditor.util.ThemeUtils;
import shared.turboeditor.util.ViewUtils;
import shared.turboeditor.views.CustomDrawerLayout;
import shared.turboeditor.views.DialogHelper;
import shared.turboeditor.views.GoodScrollView;

public abstract class MainActivity extends AppCompatActivity implements IHomeActivity, FindTextDialog
        .SearchDialogInterface, GoodScrollView.ScrollInterface, PageSystem.PageSystemInterface,
        PageSystemButtons.PageButtonsInterface, NumberPickerDialog.INumberPickerDialog, SaveFileDialog.ISaveDialog,
        AdapterView.OnItemClickListener, AdapterDrawer.Callbacks, AccessoryView.IAccessoryView, EditTextDialog.EditDialogListener {

    //region VARIABLES
    private static final int READ_REQUEST_CODE = 42,
            CREATE_REQUEST_CODE = 43,
            SAVE_AS_REQUEST_CODE = 44,
            SELECT_FILE_CODE = 121,
            SYNTAX_DELAY_MILLIS_SHORT = 250,
            SYNTAX_DELAY_MILLIS_LONG = 1500,
            ID_UNDO = R.id.im_undo,
            ID_REDO = R.id.im_redo;
    public static final int REQUEST_WRITE_STORAGE_PERMISSION = 130;
    private final Handler updateHandler = new Handler();
    private final Runnable colorRunnable_duringEditing =
            new Runnable() {
                @Override
                public void run() {
                    mEditor.replaceTextKeepCursor(null);
                }
            };
    private final Runnable colorRunnable_duringScroll =
            new Runnable() {
                @Override
                public void run() {
                    mEditor.replaceTextKeepCursor(null);
                }
            };
    private boolean fileOpened = false;
    public static String fileExtension = "";
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
    public static GoodScrollView verticalScroll;
    private static GreatUri greatUri = new GreatUri(Uri.EMPTY, "", "");
    private Editor mEditor;
    private HorizontalScrollView horizontalScroll;
    private static SearchResult searchResult;
    public static PageSystem pageSystem;
    private PageSystemButtons pageSystemButtons;
    private static String currentEncoding = "UTF-16";
    private Toolbar toolbar;

    /*
    Navigation Drawer
     */
    private AdapterDrawer arrayAdapter;
    private LinkedList<GreatUri> greatUris;
    //endregion

    private MainViewModel viewModel;

    //region Activity facts

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // set the windows background
        ThemeUtils.setWindowsBackground(this);
        // super!!
        super.onCreate(savedInstanceState);
        // setup the layout
        setContentView(R.layout.activity_home);

        MainViewModelFactory factory = new MainViewModelFactory(new AndroidHighlightColorProvider());
        viewModel = ViewModelProviders.of(this, factory).get(MainViewModel.class);

        toolbar = (Toolbar) findViewById(R.id.my_awesome_toolbar);
        setSupportActionBar(toolbar);
        // setup the navigation drawer
        setupNavigationDrawer();
        // reset text editor
        setupTextEditor();
        hideTextEditor();
        /* First Time we open this activity */
        if (savedInstanceState == null) {
            // Open
            mDrawerLayout.openDrawer(GravityCompat.START);
            // Set the default title
            getSupportActionBar().setTitle(getString(R.string.nome_app_turbo_editor));
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
        // Refresh the list view
        refreshList();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        parseIntent(intent);
    }

    @Override
    public void onPause() {
        super.onPause();

        if (PreferenceHelper.getAutoSave(getBaseContext()) && mEditor.canSaveFile()) {
            saveTheFile(false);
            mEditor.fileSaved(); // so it doesn't ask to save in onDetach
        }
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
    public final void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            onBackPressed();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_MENU) {
            return false;
        } else {
            if (mEditor == null)
                mEditor = findViewById(R.id.editor);

            // this will happen on first key pressed on hard-keyboard only. Once myInputField
            // gets the focus again, it will automatically receive further key presses.

            if (fileOpened && mEditor != null && !mEditor.hasFocus()) {
                mEditor.requestFocus();
                mEditor.onKeyDown(keyCode, event);
                return true;
            }
        }

        return false;
    }

    @Override
    public void onBackPressed() {

        try {
            // if we should ignore the back button
            if (PreferenceHelper.getIgnoreBackButton(this))
                return;

            if (mDrawerLayout.isDrawerOpen(GravityCompat.START) && fileOpened) {
                mDrawerLayout.closeDrawer(GravityCompat.START);
            } else if (mDrawerLayout.isDrawerOpen(GravityCompat.END) && fileOpened) {
                mDrawerLayout.closeDrawer(GravityCompat.END);
            } else if (fileOpened && mEditor.canSaveFile()) {
                new SaveFileDialog(greatUri, pageSystem.getAllText(mEditor
                        .getText().toString()), currentEncoding).show(getFragmentManager(),
                        "dialog");
            } else if (fileOpened) {

                // remove editor fragment
                hideTextEditor();

                // Set the default title
                getSupportActionBar().setTitle(getString(R.string.nome_app_turbo_editor));

                closedTheFile();

                mDrawerLayout.openDrawer(GravityCompat.START);
                mDrawerLayout.closeDrawer(GravityCompat.END);
            } else {
                showInterstitial();
                super.onBackPressed();
            }
        } catch (Exception e) {
            // maybe something is null, who knows
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (resultCode == RESULT_OK) {
            if (requestCode == SELECT_FILE_CODE) {

                final Uri data = intent.getData();
                final GreatUri newUri = new GreatUri(data, AccessStorageApi.getPath(this, data), AccessStorageApi.getName(this, data));

                newFileToOpen(newUri, "");
            } else {

                final Uri data = intent.getData();
                final GreatUri newUri = new GreatUri(data, AccessStorageApi.getPath(this, data), AccessStorageApi.getName(this, data));

                // grantUriPermission(getPackageName(), data, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                final int takeFlags = intent.getFlags()
                        & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                // Check for the freshest data.
                getContentResolver().takePersistableUriPermission(data, takeFlags);

                if (requestCode == READ_REQUEST_CODE || requestCode == CREATE_REQUEST_CODE) {

                    newFileToOpen(newUri, "");
                }

                if (requestCode == SAVE_AS_REQUEST_CODE) {

                    new SaveFileTask(this, newUri, pageSystem.getAllText(mEditor.getText().toString()), currentEncoding, new SaveFileTask.SaveFileInterface() {
                        @Override
                        public void fileSaved(Boolean success) {
                            savedAFile(greatUri, false);
                            newFileToOpen(newUri, "");
                        }
                    }).execute();
                }
            }
        }
    }

    private void forceUseStorageAccessFramework() {
        PreferenceHelper.setUseStorageAccessFramework(this, true);
        final SwitchCompat swStorageAccessFramework = (SwitchCompat) findViewById(R.id.switch_storage_access_framework);
        if (swStorageAccessFramework != null) {
            swStorageAccessFramework.setChecked(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_WRITE_STORAGE_PERMISSION:
                if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    // Permission denied.
                    Toast.makeText(this, R.string.storage_required, Toast.LENGTH_LONG).show();
                    forceUseStorageAccessFramework();
                } else {
                    PreferenceHelper.setUseStorageAccessFramework(this, false);
                }
                break;
        }
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // Send the event that a file was selected
        newFileToOpen(greatUris.get(position), "");
    }

    //endregion

    //region MENU

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (fileOpened && searchResult != null)
            getMenuInflater().inflate(R.menu.fragment_editor_search, menu);
        else if (fileOpened)
            getMenuInflater().inflate(R.menu.fragment_editor, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        if (fileOpened && searchResult != null) {
            MenuItem imReplace = menu.findItem(R.id.im_replace);
            MenuItem imReplaceAll = menu.findItem(R.id.im_replace_all);
            MenuItem imPrev = menu.findItem(R.id.im_previous_item);
            MenuItem imNext = menu.findItem(R.id.im_next_item);

            if (imReplace != null)
                imReplace.setVisible(searchResult.canReplaceSomething());

            if (imReplaceAll != null)
                imReplaceAll.setVisible(searchResult.canReplaceSomething());

            if (imPrev != null)
                imPrev.setVisible(searchResult.hasPrevious());

            if (imNext != null)
                imNext.setVisible(searchResult.hasNext());


        } else if (fileOpened) {
            MenuItem imSave = menu.findItem(R.id.im_save);
            MenuItem imUndo = menu.findItem(R.id.im_undo);
            MenuItem imRedo = menu.findItem(R.id.im_redo);

            if (mEditor != null) {
                if (imSave != null)
                    imSave.setVisible(mEditor.canSaveFile());
                if (imUndo != null)
                    imUndo.setVisible(mEditor.getCanUndo());
                if (imRedo != null)
                    imRedo.setVisible(mEditor.getCanRedo());
            } else {
                imSave.setVisible(false);
                imUndo.setVisible(false);
                imRedo.setVisible(false);
            }

            MenuItem imMarkdown = menu.findItem(R.id.im_view_markdown);
            boolean isMarkdown = Arrays.asList(MimeTypes.MIME_MARKDOWN).contains(FilenameUtils.getExtension(greatUri.getFileName()));
            if (imMarkdown != null)
                imMarkdown.setVisible(isMarkdown);

            MenuItem imShare = menu.findItem(R.id.im_share);
            if (imMarkdown != null) {
                ShareActionProvider shareAction = (ShareActionProvider) MenuItemCompat
                        .getActionProvider(imShare);
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_STREAM, greatUri.getUri());
                shareIntent.setType("text/plain");
                shareAction.setShareIntent(shareIntent);
            }
        }

        MenuItem imDonate = menu.findItem(R.id.im_donate);
        if (imDonate != null)
            if (ProCheckUtils.isPro(this, false))
                imDonate.setVisible(false);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int i = item.getItemId();
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            Toast.makeText(getBaseContext(), "drawer click", Toast.LENGTH_SHORT).show();
            mDrawerLayout.closeDrawer(GravityCompat.END);
            return true;
        } else if (i == R.id.im_save_normaly) {
            saveTheFile(false);

        } else if (i == R.id.im_save_as) {
            saveTheFile(true);

        } else if (i == R.id.im_rename) {
            EditTextDialog.newInstance(EditTextDialog.Actions.Rename, greatUri.getFileName()).show(getFragmentManager().beginTransaction(), "dialog");
        } else if (i == R.id.im_undo) {
            mEditor.onTextContextMenuItem(ID_UNDO);

        } else if (i == R.id.im_redo) {
            mEditor.onTextContextMenuItem(ID_REDO);

        } else if (i == R.id.im_search) {
            FindTextDialog.newInstance(mEditor.getText().toString()).show(getFragmentManager()
                    .beginTransaction(), "dialog");
        } else if (i == R.id.im_cancel) {
            searchResult = null;
            invalidateOptionsMenu();

        } else if (i == R.id.im_replace) {
            replaceText(false);

        } else if (i == R.id.im_replace_all) {
            replaceText(true);

        } else if (i == R.id.im_next_item) {
            nextResult();

        } else if (i == R.id.im_previous_item) {
            previousResult();

        } else if (i == R.id.im_goto_line) {
            int min = mEditor.getLineUtils().firstReadLine();
            int max = mEditor.getLineUtils().lastReadLine();
            NumberPickerDialog.newInstance
                    (NumberPickerDialog.Actions.GoToLine, min, min, max).show(getFragmentManager().beginTransaction(), "dialog");
        } else if (i == R.id.im_view_it_on_browser) {
            Intent browserIntent;
            try {
                browserIntent = new Intent(Intent.ACTION_VIEW);
                browserIntent.setDataAndType(greatUri.getUri(), "*/*");
                startActivity(browserIntent);
            } catch (ActivityNotFoundException ex2) {
                //
            }

        } else if (i == R.id.im_view_markdown) {
            Intent browserIntent = new Intent(MainActivity.this, MarkdownActivity.class);
            browserIntent.putExtra("text", pageSystem.getAllText(mEditor.getText().toString()));
            startActivity(browserIntent);
        } else if (i == R.id.im_info) {
            FileInfoDialog.newInstance(greatUri.getUri()).show(getFragmentManager().beginTransaction(), "dialog");
        } else if (i == R.id.im_donate) {
            final String appPackageName = "com.maskyn.fileeditorpro";
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
            } catch (android.content.ActivityNotFoundException anfe) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
            }
        }
        return super.onOptionsItemSelected(item);
    }
    //endregion

    // region OTHER THINGS
    void replaceText(boolean all) {
        if (all) {
            if (searchResult.isRegex)
                mEditor.setText(pageSystem.getAllText(mEditor.getText().toString()).replaceAll(searchResult.whatToSearch, searchResult.textToReplace));
            else
                mEditor.setText(pageSystem.getAllText(mEditor.getText().toString()).replace(searchResult.whatToSearch, searchResult.textToReplace));

            searchResult = null;
            invalidateOptionsMenu();
        } else {
            int start = searchResult.foundIndex.get(searchResult.index);
            int end = start + searchResult.textLength;
            mEditor.setText(mEditor.getText().replace(start, end, searchResult.textToReplace));
            searchResult.doneReplace();

            invalidateOptionsMenu();

            if (searchResult.hasNext())
                nextResult();
            else if (searchResult.hasPrevious())
                previousResult();
        }
    }

    void nextResult() {
        if (searchResult.index == mEditor.getLineCount() - 1) // last result of page
        {
            return;
        }


        if (searchResult.index < searchResult.numberOfResults() - 1) { // equal zero is not good
            searchResult.index++;
            final int line = mEditor.getLineUtils().getLineFromIndex(searchResult.foundIndex.get
                    (searchResult.index), mEditor.getLineCount(), mEditor.getLayout());


            verticalScroll.post(() -> {
                int y = mEditor.getLayout().getLineTop(line);
                if (y > 100)
                    y -= 100;
                else
                    y = 0;

                verticalScroll.scrollTo(0, y);
            });

            mEditor.setFocusable(true);
            mEditor.requestFocus();
            mEditor.setSelection(searchResult.foundIndex.get(searchResult.index),
                    searchResult.foundIndex.get(searchResult.index) + searchResult.textLength);
        }

        invalidateOptionsMenu();
    }

    void previousResult() {
        if (searchResult.index == 0)
            return;
        if (searchResult.index > 0) {
            searchResult.index--;
            final int line = LineUtils.getLineFromIndex(searchResult.foundIndex.get
                    (searchResult.index), mEditor.getLineCount(), mEditor.getLayout());
            verticalScroll.post(new Runnable() {
                @Override
                public void run() {
                    int y = mEditor.getLayout().getLineTop(line);
                    if (y > 100)
                        y -= 100;
                    else
                        y = 0;
                    verticalScroll.scrollTo(0, y);
                }
            });

            mEditor.setFocusable(true);
            mEditor.requestFocus();
            mEditor.setSelection(searchResult.foundIndex.get(searchResult.index),
                    searchResult.foundIndex.get(searchResult.index) + searchResult.textLength);
        }

        invalidateOptionsMenu();
    }

    private boolean useStorageAccessFramework() {
        if (!Device.hasKitKatApi())
            return false;
        boolean pref = PreferenceHelper.getUseStorageAccessFramework(this);
        if (pref)
            return true;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            forceUseStorageAccessFramework();
            return true;
        }
        return false;
    }

    public void saveTheFile(boolean saveAs) {
        if (!saveAs && greatUri != null && greatUri.getUri() != null && greatUri.getUri() != Uri.EMPTY)
            new SaveFileTask(this, greatUri, pageSystem.getAllText(mEditor.getText()
                    .toString()), currentEncoding, success -> savedAFile(greatUri, true)).execute();
        else {
            if (useStorageAccessFramework()) {
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_TITLE, greatUri.getFileName());
                startActivityForResult(intent, SAVE_AS_REQUEST_CODE);
            } else {
                new NewFileDetailsDialog(
                        greatUri,
                        pageSystem.getAllText(mEditor.getText().toString()),
                        currentEncoding
                ).show(getFragmentManager().beginTransaction(), "dialog");
            }

        }
    }

    /**
     * Setup the navigation drawer
     */
    private void setupNavigationDrawer() {
        mDrawerLayout = (CustomDrawerLayout) findViewById(R.id.drawer_layout);
        /* Action Bar
        final ActionBar ab = toolbar;
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setHomeButtonEnabled(true);*/
        /* Navigation drawer */
        mDrawerToggle =
                new ActionBarDrawerToggle(
                        this,
                        mDrawerLayout,
                        toolbar,
                        R.string.nome_app_turbo_editor,
                        R.string.nome_app_turbo_editor) {

                    @Override
                    public void onDrawerOpened(View drawerView) {
                        supportInvalidateOptionsMenu();
                        try {
                            closeKeyBoard();
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onDrawerClosed(View view) {
                        supportInvalidateOptionsMenu();
                    }
                };
        /* link the mDrawerToggle to the Drawer Layout */
        mDrawerLayout.setDrawerListener(mDrawerToggle);
//mDrawerLayout.setFocusableInTouchMode(false);

        ListView listView = (ListView) findViewById(android.R.id.list);
        listView.setEmptyView(findViewById(android.R.id.empty));
        greatUris = new LinkedList<>();
        arrayAdapter = new AdapterDrawer(this, greatUris, this);
        listView.setAdapter(arrayAdapter);
        listView.setOnItemClickListener(this);
    }

    private void setupTextEditor() {

        verticalScroll = findViewById(R.id.vertical_scroll);
        horizontalScroll = findViewById(R.id.horizontal_scroll);
        mEditor = findViewById(R.id.editor);

        AccessoryView accessoryView = (AccessoryView) findViewById(R.id.accessoryView);
        accessoryView.setInterface(this);

        HorizontalScrollView parentAccessoryView = (HorizontalScrollView) findViewById(R.id.parent_accessory_view);
        ViewUtils.setVisible(parentAccessoryView, PreferenceHelper.getUseAccessoryView(this));


        if (PreferenceHelper.getWrapContent(this)) {
            horizontalScroll.removeView(mEditor);
            verticalScroll.removeView(horizontalScroll);
            verticalScroll.addView(mEditor);
        }

        verticalScroll.setScrollInterface(this);

        pageSystem = new PageSystem(this, this, "");

        pageSystemButtons = new PageSystemButtons(this, this,
                findViewById(R.id.fabPrev),
                findViewById(R.id.fabNext));

        mEditor.setupEditor();
    }

    private void showTextEditor() {

        fileOpened = true;

        findViewById(R.id.text_editor).setVisibility(View.VISIBLE);
        findViewById(R.id.no_file_opened_messagge).setVisibility(View.GONE);

        mEditor.resetVariables();
        searchResult = null;

        invalidateOptionsMenu();

        mEditor.disableTextChangedListener();
        mEditor.replaceTextKeepCursor(pageSystem.getCurrentPageText());
        mEditor.enableTextChangedListener();
    }

    private void hideTextEditor() {

        fileOpened = false;

        try {
            findViewById(R.id.text_editor).setVisibility(View.GONE);
            findViewById(R.id.no_file_opened_messagge).setVisibility(View.VISIBLE);

            mEditor.disableTextChangedListener();
            mEditor.replaceTextKeepCursor("");
            mEditor.enableTextChangedListener();
        } catch (Exception e) {
            // lol
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
            //newFileToOpen(new File(intent
            //        .getData().getPath()), "");
            Uri uri = intent.getData();
            GreatUri newUri = new GreatUri(uri, AccessStorageApi.getPath(this, uri), AccessStorageApi.getName(this, uri));
            newFileToOpen(newUri, "");
        } else if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                newFileToOpen(new GreatUri(Uri.EMPTY, "", ""), intent.getStringExtra(Intent.EXTRA_TEXT));
            }
        }
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
            ChangelogDialog.showChangeLogDialog(getFragmentManager());
        }
    }

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

    void updateTextSyntax() {
        if (!PreferenceHelper.getSyntaxHighlight(this) || mEditor.hasSelection() ||
                updateHandler == null || colorRunnable_duringEditing == null)
            return;

        updateHandler.removeCallbacks(colorRunnable_duringEditing);
        updateHandler.removeCallbacks(colorRunnable_duringScroll);
        updateHandler.postDelayed(colorRunnable_duringEditing, SYNTAX_DELAY_MILLIS_LONG);
    }

    private void refreshList() {
        refreshList(null, false, false);
    }

    private void refreshList(@Nullable GreatUri thisUri, boolean add, boolean delete) {
        int max_recent_files = 15;
        if (add)
            max_recent_files--;

        // File paths saved in preferences
        String[] savedPaths = PreferenceHelper.getSavedPaths(this);
        int first_index_of_array = savedPaths.length > max_recent_files ? savedPaths.length - max_recent_files : 0;
        savedPaths = Arrays.copyOfRange(savedPaths, first_index_of_array, savedPaths.length);
        // File names for the list
        greatUris.clear();
        // StringBuilder that will contain the file paths
        StringBuilder sb = new StringBuilder();

        // for cycle to convert paths to names
        for (int i = 0; i < savedPaths.length; i++) {
            Uri particularUri = Uri.parse(savedPaths[i]);
            String name = AccessStorageApi.getName(this, particularUri);
            // Check that the file exist
            // if is null or empty the particular url we dont use it
            if (particularUri != null && !particularUri.equals(Uri.EMPTY) && !TextUtils.isEmpty(name)) {
                // if the particular uri is good
                boolean good = false;
                if (thisUri == null || thisUri.getUri() == null || thisUri.getUri() == Uri.EMPTY)
                    good = true;
                else {
                    if (delete == false)
                        good = true;
                    else if (!thisUri.getUri().equals(particularUri))
                        good = true;
                    else
                        good = false;
                }
                if (good) {
                    greatUris.addFirst(new GreatUri(particularUri, AccessStorageApi.getPath(this, particularUri), name));
                    sb.append(savedPaths[i]).append(",");
                }
            }
            //}
        }
        // if is not null, empty, we have to add something and we dont already have this uri
        if (thisUri != null && !thisUri.getUri().equals(Uri.EMPTY) && add && !Arrays.asList(savedPaths).contains(thisUri.getUri().toString())) {
            sb.append(thisUri.getUri().toString()).append(",");
            greatUris.addFirst(thisUri);
        }
        // save list without empty or non existed files
        PreferenceHelper.setSavedPaths(this, sb);
        // Set adapter
        arrayAdapter.notifyDataSetChanged();
    }
    //endregion

    //region EVENTBUS
    void newFileToOpen(final GreatUri newUri, final String newFileText) {

        if (fileOpened && mEditor != null && mEditor.canSaveFile() && greatUri != null && pageSystem != null && currentEncoding != null) {
            new SaveFileDialog(greatUri, pageSystem.getAllText(mEditor
                    .getText().toString()), currentEncoding, true, newUri).show(getFragmentManager(),
                    "dialog");
            return;
        }

        new AsyncTask<Void, Void, Void>() {

            String message = "";
            String fileText = "";
            String fileName = "";
            String encoding = "UTF-16";
            boolean isRootRequired = false;
            ProgressDialog progressDialog;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                // Close the drawer
                mDrawerLayout.closeDrawer(GravityCompat.START);
                progressDialog = new ProgressDialog(MainActivity.this);
                progressDialog.setMessage(getString(R.string.please_wait));
                progressDialog.show();

            }

            @Override
            protected Void doInBackground(Void... params) {
                try {
                    // if no new uri
                    if (newUri == null || newUri.getUri() == null || newUri.getUri() == Uri.EMPTY) {
                        fileExtension = "txt";
                        fileText = newFileText;
                    } else {
                        String filePath = newUri.getFilePath();

                        // if the uri has no path
                        if (TextUtils.isEmpty(filePath)) {
                            fileName = newUri.getFileName();
                            fileExtension = FilenameUtils.getExtension(fileName).toLowerCase();

                            readUri(newUri.getUri(), filePath, false);
                        }
                        // if the uri has a path
                        else {
                            fileName = FilenameUtils.getName(filePath);
                            fileExtension = FilenameUtils.getExtension(fileName).toLowerCase();

                            isRootRequired = !newUri.isReadable();
                            // if we cannot read the file, root permission required
                            if (isRootRequired) {
                                readUri(newUri.getUri(), filePath, true);
                            }
                            // if we can read the file associated with the uri
                            else {
                                readUri(newUri.getUri(), filePath, false);
                            }
                        }

                    }

                    greatUri = newUri;
                } catch (Exception e) {
                    message = e.getMessage();
                    fileText = "";
                }

                while (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                return null;
            }

            private void readUri(Uri uri, String path, boolean asRoot) throws IOException {


                BufferedReader buffer = null;
                StringBuilder stringBuilder = new StringBuilder();
                String line;

                if (asRoot) {

                    encoding = "UTF-8";

                    // Connect the shared connection
                    if (RootFW.connect()) {
                        FileReader reader = RootFW.getFileReader(path);
                        buffer = new BufferedReader(reader);
                    }
                } else {

                    boolean autoencoding = PreferenceHelper.getAutoEncoding(MainActivity.this);
                    if (autoencoding) {
                        encoding = FileUtils.getDetectedEncoding(getContentResolver().openInputStream(uri));
                        if (encoding.isEmpty()) {
                            encoding = PreferenceHelper.getEncoding(MainActivity.this);
                        }
                    } else {
                        encoding = PreferenceHelper.getEncoding(MainActivity.this);
                    }

                    InputStream inputStream = getContentResolver().openInputStream(uri);
                    if (inputStream != null) {
                        buffer = new BufferedReader(new InputStreamReader(inputStream, encoding));
                    }
                }

                if (buffer != null) {
                    while ((line = buffer.readLine()) != null) {
                        stringBuilder.append(line);
                        stringBuilder.append("\n");
                    }
                    buffer.close();
                    fileText = stringBuilder.toString();
                }

                if (isRootRequired)
                    RootFW.disconnect();
            }

            @Override
            protected void onPostExecute(Void result) {
                super.onPostExecute(result);
                progressDialog.hide();

                if (!TextUtils.isEmpty(message)) {
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                    cannotOpenFile();
                } else {

                    pageSystem = new PageSystem(MainActivity.this, MainActivity.this, fileText);
                    currentEncoding = encoding;

                    aFileWasSelected(greatUri);

                    showTextEditor();

                    if (fileName.isEmpty())
                        getSupportActionBar().setTitle(R.string.new_file);
                    else
                        getSupportActionBar().setTitle(fileName);

                    if (greatUri != null) {
                        refreshList(greatUri, true, false);
                    }
                }

            }
        }.execute();
    }

    public void savedAFile(GreatUri uri, boolean updateList) {

        if (uri != null) {

            greatUri = uri;

            String name = uri.getFileName();
            fileExtension = FilenameUtils.getExtension(name).toLowerCase();
            toolbar.setTitle(name);

            if (updateList) {
                refreshList(uri, true, false);
                arrayAdapter.selectPosition(uri);
            }
        }

        mEditor.clearHistory();
        mEditor.fileSaved();
        invalidateOptionsMenu();

        try {
            closeKeyBoard();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    /**
     * When a file can't be opened
     * Invoked by the EditorFragment
     *
     * @param event The event called
     */
    void cannotOpenFile() {
        //
        mDrawerLayout.openDrawer(Gravity.LEFT);
        //
        getSupportActionBar().setTitle(getString(R.string.nome_app_turbo_editor));
        //
        supportInvalidateOptionsMenu();
        // Replace fragment
        hideTextEditor();
    }

    public void aPreferenceValueWasChanged(final PreferenceChangeType type) {
        this.aPreferenceValueWasChanged(new ArrayList<PreferenceChangeType>() {{
            add(type);
        }});
    }

    void aPreferenceValueWasChanged(List<PreferenceChangeType> types) {

        if (types.contains(PreferenceChangeType.THEME_CHANGE)) {
            ThemeUtils.setWindowsBackground(this);
            AccessoryView accessoryView = (AccessoryView) findViewById(R.id.accessoryView);
            accessoryView.updateTextColors();
        }

        if (types.contains(PreferenceChangeType.WRAP_CONTENT)) {
            if (PreferenceHelper.getWrapContent(this)) {
                horizontalScroll.removeView(mEditor);
                verticalScroll.removeView(horizontalScroll);
                verticalScroll.addView(mEditor);
            } else {
                verticalScroll.removeView(mEditor);
                verticalScroll.addView(horizontalScroll);
                horizontalScroll.addView(mEditor);
            }
        } else if (types.contains(PreferenceChangeType.LINE_NUMERS)) {
            mEditor.disableTextChangedListener();
            mEditor.replaceTextKeepCursor(null);
            mEditor.enableTextChangedListener();
            mEditor.updatePadding();
        } else if (types.contains(PreferenceChangeType.SYNTAX)) {
            mEditor.disableTextChangedListener();
            mEditor.replaceTextKeepCursor(mEditor.getText().toString());
            mEditor.enableTextChangedListener();
        } else if (types.contains(PreferenceChangeType.MONOSPACE)) {
            if (PreferenceHelper.getUseMonospace(this))
                mEditor.setTypeface(Typeface.MONOSPACE);
            else
                mEditor.setTypeface(Typeface.DEFAULT);
        } else if (types.contains(PreferenceChangeType.THEME_CHANGE)) {
            if (PreferenceHelper.getLightTheme(this)) {
                mEditor.setTextColor(getResources().getColor(R.color.textColorInverted));
            } else {
                mEditor.setTextColor(getResources().getColor(R.color.textColor));
            }
        } else if (types.contains(PreferenceChangeType.TEXT_SUGGESTIONS) || types.contains(PreferenceChangeType.READ_ONLY)) {
            if (PreferenceHelper.getReadOnly(this)) {
                mEditor.setReadOnly(true);
            } else {
                mEditor.setReadOnly(false);
                if (PreferenceHelper.getSuggestionActive(this)) {
                    mEditor.setInputType(InputType.TYPE_CLASS_TEXT | InputType
                            .TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE);
                } else {
                    mEditor.setInputType(InputType.TYPE_CLASS_TEXT | InputType
                            .TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                            | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD | InputType
                            .TYPE_TEXT_FLAG_IME_MULTI_LINE);
                }
            }
            // sometimes it becomes monospace after setting the input type
            if (PreferenceHelper.getUseMonospace(this))
                mEditor.setTypeface(Typeface.MONOSPACE);
            else
                mEditor.setTypeface(Typeface.DEFAULT);
        } else if (types.contains(PreferenceChangeType.FONT_SIZE)) {
            mEditor.updatePadding();
            mEditor.setTextSize(PreferenceHelper.getFontSize(this));
        } else if (types.contains(PreferenceChangeType.ACCESSORY_VIEW)) {
            HorizontalScrollView parentAccessoryView = (HorizontalScrollView) findViewById(R.id.parent_accessory_view);
            ViewUtils.setVisible(parentAccessoryView, PreferenceHelper.getUseAccessoryView(this));
            mEditor.updatePadding();
        } else if (types.contains(PreferenceChangeType.ENCODING)) {
            String oldEncoding, newEncoding;
            oldEncoding = currentEncoding;
            newEncoding = PreferenceHelper.getEncoding(this);
            try {
                final byte[] oldText = mEditor.getText().toString().getBytes(oldEncoding);
                mEditor.disableTextChangedListener();
                mEditor.replaceTextKeepCursor(new String(oldText, newEncoding));
                mEditor.enableTextChangedListener();
                currentEncoding = newEncoding;
            } catch (UnsupportedEncodingException ignored) {
                try {
                    final byte[] oldText = mEditor.getText().toString().getBytes(oldEncoding);
                    mEditor.disableTextChangedListener();
                    mEditor.replaceTextKeepCursor(new String(oldText, "UTF-16"));
                    mEditor.enableTextChangedListener();
                } catch (UnsupportedEncodingException ignored2) {
                }
            }
        }
    }

    void aFileWasSelected(GreatUri uri) {
        arrayAdapter.selectPosition(uri);
    }

    void closedTheFile() {
        arrayAdapter.selectPosition(new GreatUri(Uri.EMPTY, "", ""));
    }
    //endregion

    public void OpenFolder(View view) {
        if (useStorageAccessFramework()) {

        } else {
            Intent intent = SelectFileActivity.CreateSelectFileActivityIntnet(this, SelectFileActivity.Action.SelectFolder);
            AnimationUtils.startActivityWithScale(this, intent, true, SELECT_FILE_CODE, view);
        }
    }

    //region Calls from the layout
    public void OpenFile(View view) {

        if (useStorageAccessFramework()) {
            // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
            // browser.
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

            // Filter to only show results that can be "opened", such as a
            // file (as opposed to a list of contacts or timezones)
            intent.addCategory(Intent.CATEGORY_OPENABLE);

            // Filter to show only images, using the image MIME data type.
            // If one wanted to search for ogg vorbis files, the type would be "audio/ogg".
            // To search for all documents available via installed storage providers,
            // it would be "*/*".
            intent.setType("*/*");

            startActivityForResult(intent, READ_REQUEST_CODE);
        } else {
            Intent intent = SelectFileActivity.CreateSelectFileActivityIntnet(this, SelectFileActivity.Action.SelectFile);
            AnimationUtils.startActivityWithScale(this, intent, true, SELECT_FILE_CODE, view);
        }
    }

    public void CreateFile(View view) {
        if (useStorageAccessFramework()) {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.setType("*/*");
            //intent.putExtra(Intent.EXTRA_TITLE, ".txt");
            startActivityForResult(intent, CREATE_REQUEST_CODE);
        } else {
            newFileToOpen(new GreatUri(Uri.EMPTY, "", ""), "");
        }
    }

    public void OpenInfo(View view) {
        DialogHelper.showAboutDialog(this);
    }

    public void OpenSettings(View view) {
        mDrawerLayout.closeDrawer(Gravity.START);
        mDrawerLayout.openDrawer(Gravity.END);
    }
    //endregion

    //region Ovverideses
    @Override
    public void nextPageClicked() {
        pageSystem.savePage(mEditor.getText().toString());
        pageSystem.nextPage();
        mEditor.disableTextChangedListener();
        mEditor.replaceTextKeepCursor(pageSystem.getCurrentPageText());
        mEditor.enableTextChangedListener();

        verticalScroll.postDelayed(new Runnable() {
            @Override
            public void run() {
                verticalScroll.smoothScrollTo(0, 0);
            }
        }, 200);

        if (!PreferenceHelper.getPageSystemButtonsPopupShown(this)) {
            PreferenceHelper.setPageSystemButtonsPopupShown(this, true);
            Toast.makeText(this, getString(R.string.long_click_for_more_options),
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void prevPageClicked() {
        pageSystem.savePage(mEditor.getText().toString());
        pageSystem.prevPage();
        mEditor.disableTextChangedListener();
        mEditor.replaceTextKeepCursor(pageSystem.getCurrentPageText());
        mEditor.enableTextChangedListener();

        verticalScroll.postDelayed(new Runnable() {
            @Override
            public void run() {
                verticalScroll.smoothScrollTo(0, 0);
            }
        }, 200);

        if (!PreferenceHelper.getPageSystemButtonsPopupShown(this)) {
            PreferenceHelper.setPageSystemButtonsPopupShown(this, true);
            Toast.makeText(this, getString(R.string.long_click_for_more_options),
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void pageSystemButtonLongClicked() {
        int maxPages = pageSystem.getMaxPage();
        int currentPage = pageSystem.getCurrentPage();
        NumberPickerDialog.newInstance
                (NumberPickerDialog.Actions.SelectPage, 0, currentPage, maxPages).show(getFragmentManager().beginTransaction(), "dialog");
    }

    @Override
    public boolean canReadNextPage() {
        return pageSystem.canReadNextPage();
    }

    @Override
    public boolean canReadPrevPage() {
        return pageSystem.canReadPrevPage();
    }

    @Override
    public void onSearchDone(SearchResult searchResult) {
        MainActivity.searchResult = searchResult;
        invalidateOptionsMenu();

        final int line = LineUtils.getLineFromIndex(searchResult.foundIndex.getFirst
                (), mEditor.getLineCount(), mEditor.getLayout());
        verticalScroll.post(new Runnable() {
            @Override
            public void run() {
                int y = mEditor.getLayout().getLineTop(line);
                if (y > 100)
                    y -= 100;
                else
                    y = 0;

                verticalScroll.scrollTo(0, y);
            }
        });

        mEditor.setFocusable(true);
        mEditor.requestFocus();
        mEditor.setSelection(searchResult.foundIndex.getFirst(), searchResult.foundIndex.getFirst
                () + searchResult.textLength);

    }

    @Override
    public void onPageChanged(int page) {
        pageSystemButtons.updateVisibility(false);
        searchResult = null;
        mEditor.clearHistory();
        invalidateOptionsMenu();
    }

    @Override
    public void onScrollChanged(int l, int t, int oldl, int oldt) {
        pageSystemButtons.updateVisibility(Math.abs(t) > 10);

        if (!PreferenceHelper.getSyntaxHighlight(this) || (mEditor.hasSelection() &&
                searchResult == null) || updateHandler == null || colorRunnable_duringScroll == null)
            return;

        updateHandler.removeCallbacks(colorRunnable_duringEditing);
        updateHandler.removeCallbacks(colorRunnable_duringScroll);
        updateHandler.postDelayed(colorRunnable_duringScroll, SYNTAX_DELAY_MILLIS_SHORT);
    }

    @Override
    public void onNumberPickerDialogDismissed(NumberPickerDialog.Actions action, int value) {
        if (action == NumberPickerDialog.Actions.SelectPage) {
            pageSystem.savePage(mEditor.getText().toString());
            pageSystem.goToPage(value);
            mEditor.disableTextChangedListener();
            mEditor.replaceTextKeepCursor(pageSystem.getCurrentPageText());
            mEditor.enableTextChangedListener();

            verticalScroll.postDelayed(new Runnable() {
                @Override
                public void run() {
                    verticalScroll.smoothScrollTo(0, 0);
                }
            }, 200);

        } else if (action == NumberPickerDialog.Actions.GoToLine) {

            int fakeLine = mEditor.getLineUtils().fakeLineFromRealLine(value);
            final int y = mEditor.getLineUtils().getYAtLine(verticalScroll,
                    mEditor.getLineCount(), fakeLine);

            verticalScroll.postDelayed(new Runnable() {
                @Override
                public void run() {
                    verticalScroll.smoothScrollTo(0, y);
                }
            }, 200);
        }

    }

    @Override
    public void userDoesntWantToSave(boolean openNewFile, GreatUri newUri) {
        mEditor.fileSaved();
        if (openNewFile)
            newFileToOpen(newUri, "");
        else
            cannotOpenFile();
    }

    @Override
    public void CancelItem(int position, boolean andCloseOpenedFile) {
        refreshList(greatUris.get(position), false, true);
        if (andCloseOpenedFile)
            cannotOpenFile();
    }

    @Override
    public void onButtonAccessoryViewClicked(String text) {
        mEditor.getText().insert(mEditor.getSelectionStart(), text);
    }

    @Override
    public void onEdittextDialogEnded(String result, String hint, EditTextDialog.Actions action) {

        if (Device.hasKitKatApi() && TextUtils.isEmpty(greatUri.getFilePath())) {
            Uri newUri = null;
            try {
                DocumentsContract.renameDocument(getContentResolver(), greatUri.getUri(), result);
            } catch (FileNotFoundException e) {
                newUri = null;
            }
            // if everything is ok
            if (newUri != null) {

                // delete current
                refreshList(greatUri, false, true);

                greatUri.setUri(newUri);
                greatUri.setFilePath(AccessStorageApi.getPath(this, newUri));
                greatUri.setFileName(AccessStorageApi.getName(this, newUri));

                new SaveFileTask(this, greatUri, pageSystem.getAllText(mEditor.getText().toString()), currentEncoding, new SaveFileTask.SaveFileInterface() {
                    @Override
                    public void fileSaved(Boolean success) {
                        savedAFile(greatUri, true);
                    }
                }).execute();
            } else {
                Toast.makeText(this, R.string.file_cannot_be_renamed, Toast.LENGTH_SHORT).show();
            }
        } else {
            File newFile = new File(greatUri.getParentFolder(), result);
            // if everything is ok
            if (new File(greatUri.getFilePath()).renameTo(newFile)) {

                // delete current
                refreshList(greatUri, false, true);

                greatUri.setUri(Uri.fromFile(newFile));
                greatUri.setFilePath(newFile.getAbsolutePath());
                greatUri.setFileName(newFile.getName());

                new SaveFileTask(this, greatUri, pageSystem.getAllText(mEditor.getText().toString()), currentEncoding, new SaveFileTask.SaveFileInterface() {
                    @Override
                    public void fileSaved(Boolean success) {

                        savedAFile(greatUri, true);
                    }
                }).execute();
            } else {
                Toast.makeText(this, R.string.file_cannot_be_renamed, Toast.LENGTH_SHORT).show();
            }
        }


        /*new SaveFileTask(this, greatUri, pageSystem.getAllText(mEditor.getText().toString()), currentEncoding, new SaveFileTask.SaveFileInterface() {
            @Override
            public void fileSaved(Boolean success) {
                savedAFile(greatUri, true);
            }
        }).execute();*/
    }

    //endregion
}
