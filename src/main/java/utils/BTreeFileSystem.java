package utils;

import index.BTreeMap;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * @author jtchen
 * @version 1.0
 * @date 2021/6/11 19:25
 */
public class BTreeFileSystem {

	/**
	 * 日志
	 */
	private static Logger logger = Logger.getLogger(BTreeFileSystem.class);

	/**
	 * b树的度
	 */
	private int t;

	/**
	 * root的在文件的偏移量
	 */
	private int rootPos;

	/**
	 * 存储头节点信息的文件
	 */
	private RandomAccessFile rootFile;

	/**
	 * 存储节点信息的文件
	 */
	private RandomAccessFile nodeFile;

	/**
	 * 存储键值对的文件
	 */
	private RandomAccessFile entryFile;

	/**
	 * 是否需要加载?
	 */
	private boolean rootFileExist;

	@SuppressWarnings("ResultOfMethodCallIgnored")
	public BTreeFileSystem(String basePath) throws IOException {
		File root = new File(basePath + "/root.b");
		File node = new File(basePath + "/node.b");
		File entry = new File(basePath + "/entry.b");
		rootFileExist = true;
		// root file not found
		// should create 3 files
		if (root.createNewFile()) {
			logger.info("root file not found, now create");
			rootFileExist = false;
			if (node.delete()) node.createNewFile();
			if (entry.delete()) entry.createNewFile();
		}
		this.rootFile = new RandomAccessFile(root, "rws");
		this.nodeFile = new RandomAccessFile(node, "rws");
		this.entryFile = new RandomAccessFile(entry, "rws");
		if (rootFileExist) {
			t = BTreeFileUtil.read4byte(rootFile);
			rootPos = BTreeFileUtil.read4byte(rootFile);
		}
	}

	/**
	 * 首先检查有没有三个文件:
	 * 1. root
	 * 2. node
	 * 3. entry
	 * 如果没有则重新创建
	 * 返回磁盘中是否有存在
	 */
	@SuppressWarnings("ResultOfMethodCallIgnored")
	public BTreeMap.BPage loadRoot() {
		if (rootFileExist) return diskRead(rootPos);
		else return null;
	}

	/**
	 * 通过偏移量在文件系统中读取一页
	 */
	public BTreeMap.BPage diskRead(int pos) {
		try {
			nodeFile.seek(pos);
			int flag = nodeFile.read();
			boolean leaf = (flag & 1) == 0;
			int n = BTreeFileUtil.read4byte(nodeFile);
			int[] childrenPos = new int[2 * t];
			int[] entriesPos = new int[2 * t - 1];
			for (int i = 0; i < t * 2 - 1; i++) {
				entriesPos[i] = BTreeFileUtil.read4byte(nodeFile);
			}
			for (int i = 0; i < t * 2; i++) {
				childrenPos[i] = BTreeFileUtil.read4byte(nodeFile);
			}
			BTreeMap.BPage bPage = new BTreeMap.BPage(t, leaf);
			bPage.n = n;
			bPage.entriesPos = entriesPos;
			bPage.childrenPos = childrenPos;

			bPage.pos = pos;
			return bPage;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * 通过偏移量在文件系统中读取一个键值对
	 */
	public BTreeMap.BEntry diskReadEntry(int pos) {
		try {
			entryFile.seek(pos);
			int flag = entryFile.read();
			int lenKey = BTreeFileUtil.read4byte(entryFile);
			StringBuilder builder = new StringBuilder();
			for (int i = 0; i < lenKey; i++) {
				builder.append((char) entryFile.read());
			}
			String key = builder.toString();
			builder = new StringBuilder();
			int lenValue = BTreeFileUtil.read4byte(entryFile);
			for (int i = 0; i < lenValue; i++) {
				builder.append((char) entryFile.read());
			}
			String value = builder.toString();
			return new BTreeMap.BEntry(key, value);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public void diskWriteRoot(int t, int rootPos) {
		try {
			rootFile.seek(0);
			BTreeFileUtil.write4byte(rootFile, t);
			BTreeFileUtil.write4byte(rootFile, rootPos);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void diskWriteRootPos(int pos) {
		try {
			rootFile.seek(4);
			BTreeFileUtil.write4byte(rootFile, pos);
			// what tr root pos use for ...?
			this.rootPos = pos;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 将一页写入文件系统中(新增)
	 */
	public int diskWrite(BTreeMap.BPage page) {
		int pos = -1;
		try {
			pos = Math.toIntExact(nodeFile.length());
			nodeFile.seek(pos);
			if (page.leaf) nodeFile.write(0);
			else nodeFile.write(1);
			BTreeFileUtil.write4byte(nodeFile, page.n);

			// write the entries
			// for (int i = 0; i < 2 * t - 1; i++) {
			// 	if (page.entriesPos[i] != -1) {
			// 		int newPos = diskWriteEntry(page.entries[i]);
			// 		page.entriesPos[i] = newPos;
			// 	}
			// }

			for (int i = 0; i < 2 * t - 1; i++) {
				BTreeFileUtil.write4byte(nodeFile, page.entriesPos[i]);
			}
			for (int i = 0; i < 2 * t; i++) {
				BTreeFileUtil.write4byte(nodeFile, page.childrenPos[i]);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return pos;
	}

	/**
	 * 将一页写入文件系统中(覆盖)
	 */
	public void diskWrite(BTreeMap.BPage page, int pos) {
		try {
			nodeFile.seek(pos);
			if (page.leaf) nodeFile.write(0);
			else nodeFile.write(1);
			BTreeFileUtil.write4byte(nodeFile, page.n);

			// write the entries
			// for (int i = 0; i < 2 * t - 1; i++) {
			// 	if (page.entriesPos[i] != -1) {
			// 		int newPos = diskWriteEntry(page.entries[i]);
			// 		page.entriesPos[i] = newPos;
			// 	}
			// }

			for (int i = 0; i < 2 * t - 1; i++) {
				BTreeFileUtil.write4byte(nodeFile, page.entriesPos[i]);
			}
			for (int i = 0; i < 2 * t; i++) {
				BTreeFileUtil.write4byte(nodeFile, page.childrenPos[i]);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 将一个键值对放在文件系统中
	 */
	public int diskWriteEntry(BTreeMap.BEntry entry) {
		int pos = -1;
		try {
			entryFile.seek(entryFile.length());
			pos = Math.toIntExact(entryFile.length());
			int keyLen = entry.key.length();
			int valueLen = entry.value.length();
			entryFile.write(1);
			BTreeFileUtil.write4byte(entryFile, keyLen);
			entryFile.write(entry.key.getBytes());
			BTreeFileUtil.write4byte(entryFile, valueLen);
			entryFile.write(entry.value.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return pos;
	}

	/**
	 * 将文件系统node区的某个偏移量对应的page标志为"已经删除"
	 */
	public void diskMarkDelete(int pos) {
		try {
			nodeFile.seek(pos);
			nodeFile.write(2);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 当加载到root文件的时候要get
	 */
	public int getT() {
		return t;
	}

	/**
	 * 加载不到的时候要set
	 */
	public void setT(int t) {
		this.t = t;
	}

	/**
	 * 将文件系统entry区的某个偏移量对应的键值对标志为"已经删除"
	 */
	public void diskMarkDeleteEntry(int pos) {
		try {
			entryFile.seek(pos);
			entryFile.write(0);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
