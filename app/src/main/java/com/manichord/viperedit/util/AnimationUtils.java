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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;
import android.view.View;

public class AnimationUtils {
    public static Bundle getScaleBundle(View view) {
        return ActivityOptionsCompat.makeScaleUpAnimation(
                view, 0, 0, view.getWidth(), view.getHeight()).toBundle();
    }

    public static void startActivityWithScale(@NonNull Activity startActivity, @NonNull Intent subActivity, @NonNull boolean forResult, @Nullable int code, @NonNull View view) {
        if(forResult){
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN)
                startActivity.startActivityForResult(subActivity, code, AnimationUtils.getScaleBundle
                        (view));
            else
                startActivity.startActivityForResult(subActivity, code);
        }
        else {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN)
                startActivity.startActivity(subActivity, AnimationUtils.getScaleBundle
                        (view));
            else
                startActivity.startActivity(subActivity);
        }
    }
}
