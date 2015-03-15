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

import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.Toolbar;
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

import com.faizmalkani.floatingactionbutton.FloatingActionButton;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.sufficientlysecure.rootcommands.Shell;
import org.sufficientlysecure.rootcommands.Toolbox;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sharedcode.turboeditor.R;
import sharedcode.turboeditor.adapter.AdapterDrawer;
import sharedcode.turboeditor.dialogfragment.ChangelogDialog;
import sharedcode.turboeditor.dialogfragment.FileInfoDialog;
import sharedcode.turboeditor.dialogfragment.FindTextDialog;
import sharedcode.turboeditor.dialogfragment.NewFileDetailsDialog;
import sharedcode.turboeditor.dialogfragment.NumberPickerDialog;
import sharedcode.turboeditor.dialogfragment.SaveFileDialog;
import sharedcode.turboeditor.preferences.PreferenceChangeType;
import sharedcode.turboeditor.preferences.PreferenceHelper;
import sharedcode.turboeditor.task.SaveFileTask;
import sharedcode.turboeditor.texteditor.EditTextPadding;
import sharedcode.turboeditor.texteditor.FileUtils;
import sharedcode.turboeditor.texteditor.LineUtils;
import sharedcode.turboeditor.texteditor.PageSystem;
import sharedcode.turboeditor.texteditor.PageSystemButtons;
import sharedcode.turboeditor.texteditor.Patterns;
import sharedcode.turboeditor.texteditor.SearchResult;
import sharedcode.turboeditor.util.AccessStorageApi;
import sharedcode.turboeditor.util.AccessoryView;
import sharedcode.turboeditor.util.AnimationUtils;
import sharedcode.turboeditor.util.AppInfoHelper;
import sharedcode.turboeditor.util.IHomeActivity;
import sharedcode.turboeditor.util.MimeTypes;
import sharedcode.turboeditor.util.ProCheckUtils;
import sharedcode.turboeditor.util.ThemeUtils;
import sharedcode.turboeditor.views.CustomDrawerLayout;
import sharedcode.turboeditor.views.DialogHelper;
import sharedcode.turboeditor.views.GoodScrollView;

public abstract class MainActivity extends ActionBarActivity implements IHomeActivity, FindTextDialog
        .SearchDialogInterface, GoodScrollView.ScrollInterface, PageSystem.PageSystemInterface,
        PageSystemButtons.PageButtonsInterface, NumberPickerDialog.INumberPickerDialog, SaveFileDialog.ISaveDialog,
        AdapterView.OnItemClickListener, AdapterDrawer.Callbacks, AccessoryView.IAccessoryView{

    //region VARIABLES
    private static final int READ_REQUEST_CODE = 42;
    private static final int
            ID_SELECT_ALL = android.R.id.selectAll;
    private static final int ID_CUT = android.R.id.cut;
    private static final int ID_COPY = android.R.id.copy;
    private static final int ID_PASTE = android.R.id.paste;
    private static final int SELECT_FILE_CODE = 121;
    private static final int SYNTAX_DELAY_MILLIS_SHORT = 250;
    private static final int SYNTAX_DELAY_MILLIS_LONG = 1500;
    private static final int ID_UNDO = R.id.im_undo;
    private static final int ID_REDO = R.id.im_redo;
    private static final int CHARS_TO_COLOR = 2500;
    private static  final Handler updateHandler = new Handler();
    private static final Runnable colorRunnable_duringEditing =
            new Runnable() {
                @Override
                public void run() {
                    mEditor.replaceTextKeepCursor(null);
                }
            };
    private static final Runnable colorRunnable_duringScroll =
            new Runnable() {
                @Override
                public void run() {
                    mEditor.replaceTextKeepCursor(null);
                }
            };
    private static boolean fileOpened = false;
    private static String fileExtension;
    /*
    * This class provides a handy way to tie together the functionality of
    * {@link DrawerLayout} and the framework <code>ActionBar</code> to implement the recommended
    * design for navigation drawers.
    */
    private static ActionBarDrawerToggle mDrawerToggle;
    /*
    * The Drawer Layout
    */
    private static CustomDrawerLayout mDrawerLayout;
    private static GoodScrollView verticalScroll;
    private static String sFilePath = "";
    private static Editor mEditor;
    private static HorizontalScrollView horizontalScroll;
    private static SearchResult searchResult;
    private static PageSystem pageSystem;
    private static PageSystemButtons pageSystemButtons;
    private static String currentEncoding = "UTF-16";
    private static Toolbar toolbar;

    /*
    Navigation Drawer
     */
    private static AdapterDrawer arrayAdapter;
    private static LinkedList<File> files;
    //endregion

    //region Activity facts

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // set the windows background
        ThemeUtils.setWindowsBackground(this);
        // super!!
        super.onCreate(savedInstanceState);
        // setup the layout
        setContentView(R.layout.activity_home);
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
            mDrawerLayout.openDrawer(Gravity.START);
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
                mEditor = (Editor) findViewById(R.id.editor);

            // this will happen on first key pressed on hard-keyboard only. Once myInputField
            // gets the focus again, it will automatically receive further key presses.

            try {
                if (fileOpened && mEditor != null && !mEditor.hasFocus()) {
                    mEditor.requestFocus();
                    mEditor.onKeyDown(keyCode, event);
                    return true;
                }
            } catch (NullPointerException ex) {

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

            if (mDrawerLayout.isDrawerOpen(Gravity.START) && fileOpened) {
                mDrawerLayout.closeDrawer(Gravity.START);
            } else if (mDrawerLayout.isDrawerOpen(Gravity.END) && fileOpened) {
                mDrawerLayout.closeDrawer(Gravity.END);
            } else if (fileOpened && mEditor.canSaveFile()) {
                SaveFileDialog.newInstance(sFilePath, pageSystem.getAllText(mEditor
                        .getText().toString()), currentEncoding).show(getFragmentManager(),
                        "dialog");
            } else if (fileOpened) {

                // remove editor fragment
                hideTextEditor();

                // Set the default title
                getSupportActionBar().setTitle(getString(R.string.nome_app_turbo_editor));

                closedTheFile();

                mDrawerLayout.openDrawer(Gravity.START);
                mDrawerLayout.closeDrawer(Gravity.END);
            } else {
                showInterstitial();
                super.onBackPressed();
            }
        } catch (Exception e) {
            // maybe something is null, who knows
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            String path = "";
            if (requestCode == SELECT_FILE_CODE) {
                path = data.getStringExtra("path");
                if(TextUtils.isEmpty(path))
                    path = AccessStorageApi.getPath(getBaseContext(), data.getData());
            }

            if (requestCode == READ_REQUEST_CODE) {
                path = AccessStorageApi.getPath(getBaseContext(), data.getData());
            }

            if (!TextUtils.isEmpty(path)) {
                File file = new File(path);
                if (file.isFile() && file.exists()) {
                    newFileToOpen(new File
                            (path), "");
                }
            }

        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // Path of the file selected
        String filePath = files.get(position).getAbsolutePath();
        // Send the event that a file was selected
        newFileToOpen(new File(filePath), "");
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
            boolean isMarkdown = Arrays.asList(MimeTypes.MIME_MARKDOWN).contains(FilenameUtils.getExtension(sFilePath));
            if (imMarkdown != null)
                imMarkdown.setVisible(isMarkdown);

            MenuItem imShare = menu.findItem(R.id.im_share);
            ShareActionProvider shareAction = (ShareActionProvider) MenuItemCompat
                    .getActionProvider(imShare);
            File f = new File(sFilePath);
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(f));
            shareIntent.setType("text/plain");
            shareAction.setShareIntent(shareIntent);
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
            mDrawerLayout.closeDrawer(Gravity.END);
            return true;
        } else if (i == R.id.im_save_normaly) {
            saveTheFile(false);

        }  else if (i == R.id.im_save_as) {
            saveTheFile(true);

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
                browserIntent.setDataAndType(Uri.fromFile(new File(sFilePath)), "text/*");
                startActivity(browserIntent);
            } catch (ActivityNotFoundException ex2) {
                //
            }

        } else if (i == R.id.im_view_markdown) {
            Intent browserIntent = new Intent(MainActivity.this, MarkdownActivity.class);
            browserIntent.putExtra("text", mEditor.getText().toString());
            startActivity(browserIntent);
        } else if (i == R.id.im_info) {
            FileInfoDialog.newInstance(sFilePath).show(getFragmentManager().beginTransaction(), "dialog");
        }
        return super.onOptionsItemSelected(item);
    }
    //endregion

    // region OTHER THINGS
    void replaceText(boolean all) {
        if (all) {
            mEditor.setText(mEditor.getText().toString().replaceAll(searchResult.whatToSearch, searchResult.textToReplace));

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

    private void saveTheFile(boolean saveAs) {
        File file = new File(sFilePath);
        if (!file.getName().isEmpty() && !saveAs)
            new SaveFileTask(this, sFilePath, pageSystem.getAllText(mEditor.getText()
                    .toString()), currentEncoding).execute();
        else {
            NewFileDetailsDialog.newInstance(
                file.getParent(),
                file.getName(),
                pageSystem.getAllText(mEditor.getText().toString()),
                currentEncoding
            ).show(getFragmentManager().beginTransaction(), "dialog");
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
        files = new LinkedList<>();
        arrayAdapter = new AdapterDrawer(this, files, this);
        listView.setAdapter(arrayAdapter);
        listView.setOnItemClickListener(this);
    }

    private void setupTextEditor() {

        verticalScroll = (GoodScrollView) findViewById(R.id.vertical_scroll);
        horizontalScroll = (HorizontalScrollView) findViewById(R.id.horizontal_scroll);
        mEditor = (Editor) findViewById(R.id.editor);

        AccessoryView accessoryView = (AccessoryView) findViewById(R.id.accessoryView);
        accessoryView.setInterface(this);
        //mEditor.setLayerType(View.LAYER_TYPE_NONE, null);

        if (PreferenceHelper.getWrapContent(this)) {
            horizontalScroll.removeView(mEditor);
            verticalScroll.removeView(horizontalScroll);
            verticalScroll.addView(mEditor);
        }

        verticalScroll.setScrollInterface(this);

        pageSystem = new PageSystem(this, this, "", null);

        pageSystemButtons = new PageSystemButtons(this, this,
                (FloatingActionButton) findViewById(R.id.fabPrev),
                (FloatingActionButton) findViewById(R.id.fabNext));
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
           newFileToOpen(new File(intent
                   .getData().getPath()), "");
        } else if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                newFileToOpen(null, intent.getStringExtra(Intent.EXTRA_TEXT));
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

    private void refreshList(){
        refreshList(null, false, false);
    }

    private void refreshList(@Nullable String path, boolean add, boolean delete) {
        int max_recent_files = 15;
        if(add)
            max_recent_files--;

        // File paths saved in preferences
        String[] savedPaths = PreferenceHelper.getSavedPaths(this);
        int first_index_of_array = savedPaths.length > max_recent_files ? savedPaths.length - max_recent_files : 0;
        savedPaths = ArrayUtils.subarray(savedPaths, first_index_of_array, savedPaths.length);
        // File names for the list
        files.clear();
        // StringBuilder that will contain the file paths
        StringBuilder sb = new StringBuilder();
        // for cycle to convert paths to names

        for(int i = 0; i < savedPaths.length; i++){
            String savedPath = savedPaths[i];
            File file = new File(savedPath);
            // Check that the file exist
            if (file.exists()) {
                if(path != null && path.equals(savedPath) && delete)
                    continue;
                else {
                    files.addFirst(file);
                    sb.append(savedPath).append(",");
                }
            }
        }
        if(path != null && !path.isEmpty() && add && !ArrayUtils.contains(savedPaths, path)) {
            sb.append(path).append(",");
            files.addFirst(new File(path));
        }
        // save list without empty or non existed files
        PreferenceHelper.setSavedPaths(this, sb);
        // Set adapter
        arrayAdapter.notifyDataSetChanged();
    }
    //endregion

    //region EVENTBUS
    public void newFileToOpen(final File newFile, final String newFileText) {

        if (fileOpened && mEditor != null && mEditor.canSaveFile()) {
            SaveFileDialog.newInstance(sFilePath, pageSystem.getAllText(mEditor
                    .getText().toString()), currentEncoding, true, newFile.getAbsolutePath()).show(getFragmentManager(),
                    "dialog");
            return;
        }

        new AsyncTask<Void, Void, Void>() {

            File file;
            String message = "";
            String fileText;
            String encoding;
            ProgressDialog progressDialog;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                // Close the drawer
                mDrawerLayout.closeDrawer(Gravity.START);
                progressDialog = new ProgressDialog(MainActivity.this);
                progressDialog.setMessage(getString(R.string.please_wait));
                progressDialog.show();

            }

            @Override
            protected Void doInBackground(Void... params) {
                file = newFile;
                if (file == null) {
                    file = new File("");
                }
                try {
                    if (!file.exists() || !file.isFile()) {
                        fileText = newFileText;
                        sFilePath = file.getAbsolutePath();
                        fileExtension = "txt";
                        return null;
                    }

                    file = file.getCanonicalFile();
                    sFilePath = file.getAbsolutePath();
                    fileExtension = FilenameUtils.getExtension(sFilePath).toLowerCase();

                    boolean isRoot;

                    if (!file.canRead()) {
                        Shell shell;
                        shell = Shell.startRootShell();
                        Toolbox tb = new Toolbox(shell);
                        isRoot = tb.isRootAccessGiven();

                        if (isRoot) {
                            File tempFile = new File(getFilesDir(), "temp.root.file");
                            if (!tempFile.exists())
                                tempFile.createNewFile();
                            tb.copyFile(file.getAbsolutePath(),
                                    tempFile.getAbsolutePath(), false, false);
                            file = new File(tempFile.getAbsolutePath());
                        }
                    }

                    boolean autoencoding = PreferenceHelper.getAutoEncoding(MainActivity.this);
                    if (autoencoding) {

                        encoding = FileUtils.getDetectedEncoding(file);
                        if (encoding.isEmpty()) {
                            encoding = PreferenceHelper.getEncoding(MainActivity.this);
                        }
                    } else {
                        encoding = PreferenceHelper.getEncoding(MainActivity.this);
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
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                    cannotOpenFile();
                } else {

                    pageSystem = new PageSystem(MainActivity.this, MainActivity.this, fileText, new File(sFilePath));
                    currentEncoding = encoding;

                    aFileWasSelected(sFilePath);

                    showTextEditor();

                    String name = FilenameUtils.getName(sFilePath);
                    if (name.isEmpty())
                        getSupportActionBar().setTitle(R.string.new_file);
                    else
                        getSupportActionBar().setTitle(name);

                    if(!name.isEmpty()) {
                        refreshList(sFilePath, true, false);
                    }
                }

            }
        }.execute();
    }

    public void savedAFile(String filePath) {

        sFilePath = filePath;
        fileExtension = FilenameUtils.getExtension(sFilePath).toLowerCase();

        toolbar.setTitle(FilenameUtils.getName(sFilePath));

        mEditor.clearHistory();
        mEditor.fileSaved();
        invalidateOptionsMenu();

        try {
            closeKeyBoard();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        refreshList(filePath, true, false);
        arrayAdapter.selectView(filePath);
    }

    /**
     * When a file can't be opened
     * Invoked by the EditorFragment
     *
     * @param event The event called
     */
    public void cannotOpenFile() {
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

    public void aPreferenceValueWasChanged(List<PreferenceChangeType> types) {

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
            if (PreferenceHelper.getLineNumbers(this)) {
                mEditor.setPadding(
                        EditTextPadding.getPaddingWithLineNumbers(this, PreferenceHelper.getFontSize(this)),
                        EditTextPadding.getPaddingTop(this),
                        EditTextPadding.getPaddingTop(this),
                        0);
            } else {
                mEditor.setPadding(
                        EditTextPadding.getPaddingWithoutLineNumbers(this),
                        EditTextPadding.getPaddingTop(this),
                        EditTextPadding.getPaddingTop(this),
                        0);
            }
        } else if (types.contains(PreferenceChangeType.SYNTAX)) {
            mEditor.disableTextChangedListener();
            mEditor.replaceTextKeepCursor(null);
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
            if (PreferenceHelper.getLineNumbers(this)) {
                mEditor.setPadding(
                        EditTextPadding.getPaddingWithLineNumbers(this, PreferenceHelper.getFontSize(this)),
                        EditTextPadding.getPaddingTop(this),
                        EditTextPadding.getPaddingTop(this),
                        0);
            } else {
                mEditor.setPadding(
                        EditTextPadding.getPaddingWithoutLineNumbers(this),
                        EditTextPadding.getPaddingTop(this),
                        EditTextPadding.getPaddingTop(this),
                        0);
            }
            mEditor.setTextSize(PreferenceHelper.getFontSize(this));
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

   public void aFileWasSelected(String filePath) {
        arrayAdapter.selectView(filePath);
    }

    public void closedTheFile() {
        arrayAdapter.selectView("");
    }
    //endregion

    //region Calls from the layout
    public void OpenFile(View view) {

//        if (Device.hasKitKatApi()) {
//            // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
//            // browser.
//            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//
//            // Filter to only show results that can be "opened", such as a
//            // file (as opposed to a list of contacts or timezones)
//            //intent.addCategory(Intent.CATEGORY_OPENABLE);
//
//            // Filter to show only images, using the image MIME data type.
//            // If one wanted to search for ogg vorbis files, the type would be "audio/ogg".
//            // To search for all documents available via installed storage providers,
//            // it would be "*/*".
//            intent.setType("*/*");
//
//            startActivityForResult(intent, READ_REQUEST_CODE);
//        } else {
            Intent subActivity = new Intent(MainActivity.this, SelectFileActivity.class);
            subActivity.putExtra("action", SelectFileActivity.Actions.SelectFile);
            AnimationUtils.startActivityWithScale(this, subActivity, true, SELECT_FILE_CODE, view);
//        }
    }

    public void CreateFile(View view) {
        newFileToOpen(null, ""); // do not send the event to others
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
    public void userDoesntWantToSave(boolean openNewFile, String pathOfNewFile) {
        Editor.canSaveFile = false;
        if(openNewFile)
            newFileToOpen(new File(pathOfNewFile), "");
        else
            cannotOpenFile();
    }

    @Override
    public void CancelItem(int position, boolean andCloseOpenedFile) {
        refreshList(files.get(position).getAbsolutePath(), false, true);
        if (andCloseOpenedFile)
            cannotOpenFile();
    }

    @Override
    public void onButtonAccessoryViewClicked(String text) {
        mEditor.getText().insert(mEditor.getSelectionStart(), text);
    }

    //endregion

    public static class Editor extends EditText {

        //region VARIABLES
        private static final TextPaint mPaintNumbers = new TextPaint();
        /**
         * The edit history.
         */
        private final EditHistory mEditHistory;
        /**
         * The change listener.
         */
        private final EditTextChangeListener
                mChangeListener;
        /**
         * Disconnect this undo/redo from the text
         * view.
         */
        private static boolean enabledChangeListener;
        private static int paddingTop;
        private static int numbersWidth;
        private static int lineHeight;

        private static int lineCount, realLine, startingLine;
        private static LineUtils lineUtils;
        /**
         * Is undo/redo being performed? This member
         * signals if an undo/redo operation is
         * currently being performed. Changes in the
         * text during undo/redo are not recorded
         * because it would mess up the undo history.
         */
        private static boolean mIsUndoOrRedo;
        private static Matcher m;
        private static boolean mShowUndo, mShowRedo;
        private static boolean canSaveFile;
        private static KeyListener keyListener;
        private static int firstVisibleIndex, firstColoredIndex;
        private static int deviceHeight;
        private static int editorHeight;
        private static boolean[] isGoodLineArray;
        private static int[] realLines;
        private static boolean wrapContent;
        //private static int lastLine;
        //private static int firstLine;
        private static CharSequence textToHighlight;
        private static int lastVisibleIndex;
        private static int i;
        //endregion

        //region CONSTRUCTOR
        public Editor(final Context context, AttributeSet attrs) {
            super(context, attrs);

            //setLayerType(View.LAYER_TYPE_NONE, null);

            mEditHistory = new EditHistory();
            mChangeListener = new EditTextChangeListener();
            lineUtils = new LineUtils();

            deviceHeight = getResources().getDisplayMetrics().heightPixels;

            paddingTop = EditTextPadding.getPaddingTop(getContext());

            mPaintNumbers.setAntiAlias(true);
            mPaintNumbers.setDither(false);
            mPaintNumbers.setTextAlign(Paint.Align.RIGHT);
            mPaintNumbers.setColor(getResources().getColor(R.color.file_text));

            if (PreferenceHelper.getLightTheme(getContext())) {
                setTextColor(getResources().getColor(R.color.textColorInverted));
            } else {
                setTextColor(getResources().getColor(R.color.textColor));
            }
            if (PreferenceHelper.getLineNumbers(getContext())) {
                setPadding(
                        EditTextPadding.getPaddingWithLineNumbers(getContext(), PreferenceHelper.getFontSize(getContext())),
                        EditTextPadding.getPaddingTop(getContext()),
                        EditTextPadding.getPaddingTop(getContext()),
                        0);
            } else {
                setPadding(
                        EditTextPadding.getPaddingWithoutLineNumbers(getContext()),
                        EditTextPadding.getPaddingTop(getContext()),
                        EditTextPadding.getPaddingTop(getContext()),
                        0);
            }

            if (PreferenceHelper.getReadOnly(getContext())) {
                setReadOnly(true);
            } else {
                setReadOnly(false);
                if (PreferenceHelper.getSuggestionActive(getContext())) {
                    setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                            | InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE);
                } else {
                    setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                            | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType
                            .TYPE_TEXT_VARIATION_VISIBLE_PASSWORD | InputType
                            .TYPE_TEXT_FLAG_IME_MULTI_LINE);
                }
            }

            if (PreferenceHelper.getUseMonospace(getContext())) {
                setTypeface(Typeface.MONOSPACE);
            } else {
                setTypeface(Typeface.DEFAULT);
            }
            setTextSize(PreferenceHelper.getFontSize(getContext()));

            setFocusable(true);
            setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!PreferenceHelper.getReadOnly(getContext())) {
                        verticalScroll.tempDisableListener(1000);
                        ((InputMethodManager) getContext().getSystemService(Context
                                .INPUT_METHOD_SERVICE))
                                .showSoftInput(Editor.this, InputMethodManager.SHOW_IMPLICIT);
                    }

                }
            });
            setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus && !PreferenceHelper.getReadOnly(getContext())) {
                        verticalScroll.tempDisableListener(1000);
                        ((InputMethodManager) getContext().getSystemService(Context
                                .INPUT_METHOD_SERVICE))
                                .showSoftInput(Editor.this, InputMethodManager.SHOW_IMPLICIT);
                    }
                }
            });

            setMaxHistorySize(30);

            resetVariables();
        }

        public void setReadOnly(boolean value) {
            if (value) {
                keyListener = getKeyListener();
                setKeyListener(null);
            } else {
                if (keyListener != null)
                    setKeyListener(keyListener);
            }
        }

        //region OVERRIDES
        @Override
        public void setTextSize(float size) {
            super.setTextSize(size);
            final float scale = getContext().getResources().getDisplayMetrics().density;
            mPaintNumbers.setTextSize((int) (size * scale * 0.65f));
            numbersWidth = (int) (EditTextPadding.getPaddingWithLineNumbers(getContext(),
                    PreferenceHelper.getFontSize(getContext())) * 0.8);
            lineHeight = getLineHeight();
        }


        @Override
        public void onDraw(@NonNull final Canvas canvas) {

            if (lineCount != getLineCount() || startingLine != pageSystem.getStartingLine()) {
                startingLine = pageSystem.getStartingLine();
                lineCount = getLineCount();
                lineUtils.updateHasNewLineArray(pageSystem
                        .getStartingLine(), lineCount, getLayout(), getText().toString());

                isGoodLineArray = lineUtils.getGoodLines();
                realLines = lineUtils.getRealLines();

            }

            //editorHeight = getHeight();

            if (PreferenceHelper.getLineNumbers(getContext())) {
                wrapContent = PreferenceHelper.getWrapContent(getContext());

                for (i = 0; i < lineCount; i++) {
                    // if last line we count it anyway
                    if (!wrapContent
                            || isGoodLineArray[i]) {
                        realLine = realLines[i];

                        canvas.drawText(String.valueOf(realLine),
                                numbersWidth, // they are all center aligned
                                paddingTop + lineHeight * (i + 1),
                                mPaintNumbers);
                    }
                }
            }

            super.onDraw(canvas);
        }


        //endregion

        //region Other

        @Override
        public boolean onKeyDown(int keyCode, @NonNull KeyEvent event) {

            if (event.isCtrlPressed()) {
                switch (keyCode) {
                    case KeyEvent.KEYCODE_A:
                        return onTextContextMenuItem(ID_SELECT_ALL);
                    case KeyEvent.KEYCODE_X:
                        return onTextContextMenuItem(ID_CUT);
                    case KeyEvent.KEYCODE_C:
                        return onTextContextMenuItem(ID_COPY);
                    case KeyEvent.KEYCODE_V:
                        return onTextContextMenuItem(ID_PASTE);
                    case KeyEvent.KEYCODE_Z:
                        if (getCanUndo()) {
                            return onTextContextMenuItem(ID_UNDO);
                        }
                    case KeyEvent.KEYCODE_Y:
                        if (getCanRedo()) {
                            return onTextContextMenuItem(ID_REDO);
                        }
                    case KeyEvent.KEYCODE_S:
                        ((MainActivity) getContext()).saveTheFile(false);
                        return true;
                    default:
                        return super.onKeyDown(keyCode, event);
                }
            } else {
                switch (keyCode) {
                    case KeyEvent.KEYCODE_TAB:
                        String textToInsert = "  ";
                        int start, end;
                        start = Math.max(getSelectionStart(), 0);
                        end = Math.max(getSelectionEnd(), 0);
                        getText().replace(Math.min(start, end), Math.max(start, end),
                                textToInsert, 0, textToInsert.length());
                        return true;
                    default:
                        return super.onKeyDown(keyCode, event);
                }
            }
        }

        @Override
        public boolean onKeyUp(int keyCode, @NonNull KeyEvent event) {
            if (event.isCtrlPressed()) {
                switch (keyCode) {
                    case KeyEvent.KEYCODE_A:
                    case KeyEvent.KEYCODE_X:
                    case KeyEvent.KEYCODE_C:
                    case KeyEvent.KEYCODE_V:
                    case KeyEvent.KEYCODE_Z:
                    case KeyEvent.KEYCODE_Y:
                    case KeyEvent.KEYCODE_S:
                        return true;
                    default:
                        return false;
                }
            } else {
                switch (keyCode) {
                    case KeyEvent.KEYCODE_TAB:
                        return true;
                    default:
                        return false;
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean onTextContextMenuItem(
                final int id) {
            if (id == ID_UNDO) {
                undo();
                return true;
            } else if (id == ID_REDO) {
                redo();
                return true;
            } else {
                return super.onTextContextMenuItem(id);
            }
        }

        /**
         * Can undo be performed?
         */
        public boolean getCanUndo() {
            return (mEditHistory.mmPosition > 0);
        }

        /**
         * Can redo be performed?
         */
        public boolean getCanRedo() {
            return (mEditHistory.mmPosition
                    < mEditHistory.mmHistory.size());
        }

        /**
         * Perform undo.
         */
        public void undo() {
            EditItem edit = mEditHistory.getPrevious();
            if (edit == null) {
                return;
            }

            Editable text = getEditableText();
            int start = edit.mmStart;
            int end = start + (edit.mmAfter != null
                    ? edit.mmAfter.length() : 0);

            mIsUndoOrRedo = true;
            text.replace(start, end, edit.mmBefore);
            mIsUndoOrRedo = false;

            // This will get rid of underlines inserted when editor tries to come
            // up with a suggestion.
            for (Object o : text.getSpans(0,
                    text.length(), UnderlineSpan.class)) {
                text.removeSpan(o);
            }

            Selection.setSelection(text,
                    edit.mmBefore == null ? start
                            : (start + edit.mmBefore.length()));
        }

        /**
         * Perform redo.
         */
        public void redo() {
            EditItem edit = mEditHistory.getNext();
            if (edit == null) {
                return;
            }

            Editable text = getEditableText();
            int start = edit.mmStart;
            int end = start + (edit.mmBefore != null
                    ? edit.mmBefore.length() : 0);

            mIsUndoOrRedo = true;
            text.replace(start, end, edit.mmAfter);
            mIsUndoOrRedo = false;

            // This will get rid of underlines inserted when editor tries to come
            // up with a suggestion.
            for (Object o : text.getSpans(0,
                    text.length(), UnderlineSpan.class)) {
                text.removeSpan(o);
            }

            Selection.setSelection(text,
                    edit.mmAfter == null ? start
                            : (start + edit.mmAfter.length()));
        }

        /**
         * Set the maximum history size. If size is
         * negative, then history size is only limited
         * by the device memory.
         */
        public void setMaxHistorySize(
                int maxHistorySize) {
            mEditHistory.setMaxHistorySize(
                    maxHistorySize);
        }

        public void resetVariables() {
            mEditHistory.clear();
            enabledChangeListener = false;
            lineCount = 0;
            realLine = 0;
            startingLine = 0;
            mIsUndoOrRedo = false;
            mShowUndo = false;
            mShowRedo = false;
            canSaveFile = false;
            firstVisibleIndex = 0;
            firstColoredIndex = 0;
        }

        public boolean canSaveFile() {
            return canSaveFile;
        }

        public void fileSaved() {
            canSaveFile = false;
        }

        public void replaceTextKeepCursor(String textToUpdate) {

            int cursorPos;
            int cursorPosEnd;

            if (textToUpdate != null) {
                cursorPos = 0;
                cursorPosEnd = 0;
            } else {
                cursorPos = getSelectionStart();
                cursorPosEnd = getSelectionEnd();
            }

            disableTextChangedListener();

            if (PreferenceHelper.getSyntaxHighlight(getContext())) {
                setText(highlight(textToUpdate == null ? getEditableText() : Editable.Factory
                        .getInstance().newEditable(textToUpdate), textToUpdate != null));
            }
            else {
                setText(textToUpdate == null ? getEditableText() : textToUpdate);
            }

            enableTextChangedListener();

            boolean mantainCursorPos = cursorPos >= firstVisibleIndex && cursorPos <= lastVisibleIndex;

            if (mantainCursorPos)
                firstVisibleIndex = cursorPos;

            if (firstVisibleIndex > -1 && firstVisibleIndex < length()) {
                if (cursorPosEnd != cursorPos)
                    setSelection(cursorPos, cursorPosEnd);
                else
                    setSelection(firstVisibleIndex);
            }
        }
        //endregion

        //region UNDO REDO

        public void disableTextChangedListener() {
            enabledChangeListener = false;
            removeTextChangedListener(mChangeListener);
        }

        public Editable highlight(Editable editable, boolean newText) {
            editable.clearSpans();

            if (editable.length() == 0) {
                return editable;
            }

            editorHeight = getHeight();

            if (!newText && editorHeight > 0) {
                firstVisibleIndex = getLayout().getLineStart(lineUtils.getFirstVisibleLine(verticalScroll, editorHeight, lineCount));
                lastVisibleIndex = getLayout().getLineStart(lineUtils.getLastVisibleLine(verticalScroll, editorHeight, lineCount, deviceHeight));
            } else {
                firstVisibleIndex = 0;
                lastVisibleIndex = CHARS_TO_COLOR;
            }

            firstColoredIndex = firstVisibleIndex - (CHARS_TO_COLOR / 5);

            // normalize
            if (firstColoredIndex < 0)
                firstColoredIndex = 0;
            if (lastVisibleIndex > editable.length())
                lastVisibleIndex = editable.length();
            if (firstColoredIndex > lastVisibleIndex)
                firstColoredIndex = lastVisibleIndex;


            textToHighlight = editable.subSequence(firstColoredIndex, lastVisibleIndex);

            if (fileExtension.contains("htm")
                    || fileExtension.contains("xml")) {
                color(Patterns.HTML_TAGS, editable, textToHighlight, firstColoredIndex);
                color(Patterns.HTML_ATTRS, editable, textToHighlight, firstColoredIndex);
                color(Patterns.GENERAL_STRINGS, editable, textToHighlight, firstColoredIndex);
                color(Patterns.XML_COMMENTS, editable, textToHighlight, firstColoredIndex);
            } else if (fileExtension.equals("css")) {
                //color(CSS_STYLE_NAME, editable);
                color(Patterns.CSS_ATTRS, editable, textToHighlight, firstColoredIndex);
                color(Patterns.CSS_ATTR_VALUE, editable, textToHighlight, firstColoredIndex);
                color(Patterns.SYMBOLS, editable, textToHighlight, firstColoredIndex);
                color(Patterns.GENERAL_COMMENTS, editable, textToHighlight, firstColoredIndex);
            } else if (Arrays.asList(MimeTypes.MIME_CODE).contains(fileExtension)) {
                switch (fileExtension) {
                    case "lua":
                        color(Patterns.LUA_KEYWORDS, editable, textToHighlight, firstColoredIndex);
                        break;
                    case "py":
                        color(Patterns.PY_KEYWORDS, editable, textToHighlight, firstColoredIndex);
                        break;
                    default:
                        color(Patterns.GENERAL_KEYWORDS, editable, textToHighlight, firstColoredIndex);
                        break;
                }
                color(Patterns.NUMBERS_OR_SYMBOLS, editable, textToHighlight, firstColoredIndex);
                color(Patterns.GENERAL_STRINGS, editable, textToHighlight, firstColoredIndex);
                color(Patterns.GENERAL_COMMENTS, editable, textToHighlight, firstColoredIndex);
                if (fileExtension.equals("php"))
                    color(Patterns.PHP_VARIABLES, editable, textToHighlight, firstColoredIndex);
            } else if (Arrays.asList(MimeTypes.MIME_SQL).contains(fileExtension)) {
                color(Patterns.SYMBOLS, editable, textToHighlight, firstColoredIndex);
                color(Patterns.GENERAL_STRINGS, editable, textToHighlight, firstColoredIndex);
                color(Patterns.SQL_KEYWORDS, editable, textToHighlight, firstColoredIndex);
            } else {
                if (!(Arrays.asList(MimeTypes.MIME_MARKDOWN).contains(fileExtension)))
                    color(Patterns.GENERAL_KEYWORDS, editable, textToHighlight, firstColoredIndex);
                color(Patterns.NUMBERS_OR_SYMBOLS, editable, textToHighlight, firstColoredIndex);
                color(Patterns.GENERAL_STRINGS, editable, textToHighlight, firstColoredIndex);
                if (fileExtension.equals("prop") || fileExtension.contains("conf") ||
                        (Arrays.asList(MimeTypes.MIME_MARKDOWN).contains(fileExtension)))
                    color(Patterns.GENERAL_COMMENTS_NO_SLASH, editable, textToHighlight,
                            firstColoredIndex);
                else
                    color(Patterns.GENERAL_COMMENTS, editable, textToHighlight, firstColoredIndex);

                if ((Arrays.asList(MimeTypes.MIME_MARKDOWN).contains(fileExtension)))
                    color(Patterns.LINK, editable, textToHighlight, firstColoredIndex);
            }

            return editable;
        }

        public void enableTextChangedListener() {
            if (!enabledChangeListener) {
                addTextChangedListener(mChangeListener);
                enabledChangeListener = true;
            }
        }

        public LineUtils getLineUtils() {
            return lineUtils;
        }

        private void color(Pattern pattern,
                           Editable allText,
                           CharSequence textToHighlight,
                           int start) {
            int color = 0;
            if (pattern.equals(Patterns.HTML_TAGS)
                    || pattern.equals(Patterns.GENERAL_KEYWORDS)
                    || pattern.equals(Patterns.SQL_KEYWORDS)
                    || pattern.equals(Patterns.PY_KEYWORDS)
                    || pattern.equals(Patterns.LUA_KEYWORDS)

                    ) {
                color = getResources().getColor(R.color.syntax_keyword);
            } else if (pattern.equals(Patterns.HTML_ATTRS)
                    || pattern.equals(Patterns.CSS_ATTRS)
                    || pattern.equals(Patterns.LINK)) {
                color = getResources().getColor(R.color.syntax_attr);
            } else if (pattern.equals(Patterns.CSS_ATTR_VALUE)) {
                color = getResources().getColor(R.color.syntax_attr_value);
            } else if (pattern.equals(Patterns.XML_COMMENTS)
                    || pattern.equals(Patterns.GENERAL_COMMENTS)
                    || pattern.equals(Patterns.GENERAL_COMMENTS_NO_SLASH)) {
                color = getResources().getColor(R.color.syntax_comment);
            } else if (pattern.equals(Patterns.GENERAL_STRINGS)) {
                color = getResources().getColor(R.color.syntax_string);
            } else if (pattern.equals(Patterns.NUMBERS) || pattern.equals(Patterns.SYMBOLS) || pattern.equals(Patterns.NUMBERS_OR_SYMBOLS)) {
                color = getResources().getColor(R.color.syntax_number);
            } else if (pattern.equals(Patterns.PHP_VARIABLES)) {
                color = getResources().getColor(R.color.syntax_variable);
            }

            m = pattern.matcher(textToHighlight);

            while (m.find()) {
                allText.setSpan(
                        new ForegroundColorSpan(color),
                        start + m.start(),
                        start + m.end(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        /**
         * Clear history.
         */
        public void clearHistory() {
            mEditHistory.clear();
            mShowUndo = getCanUndo();
            mShowRedo = getCanRedo();
        }

        /**
         * Store preferences.
         */
        public void storePersistentState(
                SharedPreferences.Editor editor,
                String prefix) {
            // Store hash code of text in the editor so that we can check if the
            // editor contents has changed.
            editor.putString(prefix + ".hash",
                    String.valueOf(
                            getText().toString().hashCode()));
            editor.putInt(prefix + ".maxSize",
                    mEditHistory.mmMaxHistorySize);
            editor.putInt(prefix + ".position",
                    mEditHistory.mmPosition);
            editor.putInt(prefix + ".size",
                    mEditHistory.mmHistory.size());

            int i = 0;
            for (EditItem ei : mEditHistory.mmHistory) {
                String pre = prefix + "." + i;

                editor.putInt(pre + ".start", ei.mmStart);
                editor.putString(pre + ".before",
                        ei.mmBefore.toString());
                editor.putString(pre + ".after",
                        ei.mmAfter.toString());

                i++;
            }
        }

        /**
         * Restore preferences.
         *
         * @param prefix The preference key prefix
         *               used when state was stored.
         * @return did restore succeed? If this is
         * false, the undo history will be empty.
         */
        public boolean restorePersistentState(
                SharedPreferences sp, String prefix)
                throws IllegalStateException {

            boolean ok =
                    doRestorePersistentState(sp, prefix);
            if (!ok) {
                mEditHistory.clear();
            }

            return ok;
        }

        private boolean doRestorePersistentState(
                SharedPreferences sp, String prefix) {

            String hash =
                    sp.getString(prefix + ".hash", null);
            if (hash == null) {
                // No state to be restored.
                return true;
            }

            if (Integer.valueOf(hash)
                    != getText().toString().hashCode()) {
                return false;
            }

            mEditHistory.clear();
            mEditHistory.mmMaxHistorySize =
                    sp.getInt(prefix + ".maxSize", -1);

            int count = sp.getInt(prefix + ".size", -1);
            if (count == -1) {
                return false;
            }

            for (int i = 0; i < count; i++) {
                String pre = prefix + "." + i;

                int start = sp.getInt(pre + ".start", -1);
                String before =
                        sp.getString(pre + ".before", null);
                String after =
                        sp.getString(pre + ".after", null);

                if (start == -1
                        || before == null
                        || after == null) {
                    return false;
                }
                mEditHistory.add(
                        new EditItem(start, before, after));
            }

            mEditHistory.mmPosition =
                    sp.getInt(prefix + ".position", -1);
            return mEditHistory.mmPosition != -1;

        }

        /**
         * Class that listens to changes in the text.
         */
        private final class EditTextChangeListener
                implements TextWatcher {

            /**
             * The text that will be removed by the
             * change event.
             */
            private CharSequence mBeforeChange;

            /**
             * The text that was inserted by the change
             * event.
             */
            private CharSequence mAfterChange;

            public void beforeTextChanged(
                    CharSequence s, int start, int count,
                    int after) {
                if (mIsUndoOrRedo) {
                    return;
                }

                mBeforeChange =
                        s.subSequence(start, start + count);
            }

            public void onTextChanged(CharSequence s,
                                      int start, int before,
                                      int count) {
                if (mIsUndoOrRedo) {
                    return;
                }

                mAfterChange =
                        s.subSequence(start, start + count);
                mEditHistory.add(
                        new EditItem(start, mBeforeChange,
                                mAfterChange));
            }

            public void afterTextChanged(Editable s) {
                boolean showUndo = getCanUndo();
                boolean showRedo = getCanRedo();
                if (!canSaveFile)
                    canSaveFile = getCanUndo();
                if (showUndo != mShowUndo || showRedo != mShowRedo) {
                    mShowUndo = showUndo;
                    mShowRedo = showRedo;
                    ((MainActivity) getContext()).invalidateOptionsMenu();
                }

                ((MainActivity) getContext()).updateTextSyntax();
            }
        }

        //endregion

        //region EDIT HISTORY

        /**
         * Keeps track of all the edit history of a
         * text.
         */
        private final class EditHistory {

            /**
             * The list of edits in chronological
             * order.
             */
            private final LinkedList<EditItem>
                    mmHistory = new LinkedList<>();
            /**
             * The position from which an EditItem will
             * be retrieved when getNext() is called. If
             * getPrevious() has not been called, this
             * has the same value as mmHistory.size().
             */
            private int mmPosition = 0;
            /**
             * Maximum undo history size.
             */
            private int mmMaxHistorySize = -1;

            private int size() {
                return mmHistory.size();
            }

            /**
             * Clear history.
             */
            private void clear() {
                mmPosition = 0;
                mmHistory.clear();
            }

            /**
             * Adds a new edit operation to the history
             * at the current position. If executed
             * after a call to getPrevious() removes all
             * the future history (elements with
             * positions >= current history position).
             */
            private void add(EditItem item) {
                while (mmHistory.size() > mmPosition) {
                    mmHistory.removeLast();
                }
                mmHistory.add(item);
                mmPosition++;

                if (mmMaxHistorySize >= 0) {
                    trimHistory();
                }
            }

            /**
             * Trim history when it exceeds max history
             * size.
             */
            private void trimHistory() {
                while (mmHistory.size()
                        > mmMaxHistorySize) {
                    mmHistory.removeFirst();
                    mmPosition--;
                }

                if (mmPosition < 0) {
                    mmPosition = 0;
                }
            }

            /**
             * Set the maximum history size. If size is
             * negative, then history size is only
             * limited by the device memory.
             */
            private void setMaxHistorySize(
                    int maxHistorySize) {
                mmMaxHistorySize = maxHistorySize;
                if (mmMaxHistorySize >= 0) {
                    trimHistory();
                }
            }

            /**
             * Traverses the history backward by one
             * position, returns and item at that
             * position.
             */
            private EditItem getPrevious() {
                if (mmPosition == 0) {
                    return null;
                }
                mmPosition--;
                return mmHistory.get(mmPosition);
            }

            /**
             * Traverses the history forward by one
             * position, returns and item at that
             * position.
             */
            private EditItem getNext() {
                if (mmPosition >= mmHistory.size()) {
                    return null;
                }

                EditItem item = mmHistory.get(mmPosition);
                mmPosition++;
                return item;
            }
        }

        /**
         * Represents the changes performed by a
         * single edit operation.
         */
        private final class EditItem {
            private final int mmStart;
            private final CharSequence mmBefore;
            private final CharSequence mmAfter;

            /**
             * Constructs EditItem of a modification
             * that was applied at position start and
             * replaced CharSequence before with
             * CharSequence after.
             */
            public EditItem(int start,
                            CharSequence before, CharSequence after) {
                mmStart = start;
                mmBefore = before;
                mmAfter = after;
            }
        }
        //endregion


    }
}
