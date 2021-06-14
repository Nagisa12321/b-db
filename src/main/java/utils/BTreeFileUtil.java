package utils;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * @author jtchen
 * @version 1.0
 * @date 2021/6/12 14:35
 */
public class BTreeFileUtil {

	public static int read4byte(RandomAccessFile f) throws IOException {
		int res = 0;
		for (int i = 0; i < 4; i++) {
			int data = f.read();
			res <<= 8;
			res += data;
		}
		return res;
	}

	public static void write4byte(RandomAccessFile f, int num) throws IOException {
		int data4 = num & 0x0ff;
		int data3 = (num >> 8) & 0x0ff;
		int data2 = (num >> 16) & 0x0ff;
		int data1 = (num >> 24) & 0x0ff;

		f.write(data1);
		f.write(data2);
		f.write(data3);
		f.write(data4);
	}
}
