

package org.sufficientlysecure.rootcommands;

import org.sufficientlysecure.rootcommands.util.Log;

public class RootCommands {
    public static boolean DEBUG = false;
    public static int DEFAULT_TIMEOUT = 10000;

    public static final String TAG = "RootCommands";

    /**
     * General method to check if user has su binary and accepts root access for this program!
     * 
     * @return true if everything worked
     */
    public static boolean rootAccessGiven() {
        boolean rootAccess = false;

        try {
            Shell rootShell = Shell.startRootShell();

            Toolbox tb = new Toolbox(rootShell);
            if (tb.isRootAccessGiven()) {
                rootAccess = true;
            }

            rootShell.close();
        } catch (Exception e) {
            Log.e(TAG, "Problem while checking for root access!", e);
        }

        return rootAccess;
    }
}
