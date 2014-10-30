/* All code below is 
 * 
 * Copyright (C) 2012 Kieran White
 * All rights reserved
 * 
 * Except for the method isSymLink which is 
 * licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this function except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hourglassapps.util;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;

public class Rtu {
	private final static String TAG="com.hourglassapps.Rtu";
	
	public static boolean RELEASE=true;
	public final static String ID_COL_NAME="_id";
	
	public static String[] EMPTY_STRING_ARRAY=new String[]{};

    public static String safeToString(Object pObj) {
    	if(pObj==null) {
    		return null;
    	}
    	return pObj.toString();
    }
    
	public static <T> boolean typeSafeEq(T o1, T o2) {
		return Rtu.safeEq(o1, o2);
	}
	
	public static boolean safeEq(Object o1, Object o2) {
		if(o1==o2) {
			return true;
		}
		if(o1==null || o2==null) {
			return false;
		}
		return o1.equals(o2);
	}
	
	public static int safeStrCmp(String s1, String s2, boolean ignoreCase) {
		if(s1==s2) {
			return 0;
		}
		if(s1==null) {
			return -1;
		}
		if(s2==null) {
			return +1;
		}
		
		if(ignoreCase) {
			return s1.compareToIgnoreCase(s2);
		} else {
			return s1.compareTo(s2);				
		}
	}

	public static <I> int safeCmp(Comparable<I> s1, I s2) {
		if(s1==s2) {
			return 0;
		}
		if(s1==null) {
			return -1;
		}
		if(s2==null) {
			return +1;
		}
		
		return s1.compareTo(s2);				
	}

	public static File setExtension(File f, String ext) {
		ext="."+ext;
		String path=f.toString();
		File newF=new File(path);
		int extLen=ext.length();
		int startIdx=path.length()-extLen;
		if(!newF.exists() && !path.substring(startIdx>=0?startIdx:0, path.length()).equalsIgnoreCase(ext)) {
			newF=new File(path+ext);
			if(newF.exists()) {
				newF=new File(path);
			}
		}
		return newF;
	}
		
	public static void copyFile(FileInputStream src, FileOutputStream dst, boolean fast) throws IOException {
		if(fast) {
			FileChannel outChannel = dst.getChannel();
			FileChannel inChannel= src.getChannel();
			inChannel.transferTo(0, inChannel.size(), outChannel);
			//long size=inChannel.size();
			//ByteBuffer mapFile=inChannel.map(FileChannel.MapMode.READ_ONLY, 0, size);
			//outChannel.write(mapFile);

			inChannel.close();
			outChannel.close();
		} else 
		{
			copyFile(src, dst);
		}
	}

	public static void copyFile(InputStream src, OutputStream dst) throws IOException {
		byte block[]=new byte[32*1024];
		try {
			try {
				int read=src.read(block);
				while(read!=-1) {
					dst.write(block, 0, read);
					read=src.read(block);
				}
			} finally {
				//dst.close();
			}
		} finally {
			//src.close();
		}
	}
	/*
	public static byte[] copyFile(FileInputStream src, int size) throws IOException {
		byte block[]=new byte[size];
		try {
			src.read(block);
		} finally {
			src.close();
		}		
		return block;
	}
	
	public static byte[] copyFile(File src) throws IOException {
		long size=src.length();
		int sizeInt=(int)size;
		FileInputStream in=null;
		try {
			in=new FileInputStream(src);
			return copyFile(in, sizeInt);
		} finally {
			if(in!=null) {
				in.close();
			}
		}
		
	}*/

	public static void copyFile(byte[] src, File dest) throws IOException {
		BufferedOutputStream out=new BufferedOutputStream(new FileOutputStream(dest)); 
		try {
			out.write(src);
		} finally {
			out.close();
		}
	}
	
	public static void copyFile(File src, FileOutputStream dst) throws IOException {
		FileInputStream in=null;
		try {
			in=new FileInputStream(src);
			copyFile(in, dst);
		} finally {
			if(in!=null) {
				in.close();
			}
		}
		/*
		FileChannel inChannel = new FileInputStream(src).getChannel();
		FileChannel outChannel = dst.getChannel();
		try {
			inChannel.transferTo(0, inChannel.size(), outChannel);
		} finally {
			if (inChannel != null)
				inChannel.close();
			if (outChannel != null)
				outChannel.close();
		}
		*/
	}
	
	public static void copyFile(File src, File dst) throws IOException {
		FileOutputStream out=null;
		try {
			out=new FileOutputStream(dst);
			copyFile(src, out);		
		} finally {
			if(out!=null) {
				out.close();
			}
		}
	}
	
    public static boolean isSymlink(File file) throws IOException {
    	if (file == null)
    		throw new NullPointerException("File must not be null");
    	File canon;
    	if (file.getParent() == null) {
    		canon = file;
    	} else {
    		File canonDir = file.getParentFile().getCanonicalFile();
    		canon = new File(canonDir, file.getName());
    	}
    	return !canon.getCanonicalFile().equals(canon.getAbsoluteFile());
    }
    
    private final static int twoToTwenty=1024*1024;
    public static double bytes2MB(long bytes) {
    	return ((double)bytes)/(twoToTwenty);
    }

	private final static int LONG_PARSER_RADIX=10;
	private final static long LONG_PARSER_MAX=Long.MIN_VALUE/LONG_PARSER_RADIX;

	public static boolean isLong(String string) {
    	if (string == null) {
            return false;
        }
        int length = string.length(), offset = 0;
        if (length == 0) {
        	return false;
        }
        boolean negative = string.charAt(offset) == '-';
        if (negative && ++offset == length) {
            return false;
        }

        long result = 0;
        length = string.length();
        while (offset < length) {
            int digit = Character.digit(string.charAt(offset++), LONG_PARSER_RADIX);
            if (digit == -1) {
                return false;
            }
            if (LONG_PARSER_MAX > result) {
                return false;
            }
            long next = result * LONG_PARSER_RADIX - digit;
            if (next > result) {
                return false;
            }
            result = next;
        }
        if (!negative) {
            result = -result;
            if (result < 0) {
                return false;
            }
        }
        return true;
    }

	public static String convertStreamToString(java.io.InputStream is) {
	    java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
	    return s.hasNext() ? s.next() : "";
	}
	
	public static Integer[] box(int[] array) {
		Integer[] result = new Integer[array.length];
		for (int i = 0; i < array.length; i++)
			result[i] = array[i];
		return result;
	}

	public static Byte[] box(byte[] array) {
		Byte[] result = new Byte[array.length];
		for (int i = 0; i < array.length; i++)
			result[i] = array[i];
		return result;
	}
	
	public static String uuidV4() {
		return java.util.UUID.randomUUID().toString();
	}
	
	public static String strippedUuidV4() {
		return Rtu.uuidV4().replaceAll("-","");
	}

	public static String join(List<String> pStrings, String pJoint) {
		if(pStrings.size()==0) {
			return "";
		}
		StringBuilder stringsJoined=new StringBuilder();
		assert(pStrings.get(0)!=null);
		stringsJoined.append(pStrings.get(0));
		for(String term: pStrings.subList(1, pStrings.size())) {
			assert(term!=null);
			stringsJoined.append(pJoint).append(term);
		}
		return stringsJoined.toString();
	}
}
