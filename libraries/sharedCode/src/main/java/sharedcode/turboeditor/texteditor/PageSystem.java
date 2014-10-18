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

package sharedcode.turboeditor.texteditor;

import android.content.Context;

import java.util.LinkedList;
import java.util.List;

import sharedcode.turboeditor.preferences.PreferenceHelper;

public class PageSystem {

    private List<String> pages;
    private int[] startingLines;
    private int currentPage = 0;
    private PageSystemInterface pageSystemInterface;

    public PageSystem(Context context, PageSystemInterface pageSystemInterface, String text) {
        this.pageSystemInterface = pageSystemInterface;
        pages = new LinkedList<>();

        int i = 0;
        int charForPage = 15000;
        int maxLenghtInOnePage = 30000;
        int to;
        int indexOfReturn;
        int textLenght = text.length();
        boolean pageSystemEnabled = PreferenceHelper.getPageSystemEnabled(context);
        if (pageSystemEnabled && textLenght > maxLenghtInOnePage) {
            while (i < textLenght && pageSystemEnabled) {
                to = i + charForPage;
                indexOfReturn = text.indexOf("\n", to);
                if (indexOfReturn > to) to = indexOfReturn;
                if (to > text.length()) to = text.length();
                pages.add(text.substring(i, to));
                i = to + 1;
            }


            if (i == 0)
                pages.add("");
        } else {
            pages.add(text);
        }

        startingLines = new int[pages.size()];
        setStartingLines();
    }

    public int getStartingLine() {
        return startingLines[currentPage];
    }

    public String getCurrentPageText() {
        return pages.get(currentPage);
    }

    public String getTextOfNextPages(boolean includeCurrent, int nOfPages) {
        StringBuilder stringBuilder = new StringBuilder();
        int i;
        for (i = includeCurrent ? 0 : 1; i < nOfPages; i++) {
            if (pages.size() > (currentPage + i)) {
                stringBuilder.append(pages.get(currentPage + 1));
            }
        }

        return stringBuilder.toString();
    }

    public void savePage(String currentText) {
        pages.set(currentPage, currentText);
    }

    public void nextPage() {
        if (!canReadNextPage()) return;
        goToPage(currentPage + 1);
    }

    public void prevPage() {
        if (!canReadPrevPage()) return;
        goToPage(currentPage - 1);
    }

    public void goToPage(int page) {
        if (page >= pages.size())
        	page = pages.size() - 1;
        if (page < 0)
        	page = 0;
        boolean shouldUpdateLines = page > currentPage && canReadNextPage();
        if (shouldUpdateLines) {
            String text = getCurrentPageText();
            int nOfNewLineNow = (text.length() - text.replace("\n", "").length()) + 1; // normally the last line is not counted so we have to add 1
            int nOfNewLineBefore = startingLines[currentPage + 1] - startingLines[currentPage];
            int difference = nOfNewLineNow - nOfNewLineBefore;
            updateStartingLines(currentPage + 1, difference);
        }
        currentPage = page;
        pageSystemInterface.onPageChanged(page);
    }

    public void setStartingLines() {
        int startingLine;
        int nOfNewLines;
        String text;
        startingLines[0] = 0;
        for (int i = 1; i < pages.size(); ++i) {
            text = pages.get(i - 1);
            nOfNewLines = text.length() - text.replace("\n", "").length() + 1;
            startingLine = startingLines[i - 1] + nOfNewLines;
            startingLines[i] = startingLine;
        }
    }

    public void updateStartingLines(int fromPage, int difference) {
        if (difference == 0)
            return;
        if (fromPage < 1)
        	fromPage = 1;
        for (int i = fromPage; i < pages.size(); ++i) {
            startingLines[i] += difference;
        }
    }

    public int getMaxPage() {
        return pages.size() - 1;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public String getAllText(String currentPageText) {
        pages.set(currentPage, currentPageText);
        StringBuilder allText = new StringBuilder();
        for (String page : pages) {
        	allText.append(page).append("\n");
        }
        return allText.toString();
    }

    public boolean canReadNextPage() {
        return currentPage < pages.size() - 1;
    }

    public boolean canReadPrevPage() {
        return currentPage >= 1;
    }

    public interface PageSystemInterface {
        void onPageChanged(int page);
    }
}
