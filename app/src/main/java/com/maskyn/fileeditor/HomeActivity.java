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

import shared.turboeditor.activity.MainActivity;

public class HomeActivity extends MainActivity {

    private AdsHelper adsHelper;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // TODO: move to firebase-crahslytics
//        if(shared.turboeditor.preferences.PreferenceHelper.getSendErrorReports(this))
//            Fabric.with(this, new Crashlytics());
        // setup the ads
        if(!shared.turboeditor.util.ProCheckUtils.isPro(this))
            adsHelper = new AdsHelper(this);
    }

    @Override
    public boolean showInterstitial() {
        if(adsHelper != null && !shared.turboeditor.util.ProCheckUtils.isPro(this)) {
            adsHelper.displayInterstitial();
            return true;
        }
        else {
            return false;
        }
    }


}
