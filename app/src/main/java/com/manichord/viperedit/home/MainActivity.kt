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

package com.manichord.viperedit.home

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.provider.DocumentsContract
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.MenuItemCompat
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.ShareActionProvider
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProviders

import android.text.InputType
import android.text.TextUtils
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.HorizontalScrollView
import android.widget.ListView
import android.widget.Toast
import androidx.lifecycle.Observer

import org.apache.commons.io.FilenameUtils

import java.io.File
import java.io.FileNotFoundException
import java.io.UnsupportedEncodingException
import java.util.ArrayList
import java.util.Arrays
import java.util.LinkedList

import com.manichord.viperedit.R
import com.manichord.viperedit.files.OpenFileManager
import com.manichord.viperedit.files.SaveFileManager
import com.manichord.viperedit.markdown.MarkdownActivity
import com.manichord.viperedit.explorer.SelectFileActivity
import com.manichord.viperedit.adapter.AdapterDrawer
import com.manichord.viperedit.dialogfragment.ChangelogDialog
import com.manichord.viperedit.dialogfragment.EditTextDialog
import com.manichord.viperedit.dialogfragment.FileInfoDialog
import com.manichord.viperedit.dialogfragment.FindTextDialog
import com.manichord.viperedit.dialogfragment.NewFileDetailsDialog
import com.manichord.viperedit.dialogfragment.NumberPickerDialog
import com.manichord.viperedit.dialogfragment.SaveFileDialog
import com.manichord.viperedit.preferences.PreferenceChangeType
import com.manichord.viperedit.preferences.PreferenceHelper
import com.manichord.viperedit.home.texteditor.LineUtils
import com.manichord.viperedit.home.texteditor.PageSystem
import com.manichord.viperedit.home.texteditor.PageSystemButtons
import com.manichord.viperedit.home.texteditor.SearchResult
import com.manichord.viperedit.util.AccessStorageApi
import com.manichord.viperedit.util.AccessoryView
import com.manichord.viperedit.util.AnimationUtils
import com.manichord.viperedit.util.AppInfoHelper
import com.manichord.viperedit.util.Device
import com.manichord.viperedit.util.GreatUri
import com.manichord.viperedit.util.MimeTypes
import com.manichord.viperedit.util.ProCheckUtils
import com.manichord.viperedit.util.ThemeUtils
import com.manichord.viperedit.util.ViewUtils
import com.manichord.viperedit.views.CustomDrawerLayout
import com.manichord.viperedit.views.DialogHelper
import com.manichord.viperedit.views.GoodScrollView
import java.nio.charset.Charset

abstract class MainActivity : AppCompatActivity(), FindTextDialog.SearchDialogInterface, GoodScrollView.ScrollInterface, PageSystem.PageSystemInterface, PageSystemButtons.PageButtonsInterface, NumberPickerDialog.INumberPickerDialog, SaveFileDialog.ISaveDialog, AdapterView.OnItemClickListener, AdapterDrawer.Callbacks, AccessoryView.IAccessoryView, EditTextDialog.EditDialogListener {
    private val updateHandler = Handler()
    private val colorRunnable_duringEditing = Runnable { mEditor!!.replaceTextKeepCursor(null) }
    private val colorRunnable_duringScroll = Runnable { mEditor!!.replaceTextKeepCursor(null) }
    private var fileOpened = false
    /*
     * This class provides a handy way to tie together the functionality of
     * {@link DrawerLayout} and the framework <code>ActionBar</code> to implement the recommended
     * design for navigation drawers.
     */
    private var mDrawerToggle: ActionBarDrawerToggle? = null
    /*
     * The Drawer Layout
     */
    private var mDrawerLayout: CustomDrawerLayout? = null
    private var mEditor: Editor? = null
    private var horizontalScroll: HorizontalScrollView? = null
    private var pageSystemButtons: PageSystemButtons? = null
    private var toolbar: Toolbar? = null

    internal lateinit var progressDialog: ProgressDialog

    /*
    Navigation Drawer
     */
    private var arrayAdapter: AdapterDrawer? = null
    private var greatUris: LinkedList<GreatUri>? = null
    //endregion

    private var viewModel: MainViewModel? = null

    //region Activity facts

    override fun onCreate(savedInstanceState: Bundle?) {
        // set the windows background
        ThemeUtils.setWindowsBackground(this)
        // super!!
        super.onCreate(savedInstanceState)
        // setup the layout
        setContentView(R.layout.activity_home)

        val factory = MainViewModelFactory(
                OpenFileManager(this),
                SaveFileManager(this)
        )
        viewModel = ViewModelProviders.of(this, factory).get(MainViewModel::class.java)

        toolbar = findViewById(R.id.my_awesome_toolbar)
        setSupportActionBar(toolbar)
        // setup the navigation drawer
        setupNavigationDrawer()
        // reset text editor
        setupTextEditor()
        hideTextEditor()
        /* First Time we open this activity */
        if (savedInstanceState == null) {
            // Open
            mDrawerLayout!!.openDrawer(GravityCompat.START)
            // Set the default title
            supportActionBar!!.title = getString(R.string.nome_app_turbo_editor)
        }
        // parse the intent
        parseIntent(intent)
        // show a dialog with the changelog
        showChangeLog()

        viewModel?.openFileLiveData?.observe(this, Observer { item ->
            when (item) {
                OpenFileState.OpenFileStartState -> {
                    mDrawerLayout!!.closeDrawer(GravityCompat.START)
                    progressDialog = ProgressDialog(this@MainActivity)
                    progressDialog.setMessage(getString(R.string.please_wait))
                    progressDialog.show()
                }

                is OpenFileState.FileLoadedState -> {
                    progressDialog.hide()

                    pageSystem = PageSystem(this@MainActivity, this@MainActivity, item.fileText)
                    //                    viewModel.currentEncoding = encoding; TODO

                    aFileWasSelected(viewModel!!.greatUri)

                    showTextEditor()

                    if (item.fileName.isEmpty())
                        supportActionBar!!.setTitle(R.string.new_file)
                    else
                        supportActionBar!!.title = item.fileName

                    if (viewModel!!.greatUri != null) {
                        refreshList(viewModel!!.greatUri, add = true, delete = false)
                    }
                }
            }
        })

        viewModel?.saveFileLiveData?.observe(this, Observer { item ->
            when (item) {
                is SaveFileState.Success -> {
                    Toast.makeText(this, String.format(getString(R.string.file_saved_with_success), item.fileName), Toast.LENGTH_SHORT).show()
                    savedAFile(viewModel?.greatUri, true)
                }
                is SaveFileState.SuccessAndOpen -> {
                    savedAFile(viewModel?.greatUri, false)
                    newFileToOpen(viewModel?.greatUri, "")
                }
                SaveFileState.Failed -> Toast.makeText(this, getString(R.string.err_occured), Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        mDrawerToggle!!.syncState()
    }

    public override fun onResume() {
        super.onResume()
        // Refresh the list view
        refreshList()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        parseIntent(intent)
    }

    public override fun onPause() {
        super.onPause()

        if (PreferenceHelper.getAutoSave(baseContext) && mEditor!!.canSaveFile()) {
            saveTheFile(false)
            mEditor!!.fileSaved() // so it doesn't ask to save in onDetach
        }
    }

    override fun onDestroy() {
        try {
            closeKeyBoard()
        } catch (e: NullPointerException) {
            e.printStackTrace()
        }

        super.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        mDrawerToggle!!.onConfigurationChanged(newConfig)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            onBackPressed()
            return true
        } else if (keyCode == KeyEvent.KEYCODE_MENU) {
            return false
        } else {
            if (mEditor == null)
                mEditor = findViewById(R.id.editor)

            // this will happen on first key pressed on hard-keyboard only. Once myInputField
            // gets the focus again, it will automatically receive further key presses.

            if (fileOpened && mEditor != null && !mEditor!!.hasFocus()) {
                mEditor!!.requestFocus()
                mEditor!!.onKeyDown(keyCode, event)
                return true
            }
        }

        return false
    }

    override fun onBackPressed() {

        try {
            // if we should ignore the back button
            if (PreferenceHelper.getIgnoreBackButton(this))
                return

            if (mDrawerLayout!!.isDrawerOpen(GravityCompat.START) && fileOpened) {
                mDrawerLayout!!.closeDrawer(GravityCompat.START)
            } else if (mDrawerLayout!!.isDrawerOpen(GravityCompat.END) && fileOpened) {
                mDrawerLayout!!.closeDrawer(GravityCompat.END)
            } else if (fileOpened && mEditor!!.canSaveFile()) {
                SaveFileDialog.create(viewModel?.greatUri, pageSystem?.getAllText(mEditor!!.text.toString()), viewModel?.currentEncoding).show(supportFragmentManager, "dialog")
            } else if (fileOpened) {

                // remove editor fragment
                hideTextEditor()

                // Set the default title
                supportActionBar!!.title = getString(R.string.nome_app_turbo_editor)

                closedTheFile()

                mDrawerLayout!!.openDrawer(GravityCompat.START)
                mDrawerLayout!!.closeDrawer(GravityCompat.END)
            } else {
                super.onBackPressed()
            }
        } catch (e: Exception) {
            // maybe something is null, who knows
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == SELECT_FILE_CODE) {

                val data = intent!!.data
                val newUri = GreatUri(data, AccessStorageApi.getPath(this, data), AccessStorageApi.getName(this, data))

                newFileToOpen(newUri, "")
            } else {

                val data = intent!!.data
                val newUri = GreatUri(data, AccessStorageApi.getPath(this, data), AccessStorageApi.getName(this, data))

                // grantUriPermission(getPackageName(), data, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                val takeFlags = intent.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                // Check for the freshest data.
                contentResolver.takePersistableUriPermission(data!!, takeFlags)

                if (requestCode == READ_REQUEST_CODE || requestCode == CREATE_REQUEST_CODE) {

                    newFileToOpen(newUri, "")
                }

                if (requestCode == SAVE_AS_REQUEST_CODE) {
                    viewModel?.saveFileAndOpen(newUri, pageSystem!!.getAllText(mEditor!!.text!!.toString()), viewModel!!.currentEncoding!!)
                }
            }
        }
    }

    private fun forceUseStorageAccessFramework() {
        PreferenceHelper.setUseStorageAccessFramework(this, true)
        val swStorageAccessFramework = findViewById<View>(R.id.switch_storage_access_framework) as SwitchCompat
        swStorageAccessFramework.isChecked = true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_WRITE_STORAGE_PERMISSION -> if (grantResults.size == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                // Permission denied.
                Toast.makeText(this, R.string.storage_required, Toast.LENGTH_LONG).show()
                forceUseStorageAccessFramework()
            } else {
                PreferenceHelper.setUseStorageAccessFramework(this, false)
            }
        }
    }


    override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        // Send the event that a file was selected
        newFileToOpen(greatUris!![position], "")
    }

    //endregion

    //region MENU

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (fileOpened && searchResult != null)
            menuInflater.inflate(R.menu.fragment_editor_search, menu)
        else if (fileOpened)
            menuInflater.inflate(R.menu.fragment_editor, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {

        if (fileOpened && searchResult != null) {
            val imReplace = menu.findItem(R.id.im_replace)
            val imReplaceAll = menu.findItem(R.id.im_replace_all)
            val imPrev = menu.findItem(R.id.im_previous_item)
            val imNext = menu.findItem(R.id.im_next_item)

            if (imReplace != null)
                imReplace.isVisible = searchResult!!.canReplaceSomething()

            if (imReplaceAll != null)
                imReplaceAll.isVisible = searchResult!!.canReplaceSomething()

            if (imPrev != null)
                imPrev.isVisible = searchResult!!.hasPrevious()

            if (imNext != null)
                imNext.isVisible = searchResult!!.hasNext()


        } else if (fileOpened) {
            val imSave = menu.findItem(R.id.im_save)
            val imUndo = menu.findItem(R.id.im_undo)
            val imRedo = menu.findItem(R.id.im_redo)

            if (mEditor != null) {
                if (imSave != null)
                    imSave.isVisible = mEditor!!.canSaveFile()
                if (imUndo != null)
                    imUndo.isVisible = mEditor!!.canUndo
                if (imRedo != null)
                    imRedo.isVisible = mEditor!!.canRedo
            } else {
                imSave!!.isVisible = false
                imUndo!!.isVisible = false
                imRedo!!.isVisible = false
            }

            val imMarkdown = menu.findItem(R.id.im_view_markdown)
            val isMarkdown = Arrays.asList(*MimeTypes.MIME_MARKDOWN).contains(FilenameUtils.getExtension(viewModel!!.greatUri!!.fileName))
            if (imMarkdown != null)
                imMarkdown.isVisible = isMarkdown

            val imShare = menu.findItem(R.id.im_share)
            if (imMarkdown != null) {
                val shareAction = MenuItemCompat
                        .getActionProvider(imShare) as ShareActionProvider
                val shareIntent = Intent()
                shareIntent.action = Intent.ACTION_SEND
                shareIntent.putExtra(Intent.EXTRA_STREAM, viewModel!!.greatUri!!.uri)
                shareIntent.type = "text/plain"
                shareAction.setShareIntent(shareIntent)
            }
        }

        val imDonate = menu.findItem(R.id.im_donate)
        if (imDonate != null)
            if (ProCheckUtils.isPro(this, false))
                imDonate.isVisible = false

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val i = item.itemId
        if (mDrawerToggle!!.onOptionsItemSelected(item)) {
            Toast.makeText(baseContext, "drawer click", Toast.LENGTH_SHORT).show()
            mDrawerLayout!!.closeDrawer(GravityCompat.END)
            return true
        } else if (i == R.id.im_save_normaly) {
            saveTheFile(false)

        } else if (i == R.id.im_save_as) {
            saveTheFile(true)

        } else if (i == R.id.im_rename) {
            EditTextDialog.newInstance(EditTextDialog.Actions.Rename, viewModel!!.greatUri!!.fileName).show(fragmentManager.beginTransaction(), "dialog")
        } else if (i == R.id.im_undo) {
            mEditor!!.onTextContextMenuItem(ID_UNDO)

        } else if (i == R.id.im_redo) {
            mEditor!!.onTextContextMenuItem(ID_REDO)

        } else if (i == R.id.im_search) {
            FindTextDialog.newInstance(mEditor!!.text!!.toString()).show(fragmentManager
                    .beginTransaction(), "dialog")
        } else if (i == R.id.im_cancel) {
            searchResult = null
            invalidateOptionsMenu()

        } else if (i == R.id.im_replace) {
            replaceText(false)

        } else if (i == R.id.im_replace_all) {
            replaceText(true)

        } else if (i == R.id.im_next_item) {
            nextResult()

        } else if (i == R.id.im_previous_item) {
            previousResult()

        } else if (i == R.id.im_goto_line) {
            val min = mEditor!!.lineUtils.firstReadLine()
            val max = mEditor!!.lineUtils.lastReadLine()
            NumberPickerDialog.newInstance(NumberPickerDialog.Actions.GoToLine, min, min, max).show(fragmentManager.beginTransaction(), "dialog")
        } else if (i == R.id.im_view_it_on_browser) {
            val browserIntent: Intent
            try {
                browserIntent = Intent(Intent.ACTION_VIEW)
                browserIntent.setDataAndType(viewModel!!.greatUri!!.uri, "*/*")
                startActivity(browserIntent)
            } catch (ex2: ActivityNotFoundException) {
                //
            }

        } else if (i == R.id.im_view_markdown) {
            val browserIntent = Intent(this@MainActivity, MarkdownActivity::class.java)
            browserIntent.putExtra("text", pageSystem!!.getAllText(mEditor!!.text!!.toString()))
            startActivity(browserIntent)
        } else if (i == R.id.im_info) {
            FileInfoDialog.newInstance(viewModel!!.greatUri!!.uri).show(fragmentManager.beginTransaction(), "dialog")
        } else if (i == R.id.im_donate) {
            val appPackageName = "com.maskyn.fileeditorpro"
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appPackageName")))
            } catch (anfe: android.content.ActivityNotFoundException) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")))
            }

        }
        return super.onOptionsItemSelected(item)
    }
    //endregion

    // region OTHER THINGS
    internal fun replaceText(all: Boolean) {
        if (all) {
            if (searchResult!!.isRegex)
                mEditor!!.setText(pageSystem!!.getAllText(mEditor!!.text!!.toString()).replace(searchResult!!.whatToSearch.toRegex(), searchResult!!.textToReplace))
            else
                mEditor!!.setText(pageSystem!!.getAllText(mEditor!!.text!!.toString()).replace(searchResult!!.whatToSearch, searchResult!!.textToReplace))

            searchResult = null
            invalidateOptionsMenu()
        } else {
            val start = searchResult!!.foundIndex[searchResult!!.index]
            val end = start + searchResult!!.textLength
            mEditor!!.text = mEditor!!.text!!.replace(start, end, searchResult!!.textToReplace)
            searchResult!!.doneReplace()

            invalidateOptionsMenu()

            if (searchResult!!.hasNext())
                nextResult()
            else if (searchResult!!.hasPrevious())
                previousResult()
        }
    }

    internal fun nextResult() {
        if (searchResult!!.index == mEditor!!.lineCount - 1)
        // last result of page
        {
            return
        }


        if (searchResult!!.index < searchResult!!.numberOfResults() - 1) { // equal zero is not good
            searchResult!!.index++
            val line = LineUtils.getLineFromIndex(searchResult!!.foundIndex[searchResult!!.index], mEditor!!.lineCount, mEditor!!.layout)


            verticalScroll.post {
                var y = mEditor!!.layout.getLineTop(line)
                if (y > 100)
                    y -= 100
                else
                    y = 0

                verticalScroll.scrollTo(0, y)
            }

            mEditor!!.isFocusable = true
            mEditor!!.requestFocus()
            mEditor!!.setSelection(searchResult!!.foundIndex[searchResult!!.index],
                    searchResult!!.foundIndex[searchResult!!.index] + searchResult!!.textLength)
        }

        invalidateOptionsMenu()
    }

    internal fun previousResult() {
        if (searchResult!!.index == 0)
            return
        if (searchResult!!.index > 0) {
            searchResult!!.index--
            val line = LineUtils.getLineFromIndex(searchResult!!.foundIndex[searchResult!!.index], mEditor!!.lineCount, mEditor!!.layout)
            verticalScroll.post {
                var y = mEditor!!.layout.getLineTop(line)
                if (y > 100)
                    y -= 100
                else
                    y = 0
                verticalScroll.scrollTo(0, y)
            }

            mEditor!!.isFocusable = true
            mEditor!!.requestFocus()
            mEditor!!.setSelection(searchResult!!.foundIndex[searchResult!!.index],
                    searchResult!!.foundIndex[searchResult!!.index] + searchResult!!.textLength)
        }

        invalidateOptionsMenu()
    }

    private fun useStorageAccessFramework(): Boolean {
        if (!Device.hasKitKatApi())
            return false
        val pref = PreferenceHelper.getUseStorageAccessFramework(this)
        if (pref)
            return true
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            forceUseStorageAccessFramework()
            return true
        }
        return false
    }

    fun saveTheFile(saveAs: Boolean) {
        if (!saveAs && viewModel!!.greatUri != null && viewModel!!.greatUri!!.uri != null && viewModel!!.greatUri!!.uri !== Uri.EMPTY)
            viewModel?.saveFile(viewModel!!.greatUri!!, pageSystem!!.getAllText(mEditor!!.text!!
                    .toString()), viewModel!!.currentEncoding!!)
        else {
            if (useStorageAccessFramework()) {
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                intent.type = "*/*"
                intent.putExtra(Intent.EXTRA_TITLE, viewModel!!.greatUri!!.fileName)
                startActivityForResult(intent, SAVE_AS_REQUEST_CODE)
            } else {
                NewFileDetailsDialog(
                        viewModel!!.greatUri!!,
                        pageSystem!!.getAllText(mEditor!!.text!!.toString()),
                        viewModel!!.currentEncoding!!
                ).show(supportFragmentManager, "dialog")
            }
        }
    }

    /**
     * Setup the navigation drawer
     */
    private fun setupNavigationDrawer() {
        mDrawerLayout = findViewById(R.id.drawer_layout)
        /* Action Bar
        final ActionBar ab = toolbar;
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setHomeButtonEnabled(true);*/
        /* Navigation drawer */
        mDrawerToggle = object : ActionBarDrawerToggle(
                this,
                mDrawerLayout,
                toolbar,
                R.string.nome_app_turbo_editor,
                R.string.nome_app_turbo_editor) {

            override fun onDrawerOpened(drawerView: View) {
                supportInvalidateOptionsMenu()
                try {
                    closeKeyBoard()
                } catch (e: NullPointerException) {
                    e.printStackTrace()
                }

            }

            override fun onDrawerClosed(view: View) {
                supportInvalidateOptionsMenu()
            }
        }
        /* link the mDrawerToggle to the Drawer Layout */
        mDrawerLayout!!.setDrawerListener(mDrawerToggle)
        //mDrawerLayout.setFocusableInTouchMode(false);

        val listView = findViewById<ListView>(android.R.id.list)
        listView.emptyView = findViewById(android.R.id.empty)
        greatUris = LinkedList()
        arrayAdapter = AdapterDrawer(this, greatUris, this)
        listView.adapter = arrayAdapter
        listView.onItemClickListener = this
    }

    private fun setupTextEditor() {

        verticalScroll = findViewById(R.id.vertical_scroll)
        horizontalScroll = findViewById(R.id.horizontal_scroll)
        mEditor = findViewById(R.id.editor)

        val accessoryView = findViewById<AccessoryView>(R.id.accessoryView)
        accessoryView.setInterface(this)

        val parentAccessoryView = findViewById<HorizontalScrollView>(R.id.parent_accessory_view)
        ViewUtils.setVisible(parentAccessoryView, PreferenceHelper.getUseAccessoryView(this))


        if (PreferenceHelper.getWrapContent(this)) {
            horizontalScroll!!.removeView(mEditor)
            verticalScroll.removeView(horizontalScroll)
            verticalScroll.addView(mEditor)
        }

        verticalScroll.setScrollInterface(this)

        pageSystem = PageSystem(this, this, "")

        pageSystemButtons = PageSystemButtons(this, this,
                findViewById(R.id.fabPrev),
                findViewById(R.id.fabNext))

        mEditor!!.setupEditor()
    }

    private fun showTextEditor() {

        fileOpened = true

        findViewById<View>(R.id.text_editor).visibility = View.VISIBLE
        findViewById<View>(R.id.no_file_opened_messagge).visibility = View.GONE

        mEditor!!.resetVariables()
        searchResult = null

        invalidateOptionsMenu()

        mEditor!!.disableTextChangedListener()
        mEditor!!.replaceTextKeepCursor(pageSystem!!.currentPageText)
        mEditor!!.enableTextChangedListener()
    }

    private fun hideTextEditor() {

        fileOpened = false

        try {
            findViewById<View>(R.id.text_editor).visibility = View.GONE
            findViewById<View>(R.id.no_file_opened_messagge).visibility = View.VISIBLE

            mEditor!!.disableTextChangedListener()
            mEditor!!.replaceTextKeepCursor("")
            mEditor!!.enableTextChangedListener()
        } catch (e: Exception) {
            // lol
        }

    }

    /**
     * Parses the intent
     */
    private fun parseIntent(intent: Intent) {
        val action = intent.action
        val type = intent.type

        if (Intent.ACTION_VIEW == action
                || Intent.ACTION_EDIT == action
                || Intent.ACTION_PICK == action && type != null) {
            // Post event
            //newFileToOpen(new File(intent
            //        .getData().getPath()), "");
            val uri = intent.data
            val newUri = GreatUri(uri, AccessStorageApi.getPath(this, uri), AccessStorageApi.getName(this, uri))
            newFileToOpen(newUri, "")
        } else if (Intent.ACTION_SEND == action && type != null) {
            if ("text/plain" == type) {
                newFileToOpen(GreatUri(Uri.EMPTY, "", ""), intent.getStringExtra(Intent.EXTRA_TEXT))
            }
        }
    }

    /**
     * Show a dialog with the changelog
     */
    private fun showChangeLog() {
        val currentVersion = AppInfoHelper.getCurrentVersion(this)
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val lastVersion = preferences.getString("last_version", currentVersion)
        preferences.edit().putString("last_version", currentVersion).apply()
        if (lastVersion != currentVersion) {
            ChangelogDialog.showChangeLogDialog(fragmentManager)
        }
    }

    // closes the soft keyboard
    @Throws(NullPointerException::class)
    private fun closeKeyBoard() {
        // Central system API to the overall input method framework (IMF) architecture
        val inputManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        // Base interface for a remotable object
        val windowToken = currentFocus!!.windowToken

        // Hide type
        val hideType = InputMethodManager.HIDE_NOT_ALWAYS

        // Hide the KeyBoard
        inputManager.hideSoftInputFromWindow(windowToken, hideType)
    }

    fun updateTextSyntax() {
        if (!PreferenceHelper.getSyntaxHighlight(this) || mEditor!!.hasSelection() ||
                updateHandler == null || colorRunnable_duringEditing == null)
            return

        updateHandler.removeCallbacks(colorRunnable_duringEditing)
        updateHandler.removeCallbacks(colorRunnable_duringScroll)
        updateHandler.postDelayed(colorRunnable_duringEditing, SYNTAX_DELAY_MILLIS_LONG.toLong())
    }

    private fun refreshList(thisUri: GreatUri? = null, add: Boolean = false, delete: Boolean = false) {
        var max_recent_files = 15
        if (add)
            max_recent_files--

        // File paths saved in preferences
        var savedPaths = PreferenceHelper.getSavedPaths(this)
        val first_index_of_array = if (savedPaths.size > max_recent_files) savedPaths.size - max_recent_files else 0
        savedPaths = Arrays.copyOfRange(savedPaths, first_index_of_array, savedPaths.size)
        // File names for the list
        greatUris!!.clear()
        // StringBuilder that will contain the file paths
        val sb = StringBuilder()

        // for cycle to convert paths to names
        for (i in savedPaths.indices) {
            val particularUri = Uri.parse(savedPaths[i])
            val name = AccessStorageApi.getName(this, particularUri)
            // Check that the file exist
            // if is null or empty the particular url we dont use it
            if (particularUri != null && particularUri != Uri.EMPTY && !TextUtils.isEmpty(name)) {
                // if the particular uri is good
                var good = false
                if (thisUri == null || thisUri.uri == null || thisUri.uri === Uri.EMPTY)
                    good = true
                else {
                    good = if (!delete)
                        true
                    else thisUri.uri != particularUri
                }
                if (good) {
                    greatUris!!.addFirst(GreatUri(particularUri, AccessStorageApi.getPath(this, particularUri), name))
                    sb.append(savedPaths[i]).append(",")
                }
            }
            //}
        }
        // if is not null, empty, we have to add something and we dont already have this uri
        if (thisUri != null && thisUri.uri != Uri.EMPTY && add && !Arrays.asList(*savedPaths).contains(thisUri.uri!!.toString())) {
            sb.append(thisUri.uri!!.toString()).append(",")
            greatUris!!.addFirst(thisUri)
        }
        // save list without empty or non existed files
        PreferenceHelper.setSavedPaths(this, sb)
        // Set adapter
        arrayAdapter!!.notifyDataSetChanged()
    }
    //endregion

    //region EVENTBUS
    internal fun newFileToOpen(newUri: GreatUri?, newFileText: String?) {

        if (fileOpened && mEditor != null && mEditor!!.canSaveFile() && viewModel!!.greatUri != null && pageSystem != null && viewModel!!.currentEncoding != null) {
            SaveFileDialog.create(viewModel!!.greatUri, pageSystem!!.getAllText(mEditor!!
                    .text!!.toString()), viewModel!!.currentEncoding, true, newUri!!)
                    .show(supportFragmentManager, "dialog")
            return
        }

        viewModel?.openFile(newUri, newFileText)
    }

    fun savedAFile(uri: GreatUri?, updateList: Boolean) {

        if (uri != null) {

            //            greatUri = uri;

            val name = uri.fileName
            fileExtension = FilenameUtils.getExtension(name).toLowerCase()
            toolbar!!.title = name

            if (updateList) {
                refreshList(uri, true, false)
                arrayAdapter!!.selectPosition(uri)
            }
        }

        mEditor!!.clearHistory()
        mEditor!!.fileSaved()
        invalidateOptionsMenu()

        try {
            closeKeyBoard()
        } catch (e: NullPointerException) {
            e.printStackTrace()
        }

    }

    /**
     * When a file can't be opened
     * Invoked by the EditorFragment
     *
     * @param event The event called
     */
    internal fun cannotOpenFile() {
        //
        mDrawerLayout!!.openDrawer(GravityCompat.START)
        //
        supportActionBar!!.title = getString(R.string.nome_app_turbo_editor)
        //
        supportInvalidateOptionsMenu()
        // Replace fragment
        hideTextEditor()
    }

    fun aPreferenceValueWasChanged(type: PreferenceChangeType) {
        this.aPreferenceValueWasChanged(object : ArrayList<PreferenceChangeType>() {
            init {
                add(type)
            }
        })
    }

    internal fun aPreferenceValueWasChanged(types: List<PreferenceChangeType>) {

        if (types.contains(PreferenceChangeType.THEME_CHANGE)) {
            ThemeUtils.setWindowsBackground(this)
            val accessoryView = findViewById<AccessoryView>(R.id.accessoryView)
            accessoryView.updateTextColors()
        }

        if (types.contains(PreferenceChangeType.WRAP_CONTENT)) {
            if (PreferenceHelper.getWrapContent(this)) {
                horizontalScroll!!.removeView(mEditor)
                verticalScroll.removeView(horizontalScroll)
                verticalScroll.addView(mEditor)
            } else {
                verticalScroll.removeView(mEditor)
                verticalScroll.addView(horizontalScroll)
                horizontalScroll!!.addView(mEditor)
            }
        } else if (types.contains(PreferenceChangeType.LINE_NUMERS)) {
            mEditor!!.disableTextChangedListener()
            mEditor!!.replaceTextKeepCursor(null)
            mEditor!!.enableTextChangedListener()
            mEditor!!.updatePadding()
        } else if (types.contains(PreferenceChangeType.SYNTAX)) {
            mEditor!!.disableTextChangedListener()
            mEditor!!.replaceTextKeepCursor(mEditor!!.text!!.toString())
            mEditor!!.enableTextChangedListener()
        } else if (types.contains(PreferenceChangeType.MONOSPACE)) {
            if (PreferenceHelper.getUseMonospace(this))
                mEditor!!.setTypeface(Typeface.MONOSPACE)
            else
                mEditor!!.setTypeface(Typeface.DEFAULT)
        } else if (types.contains(PreferenceChangeType.THEME_CHANGE)) {
            if (PreferenceHelper.getLightTheme(this)) {
                mEditor!!.setTextColor(resources.getColor(R.color.textColorInverted))
            } else {
                mEditor!!.setTextColor(resources.getColor(R.color.textColor))
            }
        } else if (types.contains(PreferenceChangeType.TEXT_SUGGESTIONS) || types.contains(PreferenceChangeType.READ_ONLY)) {
            if (PreferenceHelper.getReadOnly(this)) {
                mEditor!!.setReadOnly(true)
            } else {
                mEditor!!.setReadOnly(false)
                if (PreferenceHelper.getSuggestionActive(this)) {
                    mEditor!!.inputType = InputType.TYPE_CLASS_TEXT or InputType
                            .TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE
                } else {
                    mEditor!!.inputType = (InputType.TYPE_CLASS_TEXT or InputType
                            .TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                            or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD or InputType
                            .TYPE_TEXT_FLAG_IME_MULTI_LINE)
                }
            }
            // sometimes it becomes monospace after setting the input type
            if (PreferenceHelper.getUseMonospace(this))
                mEditor!!.setTypeface(Typeface.MONOSPACE)
            else
                mEditor!!.setTypeface(Typeface.DEFAULT)
        } else if (types.contains(PreferenceChangeType.FONT_SIZE)) {
            mEditor!!.updatePadding()
            mEditor!!.textSize = PreferenceHelper.getFontSize(this).toFloat()
        } else if (types.contains(PreferenceChangeType.ACCESSORY_VIEW)) {
            val parentAccessoryView = findViewById<HorizontalScrollView>(R.id.parent_accessory_view)
            ViewUtils.setVisible(parentAccessoryView, PreferenceHelper.getUseAccessoryView(this))
            mEditor!!.updatePadding()
        } else if (types.contains(PreferenceChangeType.ENCODING)) {
            val oldEncoding: String = viewModel!!.currentEncoding!!
            val newEncoding: String = PreferenceHelper.getEncoding(this)
            try {
                val oldText = mEditor!!.text!!.toString().toByteArray(charset(oldEncoding))
                mEditor!!.disableTextChangedListener()
                mEditor!!.replaceTextKeepCursor(String(oldText, Charset.forName(newEncoding)))
                mEditor!!.enableTextChangedListener()
                //                currentEncoding = newEncoding; TODO
            } catch (ignored: UnsupportedEncodingException) {
                try {
                    val oldText = mEditor!!.text!!.toString().toByteArray(charset(oldEncoding))
                    mEditor!!.disableTextChangedListener()
                    mEditor!!.replaceTextKeepCursor(String(oldText, Charset.forName("UTF-16")))
                    mEditor!!.enableTextChangedListener()
                } catch (ignored2: UnsupportedEncodingException) {
                }

            }

        }
    }

    internal fun aFileWasSelected(uri: GreatUri?) {
        arrayAdapter!!.selectPosition(uri)
    }

    internal fun closedTheFile() {
        arrayAdapter!!.selectPosition(GreatUri(Uri.EMPTY, "", ""))
    }
    //endregion

    //region Calls from the layout
    fun OpenFolder(view: View) {
        if (useStorageAccessFramework()) {
            // ACTION_OPEN_DOCUMENT_TREE is the intent to choose a file via the system's file
            // browser.
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)

            // Filter to only show results that can be "opened", such as a
            // file (as opposed to a list of contacts or timezones)
            intent.addCategory(Intent.CATEGORY_OPENABLE)

            // Filter to show only images, using the image MIME data type.
            // If one wanted to search for ogg vorbis files, the type would be "audio/ogg".
            // To search for all documents available via installed storage providers,
            // it would be "*/*".
            intent.type = "*/*"

            startActivityForResult(intent, READ_REQUEST_CODE)
        } else {
            val intent = SelectFileActivity.CreateSelectFileActivityIntnet(this, SelectFileActivity.Action.SelectFolder)
            AnimationUtils.startActivityWithScale(this, intent, true, SELECT_FILE_CODE, view)
        }
    }

    fun OpenFile(view: View) {

        if (useStorageAccessFramework()) {
            // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
            // browser.
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)

            // Filter to only show results that can be "opened", such as a
            // file (as opposed to a list of contacts or timezones)
            intent.addCategory(Intent.CATEGORY_OPENABLE)

            // Filter to show only images, using the image MIME data type.
            // If one wanted to search for ogg vorbis files, the type would be "audio/ogg".
            // To search for all documents available via installed storage providers,
            // it would be "*/*".
            intent.type = "*/*"

            startActivityForResult(intent, READ_REQUEST_CODE)
        } else {
            val intent = SelectFileActivity.CreateSelectFileActivityIntnet(this, SelectFileActivity.Action.SelectFile)
            AnimationUtils.startActivityWithScale(this, intent, true, SELECT_FILE_CODE, view)
        }
    }

    fun CreateFile(view: View) {
        if (useStorageAccessFramework()) {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
            intent.type = "*/*"
            //intent.putExtra(Intent.EXTRA_TITLE, ".txt");
            startActivityForResult(intent, CREATE_REQUEST_CODE)
        } else {
            newFileToOpen(GreatUri(Uri.EMPTY, "", ""), "")
        }
    }

    fun OpenInfo(view: View) {
        DialogHelper.showAboutDialog(this)
    }

    fun OpenSettings(view: View) {
        mDrawerLayout!!.closeDrawer(GravityCompat.START)
        mDrawerLayout!!.openDrawer(GravityCompat.END)
    }
    //endregion

    //region Ovverideses
    override fun nextPageClicked() {
        pageSystem!!.savePage(mEditor!!.text!!.toString())
        pageSystem!!.nextPage()
        mEditor!!.disableTextChangedListener()
        mEditor!!.replaceTextKeepCursor(pageSystem!!.currentPageText)
        mEditor!!.enableTextChangedListener()

        verticalScroll.postDelayed({ verticalScroll.smoothScrollTo(0, 0) }, 200)

        if (!PreferenceHelper.getPageSystemButtonsPopupShown(this)) {
            PreferenceHelper.setPageSystemButtonsPopupShown(this, true)
            Toast.makeText(this, getString(R.string.long_click_for_more_options),
                    Toast.LENGTH_LONG).show()
        }
    }

    override fun prevPageClicked() {
        pageSystem!!.savePage(mEditor!!.text!!.toString())
        pageSystem!!.prevPage()
        mEditor!!.disableTextChangedListener()
        mEditor!!.replaceTextKeepCursor(pageSystem!!.currentPageText)
        mEditor!!.enableTextChangedListener()

        verticalScroll.postDelayed({ verticalScroll.smoothScrollTo(0, 0) }, 200)

        if (!PreferenceHelper.getPageSystemButtonsPopupShown(this)) {
            PreferenceHelper.setPageSystemButtonsPopupShown(this, true)
            Toast.makeText(this, getString(R.string.long_click_for_more_options),
                    Toast.LENGTH_LONG).show()
        }
    }

    override fun pageSystemButtonLongClicked() {
        val maxPages = pageSystem!!.maxPage
        val currentPage = pageSystem!!.currentPage
        NumberPickerDialog.newInstance(NumberPickerDialog.Actions.SelectPage, 0, currentPage, maxPages).show(fragmentManager.beginTransaction(), "dialog")
    }

    override fun canReadNextPage(): Boolean {
        return pageSystem!!.canReadNextPage()
    }

    override fun canReadPrevPage(): Boolean {
        return pageSystem!!.canReadPrevPage()
    }

    override fun onSearchDone(searchResult: SearchResult) {
        MainActivity.searchResult = searchResult
        invalidateOptionsMenu()

        val line = LineUtils.getLineFromIndex(searchResult.foundIndex.first, mEditor!!.lineCount, mEditor!!.layout)
        verticalScroll.post {
            var y = mEditor!!.layout.getLineTop(line)
            if (y > 100)
                y -= 100
            else
                y = 0

            verticalScroll.scrollTo(0, y)
        }

        mEditor!!.isFocusable = true
        mEditor!!.requestFocus()
        mEditor!!.setSelection(searchResult.foundIndex.first, searchResult.foundIndex.first + searchResult.textLength)

    }

    override fun onPageChanged(page: Int) {
        pageSystemButtons!!.updateVisibility(false)
        searchResult = null
        mEditor!!.clearHistory()
        invalidateOptionsMenu()
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        pageSystemButtons!!.updateVisibility(Math.abs(t) > 10)

        if (!PreferenceHelper.getSyntaxHighlight(this) || mEditor!!.hasSelection() && searchResult == null || updateHandler == null || colorRunnable_duringScroll == null)
            return

        updateHandler.removeCallbacks(colorRunnable_duringEditing)
        updateHandler.removeCallbacks(colorRunnable_duringScroll)
        updateHandler.postDelayed(colorRunnable_duringScroll, SYNTAX_DELAY_MILLIS_SHORT.toLong())
    }

    override fun onNumberPickerDialogDismissed(action: NumberPickerDialog.Actions, value: Int) {
        if (action == NumberPickerDialog.Actions.SelectPage) {
            pageSystem!!.savePage(mEditor!!.text!!.toString())
            pageSystem!!.goToPage(value)
            mEditor!!.disableTextChangedListener()
            mEditor!!.replaceTextKeepCursor(pageSystem!!.currentPageText)
            mEditor!!.enableTextChangedListener()

            verticalScroll.postDelayed({ verticalScroll.smoothScrollTo(0, 0) }, 200)

        } else if (action == NumberPickerDialog.Actions.GoToLine) {

            val fakeLine = mEditor!!.lineUtils.fakeLineFromRealLine(value)
            val y = LineUtils.getYAtLine(verticalScroll,
                    mEditor!!.lineCount, fakeLine)

            verticalScroll.postDelayed({ verticalScroll.smoothScrollTo(0, y) }, 200)
        }

    }

    override fun userDoesNotWantToSave(openNewFile: Boolean, newUri: GreatUri) {
        mEditor!!.fileSaved()
        if (openNewFile)
            newFileToOpen(newUri, "")
        else
            cannotOpenFile()
    }

    override fun startSavingFile(uri: GreatUri, text: String, encoding: String) {
        viewModel?.saveFile(uri, text, encoding)
    }

    override fun CancelItem(position: Int, andCloseOpenedFile: Boolean) {
        refreshList(greatUris!![position], false, true)
        if (andCloseOpenedFile)
            cannotOpenFile()
    }

    override fun onButtonAccessoryViewClicked(text: String) {
        mEditor!!.text!!.insert(mEditor!!.selectionStart, text)
    }

    override fun onEdittextDialogEnded(result: String, hint: String, action: EditTextDialog.Actions) {

        if (Device.hasKitKatApi() && TextUtils.isEmpty(viewModel!!.greatUri!!.filePath)) {
            var newUri: Uri? = null
            try {
                DocumentsContract.renameDocument(contentResolver, viewModel!!.greatUri!!.uri!!, result)
            } catch (e: FileNotFoundException) {
                newUri = null
            }

            // if everything is ok
            if (newUri != null) {

                // delete current
                refreshList(viewModel!!.greatUri, false, true)

                viewModel!!.greatUri!!.uri = newUri
                viewModel!!.greatUri!!.filePath = AccessStorageApi.getPath(this, newUri)
                viewModel!!.greatUri!!.fileName = AccessStorageApi.getName(this, newUri)

                viewModel?.saveFile(viewModel!!.greatUri!!, pageSystem!!.getAllText(mEditor!!.text!!.toString()), viewModel!!.currentEncoding!!)
            } else {
                Toast.makeText(this, R.string.file_cannot_be_renamed, Toast.LENGTH_SHORT).show()
            }
        } else {
            val newFile = File(viewModel!!.greatUri!!.parentFolder, result)
            // if everything is ok
            if (File(viewModel!!.greatUri!!.filePath!!).renameTo(newFile)) {

                // delete current
                refreshList(viewModel!!.greatUri, false, true)

                viewModel!!.greatUri!!.uri = Uri.fromFile(newFile)
                viewModel!!.greatUri!!.filePath = newFile.absolutePath
                viewModel!!.greatUri!!.fileName = newFile.name

                viewModel?.saveFile(viewModel!!.greatUri!!, pageSystem!!.getAllText(mEditor!!.text!!.toString()), viewModel!!.currentEncoding!!)
            } else {
                Toast.makeText(this, R.string.file_cannot_be_renamed, Toast.LENGTH_SHORT).show()
            }
        }


        /*new SaveFileTask(this, greatUri, pageSystem.getAllText(mEditor.getText().toString()), currentEncoding, new SaveFileTask.SaveFileInterface() {
            @Override
            public void fileSaved(Boolean success) {
                savedAFile(greatUri, true);
            }
        }).execute();*/
    }

    companion object {

        //region VARIABLES
        private const val READ_REQUEST_CODE = 42
        private const val CREATE_REQUEST_CODE = 43
        private const val SAVE_AS_REQUEST_CODE = 44
        private const val SELECT_FILE_CODE = 121
        private const val SYNTAX_DELAY_MILLIS_SHORT = 250
        private const val SYNTAX_DELAY_MILLIS_LONG = 1500
        private val ID_UNDO = R.id.im_undo
        private val ID_REDO = R.id.im_redo

        const val REQUEST_WRITE_STORAGE_PERMISSION = 130
        var fileExtension = ""
        private var searchResult: SearchResult? = null

        var pageSystem: PageSystem? = null
        lateinit var verticalScroll: GoodScrollView
    }

    //endregion
}
