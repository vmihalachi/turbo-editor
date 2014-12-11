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

package sharedcode.turboeditor.views;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ScrollView;

public class GoodScrollView extends ScrollView {

    public ScrollInterface scrollInterface;
    int lastY;
    boolean listenerEnabled = true;

    public GoodScrollView(Context context) {
        super(context);
    }

    public GoodScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GoodScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setScrollInterface(ScrollInterface scrollInterface) {
        this.scrollInterface = scrollInterface;
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);

        if (scrollInterface == null || !listenerEnabled) return;

        if (Math.abs(lastY - t) > 100) {
            lastY = t;
            scrollInterface.onScrollChanged(l, t, oldl, oldt);
        }

    }

    public boolean hasReachedBottom() {
        View firstChild = getChildAt(getChildCount() - 1);

        int diff = (firstChild.getBottom() - (getHeight() + getScrollY() + firstChild.getTop()));// Calculate the scrolldiff
        return diff <= 0;
    }

    public void tempDisableListener(int mills) {
        listenerEnabled = false;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                listenerEnabled = true;
            }
        }, mills);
    }


    public interface ScrollInterface {
        public void onScrollChanged(int l, int t, int oldl, int oldt);
    }
}
