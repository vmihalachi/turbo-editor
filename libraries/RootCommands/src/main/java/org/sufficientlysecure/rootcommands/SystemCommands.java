

package org.sufficientlysecure.rootcommands;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.location.LocationManager;
import android.os.PowerManager;
import android.provider.Settings;

/**
 * This methods work when the apk is installed as a system app (under /system/app)
 */
public class SystemCommands {
    Context context;

    public SystemCommands(Context context) {
        super();
        this.context = context;
    }

    /**
     * Get GPS status
     * 
     * @return
     */
    public boolean getGPS() {
        return ((LocationManager) context.getSystemService(Context.LOCATION_SERVICE))
                .isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    /**
     * Enable/Disable GPS
     * 
     * @param value
     */
    @TargetApi(8)
    public void setGPS(boolean value) {
        ContentResolver localContentResolver = context.getContentResolver();
        Settings.Secure.setLocationProviderEnabled(localContentResolver,
                LocationManager.GPS_PROVIDER, value);
    }

    /**
     * TODO: Not ready yet
     */
    @TargetApi(8)
    public void reboot() {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        pm.reboot("recovery");
        pm.reboot(null);

        // not working:
        // reboot(null);
    }

    /**
     * Reboot the device immediately, passing 'reason' (may be null) to the underlying __reboot
     * system call. Should not return.
     * 
     * Taken from com.android.server.PowerManagerService.reboot
     */
    // public void reboot(String reason) {
    //
    // // final String finalReason = reason;
    // Runnable runnable = new Runnable() {
    // public void run() {
    // synchronized (this) {
    // // ShutdownThread.reboot(mContext, finalReason, false);
    // try {
    // Class<?> clazz = Class.forName("com.android.internal.app.ShutdownThread");
    //
    // // if (mReboot) {
    // Method method = clazz.getMethod("reboot", Context.class, String.class,
    // Boolean.TYPE);
    // method.invoke(null, context, null, false);
    //
    // // if (mReboot) {
    // // Method method = clazz.getMethod("reboot", Context.class, String.class,
    // // Boolean.TYPE);
    // // method.invoke(null, mContext, mReason, mConfirm);
    // // } else {
    // // Method method = clazz.getMethod("shutdown", Context.class, Boolean.TYPE);
    // // method.invoke(null, mContext, mConfirm);
    // // }
    // } catch (Exception e) {
    // e.printStackTrace();
    // }
    // }
    //
    // }
    // };
    // // ShutdownThread must run on a looper capable of displaying the UI.
    // mHandler.post(runnable);
    //
    // // PowerManager.reboot() is documented not to return so just wait for the inevitable.
    // // synchronized (runnable) {
    // // while (true) {
    // // try {
    // // runnable.wait();
    // // } catch (InterruptedException e) {
    // // }
    // // }
    // // }
    // }

}
