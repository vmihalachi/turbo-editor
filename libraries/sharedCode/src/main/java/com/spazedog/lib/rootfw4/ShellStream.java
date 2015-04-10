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

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import android.util.Log;

/**
 * This class opens a connection to the shell and creates a consistent output stream
 * that can be read using the {@link OnStreamListener} interface. It also
 * contains an input stream that can be used to execute shell commands. 
 */
public class ShellStream {
	public static final String TAG = Common.TAG + ".ShellStream";
	
	protected Process mConnection;
	
	protected DataOutputStream mStdInput;
	protected BufferedReader mStdOutput;
	
	protected Thread mStdOutputWorker;
	
	protected OnStreamListener mListener;
	
	protected final Counter mCounter = new Counter();
	protected final Object mLock = new Object();
	protected Boolean mIsActive = false;
	protected Boolean mIsRoot = false;
	
	protected String mCommandEnd = "EOL:a00c38d8:EOL";
	
	protected static class Counter {
		private volatile Integer mCount = 0;
		private volatile Object mLock = new Object();
		
		public Integer size() {
			synchronized(mLock) {
				return mCount;
			}
		}
		
		public Integer encrease() {
			synchronized(mLock) {
				return (mCount += 1);
			}
		}
		
		public Integer decrease() {
			synchronized(mLock) {
				return mCount > 0 ? (mCount -= 1) : (mCount = 0);
			}
		}
		
		public void reset() {
			synchronized(mLock) {
				mCount = 0;
			}
		}
	}
	
	/**
	 * This interface is used to read the input from the shell.
	 */
	public static interface OnStreamListener {
		/**
		 * This is called before a command is sent to the shell output stream.
		 */
		public void onStreamStart();
		
		/**
		 * This is called on each line from the shell input stream.
		 * 
		 * @param inputLine
		 *     The current shell input line that is being processed
		 */
		public void onStreamInput(String outputLine);
		
		/**
		 * This is called after all shell input has been processed. 
		 * 
		 * @param exitCode
		 *     The exit code returned by the shell
		 */
		public void onStreamStop(Integer resultCode);
		
		/**
		 * This is called when the shell connection dies. 
		 * This can either be because a command executed 'exit', or if the method {@link ShellStream#destroy()} was called. 
		 */
		public void onStreamDied();
	}
	
	/**
	 * Connect to a shell and create a consistent I/O stream.
	 */
	public ShellStream(Boolean requestRoot, OnStreamListener listener) {
		try {
			if(Common.DEBUG)Log.d(TAG, "Construct: Establishing a new shell stream");
			
			ProcessBuilder builder = new ProcessBuilder(requestRoot ? "su" : "sh");
			builder.redirectErrorStream(true);
			
			mIsRoot = requestRoot;
			mIsActive = true;
			mListener = listener;
			mConnection = builder.start();
			mStdInput = new DataOutputStream(mConnection.getOutputStream());
			mStdOutput = new BufferedReader(new InputStreamReader(mConnection.getInputStream()));
			
			mStdOutputWorker = new Thread() {
				@Override
				public void run() {
					String output = null;
					
					try {
						while (mIsActive && (output = mStdOutput.readLine()) != null) {
							if (mListener != null && mCounter.size() > 0) {
								if (output.contains(mCommandEnd)) {
									Integer result = 0;
									
									try {
										if (output.startsWith(mCommandEnd)) {
											result = Integer.parseInt(output.substring(mCommandEnd.length()+1));
											
										} else {
											result = 1;
										}
										
									} catch (Throwable e) {
										Log.w(TAG, e.getMessage(), e);
									}
									
									mListener.onStreamStop(result);
									mCounter.decrease();
									
									synchronized(mLock) {
										mLock.notifyAll();
									}
									
								} else {
									mListener.onStreamInput(output);
								}
							}
						}
						
					} catch (IOException e) {
						Log.w(TAG, e.getMessage(), e); output = null;
					}
					
					if (output == null) {
						ShellStream.this.destroy();
					}
				}
			};
			
			mStdOutputWorker.start();
			
		} catch (IOException e) {
			Log.w(TAG, e.getMessage(), e); mIsActive = false;
		}
	}
	
	/**
	 * Send a command to the shell input stream.<br /><br />
	 * 
	 * This method is executed asynchronous. If you need to wait until the command finishes, 
	 * then use {@link ShellStream#waitFor()}. 
	 * 
	 * @param command
	 *     The command to send to the shell
	 */
	public synchronized void execute(final String command) {
		final Object lock = new Object();
		
		new Thread() {
			@Override
			public void run() {
				mCounter.encrease();
				
				synchronized(lock) {
					lock.notifyAll();
				}
				
				synchronized(mLock) {
					if (waitFor(0, -1)) {
						mListener.onStreamStart();
						
						String input = command + "\n";
						input += "    echo " + mCommandEnd + " $?\n";
						
						try {
							mStdInput.write( input.getBytes() );
							
							/*
							 * Things often get written to the shell without flush().
							 * This breaks when using exit, as it some times get destroyed before reaching here. 
							 */
							if (mStdInput != null) {
								mStdInput.flush();
							}
							
						} catch (IOException e) {
							Log.w(TAG, e.getMessage(), e);
						}
					}
				}
			}
			
		}.start();
		
		synchronized (lock) {
			try {
				lock.wait();
				
			} catch (InterruptedException e) {}
		}
	}
	
	/**
	 * Sleeps until the shell is done with a current command and ready for new input. 
	 * 
	 * @see {@link ShellStream#waitFor(Integer)}
	 * 
	 * @return
	 *     True if the shell connection is OK or false on connection error
	 */
	public Boolean waitFor() {
		return waitFor(0, 0);
	}
	
	/**
	 * Sleeps until the shell is done with a current command and ready for new input, 
	 * or until the specified timeout has expired.<br /><br />
	 * 
	 * Note that this method keeps track of the order of executions. This means that
	 * the shell might not be ready, just because this lock was cleared. There might have been
	 * added more locks after this one was set. 
	 * 
	 * @param timeout
	 *     Timeout in milliseconds
	 * 
	 * @return
	 *     True if the shell connection is OK or false on connection error
	 */
	public Boolean waitFor(Integer timeout) {
		return waitFor(timeout, 0);
	}
	
	/**
	 * This is an internal method, which is used to change which object to add a lock to.
	 */
	protected Boolean waitFor(Integer timeout, Integer index) {
		Integer counter = mCounter.size()+index;
		
		if (counter > 0) {
			Long timeoutMilis = timeout > 0 ? System.currentTimeMillis() + timeout : 0L;
			
			synchronized(mLock) {
				while (mCounter.size() > 0 && mIsActive) {
					try {
						counter -= 1;
						
						mLock.wait(timeout.longValue());
						
						if (timeout > 0 && System.currentTimeMillis() >= timeoutMilis) {
							return mCounter.size() == 0 && mIsActive;
							
						} else if (counter <= 0) {
							return mIsActive;
						}
						
					} catch (InterruptedException e) {
						Log.w(TAG, e.getMessage(), e);
					}
				}
			}
		}
		
		return mIsActive;
	}
	
	/**
	 * Check whether there is a connection to the shell.  
	 * 
	 * @return
	 *     True if there is a connection or False if not 
	 */
	public Boolean isActive() {
		return mIsActive;
	}
	
	/**
	 * Check whether the shell is currently busy processing a command. 
	 * 
	 * @return
	 *     True if the shell is busy or False otherwise
	 */
	public Boolean isRunning() {
		return mCounter.size() > 0;
	}
	
	/**
	 * Check whether or not root was requested for this instance.
	 */
	public Boolean isRoot() {
		return mIsRoot;
	}
	
	/**
	 * Close the shell connection. <br /><br />
	 * 
	 * This will force close the connection. Use this only when running a consistent command (if {@link ShellStream#isRunning()} returns true). 
	 * When possible, sending the 'exit' command to the shell is a better choice. <br /><br />
	 * 
	 * This method is executed asynchronous.
	 */
	public synchronized void destroy() {
		if (mStdInput != null) {
			mIsActive = false;
			
			mCounter.reset();
			
			try {
				mStdInput.close();
				mStdInput = null;
				
			} catch (IOException e) {}
			
			mStdOutputWorker.interrupt();
			mStdOutputWorker = null;
			
			synchronized (mLock) {
				mLock.notifyAll();
			}
			
			mListener.onStreamDied();
			mListener = null;
		}
	}
}
