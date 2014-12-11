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

package sharedcode.turboeditor.util.systemui;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.view.View;

@TargetApi(Build.VERSION_CODES.KITKAT)
class SystemUiHelperImplKK extends SystemUiHelperImplJB {

    SystemUiHelperImplKK(Activity activity, int level, int flags,
            SystemUiHelper.OnVisibilityChangeListener onVisibilityChangeListener) {
        super(activity, level, flags, onVisibilityChangeListener);
    }

    @Override
    protected int createHideFlags() {
        int flag = super.createHideFlags();

        if (mLevel == SystemUiHelper.LEVEL_IMMERSIVE) {
            // If the client requested immersive mode, and we're on Android 4.4
            // or later, add relevant flags. Applying HIDE_NAVIGATION without
            // IMMERSIVE prevents the activity from accepting all touch events,
            // so we only do this on Android 4.4 and later (where IMMERSIVE is
            // present).
            flag |= ((mFlags & SystemUiHelper.FLAG_IMMERSIVE_STICKY) != 0)
                            ? View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            : View.SYSTEM_UI_FLAG_IMMERSIVE;
        }

        return flag;
    }

}
