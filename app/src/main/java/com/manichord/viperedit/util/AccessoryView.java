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

package com.manichord.viperedit.util;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.manichord.viperedit.R;
import com.manichord.viperedit.preferences.PreferenceHelper;

/**
 * Created by mac on 15/02/15.
 */
public class AccessoryView extends LinearLayout {

    public IAccessoryView iAccessoryView;
    private TypedValue outValue;

    public AccessoryView(Context context) {
        super(context);
        init();
    }

    public AccessoryView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AccessoryView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setOrientation(HORIZONTAL);
        createView();
    }

    public void setInterface(IAccessoryView iBreadcrumb) {
        this.iAccessoryView = iBreadcrumb;
    }


    public void createView() {
        removeAllViews();

        // If we're running on Honeycomb or newer, then we can use the Theme's
        // selectableItemBackground to ensure that the View has a pressed state
        outValue = new TypedValue();
        getContext().getTheme().resolveAttribute(R.attr.selectableItemBackgroundBorderless, outValue, true);

        String[] characters = {
                "<", ">", "!", "/", ".", ";", "=", "\"", "{", "}", "[", "]", "(", ")", "&", "|", "#", "*", "+", "-", ":", "%", ",", "_", "@", "?", "^", "'",
        };

        for (int i = 0; i < characters.length; i++)
                addAButton(characters[i]);

        updateTextColors();
    }

    private void addAButton(final String text) {
        int dimension = (int) PixelDipConverter.convertDpToPixel(50, getContext());
        //int padding = (int) PixelDipConverter.convertDpToPixel(10, getContext());
        final Button name = new Button(getContext());

        name.setLayoutParams(new LinearLayout.LayoutParams(dimension, dimension));


        name.setGravity(Gravity.CENTER);

        name.setText(text);
        name.setTextSize(15);
        name.setAllCaps(true);

        //name.setPadding(padding, padding, padding, padding);

        name.setClickable(true);

        name.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                iAccessoryView.onButtonAccessoryViewClicked(text);
            }
        });

        name.setBackgroundResource(outValue.resourceId);
        addView(name);
    }

    public void updateTextColors() {
        boolean isLightTheme = PreferenceHelper.getLightTheme(getContext());
        for (int i = 0; i < getChildCount(); i++) {
            ((Button) getChildAt(i)).setTextColor(isLightTheme ? getResources().getColor(android.R.color.background_dark) : getResources().getColor(android.R.color.white));
        }
    }

    public interface IAccessoryView {
        public void onButtonAccessoryViewClicked(String text);
    }
}
