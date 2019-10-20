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

package sharedcode.turboeditor.util;

import android.graphics.Matrix;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by Artem on 21.01.14.
 */
public class ViewUtils {

    private static final String TAG = "ViewUtils";

    @NonNull
    private static final MotionEventHandler MOTION_EVENT_HANDLER = Device.hasKitKatApi()
            ? new MotionEventHandlerReflection()
            : new MotionEventHandlerCompat();

    public static boolean isTouchPointInView(@NonNull View view, float x, float y) {
        final int[] coordinates = new int[3];
        view.getLocationInWindow(coordinates);
        int left = coordinates[0];
        int top = coordinates[1];
        return x >= left && x <= left + view.getWidth() &&
                y >= top && y <= top + view.getHeight();
    }

    public static int getLeft(@NonNull View view) {
        final int[] coordinates = new int[3];
        view.getLocationInWindow(coordinates);
        return coordinates[0];
    }

    public static int getTop(@NonNull View view) {
        final int[] coordinates = new int[3];
        view.getLocationInWindow(coordinates);
        return coordinates[1];
    }

    public static int getBottom(@NonNull View view) {
        return getTop(view) + view.getHeight();
    }

    public static int indexOf(@NonNull ViewGroup container, @NonNull View view) {
        int length = container.getChildCount();
        for (int i = 0; i < length; i++) {
            View child = container.getChildAt(i);
            assert child != null;

            if (child.equals(view)) {
                return i;
            }
        }
        return -1;
    }

    // //////////////////////////////////////////
    // //////////// -- VISIBILITY -- ////////////
    // //////////////////////////////////////////

    public static void setVisible(@NonNull View view, boolean visible) {
        setVisible(view, visible, View.GONE);
    }

    public static void setVisible(@NonNull View view, boolean visible, int invisibleFlag) {
        int visibility = view.getVisibility();
        int visibilityNew = visible ? View.VISIBLE : invisibleFlag;

        if (visibility != visibilityNew) {
            view.setVisibility(visibilityNew);
        }
    }

    public static void safelySetText(@NonNull TextView textView, @Nullable CharSequence text) {
        final boolean visible = text != null;
        if (visible) textView.setText(text);
        ViewUtils.setVisible(textView, visible);
    }

    // //////////////////////////////////////////
    // /////////// -- TOUCH EVENTS -- ///////////
    // //////////////////////////////////////////

    public static boolean pointInView(@NonNull View view, float localX, float localY, float slop) {
        return localX >= view.getLeft() - slop
                && localX < view.getRight() + slop
                && localY >= view.getTop() - slop
                && localY < view.getBottom() + slop;
    }

    /**
     * Transforms a motion event from view-local coordinates to on-screen
     * coordinates.
     *
     * @param ev the view-local motion event
     * @return false if the transformation could not be applied
     */
    public static boolean toGlobalMotionEvent(@NonNull View view, @NonNull MotionEvent ev) {
        return MOTION_EVENT_HANDLER.toGlobalMotionEvent(view, ev);
    }

    /**
     * Transforms a motion event from on-screen coordinates to view-local
     * coordinates.
     *
     * @param ev the on-screen motion event
     * @return false if the transformation could not be applied
     */
    public static boolean toLocalMotionEvent(@NonNull View view, @NonNull MotionEvent ev) {
        return MOTION_EVENT_HANDLER.toLocalMotionEvent(view, ev);
    }

    private static abstract class MotionEventHandler {

        /**
         * Transforms a motion event from view-local coordinates to on-screen
         * coordinates.
         *
         * @param ev the view-local motion event
         * @return false if the transformation could not be applied
         */
        abstract boolean toGlobalMotionEvent(@NonNull View view, @NonNull MotionEvent ev);

        /**
         * Transforms a motion event from on-screen coordinates to view-local
         * coordinates.
         *
         * @param ev the on-screen motion event
         * @return false if the transformation could not be applied
         */
        abstract boolean toLocalMotionEvent(@NonNull View view, @NonNull MotionEvent ev);

    }

    private static final class MotionEventHandlerReflection extends MotionEventHandler {

        @Override
        boolean toGlobalMotionEvent(@NonNull View view, @NonNull MotionEvent ev) {
            return toMotionEvent(view, ev, "toGlobalMotionEvent");
        }

        @Override
        boolean toLocalMotionEvent(@NonNull View view, @NonNull MotionEvent ev) {
            return toMotionEvent(view, ev, "toLocalMotionEvent");
        }

        private boolean toMotionEvent(View view, MotionEvent ev, String methodName) {
            try {
                Method method = View.class.getDeclaredMethod(methodName, MotionEvent.class);
                method.setAccessible(true);
                return (boolean) method.invoke(view, ev);
            } catch (InvocationTargetException
                    | IllegalAccessException
                    | NoSuchMethodException
                    | NoClassDefFoundError e) {
                Log.wtf(TAG, "Failed to access motion event transforming!!!");
                e.printStackTrace();
            }
            return false;
        }

    }

    private static final class MotionEventHandlerCompat extends MotionEventHandler {

        @Nullable
        private static int[] getWindowPosition(@NonNull View view) {
            Object info;
            try {
                Field field = View.class.getDeclaredField("mAttachInfo");
                field.setAccessible(true);
                info = field.get(view);
            } catch (Exception e) {
                info = null;
                Log.e(TAG, "Failed to get AttachInfo.");
            }

            if (info == null) {
                return null;
            }

            int[] position = new int[2];

            try {
                Class clazz = Class.forName("android.view.View$AttachInfo");

                Field field = clazz.getDeclaredField("mWindowLeft");
                field.setAccessible(true);
                position[0] = field.getInt(info);

                field = clazz.getDeclaredField("mWindowTop");
                field.setAccessible(true);
                position[1] = field.getInt(info);
            } catch (Exception e) {
                Log.e(TAG, "Failed to get window\'s position from AttachInfo.");
                return null;
            }

            return position;
        }

        /**
         * Recursive helper method that applies transformations in post-order.
         *
         * @param ev the on-screen motion event
         */
        private static void transformMotionEventToLocal(@NonNull View view, @NonNull MotionEvent ev) {
            final ViewParent parent = view.getParent();
            if (parent instanceof View) {
                final View vp = (View) parent;
                transformMotionEventToLocal(vp, ev);
                ev.offsetLocation(vp.getScrollX(), vp.getScrollY());
            } // TODO: Use reflections to access ViewRootImpl
            // else if (parent instanceof ViewRootImpl) {
            //    final ViewRootImpl vr = (ViewRootImpl) parent;
            //    ev.offsetLocation(0, vr.mCurScrollY);
            // }

            ev.offsetLocation(-view.getLeft(), -view.getTop());

            Matrix matrix = view.getMatrix();
            if (matrix != null) {
                ev.transform(matrix);
            }
        }

        /**
         * Recursive helper method that applies transformations in pre-order.
         *
         * @param ev the on-screen motion event
         */
        private static void transformMotionEventToGlobal(@NonNull View view, @NonNull MotionEvent ev) {
            Matrix matrix = view.getMatrix();
            if (matrix != null) {
                ev.transform(matrix);
            }

            ev.offsetLocation(view.getLeft(), view.getTop());

            final ViewParent parent = view.getParent();
            if (parent instanceof View) {
                final View vp = (View) parent;
                ev.offsetLocation(-vp.getScrollX(), -vp.getScrollY());
                transformMotionEventToGlobal(vp, ev);
            } // TODO: Use reflections to access ViewRootImpl
            // else if (parent instanceof ViewRootImpl) {
            //    final ViewRootImpl vr = (ViewRootImpl) parent;
            //    ev.offsetLocation(0, -vr.mCurScrollY);
            // }
        }

        @Override
        boolean toGlobalMotionEvent(@NonNull View view, @NonNull MotionEvent ev) {
            final int[] windowPosition = getWindowPosition(view);
            if (windowPosition == null) {
                return false;
            }

            transformMotionEventToGlobal(view, ev);
            ev.offsetLocation(windowPosition[0], windowPosition[1]);
            return true;
        }

        @Override
        boolean toLocalMotionEvent(@NonNull View view, @NonNull MotionEvent ev) {
            final int[] windowPosition = getWindowPosition(view);
            if (windowPosition == null) {
                return false;
            }

            ev.offsetLocation(-windowPosition[0], -windowPosition[1]);
            transformMotionEventToLocal(view, ev);
            return true;
        }
    }

}
