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

package com.spazedog.lib.rootfw4;

import java.util.HashSet;
import java.util.Set;

import com.spazedog.lib.rootfw4.Shell.Attempts;
import com.spazedog.lib.rootfw4.Shell.OnShellConnectionListener;
import com.spazedog.lib.rootfw4.Shell.OnShellResultListener;
import com.spazedog.lib.rootfw4.Shell.OnShellValidateListener;
import com.spazedog.lib.rootfw4.Shell.Result;
import com.spazedog.lib.rootfw4.utils.Device;
import com.spazedog.lib.rootfw4.utils.Device.Process;
import com.spazedog.lib.rootfw4.utils.File;
import com.spazedog.lib.rootfw4.utils.Filesystem;
import com.spazedog.lib.rootfw4.utils.Memory;
import com.spazedog.lib.rootfw4.utils.Filesystem.Disk;
import com.spazedog.lib.rootfw4.utils.Memory.CompCache;
import com.spazedog.lib.rootfw4.utils.Memory.Swap;
import com.spazedog.lib.rootfw4.utils.io.FileReader;
import com.spazedog.lib.rootfw4.utils.io.FileWriter;

/**
 * This is a global static front-end to {@link com.spazedog.lib.rootfw4.Shell}. It allows one global shell connection to be
 * easily shared across classes and threads without having to create multiple connections. 
 */
public class RootFW {
	
	protected static volatile Shell mShell;
	protected static volatile Integer mLockCount = 0;
	protected static final Object mLock = new Object();
	
	protected static Set<OnConnectionListener> mListeners = new HashSet<OnConnectionListener>();
	
	/**
	 * An interface that can be used to monitor the current state of the global connection.
	 * 
	 * @see #addConnectionListener(com.spazedog.lib.rootfw4.RootFW.OnConnectionListener)
	 */
	public static interface OnConnectionListener extends OnShellConnectionListener {
		/**
		 * Invoked when the shell has been connected
		 */
		public void onShellConnect();
	}
	
	/**
	 * Create a new connection to the global shell.<br /><br />
	 * 
	 * Note that this method will fallback on a normal user shell if root could not be obtained. 
	 * Therefore it is always a good idea to check the state of root using {@link #isRoot()}
	 * 
	 * @return 
	 *     True if the shell was connected successfully
	 */
	public static Boolean connect() {
		synchronized(mLock) {
			if (mShell == null || !mShell.isConnected()) {
				mLockCount = 0;
				mShell = new Shell(true);
				
				/*
				 * Fallback to a regular user shell
				 */
				if (!mShell.isConnected()) {
					mShell = new Shell(false);
				}
				
				mShell.addShellConnectionListener(new OnShellConnectionListener(){
					@Override
					public void onShellDisconnect() {
						for (OnConnectionListener listener : mListeners) {
							listener.onShellDisconnect();
						}
					}
				});
				
				for (OnConnectionListener listener : mListeners) {
					listener.onShellConnect();
				}
			}
			
			return mShell.isConnected();
		}
	}
	
	/**
	 * @see #disconnect(Boolean)
	 */
	public static void disconnect() {
		disconnect(false);
	}
	
	/**
	 * Destroy the connection to the global shell.<br /><br />
	 * 
	 * The connection will only be destroyed if there is no current locks on this connecton. 
	 * 
	 * @see #lock()
	 */
	public static void disconnect(Boolean force) {
		synchronized(mLock) {
			if (mLockCount == 0 || force) {
				mLockCount = 0;
				mShell.destroy();
				mShell = null;
			}
		}
	}
	
	/**
	 * Add a new lock on this connection. Each call to this method will add an additional lock. 
	 * As long as there are 1 or more locks on this connection, it cannot be destroyed using {@link #disconnect()}
	 * 
	 * @see #unlock()
	 */
	public static void lock() {
		synchronized(mLock) {
			mLockCount += 1;
		}
	}
	
	/**
	 * Removes one lock from this connection. Each call will remove 1 lock as long as there are 1 or more locks attached. 
	 * 
	 * @see #lock()
	 */
	public static void unlock() {
		synchronized(mLock) {
			if (mLockCount > 0) {
				mLockCount -= 1;
				
			} else {
				mLockCount = 0;
			}
		}
	}
	
	/**
	 * Checks if there are any active locks on the connection.
	 */
	public static Boolean isLocked() {
		synchronized(mLock) {
			return mLockCount == 0;
		}
	}
	
	/**
	 * Add a new {@link com.spazedog.lib.rootfw4.RootFW.OnConnectionListener} to the global shell
	 * 
	 * @see #removeConnectionListener(com.spazedog.lib.rootfw4.RootFW.OnConnectionListener)
	 */
	public static void addConnectionListener(OnConnectionListener listener) {
		synchronized(mLock) {
			mListeners.add(listener);
		}
	}
	
	/**
	 * Remove a {@link com.spazedog.lib.rootfw4.RootFW.OnConnectionListener} from the global shell
	 * 
	 * @see #addConnectionListener(com.spazedog.lib.rootfw4.RootFW.OnConnectionListener)
	 */
	public static void removeConnectionListener(OnConnectionListener listener) {
		synchronized(mLock) {
			mListeners.remove(listener);
		}
	}
	
	/**
	 * @see com.spazedog.lib.rootfw4.Shell#execute(String)
	 */
	public static Result execute(String command) {
		return mShell.execute(command);
	}
	
	/**
	 * @see com.spazedog.lib.rootfw4.Shell#execute(String[])
	 */
	public static Result execute(String[] commands) {
		return mShell.execute(commands);
	}
	
	/**
	 * @see com.spazedog.lib.rootfw4.Shell#execute(String[], Integer[], com.spazedog.lib.rootfw4.Shell.OnShellValidateListener)
	 */
	public static Result execute(String[] commands, Integer[] resultCodes, OnShellValidateListener validater) {
		return mShell.execute(commands, resultCodes, validater);
	}
	
	/**
	 * @see com.spazedog.lib.rootfw4.Shell#executeAsync(String, com.spazedog.lib.rootfw4.Shell.OnShellResultListener)
	 */
	public static void executeAsync(String command, OnShellResultListener listener) {
		mShell.executeAsync(command, listener);
	}
	
	/**
	 * @see com.spazedog.lib.rootfw4.Shell#executeAsync(String[], com.spazedog.lib.rootfw4.Shell.OnShellResultListener)
	 */
	public static void executeAsync(String[] commands, OnShellResultListener listener) {
		mShell.executeAsync(commands, listener);
	}
	
	/**
	 * @see com.spazedog.lib.rootfw4.Shell#executeAsync(String[], Integer[], com.spazedog.lib.rootfw4.Shell.OnShellValidateListener, com.spazedog.lib.rootfw4.Shell.OnShellResultListener)
	 */
	public static void executeAsync(String[] commands, Integer[] resultCodes, OnShellValidateListener validater, OnShellResultListener listener) {
		mShell.executeAsync(commands, resultCodes, validater, listener);
	}
	
	/**
	 * @see com.spazedog.lib.rootfw4.Shell#isRoot()
	 */
	public static Boolean isRoot() {
		return mShell.isRoot();
	}
	
	/**
	 * @see com.spazedog.lib.rootfw4.Shell#isConnected()
	 */
	public static Boolean isConnected() {
		return mShell != null && mShell.isConnected();
	}
	
	/**
	 * @see com.spazedog.lib.rootfw4.Shell#getTimeout()
	 */
	public static Integer getTimeout() {
		return mShell.getTimeout();
	}
	
	/**
	 * @see com.spazedog.lib.rootfw4.Shell#setTimeout(Integer)
	 */
	public static void setTimeout(Integer timeout) {
		mShell.setTimeout(timeout);
	}
	
	/**
	 * @see com.spazedog.lib.rootfw4.Shell#getBinary(String)
	 */
	public static String findCommand(String bin) {
		return mShell.findCommand(bin);
	}
	
	/**
	 * @see com.spazedog.lib.rootfw4.Shell#createAttempts(String)
	 */
	public static Attempts createAttempts(String command) {
		return mShell.createAttempts(command);
	}
	
	/**
	 * @see com.spazedog.lib.rootfw4.Shell#getFileReader(String)
	 */
	public static FileReader getFileReader(String file) {
		return mShell.getFileReader(file);
	}
	
	/**
	 * @see com.spazedog.lib.rootfw4.Shell#getFileWriter(String, Boolean)
	 */
	public static FileWriter getFileWriter(String file, Boolean append) {
		return mShell.getFileWriter(file, append);
	}
	
	/**
	 * @see com.spazedog.lib.rootfw4.Shell#getFile(String)
	 */
	public static File getFile(String file) {
		return mShell.getFile(file);
	}
	
	/**
	 * @see com.spazedog.lib.rootfw4.Shell#getFilesystem()
	 */
	public static Filesystem getFilesystem() {
		return mShell.getFilesystem();
	}
	
	/**
	 * @see com.spazedog.lib.rootfw4.Shell#getDisk(String)
	 */
	public static Disk getDisk(String disk) {
		return mShell.getDisk(disk);
	}
	
	/**
	 * @see com.spazedog.lib.rootfw4.Shell#getDevice()
	 */
	public static Device getDevice() {
		return mShell.getDevice();
	}
	
	/**
	 * @see com.spazedog.lib.rootfw4.Shell#getProcess(String)
	 */
	public static Process getProcess(String process) {
		return mShell.getProcess(process);
	}
	
	/**
	 * @see com.spazedog.lib.rootfw4.Shell#getProcess(Integer)
	 */
	public static Process getProcess(Integer pid) {
		return mShell.getProcess(pid);
	}
	
	/**
	 * @see com.spazedog.lib.rootfw4.Shell#getMemory()
	 */
	public static Memory getMemory() {
		return mShell.getMemory();
	}
	
	/**
	 * @see com.spazedog.lib.rootfw4.Shell#getCompCache()
	 */
	public static CompCache getCompCache() {
		return mShell.getCompCache();
	}
	
	/**
	 * @see com.spazedog.lib.rootfw4.Shell#getSwap(String device)
	 */
	public static Swap getSwap(String device) {
		return mShell.getSwap(device);
	}
}
