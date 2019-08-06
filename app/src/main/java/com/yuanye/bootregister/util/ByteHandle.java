package com.yuanye.bootregister.util;

import java.io.ByteArrayOutputStream;

import android.annotation.SuppressLint;

public class ByteHandle {

	public static byte[] byteConnection(byte[] arg1, byte[] arg2) {
		byte[] newbyte = new byte[arg1.length + arg2.length];
		System.arraycopy(arg1, 0, newbyte, 0, arg1.length);
		System.arraycopy(arg2, 0, newbyte, arg1.length, arg2.length);
		return newbyte;
	}

	public static String bytesToHexString(byte[] src) {
		StringBuilder stringBuilder = new StringBuilder("");
		if (src == null || src.length <= 0) {
			return null;
		}
		for (int i = 0; i < src.length; i++) {
			int v = src[i] & 0xFF;
			String hv = Integer.toHexString(v);
			if (hv.length() < 2) {
				stringBuilder.append(0);
			}
			stringBuilder.append(hv);
		}
		return stringBuilder.toString();
	}

	@SuppressLint("DefaultLocale")
	public static byte[] hexStringToBytes(String hexString) {
		if (hexString == null || hexString.equals("")) {
			return null;
		}
		hexString = hexString.toUpperCase();
		int length = hexString.length() / 2;
		char[] hexChars = hexString.toCharArray();
		byte[] d = new byte[length];
		for (int i = 0; i < length; i++) {
			int pos = i * 2;
			d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
		}
		return d;
	}

	private static byte charToByte(char c) {
		return (byte) "0123456789ABCDEF".indexOf(c);
	}

	public static String byteToHexString(byte b) {
		int v = b & 0xFF;
		String hv = Integer.toHexString(v);
		if (hv.length() < 2) {
			hv = "0" + hv;
		}
		return hv;
	}
	
	public static int byte2int(byte b){
		return b & 0xFF;
	}
	
	//10 -> "0a", n < 256
	public static String intToHexString(int n) {
		String str = Integer.toHexString(n);
		int len = str.length();
		if (len == 1)
			return "0" + str;
		else
			return str.substring(len - 2, len);
	}
	
	//{0x12,0x34} -> 0x1234
	public static int n2int(byte[] n){
		if (n == null)
			return 0;
		int data;
		if (n.length >= 4) {
			data = (n[3] & 0xff) | ((n[2] << 8) & 0xff00)
					| ((n[1] << 24) >>> 8) | (n[0] << 24);
		} else if (n.length == 3) {
			data = (n[2] & 0xff) | ((n[1] << 8) & 0xff00)
					| ((n[0] << 24) >>> 8);
		} else if (n.length == 2) {
			data = (n[1] & 0xff) | ((n[0] << 8) & 0xff00);
		} else if (n.length == 1) {
			data = (n[0] & 0xff);
		} else {
			data = 0;
		}
		return data;
	}
	
	//0x1234 -> {0x12,0x34}
	public static byte[] int2n(int value){
		byte[] targets = new byte[4];
		targets[0] = (byte) (value >> 24);
		targets[1] = (byte) ((value >> 16) & 0xff);
		targets[2] = (byte) ((value >> 8) & 0xff);
		targets[3] = (byte) (value & 0xff);
		if (value <= 0xff)
			return new byte[] { targets[3] };
		else if (value > 0xff && value <= 0xffff) {
			return new byte[] { targets[2], targets[3] };
		} else if (value > 0xffff && value <= 0xffffff) {
			return new byte[] { targets[1], targets[2], targets[3] };
		} else {
			return targets;
		}
	}
	
	// {0x12,0x34,0x56,0x78} -> "12345678"
	public static String cn2str(byte[] cn) {
		if(cn == null || cn.length == 0)
			return null;
		StringBuffer temp = new StringBuffer(cn.length * 2);

		for (int i = 0; i < cn.length; i++) {
			temp.append((byte) ((cn[i] & 0xf0) >>> 4));
			temp.append((byte) (cn[i] & 0x0f));
		}
		return temp.toString().substring(0, 1).equalsIgnoreCase("0") ? temp
				.toString().substring(1) : temp.toString();
	}

	// "12345678" -> {0x12,0x34,0x56,0x78}
	public static byte[] str2cn(String str) {
		if(str == null || str.length() == 0)
			return null;
		int len = str.length();
		int mod = len % 2;

		if (mod != 0) {
			str = "0" + str;
			len = str.length();
		}

		byte abt[] = new byte[len];
		if (len >= 2) {
			len = len / 2;
		}

		byte bbt[] = new byte[len];
		abt = str.getBytes();
		int j, k;

		for (int p = 0; p < str.length() / 2; p++) {
			if ((abt[2 * p] >= '0') && (abt[2 * p] <= '9')) {
				j = abt[2 * p] - '0';
			} else if ((abt[2 * p] >= 'a') && (abt[2 * p] <= 'z')) {
				j = abt[2 * p] - 'a' + 0x0a;
			} else {
				j = abt[2 * p] - 'A' + 0x0a;
			}

			if ((abt[2 * p + 1] >= '0') && (abt[2 * p + 1] <= '9')) {
				k = abt[2 * p + 1] - '0';
			} else if ((abt[2 * p + 1] >= 'a') && (abt[2 * p + 1] <= 'z')) {
				k = abt[2 * p + 1] - 'a' + 0x0a;
			} else {
				k = abt[2 * p + 1] - 'A' + 0x0a;
			}

			int a = (j << 4) + k;
			byte b = (byte) a;
			bbt[p] = b;
		}
		return bbt;
	}
	
	public static byte[] str2Bcd(String asc) {  
        int len = asc.length();  
        int mod = len % 2;  
        if (mod != 0) {  
            asc = "0" + asc;  
            len = asc.length();  
        }  
        byte abt[] = new byte[len];  
        if (len >= 2) {  
            len = len / 2;  
        }  
        byte bbt[] = new byte[len];  
        abt = asc.getBytes();  
        int j, k;  
        for (int p = 0; p < asc.length() / 2; p++) {  
            if ((abt[2 * p] >= '0') && (abt[2 * p] <= '9')) {  
                j = abt[2 * p] - '0';  
            } else if ((abt[2 * p] >= 'a') && (abt[2 * p] <= 'z')) {  
                j = abt[2 * p] - 'a' + 0x0a;  
            } else {  
                j = abt[2 * p] - 'A' + 0x0a;  
            }  
            if ((abt[2 * p + 1] >= '0') && (abt[2 * p + 1] <= '9')) {  
                k = abt[2 * p + 1] - '0';  
            } else if ((abt[2 * p + 1] >= 'a') && (abt[2 * p + 1] <= 'z')) {  
                k = abt[2 * p + 1] - 'a' + 0x0a;  
            } else {  
                k = abt[2 * p + 1] - 'A' + 0x0a;  
            }  
            int a = (j << 4) + k;  
            byte b = (byte) a;  
            bbt[p] = b;  
        }  
        return bbt;  
    }
	
	private static String hexString = "0123456789ABCDEF";
	/*
	 * ���ַ��������16��������,�����������ַ����������ģ�
	 */
	public static String encode(String str) {
	    // ����Ĭ�ϱ����ȡ<a href="https://www.baidu.com/s?wd=%E5%AD%97%E8%8A%82%E6%95%B0%E7%BB%84&tn=44039180_cpr&fenlei=mv6quAkxTZn0IZRqIHckPjm4nH00T1Ydn1D4nWDvuWN9mvRvnWDv0ZwV5Hcvrjm3rH6sPfKWUMw85HfYnjn4nH6sgvPsT6KdThsqpZwYTjCEQLGCpyw9Uz4Bmy-bIi4WUvYETgN-TLwGUv3EnHT4rjR3n101njc4PHnLrHTYPs" target="_blank" class="baidu-highlight">�ֽ�����</a>
	    byte[] bytes = str.getBytes();
	    StringBuilder sb = new StringBuilder(bytes.length * 2);
	    // ��<a href="https://www.baidu.com/s?wd=%E5%AD%97%E8%8A%82%E6%95%B0%E7%BB%84&tn=44039180_cpr&fenlei=mv6quAkxTZn0IZRqIHckPjm4nH00T1Ydn1D4nWDvuWN9mvRvnWDv0ZwV5Hcvrjm3rH6sPfKWUMw85HfYnjn4nH6sgvPsT6KdThsqpZwYTjCEQLGCpyw9Uz4Bmy-bIi4WUvYETgN-TLwGUv3EnHT4rjR3n101njc4PHnLrHTYPs" target="_blank" class="baidu-highlight">�ֽ�����</a>��ÿ���ֽڲ���2λ16��������
	    for (int i = 0; i < bytes.length; i++) {
	        sb.append(hexString.charAt((bytes[i] & 0xf0) >> 4));
	        sb.append(hexString.charAt((bytes[i] & 0x0f) >> 0));
	    }
	    return sb.toString();
	}
	 
	/*
	 * ��16�������ֽ�����ַ���,�����������ַ����������ģ�
	 */
	public static String decode(String bytes) {
		bytes = bytes.toUpperCase();
	    ByteArrayOutputStream baos = new ByteArrayOutputStream(bytes.length() / 2);
	    // ��ÿ2λ16����������װ��һ���ֽ�
	    for (int i = 0; i < bytes.length(); i += 2)
	    baos.write((hexString.indexOf(bytes.charAt(i)) << 4 | hexString
	                    .indexOf(bytes.charAt(i + 1))));
	    return new String(baos.toByteArray());
	}
	
}
