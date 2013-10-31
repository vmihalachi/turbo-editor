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

package com.vmihalachi.turboeditor.fragment;

import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Typeface;
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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.vmihalachi.turboeditor.R;
import com.vmihalachi.turboeditor.event.ErrorOpeningFileEvent;
import com.vmihalachi.turboeditor.event.FileSavedEvent;
import com.vmihalachi.turboeditor.helper.PixelDipConverter;
import com.vmihalachi.turboeditor.helper.PreferenceHelper;
import com.vmihalachi.turboeditor.util.Patterns;

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

import de.greenrobot.event.EventBus;

public class EditorFragment extends Fragment implements EditDialogFragment.EditDialogListener, SeekbarDialogFragment.onSeekbarDialogDismissed {

    private static final String TAG = "A0A";
    private Editor mEditor;

    // Editor Variables
    static boolean sLineNumbers;
    static boolean sColorSyntax;
    static int sFontSize;
    //
    private boolean mUseMonospace;
    private String mCurrentEncoding;
    private static String sFilePath;


    public static EditorFragment newInstance(String filePath) {
        EditorFragment frag = new EditorFragment();
        Bundle args = new Bundle();
        args.putString("filePath", filePath);
        frag.setArguments(args);
        return frag;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        this.sFilePath = getArguments().getString("filePath");
        this.mCurrentEncoding = PreferenceHelper.getEncoding(getActivity());
        this.mUseMonospace = PreferenceHelper.getUseMonospace(getActivity());
        this.sColorSyntax = PreferenceHelper.getSyntaxHiglight(getActivity());
        this.sLineNumbers = PreferenceHelper.getLineNumbers(getActivity());
        this.sFontSize = PreferenceHelper.getFontSize(getActivity());
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_editor, container, false);
        mEditor = (Editor) rootView.findViewById(R.id.editor);
        if (this.sLineNumbers) {
            int paddingLeft = (int) PixelDipConverter.convertDpToPixel(sFontSize * 1.5f, getActivity());
            mEditor.setPadding(paddingLeft, 0, 0, 0);
        } else {
            int paddingLeft = (int) PixelDipConverter.convertDpToPixel(5, getActivity());
            mEditor.setPadding(paddingLeft, 0, 0, 0);
        }
        if(this.mUseMonospace){
            mEditor.setTypeface(Typeface.MONOSPACE);
        }
        mEditor.setTextSize(sFontSize);
        return rootView;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        String fileName = FilenameUtils.getName(getArguments().getString("filePath"));
        getActivity().getActionBar().setTitle(fileName);
        try {
            final FileInputStream inputStream =
                    new FileInputStream(
                            new File(this.sFilePath));
            mEditor.setText(IOUtils.toString(inputStream, this.mCurrentEncoding));
            inputStream.close();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_LONG).show();
            EventBus.getDefault().post(new ErrorOpeningFileEvent());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_editor, menu);
        menu.findItem(R.id.im_line_numbers).setChecked(this.sLineNumbers);
        menu.findItem(R.id.im_syntax_highlight).setChecked(this.sColorSyntax);
        menu.findItem(R.id.im_use_monospace).setChecked(this.mUseMonospace);
        super.onCreateOptionsMenu(menu, inflater);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int i = item.getItemId();
        if (i == R.id.im_save) {
            new SaveFile().execute();
        } else if (i == R.id.im_undo) {
            this.mEditor.onKeyShortcut(KeyEvent.KEYCODE_Z, new KeyEvent(KeyEvent.ACTION_DOWN,
                    KeyEvent.KEYCODE_Z));
        } else if (i == R.id.im_redo) {
            this.mEditor.onKeyShortcut(KeyEvent.KEYCODE_Y, new KeyEvent(KeyEvent.ACTION_DOWN,
                    KeyEvent.KEYCODE_Y));
        } else if (i == R.id.im_editor_encoding) {
            EditDialogFragment dialogFrag = EditDialogFragment.newInstance(EditDialogFragment.Actions.Encoding, this.mCurrentEncoding);
            dialogFrag.setTargetFragment(this, 0);
            dialogFrag.show(getFragmentManager().beginTransaction(), "dialog");
        } else if (i == R.id.im_text_size) {
            int fontMax = 36;
            float scaledDensity = getResources().getDisplayMetrics().scaledDensity;
            int fontCurrent = (int) (mEditor.getTextSize() / scaledDensity);
            SeekbarDialogFragment dialogFrag = SeekbarDialogFragment.newInstance(SeekbarDialogFragment.Actions.FileSize, fontCurrent, fontMax);
            dialogFrag.setTargetFragment(this, 0);
            dialogFrag.show(getFragmentManager().beginTransaction(), "dialog");
        } else if (i == R.id.im_syntax_highlight) {
            item.setChecked(!item.isChecked());
            PreferenceHelper.setSyntaxHiglight(getActivity(), item.isChecked());
            updateTextEditor();
        } else if (i == R.id.im_line_numbers) {
            item.setChecked(!item.isChecked());
            PreferenceHelper.setLineNumbers(getActivity(), item.isChecked());
            updateTextEditor();
        } else if (i == R.id.im_use_monospace) {
            item.setChecked(!item.isChecked());
            PreferenceHelper.setUseMonospace(getActivity(), item.isChecked());
            updateTextEditor();
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onFinishEditDialog(final String inputText, final String hint, final EditDialogFragment.Actions actions) {
        if(actions == EditDialogFragment.Actions.Encoding){
            PreferenceHelper.setEncoding(getActivity(), inputText);
            updateTextEditor();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSeekbarDialogDismissed(SeekbarDialogFragment.Actions action, int value) {
        PreferenceHelper.setFontSize(getActivity(), value);
        updateTextEditor();
    }

    private void updateTextEditor() {
        final boolean wrapText = PreferenceHelper.getLineNumbers(getActivity());
        final boolean syntaxHighlight = PreferenceHelper.getSyntaxHiglight(getActivity());
        final boolean useMonospace = PreferenceHelper.getUseMonospace(getActivity());
        final String encoding = PreferenceHelper.getEncoding(getActivity());
        final int fontSize = PreferenceHelper.getFontSize(getActivity());

        if (this.sLineNumbers != wrapText) {
            this.sLineNumbers = wrapText;
            this.mEditor.setText(this.mEditor.getText().toString());
            if (this.sLineNumbers) {
                int paddingLeft = (int) PixelDipConverter.convertDpToPixel(sFontSize * 1.5f, getActivity());
                mEditor.setPadding(paddingLeft, 0, 0, 0);
            } else {
                int paddingLeft = (int) PixelDipConverter.convertDpToPixel(5, getActivity());
                mEditor.setPadding(paddingLeft, 0, 0, 0);
            }
        }

        if (this.sColorSyntax != syntaxHighlight) {
            this.sColorSyntax = syntaxHighlight;
            this.mEditor.setText(this.mEditor.getText().toString());
        }

        if (this.mUseMonospace != useMonospace) {
            this.mUseMonospace = useMonospace;
            this.mEditor.setTypeface(Typeface.MONOSPACE);
        }

        if (this.sFontSize != fontSize) {
            this.sFontSize = fontSize;
            int paddingLeft = (int) PixelDipConverter.convertDpToPixel(fontSize * 1.5f, getActivity());
            mEditor.setPadding(paddingLeft, 0, 0, 0);
            this.mEditor.setTextSize(fontSize);
        }

        if (!this.mCurrentEncoding.equals(encoding)) {
            try {
                final byte[] oldText = this.mEditor.getText().toString().getBytes(this.mCurrentEncoding);
                this.mEditor.setText(new String(oldText, encoding));
                this.mCurrentEncoding = encoding;
            } catch (UnsupportedEncodingException ignored) {
            }
        }
    }

    class SaveFile extends AsyncTask<Void, Void, Void> {

        /**
         * {@inheritDoc}
         */
        @Override
        protected Void doInBackground(final Void... voids) {
            try {
                FileUtils.write(new File(EditorFragment.this.sFilePath),
                        EditorFragment.this.mEditor.getText(),
                        EditorFragment.this.mCurrentEncoding);
            } catch (IOException e) {
                Log.e(TAG, e.getMessage(), e);
            }
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void onPostExecute(final Void aVoid) {
            super.onPostExecute(aVoid);
            EventBus.getDefault().post(new FileSavedEvent(EditorFragment.this.sFilePath));
        }
    }


    public static class Editor extends EditText {

        protected static final int
                ID_SELECT_ALL = android.R.id.selectAll,
                ID_CUT = android.R.id.cut,
                ID_COPY = android.R.id.copy,
                ID_PASTE = android.R.id.paste,
                ID_UNDO = R.id.im_undo,
                ID_REDO = R.id.im_redo;
        private static final int SYNTAX_DELAY_MILLIS = 800;
        private final Handler updateHandler = new Handler();
        private final TextPaint mPaintNumbers = new TextPaint();
        private boolean modified = true;
        private boolean firstTimeColoring = true;

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
                    /**
                     * {@inheritDoc}
                     */
                    @Override
                    public void run() {
                            replaceTextKeepCursor(getText());
                    }
                };

        public Editor(Context context,
                      AttributeSet attrs) {
            super(context, attrs);
            init();
        }

        // Init the class
        private void init() {
            mEditHistory = new EditHistory();
            mChangeListener = new EditTextChangeListener();
            addTextChangedListener(mChangeListener);

            this.mPaintNumbers.setColor(getTextColors().getDefaultColor());
            this.mPaintNumbers.setAntiAlias(true);

            // Syntax editor
            setFilters(new InputFilter[]{
                    new InputFilter() {
                        /**
                         * {@inheritDoc}
                         */
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
        public void setTextSize(float size) {
            super.setTextSize(size);
            final float scale = getContext().getResources().getDisplayMetrics().density;
            this.mPaintNumbers.setTextSize((int) (size * scale * 0.5f));
        }

        /**
         * {@inheritDoc}
         */
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
         * {@inheritDoc}
         */
        @Override
        public void onDraw(final Canvas canvas) {
            if (EditorFragment.sLineNumbers) {
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
            disconnect();
            modified = false;

            setText(highlight(e));

            modified = true;
            addTextChangedListener(mChangeListener);

            if (p > -1) {
                setSelection(p);
            }
        }

        private CharSequence highlight(Editable editable) {
            final String fileExtension = FilenameUtils.getExtension(EditorFragment.sFilePath);
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
            } else if (fileExtension.equals("php")){
                color(Patterns.HTML_OPEN_TAGS, editable);
                color(Patterns.HTML_CLOSE_TAGS, editable);
                color(Patterns.HTML_ATTRS, editable);
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

                if (!EditorFragment.sColorSyntax || !modified) {
                    return;
                }
                if(firstTimeColoring){
                    firstTimeColoring = false;
                    updateHandler.post(updateRunnable);
                } else {
                    updateHandler.removeCallbacks(updateRunnable);
                    updateHandler.postDelayed(updateRunnable, SYNTAX_DELAY_MILLIS);
                }
            }
        }
    }
}
