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

import com.spazedog.lib.rootfw4.Common;
import com.spazedog.lib.rootfw4.Shell;
import com.spazedog.lib.rootfw4.Shell.Result;
import com.spazedog.lib.rootfw4.containers.BasicContainer;
import com.spazedog.lib.rootfw4.utils.File.FileData;

/**
 * This class is used to get information about the device memory. 
 * It can provide information about total memory and swap space, free memory and swap etc. 
 * It can also be used to check the device support for CompCache/ZRam and Swap along with 
 * providing a list of active Swap and CompCache/ZRam devices. 
 */
public class Memory {
	public static final String TAG = Common.TAG + ".Memory";
	
	protected final static Pattern oPatternSpaceSearch = Pattern.compile("[ \t]+");
	
	protected static Boolean oCompCacheSupport;
	protected static Boolean oSwapSupport;
	
	protected Shell mShell;
	
	/**
	 * This is a container which is used to store information about a SWAP device
	 */
	public static class SwapStat extends BasicContainer {
		private String mDevice;
		private Long mSize;
		private Long mUsage;
		
		/** 
		 * @return
		 *     Path to the SWAP device
		 */
		public String device() {
			return mDevice;
		}
		
		/** 
		 * @return
		 *     SWAP size in bytes
		 */
		public Long size() {
			return mSize;
		}
		
		/** 
		 * @return
		 *     SWAP usage in bytes
		 */
		public Long usage() {
			return mUsage;
		}
	}
	
	/**
	 * This container is used to store all memory information from /proc/meminfo.
	 */
	public static class MemStat extends BasicContainer {
		private Long mMemTotal = 0L;
		private Long mMemFree = 0L;
		private Long mMemCached = 0L;
		private Long mSwapTotal = 0L;
		private Long mSwapFree = 0L;
		private Long mSwapCached = 0L;
		
		/** 
		 * @return
		 *     Total amount of memory in bytes, including SWAP space
		 */
		public Long total() {
			return mMemTotal + mSwapTotal;
		}
		
		/** 
		 * @return
		 *     Free amount of memory in bytes, including SWAP space and cached memory
		 */
		public Long free() {
			return mMemFree + mSwapFree + (mMemCached + mSwapCached);
		}
		
		/** 
		 * @return
		 *     Amount of cached memory including SWAP space
		 */
		public Long cached() {
			return mMemCached + mSwapCached;
		}
		
		/** 
		 * @return
		 *     Amount of used memory including SWAP (Cached memory not included)
		 */
		public Long usage() {
			return total() - free();
		}
		
		/** 
		 * @return
		 *     Memory usage in percentage, including SWAP space (Cached memory not included)
		 */
		public Integer percentage() {
			return ((Long) ((usage() * 100L) / total())).intValue();
		}
		
		/** 
		 * @return
		 *     Total amount of memory in bytes
		 */
		public Long memTotal() {
			return mMemTotal;
		}
		
		/** 
		 * @return
		 *     Free amount of memory in bytes, including cached memory
		 */
		public Long memFree() {
			return mMemFree + mMemCached;
		}
		
		/** 
		 * @return
		 *     Amount of cached memory
		 */
		public Long memCached() {
			return mMemCached;
		}
		
		/** 
		 * @return
		 *     Amount of used memory (Cached memory not included)
		 */
		public Long memUsage() {
			return memTotal() - memFree();
		}
		
		/** 
		 * @return
		 *     Memory usage in percentage (Cached memory not included)
		 */
		public Integer memPercentage() {
			try {
				return ((Long) ((memUsage() * 100L) / memTotal())).intValue();
			
			} catch (Throwable e) {
				return 0;
			}
		}
		
		/** 
		 * @return
		 *     Total amount of SWAP space in bytes
		 */
		public Long swapTotal() {
			return mSwapTotal;
		}
		
		/** 
		 * @return
		 *     Free amount of SWAP space in bytes, including cached memory
		 */
		public Long swapFree() {
			return mSwapFree + mSwapCached;
		}
		
		/** 
		 * @return
		 *     Amount of cached SWAP space
		 */
		public Long swapCached() {
			return mSwapCached;
		}
		
		/** 
		 * @return
		 *     Amount of used SWAP space (Cached memory not included)
		 */
		public Long swapUsage() {
			return swapTotal() - swapFree();
		}
		
		/** 
		 * @return
		 *     SWAP space usage in percentage (Cached memory not included)
		 */
		public Integer swapPercentage() {
			try {
				return ((Long) ((swapUsage() * 100L) / swapTotal())).intValue();
			
			} catch (Throwable e) {
				return 0;
			}
		}
	}
	
	public Memory(Shell shell) {
		mShell = shell;
	}
	
	/**
	 * Get memory information like ram usage, ram total, cached memory, swap total etc.
	 */
	public MemStat getUsage() {
		FileData data = mShell.getFile("/proc/meminfo").read();
		
		if (data != null && data.size() > 0) {
			String[] lines = data.getArray();
			MemStat stat = new MemStat();
			
			for (int i=0; i < lines.length; i++) {
				String[] parts = oPatternSpaceSearch.split(lines[i]);
				
				if (parts[0].equals("MemTotal:")) {
					stat.mMemTotal = Long.parseLong(parts[1]) * 1024L;
					
				} else if (parts[0].equals("MemFree:")) {
					stat.mMemFree = Long.parseLong(parts[1]) * 1024L;
					
				} else if (parts[0].equals("Cached:")) {
					stat.mMemCached = Long.parseLong(parts[1]) * 1024L;
					
				} else if (parts[0].equals("SwapTotal:")) {
					stat.mSwapTotal = Long.parseLong(parts[1]) * 1024L;
					
				} else if (parts[0].equals("SwapFree:")) {
					stat.mSwapFree = Long.parseLong(parts[1]) * 1024L;
					
				} else if (parts[0].equals("SwapCached:")) {
					stat.mSwapCached = Long.parseLong(parts[1]) * 1024L;
					
				}
			}
			
			return stat;
		}
		
		return null;
	}
	
	/**
	 * Get a list of all active SWAP devices.
	 *     
	 * @return
	 *     An SwapStat array of all active SWAP devices
	 */
	public SwapStat[] getSwapList() {
		File file = mShell.getFile("/proc/swaps");
		
		if (file.exists()) {
			String[] data = file.readMatch("/dev/", false).trim().getArray();
			List<SwapStat> statList = new ArrayList<SwapStat>();
			
			if (data != null && data.length > 0) {
				for (int i=0; i < data.length; i++) {
					try {
						String[] sections = oPatternSpaceSearch.split(data[i].trim());
						
						SwapStat stat = new SwapStat();
						stat.mDevice = sections[0];
						stat.mSize = Long.parseLong(sections[2]) * 1024L;
						stat.mUsage = Long.parseLong(sections[3]) * 1024L;
						
						statList.add(stat);
						
					} catch(Throwable e) {}
				}
				
				return statList.size() > 0 ? statList.toArray( new SwapStat[ statList.size() ] ) : null;
			}
		}
		
		return null;
	}
	
	/**
	 * Check whether or not CompCache/ZRam is supported by the kernel. 
	 * This also checks for Swap support. If this returns FALSE, then none of them is supported. 
	 */
	public Boolean hasCompCacheSupport() {
		if (oCompCacheSupport == null) {
			oCompCacheSupport = false;
			
			if (hasSwapSupport()) {
				String[] files = new String[]{"/dev/block/ramzswap0", "/dev/block/zram0", "/system/lib/modules/ramzswap.ko", "/system/lib/modules/zram.ko"};
				
				for (String file : files) {
					if (mShell.getFile(file).exists()) {
						oCompCacheSupport = true; break;
					}
				}
			}
		}
		
		return oCompCacheSupport;
	}
	
	/**
	 * Check whether or not Swap is supported by the kernel. 
	 */
	public Boolean hasSwapSupport() {
		if (oSwapSupport == null) {
			oSwapSupport = mShell.getFile("/proc/swaps").exists();
		}
		
		return oSwapSupport;
	}
	
	/**
	 * Get a new instance of {@link Swap}
	 * 
	 * @param device
	 *     The Swap block device
	 */
	public Swap getSwap(String device) {
		return new Swap(mShell, device);
	}
	
	/**
	 * Get a new instance of {@link CompCache}
	 */
	public CompCache getCompCache() {
		return new CompCache(mShell);
	}
	
	/**
	 * Change the swappiness level.<br /><br />
	 * 
	 * The level should be between 0 for low swap usage and 100 for high swap usage.
	 * 
	 * @param level
	 *     The swappiness level
	 */
	public Boolean setSwappiness(Integer level) {
		Result result = null;
		
		if (level >= 0 && level <= 100 && hasSwapSupport()) {
			result = mShell.execute("echo '" + level + "' > /proc/sys/vm/swappiness");
		}
		
		return result != null && result.wasSuccessful();
	}
	
	/**
	 * Get the current swappiness level.
	 */
	public Integer getSwappiness() {
		if (hasSwapSupport()) {
			String output = mShell.getFile("/proc/sys/vm/swappiness").readOneLine();
			
			if (output != null) {
				try {
					return Integer.parseInt(output);
					
				} catch (Throwable e) {}
			}
		}
		
		return 0;
	}
	
	/**
	 * This class is an extension of the {@link Memory} class. 
	 * This can be used to get information about-, and handle swap and CompCache/ZRam devices.
	 */
	public static class Swap extends Memory {
		
		protected File mSwapDevice;
		
		/**
		 * Create a new instance of this class.
		 * 
		 * @param shell
		 *     An instance of the {@link Shell} class
		 *     
		 * @param device
		 *     The swap/zram block device
		 */
		public Swap(Shell shell, String device) {
			super(shell);
			
			if (device != null) {
				mSwapDevice = mShell.getFile(device);
				
				if (!mSwapDevice.getAbsolutePath().startsWith("/dev/")) {
					mSwapDevice = null;
				}
			}
		}
		
		/**
		 * Get information like size and usage of a specific SWAP device. This method will return null if the device does not exist, or if it has not been activated.
		 * 
		 * @param device
		 *     The specific SWAP device path to get infomation about
		 *     
		 * @return
		 *     An SwapStat object containing information about the requested SWAP device
		 */
		public SwapStat getSwapDetails() {
			if (exists()) {
				File file = mShell.getFile("/proc/swaps");
				
				if (file.exists()) {
					String data = file.readMatch(mSwapDevice.getAbsolutePath(), false).getLine();
					
					if (data != null && data.length() > 0) {
						try {
							String[] sections = oPatternSpaceSearch.split(data);
							
							SwapStat stat = new SwapStat();
							stat.mDevice = sections[0];
							stat.mSize = Long.parseLong(sections[2]) * 1024L;
							stat.mUsage = Long.parseLong(sections[3]) * 1024L;
							
							return stat;
							
						} catch(Throwable e) {}
					}
				}
			}
			
			return null;
		}
		
		/**
		 * Check whether or not this block device exists. 
		 * This will also return FALSE if this is not a /dev/ device.
		 */
		public Boolean exists() {
			return mSwapDevice != null && mSwapDevice.exists();
		}
		
		/**
		 * Check whether or not this Swap device is currently active.
		 */
		public Boolean isActive() {
			return getSwapDetails() != null;
		}
		
		/**
		 * Get the path to the Swap block device. 
		 */
		public String getPath() {
			return mSwapDevice != null ? mSwapDevice.getResolvedPath() : null;
		}
		
		/**
		 * @see #setSwapOn(Integer)
		 */
		public Boolean setSwapOn() {
			return setSwapOn(0);
		}
		
		/**
		 * Enable this Swap device.
		 * 
		 * @param priority
		 *     Priority (Highest number is used first) or use '0' for auto
		 */
		public Boolean setSwapOn(Integer priority) {
			if (exists()) {
				Boolean status = isActive();
				
				if (!status) {
					String[] commands = null;
					
					if (priority > 0) {
						commands = new String[]{"swapon -p '" + priority + "' '" + mSwapDevice.getAbsolutePath() + "'", "swapon '" + mSwapDevice.getAbsolutePath() + "'"};
						
					} else {
						commands = new String[]{"swapon '" + mSwapDevice.getAbsolutePath() + "'"};
					}
					
					for (String command : commands) {
						Result result = mShell.createAttempts(command).execute();
						
						if (result != null && result.wasSuccessful()) {
							return true;
						}
					}
				}
				
				return status;
			}
			
			return false;
		}
		
		/**
		 * Disable this Swap device.
		 */
		public Boolean setSwapOff() {
			if (exists()) {
				Boolean status = isActive();
				
				if (status) {
					Result result = mShell.createAttempts("swapoff '" + mSwapDevice.getAbsolutePath() + "'").execute();
					
					return result != null && result.wasSuccessful();
				}
				
				return status;
			}
			
			return true;
		}
	}
	
	/**
	 * This is an extension of the {@link Swap} class. It's job is more CompCache/ZRam orientated. 
	 * Unlike it's parent, this class can not only switch a CompCache/ZRam device on and off, it can also 
	 * locate the proper supported type and load it's kernel module if not already done during boot. <br /><br />
	 * 
	 * It is advised to use this when working with CompCache/ZRam specifically. 
	 */
	public static class CompCache extends Swap {
		
		protected static String oCachedDevice;
		
		/**
		 * Create a new instance of this class. <br /><br />
		 * 
		 * This constructor will automatically look for the CompCache/ZRam block device, and 
		 * enable the feature (loading modules) if not already done by the kernel during boot. 
		 * 
		 * @param shell
		 *     An instance of the {@link Shell} class
		 */
		public CompCache(Shell shell) {
			super(shell, oCachedDevice);
			
			if (oCachedDevice == null) {
				String[] blockDevices = new String[]{"/dev/block/ramzswap0", "/dev/block/zram0"};
				String[] libraries = new String[]{"/system/lib/modules/ramzswap.ko", "/system/lib/modules/zram.ko"};
				
				for (int i=0; i < blockDevices.length; i++) {
					if (mShell.getFile(blockDevices[i]).exists()) {
						oCachedDevice = blockDevices[i]; break;
						
					} else if (mShell.getFile(libraries[i]).exists()) {
						Result result = mShell.createAttempts("insmod '" + libraries[i] + "'").execute();
						
						if (result != null && result.wasSuccessful()) {
							oCachedDevice = blockDevices[i]; break;
						}
					}
				}
				
				if (oCachedDevice != null) {
					mSwapDevice = mShell.getFile(oCachedDevice);
				}
			}
		}
		
		/**
		 * Enable this Swap device.<br /><br />
		 * 
		 * This overwrites {@link Swap#setSwapOn()} to enable to feature of 
		 * setting a cache size for the CompCache/ZRam. This method sets the size to 18% of the total device memory. <br /><br />
		 * 
		 * If you are sure that CompCache/ZRam is loaded and the device has been setup with size and swap partition and you don't want to change this, 
		 * then use {@link Swap#setSwapOn()} instead. But if nothing has been setup yet, it could fail as it does nothing else but try to activate the device as Swap.
		 * 
		 * @see #setSwapOn(Integer)
		 */
		@Override
		public Boolean setSwapOn(Integer priority) {
			return setSwapOn(priority, 18);
		}
		
		/**
		 * Enable this Swap device.<br /><br />
		 * 
		 * The total CompCache/ZRam size will be the chosen percentage 
		 * of the total device memory, although max 35%. If a greater value is chosen, 35 will be used. 
		 * 
		 * @param cacheSize
		 *     The percentage value of the total device memory
		 */
		public Boolean setSwapOn(Integer priority, Integer cacheSize) {
			cacheSize = cacheSize > 35 ? 35 : (cacheSize <= 0 ? 18 : cacheSize);
			
			if (exists()) {
				Boolean status = isActive();
				
				if (!status) {
					Result result = null;
					MemStat stat = getUsage();
					
					if (stat != null) {
						if (oCachedDevice.endsWith("/zram0")) {
							result = mShell.createAttempts(
									"echo 1 > /sys/block/zram0/reset && " + 
									"echo '" + ((stat.memTotal() * cacheSize) / 100) + "' > /sys/block/zram0/disksize && " + 
									"%binary mkswap '" + mSwapDevice.getAbsolutePath() + "'"
									
							).execute();
							
						} else {
							result = mShell.execute("rzscontrol '" + mSwapDevice.getAbsolutePath() + "' --disksize_kb='" + (((stat.memTotal() * cacheSize) / 100) * 1024) + "' --init");
						}
						
						if (result != null && result.wasSuccessful()) {
							String[] commands = null;
							
							if (priority > 0) {
								commands = new String[]{"swapon -p '" + priority + "' '" + mSwapDevice.getAbsolutePath() + "'", "swapon '" + mSwapDevice.getAbsolutePath() + "'"};
								
							} else {
								commands = new String[]{"swapon '" + mSwapDevice.getAbsolutePath() + "'"};
							}
							
							for (String command : commands) {
								result = mShell.createAttempts(command).execute();
								
								if (result != null && result.wasSuccessful()) {
									return true;
								}
							}
						}
					}
				}
				
				return status;
			}
			
			return false;
		}
		
		/**
		 * Disable this Swap device.<br /><br />
		 * 
		 * This overwrites {@link Swap#setSwapOff()} as this will also release the CompCache/ZRam from memory.
		 */
		@Override
		public Boolean setSwapOff() {
			if (exists()) {
				Boolean status = isActive();
				
				if (status) {
					Result result = null;
					
					if (oCachedDevice.endsWith("/zram0")) {
						result = mShell.createAttempts("swapoff '" + mSwapDevice.getAbsolutePath() + "' && echo 1 > /sys/block/zram0/reset").execute();
						
					} else {
						result = mShell.createAttempts("swapoff '" + mSwapDevice.getAbsolutePath() + "' && rzscontrol '" + mSwapDevice.getAbsolutePath() + "' --reset").execute();
					}
					
					return result != null && result.wasSuccessful();
				}
				
				return status;
			}
			
			return true;
		}
	}
}
