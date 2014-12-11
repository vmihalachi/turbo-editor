

package org.sufficientlysecure.rootcommands.util;

import org.sufficientlysecure.rootcommands.RootCommands;

/**
 * Wraps Android Logging to enable or disable debug output using Constants
 * 
 */
public final class Log {

    public static void v(String tag, String msg) {
        if (RootCommands.DEBUG) {
            android.util.Log.v(tag, msg);
        }
    }

    public static void v(String tag, String msg, Throwable tr) {
        if (RootCommands.DEBUG) {
            android.util.Log.v(tag, msg, tr);
        }
    }

    public static void d(String tag, String msg) {
        if (RootCommands.DEBUG) {
            android.util.Log.d(tag, msg);
        }
    }

    public static void d(String tag, String msg, Throwable tr) {
        if (RootCommands.DEBUG) {
            android.util.Log.d(tag, msg, tr);
        }
    }

    public static void i(String tag, String msg) {
        if (RootCommands.DEBUG) {
            android.util.Log.i(tag, msg);
        }
    }

    public static void i(String tag, String msg, Throwable tr) {
        if (RootCommands.DEBUG) {
            android.util.Log.i(tag, msg, tr);
        }
    }

    public static void w(String tag, String msg) {
        android.util.Log.w(tag, msg);
    }

    public static void w(String tag, String msg, Throwable tr) {
        android.util.Log.w(tag, msg, tr);
    }

    public static void w(String tag, Throwable tr) {
        android.util.Log.w(tag, tr);
    }

    public static void e(String tag, String msg) {
        android.util.Log.e(tag, msg);
    }

    public static void e(String tag, String msg, Throwable tr) {
        android.util.Log.e(tag, msg, tr);
    }

}
