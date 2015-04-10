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

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.CharBuffer;

import com.spazedog.lib.rootfw4.Common;
import com.spazedog.lib.rootfw4.Shell;
import com.spazedog.lib.rootfw4.ShellStream;

/**
 * This class allows you to open a file as root, if needed. 
 * Files that are not protected will be handled by a regular {@link java.io.FileReader} while protected files 
 * will use a shell streamer instead. Both of which will act as a normal reader that can be used together with other classes like {@link BufferedReader} and such. <br /><br />
 * 
 * Note that this should not be used for unending streams. This is only meant for regular files. If you need unending streams, like <code>/dev/input/event*</code>, 
 * you should use {@link ShellStream} instead. 
 */
public class FileReader extends Reader {
	public static final String TAG = Common.TAG + ".FileReader";

	protected InputStreamReader mStream;
	
	/**
	 * Create a new {@link InputStreamReader}. However {@link FileReader#FileReader(Shell, String)} is a better option.
	 */
	public FileReader(String file) throws FileNotFoundException {
		this(null, file);
	}
	
	/**
	 * Create a new {@link InputStreamReader}. If <code>shell</code> is not <code>NULL</code>, then
	 * the best match for <code>cat</code> will be located whenever a SuperUser connection is needed. This will be the best 
	 * option for multiple environments. 
	 */
	public FileReader(Shell shell, String file) throws FileNotFoundException {
		String filePath = new File(file).getAbsolutePath();
		
		try {
			mStream = new InputStreamReader(new FileInputStream(filePath));
			
		} catch (FileNotFoundException e) {
			String binary = shell != null ? shell.findCommand("cat") : "toolbox cat";
			
			try {
				ProcessBuilder builder = new ProcessBuilder("su");
				builder.redirectErrorStream(true);
				
				Process process = builder.start();
				mStream = new InputStreamReader(process.getInputStream());
				
				DataOutputStream stdIn = new DataOutputStream(process.getOutputStream());
				stdIn.write( (binary + " '" + filePath + "'\n").getBytes() );
				stdIn.write( ("exit $?\n").getBytes() );
				stdIn.flush();
				stdIn.close();
				
				Integer resultCode = process.waitFor();
				
				if (!resultCode.equals(0)) {
					throw new FileNotFoundException(e.getMessage());
				}
				
			} catch (Throwable te) {
				throw new FileNotFoundException(te.getMessage());
			}
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void mark(int readLimit) throws IOException {
		mStream.mark(readLimit);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean markSupported() {
		return mStream.markSupported();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void close() throws IOException {
		mStream.close();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int read(char[] buffer, int offset, int count) throws IOException {
		return mStream.read(buffer, offset, count);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public int read(CharBuffer target) throws IOException {
		return mStream.read(target);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public int read(char[] buffer) throws IOException {
		return mStream.read(buffer);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public int read() throws IOException {
		return mStream.read();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public long skip(long charCount) throws IOException {
		return mStream.skip(charCount);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void reset() throws IOException {
		mStream.reset();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean ready() throws IOException {
		return mStream.ready();
	}
}
