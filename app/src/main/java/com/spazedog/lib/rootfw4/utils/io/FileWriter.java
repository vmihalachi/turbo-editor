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

package com.spazedog.lib.rootfw4.utils.io;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Writer;

import android.os.Bundle;

import com.spazedog.lib.rootfw4.Common;
import com.spazedog.lib.rootfw4.Shell;

/**
 * This class is used to write to a file. Unlike {@link java.io.FileWriter}, this class 
 * will fallback on a SuperUser shell stream whenever a write action is not allowed by the application. 
 */
public class FileWriter extends Writer {
	public static final String TAG = Common.TAG + ".FileReader";
	
	protected DataOutputStream mStream;
	protected Process mProcess;
	
	public FileWriter(String file) throws IOException {
		this(null, file, false);
	}
	
	public FileWriter(String file, boolean append) throws IOException {
		this(null, file, append);
	}
	
	public FileWriter(Shell shell, String file, boolean append) throws IOException {
		super();
		
		String filePath = new File(file).getAbsolutePath();
		
		try {
			mStream = new DataOutputStream(new FileOutputStream(filePath, append));
			
		} catch (IOException e) {
			String binary = shell != null ? shell.findCommand("cat") : "toolbox cat";
			
			try {
				mProcess = new ProcessBuilder("su").start();
				mStream = new DataOutputStream(mProcess.getOutputStream());
				
				mStream.write( (binary + (append ? " >> " : " > ") + "'" + filePath + "' || exit 1\n").getBytes() );
				mStream.flush();
				
				try {
					synchronized(mStream) {
						/*
						 * The only way to check for errors, is by giving the shell a bit of time to fail. 
						 * This can either be an error caused by a missing binary for 'cat', or caused by something 
						 * like writing to a read-only fileystem. 
						 */
						mStream.wait(100);
					}
					
				} catch (Throwable ignore) {}
				
				try {
					if (mProcess.exitValue() == 1) {
						throw new IOException(e.getMessage());
					}
					
				} catch (IllegalThreadStateException ignore) {}
				
			} catch (Throwable te) {
				throw new IOException(te.getMessage());
			}
		}
		
		Bundle bundle = new Bundle();
		bundle.putString("action", "exists");
		bundle.putString("location", filePath);
		
		Shell.sendBroadcast("file", bundle);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void close() throws IOException {
		mStream.flush();
		mStream.close();
		mStream = null;
		
		if (mProcess != null) {
			mProcess.destroy();
			mProcess = null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void flush() throws IOException {
		mStream.flush();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void write(char[] buf, int offset, int count) throws IOException {
		synchronized(lock) {
			byte[] bytes = new byte[buf.length];
			
			for (int i=0; i < bytes.length; i++) {
				bytes[i] = (byte) buf[i];
			}
			
			mStream.write(bytes, offset, count);
		}
	}
	
	public void write(byte[] buf, int offset, int count) throws IOException {
		synchronized(lock) {
			mStream.write(buf, offset, count);
		}
	}
	
	public void write(byte[] buf) throws IOException {
		synchronized(lock) {
			mStream.write(buf);
		}
	}
}
