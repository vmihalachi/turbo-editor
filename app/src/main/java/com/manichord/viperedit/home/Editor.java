package com.manichord.viperedit.home;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
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
import android.view.KeyEvent;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatEditText;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;

import com.manichord.viperedit.R;
import com.manichord.viperedit.home.texteditor.EditTextPadding;
import com.manichord.viperedit.home.texteditor.LineUtils;
import com.manichord.viperedit.preferences.PreferenceHelper;

public class Editor extends AppCompatEditText {

    private static final int ID_SELECT_ALL = android.R.id.selectAll;
    private static final int ID_CUT = android.R.id.cut;
    private static final int ID_COPY = android.R.id.copy;
    private static final int ID_PASTE = android.R.id.paste;
    private static final int ID_UNDO = R.id.im_undo;
    private static final int ID_REDO = R.id.im_redo;
    private static final int CHARS_TO_COLOR = 2500;

    //region VARIABLES
    private final TextPaint mPaintNumbers = new TextPaint();

    /**
     * The edit history.
     */
    private EditHistory mEditHistory;
    /**
     * The change listener.
     */
    private EditTextChangeListener
            mChangeListener;
    /**
     * Disconnect this undo/redo from the text
     * view.
     */
    private boolean enabledChangeListener;
    private int paddingTop;
    private int numbersWidth;
    private int lineHeight;

    private int lineCount, realLine, startingLine;
    private LineUtils lineUtils;
    /**
     * Is undo/redo being performed? This member
     * signals if an undo/redo operation is
     * currently being performed. Changes in the
     * text during undo/redo are not recorded
     * because it would mess up the undo history.
     */
    private boolean mIsUndoOrRedo;
    private Matcher m;
    private boolean mShowUndo, mShowRedo;
    private boolean canSaveFile;
    private KeyListener keyListener;
    private int firstVisibleIndex, firstColoredIndex, lastVisibleIndex;
    private int deviceHeight;
    private int editorHeight;
    private boolean[] isGoodLineArray;
    private int[] realLines;
    private boolean wrapContent;
    private CharSequence textToHighlight;
    //endregion

    //region CONSTRUCTOR
    public Editor(final Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setupEditor() {
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
        // update the padding of the editor
        updatePadding();

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
        setOnClickListener(v -> {
            if (!PreferenceHelper.getReadOnly(getContext())) {
                MainActivity.verticalScroll.tempDisableListener(1000);
                ((InputMethodManager) getContext().getSystemService(Context
                        .INPUT_METHOD_SERVICE))
                        .showSoftInput(Editor.this, InputMethodManager.SHOW_IMPLICIT);
            }

        });
        setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && !PreferenceHelper.getReadOnly(getContext())) {
                MainActivity.verticalScroll.tempDisableListener(1000);
                ((InputMethodManager) getContext().getSystemService(Context
                        .INPUT_METHOD_SERVICE))
                        .showSoftInput(Editor.this, InputMethodManager.SHOW_IMPLICIT);
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

    public void updatePadding() {
        Context context = getContext();
        if (PreferenceHelper.getLineNumbers(context)) {
            setPadding(
                    EditTextPadding.getPaddingWithLineNumbers(context, PreferenceHelper.getFontSize(context)),
                    EditTextPadding.getPaddingTop(context),
                    EditTextPadding.getPaddingTop(context),
                    0);
        } else {
            setPadding(
                    EditTextPadding.getPaddingWithoutLineNumbers(context),
                    EditTextPadding.getPaddingTop(context),
                    EditTextPadding.getPaddingTop(context),
                    0);
        }
        // add a padding from bottom
        MainActivity.verticalScroll.setPadding(0, 0, 0, EditTextPadding.getPaddingBottom(context));
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

        if (lineCount != getLineCount() || startingLine != MainActivity.Companion.getPageSystem().getStartingLine()) {
            startingLine = MainActivity.Companion.getPageSystem().getStartingLine();
            lineCount = getLineCount();
            lineUtils.updateHasNewLineArray(MainActivity.Companion.getPageSystem()
                    .getStartingLine(), lineCount, getLayout(), getText().toString());

            isGoodLineArray = lineUtils.getGoodLines();
            realLines = lineUtils.getRealLines();

        }

        if (PreferenceHelper.getLineNumbers(getContext())) {
            wrapContent = PreferenceHelper.getWrapContent(getContext());

            for (int i = 0; i < lineCount; i++) {
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
                    getMainActivity().saveTheFile(false);
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
        } else {
            setText(textToUpdate == null ? getEditableText() : textToUpdate);
        }

        enableTextChangedListener();

        int newCursorPos;

        boolean cursorOnScreen = cursorPos >= firstVisibleIndex && cursorPos <= lastVisibleIndex;

        if (cursorOnScreen) { // if the cursor is on screen
            newCursorPos = cursorPos; // we dont change its position
        } else {
            newCursorPos = firstVisibleIndex; // else we set it to the first visible pos
        }

        if (newCursorPos > -1 && newCursorPos <= length()) {
            if (cursorPosEnd != cursorPos)
                setSelection(cursorPos, cursorPosEnd);
            else
                setSelection(newCursorPos);
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
            firstVisibleIndex = getLayout().getLineStart(LineUtils.Companion.getFirstVisibleLine(MainActivity.verticalScroll, editorHeight, lineCount));
            lastVisibleIndex = getLayout().getLineEnd(LineUtils.Companion.getLastVisibleLine(MainActivity.verticalScroll, editorHeight, lineCount, deviceHeight) - 1);
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

        if (TextUtils.isEmpty(MainActivity.Companion.getFileExtension()))
            MainActivity.Companion.setFileExtension("");

        HighlightDriver highlightDriver = new HighlightDriver(new AndroidHighlightColorProvider(),
                MainActivity.Companion.getFileExtension());

        List<HighlightInfo> highlights = highlightDriver.highlightText(textToHighlight, firstColoredIndex);
        for (HighlightInfo info : highlights) {
            editable.setSpan(
                    new ForegroundColorSpan(info.getColor()),
                    info.getStart(),
                    info.getEnd(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
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

    private MainActivity getMainActivity() {
        return (MainActivity) getContext();
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
                getMainActivity().invalidateOptionsMenu();
            }

            getMainActivity().updateTextSyntax();
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