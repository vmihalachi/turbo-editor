/*******************************************************************************
 * Copyright (c) 2012, 2013 Vlad Mihalachi
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.vmihalachi.turboeditor;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Selection;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import com.vmihalachi.turboeditor.helper.PixelDipConverter;
import com.vmihalachi.turboeditor.helper.PreferenceHelper;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity
        extends Activity
        implements EditTextDialog.EditDialogListener {

    private static final String TAG = "A0A";
    private Editor mEditText;
    private String fileName;
    private static String fileExtension;
    private String filePath;
    private String currentEncoding;
    public static boolean mColorSyntax;
    public static boolean mCountLines;

    @Override
    protected void onCreate(
            final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /** Set Content View */
        setContentView(R.layout.editor_container);

        /** Views */
        mEditText = (Editor) findViewById(R.id.editor);

        /** Class Variables */
        this.mCountLines = PreferenceHelper.getWrapText(this);
        this.mColorSyntax = PreferenceHelper.getSyntaxHiglight(this);
        this.currentEncoding = PreferenceHelper.getEncoding(this);

        // Get intent, action and MIME type
        final Intent intent = getIntent();
        final String action = intent.getAction();
        final String type = intent.getType();

        if (Intent.ACTION_VIEW.equals(action)
                || Intent.ACTION_EDIT.equals(action)
                || Intent.ACTION_PICK.equals(action)
                && type != null) {
            final File fileOpened =
                    new File(intent.getData().getPath());
            this.fileName = fileOpened.getName();
            this.fileExtension =
                    FilenameUtils.getExtension(fileName);
            this.filePath =
                    fileOpened.getAbsolutePath();
        } else {
            Toast.makeText(this,
                    getString(R.string.err_occured),
                    Toast.LENGTH_SHORT).show();
            finish();
        }

        final ActionBar actionBar =
                getActionBar();
        actionBar.setTitle(this.fileName);
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(true);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final FileInputStream inputStream =
                            new FileInputStream(
                                    new File(filePath));
                    final String fileText =
                            IOUtils.toString(inputStream,
                                    currentEncoding);
                    runOnUiThread(
                            new SetTextRunnable(fileText));
                    inputStream.close();
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage(), e);
                    Toast.makeText(MainActivity.this,
                            e.getMessage(), Toast.LENGTH_SHORT)
                            .show();
                }
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        try {
            final InputMethodManager inputManager =
                    (InputMethodManager) getSystemService(
                            Context.INPUT_METHOD_SERVICE);

            inputManager.hideSoftInputFromWindow(
                    getCurrentFocus().getWindowToken(),
                    InputMethodManager.HIDE_NOT_ALWAYS);
        } catch (NullPointerException e) {
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(
            final Menu menu) {
        getMenuInflater().inflate(
                R.menu.activity_fileeditor, menu);
        menu.findItem(R.id.im_editor_wrap)
                .setChecked(this.mCountLines);
        menu.findItem(R.id.im_syntax_highlight)
                .setChecked(this.mColorSyntax);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(
            final MenuItem item) {
        int i = item.getItemId();
        if (i == R.id.im_save) {
            new SaveFile().execute();
        } else if (i == R.id.im_undo) {
            this.mEditText
                    .onKeyShortcut(KeyEvent.KEYCODE_Z,
                            new KeyEvent(KeyEvent.ACTION_DOWN,
                                    KeyEvent.KEYCODE_Z));
        } else if (i == R.id.im_redo) {
            this.mEditText
                    .onKeyShortcut(KeyEvent.KEYCODE_Y,
                            new KeyEvent(KeyEvent.ACTION_DOWN,
                                    KeyEvent.KEYCODE_Y));
        } else if (i == R.id.im_editor_encoding) {
            showEncodingDialog();
        } else if (i == R.id.im_syntax_highlight) {
            item.setChecked(!item.isChecked());
            PreferenceHelper.setSyntaxHiglight(this,
                    item.isChecked());
            updateTextEditor();
        } else if (i == R.id.im_editor_wrap) {
            item.setChecked(!item.isChecked());
            PreferenceHelper.setWrapText(this,
                    item.isChecked());
            updateTextEditor();
        } else if (i == R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    private synchronized void showEncodingDialog() {
        final EditTextDialog dialogFrag =
                EditTextDialog.newInstance(this.currentEncoding);
        dialogFrag.show(
                getFragmentManager().beginTransaction(),
                "dialog");
    }

    @Override
    public void onFinishEditDialog(
            final String inputText,
            final EditTextDialog.Actions action,
            final String hint) {
        PreferenceHelper.setEncoding(this, inputText);
        updateTextEditor();
    }

    private synchronized void updateTextEditor() {
        final boolean wrap_text =
                PreferenceHelper.getWrapText(this);
        final boolean syntax_highlight =
                PreferenceHelper.getSyntaxHiglight(this);
        final String encoding =
                PreferenceHelper.getEncoding(this);

        if (this.mCountLines != wrap_text) {
            this.mCountLines = wrap_text;
            mEditText.invalidate();

            if (this.mCountLines) {
                int paddingLeft = (int) PixelDipConverter.convertDpToPixel(25, this);
                mEditText.setPadding(paddingLeft, 0, 0, 0);
            } else {
                int paddingLeft = (int) PixelDipConverter.convertDpToPixel(5, this);
                mEditText.setPadding(paddingLeft, 0, 0, 0);
            }
        }

        if (this.mColorSyntax != syntax_highlight) {
            this.mColorSyntax = syntax_highlight;
            mEditText.setText(mEditText.getText().toString());
        }

        if (!this.currentEncoding.equals(encoding)) {
            try {
                final byte[] oldText = this.mEditText
                        .getText()
                        .toString()
                        .getBytes(this.currentEncoding);
                this.mEditText
                        .setText(new String(oldText, encoding));
                this.currentEncoding = encoding;
            } catch (UnsupportedEncodingException ignored) {
            }
        }
    }

    private class SaveFile extends AsyncTask<Void, Void, Boolean> {

        Exception exception;

        @Override
        protected Boolean doInBackground(
                final Void... voids) {
            try {
                FileUtils.write(new File(
                        MainActivity.this.filePath),
                        MainActivity.this.mEditText
                                .getText(),
                        MainActivity.this.currentEncoding);
                return true;
            } catch (IOException e) {
                Log.e(TAG, e.getMessage(), e);
                exception = e;
                return false;
            }
        }

        @Override
        protected void onPostExecute(
                final Boolean success) {
            super.onPostExecute(success);
            String message = success ? String.format(getString(R.string.file_saved_with_success), fileName) : exception.getMessage();
            Toast.makeText(getBaseContext(),
                    message,
                    Toast.LENGTH_SHORT).show();

            //final Intent returnIntent = new Intent();
            //setResult(Activity.RESULT_OK, returnIntent);
            //finish();
        }
    }

    private class SetTextRunnable implements Runnable {
        private final String fileText;

        public SetTextRunnable(String fileText) {
            this.fileText = fileText;
        }

        @Override
        public void run() {
            mEditText.setText(fileText);
        }
    }

    public static class Editor extends EditText {
        protected static final int
                ID_SELECT_ALL =android. R.id.selectAll,
                ID_CUT = android.R.id.cut,
                ID_COPY = android.R.id.copy,
                ID_PASTE = android.R.id.paste,
                ID_UNDO = R.id.undo,
                ID_REDO = R.id.redo;
        private static final int SYNTAX_DELAY_MILLIS =
                0;
        private static final float textSize = 16;
        private final Handler updateHandler =
                new Handler();
        private final TextPaint mPaintNumbers =
                new TextPaint();
        //private final Rect mLineBounds = new Rect();
        private final float mScale;
        private boolean modified = true;

        /**
         * Is undo/redo being performed? This member
         * signals if an undo/redo operation is
         * currently being performed. Changes in the
         * text during undo/redo are not recorded
         * because it would mess up the undo history.
         */
        private boolean mIsUndoOrRedo = false;

        /**
         * The edit history.
         */
        private EditHistory mEditHistory;

        /**
         * The change listener.
         */
        private EditTextChangeListener
                mChangeListener;

        private final Runnable updateRunnable =
                new Runnable() {
                    @Override
                    public void run() {
                        replaceTextKeepCursor(getText());
                    }
                };

        public Editor(Context context,
                      AttributeSet attrs) {
            super(context, attrs);
            this.mScale = context.getResources()
                    .getDisplayMetrics().density;
            init(context);
        }

        // Init the class
        private void init(final Context context) {
            mEditHistory = new EditHistory();
            mChangeListener =
                    new EditTextChangeListener();
            addTextChangedListener(mChangeListener);

            this.mPaintNumbers
                        .setColor(
                                getTextColors().getDefaultColor());
            this.mPaintNumbers
                        .setTextSize(
                                textSize * this.mScale * 0.8f);
            this.mPaintNumbers.setAntiAlias(true);

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

        @Override
        public boolean onKeyShortcut(
                final int keyCode, final KeyEvent event) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_A:
                    return onTextContextMenuItem(
                            ID_SELECT_ALL);
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
                    break;
                case KeyEvent.KEYCODE_Y:
                    if (getCanRedo()) {
                        return onTextContextMenuItem(ID_REDO);
                    }
                    break;
            }

            return super.onKeyShortcut(keyCode, event);
        }

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
            if (mCountLines) {
                final int max = getLineCount();
                final TextPaint paint = mPaintNumbers;
                for (int min = 0; min < max; min++) {
                    canvas.drawText(String.valueOf(min + 1),
                            0,
                            getLineBounds(min, null),
                            paint);
                }
            }
            super.onDraw(canvas);
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

        private void cancelUpdate() {
            updateHandler.removeCallbacks(
                    updateRunnable);
        }

        private void replaceTextKeepCursor(
                Editable e) {
            int p = getSelectionStart();

            replaceText(e);

            if (p > -1) {
                setSelection(p);
            }
        }

        private void replaceText(Editable e) {
            disconnect();
            modified = false;
            setText(highlight(e));
            modified = true;
            addTextChangedListener(mChangeListener);
        }

        private CharSequence highlight(
                Editable editable) {
            editable.clearSpans();

            if (editable.length() == 0) {
                return editable;
            }

            if (fileExtension.contains("html")
                    || fileExtension.contains("xml")) {
                color(Patterns.HTML_OPEN_TAGS, editable);
                color(Patterns.HTML_CLOSE_TAGS, editable);
                color(Patterns.HTML_ATTRS, editable);
                color(Patterns.GENERAL_STRINGS, editable);
                color(Patterns.XML_COMMENTS, editable);
            } else if (fileExtension.equals("css")) {
                //color(CSS_STYLE_NAME, editable);
                color(Patterns.CSS_ATTRS, editable);
                color(Patterns.CSS_ATTR_VALUE, editable);
                color(Patterns.GENERAL_COMMENTS, editable);
            } else if (fileExtension.equals("js")) {
                color(Patterns.GENERAL_KEYWORDS, editable);
                color(Patterns.NUMBERS, editable);
                color(Patterns.GENERAL_COMMENTS, editable);
            } else {
                color(Patterns.GENERAL_KEYWORDS, editable);
                color(Patterns.NUMBERS, editable);
                color(Patterns.GENERAL_COMMENTS, editable);
            }

            return editable;
        }

        private void color(Pattern pattern,
                           Editable editable) {
            int color = 0;
            if (pattern.equals(Patterns.HTML_OPEN_TAGS)
                    || pattern.equals(Patterns.HTML_CLOSE_TAGS)
                    || pattern.equals(Patterns.GENERAL_KEYWORDS)
                //|| pattern.equals(CSS_STYLE_NAME)
                    ) {
                color = Patterns.COLOR_KEYWORD;
            } else if (pattern.equals(Patterns.HTML_ATTRS)
                    || pattern.equals(Patterns.CSS_ATTRS)) {
                color = Patterns.COLOR_ATTR;
            } else if (pattern.equals(Patterns.CSS_ATTR_VALUE)) {
                color = Patterns.COLOR_ATTR_VALUE;
            } else if (pattern.equals(Patterns.XML_COMMENTS)
                    || pattern.equals(Patterns.GENERAL_COMMENTS)) {
                color = Patterns.COLOR_COMMENT;
            } else if (pattern.equals(
                    Patterns.GENERAL_STRINGS)) {
                color = Patterns.COLOR_STRING;
            } else if (pattern.equals(Patterns.NUMBERS)) {
                color = Patterns.COLOR_NUMBER;
            }

            for (final Matcher m =
                         pattern.matcher(editable);
                 m.find(); ) {
                editable.setSpan(
                        new ForegroundColorSpan(color),
                        m.start(),
                        m.end(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        // =================================================================== //

        /**
         * Disconnect this undo/redo from the text
         * view.
         */
        public void disconnect() {
            removeTextChangedListener(mChangeListener);
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

        /**
         * Clear history.
         */
        public void clearHistory() {
            mEditHistory.clear();
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
            if (mEditHistory.mmPosition == -1) {
                return false;
            }

            return true;
        }

        // =================================================================== //

        /**
         * Keeps track of all the edit history of a
         * text.
         */
        private final class EditHistory {

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

            /**
             * The list of edits in chronological
             * order.
             */
            private final LinkedList<EditItem>
                    mmHistory = new LinkedList<EditItem>();

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
                cancelUpdate();

                if (!mColorSyntax || !modified) {
                    return;
                }

                updateHandler.postDelayed(
                        updateRunnable,
                        SYNTAX_DELAY_MILLIS);
            }
        }
    }
}
