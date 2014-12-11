

package org.sufficientlysecure.rootcommands.command;

import java.io.File;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;

public abstract class ExecutableCommand extends Command {
    public static final String EXECUTABLE_PREFIX = "lib";
    public static final String EXECUTABLE_SUFFIX = "_exec.so";

    /**
     * This class provides a way to use your own binaries!
     * 
     * Include your own executables, renamed from * to lib*_exec.so, in your libs folder under the
     * architecture directories. Now they will be deployed by Android the same way libraries are
     * deployed!
     * 
     * See README for more information how to use your own executables!
     *
     * @param context
     * @param executableName
     * @param parameters
     */
    public ExecutableCommand(Context context, String executableName, String parameters) {
        super(getLibDirectory(context) + File.separator + EXECUTABLE_PREFIX + executableName
                + EXECUTABLE_SUFFIX + " " + parameters);
    }

    /**
     * Get full path to lib directory of app
     * 
     * @return dir as String
     */
    @SuppressLint("NewApi")
    private static String getLibDirectory(Context context) {
        if (Build.VERSION.SDK_INT >= 9) {
            return context.getApplicationInfo().nativeLibraryDir;
        } else {
            return context.getApplicationInfo().dataDir + File.separator + "lib";
        }
    }

    public abstract void output(int id, String line);

    public abstract void afterExecution(int id, int exitCode);

}