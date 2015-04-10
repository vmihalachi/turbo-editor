/*
 * This file is part of the RootFW Project: https://github.com/spazedog/rootfw
 *  
 * Copyright (c) 2015 Daniel Bergløv
 *
 * RootFW is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * RootFW is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.

 * You should have received a copy of the GNU Lesser General Public License
 * along with RootFW. If not, see <http://www.gnu.org/licenses/>
 */

package com.spazedog.lib.rootfw4.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import android.content.Context;
import android.os.PowerManager;
import android.util.Log;

import com.spazedog.lib.rootfw4.Common;
import com.spazedog.lib.rootfw4.Shell;
import com.spazedog.lib.rootfw4.Shell.Result;
import com.spazedog.lib.rootfw4.containers.BasicContainer;

public class Device {
	public static final String TAG = Common.TAG + ".Device";
	
	protected final static Pattern oPatternPidMatch = Pattern.compile("^[0-9]+$");
	protected final static Pattern oPatternSpaceSearch = Pattern.compile("[ \t]+");
	
	protected Shell mShell;
	
	/**
	 * This is a container class used to store information about a process.
	 */
	public static class ProcessInfo extends BasicContainer {
		private String mPath;
		private String mProcess;
		private Integer mProcessId;
		
		/** 
		 * @return
		 *     The process path (Could be NULL) as not all processes has a path assigned
		 */
		public String path() {
			return mPath;
		}
		
		/** 
		 * @return
		 *     The name of the process
		 */
		public String name() {
			return mProcess;
		}
		
		/** 
		 * @return
		 *     The pid of the process
		 */
		public Integer pid() {
			return mProcessId;
		}
	}
	
	public Device(Shell shell) {
		mShell = shell;
	}
	
	/**
	 * Return a list of all active processes.
	 */
	public ProcessInfo[] getProcessList() {
		return getProcessList(null);
	}
	
	/**
	 * Return a list of all active processes which names matches 'pattern'.
	 * Note that this does not provide an advanced search, it just checks whether or not 'pattern' exists in the process name. 
	 * 
	 * @param pattern
	 *     The pattern to search for
	 */
	public ProcessInfo[] getProcessList(String pattern) {
		String[] files = mShell.getFile("/proc").getList();
		
		if (files != null) {
			List<ProcessInfo> processes = new ArrayList<ProcessInfo>();
			String process = null;
			String path = null;
			
			for (int i=0; i < files.length; i++) {
				if (oPatternPidMatch.matcher(files[i]).matches()) {
					if ((process = mShell.getFile("/proc/" + files[i] + "/cmdline").readOneLine()) == null) {
						if ((process = mShell.getFile("/proc/" + files[i] + "/stat").readOneLine()) != null) {
							try {
								if (pattern == null || process.contains(pattern)) {
									process = oPatternSpaceSearch.split(process.trim())[1];
									process = process.substring(1, process.length()-1);
									
								} else {
									continue;
								}
								
							} catch(Throwable e) { process = null; }
						}
						
					} else if (pattern == null || process.contains(pattern)) {
						if (process.contains("/")) {
							try {
								path = process.substring(process.indexOf("/"), process.contains("-") ? process.indexOf("-", process.lastIndexOf("/", process.indexOf("-"))) : process.length());
							} catch (Throwable e) { path = null; }
								
							if (!process.startsWith("/")) {
								process = process.substring(0, process.indexOf("/"));
								
							} else {
								try {
									process = process.substring(process.lastIndexOf("/", process.contains("-") ? process.indexOf("-") : process.length())+1, process.contains("-") ? process.indexOf("-", process.lastIndexOf("/", process.indexOf("-"))) : process.length());
									
								} catch (Throwable e) { process = null; }
							}
							
						} else if (process.contains("-")) {
							process = process.substring(0, process.indexOf("-"));
						}
						
					} else {
						continue;
					}
					
					if (pattern == null || (process != null && process.contains(pattern))) {
						ProcessInfo stat = new ProcessInfo();
						stat.mPath = path;
						stat.mProcess = process;
						stat.mProcessId = Integer.parseInt(files[i]);
						
						processes.add(stat);
					}
				}
			}
			
			return processes.toArray( new ProcessInfo[ processes.size() ] );
		}
		
		return null;
	}
	
	/**
	 * Reboots the device into the recovery.<br /><br />
	 * 
	 * This method first tries using the {@link PowerManager}, if that fails it fallbacks on using the reboot command from toolbox.<br /><br />
	 * 
	 * Note that using the {@link PowerManager} requires your app to optain the 'REBOOT' permission. If you don't want this, just parse NULL as {@link Context} 
	 * and the method will use the fallback. This however is more likely to fail, as many toolbox versions does not support the reboot command. 
	 * And since only the kernel can write to the CBC, we need a native caller to invoke this. So there is no fallback for missing toolbox support when it comes 
	 * to rebooting into the recovery. 
	 * 
	 * @param context
	 *     A {@link Context} or NULL to skip using the {@link PowerManager}
	 */
	public Boolean rebootRecovery(Context context) {
		if (context != null) {
			try {
				PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
				pm.reboot(null);
				
				/*
				 * This will never be reached if the reboot is successful
				 */
				return false;	
			
			} catch (Throwable e) {}
		}
		
		Result result = mShell.execute("toolbox reboot recovery");
		
		return result != null && result.wasSuccessful();
	}
	
	/**
	 * Invokes a soft reboot on the device (Restart all services) by using a sysrq trigger.
	 */
	public Boolean rebootSoft() {
		Result result = mShell.execute("echo 1 > /proc/sys/kernel/sysrq && echo s > /proc/sysrq-trigger && echo e > /proc/sysrq-trigger");
		
		return result != null && result.wasSuccessful();
	}
	
	/**
	 * Reboots the device.<br /><br />
	 * 
	 * This method first tries using the reboot command from toolbox. 
	 * But since some toolbox versions does not have this, it further fallbacks on using a sysrq trigger.
	 */
	public Boolean reboot() {
		Result result = mShell.execute("toolbox reboot");
		
		if (result == null || !result.wasSuccessful()) {
			result = mShell.execute("echo 1 > /proc/sys/kernel/sysrq && echo s > /proc/sysrq-trigger && echo b > /proc/sysrq-trigger");
		}
		
		return result != null && result.wasSuccessful();
	}
	
	/**
	 * Shuts down the device.<br /><br />
	 * 
	 * This method first tries using the reboot command from toolbox. 
	 * But since some toolbox versions does not have this, it further fallbacks on using a sysrq trigger.
	 */
	public Boolean shutdown() {
		Result result = mShell.execute("toolbox reboot -p");
		
		if (result == null || !result.wasSuccessful()) {
			result = mShell.execute("echo 1 > /proc/sys/kernel/sysrq && echo s > /proc/sysrq-trigger && echo o > /proc/sysrq-trigger");
		}
		
		return result != null && result.wasSuccessful();
	}
	
	/**
	 * Get a new {@link Process} instance
	 * 
	 * @param process
	 *     The name of the process
	 */
	public Process getProcess(String process) {
		return new Process(mShell, process);
	}
	
	/**
	 * Get a new {@link Process} instance
	 * 
	 * @param pid
	 *     The process id
	 */
	public Process getProcess(Integer pid) {
		return new Process(mShell, pid);
	}
	
	public static class Process extends Device {
		
		protected Integer mPid;
		protected String mProcess;
		
		public Process(Shell shell, String process) {
			super(shell);
			
			if (oPatternPidMatch.matcher(process).matches()) {
				mPid = Integer.parseInt(process);
				
			} else {
				mProcess = process;
			}
		}
		
		public Process(Shell shell, Integer pid) {
			super(shell);
			
			mPid = pid;
		}
		
		/**
		 * Get the pid of the current process. 
		 * If you initialized this object using a process id, this method will return that id. 
		 * Otherwise it will return the first found pid in /proc.
		 */
		public Integer getPid() {
			/*
			 * A process might have more than one pid. So we never cache a search. If mPid is not null, 
			 * then a specific pid was chosen for this instance, and this is what we should work with. 
			 * But if a process name was chosen, we should never cache the pid as we might one the next one if we kill this process or it dies or reboots. 
			 */
			if (mPid != null) {
				return mPid;
			}
			
			/*
			 * Busybox returns 1 both when pidof is not supported and if the process does not exist. 
			 * We need to check if we have some kind of pidof support from either busybox, toolbox or another registered binary. 
			 * If not, we fallback on a /proc search. 
			 */
			String cmd = mShell.findCommand("pidof");
			
			if (cmd != null) {
				Result result = mShell.execute(cmd + " '" + mProcess + "'");
				
				String pids = result.getLine();
				
				if (pids != null) {
					try {
						return Integer.parseInt(oPatternSpaceSearch.split(pids.trim())[0]);
					
					} catch (Throwable e) {
						Log.w(TAG, e.getMessage(), e);
					}
				}
				
			} else {
				ProcessInfo[] processes = getProcessList();
				
				if (processes != null) {
					for (int i=0; i < processes.length; i++) {
						if (mProcess.equals(processes[i].name())) {
							return processes[i].pid();
						}
					}
				}
			}
			
			return 0;
		}
		
		/**
		 * Get a list of all pid's for this process name. 
		 */
		public Integer[] getPids() {
			String name = getName();
			String cmd = mShell.findCommand("pidof");
			
			if (cmd != null) {
				Result result = mShell.createAttempts(cmd + " '" + name + "'").execute();
				
				if (result != null && result.wasSuccessful()) {
					String pids = result.getLine();
					
					if (pids != null) {
						String[] parts = oPatternSpaceSearch.split(pids.trim());
						Integer[] values = new Integer[ parts.length ];
								
						for (int i=0; i < parts.length; i++) {
							try {
								values[i] = Integer.parseInt(parts[i]);
								
							} catch(Throwable e) {}
						}
						
						return values;
					}
				}
				
			} else {
				ProcessInfo[] processes = getProcessList();
				
				if (name != null && processes != null && processes.length > 0) {
					List<Integer> list = new ArrayList<Integer>();
					
					for (int i=0; i < processes.length; i++) {
						if (name.equals(processes[i].name())) {
							list.add(processes[i].pid());
						}
					}
					
					return list.toArray( new Integer[ list.size() ] );
				}
			}
			
			return null;
		}
		
		/**
		 * Get the process name of the current process. 
		 * If you initialized this object using a process name, this method will return that name. 
		 * Otherwise it will locate it in /proc based on the pid.
		 */
		public String getName() {
			if (mProcess != null) {
				return mProcess;
			}
			
			String process = null;
			
			if ((process = mShell.getFile("/proc/" + mPid + "/cmdline").readOneLine()) == null) {
				if ((process = mShell.getFile("/proc/" + mPid + "/stat").readOneLine()) != null) {
					try {
						process = oPatternSpaceSearch.split(process.trim())[1];
						process = process.substring(1, process.length()-1);
						
					} catch(Throwable e) { process = null; }
				}
				
			} else if (process.contains("/")) {
				if (!process.startsWith("/")) {
					process = process.substring(0, process.indexOf("/"));
					
				} else {
					try {
						process = process.substring(process.lastIndexOf("/", process.contains("-") ? process.indexOf("-") : process.length())+1, process.contains("-") ? process.indexOf("-", process.lastIndexOf("/", process.indexOf("-"))) : process.length());
						
					} catch (Throwable e) { process = null; }
				}
				
			} else if (process.contains("-")) {
				process = process.substring(0, process.indexOf("-"));
			}
			
			return process;
		}
		
		/**
		 * Kill this process. 
		 * If you initialized this object using a pid, only this single process will be killed. 
		 * If you used a process name, all processes with this process name will be killed.  
		 */
		public Boolean kill() {
			Result result = null;
			
			if (mPid != null) {
				result = mShell.createAttempts("kill -9 '" + mPid + "'").execute();
				
			} else {
				result = mShell.createAttempts("killall '" + mProcess + "'").execute();
				
				/*
				 * Toolbox does not support killall
				 */
				if (result == null || !result.wasSuccessful()) {
					Integer[] pids = getPids();
					
					for (Integer pid : pids) {
						result = mShell.createAttempts("kill -9 '" + pid + "'").execute();
						
						if (result == null || !result.wasSuccessful()) {
							return false;
						}
					}
				}
			}

			return result != null && result.wasSuccessful();
		}
	}
}
