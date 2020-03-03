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

package com.manichord.viperedit.util.systemui;

import android.app.Activity;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.view.WindowManager;

class SystemUiHelperImplHC extends SystemUiHelper.SystemUiHelperImpl
            implements View.OnSystemUiVisibilityChangeListener {

        final View mDecorView;

        SystemUiHelperImplHC(Activity activity, int level, int flags,
                             SystemUiHelper.OnVisibilityChangeListener onVisibilityChangeListener) {
            super(activity, level, flags, onVisibilityChangeListener);

            mDecorView = activity.getWindow().getDecorView();
            mDecorView.setOnSystemUiVisibilityChangeListener(this);
        }


        @Override
        void show() {
            mDecorView.setSystemUiVisibility(createShowFlags());
        }

        @Override
        void hide() {
            mDecorView.setSystemUiVisibility(createHideFlags());
        }

        @Override
        public final void onSystemUiVisibilityChange(int visibility) {
            if ((visibility & createTestFlags()) != 0) {
                onSystemUiHidden();
            } else {
                onSystemUiShown();
            }
        }

        protected void onSystemUiShown() {
            ActionBar ab = ((AppCompatActivity) mActivity).getSupportActionBar();
            if (ab != null) {
                ab.show();
            }

            mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

            setIsShowing(true);
        }

        protected void onSystemUiHidden() {
            ActionBar ab = ((AppCompatActivity) mActivity).getSupportActionBar();
            if (ab != null) {
                ab.hide();
            }

            mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

            setIsShowing(false);
        }

        protected int createShowFlags() {
            return View.STATUS_BAR_VISIBLE;
        }

        protected int createHideFlags() {
            return View.STATUS_BAR_HIDDEN;
        }

        protected int createTestFlags() {
            return View.STATUS_BAR_HIDDEN;
        }
    }