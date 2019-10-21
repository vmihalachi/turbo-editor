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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.spazedog.lib.rootfw4.Common;
import com.spazedog.lib.rootfw4.Shell;
import com.spazedog.lib.rootfw4.Shell.Attempts;
import com.spazedog.lib.rootfw4.Shell.OnShellBroadcastListener;
import com.spazedog.lib.rootfw4.Shell.OnShellResultListener;
import com.spazedog.lib.rootfw4.Shell.OnShellValidateListener;
import com.spazedog.lib.rootfw4.Shell.Result;
import com.spazedog.lib.rootfw4.containers.BasicContainer;
import com.spazedog.lib.rootfw4.containers.Data;
import com.spazedog.lib.rootfw4.containers.Data.DataSorting;
import com.spazedog.lib.rootfw4.utils.Filesystem.DiskStat;
import com.spazedog.lib.rootfw4.utils.Filesystem.MountStat;
import com.spazedog.lib.rootfw4.utils.io.FileReader;
import com.spazedog.lib.rootfw4.utils.io.FileWriter;

public class File {
	public static final String TAG = Common.TAG + ".File";
	
	protected final static Pattern oPatternEscape = Pattern.compile("([\"\'`\\\\])");
	protected final static Pattern oPatternColumnSearch = Pattern.compile("[ ]{2,}");
	protected final static Pattern oPatternSpaceSearch = Pattern.compile("[ \t]+");
	protected final static Pattern oPatternStatSplitter = Pattern.compile("\\|");
	protected final static Pattern oPatternStatSearch = Pattern.compile("^([a-z-]+)(?:[ \t]+([0-9]+))?[ \t]+([0-9a-z_]+)[ \t]+([0-9a-z_]+)(?:[ \t]+(?:([0-9]+),[ \t]+)?([0-9]+))?[ \t]+([A-Za-z]+[ \t]+[0-9]+[ \t]+[0-9:]+|[0-9-/]+[ \t]+[0-9:]+)[ \t]+(?:(.*) -> )?(.*)$");

	protected final static Map<String, Integer> oOctals = new HashMap<String, Integer>();
	static {
		oOctals.put("1:r", 400);
		oOctals.put("2:w", 200);
		oOctals.put("3:x", 100);
		oOctals.put("3:s", 4100);
		oOctals.put("3:S", 4000);
		oOctals.put("4:r", 40);
		oOctals.put("5:w", 20);
		oOctals.put("6:x", 10);
		oOctals.put("6:s", 2010);
		oOctals.put("6:S", 2000);
		oOctals.put("7:r", 4);
		oOctals.put("8:w", 2);
		oOctals.put("9:x", 1);
		oOctals.put("9:t", 1001);
		oOctals.put("9:T", 1000);
	}
	
	protected java.io.File mFile;
	protected Shell mShell;
	protected final Object mLock = new Object();
	
	protected Integer mExistsLevel = -1;
	protected Integer mFolderLevel = -1;
	protected Integer mLinkLevel = -1;
	
	/**
	 * This class is extended from the Data class. As for now, there is nothing custom added to this class. But it might differ from the Data class at some point.
	 */
	public static class FileData extends Data<FileData> {
		public FileData(String[] lines) {
			super(lines);
		}
	}
	
	/**
	 * This class is a container which is used by {@link FileExtender#getDetails()} and {@link FileExtender#getDetailedList(Integer)}
	 */
	public static class FileStat extends BasicContainer {
		private String mName;
		private String mLink;
		private String mType;
		private Integer mUser;
		private Integer mGroup;
		private String mAccess;
		private Integer mPermission;
		private String mMM;
		private Long mSize;
		
		/** 
		 * @return
		 *     The filename
		 */
		public String name() {
			return mName;
		}
		
		/** 
		 * @return
		 *     The path to the original file if this is a symbolic link
		 */
		public String link() {
			return mLink;
		}
		
		/** 
		 * @return
		 *     The file type ('d'=>Directory, 'f'=>File, 'b'=>Block Device, 'c'=>Character Device, 'l'=>Symbolic Link)
		 */
		public String type() {
			return mType;
		}
		
		/** 
		 * @return
		 *     The owners user id
		 */
		public Integer user() {
			return mUser;
		}
		
		/** 
		 * @return
		 *     The owners group id
		 */
		public Integer group() {
			return mGroup;
		}
		
		/** 
		 * @return
		 *     The files access string like (drwxrwxr-x)
		 */
		public String access() {
			return mAccess;
		}
		
		/** 
		 * @return
		 *     The file permissions like (0755)
		 */
		public Integer permission() {
			return mPermission;
		}
		
		/** 
		 * @return
		 *     The file Major:Minor number (If this is a Block or Character device file)
		 */
		public String mm() {
			return mMM;
		}
		
		/** 
		 * @return
		 *     The file size in bytes
		 */
		public Long size() {
			return mSize;
		}
	}
	
	public File(Shell shell, String file) {
		mFile = new java.io.File(file);
		mShell = shell;
		
		/*
		 * This broadcast listener lets us update information about files in other instances of this class. 
		 * Some information uses a lot of resources to gather and are quite persistent, and by using this mechanism, we can cache those information 
		 * and still have them updated when needed. 
		 */
		mShell.addBroadcastListener(new OnShellBroadcastListener() {
			@Override
			public void onShellBroadcast(String key, Bundle data) {
				if ("file".equals(key)) {
					String action = data.getString("action");
					String location = data.getString("location");
					
					if ("exists".equals(action) && (getAbsolutePath().equals(location) || getAbsolutePath().startsWith(location + "/"))) {
						mExistsLevel = -1;
						mFolderLevel = -1;
						mLinkLevel = -1;
						
					} else if ("moved".equals(action) && getAbsolutePath().equals(location)) {
						mFile = new java.io.File(data.getString("destination"));
					}
				}
			}
		});
	}
	
	/**
	 * Get information about this file or folder. This will return information like 
	 * size (on files), path to linked file (on links), permissions, group, user etc.
	 * 
	 * @return
	 *     A new {@link FileStat} object with all the file information
	 */
	public FileStat getDetails() {
		synchronized (mLock) {
			if (exists()) {
				FileStat[] stat = getDetailedList(1);
				
				if (stat != null && stat.length > 0) {
					String name = mFile.getName();
					
					if (stat[0].name().equals(".")) {
						stat[0].mName = name;
						
						return stat[0];
						
					} else if (stat[0].name().equals(name)) {
						return stat[0];
						
					} else {
						/* On devices without busybox, we could end up using limited toolbox versions
						 * that does not support the "-a" argument in it's "ls" command. In this case,
						 * we need to do a more manual search for folders.
						 */
						stat = getParentFile().getDetailedList();
						
						if (stat != null && stat.length > 0) {
							for (int i=0; i < stat.length; i++) {
								if (stat[i].name().equals(name)) {
									return stat[i];
								}
							}
						}
					}
				}
			}
			
			return null;
		}
	}
	
	/**
	 * @see #getDetailedList(Integer)
	 */
	public FileStat[] getDetailedList() {
		return getDetailedList(0);
	}
	
	/**
	 * This is the same as {@link #getDetails()}, only this will provide a whole list 
	 * with information about each item in a directory.
	 * 
	 * @param maxLines
	 *     The max amount of lines to return. This also excepts negative numbers. 0 equals all lines.
	 * 
	 * @return
	 *     An array of {@link FileStat} object
	 */
	public FileStat[] getDetailedList(Integer maxLines) {
		synchronized (mLock) {
			if (exists()) {
				String path = getAbsolutePath();
				String[] attemptCommands = new String[]{"ls -lna '" + path + "'", "ls -la '" + path + "'", "ls -ln '" + path + "'", "ls -l '" + path + "'"};
				
				for (String command : attemptCommands) {
					Result result = mShell.createAttempts(command).execute();
					
					if (result.wasSuccessful()) {
						List<FileStat> list = new ArrayList<FileStat>();
						String[] lines = result.trim().getArray();
						Integer maxIndex = (maxLines == null || maxLines == 0 ? lines.length : (maxLines < 0 ? lines.length + maxLines : maxLines));
						
						for (int i=0,indexCount=1; i < lines.length && indexCount <= maxIndex; i++) {
							/* There are a lot of different output from the ls command, depending on the arguments supported, whether we used busybox or toolbox and the versions of the binaries. 
							 * We need some serious regexp help to sort through all of the different output options. 
							 */
							String[] parts = oPatternStatSplitter.split( oPatternStatSearch.matcher(lines[i]).replaceAll("$1|$3|$4|$5|$6|$8|$9") );
							
							if (parts.length == 7) {
								FileStat stat = new FileStat();
								
								stat.mType = parts[0].substring(0, 1).equals("-") ? "f" : parts[0].substring(0, 1);
								stat.mAccess = parts[0];
								stat.mUser = Common.getUID(parts[1]);
								stat.mGroup = Common.getUID(parts[2]);
								stat.mSize = parts[4].equals("null") || !parts[3].equals("null") ? 0L : Long.parseLong(parts[4]);
								stat.mMM = parts[3].equals("null") ? null : parts[3] + ":" + parts[4];
								stat.mName = parts[5].equals("null") ? parts[6].substring( parts[6].lastIndexOf("/") + 1 ) : parts[5].substring( parts[5].lastIndexOf("/") + 1 );
								stat.mLink = parts[5].equals("null") ? null : parts[6];
								stat.mPermission = 0;

								for (int x=1; x < stat.mAccess.length(); x++) {
									Character ch = stat.mAccess.charAt(x);
									Integer number = oOctals.get(x + ":" + ch);

									if (number != null) {
										stat.mPermission += number;
									}
								}
								
								if (stat.mName.contains("/")) {
									stat.mName = stat.mName.substring( stat.mName.lastIndexOf("/")+1 );
								}
								
								list.add(stat);
								
								indexCount++;
							}
						}
						
						return list.toArray( new FileStat[ list.size() ] );
					}
				}
			}
			
			return null;
		}
	}
	
	/**
	 * This will provide a simple listing of a directory.
	 * For a more detailed listing, use {@link #getDetailedList()} instead. 
	 * 
	 * @return
	 *     An array with the names of all the items in the directory
	 */
	public String[] getList() {
		synchronized (mLock) {
			if (isDirectory()) {
				String[] list = mFile.list();
				
				if (list == null) {
					String path = getAbsolutePath();
					String[] commands = new String[]{"ls -a1 '" + path + "'", "ls -a '" + path + "'", "ls '" + path + "'"};
					
					for (int i=0; i < commands.length; i++) {
						Result result = mShell.createAttempts(commands[i]).execute();
						
						if (result != null && result.wasSuccessful()) {
							if (i == 0) {
								result.sort(new DataSorting(){
									@Override
									public Boolean test(String input) {
										return !".".equals(input) && !"..".equals(input);
									}
								});
								
								return result.getArray();
								
							} else {
								/*
								 * Most toolbox versions supports very few flags, and 'ls -a' on toolbox might return 
								 * a list, whereas busybox mostly returns columns. So we need to be able to handle both types of output. 
								 * Some toolbox versions does not support any flags at all, and they differ from each version about what kind of output 
								 * they return. 
								 */
								String[] lines = oPatternColumnSearch.split( result.trim().getString("  ").trim() );
								List<String> output = new ArrayList<String>();
								
								for (String line : lines) {
									if (!".".equals(line) && !"..".equals(line)) {
										output.add(line);
									}
								}
								
								return output.toArray(new String[output.size()]);
							}
						}
					}
				}
				
				return list;
			}
			
			return null;
		}
	}
	
	/**
	 * Extract the first line of the file.
	 * 
	 * @return
	 *     The first line of the file as a string
	 */
	public String readOneLine() {
		synchronized (mLock) {
			if (isFile()) {
				try {
					BufferedReader reader = new BufferedReader(new java.io.FileReader(mFile));
					String line = reader.readLine();
					reader.close();
					
					return line;
					
				} catch (Throwable e) {
					String[] attemptCommands = new String[]{"sed -n '1p' '" + getAbsolutePath() + "' 2> /dev/null", "cat '" + getAbsolutePath() + "' 2> /dev/null"};
					
					for (String command : attemptCommands) {
						Result result = mShell.createAttempts(command).execute();
						
						if (result != null && result.wasSuccessful()) {
							return result.getLine(0);
						}
					}
				}
			}
			
			return null;
		}
	}
	
	/**
	 * Extract the content from the file and return it.
	 * 
	 * @return
	 *     The entire file content wrapped in a {@link FileData} object
	 */
	public FileData read() {
		synchronized (mLock) {
			if (isFile()) {
				try {
					BufferedReader reader = new BufferedReader(new java.io.FileReader(mFile));
					List<String> content = new ArrayList<String>();
					String line;
					
					while ((line = reader.readLine()) != null) {
						content.add(line);
					}
					
					reader.close();
					
					return new FileData( content.toArray( new String[ content.size() ] ) );
					
				} catch(Throwable e) {
					Result result = mShell.createAttempts("cat '" + getAbsolutePath() + "' 2> /dev/null").execute();
					
					if (result != null && result.wasSuccessful()) {
						return new FileData( result.getArray() );
					}
				}
			}
			
			return null;
		}
	}
	
	/**
	 * Search the file line by line to find a match for a specific word or sentence and return all of the matched lines or the ones not matching.
	 * 
	 * @param match
	 *     Word or sentence to match
	 *     
	 * @param invert
	 *     Whether or not to return the non-matching lines instead
	 * 
	 * @return
	 *     All of the matched or non-matched lines wrapped in a {@link FileData} object
	 */
	public FileData readMatch(final String match, final Boolean invert) {
		synchronized (mLock) {
			if (isFile()) {
				try {
					BufferedReader reader = new BufferedReader(new java.io.FileReader(mFile));
					List<String> content = new ArrayList<String>();
					String line;
					
					while ((line = reader.readLine()) != null) {
						if (invert != line.contains(match)) {
							content.add(line);
						}
					}
					
					reader.close();
					
					return new FileData( content.toArray( new String[ content.size() ] ) );
					
				} catch (Throwable e) {
					String escapedMatch = oPatternEscape.matcher(match).replaceAll("\\\\$1");
					
					/*
					 * 'grep' returns failed on 0 matches, which will normally make the shell continue it's attempts. 
					 * So we use a validate listener to check the output. If there is no real errors, then we will have no output. 
					 */
					Result result = mShell.createAttempts("grep " + (invert ? "-v " : "") + "'" + escapedMatch + "' '" + getAbsolutePath() + "'").execute(new OnShellValidateListener(){
						@Override
						public Boolean onShellValidate(String command, Integer result, List<String> output, Set<Integer> resultCodes) {
							return result.equals(0) || output.size() == 0;
						}
					});
					
					if (result.wasSuccessful()) {
						return new FileData( result.getArray() );
						
					} else {
						result = mShell.createAttempts("cat '" + getAbsolutePath() + "' 2> /dev/null").execute();
						
						if (result != null && result.wasSuccessful()) {
							result.sort(new DataSorting() {
								@Override
								public Boolean test(String input) {
									return invert != input.contains(match);
								}
							});
							
							return new FileData( result.getArray() );
						}
					}
				}
			}
			
			return null;
		}
	}
	
	/**
	 * @see #write(String[], Boolean)
	 */
	public Boolean write(String input) {
		return write(input.trim().split("\n"), false);
	}
	
	/**
	 * @see #write(String[], Boolean)
	 */
	public Boolean write(String input, Boolean append) {
		return write(input.trim().split("\n"), append);
	}
	
	/**
	 * @see #write(String[], Boolean)
	 */
	public Boolean write(String[] input) {
		return write(input, false);
	}
	
	/**
	 * Write data to the file. The data should be an array where each index is a line that should be written to the file. 
	 * <br />
	 * If the file does not already exist, it will be created. 
	 * 
	 * @param input
	 *     The data that should be written to the file
	 *     
	 * @param append
	 *     Whether or not to append the data to the existing content in the file
	 * 
	 * @return
	 *     <code>True</code> if the data was successfully written to the file, <code>False</code> otherwise
	 */
	public Boolean write(String[] input, Boolean append) {
		synchronized (mLock) {
			Boolean status = false;
			
			if (input != null && !isDirectory()) {
				try {
					BufferedWriter output = new BufferedWriter(new java.io.FileWriter(mFile, append));
					
					for (String line : input) {
						output.write(line);
						output.newLine();
					}
					
					output.close();
					status = true;
					
				} catch(Throwable e) {
					String redirect = append ? ">>" : ">";
					String path = getAbsolutePath();
					
					for (String line : input) {
						String escapedInput = oPatternEscape.matcher(line).replaceAll("\\\\$1");
						Attempts attempts = mShell.createAttempts("echo '" + escapedInput + "' " + redirect + " '" + path + "' 2> /dev/null");
						Result result = attempts.execute();
						
						if (result != null && !(status = result.wasSuccessful())) {
							break;
						}
						
						redirect = ">>";
					}
				}
				
				/*
				 * Alert other instances using this file, that the state might have changed. 
				 */
				if (status) {
					Bundle bundle = new Bundle();
					bundle.putString("action", "exists");
					bundle.putString("location", getAbsolutePath());
					
					Shell.sendBroadcast("file", bundle);
				}
			}
			
			return status;
		}
	}

	/**
	 * @see #write(String[], Boolean)
	 */
	public Result writeResult(String input) {
		return writeResult(input.trim().split("\n"), false);
	}

	public Result writeResult(String[] input, Boolean append) {
		synchronized (mLock) {
			Boolean status = false;
			Result result = null;

			if (input != null && !isDirectory()) {
				try {
					BufferedWriter output = new BufferedWriter(new java.io.FileWriter(mFile, append));

					for (String line : input) {
						output.write(line);
						output.newLine();
					}

					output.close();
					status = true;

				} catch(Throwable e) {
					String redirect = append ? ">>" : ">";
					String path = getAbsolutePath();

					for (String line : input) {
						String escapedInput = oPatternEscape.matcher(line).replaceAll("\\\\$1");
						Attempts attempts = mShell.createAttempts("echo '" + escapedInput + "' " + redirect + " '" + path + "' 2> /dev/null");
						result = attempts.execute();

						if (result != null && !(status = result.wasSuccessful())) {
							break;
						}

						redirect = ">>";
					}
				}

				/*
				 * Alert other instances using this file, that the state might have changed.
				 */
				if (status) {
					Bundle bundle = new Bundle();
					bundle.putString("action", "exists");
					bundle.putString("location", getAbsolutePath());

					Shell.sendBroadcast("file", bundle);
				}
			}

			return result;
		}
	}
	
	/**
	 * Remove the file. 
	 * Folders will be recursively cleaned before deleting. 
	 * 
	 * @return
	 *     <code>True</code> if the file was deleted, <code>False</code> otherwise
	 */
	public Boolean remove() {
		synchronized (mLock) {
			Boolean status = false;
			
			if (exists()) {
				String[] fileList = getList();
				String path = getAbsolutePath();
				
				if (fileList != null) {
					for (String intry : fileList) {
						if(!getFile(path + "/" + intry).remove()) {
							return false;
						}
					}
				}
				
				if (!(status = mFile.delete())) {
					String rmCommand = isFile() || isLink() ? "unlink" : "rmdir";
					String[] commands = new String[]{"rm -rf '" + path + "' 2> /dev/null", rmCommand + " '" + path + "' 2> /dev/null"};
					
					for (String command : commands) {
						Result result = mShell.createAttempts(command).execute();
						
						if (result != null && (status = result.wasSuccessful())) {
							break;
						}
					}
				}
				
				/*
				 * Alert other instances using this file, that the state might have changed. 
				 */
				if (status) {
					Bundle bundle = new Bundle();
					bundle.putString("action", "exists");
					bundle.putString("location", path);
					
					Shell.sendBroadcast("file", bundle);
				}
				
			} else {
				status = true;
			}
			
			return status;
		}
	}
	
	/**
	 * Create a new directory based on the path from this file object.
	 * 
	 * @see #createDirectories()
	 * 
	 * @return
	 *     <code>True</code> if the directory was created successfully or if it existed to begin with, <code>False</code> oherwise
	 */
	public Boolean createDirectory() {
		synchronized (mLock) {
			Boolean status = false;
			
			if (!exists()) {
				if (!(status = mFile.mkdir())) {
					Result result = mShell.createAttempts("mkdir '" + getAbsolutePath() + "' 2> /dev/null").execute();
					
					if (result == null || !(status = result.wasSuccessful())) {
						return false;
					}
				}
				
				/*
				 * Alert other instances using this directory, that the state might have changed. 
				 */
				if (status) {
					Bundle bundle = new Bundle();
					bundle.putString("action", "exists");
					bundle.putString("location", getAbsolutePath());
					
					Shell.sendBroadcast("file", bundle);
				}
				
			} else {
				status = isDirectory();
			}
			
			return status;
		}
	}
	
	/**
	 * Create a new directory based on the path from this file object. 
	 * The method will also create any missing parent directories.  
	 * 
	 * @see #createDirectory()
	 * 
	 * @return
	 *     <code>True</code> if the directory was created successfully
	 */
	public Boolean createDirectories() {
		synchronized (mLock) {
			Boolean status = false;
			
			if (!exists()) {
				if (!(status = mFile.mkdirs())) {
					Result result = mShell.createAttempts("mkdir -p '" + getAbsolutePath() + "' 2> /dev/null").execute();
					
					if (result == null || !(status = result.wasSuccessful())) {
						/*
						 * Some toolbox version does not support the '-p' flag in 'mkdir'
						 */
						String[] dirs = getAbsolutePath().substring(1).split("/");
						String path = "";
						
						for (String dir : dirs) {
							path = path + "/" + dir;
							
							if (!(status = getFile(path).createDirectory())) {
								return false;
							}
						}
					}
				}
				
				/*
				 * Alert other instances using this directory, that the state might have changed. 
				 */
				if (status) {
					Bundle bundle = new Bundle();
					bundle.putString("action", "exists");
					bundle.putString("location", getAbsolutePath());
					
					Shell.sendBroadcast("file", bundle);
				}
				
			} else {
				status = isDirectory();
			}
			
			return status;
		}
	}
	
	/**
	 * Create a link to this file.
	 * 
	 * @param linkPath
	 *     Path (Including name) to the link which should be created
	 * 
	 * @return
	 *     <code>True</code> on success, <code>False</code> otherwise
	 */
	public Boolean createLink(String linkPath) {
		synchronized (mLock) {
			File linkFile = getFile(linkPath);
			Boolean status = false;
			
			if (exists() && !linkFile.exists()) {
				Result result = mShell.createAttempts("ln -s '" + getAbsolutePath() + "' '" + linkFile.getAbsolutePath() + "' 2> /dev/null").execute();
				
				if (result == null || !(status = result.wasSuccessful())) {
					return false;
				}
				
				/*
				 * Alert other instances using this directory, that the state might have changed. 
				 */
				if (status) {
					Bundle bundle = new Bundle();
					bundle.putString("action", "exists");
					bundle.putString("location", linkFile.getAbsolutePath());
					
					Shell.sendBroadcast("file", bundle);
				}
				
			} else if (exists() && linkFile.isLink()) {
				status = getAbsolutePath().equals(linkFile.getCanonicalPath());
			}
			
			return status;
		}
	}
	
	/**
	 * Create a reference from this path to another (This will become the link)
	 * 
	 * @param linkPath
	 *     Path (Including name) to the original location
	 * 
	 * @return
	 *     <code>True</code> on success, <code>False</code> otherwise
	 */
	public Boolean createAsLink(String originalPath) {
		return getFile(originalPath).createLink(getAbsolutePath());
	}
	
	/**
	 * @see #move(String, Boolean)
	 */
	public Boolean move(String dstPath) {
		return move(dstPath, false);
	}
	
	/**
	 * Move the file to another location. 
	 * 
	 * @param dstPath
	 *     The destination path including the file name
	 * 
	 * @return
	 *     <code>True</code> on success, <code>False</code> otherwise
	 */
	public Boolean move(String dstPath, Boolean overwrite) {
		synchronized (mLock) {
			Boolean status = false;
			
			if (exists()) {
				File dstFile = getFile(dstPath);

				if (!dstFile.exists() || (overwrite && dstFile.remove())) {
					if (!(status = mFile.renameTo(dstFile.mFile))) {
						Result result = mShell.createAttempts("mv '" + getAbsolutePath() + "' '" + dstFile.getAbsolutePath() + "'").execute();
						
						if (result == null || !(status = result.wasSuccessful())) {
							return false;
						}
					}
				}
				
				/*
				 * Alert other instances using this file, that it has been moved.
				 */
				if (status) {
					Bundle bundle = new Bundle();
					bundle.putString("action", "exists");
					bundle.putString("location", dstFile.getAbsolutePath());
					Shell.sendBroadcast("file", bundle);
					
					bundle.putString("action", "moved");
					bundle.putString("location", getAbsolutePath());
					bundle.putString("destination", dstFile.getAbsolutePath());
					
					mFile = dstFile.mFile;
					Shell.sendBroadcast("file", bundle);
				}
			}
			
			return status;
		}
	}
	
	/**
	 * Rename the file. 
	 * 
	 * @param name
	 *     The new name to use
	 * 
	 * @return
	 *     <code>True</code> on success, <code>False</code> otherwise
	 */
	public Boolean rename(String name) {
		return move( (getParentPath() == null ? "" : getParentPath()) + "/" + name, false );
	}
	
	/**
	 * @see #copy(String, Boolean, Boolean)
	 */
	public Boolean copy(String dstPath) {
		return copy(dstPath, false, false);
	}
	
	/**
	 * @see #copy(String, Boolean, Boolean)
	 */
	public Boolean copy(String dstPath, Boolean overwrite) {
		return copy(dstPath, overwrite, false);
	}
	
	/**
	 * Copy the file to another location.
	 * 
	 * @param dstPath
	 *     The destination path
	 *     
	 * @param overwrite
	 *     Overwrite any existing files. If false, then folders will be merged if a destination folder exist.
	 *     
	 * @param preservePerms
	 *     Preserve permissions
	 * 
	 * @return
	 *     <code>True</code> on success, <code>False</code> otherwise
	 */
	public Boolean copy(String dstPath, Boolean overwrite, Boolean preservePerms) {
		synchronized (mLock) {
			Boolean status = false;
			
			if (exists()) {
				File dstFile = getFile(dstPath);
				FileStat stat = null;
				
				/*
				 * On overwrite, delete the destination if it exists, and make sure that we are able to recreate 
				 * destination directory, if the source is one.
				 * 
				 * On non-overwrite, skip files if they exists, or merge if source and destination are directories. 
				 */
				if (isLink()) {
					if (!dstFile.exists() || (overwrite && dstFile.remove())) {
						stat = getDetails();

						if (stat == null || stat.link() == null || !(status = dstFile.createAsLink(stat.link()))) {
							return false;
						}
					}

				} else if (isDirectory() && (!overwrite || (!dstFile.exists() || dstFile.remove())) && ((!dstFile.exists() && dstFile.createDirectories()) || dstFile.isDirectory())) {
					String[] list = getList();
					
					if (list != null) {
						status = true;
						String srcAbsPath = getAbsolutePath();
						String dstAbsPath = dstFile.getAbsolutePath();
						
						for (String entry : list) {
							File entryFile = getFile(srcAbsPath + "/" + entry);
							
							if (!(status = entryFile.copy(dstAbsPath + "/" + entry, overwrite, preservePerms))) {
								if (entryFile.isDirectory() || overwrite == entryFile.exists()) {
									return false;
									
								} else {
									status = true;
								}
							}
						}
					}
					
				} else if (!isDirectory() && (!dstFile.exists() || (overwrite && dstFile.remove()))) {
					try {
						InputStream input = new FileInputStream(mFile);
						OutputStream output = new FileOutputStream(dstFile.mFile);
						
						byte[] buffer = new byte[1024];
						Integer length;
						
						while ((length = input.read(buffer)) > 0) {
							output.write(buffer, 0, length);
						}
						
						input.close();
						output.close();
						
						status = true;
						
					} catch (Throwable e) {
						Result result = mShell.createAttempts("cat '" + getAbsolutePath() + "' > '" + dstFile.getAbsolutePath() + "' 2> /dev/null").execute();
						
						if (result == null || !(status = result.wasSuccessful())) {
							return false;
						}
					}
				}
				
				if (status) {
					Bundle bundle = new Bundle();
					bundle.putString("action", "exists");
					bundle.putString("location", dstFile.getAbsolutePath());
					
					Shell.sendBroadcast("file", bundle);
					
					if (preservePerms) {
						if (stat == null) {
							stat = getDetails();
						}
						
						dstFile.changeAccess(stat.user(), stat.group(), stat.permission(), false);
					}
				}
			}
			
			return status;
		}
	}
	
	/**
	 * @see #changeAccess(String, String, Integer, Boolean)
	 */
	public Boolean changeAccess(String user, String group, Integer mod) {
		return changeAccess(Common.getUID(user), Common.getUID(group), mod, false);
	}
	
	/**
	 * @see #changeAccess(Integer, Integer, Integer, Boolean)
	 */
	public Boolean changeAccess(Integer user, Integer group, Integer mod) {
		return changeAccess(user, group, mod, false);
	}
	
	/**
	 * Change ownership (user and group) and permissions on a file or directory.<br /><br />
	 * 
	 * Never use octal numbers for the permissions like '0775'. Always write it as '775', otherwise it will be converted 
	 * and your permissions will not be changed to the expected value. The reason why this argument is an Integer, is to avoid 
	 * things like 'a+x', '+x' and such. While this is supported in Linux normally, few Android binaries supports it as they have been 
	 * stripped down to the bare minimum. 
	 * 
	 * @param user
	 *     The user name or NULL if this should not be changed
	 *     
	 * @param group
	 *     The group name or NULL if this should not be changed
	 *     
	 * @param mod
	 *     The octal permissions or -1 if this should not be changed
	 *     
	 * @param recursive
	 *     Change the access recursively
	 */
	public Boolean changeAccess(String user, String group, Integer mod, Boolean recursive) {
		return changeAccess(Common.getUID(user), Common.getUID(group), mod, recursive);
	}
	
	/**
	 * Change ownership (user and group) and permissions on a file or directory. <br /><br />
	 * 
	 * Never use octal numbers for the permissions like '0775'. Always write it as '775', otherwise it will be converted 
	 * and your permissions will not be changed to the expected value. The reason why this argument is an Integer, is to avoid 
	 * things like 'a+x', '+x' and such. While this is supported in Linux normally, few Android binaries supports it as they have been 
	 * stripped down to the bare minimum. 
	 * 
	 * @param user
	 *     The uid or -1 if this should not be changed
	 *     
	 * @param group
	 *     The gid or -1 if this should not be changed
	 *     
	 * @param mod
	 *     The octal permissions or -1 if this should not be changed
	 *     
	 * @param recursive
	 *     Change the access recursively
	 */
	public Boolean changeAccess(Integer user, Integer group, Integer mod, Boolean recursive) {
		synchronized (mLock) {
			StringBuilder builder = new StringBuilder();
			
			if ((user != null && user >= 0) || (group != null && group >= 0)) {
				builder.append("%binary chown ");
				
				if (recursive)
					builder.append("-R ");
				
				if (user != null && user >= 0) 
					builder.append("" + user);
				
				if (group != null && group >= 0) 
					builder.append("." + user);
			}
			
			if (mod != null && mod > 0) {
				if (builder.length() > 0)
					builder.append(" && ");
				
				builder.append("%binary chmod ");
				
				if (recursive)
					builder.append("-R ");
				
				builder.append((mod <= 777 ? "0" : "") + mod);
			}
			
			if (builder.length() > 0) {
				builder.append(" '" + getAbsolutePath() + "'");
				
				Result result = mShell.createAttempts(builder.toString()).execute();
				
				if (result != null && result.wasSuccessful()) {
					return true;
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Calculates the size of a file or folder. <br /><br />
	 * 
	 * Note that on directories with a lot of sub-folders and files, 
	 * this can be a slow operation.
	 * 
	 * @return
	 *     0 if the file does not exist, or if it is actually 0 in size of course. 
	 */
	public Long size() {
		synchronized (mLock) {
			Long size = 0L;
			
			if (exists()) {
				if (isDirectory()) {
					String[] list = getList();
					
					if (list != null) {
						String path = getAbsolutePath();
						
						for (String entry : list) {
							size += getFile(path + "/" + entry).size();
						}
					}
					
				} else if ((size = mFile.length()) == 0) {
					String path = getAbsolutePath();
					String[] commands = new String[]{"wc -c < '" + path + "' 2> /dev/null", "wc < '" + path + "' 2> /dev/null"};
					Result result = null;
					
					for (int i=0; i < commands.length; i++) {
						result = mShell.createAttempts(commands[i]).execute();
						
						if (result != null && result.wasSuccessful()) {
							try {
								size = Long.parseLong( (i > 0 ? oPatternSpaceSearch.split(result.getLine().trim())[2] : result.getLine()) );
								
							} catch (Throwable e) {
								result = null;
							}
							
							break;
						}
					}
					
					if (result == null || !result.wasSuccessful()) {
						FileStat stat = getDetails();
						
						if (stat != null) {
							size = stat.size();
						}
					}
				}
			}
			
			return size;
		}
	}
	
	/**
	 * Make this file executable and run it in the shell.
	 */
	public Result runInShell() {
		synchronized(mLock) {
			if (isFile() && changeAccess(-1, -1, 777)) {
				return mShell.execute(getAbsolutePath());
			}
			
			return null;
		}
	}
	
	/**
	 * Make this file executable and run it asynchronized in the shell.
	 * 
	 * @param listener
	 *     An {@link OnShellResultListener} which will receive the output
	 */
	public void runInShell(OnShellResultListener listener) {
		synchronized(mLock) {
			if (isFile() && changeAccess(-1, -1, 777)) {
				mShell.executeAsync(getAbsolutePath(), listener);
			}
		}
	}
	
	/**
	 * Reboot into recovery and run this file/package
	 * <br />
	 * This method will add a command file in /cache/recovery which will tell the recovery the location of this 
	 * package. The recovery will then run the package and then automatically reboot back into Android. 
	 * <br />
	 * Note that this will also work on ROM's that changes the cache location or device. The method will 
	 * locate the real internal cache partition, and it will also mount it at a second location 
	 * if it is not already mounted.
	 * 
	 * @param context
	 *     A {@link Context} that can be used together with the Android <code>REBOOT</code> permission 
	 *     to use the <code>PowerManager</code> to reboot into recovery. This can be set to NULL 
	 *     if you want to just use the <code>toolbox reboot</code> command, however do note that not all 
	 *     toolbox versions support this command. 
	 * 
	 * @param args
	 *     Arguments which will be parsed to the recovery package. 
	 *     Each argument equels one prop line.
	 *     <br />
	 *     Each prop line is added to /cache/recovery/rootfw.prop and named (argument[argument number] = [value]).
	 *     For an example, if first argument is "test", it will be written to rootfw.prop as (argument1 = test).
	 * 
	 * @return
	 *     <code>False if it failed</code>
	 */
	public Boolean runInRecovery(Context context, String... args) {
		if (isFile()) {
			String cacheLocation = "/cache";
			MountStat mountStat = mShell.getDisk(cacheLocation).getFsDetails();
			
			if (mountStat != null) {
				DiskStat diskStat = mShell.getDisk( mountStat.device() ).getDiskDetails();
				
				if (diskStat == null || !cacheLocation.equals(diskStat.location())) {
					if (diskStat == null) {
						mShell.getDisk("/").mount(new String[]{"rw"});
						cacheLocation = "/cache-int";
						
						if (!getFile(cacheLocation).createDirectory()) {
							return false;
							
						} else if (!mShell.getDisk(mountStat.device()).mount(cacheLocation)) {
							return false;
						}
						
						mShell.getDisk("/").mount(new String[]{"ro"});
						
					} else {
						cacheLocation = diskStat.location();
					}
				}
			}
			
			if (getFile(cacheLocation + "/recovery").createDirectory()) {
				if (getFile(cacheLocation + "/recovery/command").write("--update_package=" + getResolvedPath())) {
					if (args != null && args.length > 0) {
						String[] lines = new String[ args.length ];
						
						for (int i=0; i < args.length; i++) {
							lines[i] = "argument" + (i+1) + "=" + args[i];
						}
						
						if (!getFile(cacheLocation + "/recovery/rootfw.prop").write(lines)) {
							getFile(cacheLocation + "/recovery/command").remove(); 
							
							return false;
						}
					}
					
					if (mShell.getDevice().rebootRecovery(context)) {
						return true;
					}
					
					getFile(cacheLocation + "/recovery/command").remove();
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Extract data from an Android Assets Path (files located in /assets/) and add it to the current file location.
	 * If the file already exist, it will be overwritten. Otherwise the file will be created. 
	 * 
	 * @param context
	 *     An android Context object
	 *     
	 * @param asset
	 *     The assets path
	 * 
	 * @return
	 *     <code>True</code> on success, <code>False</code> otherwise
	 */
	public Boolean extractResource(Context context, String asset) {
		try {
			InputStream input = context.getAssets().open(asset);
			Boolean status = extractResource(input);
			input.close();
			
			return status;
			
		} catch(Throwable e) { return false; }
	}
	
	/**
	 * Extract data from an Android resource id (files located in /res/) and add it to the current file location.
	 * If the file already exist, it will be overwritten. Otherwise the file will be created. 
	 * 
	 * @param context
	 *     An android Context object
	 *     
	 * @param resourceid
	 *     The InputStream to read from
	 * 
	 * @return
	 *     <code>True</code> on success, <code>False</code> otherwise
	 */
	public Boolean extractResource(Context context, Integer resourceid) {
		try {
			InputStream input = context.getResources().openRawResource(resourceid);
			Boolean status = extractResource(input);
			input.close();
			
			return status;
			
		} catch(Throwable e) { return false; }
	}
	
	/**
	 * Extract data from an InputStream and add it to the current file location.
	 * If the file already exist, it will be overwritten. Otherwise the file will be created. 
	 *     
	 * @param resource
	 *     The InputStream to read from
	 * 
	 * @return
	 *     <code>True</code> on success, <code>False</code> otherwise
	 */
	public Boolean extractResource(InputStream resource) {
		synchronized(mLock) {
			if (!isDirectory()) {
				try {
					FileWriter writer = getFileWriter();
					
					if (writer != null) {
						byte[] buffer = new byte[1024];
						int loc = 0;
						
						while ((loc = resource.read(buffer)) > 0) {
							writer.write(buffer, 0, loc);
						}
						
						writer.close();
					}
					
				} catch (Throwable e) {
					Log.e(TAG, e.getMessage(), e);
				}
			}
			
			return false;
		}
	}
	
	/**
	 * Get a {@link FileWriter} pointing at this file
	 */
	public FileWriter getFileWriter() {
		if (isFile()) {
			try {
				return new FileWriter(mShell, getAbsolutePath(), false);
				
			} catch (Throwable e) {
				Log.e(TAG, e.getMessage(), e); 
			}
		}
		
		return null;
	}
	
	/**
	 * Get a {@link FileReader} pointing at this file
	 */
	public FileReader getFileReader() {
		if (isFile()) {
			try {
				return new FileReader(mShell, getAbsolutePath());
				
			} catch (Throwable e) {
				Log.e(TAG, e.getMessage(), e);
			}
		}
		
		return null;
	}
	
	/**
	 * @return
	 *     <code>True</code> if the file exists, <code>False</code> otherwise
	 */
	public Boolean exists() {
		synchronized(mLock) {
			if (mExistsLevel < 0) {
				mExistsLevel = 0;
				
				/*
				 * We cannot trust a false value, since restricted files will return false. 
				 * But we can trust a true value, so we only do a shell check on false return. 
				 */
				if (!mFile.exists()) {
					Attempts attempts = mShell.createAttempts("( %binary test -e '" + getAbsolutePath() + "' && echo true ) || ( %binary test ! -e '" + getAbsolutePath() + "' && echo false )");
					Result result = attempts.execute();
					
					if (result != null && result.wasSuccessful()) {
						mExistsLevel = "true".equals(result.getLine()) ? 1 : 0;
						
					} else {
						/*
						 * Some toolsbox version does not have the 'test' command.
						 * Instead we try 'ls' on the file and check for errors, not pretty but affective. 
						 */
						result = mShell.createAttempts("ls '" + getAbsolutePath() + "' > /dev/null 2>&1").execute();
						
						if (result != null && result.wasSuccessful()) {
							mExistsLevel = 1;
						}
					}
					
				} else {
					mExistsLevel = 1;
				}
			}
			
			return mExistsLevel > 0;
		}
	}
	
	/**
	 * @return
	 *     <code>True</code> if the file exists and if it is an folder, <code>False</code> otherwise
	 */
	public Boolean isDirectory() {
		synchronized (mLock) {
			if (mFolderLevel < 0) {
				mFolderLevel = 0;
				
				if (exists()) {
					/*
					 * If it exists, but is neither a file nor a directory, then we better do a shell check
					 */
					if (!mFile.isDirectory() && !mFile.isFile()) {
						Attempts attempts = mShell.createAttempts("( %binary test -d '" + getAbsolutePath() + "' && echo true ) || ( %binary test ! -d '" + getAbsolutePath() + "' && echo false )");
						Result result = attempts.execute();
						
						if (result != null && result.wasSuccessful()) {
							mFolderLevel = "true".equals(result.getLine()) ? 1 : 0;
							
						} else {
							/*
							 * A few toolbox versions does not include the 'test' command
							 */
							FileStat stat = getCanonicalFile().getDetails();
							
							if (stat != null) {
								mFolderLevel = "d".equals(stat.type()) ? 1 : 0;
							}
						}
						
					} else {
						mFolderLevel = mFile.isDirectory() ? 1 : 0;
					}
				}
			}
			
			return mFolderLevel > 0;
		}
	}
	
	/**
	 * @return
	 *     <code>True</code> if the file is a link, <code>False</code> otherwise
	 */
	public Boolean isLink() {
		synchronized (mLock) {
			if (mLinkLevel < 0) {
				mLinkLevel = 0;
				
				if (exists()) {
					Attempts attempts = mShell.createAttempts("( %binary test -L '" + getAbsolutePath() + "' && echo true ) || ( %binary test ! -L '" + getAbsolutePath() + "' && echo false )");
					Result result = attempts.execute();
					
					if (result != null && result.wasSuccessful()) {
						mLinkLevel = "true".equals(result.getLine()) ? 1 : 0;
						
					} else {
						/*
						 * A few toolbox versions does not include the 'test' command
						 */
						FileStat stat = getDetails();
						
						if (stat != null) {
							mLinkLevel = "l".equals(stat.type()) ? 1 : 0;
						}
					}
				}
			}
			
			return mLinkLevel > 0;
		}
	}
	
	
	/**
	 * @return
	 *     <code>True</code> if the item exists and if it is an file, <code>False</code> otherwise
	 */
	public Boolean isFile() {
		synchronized (mLock) {
			return exists() && !isDirectory();
		}
	}
	
	/**
	 * Returns the absolute path. An absolute path is a path that starts at a root of the file system.
	 * 
	 * @return
	 *     The absolute path
	 */
	public String getAbsolutePath() {
		return mFile.getAbsolutePath();
	}
	
	/**
	 * Returns the path used to create this object.
	 * 
	 * @return
	 *     The parsed path
	 */
	public String getPath() {
		return mFile.getPath();
	}
	
	/**
	 * Returns the parent path. Note that on folders, this means the parent folder. 
	 * However, on files, it will return the folder path that the file resides in.
	 * 
	 * @return
	 *     The parent path
	 */
	public String getParentPath() {
		return mFile.getParent();
	}
	
	/**
	 * Get a real absolute path. <br /><br />
	 * 
	 * Java's <code>getAbsolutePath</code> is not a fully resolved path. Something like <code>./file</code> could be returned as <code>/folder/folder2/.././file</code> or simular. 
	 * This method however will resolve a path and return a fully absolute path <code>/folder/file</code>. It is a bit slower, so only use it when this is a must. 
	 */
	public String getResolvedPath() {
		synchronized (mLock) {
			String path = getAbsolutePath();
			
			if (path.contains(".")) {
				String[] directories = ("/".equals(path) ? path : path.endsWith("/") ? path.substring(1, path.length() - 1) : path.substring(1)).split("/");
				List<String> resolved = new ArrayList<String>();
				
				for (int i=0; i < directories.length; i++) {
					if (directories[i].equals("..")) {
						if (resolved.size() > 0) {
							resolved.remove( resolved.size()-1 );
						}
						
					} else if (!directories[i].equals(".")) {
						resolved.add(directories[i]);
					}
				}
				
				path = resolved.size() > 0 ? "/" + TextUtils.join("/", resolved) : "/";
			}
			
			return path;
		}
	}
	
	/**
	 * Get the canonical path of this file or folder. 
	 * This means that if this is a link, you will get the path to the target, no matter how many links are in between.
	 * It also means that things like <code>/folder1/../folder2</code> will be resolved to <code>/folder2</code>.
	 * 
	 * @return
	 *     The canonical path
	 */
	public String getCanonicalPath() {
		synchronized (mLock) {
			if (exists()) {
				try {
					/*
					 * First let's try using the native tools 
					 */
					String canonical = mFile.getCanonicalPath();
					
					if (canonical != null) {
						return canonical;
					}
					
				} catch(Throwable e) {}
				
				/*
				 * Second we try using readlink, if the first failed. 
				 */
				Result result = mShell.createAttempts("readlink -f '" + getAbsolutePath() + "' 2> /dev/null").execute();
				
				if (result.wasSuccessful()) {
					return result.getLine();
					
				} else {
					/*
					 * And third we fallback to a slower but affective method
					 */
					FileStat stat = getDetails();
					
					if (stat != null && stat.link() != null) {
						String realPath = stat.link();
						
						while ((stat = getFile(realPath).getDetails()) != null && stat.link() != null) {
							realPath = stat.link();
						}
						
						return realPath;
					}
					
					return getAbsolutePath();
				}
			}
			
			return null;
		}
	}
	
	/**
	 * Open a new {@link File} object pointed at another file.
	 * 
	 * @param fileName
	 *     The file to point at
	 * 
	 * @return
	 *     A new instance of this class representing another file
	 */
	public File getFile(String file) {
		return new File(mShell, file);
	}
	
	/**
	 * Open a new {@link File} object with the parent of this file.
	 * 
	 * @return
	 *     A new instance of this class representing the parent directory
	 */
	public File getParentFile() {
		return new File(mShell, getParentPath());
	}
	
	/**
	 * If this is a link, this method will return a new {@link File} object with the real path attached. 
	 * 
	 * @return
	 *     A new instance of this class representing the real path of a possible link
	 */
	public File getCanonicalFile() {
		return new File(mShell, getCanonicalPath());
	}
	
	/**
	 * @return
	 *     The name of the file
	 */
	public String getName() {
		return mFile.getName();
	}
}
