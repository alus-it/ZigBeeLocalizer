//=======================================================================================
// This file is part of: ZigBee Localizer - LocationService
// A localization system for ZigBee networks, based on the analysis of RSSI.
// It estimates the positions of the mobile nodes. This Java program produces
// results in a accessible webservlet where the user can manage the WSN network.
//
// Author         : Alberto Realis-Luc <alberto.realisluc@gmail.com>
// Since          : May 2008
// Web            : http://www.alus.it/pubs/LocationService/
// Git Repository : https://github.com/alus-it/ZigBeeLocalizer.git
// Version        : 1.0
// Copyright      : © 2008-2009 Alberto Realis-Luc
// License        : GPL
// Last change    : September 2009
//=======================================================================================

package it.ismb.locationService;

import java.util.Calendar;

public class Converter {
	public static String Vector2String(byte[] datapayload) {
		String s = "";
		for (int i = 0; i < datapayload.length; i++) {
			String a = "" ;
			if (datapayload[i] <= 0xf) a = "0";
			s += "0x" + a + Long.toHexString(datapayload[i] & 0xff) + " ";
		}
		return s;
	}

	public static String Vector2HexString(int[] datapayload) {
		String s = "";
		for (int i = 0; i < datapayload.length; i++) {
			String a = "";
			if (datapayload[i] <= 0xf) a = "0";
			s += "0x" + a + Long.toHexString(datapayload[i]) + " ";
		}
		return s;
	}

	public static String Vector2IntString(int[] datapayload) {
		String s = "";
		for(int i=0;i<datapayload.length; i++) s+=Integer.toString(datapayload[i])+" ";
		return s;
	}
	
	private static char toHexChar(int i) {
		if(0<=i && i<=9) return (char)('0'+i);
		else return (char)('a'+(i-10));
	}

	private static String byteToHex(byte data) {
		StringBuffer buf = new StringBuffer();
		buf.append(toHexChar((data>>>4)&0x0F));
		buf.append(toHexChar(data&0x0F));
		return buf.toString();
	}


	public static String Vector2HexString(byte[] datapayload) {
		String s = "";
		for(int i=0;i<datapayload.length; i++) s+="0x"+byteToHex(datapayload[i])+" ";
		return s;

	}

	public static String Vector2HexStringNoSpaces(int[] datapayload) {
		String s = "";
		for (int i=0; i<datapayload.length; i++) s+=""+byteToHex(datapayload[i]);
		return s;
	}

	private static String byteToHex(int data) {
		StringBuffer buf = new StringBuffer();
		buf.append(toHexChar((data>>>4)&0x0F));
		buf.append(toHexChar(data&0x0F));
		return buf.toString();
	}

	public static int Decode8U(byte[] rawpayload, int i) {
		return (rawpayload[i] & 0xff);
	}

	public static double Decode32U(byte[] rawpayload, int startindex) {
		int hi1, hi2, hi3, hi4; // little endian
		hi4 = (0x000000FF & ((int) rawpayload[startindex]));
		hi3 = (0x000000FF & ((int) rawpayload[startindex + 1]));
		hi2 = (0x000000FF & ((int) rawpayload[startindex + 2]));
		hi1 = (0x000000FF & ((int) rawpayload[startindex + 3]));
		long anUnsignedInt = ((long) (hi1 << 24 | hi2 << 16 | hi3 << 8 | hi4)) & 0xFFFFFFFFL;
		return anUnsignedInt;
	}

	public static byte[] DecodeVector8U(byte[] rawpayload, int start,
			int datalen) {
		byte[] ret = new byte[datalen];
		for(int i=0;i<datalen;i++) ret[i]=rawpayload[start+i];
		return ret;
	}

	public static int[] DecodeVector16U(byte[] rawpayload,int startoffset_bytes,int datalen_bytes) {
		int hi;
		int low;
		int[] ret = new int[datalen_bytes / 2];
		for (int i = 0; i < (datalen_bytes - 1); i += 2) {
			hi = 0x000000FF & rawpayload[startoffset_bytes + i];
			low = 0x000000FF & rawpayload[startoffset_bytes + i + 1];
			ret[i / 2] = (int) ((low << 8 | hi) & 0xFFFFFFFFL);
		}
		return ret;
	}

	public static byte[] subarray(byte[] packet, int start, int stop) {
		byte[] ret = new byte[stop - start + 1];
		for (int i=start;i<=stop;i++) ret[i-start]=packet[i];
		return ret; //Start and stop positions are included
	}

	public static int Decode8(byte[] payload, int i) {
		int val = Decode8U(payload, i);
		if(val<=127) return val;
		 else return val-256;
	}


	public static double Decode32(byte[] payload, int startindex) {
		int hi1, hi2, hi3, hi4;
		boolean isnegative = false;
		hi4 = (0x000000FF & ((int) payload[startindex]));
		hi3 = (0x000000FF & ((int) payload[startindex + 1]));
		hi2 = (0x000000FF & ((int) payload[startindex + 2]));
		hi1 = (0x000000FF & ((int) payload[startindex + 3]));
		if((hi1 & 0x80)!= 0) isnegative = true;	
		hi1 = hi1 & (~0x80);
		long anUnsignedInt = ((long) (hi1 << 24 | hi2 << 16 | hi3 << 8 | hi4)) & 0xFFFFFFFFL;
		if(isnegative) anUnsignedInt = -(0x7fffffff - anUnsignedInt + 1);
		return anUnsignedInt;
	}

	public static int Decode16U(byte[] rawpayload, int startindex) {
		int hi3, hi4;
		hi4 = (0x000000FF & ((int) rawpayload[startindex]));
		hi3 = (0x000000FF & ((int) rawpayload[startindex + 1]));
		int anUnsignedInt = (int) ((hi3 << 8 | hi4) & 0xFFFFFFFFL);
		return anUnsignedInt;
	}
	
	public static int Decode16U(byte[] rawpayload, int startindex, boolean bigendian) {
		byte[] mybuffer = new byte[2];
		if (bigendian) {
			mybuffer[0] = rawpayload[startindex];
			mybuffer[1] = rawpayload[startindex + 1];
		} else {
			mybuffer[1] = rawpayload[startindex];
			mybuffer[0] = rawpayload[startindex + 1];
		}
		return Decode16U(mybuffer, 0);
	}

	public static double Decode32U(byte[] rawpayload, int startindex,
			boolean bigendian) {
		byte[] mybuffer = new byte[4];

		if (bigendian) {
			mybuffer[3] = rawpayload[startindex + 3];
			mybuffer[2] = rawpayload[startindex + 2];
			mybuffer[1] = rawpayload[startindex + 1];
			mybuffer[0] = rawpayload[startindex];

		} else {
			mybuffer[0] = rawpayload[startindex + 3];
			mybuffer[1] = rawpayload[startindex + 2];
			mybuffer[2] = rawpayload[startindex + 1];
			mybuffer[3] = rawpayload[startindex];
		}

		return Decode32U(mybuffer, 0);

	}

	public static byte[] Encode16U(int value, boolean bigendian) {
		byte[] ret = new byte[2];
		byte byte_h = (byte) ((value >> 8) & 0xff);
		byte byte_l = (byte) (value & 0xff);
		if (bigendian) {
			ret[0] = byte_l;
			ret[1] = byte_h;
		} else {
			ret[0] = byte_h;
			ret[1] = byte_l;
		}
		return ret;
	}

	public static void Encode16U(byte[] resultdestination, int resultindex,
			int value, boolean bigendian) {
		byte[] enc = Encode16U(value, bigendian);
		resultdestination[resultindex] = enc[0];
		resultdestination[resultindex + 1] = enc[1];
	}

	/**
	 * Compares two {@link Float} numbers, wrapped into {@link String}s.
	 * Normally used when checking file versions.
	 * 
	 * @param f1
	 *            The first number to compare
	 * @param f2
	 *            The second number to compare
	 * @return the value 0 if f1 is numerically equal to f2; a value less than 0
	 *         if f1 is numerically less than f2; and a value greater than 0 if
	 *         f1 is numerically greater than f2.
	 * @see Float
	 * @throws NumberFormatException
	 *             If a numeric conversion error occurred. The message of this
	 *             exception is <code>"first"</code> or <code>"second"</code>,
	 *             depending on which String caused the error. The first string
	 *             is checked before the second one.
	 */
	public static int floatCompare(String f1, String f2)
	throws NumberFormatException {
		Float float1, float2;
		try {
			float1 = Float.valueOf(f1);
		} catch (NumberFormatException e) {
			throw new NumberFormatException("first");
		}
		try {
			float2 = Float.valueOf(f2);
		} catch (NumberFormatException e) {
			throw new NumberFormatException("second");
		}

		return Float.compare(float1, float2);
	}

	/**
	 * Translates a hex number into a byte array.
	 * 
	 * @param hexNumber
	 *            A {@link String} representing the hex value to convert. This
	 *            parameter must contain only hex-mappable data, so, for
	 *            istance, a leading '0x' or '0X' should not be present.
	 * @return The converted number or <i>null</i> if some error occurred.
	 */
	public static byte[] hex2byte(String hexNumber) {
		if (hexNumber == null || hexNumber.length() % 2 != 0)
			return null;

		byte[] byteArray = new byte[hexNumber.length() / 2];

		try {
			for (int i = 0; i < byteArray.length; i++)
				byteArray[i] = (byte) Integer.parseInt(hexNumber.substring(
						2 * i, 2 * i + 2), 16);
		} catch (NumberFormatException e) {
			e.printStackTrace();
			System.out.println("hex2byte received an invalid hex number: "+ hexNumber);
			return null;
		}

		return byteArray;
	}

	/**
	 * Gives the hex representation of the provided byte
	 * 
	 * @param b
	 *            The byte to represent
	 * @return The representation
	 */
	public static String byte2hex(byte b) {
		int lower, upper;

		lower = (b + 256) % 16;
		b = (byte) (b >> 4);
		upper = (b + 256) % 16;

		return new StringBuilder().append(Integer.toHexString(upper)).append(
				Integer.toHexString(lower)).toString();
	}

	/**
	 * Gives the hex representation of the provided array of bytes
	 * 
	 * @param b
	 *            The array of bytes to convert
	 * @return The desired hex representation
	 */
	public static String byte2hex(byte b[]) {
		return byte2hex(b, "");
	}

	/**
	 * Gives the hex representation of the provided array of bytes, using the
	 * provided separator between bytes
	 * 
	 * @param b
	 *            The array of bytes to convert
	 * @param separator
	 *            An optional separator that should be inserted between bytes
	 * @return The desired hex representation
	 */
	public static String byte2hex(byte b[], String separator) {
		StringBuilder buf = new StringBuilder();

		for (byte n : b)
			buf.append(byte2hex(n) + separator);

		return buf.toString();
	}

	/**
	 * Translates a byte array into the integer value it represents. This method
	 * supports:
	 * <ul>
	 * <li><b>unsigned integers</b> in RAW binary form</li>
	 * <li><b>signed integers</b> in two's complement</li>
	 * </ul>
	 * 
	 * @param dataArray
	 *            The source byte array
	 * @param bigEndian
	 *            Whether the integer value is encoded in big or little endian
	 * @param signed
	 *            Whether the encoded integer is signed or not
	 * @return The encoded integer
	 * @deprecated Whenever possible and if that does not cause further
	 *             problems, please use the method
	 *             {@link #byte2long(byte[], boolean, boolean)} as it correctly
	 *             handles large values. This method is a wrapper around it,
	 *             anyway.
	 */
	public static int byte2int(byte[] dataArray, boolean bigEndian, boolean signed) {
		return (int) byte2long(dataArray, bigEndian, signed);
	}
	
	public static int byte2int(byte b, boolean signed) {
		if(signed) return b;
		else return(b >= 0 ? b : b + 256);
	}

	/**
	 * Translates a byte array into the integer value it represents. This method
	 * supports:
	 * <ul>
	 * <li><b>unsigned integers</b> in RAW binary form</li>
	 * <li><b>signed integers</b> in two's complement</li>
	 * </ul>
	 * 
	 * @param dataArray
	 *            The source byte array
	 * @param bigEndian
	 *            Whether the integer value is encoded in big or little endian
	 * @param signed
	 *            Whether the encoded integer is signed or not
	 * @return The encoded integer
	 */
	public static long byte2long(byte[] dataArray, boolean bigEndian,
			boolean signed) {
		long toReturn;

		if (bigEndian) {
			if (signed)
				toReturn = dataArray[0];
			else
				toReturn = (dataArray[0] >= 0 ? dataArray[0]
				                                          : dataArray[0] + 256);

			for (int i = 1; i < dataArray.length; i++)
				toReturn = 256
				* toReturn
				+ (dataArray[i] >= 0 ? dataArray[i]
				                                 : dataArray[i] + 256);

		} else {
			if (signed)
				toReturn = dataArray[dataArray.length - 1];
			else
				toReturn = (dataArray[dataArray.length - 1] >= 0 ? dataArray[dataArray.length - 1]
				                                                             : dataArray[dataArray.length - 1] - Byte.MIN_VALUE);

			for (int i = dataArray.length - 2; i >= 0; i--)
				toReturn = 256
				* toReturn
				+ (dataArray[i] >= 0 ? dataArray[i] : dataArray[i]
				                                                - Byte.MIN_VALUE);
		}

		return toReturn;
	}

	/**
	 * Computes a bit-to-bit masking between the provided mask and the data
	 * extracted from the provided array
	 * 
	 * @param mask
	 *            The mask to use in the computation; it defines the length of
	 *            the result
	 * @param data
	 *            The array from which fetch the data
	 * @param offset
	 *            The offset at which the method starts to fetch the data from
	 *            the provided array
	 * @return A array (long as the mask is) with the result of the bit-to-bit
	 *         masking
	 */
	public static byte[] maskData(byte[] mask, byte[] data, int offset) {
		byte[] masked = new byte[mask.length];

		//I fear there's something wrong with this...
		for (int i = 0; i < mask.length; i++)
			masked[i] = (byte) (mask[i] & data[offset + i]);

		return masked;
	}

	/**
	 * Returns the provided number as {@link String}, adding leading zeros if
	 * necessary.
	 * 
	 * @param number
	 *            The integer to format
	 * @param minimumDigits
	 *            The minimum number of digits that the String representation
	 *            should have
	 * @return A {@link String} representation of the number, following the
	 *         provided rules.
	 */
	public static String addLeadingZeros(int number, int minimumDigits) {
		StringBuilder numAsStr = new StringBuilder().append(number);

		if (numAsStr.length() >= minimumDigits)
			return numAsStr.toString();

		StringBuilder toReturn = new StringBuilder();
		for (int i = 0; i < minimumDigits - numAsStr.length(); i++)
			toReturn.append('0');

		return toReturn.append(numAsStr).toString();
	}

	public static void Encode8U(byte[] datapayload, int i, int value) {
		datapayload[i] = (byte) value;
	}

	public static void Encode32U(byte[] resultdestination, int resultindex,
			int value, boolean bigendian) {
		byte[] enc = Encode32U(value, bigendian);
		resultdestination[resultindex] = enc[0];
		resultdestination[resultindex + 1] = enc[1];
		resultdestination[resultindex + 2] = enc[2];
		resultdestination[resultindex + 3] = enc[3];
	}

	private static byte[] Encode32U(int value, boolean bigendian) {
		byte[] ret = new byte[4];
		byte byte_hh = (byte) ((value >> 24) & 0xff);
		byte byte_h = (byte) ((value >> 16) & 0xff);
		byte byte_l = (byte) ((value >> 8) & 0xff);
		byte byte_ll = (byte) (value & 0xff);
		if (bigendian) {
			ret[0] = byte_hh;
			ret[1] = byte_h;
			ret[2] = byte_l;
			ret[3] = byte_ll;
		} else {
			ret[3] = byte_hh;
			ret[2] = byte_h;
			ret[1] = byte_l;
			ret[0] = byte_ll;
		}
		return ret;
	}

	public static byte[] EncodeDistance(double distMeter) {
		byte[] ret=new byte[2];
		if(distMeter<=0) {
			ret[0]=(byte) 0x00;
			ret[1]=(byte) 0x0;
		} else if(distMeter>=16384) {
			ret[0]=(byte) 0xff;
			ret[1]=(byte) 0xff;
		} else {
			int intvalue=(int)distMeter;
			distMeter=distMeter-intvalue;
			if(distMeter>0.875) distMeter=intvalue+1;
			else if(distMeter>0.625) distMeter=intvalue+0.75;
				else if(distMeter>0.375) distMeter=intvalue+0.5;
					else if(distMeter>0.125) distMeter=intvalue+0.25;
						else distMeter=intvalue;
			distMeter=distMeter*4;
			ret=Encode16U((int)distMeter,false);
		}
		return ret;
	}
	
	public static double DecodeDistance(byte[] rawByteSeq, int pos) {
		double dst=0;
		if(rawByteSeq.length-pos>0) {
			dst=Decode16U(rawByteSeq,pos,false);
			dst=dst/4;
		}
		return dst;
	}
	
	public static byte[] EncodeTime(double timeSec) {
		byte[] ret=new byte[2];
		if(timeSec<=0) {
			ret[0]=(byte) 0x00;
			ret[1]=(byte) 0x00;
		} else if(timeSec>=6553.6) {
			ret[0]=(byte) 0xff;
			ret[1]=(byte) 0xff;
		} else {
			int intvalue=(int)timeSec;
			timeSec=timeSec-intvalue;
			if(timeSec>0.95) timeSec=(intvalue+1);
			else if(timeSec>0.85) timeSec=(intvalue+0.9);
				else if(timeSec>0.75) timeSec=(intvalue+0.8);
					else if(timeSec>0.65) timeSec=(intvalue+0.7);
						else if(timeSec>0.55) timeSec=(intvalue+0.6);
							else if(timeSec>0.45) timeSec=(intvalue+0.5);
								else if(timeSec>0.35) timeSec=(intvalue+0.4);
									else if(timeSec>0.25) timeSec=(intvalue+0.3);
										else if(timeSec>0.15) timeSec=(intvalue+0.2);
											else if(timeSec>0.05) timeSec=(intvalue+0.1);
												else timeSec=intvalue;
			ret=Encode16U((int)(timeSec*10),false);
		}
		return ret;
	}
	
	public static int[] DecodeIEEEaddr(byte[] rawByteSeq, int pos) {
		int[] ieeeaddr = new int[8];
		for(int i=0, j=7;i<8;i++,j--) ieeeaddr[i]=Decode8(rawByteSeq,pos+j);
		return ieeeaddr;
	}
	
	public static int[] DecodeIEEEaddr(String rawString) {
		int[] ieeeaddr = new int[8];
		for(int i=0;i<8;i++) ieeeaddr[i]=Integer.parseInt(rawString.substring(i*2,i*2+2),16);
		return ieeeaddr;
	}
		
	public static String TimeMillisToString(long timestamp) {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(timestamp);
		return(String.format("%1$ta %1$te/%1$tb/%1$tY %1$tH:%1$tM:%1$tS.%1$tL",cal));
	}
	
	public static String TimeCalendarToString(Calendar timeCal) {
		return(String.format("%1$ta %1$te/%1$tb/%1$tY %1$tH:%1$tM:%1$tS.%1$tL",timeCal));
	}
	
	public static Calendar TimeMillisToCalendar(long timestamp) {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(timestamp);
		return(cal);
	}
	
	public static long DateTimeToMillis(int day, int month, int year, int hour, int min, int sec, int millis) {
		Calendar cal = Calendar.getInstance();
		cal.set(year,month-1,day,hour,min,sec);
		cal.set(Calendar.MILLISECOND,millis);
		return(cal.getTimeInMillis());
	}

	public static boolean compareIEEEaddr(int[] IEEEaddr1, int[] IEEEaddr2) {
		if(IEEEaddr1.length!=8 || IEEEaddr1.length!=8) return false;
		else {
			boolean ret=true;
			for(int i=0;i<8 && ret==true;i++) if(IEEEaddr1[i]!=IEEEaddr2[i]) ret=false;
			return ret;
		}
	}

}
