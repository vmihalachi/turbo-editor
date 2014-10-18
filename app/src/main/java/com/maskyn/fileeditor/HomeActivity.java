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

package com.maskyn.fileeditor;

import android.os.Bundle;

import com.crashlytics.android.Crashlytics;

import sharedcode.turboeditor.activity.BaseHomeActivity;
import sharedcode.turboeditor.preferences.PreferenceHelper;
import sharedcode.turboeditor.util.ProCheckUtils;

public class HomeActivity extends BaseHomeActivity {

    private AdsHelper adsHelper;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(PreferenceHelper.getSendErrorReports(this))
            Crashlytics.start(this);
        // setup the ads
        if(!ProCheckUtils.isPro(this))
            adsHelper = new AdsHelper(this);
    }

    @Override
    public void displayInterstitial() {
        if(adsHelper != null && !ProCheckUtils.isPro(this))
            adsHelper.displayInterstitial();
    }
}
