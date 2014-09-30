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

package sharedcode.turboeditor.fragment;

import android.app.ActionBar;
import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Selection;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.text.method.KeyListener;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;
import android.widget.Toast;

import com.faizmalkani.floatingactionbutton.FloatingActionButton;
import sharedcode.turboeditor.R;

import sharedcode.turboeditor.preferences.SettingsFragment;
import sharedcode.turboeditor.util.EditorInterface;
import sharedcode.turboeditor.util.EdittextPadding;
import sharedcode.turboeditor.util.EventBusEvents;
import sharedcode.turboeditor.util.LineUtils;
import sharedcode.turboeditor.views.GoodScrollView;
import sharedcode.turboeditor.util.MimeTypes;
import sharedcode.turboeditor.util.PageSystem;
import sharedcode.turboeditor.util.PageSystemButtons;
import sharedcode.turboeditor.util.Patterns;
import sharedcode.turboeditor.preferences.PreferenceHelper;
import sharedcode.turboeditor.util.SaveFileTask;
import sharedcode.turboeditor.util.SearchResult;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.greenrobot.event.EventBus;

import static sharedcode.turboeditor.util.EventBusEvents.APreferenceValueWasChanged.Type.ENCODING;
import static sharedcode.turboeditor.util.EventBusEvents.APreferenceValueWasChanged.Type.FONT_SIZE;
import static sharedcode.turboeditor.util.EventBusEvents.APreferenceValueWasChanged.Type.LINE_NUMERS;
import static sharedcode.turboeditor.util.EventBusEvents.APreferenceValueWasChanged.Type.MONOSPACE;
import static sharedcode.turboeditor.util.EventBusEvents.APreferenceValueWasChanged.Type.READ_ONLY;
import static sharedcode.turboeditor.util.EventBusEvents.APreferenceValueWasChanged.Type.SYNTAX;
import static sharedcode.turboeditor.util.EventBusEvents.APreferenceValueWasChanged.Type.TEXT_SUGGESTIONS;
import static sharedcode.turboeditor.util.EventBusEvents.APreferenceValueWasChanged.Type.THEME_CHANGE;
import static sharedcode.turboeditor.util.EventBusEvents.APreferenceValueWasChanged.Type.WRAP_CONTENT;

public class EditorFragment extends Fragment implements FindTextDialogFragment.SearchDialogInterface, GoodScrollView.ScrollInterface, EditorInterface, PageSystem.PageSystemInterface, PageSystemButtons.PageButtonsInterface, SeekbarDialogFragment.onSeekbarDialogDismissed {

    //region VARIABLES
    private GoodScrollView verticalScroll;
    private String sFilePath;
    private Editor mEditor;
    private HorizontalScrollView horizontalScroll;
    private boolean searchingText;
    private SearchResult searchResult;
    private PageSystem pageSystem;
    private PageSystemButtons pageSystemButtons;
    private String currentEncoding;

    private static final int SYNTAX_DELAY_MILLIS_SHORT = 250;
    private static final int SYNTAX_DELAY_MILLIS_LONG = 1500;
    static final int
            ID_SELECT_ALL = android.R.id.selectAll;
    static final int ID_CUT = android.R.id.cut;
    static final int ID_COPY = android.R.id.copy;
    static final int ID_PASTE = android.R.id.paste;
    private static final int ID_UNDO = R.id.im_undo;
    private static final int ID_REDO = R.id.im_redo;
    private static final int CHARS_TO_COLOR = 2500;

    private final Handler updateHandler = new Handler();
    private final Runnable colorRunnable_duringEditing =
            new Runnable() {
                @Override
                public void run() {
                    mEditor.replaceTextKeepCursor(null, true);
                }
            };
    private final Runnable colorRunnable_duringScroll =
            new Runnable() {
                @Override
                public void run() {
                    mEditor.replaceTextKeepCursor(null, false);
                }
            };
    //endregion

    public static EditorFragment newInstance(String filePath, String fileText, String encoding) {
        EditorFragment frag = new EditorFragment();
        Bundle args = new Bundle();
        args.putString("filePath", filePath);
        args.putString("fileText", fileText);
        args.putString("encoding", encoding);
        frag.setArguments(args);
        return frag;
    }

    //region ACTIVITY LIFECYCLE

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        sFilePath = getArguments().getString("filePath");
        pageSystem = new PageSystem(getActivity(), this, getArguments().getString("fileText"));
        currentEncoding = getArguments().getString("encoding");
        getArguments().remove("fileText");
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_editor, container, false);
        verticalScroll = (GoodScrollView) rootView.findViewById(R.id.vertical_scroll);
        horizontalScroll = (HorizontalScrollView) rootView.findViewById(R.id.horizontal_scroll);
        mEditor = (Editor) rootView.findViewById(R.id.editor);

        mEditor.setEditorInterface(this);

        if (PreferenceHelper.getWrapContent(getActivity())) {
            horizontalScroll.removeView(mEditor);
            verticalScroll.removeView(horizontalScroll);
            verticalScroll.addView(mEditor);
        } else {
            // else show what is in the xml file fragment_editor.xml-
        }
        if (PreferenceHelper.getLightTheme(getActivity())) {
            mEditor.setTextColor(getResources().getColor(R.color.textColorInverted));
        } else {
            mEditor.setTextColor(getResources().getColor(R.color.textColor));
        }
        if (PreferenceHelper.getLineNumbers(getActivity())) {
            mEditor.setPadding(EdittextPadding.getPaddingWithLineNumbers(getActivity(), PreferenceHelper.getFontSize(getActivity())), EdittextPadding.getPaddingTop(getActivity()), 0, 0);
        } else {
            mEditor.setPadding(EdittextPadding.getPaddingWithoutLineNumbers(getActivity()), EdittextPadding.getPaddingTop(getActivity()), 0, 0);
        }

        if(PreferenceHelper.getReadOnly(getActivity())) {
            getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
            mEditor.setReadOnly(true);
        }  else {
            getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED);
            mEditor.setReadOnly(false);
            if (PreferenceHelper.getSuggestionActive(getActivity())) {
                mEditor.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE);
            } else {
                mEditor.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD | InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE);
            }
        }

        if (PreferenceHelper.getUseMonospace(getActivity())) {
            mEditor.setTypeface(Typeface.MONOSPACE);
        } else {
            mEditor.setTypeface(Typeface.DEFAULT);
        }
        mEditor.setTextSize(PreferenceHelper.getFontSize(getActivity()));

        mEditor.setFocusable(true);
        mEditor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!PreferenceHelper.getReadOnly(getActivity())) {
                    ((InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE))
                            .showSoftInput(mEditor, InputMethodManager.SHOW_IMPLICIT);
                }

            }
        });
        mEditor.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus && !PreferenceHelper.getReadOnly(getActivity())) {
                    ((InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE))
                            .showSoftInput(mEditor, InputMethodManager.SHOW_IMPLICIT);
                }
            }
        });

        mEditor.setMaxHistorySize(30);

        verticalScroll.setScrollInterface(this);

        pageSystemButtons = new PageSystemButtons(getActivity(), this, (FloatingActionButton)rootView.findViewById(R.id.fabPrev), (FloatingActionButton)rootView.findViewById(R.id.fabNext));

        mEditor.disableTextChangedListener();
        mEditor.replaceTextKeepCursor(pageSystem.getCurrentPageText(), false);
        mEditor.enableTextChangedListener();
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Register the Event Bus for events
        EventBus.getDefault().register(this);
    }


    @Override
    public void onPause() {
        super.onPause();
        // Unregister the Event Bus
        EventBus.getDefault().unregister(this);

        if(PreferenceHelper.getAutoSave(getActivity()) && mEditor.canSaveFile()) {
            onEvent(new EventBusEvents.SaveAFile());
            mEditor.fileSaved(); // so it doesn't ask to save in onDetach
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        if (!getActivity().isFinishing() && mEditor.canSaveFile())
            SaveFileDialogFragment.newInstance(sFilePath, pageSystem.getAllText(mEditor.getText().toString()), currentEncoding).show(getFragmentManager(), "dialog");
    }

    //endregion

    //region MENU
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (searchingText)
            inflater.inflate(R.menu.fragment_editor_search, menu);
        else
            inflater.inflate(R.menu.fragment_editor, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        ActionBar ab = getActivity().getActionBar();
        if (ab == null)
            return;


        if (searchingText) {
            MenuItem imReplace = menu.findItem(R.id.im_replace);
            if (imReplace != null)
                imReplace.setVisible(searchResult.isReplace);

        } else {
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
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int i = item.getItemId();
        if (i == R.id.im_save) {
            EventBus.getDefault().post(new EventBusEvents.SaveAFile());

        } else if (i == R.id.im_undo) {
            this.mEditor.onTextContextMenuItem(ID_UNDO);

        } else if (i == R.id.im_redo) {
            this.mEditor.onTextContextMenuItem(ID_REDO);

        } else if (i == R.id.im_search) {
            FindTextDialogFragment dialogFrag = FindTextDialogFragment.newInstance(pageSystem.getCurrentPageText());
            dialogFrag.setTargetFragment(EditorFragment.this, 0);
            dialogFrag.show(getFragmentManager().beginTransaction(), "dialog");

        } else if (i == R.id.im_cancel) {
            searchingText = false;
            getActivity().invalidateOptionsMenu();

        } else if (i == R.id.im_replace) {
            replaceText();

        } else if (i == R.id.im_next_item) {
            nextResult();

        } else if (i == R.id.im_previous_item) {
            previousResult();

        }
        else if(i == R.id.im_goto_line){
            int max = mEditor.getLineUtils().lastReadLine();
            SeekbarDialogFragment dialogFrag = SeekbarDialogFragment.newInstance(SeekbarDialogFragment.Actions.GoToLine, 0, 0, max);
            dialogFrag.setTargetFragment(EditorFragment.this, 0);
            dialogFrag.show(getFragmentManager().beginTransaction(), "dialog");
        }
        else if (i == R.id.im_view_it_on_browser) {
            Intent browserIntent = null;
            try {
                browserIntent = new Intent(Intent.ACTION_VIEW);
                browserIntent.setDataAndType(Uri.fromFile(new File(sFilePath)), "text/*");
                startActivity(browserIntent);
            } catch (ActivityNotFoundException ex2) {
                //
            }

        } else if (i == R.id.im_share) {
            File f = new File(sFilePath);
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(f));
            shareIntent.setType("text/plain");

            startActivity(Intent.createChooser(shareIntent, getString(R.string.share)));

        }
        return super.onOptionsItemSelected(item);
    }
    //endregion

    //region Interfaces
    @Override
    public String getFilePath() {
        return sFilePath;
    }

    @Override
    public GoodScrollView getVerticalScrollView() {
        return verticalScroll;
    }

    @Override
    public PageSystem getPageSystem() {
        return pageSystem;
    }

    @Override
    public void updateTextSyntax() {
        if (!PreferenceHelper.getSyntaxHiglight(getActivity()) || mEditor.hasSelection() || updateHandler == null || colorRunnable_duringEditing == null)
            return;

        updateHandler.removeCallbacks(colorRunnable_duringEditing);
        updateHandler.removeCallbacks(colorRunnable_duringScroll);
        updateHandler.postDelayed(colorRunnable_duringEditing, SYNTAX_DELAY_MILLIS_LONG);
    }

    @Override
    public void nextPageClicked() {
        pageSystem.savePage(mEditor.getText().toString());
        pageSystem.nextPage();
        mEditor.disableTextChangedListener();
        mEditor.replaceTextKeepCursor(pageSystem.getCurrentPageText(), false);
        mEditor.enableTextChangedListener();

        verticalScroll.postDelayed(new Runnable() {
            @Override
            public void run() {
                verticalScroll.smoothScrollTo(0, 0);
            }
        }, 200);

        if(PreferenceHelper.getPageSystemButtonsPopupShown(getActivity()) == false){
            PreferenceHelper.setPageSystemButtonsPopupShown(getActivity(), true);
            Toast.makeText(getActivity(), getString(R.string.long_click_for_more_options), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void pageSystemButtonLongClicked() {
        int maxPages = pageSystem.getMaxPage();
        int currentPage = pageSystem.getCurrentPage();
        SeekbarDialogFragment dialogFrag = SeekbarDialogFragment.newInstance(SeekbarDialogFragment.Actions.SelectPage, 0, currentPage, maxPages);
        dialogFrag.setTargetFragment(EditorFragment.this, 0);
        dialogFrag.show(getFragmentManager().beginTransaction(), "dialog");
    }

    @Override
    public void prevPageClicked() {
        pageSystem.savePage(mEditor.getText().toString());
        pageSystem.prevPage();
        mEditor.disableTextChangedListener();
        mEditor.replaceTextKeepCursor(pageSystem.getCurrentPageText(), false);
        mEditor.enableTextChangedListener();

        verticalScroll.postDelayed(new Runnable() {
            @Override
            public void run() {
                verticalScroll.smoothScrollTo(0,0);
            }
        }, 200);

        if(PreferenceHelper.getPageSystemButtonsPopupShown(getActivity()) == false){
            PreferenceHelper.setPageSystemButtonsPopupShown(getActivity(), true);
            Toast.makeText(getActivity(), getString(R.string.long_click_for_more_options), Toast.LENGTH_LONG).show();
        }
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
        this.searchResult = searchResult;
        searchingText = true;
        getActivity().invalidateOptionsMenu();

        final int line = mEditor.getLineUtils().getLineFromIndex(searchResult.foundIndex.getFirst(), mEditor.getLineCount(), mEditor.getLayout());
        verticalScroll.post(new Runnable() {
            @Override
            public void run() {
                int y = mEditor.getLayout().getLineTop(line);
                if(y > 100)
                    y -= 100;
                else
                    y = 0;

                verticalScroll.scrollTo(0, y);
            }
        });

        mEditor.setSelection(searchResult.foundIndex.getFirst(), searchResult.foundIndex.getFirst() + searchResult.textLength);

    }

    @Override
    public void onPageChanged(int page) {
        pageSystemButtons.updateVisibility(false);
        searchingText = false;
        mEditor.clearHistory();
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public void onScrollChanged(int l, int t, int oldl, int oldt) {
        pageSystemButtons.updateVisibility(Math.abs(t) > 10);

        if (!PreferenceHelper.getSyntaxHiglight(getActivity()) || (mEditor.hasSelection() && !searchingText) || updateHandler == null || colorRunnable_duringScroll == null)
            return;

        updateHandler.removeCallbacks(colorRunnable_duringEditing);
        updateHandler.removeCallbacks(colorRunnable_duringScroll);
        updateHandler.postDelayed(colorRunnable_duringScroll, SYNTAX_DELAY_MILLIS_SHORT);
    }

    @Override
    public void onSeekbarDialogDismissed(SeekbarDialogFragment.Actions action, int value) {
        if(action == SeekbarDialogFragment.Actions.SelectPage) {
            pageSystem.savePage(mEditor.getText().toString());
            pageSystem.goToPage(value);
            mEditor.disableTextChangedListener();
            mEditor.replaceTextKeepCursor(pageSystem.getCurrentPageText(), true);
            mEditor.enableTextChangedListener();

            verticalScroll.postDelayed(new Runnable() {
                @Override
                public void run() {
                    verticalScroll.smoothScrollTo(0, 0);
                }
            }, 200);

        } else if(action == SeekbarDialogFragment.Actions.GoToLine) {

            int fakeLine = mEditor.getLineUtils().fakeLineFromRealLine(value);
            final int y = mEditor.getLineUtils().getYAtLine(verticalScroll, mEditor.getLineCount(), fakeLine);

            verticalScroll.postDelayed(new Runnable() {
                @Override
                public void run() {
                    verticalScroll.smoothScrollTo(0, y);
                }
            }, 200);
        }

    }

    public void nextResult() {
        if (searchResult.index == mEditor.getLineCount() - 1) // last result of page
        {
            return;
        }


        if (searchResult.index < searchResult.numberOfResults() - 1) { // equal zero is not good
            searchResult.index++;
            final int line = mEditor.getLineUtils().getLineFromIndex(searchResult.foundIndex.get(searchResult.index), mEditor.getLineCount(), mEditor.getLayout());


            verticalScroll.post(new Runnable() {
                @Override
                public void run() {
                    int y = mEditor.getLayout().getLineTop(line);
                    if(y > 100)
                        y -= 100;
                    else
                        y = 0;

                    verticalScroll.scrollTo(0, y);
                }
            });

            mEditor.setSelection(searchResult.foundIndex.get(searchResult.index), searchResult.foundIndex.get(searchResult.index) + searchResult.textLength);
        }
    }

    public void previousResult() {
        if (searchResult.index == 0)
            return;
        if (searchResult.index > 0) {
            searchResult.index--;
            final int line = mEditor.getLineUtils().getLineFromIndex(searchResult.foundIndex.get(searchResult.index), mEditor.getLineCount(), mEditor.getLayout());
            verticalScroll.post(new Runnable() {
                @Override
                public void run() {
                    int y = mEditor.getLayout().getLineTop(line);
                    if(y > 100)
                        y -= 100;
                    else
                        y = 0;
                    verticalScroll.scrollTo(0, y);
                }
            });

            mEditor.setSelection(searchResult.foundIndex.get(searchResult.index), searchResult.foundIndex.get(searchResult.index) + searchResult.textLength);
        }
    }

    public void replaceText() {
        mEditor.setText(mEditor.getText().replace(searchResult.foundIndex.get(searchResult.index), searchResult.foundIndex.get(searchResult.index) + searchResult.textLength, searchResult.textToReplace));
        searchResult.doneReplace();
        nextResult();
    }
    //endregion

    //region Eventbus
    public void onEvent(EventBusEvents.APreferenceValueWasChanged event) {

        if (event.hasType(WRAP_CONTENT)) {
            if (PreferenceHelper.getWrapContent(getActivity())) {
                horizontalScroll.removeView(mEditor);
                verticalScroll.removeView(horizontalScroll);
                verticalScroll.addView(mEditor);
            } else {
                verticalScroll.removeView(mEditor);
                verticalScroll.addView(horizontalScroll);
                horizontalScroll.addView(mEditor);
            }
        } else if (event.hasType(LINE_NUMERS)) {
            mEditor.disableTextChangedListener();
            mEditor.replaceTextKeepCursor(null, true);
            mEditor.enableTextChangedListener();
            if (PreferenceHelper.getLineNumbers(getActivity())) {
                mEditor.setPadding(EdittextPadding.getPaddingWithLineNumbers(getActivity(), PreferenceHelper.getFontSize(getActivity())), EdittextPadding.getPaddingTop(getActivity()), 0, 0);
            } else {
                mEditor.setPadding(EdittextPadding.getPaddingWithoutLineNumbers(getActivity()), EdittextPadding.getPaddingTop(getActivity()), 0, 0);
            }
        } else if (event.hasType(SYNTAX)) {
            mEditor.disableTextChangedListener();
            mEditor.replaceTextKeepCursor(null, true);
            mEditor.enableTextChangedListener();
        } else if (event.hasType(MONOSPACE)) {
            if (PreferenceHelper.getUseMonospace(getActivity()))
                this.mEditor.setTypeface(Typeface.MONOSPACE);
            else
                this.mEditor.setTypeface(Typeface.DEFAULT);
        } else if (event.hasType(THEME_CHANGE)) {
            if (PreferenceHelper.getLightTheme(getActivity())) {
                mEditor.setTextColor(getResources().getColor(R.color.textColorInverted));
            } else {
                mEditor.setTextColor(getResources().getColor(R.color.textColor));
            }
        } else if (event.hasType(TEXT_SUGGESTIONS) || event.hasType(READ_ONLY)) {
            if(PreferenceHelper.getReadOnly(getActivity())) {
                getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                mEditor.setReadOnly(true);
            }  else {
                getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED);
                mEditor.setReadOnly(false);
                if (PreferenceHelper.getSuggestionActive(getActivity())) {
                    mEditor.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE);
                } else {
                    mEditor.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD | InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE);
                }
            }
            // sometimes it becomes monospace after setting the input type
            if (PreferenceHelper.getUseMonospace(getActivity()))
                this.mEditor.setTypeface(Typeface.MONOSPACE);
            else
                this.mEditor.setTypeface(Typeface.DEFAULT);
        } else if (event.hasType(FONT_SIZE)) {
            if (PreferenceHelper.getLineNumbers(getActivity())) {
                mEditor.setPadding(EdittextPadding.getPaddingWithLineNumbers(getActivity(), PreferenceHelper.getFontSize(getActivity())), EdittextPadding.getPaddingTop(getActivity()), 0, 0);
            } else {
                mEditor.setPadding(EdittextPadding.getPaddingWithoutLineNumbers(getActivity()), EdittextPadding.getPaddingTop(getActivity()), 0, 0);
            }
            this.mEditor.setTextSize(PreferenceHelper.getFontSize(getActivity()));
        } else if (event.hasType(ENCODING)) {
            String oldEncoding, newEncoding;
            oldEncoding = currentEncoding;
            newEncoding = PreferenceHelper.getEncoding(getActivity());
            try {
                final byte[] oldText = this.mEditor.getText().toString().getBytes(oldEncoding);
                mEditor.disableTextChangedListener();
                mEditor.replaceTextKeepCursor(new String(oldText, newEncoding), true);
                mEditor.enableTextChangedListener();
                currentEncoding = newEncoding;
            } catch (UnsupportedEncodingException ignored) {
                try {
                    final byte[] oldText = this.mEditor.getText().toString().getBytes(oldEncoding);
                    mEditor.disableTextChangedListener();
                    mEditor.replaceTextKeepCursor(new String(oldText, "UTF-8"), true);
                    mEditor.enableTextChangedListener();
                } catch (UnsupportedEncodingException ignored2) {
                }
            }
        }
    }

    public void onEvent(EventBusEvents.SaveAFile event) {
        File file = new File(sFilePath);
        if(!file.getName().isEmpty())
            new SaveFileTask(getActivity(), sFilePath, pageSystem.getAllText(mEditor.getText().toString()), currentEncoding).execute();
        else {
            NewFileDetailsDialogFragment dialogFrag = NewFileDetailsDialogFragment.newInstance(pageSystem.getAllText(mEditor.getText().toString()), currentEncoding);
            dialogFrag.show(getFragmentManager().beginTransaction(), "dialog");
        }
    }

    public void onEvent(EventBusEvents.SavedAFile event) {
        mEditor.clearHistory();
        mEditor.fileSaved();
        getActivity().invalidateOptionsMenu();
    }

    public void onEvent(EventBusEvents.InvalideTheMenu event) {
        getActivity().invalidateOptionsMenu();
    }
    //endregion

    public static class Editor extends EditText {

        //region VARIABLES
        private EditorInterface editorInterface;
        private final TextPaint mPaintNumbers = new TextPaint();
        /**
         * The edit history.
         */
        private final EditHistory mEditHistory;
        /**
         * The change listener.
         */
        private final EditTextChangeListener
                mChangeListener;
        int lineCount, realLine;
        private LineUtils lineUtils;
        private boolean modified = true;
        /**
         * Is undo/redo being performed? This member
         * signals if an undo/redo operation is
         * currently being performed. Changes in the
         * text during undo/redo are not recorded
         * because it would mess up the undo history.
         */
        private boolean mIsUndoOrRedo = false;
        private Matcher m;
        private boolean mShowUndo = false, mShowRedo = false;
        private boolean canSaveFile = false;
        private KeyListener keyListener;
        private int firstVisibleIndex = 0, firstColoredIndex = 0;
        private int deviceHeight;
        //endregion

        //region CONSTRUCTOR
        public Editor(final Context context, AttributeSet attrs) {
            super(context, attrs);
            mEditHistory = new EditHistory();
            mChangeListener = new EditTextChangeListener();
            lineUtils = new LineUtils();

            deviceHeight = getResources().getDisplayMetrics().heightPixels;

            this.mPaintNumbers.setAntiAlias(true);
            this.mPaintNumbers.setDither(false);

            // Syntax editor
            setFilters(new InputFilter[]{
                    new InputFilter() {
                        @Override
                        public CharSequence filter(
                                CharSequence source,
                                int start,
                                int end,
                                Spanned dest,
                                int dstart,
                                int dend) {
                            if (modified) {
                                return autoIndent(
                                        source,
                                        start,
                                        end,
                                        dest,
                                        dstart,
                                        dend);
                            }

                            return source;
                        }
                    }});
        }
        //endregion

        //region OVERRIDES
        @Override
        public void setTextSize(float size) {
            super.setTextSize(size);
            final float scale = getContext().getResources().getDisplayMetrics().density;
            this.mPaintNumbers.setTextSize((int) (size * scale * 0.65f));
        }

        @Override
        public void setTextColor(int color) {
            super.setTextColor(color);
            //this.mPaintNumbers.setColor(getTextColors().getDefaultColor());
            this.mPaintNumbers.setColor(getResources().getColor(R.color.file_text));
        }

        @Override
        public boolean onKeyUp(int keyCode, KeyEvent event) {
            return true;
        }

        @Override
        public boolean onKeyDown(int keyCode, KeyEvent event) {

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
                        EventBus.getDefault().post(new EventBusEvents.SaveAFile());
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

        @Override
        public void onDraw(final Canvas canvas) {

            if (PreferenceHelper.getLineNumbers(getContext())) {
                if (lineCount != getLineCount()) {
                    lineCount = getLineCount();

                    lineUtils.updateHasNewLineArray(editorInterface.getPageSystem().getStartingLine(), lineCount, getLayout(), getText().toString());
                }

                int editorHeight = getHeight();
                int i = lineUtils.getFirstVisibleLine(editorInterface.getVerticalScrollView(), editorHeight, lineCount);
                int lastLine = lineUtils.getLastVisibleLine(editorInterface.getVerticalScrollView(), editorHeight, lineCount, deviceHeight);
                boolean[] hasNewLineArray = lineUtils.getToCountLinesArray();
                int[] realLines = lineUtils.getRealLines();
                boolean wrapContent = PreferenceHelper.getWrapContent(getContext());

                while (i < lastLine) {
                    // if last line we count it anyway
                    if (!wrapContent
                            || hasNewLineArray[i]
                            || i == lastLine - 1) {
                        if (i == lastLine - 1)
                            realLine = realLines[i] + 1;
                        else
                            realLine = realLines[i];

                        canvas.drawText(String.valueOf(realLine),
                                5, // padding left
                                getLineHeight() * (i + 1),
                                mPaintNumbers);
                    }
                    i++;
                }
            }


            super.onDraw(canvas);
        }


        //endregion

        //region Other

        public boolean canSaveFile() {
            return canSaveFile;
        }

        public void fileSaved() {
            canSaveFile = false;
        }

        public void setEditorInterface(EditorInterface editorInterface){
            this.editorInterface = editorInterface;
        }

        public LineUtils getLineUtils() {
            return lineUtils;
        }

        private CharSequence autoIndent(
                CharSequence source,
                int start,
                int end,
                Spanned dest,
                int dstart,
                int dend) {
            if (end - start != 1 ||
                    start >= source.length() ||
                    source.charAt(start) != '\n' ||
                    dstart >= dest.length()) {
                return source;
            }

            int istart = dstart;
            int iend;
            String indent = "";

            // skip end of line if cursor is at the end of a line
            if (dest.charAt(istart) == '\n') {
                --istart;
            }

            // indent next line if this one isn't terminated
            if (istart > -1) {
                // skip white space
                for (; istart > -1; --istart) {
                    char c = dest.charAt(istart);

                    if (c != ' ' &&
                            c != '\t') {
                        break;
                    }
                }

                if (istart > -1) {
                    char c = dest.charAt(istart);

                    if (c != ';' &&
                            c != '\n') {
                        indent = "\t";
                    }
                }
            }

            // find start of previous line
            for (; istart > -1; --istart) {
                if (dest.charAt(istart) == '\n') {
                    break;
                }
            }

            // cursor is in the first line
            if (istart < 0) {
                return source;
            }

            // span over previous indent
            for (iend = ++istart;
                 iend < dend;
                 ++iend) {
                char c = dest.charAt(iend);

                if (c != ' ' &&
                        c != '\t') {
                    break;
                }
            }

            // copy white space of previous lines and append new indent
            return "\n" + dest.subSequence(
                    istart,
                    iend) + indent;
        }

        public void replaceTextKeepCursor(String textToUpdate, boolean mantainCursorPos) {

            int cursorPos;
            int cursorPosEnd;
            if(textToUpdate != null) {
                cursorPos = 0;
                cursorPosEnd = 0;
            } else {
                cursorPos = getSelectionStart();
                cursorPosEnd = getSelectionEnd();
            }
            disableTextChangedListener();
            modified = false;




            if(PreferenceHelper.getSyntaxHiglight(getContext()))
                setText(highlight(textToUpdate == null ? getEditableText() : Editable.Factory.getInstance().newEditable(textToUpdate)));
            else
                setText(textToUpdate == null ? getText().toString() : textToUpdate);

            modified = true;
            enableTextChangedListener();

            if(mantainCursorPos)
                firstVisibleIndex = cursorPos;

            if (firstVisibleIndex > -1) {
                if(cursorPosEnd != cursorPos)
                    setSelection(cursorPos, cursorPosEnd);
                else
                    setSelection(firstVisibleIndex);
            }
        }

        public CharSequence highlight(Editable editable) {
            final String fileExtension = FilenameUtils.getExtension(editorInterface.getFilePath()).toLowerCase();
            editable.clearSpans();

            if (editable.length() == 0) {
                return editable;
            }

            firstVisibleIndex = 0;
            int end = CHARS_TO_COLOR;
            int height = getHeight();

            if(height > 0) {
                firstVisibleIndex = getLayout().getLineStart(getLineUtils().getFirstVisibleLine(editorInterface.getVerticalScrollView(), height, getLineCount()));
                end = getLayout().getLineStart(getLineUtils().getLastVisibleLine(editorInterface.getVerticalScrollView(), height, lineCount, deviceHeight));
                //int end = firstColoredIndex + CHARS_TO_COLOR;
            }

            firstColoredIndex = firstVisibleIndex - (CHARS_TO_COLOR / 5);
            if (firstColoredIndex < 0)
                firstColoredIndex = 0;
            if (end > editable.length())
                end = editable.length();

            CharSequence textToHiglight = editable.subSequence(firstColoredIndex, end);

            if (fileExtension.contains("htm")
                    || fileExtension.contains("xml")) {
                color(Patterns.HTML_OPEN_TAGS, editable, textToHiglight, firstColoredIndex);
                color(Patterns.HTML_CLOSE_TAGS, editable, textToHiglight, firstColoredIndex);
                color(Patterns.HTML_ATTRS, editable, textToHiglight, firstColoredIndex);
                color(Patterns.GENERAL_STRINGS, editable, textToHiglight, firstColoredIndex);
                color(Patterns.XML_COMMENTS, editable, textToHiglight, firstColoredIndex);
            } else if (fileExtension.equals("css")) {
                //color(CSS_STYLE_NAME, editable);
                color(Patterns.CSS_ATTRS, editable, textToHiglight, firstColoredIndex);
                color(Patterns.CSS_ATTR_VALUE, editable, textToHiglight, firstColoredIndex);
                color(Patterns.SYMBOLS, editable, textToHiglight, firstColoredIndex);
                color(Patterns.GENERAL_COMMENTS, editable, textToHiglight, firstColoredIndex);
            } else if (Arrays.asList(MimeTypes.MIME_CODE).contains(fileExtension)) {
                if(fileExtension.equals("lua"))
                    color(Patterns.LUA_KEYWORDS, editable, textToHiglight, firstColoredIndex);
                else if(fileExtension.equals("py"))
                    color(Patterns.PY_KEYWORDS, editable, textToHiglight, firstColoredIndex);
                else
                    color(Patterns.GENERAL_KEYWORDS, editable, textToHiglight, firstColoredIndex);
                color(Patterns.NUMBERS, editable, textToHiglight, firstColoredIndex);
                color(Patterns.SYMBOLS, editable, textToHiglight, firstColoredIndex);
                color(Patterns.GENERAL_STRINGS, editable, textToHiglight, firstColoredIndex);
                color(Patterns.GENERAL_COMMENTS, editable, textToHiglight, firstColoredIndex);
                if (fileExtension.equals("php"))
                    color(Patterns.PHP_VARIABLES, editable, textToHiglight, firstColoredIndex);
            } else if (Arrays.asList(MimeTypes.MIME_SQL).contains(fileExtension)) {
                color(Patterns.SYMBOLS, editable, textToHiglight, firstColoredIndex);
                color(Patterns.GENERAL_STRINGS, editable, textToHiglight, firstColoredIndex);
                color(Patterns.SQL_KEYWORDS, editable, textToHiglight, firstColoredIndex);
            } else {
                if(!fileExtension.contains("md"))
                    color(Patterns.GENERAL_KEYWORDS, editable, textToHiglight, firstColoredIndex);
                color(Patterns.NUMBERS, editable, textToHiglight, firstColoredIndex);
                color(Patterns.SYMBOLS, editable, textToHiglight, firstColoredIndex);
                color(Patterns.GENERAL_STRINGS, editable, textToHiglight, firstColoredIndex);
                if (fileExtension.equals("prop") || fileExtension.contains("conf") || fileExtension.contains("md"))
                    color(Patterns.GENERAL_COMMENTS_NO_SLASH, editable, textToHiglight, firstColoredIndex);
                else
                    color(Patterns.GENERAL_COMMENTS, editable, textToHiglight, firstColoredIndex);

                if(fileExtension.contains("md"))
                    color(Patterns.LINK, editable, textToHiglight, firstColoredIndex);
            }

            return editable;
        }

        private void color(Pattern pattern,
                           Editable allText,
                           CharSequence textToHiglight,
                           int start) {
            int color = 0;
            if (pattern.equals(Patterns.HTML_OPEN_TAGS)
                    || pattern.equals(Patterns.HTML_CLOSE_TAGS)
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
            } else if (pattern.equals(Patterns.NUMBERS) || pattern.equals(Patterns.SYMBOLS)) {
                color = getResources().getColor(R.color.syntax_number);
            } else if (pattern.equals(Patterns.PHP_VARIABLES)) {
                color = getResources().getColor(R.color.syntax_variable);
            }

            m = pattern.matcher(textToHiglight);

            while (m.find()) {
                allText.setSpan(
                        new ForegroundColorSpan(color),
                        start + m.start(),
                        start + m.end(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        /**
         * Disconnect this undo/redo from the text
         * view.
         */
        boolean enabledChangeListener = false;
        public void disableTextChangedListener() {
            enabledChangeListener = false;
            removeTextChangedListener(mChangeListener);
        }
        public void enableTextChangedListener() {
            if(!enabledChangeListener) {
                addTextChangedListener(mChangeListener);
                enabledChangeListener = true;
            }
        }

        public void setReadOnly(boolean value){
            if(value) {
                keyListener = getKeyListener();
                setKeyListener(null);
            } else {
                if(keyListener != null)
                    setKeyListener(keyListener);
            }
        }
        //endregion

        //region UNDO REDO

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

        /**
         * Clear history.
         */
        public void clearHistory() {
            mEditHistory.clear();
            mShowUndo = getCanUndo();
            mShowRedo = getCanRedo();
        }

        /**
         * Can undo be performed?
         */
        public boolean getCanUndo() {
            return (mEditHistory.mmPosition > 0);
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
         * Can redo be performed?
         */
        public boolean getCanRedo() {
            return (mEditHistory.mmPosition
                    < mEditHistory.mmHistory.size());
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
                if(!canSaveFile)
                    canSaveFile = getCanUndo();
                if (showUndo != mShowUndo || showRedo != mShowRedo) {
                    mShowUndo = showUndo;
                    mShowRedo = showRedo;
                    EventBus.getDefault().post(new EventBusEvents.InvalideTheMenu());
                }

                editorInterface.updateTextSyntax();
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