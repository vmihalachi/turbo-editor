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

package com.spazedog.lib.rootfw4;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import android.os.Build;
import android.os.Process;

public class Common {
	private static Boolean oEmulator = false;
	
	public static final String TAG = Common.class.getPackage().getName();
	public static Boolean DEBUG = true;
	public static String[] BINARIES = new String[]{null, "busybox", "toolbox"};
	public static Map<String, Integer> UIDS = new HashMap<String, Integer>();
	public static Map<Integer, String> UNAMES = new HashMap<Integer, String>();
	
	static {
		UIDS.put("root", 0);
		UIDS.put("system", 1000);
		UIDS.put("radio", 1001);
		UIDS.put("bluetooth", 1002);
		UIDS.put("graphics", 1003);
		UIDS.put("input", 1004);
		UIDS.put("audio", 1005);
		UIDS.put("camera", 1006);
		UIDS.put("log", 1007);
		UIDS.put("compass", 1008);
		UIDS.put("mount", 1009);
		UIDS.put("wifi", 1010);
		UIDS.put("adb", 1011);
		UIDS.put("install", 1012);
		UIDS.put("media", 1013);
		UIDS.put("dhcp", 1014);
		UIDS.put("shell", 2000);
		UIDS.put("cache", 2001);
		UIDS.put("diag", 2002);
		UIDS.put("net_bt_admin", 3001);
		UIDS.put("net_bt", 3002);
		UIDS.put("inet", 3003);
		UIDS.put("net_raw", 3004);
		UIDS.put("misc", 9998);
		UIDS.put("nobody", 9999);
		
		for (Entry<String, Integer> entry : UIDS.entrySet()) {
			UNAMES.put(entry.getValue(), entry.getKey());
		}
		
		oEmulator = Build.BRAND.equalsIgnoreCase("generic") || 
				Build.MODEL.contains("google_sdk") || 
				Build.MODEL.contains("Emulator") || 
				Build.MODEL.contains("Android SDK");
	}
	
	/**
	 * Check if the current device is an emulator 
	 */
	public static Boolean isEmulator() {
		return oEmulator;
	}
	
	public static Integer getUID(String name) {
		if (name != null) {
			if (name.matches("^[0-9]+$")) {
				return Integer.parseInt(name);
			}
			
			if (UIDS.containsKey(name)) {
				return UIDS.get(name);
				
			} else if (name.startsWith("u")) {
				Integer sep = name.indexOf("_");
	
				if (sep > 0) {
					Integer uid = Integer.parseInt( name.substring(1, sep) );
					Integer gid = Integer.parseInt( name.substring(sep+2) );
					
					return uid * 100000 + ((Process.FIRST_APPLICATION_UID + gid) % 100000);
				}
			}
		}
		
		return -10000;
	}
	
	public static String getUIDName(Integer id) {
		if (UNAMES.containsKey(id)) {
			return UNAMES.get(id);
			
		} else if (id >= 10000) {
			Integer uid = id / 100000;
			Integer gid = id % Process.FIRST_APPLICATION_UID;

			return "u" + uid + "_a" + gid + "";
		}
		
		return null;
	}
}
