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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import android.text.TextUtils;

import com.spazedog.lib.rootfw4.Common;
import com.spazedog.lib.rootfw4.Shell;
import com.spazedog.lib.rootfw4.Shell.Result;
import com.spazedog.lib.rootfw4.containers.BasicContainer;
import com.spazedog.lib.rootfw4.utils.File.FileData;

public class Filesystem {
	public static final String TAG = Common.TAG + ".Filesystem";
	
	protected final static Pattern oPatternSpaceSearch = Pattern.compile("[ \t]+");
	protected final static Pattern oPatternSeparatorSearch = Pattern.compile(",");
	protected final static Pattern oPatternPrefixSearch = Pattern.compile("^.*[A-Za-z]$");
	
	protected static MountStat[] oFstabList;
	protected static final Object oFstabLock = new Object();
	
	protected Shell mShell;
	protected Object mLock = new Object();
	
	/**
	 * This is a container used to store disk information.
	 */
	public static class DiskStat extends BasicContainer {
		private String mDevice;
		private String mLocation;
		private Long mSize;
		private Long mUsage;
		private Long mAvailable;
		private Integer mPercentage;
		
		/** 
		 * @return
		 *     Device path
		 */
		public String device() {
			return mDevice;
		}
		
		/** 
		 * @return
		 *     Mount location
		 */
		public String location() {
			return mLocation;
		}
		
		/** 
		 * @return
		 *     Disk size in bytes
		 */
		public Long size() {
			return mSize;
		}
		
		/** 
		 * @return
		 *     Disk usage size in bytes
		 */
		public Long usage() {
			return mUsage;
		}
		
		/** 
		 * @return
		 *     Disk available size in bytes
		 */
		public Long available() {
			return mAvailable;
		}
		
		/** 
		 * @return
		 *     Disk usage percentage
		 */
		public Integer percentage() {
			return mPercentage;
		}
	}
	
	/**
	 * This is a container used to store mount information.
	 */
	public static class MountStat extends BasicContainer {
		private String mDevice;
		private String mLocation;
		private String mFstype;
		private String[] mOptions;
		
		/** 
		 * @return
		 *     The device path
		 */
		public String device() {
			return mDevice;
		}
		
		/** 
		 * @return
		 *     The mount location
		 */
		public String location() {
			return mLocation;
		}
		
		/** 
		 * @return
		 *     The device file system type
		 */
		public String fstype() {
			return mFstype;
		}
		
		/** 
		 * @return
		 *     The options used at mount time
		 */
		public String[] options() {
			return mOptions;
		}
	}
	
	public Filesystem(Shell shell) {
		mShell = shell;
	}
	
	/**
	 * Just like {@link #getMountList} this will provide a list of mount points and disks. The difference is that this list will not be of 
	 * all the currently mounted partitions, but a list of all defined mount point in each fstab and init.*.rc file on the device.
	 * <br /><br />
	 * It can be useful in situations where a file system might have been moved by a script, and you need the original defined location. 
	 * Or perhaps you need the original device of a specific mount location.
	 *     
	 * @return
	 *     An array of {@link MountStat} objects
	 */
	public MountStat[] getFsList() {
		synchronized(oFstabLock) {
			if (oFstabList == null) {
				Result result = mShell.execute("for DIR in /fstab.* /fstab /init.*.rc /init.rc; do echo $DIR; done");
				
				if (result != null && result.wasSuccessful()) {
					Set<String> cache = new HashSet<String>();
					List<MountStat> list = new ArrayList<MountStat>();
					String[] dirs = result.trim().getArray();
					
					for (int i=0; i < dirs.length; i++) {
						if (!Common.isEmulator() && dirs[i].contains("goldfish")) {
							continue;
						}
						
						Boolean isFstab = dirs[i].contains("fstab");
						FileData data = mShell.getFile(dirs[i]).readMatch( (isFstab ? "/dev/" : "mount "), false );
						
						if (data != null) {
							String[] lines = data.assort("#").getArray();
							
							if (lines != null) {
								for (int x=0; x < lines.length; x++) {
									try {
										String[] parts = oPatternSpaceSearch.split(lines[x].trim(), 5);
										String options = isFstab || parts.length > 4 ? parts[ isFstab ? 3 : 4 ].replaceAll(",", " ") : "";
										
										if (parts.length > 3 && !cache.contains(parts[ isFstab ? 1 : 3 ])) {
											if (!isFstab && parts[2].contains("mtd@")) {
												
												FileData mtd = mShell.getFile("/proc/mtd").readMatch( ("\"" + parts[2].substring(4) + "\""), false );
												
												if (mtd != null && mtd.size() > 0) {
													parts[2] = "/dev/block/mtdblock" + mtd.getLine().substring(3, mtd.getLine().indexOf(":"));
												}
												
											} else if (!isFstab && parts[2].contains("loop@")) {
												parts[2] = parts[2].substring(5);
												options += " loop";
											}
											
											MountStat stat = new MountStat();
											
											stat.mDevice = parts[ isFstab ? 0 : 2 ];
											stat.mFstype = parts[ isFstab ? 2 : 1 ];
											stat.mLocation = parts[ isFstab ? 1 : 3 ];
											stat.mOptions = oPatternSpaceSearch.split(options);
											
											list.add(stat);
											cache.add(parts[ isFstab ? 1 : 3 ]);
										}
										
									} catch(Throwable e) {}
								}
							}
						}
					}
					
					oFstabList = list.toArray( new MountStat[ list.size() ] );
				}
			}
			
			return oFstabList;
		}
	}
	
	/**
	 * This will return a list of all currently mounted file systems, with information like 
	 * device path, mount location, file system type and mount options.
	 *     
	 * @return
	 *     An array of {@link MountStat} objects
	 */
	public MountStat[] getMountList() {
		FileData data = mShell.getFile("/proc/mounts").read();
		
		if (data != null) {
			String[] lines = data.trim().getArray();
			MountStat[] list = new MountStat[ lines.length ];
			
			for (int i=0; i < lines.length; i++) {
				try {
					String[] parts = oPatternSpaceSearch.split(lines[i].trim());
					
					list[i] = new MountStat();
					list[i].mDevice = parts[0];
					list[i].mFstype = parts[2];
					list[i].mLocation = parts[1];
					list[i].mOptions = oPatternSeparatorSearch.split(parts[3]);
					
				} catch(Throwable e) {}
			}
			
			return list;
		}
		
		return null;
	}
	
	/**
	 * Get an instance of the {@link Disk} class.
	 * 
	 * @param disk
	 *     The location to the disk, partition or folder
	 */
	public Disk getDisk(String disk) {
		return new Disk(mShell, disk);
	}
	
	public static class Disk extends Filesystem {
		
		protected File mFile;
		
		public Disk(Shell shell, String disk) {
			super(shell);
			
			mFile = shell.getFile(disk);
		}
		
		/**
		 * This is a short method for adding additional options to a mount location or device. 
		 * For an example, parsing remount instructions. 
		 * 
		 * @see #mount(String, String, String[])
		 * 
		 * @param options
		 *     A string array containing all of the mount options to parse
		 *     
		 * @return
		 *     <code>True</code> on success, <code>False</code> otherwise
		 */
		public Boolean mount(String[] options) {
			return mount(null, null, options);
		}
		
		/**
		 * This is a short method for attaching a device or folder to a location, without any options or file system type specifics. 
		 * 
		 * @see #mount(String, String, String[])
		 * 
		 * @param location
		 *     The location where the device or folder should be attached to
		 *     
		 * @return
		 *     <code>True</code> on success, <code>False</code> otherwise
		 */
		public Boolean mount(String location) {
			return mount(location, null, null);
		}
		
		/**
		 * This is a short method for attaching a device or folder to a location, without any file system type specifics. 
		 * 
		 * @see #mount(String, String, String[])
		 * 
		 * @param location
		 *     The location where the device or folder should be attached to
		 *     
		 * @param options
		 *     A string array containing all of the mount options to parse
		 *     
		 * @return
		 *     <code>True</code> on success, <code>False</code> otherwise
		 */
		public Boolean mount(String location, String[] options) {
			return mount(location, null, options);
		}
		
		/**
		 * This is a short method for attaching a device or folder to a location, without any options. 
		 * 
		 * @see #mount(String, String, String[])
		 * 
		 * @param location
		 *     The location where the device or folder should be attached to
		 *     
		 * @param type
		 *     The file system type to mount a device as
		 *     
		 * @return
		 *     <code>True</code> on success, <code>False</code> otherwise
		 */
		public Boolean mount(String location, String type) {
			return mount(location, type, null);
		}
		
		/**
		 * This is used for attaching a device or folder to a location, 
		 * or to change any mount options on a current mounted file system. 
		 * <br />
		 * Note that if the device parsed to the constructor {@link #Disk(Shell, String)} 
		 * is a folder, this method will use the <code>--bind</code> option to attach it to the location. Also note that when attaching folders to a location, 
		 * the <code>type</code> and <code>options</code> arguments will not be used and should just be parsed as <code>NULL</code>.
		 * 
		 * @param location
		 *     The location where the device or folder should be attached to
		 *     
		 * @param type
		 *     The file system type to mount a device as
		 *     
		 * @param options
		 *     A string array containing all of the mount options to parse
		 *     
		 * @return
		 *     <code>True</code> on success, <code>False</code> otherwise
		 */
		public Boolean mount(String location, String type, String[] options) {
			String cmd = location != null && mFile.isDirectory() ? 
					"mount --bind '" + mFile.getAbsolutePath() + "' '" + location + "'" : 
						"mount" + (type != null ? " -t '" + type + "'" : "") + (options != null ? " -o '" + (location == null ? "remount," : "") + TextUtils.join(",", Arrays.asList(options)) + "'" : "") + " '" + mFile.getAbsolutePath() + "'" + (location != null ? " '" + location + "'" : "");
			
			/*
			 * On some devices, some partitions has been made read-only by writing to the block device ioctls. 
			 * This means that even mounting them as read/write will not work by itself, we need to change the ioctls as well. 
			 */
			if (options != null && !"/".equals(mFile.getAbsolutePath())) {
				for (String option : options) {
					if ("rw".equals(option)) {
						String blockdevice = null;
						
						if (mFile.isDirectory()) {
							MountStat stat = getMountDetails();
							
							if (stat != null) {
								blockdevice = stat.device();
								
							} else if ((stat = getFsDetails()) != null) {
								blockdevice = stat.device();
							}
							
						} else {
							blockdevice = mFile.getAbsolutePath();
						}
						
						if (blockdevice != null && blockdevice.startsWith("/dev/")) {
							mShell.createAttempts("blockdev --setrw '" + blockdevice + "' 2> /dev/null").execute();
						}
						
						break;
					}
				}
			}
			
			Result result = mShell.createAttempts(cmd).execute();
			
			return result != null && result.wasSuccessful();
		}
		
		/**
		 * This method is used to remove an attachment of a device or folder (unmount). 
		 *     
		 * @return
		 *     <code>True</code> on success, <code>False</code> otherwise
		 */
		public Boolean unmount() {
			String[] commands = new String[]{"umount '" + mFile.getAbsolutePath() + "'", "umount -f '" + mFile.getAbsolutePath() + "'"};
			
			for (String command : commands) {
				Result result = mShell.createAttempts(command).execute();
				
				if (result != null && result.wasSuccessful()) {
					return true;
				}
			}

			return false;
		}
		
		/**
		 * This method is used to move a mount location to another location.
		 *     
		 * @return
		 *     <code>True</code> on success, <code>False</code> otherwise
		 */
		public Boolean move(String destination) {
			Result result = mShell.createAttempts("mount --move '" + mFile.getAbsolutePath() + "' '" + destination + "'").execute();
			
			if (result == null || !result.wasSuccessful()) {
				/*
				 * Not all toolbox versions support moving mount points. 
				 * So in these cases, we fallback to a manual unmount/remount.
				 */
				MountStat stat = getMountDetails();
				
				if (stat != null && unmount()) {
					return getDisk(stat.device()).mount(stat.location(), stat.fstype(), stat.options());
				}
			}
			
			return result != null && result.wasSuccessful();
		}
		
		/**
		 * This is used to check whether the current device or folder is attached to a location (Mounted).
		 *     
		 * @return
		 *     <code>True</code> if mounted, <code>False</code> otherwise
		 */
		public Boolean isMounted() {
			return getMountDetails() != null;
		}
		
		/**
		 * This is used to check if a mounted file system was mounted with a specific mount option. 
		 * Note that options like <code>mode=xxxx</code> can also be checked by just parsing <code>mode</code> as the argument.
		 *     
		 * @param option
		 *     The name of the option to find
		 *     
		 * @return
		 *     <code>True</code> if the options was used to attach the device, <code>False</code> otherwise
		 */
		public Boolean hasOption(String option) {
			MountStat stat = getMountDetails();
			
			if (stat != null) {
				String[] options = stat.options();
				
				if (options != null && options.length > 0) {
					for (int i=0; i < options.length; i++) {
						if (options[i].equals(option) || options[i].startsWith(option + "=")) {
							return true;
						}
					}
				}
			}
			
			return false;
		}
		
		/**
		 * This can be used to get the value of a specific mount option that was used to attach the file system. 
		 * Note that options like <code>noexec</code>, <code>nosuid</code> and <code>nodev</code> does not have any values and will return <code>NULL</code>. 
		 * This method is used to get values from options like <code>gid=xxxx</code>, <code>mode=xxxx</code> and <code>size=xxxx</code> where <code>xxxx</code> is the value. 
		 *     
		 * @param option
		 *     The name of the option to find
		 *     
		 * @return
		 *     <code>True</code> if the options was used to attach the device, <code>False</code> otherwise
		 */
		public String getOption(String option) {
			MountStat stat = getMountDetails();
			
			if (stat != null) {
				String[] options = stat.options();
				
				if (options != null && options.length > 0) {
					for (int i=0; i < options.length; i++) {
						if (options[i].startsWith(option + "=")) {
							return options[i].substring( options[i].indexOf("=")+1 );
						}
					}
				}
			}
			
			return null;
		}
		
		/**
		 * This is the same as {@link #getMountList()}, 
		 * only this method will just return the mount information for this specific device or mount location. 
		 *     
		 * @return
		 *     A single {@link MountStat} object
		 */
		public MountStat getMountDetails() {
			MountStat[] list = getMountList();
			
			if (list != null) {
				String path = mFile.getAbsolutePath();
				
				if (!mFile.isDirectory()) {
					for (int i=0; i < list.length; i++) {
						if (list[i].device().equals(path)) {
							return list[i];
						}
					}
					
				} else {
					do {
						for (int i=0; i < list.length; i++) {
							if (list[i].location().equals(path)) {
								return list[i];
							}
						}
						
					} while (path.lastIndexOf("/") > 0 && !(path = path.substring(0, path.lastIndexOf("/"))).equals(""));
				}
			}
			
			return null;
		}
		
		/**
		 * This is the same as {@link #getFsList()}, 
		 * only this method will just return the mount information for this specific device or mount location. 
		 *     
		 * @return
		 *     A single {@link MountStat} object
		 */
		public MountStat getFsDetails() {
			MountStat[] list = getFsList();
			
			if (list != null) {
				String path = mFile.getAbsolutePath();
				
				if (!mFile.isDirectory()) {
					for (int i=0; i < list.length; i++) {
						if (list[i].device().equals(path)) {
							return list[i];
						}
					}
					
				} else {
					do {
						for (int i=0; i < list.length; i++) {
							if (list[i].location().equals(path)) {
								return list[i];
							}
						}
						
					} while (path.lastIndexOf("/") > 0 && !(path = path.substring(0, path.lastIndexOf("/"))).equals(""));
				}
			}
			
			return null;
		}
		
		/**
		 * Like with {@link #getMountList()}, this will also return information like device path and mount location. 
		 * However, it will not return information like file system type or mount options, but instead 
		 * information about the disk size, remaining bytes, used bytes and usage percentage. 
		 *     
		 * @return
		 *     A single {@link DiskStat} object
		 */
		public DiskStat getDiskDetails() {
			String[] commands = new String[]{"df -k '" + mFile.getAbsolutePath() + "'", "df '" + mFile.getAbsolutePath() + "'"};
			
			for (String command : commands) {
				Result result = mShell.createAttempts(command).execute();

				if (result != null && result.wasSuccessful() && result.size() > 1) {
					/* Depending on how long the line is, the df command some times breaks a line into two */
					String[] parts = oPatternSpaceSearch.split(result.sort(1).trim().getString(" ").trim());
					
					/*
					 * Any 'df' output, no mater which toolbox or busybox version, should contain at least 
					 * 'device or mount location', 'size', 'used' and 'available'
					 */
					if (parts.length > 3) {
						String pDevice=null, pLocation=null, prefix, prefixList[] = {"k", "m", "g", "t"};
						Integer pPercentage=null;
						Long pUsage, pSize, pRemaining;
						Double[] pUsageSections = new Double[3];
						
						if (parts.length > 5) {
							/* Busybox output */
							
							pDevice = parts[0];
							pLocation = parts[5];
							pPercentage = Integer.parseInt(parts[4].substring(0, parts[4].length()-1));
							
						} else {
							/* Toolbox output */
							
							/* Depending on Toolbox version, index 0 can be both the device or the mount location */
							MountStat stat = getMountDetails();
							
							if (stat != null) {
								pDevice = stat.device();
								pLocation = stat.location();
							}
						}
						
						/* Make sure that the sizes of usage, capacity etc does not have a prefix. Not all toolbox and busybox versions supports the '-k' argument, 
						 * and some does not produce an error when parsed */
						for (int i=1; i < 4; i++) {
							if (i < parts.length) {
								if (oPatternPrefixSearch.matcher(parts[i]).matches()) {
									pUsageSections[i-1] = Double.parseDouble( parts[i].substring(0, parts[i].length()-1) );
									prefix = parts[i].substring(parts[i].length()-1).toLowerCase(Locale.US);
									
									for (int x=0; x < prefixList.length; x++) {
										pUsageSections[i-1] = pUsageSections[i-1] * 1024D;
										
										if (prefixList[x].equals(prefix)) {
											break;
										}
									}
									
								} else {
									pUsageSections[i-1] = Double.parseDouble(parts[i]) * 1024D;
								}
		
							} else {
								pUsageSections[i-1] = 0D;
							}
						}
						
						pSize = pUsageSections[0].longValue();
						pUsage = pUsageSections[1].longValue();
						pRemaining = pUsageSections[2].longValue();
						
						if (pPercentage == null) {
							/* You cannot divide by zero */
							pPercentage = pSize != 0 ? ((Long) ((pUsage * 100L) / pSize)).intValue() : 0;
						}
						
						DiskStat info = new DiskStat();
						info.mDevice = pDevice;
						info.mLocation = pLocation;
						info.mSize = pSize;
						info.mUsage = pUsage;
						info.mAvailable = pRemaining;
						info.mPercentage = pPercentage;
		
						return info;
					}
				}
			}
			
			return null;
		}
	}
}
