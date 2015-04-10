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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import android.os.Bundle;
import android.util.Log;

import com.spazedog.lib.rootfw4.ShellStream.OnStreamListener;
import com.spazedog.lib.rootfw4.containers.Data;
import com.spazedog.lib.rootfw4.utils.Device;
import com.spazedog.lib.rootfw4.utils.Device.Process;
import com.spazedog.lib.rootfw4.utils.File;
import com.spazedog.lib.rootfw4.utils.Filesystem;
import com.spazedog.lib.rootfw4.utils.Filesystem.Disk;
import com.spazedog.lib.rootfw4.utils.Memory;
import com.spazedog.lib.rootfw4.utils.Memory.CompCache;
import com.spazedog.lib.rootfw4.utils.Memory.Swap;
import com.spazedog.lib.rootfw4.utils.io.FileReader;
import com.spazedog.lib.rootfw4.utils.io.FileWriter;

/**
 * This class is a front-end to {@link ShellStream} which makes it easier to work
 * with normal shell executions. If you need to execute a consistent command (one that never ends), 
 * you should work with {@link ShellStream} directly. 
 */
public class Shell {
	public static final String TAG = Common.TAG + ".Shell";
	
	protected static Set<Shell> mInstances = Collections.newSetFromMap(new WeakHashMap<Shell, Boolean>());
	protected static Map<String, String> mBinaries = new HashMap<String, String>();
	
	protected Set<OnShellBroadcastListener> mBroadcastRecievers = Collections.newSetFromMap(new WeakHashMap<OnShellBroadcastListener, Boolean>());
	protected Set<OnShellConnectionListener> mConnectionRecievers = new HashSet<OnShellConnectionListener>();
	
	protected Object mLock = new Object();
	protected ShellStream mStream;
	protected Boolean mIsConnected = false;
	protected Boolean mIsRoot = false;
	protected List<String> mOutput = null;
	protected Integer mResultCode = 0;
	protected Integer mShellTimeout = 15000;
	protected Set<Integer> mResultCodes = new HashSet<Integer>();
	
	/**
	 * This interface is used internally across utility classes.
	 */
	public static interface OnShellBroadcastListener {
		public void onShellBroadcast(String key, Bundle data);
	}
	
	/**
	 * This interface is for use with {@link Shell#executeAsync(String[], OnShellResultListener)}.
	 */
	public static interface OnShellResultListener {
		/**
		 * Called when an asynchronous execution has finished.
		 * 
		 * @param result
		 *     The result from the asynchronous execution
		 */
		public void onShellResult(Result result);
	}
	
	/**
	 * This interface is for use with the execute methods. It can be used to validate an attempt command, if that command 
	 * cannot be validated by result code alone. 
	 */
	public static interface OnShellValidateListener {
		/**
		 * Called at the end of each attempt in order to validate it. If this method returns false, then the next attempt will be executed. 
		 * 
		 * @param command
		 *     The command that was executed during this attempt
		 *     
		 * @param result
		 *     The result code from this attempt
		 *     
		 * @param output
		 *     The output from this attempt
		 *     
		 * @param resultCodes
		 *     All of the result codes that has been added as successful ones
		 *     
		 * @return 
		 *     False to continue to the next attempts, or True to stop the current execution
		 */
		public Boolean onShellValidate(String command, Integer result, List<String> output, Set<Integer> resultCodes);
	}
	
	/**
	 * This interface is used to get information about connection changes.
	 */
	public static interface OnShellConnectionListener {
		/**
		 * Called when the connection to the shell is lost.
		 */
		public void onShellDisconnect();
	}
	
	/**
	 * This class is used to store the result from shell executions. 
	 * It extends the {@link Data} class.
	 */
	public static class Result extends Data<Result> {
		private Integer mResultCode;
		private Integer[] mValidResults;
		private Integer mCommandNumber;
		
		public Result(String[] lines, Integer result, Integer[] validResults, Integer commandNumber) {
			super(lines);
			
			mResultCode = result;
			mValidResults = validResults;
			mCommandNumber = commandNumber;
		}

		/**
		 * Get the result code from the shell execution.
		 */
		public Integer getResultCode() {
			return mResultCode;
		}

		/**
		 * Compare the result code with {@link Shell#addResultCode(Integer)} to determine 
		 * whether or not the execution was a success. 
		 */
		public Boolean wasSuccessful() {
			for (int i=0; i < mValidResults.length; i++) {
				if ((int) mValidResults[i] == (int) mResultCode) {
					return true;
				}
			}
			
			return false;
		}
		
		/**
		 * Get the command number that produced a successful result. 
		 */
		public Integer getCommandNumber() {
			return mCommandNumber;
		}
	}
	
	/**
	 * A class containing automatically created shell attempts and links to both {@link Shell#executeAsync(String[], Integer[], OnShellResultListener)} and {@link Shell#execute(String[], Integer[])} <br /><br />
	 * 
	 * All attempts are created based on {@link Common#BINARIES}. <br /><br />
	 * 
	 * Example: String("ls") would become String["ls", "busybox ls", "toolbox ls"] if {@link Common#BINARIES} equals String[null, "busybox", "toolbox"].<br /><br />
	 * 
	 * You can also apply the keyword %binary if you need to apply the binaries to more than the beginning of a command. <br /><br />
	 * 
	 * Example: String("(%binary test -d '%binary pwd') || exit 1") would become String["(test -d 'pwd') || exit 1", "(busybox test -d 'busybox pwd') || exit 1", "(toolbox test -d 'toolbox pwd') || exit 1"]
	 * 
	 * @see Shell#createAttempts(String)
	 */
	public class Attempts {
		protected String[] mAttempts;
		protected Integer[] mResultCodes;
		protected OnShellValidateListener mValidateListener;
		protected OnShellResultListener mResultListener;
		
		protected Attempts(String command) {
			if (command != null) {
				Integer pos = 0;
				mAttempts = new String[ Common.BINARIES.length ];
				
				for (String binary : Common.BINARIES) {
					if (command.contains("%binary ")) {
						mAttempts[pos] = command.replaceAll("%binary ", (binary != null && binary.length() > 0 ? binary + " " : ""));
						
					} else {
						mAttempts[pos] = (binary != null && binary.length() > 0 ? binary + " " : "") + command;
					}
					
					pos += 1;
				}
			}
		}
		
		public Attempts setValidateListener(OnShellValidateListener listener) {
			mValidateListener = listener; return this;
		}
		
		public Attempts setResultListener(OnShellResultListener listener) {
			mResultListener = listener; return this;
		}
		
		public Attempts setResultCodes(Integer... resultCodes) {
			mResultCodes = resultCodes; return this;
		}
		
		public Result execute(OnShellValidateListener listener) {
			return setValidateListener(listener).execute();
		}
		
		public Result execute() {
			return Shell.this.execute(mAttempts, mResultCodes, mValidateListener);
		}
		
		public void executeAsync(OnShellResultListener listener) {
			setResultListener(listener).executeAsync();
		}
		
		public void executeAsync() {
			Shell.this.executeAsync(mAttempts, mResultCodes, mValidateListener, mResultListener);
		}
	}
	
	/**
	 * Establish a {@link ShellStream} connection.
	 * 
	 * @param requestRoot
	 *     Whether or not to request root privileges for the shell connection
	 */
	public Shell(Boolean requestRoot) {
		mResultCodes.add(0);
		mIsRoot = requestRoot;
		
		/*
		 * Kutch superuser daemon mode sometimes has problems connecting the first time.
		 * So we will give it two tries before giving up.
		 */
		for (int i=0; i < 2; i++) {
			if(Common.DEBUG)Log.d(TAG, "Construct: Running connection attempt number " + (i+1));
			
			mStream = new ShellStream(requestRoot, new OnStreamListener() {
				@Override
				public void onStreamStart() {
					if(Common.DEBUG)Log.d(TAG, "onStreamStart: ...");
					
					mOutput = new ArrayList<String>();
				}

				@Override
				public void onStreamInput(String outputLine) {
					if(Common.DEBUG)Log.d(TAG, "onStreamInput: " + (outputLine != null ? (outputLine.length() > 50 ? outputLine.substring(0, 50) + " ..." : outputLine) : "NULL"));
					
					mOutput.add(outputLine);
				}

				@Override
				public void onStreamStop(Integer resultCode) {
					if(Common.DEBUG)Log.d(TAG, "onStreamStop: " + resultCode);
					
					mResultCode = resultCode;
				}

				@Override
				public void onStreamDied() {
					if(Common.DEBUG)Log.d(TAG, "onStreamDied: The stream has been closed");
					
					if (mIsConnected) {
						if(Common.DEBUG)Log.d(TAG, "onStreamDied: The stream seams to have died, reconnecting");
						
						mStream = new ShellStream(mIsRoot, this);
						
						if (mStream.isActive()) {
							Result result = execute("echo connected");
							
							mIsConnected = result != null && "connected".equals(result.getLine());
							
						} else {
							if(Common.DEBUG)Log.d(TAG, "onStreamDied: Could not reconnect");
							
							mIsConnected = false;
						}
					}
					
					if (!mIsConnected) {
						for (OnShellConnectionListener reciever : mConnectionRecievers) {
							reciever.onShellDisconnect();
						}
					}
				}
			});
			
			if (mStream.isActive()) {
				Result result = execute("echo connected");
				
				mIsConnected = result != null && "connected".equals(result.getLine());
				
				if (mIsConnected) {
					if(Common.DEBUG)Log.d(TAG, "Construct: Connection has been established");
					
					mInstances.add(this); break;
				}
			}
		}
	}
	
	/**
	 * Execute a shell command.
	 * 
	 * @see Shell#execute(String[], Integer[])
	 * 
	 * @param command
	 *     The command to execute
	 */
	public Result execute(String command) {
		return execute(new String[]{command}, null, null);
	}
	
	/**
	 * Execute a range of commands until one is successful.
	 * 
	 * @see Shell#execute(String[], Integer[])
	 * 
	 * @param commands
	 *     The commands to try
	 */
	public Result execute(String[] commands) {
		return execute(commands, null, null);
	}
	
	/**
	 * Execute a range of commands until one is successful.<br /><br />
	 * 
	 * Android shells differs a lot from one another, which makes it difficult to program shell scripts for. 
	 * This method can help with that by trying different commands until one works. <br /><br />
	 * 
	 * <code>Shell.execute( new String(){"cat file", "toolbox cat file", "busybox cat file"} );</code><br /><br />
	 * 
	 * Whether or not a command was successful, depends on {@link Shell#addResultCode(Integer)} which by default only contains '0'. 
	 * The command number that was successful can be checked using {@link Result#getCommandNumber()}.
	 * 
	 * @param commands
	 *     The commands to try
	 *     
	 * @param resultCodes
	 *     Result Codes representing successful execution. These will be temp. merged with {@link Shell#addResultCode(Integer)}. 
	 *     
	 * @param validater
	 *     A {@link OnShellValidateListener} instance or NULL
	 */
	public Result execute(String[] commands, Integer[] resultCodes, OnShellValidateListener validater) {
		synchronized(mLock) {
			if (mStream.waitFor(mShellTimeout)) {
				Integer cmdCount = 0;
				Set<Integer> codes = new HashSet<Integer>(mResultCodes);
				
				if (resultCodes != null) {
					Collections.addAll(codes, resultCodes);
				}
				
				for (String command : commands) {
					if(Common.DEBUG)Log.d(TAG, "execute: Executing the command '" + command + "'");
					
					mStream.execute(command);
					
					if(!mStream.waitFor(mShellTimeout)) {
						/*
						 * Something is wrong, reconnect to the shell.
						 */
						mStream.destroy();
						
						return null;
					}
					
					if(Common.DEBUG)Log.d(TAG, "execute: The command finished with the result code '" + mResultCode + "'");
					
					if ((validater != null && validater.onShellValidate(command, mResultCode, mOutput, codes)) || codes.contains(mResultCode)) {
						/*
						 * If a validater excepts this, then add the result code to the list of successful codes 
						 */
						codes.add(mResultCode); break;
					}
					
					cmdCount += 1;
				}
				
				if (mOutput != null) {
					return new Result(mOutput.toArray(new String[mOutput.size()]), mResultCode, codes.toArray(new Integer[codes.size()]), cmdCount);
				}
			}
			
			return null;
		}
	}
	
	/**
	 * Execute a shell command asynchronous.
	 * 
	 * @see Shell#executeAsync(String[], Integer[], OnShellResultListener)
	 * 
	 * @param command
	 *     The command to execute
	 * 
	 * @param listener
	 *     A {@link OnShellResultListener} callback instance
	 */
	public void executeAsync(String command, OnShellResultListener listener) {
		executeAsync(new String[]{command}, null, null, listener);
	}
	
	/**
	 * Execute a range of commands asynchronous until one is successful.
	 * 
	 * @see Shell#executeAsync(String[], Integer[], OnShellResultListener)
	 * 
	 * @param commands
	 *     The commands to try
	 * 
	 * @param listener
	 *     A {@link OnShellResultListener} callback instance
	 */
	public void executeAsync(String[] commands, OnShellResultListener listener) {
		executeAsync(commands, null, null, listener);
	}
	
	/**
	 * Execute a range of commands asynchronous until one is successful.
	 * 
	 * @see Shell#execute(String[], Integer[])
	 * 
	 * @param commands
	 *     The commands to try
	 *     
	 * @param resultCodes
	 *     Result Codes representing successful execution. These will be temp. merged with {@link Shell#addResultCode(Integer)}.
	 *     
	 * @param validater
	 *     A {@link OnShellValidateListener} instance or NULL
	 * 
	 * @param listener
	 *     A {@link OnShellResultListener} callback instance
	 */
	public synchronized void executeAsync(final String[] commands, final Integer[] resultCodes, final OnShellValidateListener validater, final OnShellResultListener listener) {
		if(Common.DEBUG)Log.d(TAG, "executeAsync: Starting an async shell execution");
		
		/*
		 * If someone execute more than one async task after another, and use the same listener, 
		 * we could end up getting the result in the wrong order. We need to make sure that each Thread is started in the correct order. 
		 */
		final Object lock = new Object();
		
		new Thread() {
			@Override
			public void run() {
				Result result = null;
				
				synchronized (lock) {
					lock.notifyAll();
				}
				
				synchronized(mLock) {
					result = Shell.this.execute(commands, resultCodes, validater);
				}
				
				listener.onShellResult(result);
			}
			
		}.start();
		
		/*
		 * Do not exit this method, until the Thread is started. 
		 */
		synchronized (lock) {
			try {
				lock.wait();
				
			} catch (InterruptedException e) {}
		}
	}
	
	/**
	 * For internal usage
	 */
	public static void sendBroadcast(String key, Bundle data) {
		for (Shell instance : mInstances) {
			instance.broadcastReciever(key, data);
		}
	}
	
	/**
	 * For internal usage
	 */
	protected void broadcastReciever(String key, Bundle data) {
		for (OnShellBroadcastListener recievers : mBroadcastRecievers) {
			recievers.onShellBroadcast(key, data);
		}
	}
	
	/**
	 * For internal usage
	 */
	public void addBroadcastListener(OnShellBroadcastListener listener) {
		mBroadcastRecievers.add(listener);
	}
	
	/**
	 * Add a shell connection listener. This callback will be invoked whenever the connection to
	 * the shell changes. 
	 * 
	 * @param listener
	 *     A {@link OnShellConnectionListener} callback instance
	 */
	public void addShellConnectionListener(OnShellConnectionListener listener) {
		mConnectionRecievers.add(listener);
	}
	
	/**
	 * Remove a shell connection listener from the stack. 
	 * 
	 * @param listener
	 *     A {@link OnShellConnectionListener} callback instance
	 */
	public void removeShellConnectionListener(OnShellConnectionListener listener) {
		mConnectionRecievers.remove(listener);
	}
	
	/**
	 * Check whether or not root was requested for this shell.
	 */
	public Boolean isRoot() {
		return mIsRoot;
	}
	
	/**
	 * Check whether or not a shell connection was established. 
	 */
	public Boolean isConnected() {
		return mIsConnected;
	}
	
	/**
	 * Get the current shell execution timeout. 
	 * This is the time in milliseconds from which an execution is killed in case it has stalled. 
	 */
	public Integer getTimeout() {
		return mShellTimeout;
	}
	
	/**
	 * Change the shell execution timeout. This should be in milliseconds. 
	 * If this is set to '0', there will be no timeout. 
	 */
	public void setTimeout(Integer timeout) {
		if (timeout >= 0) {
			mShellTimeout = timeout;
		}
	}
	/**
	 * Add another result code that represent a successful execution. By default only '0' is used, since 
	 * most shell commands uses '0' for success and '1' for error. But some commands uses different values, like 'cat' 
	 * that uses '130' as success when piping content. 
	 * 
	 * @param resultCode
	 *     The result code to add to the stack
	 */
	public void addResultCode(Integer resultCode) {
		mResultCodes.add(resultCode);
	}
	
	/**
	 * Remove a result code from the stack.
	 * 
	 * @see Shell#addResultCode(Integer)
	 * 
	 * @param resultCode
	 *     The result code to remove from the stack
	 */
	public void removeResultCode(Integer resultCode) {
		mResultCodes.remove(resultCode);
	}
	
	/**
	 * Reset the stack containing result codes and set it back to default only containing '0'.
	 * 
	 * @see Shell#addResultCode(Integer)
	 */
	public void resetResultCodes() {
		mResultCodes.clear();
		mResultCodes.add(0);
	}
	
	/**
	 * Close the shell connection using 'exit 0' if possible, or by force and release all data stored in this instance. 
	 */
	public void destroy() {
		if (mStream != null) {
			mIsConnected = false;
			
			if (mStream.isRunning() || !mStream.isActive()) {
				if(Common.DEBUG)Log.d(TAG, "destroy: Destroying the stream");
				
				mStream.destroy();
				
			} else {
				if(Common.DEBUG)Log.d(TAG, "destroy: Making a clean exit on the stream");
				
				execute("exit 0");
			}
			
			mStream = null;
			mInstances.remove(this);
			mBroadcastRecievers.clear();
		}
	}
	
	/**
	 * Locate whichever toolbox in {@value Common#BINARIES} that supports a specific command.<br /><br />
	 * 
	 * Example: String("cat") might return String("busybox cat") or String("toolbox cat")
	 * 
	 * @param bin
	 *     The command to check
	 */
	public String findCommand(String bin) {
		if (!mBinaries.containsKey(bin)) {
			for (String toolbox : Common.BINARIES) {
				String cmd = toolbox != null && toolbox.length() > 0 ? toolbox + " " + bin : bin;
				Result result = execute( cmd + " -h" );
					
				if (result != null) {
					String line = result.getLine();
					
					if (!line.endsWith("not found") && !line.endsWith("such tool")) {
						mBinaries.put(bin, cmd); break;
					}
				}
			}
		}
		
		return mBinaries.get(bin);
	}
	
	/**
	 * Create a new instance of {@link Attempts}
	 * 
	 * @param command
	 *     The command to convert into multiple attempts
	 */
	public Attempts createAttempts(String command) {
		if (command != null) {
			return new Attempts(command);
		}
		
		return null;
	}
	
	/**
	 * Open a new RootFW {@link FileReader}. This is the same as {@link FileReader#FileReader(Shell, String)}.
	 * 
	 * @param file
	 *     Path to the file
	 *     
	 * @return
	 *     NULL if the file could not be opened
	 */
	public FileReader getFileReader(String file) {
		try {
			return new FileReader(this, file);
			
		} catch (FileNotFoundException e) {
			return null;
		}
	}
	
	/**
	 * Open a new RootFW {@link FileWriter}. This is the same as {@link FileWriter#FileWriter(Shell, String, boolean)}.
	 * 
	 * @param file
	 *     Path to the file
	 *     
	 * @param append
	 *     Whether or not to append new content to existing content
	 *     
	 * @return
	 *     NULL if the file could not be opened
	 */
	public FileWriter getFileWriter(String file, Boolean append) {
		try {
			return new FileWriter(this, file, append);
			
		} catch (IOException e) {
			return null;
		}
	}
	
	/**
	 * Get a new {@link File} instance.
	 * 
	 * @param file
	 *     Path to the file or directory
	 */
	public File getFile(String file) {
		return new File(this, file);
	}
	
	/**
	 * Get a new {@link Filesystem} instance.
	 */
	public Filesystem getFilesystem() {
		return new Filesystem(this);
	}
	
	/**
	 * Get a new {@link Disk} instance.
	 * 
	 * @param disk
	 *     Path to a disk, partition or a mount point
	 */
	public Disk getDisk(String disk) {
		return new Disk(this, disk);
	}
	
	
	/**
	 * Get a new {@link Device} instance.
	 */
	public Device getDevice() {
		return new Device(this);
	}
	
	/**
	 * Get a new {@link Process} instance.
	 * 
	 * @param process
	 *     The name of the process
	 */
	public Process getProcess(String process) {
		return new Process(this, process);
	}
	
	/**
	 * Get a new {@link Process} instance.
	 * 
	 * @param pid
	 *     The process id
	 */
	public Process getProcess(Integer pid) {
		return new Process(this, pid);
	}
	
	/**
	 * Get a new {@link Memory} instance.
	 */
	public Memory getMemory() {
		return new Memory(this);
	}
	
	/**
	 * Get a new {@link CompCache} instance.
	 */
	public CompCache getCompCache() {
		return new CompCache(this);
	}
	
	/**
	 * Get a new {@link Swap} instance.
	 * 
	 * @param device
	 *     The /dev/ swap device
	 */
	public Swap getSwap(String device) {
		return new Swap(this, device);
	}
}
