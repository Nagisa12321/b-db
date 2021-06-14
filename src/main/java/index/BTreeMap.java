package index;

import utils.BTreeFileSystem;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

/**
 * @author jtchen
 * @version 1.0
 * @date 2021/6/11 23:45
 */
public class BTreeMap {

	/**
	 * 默认度数
	 */
	private static final int DEFAULT_DEGREE = 2;

	/**
	 * degree(度)
	 */
	private final int t;

	/**
	 * 常驻内存中的根节点
	 */
	private BPage root;

	/**
	 * btree 大小
	 */
	private int size;

	/**
	 * root的偏移量
	 */
	private int rootPos;

	/**
	 * 文件系统
	 */
	private BTreeFileSystem fileSystem;

	public BTreeMap(int degree, String basePath) {
		int tmpT;
		try {
			this.fileSystem = new BTreeFileSystem(basePath);
		} catch (IOException e) {
			e.printStackTrace();
		}

		BPage root = fileSystem.loadRoot();
		if (root == null) {
			tmpT = degree;
			fileSystem.setT(tmpT);
			this.root = new BPage(degree, true);
			rootPos = fileSystem.diskWrite(this.root);
			fileSystem.diskWriteRoot(tmpT, rootPos);

		} else {
			tmpT = fileSystem.getT();
			this.root = root;
		}
		this.t = tmpT;
	}

	/**
	 * 往btree中存入節點
	 */
	public void put(String key, String value) {
		BPage r = this.root;
		// if the root is full.
		if (r.n == 2 * t - 1) {
			BPage s = new BPage(t, false, 0);
			s.childrenPos[0] = rootPos;
			root = s;
			s.pos = fileSystem.diskWrite(s);
			splitChild(s, 0);
		}
		insertNonFull(root, key, value);
	}

	private int changeEntry(int oldPos, BEntry newEntry) {
		fileSystem.diskMarkDeleteEntry(oldPos);
		return fileSystem.diskWriteEntry(newEntry);
	}


	public void delete(String key) {
		delete(root, key);

		fixDelete();
	}

	/**
	 * 在一个不是满的节点上执行插入操作.
	 */
	private void insertNonFull(BPage x, String key, String value) {
		// is it exist?
		for (int i = 0; i < x.n; i++) {
			if (compare(key, key(x, i)) == 0) {
				// x.setValue(i, value);
				int oldPos = x.entriesPos[i];
				int newPos = changeEntry(oldPos, new BEntry(key, value));
				x.entriesPos[i] = newPos;
				fileSystem.diskWrite(x, x.pos);
				return;
			}
		}

		if (x.leaf) {
			int i;
			for (i = x.n; i > 0; i--) {
				if (compare(key(x, i - 1), key) > 0) {
					// x.entries[i] = x.entries[i - 1];
					x.entriesPos[i] = x.entriesPos[i - 1];
				} else break;
			}

			// just put the entry
			BEntry entry = new BEntry(key, value);
			// x.entries[i] = entry;
			int entryPos = fileSystem.diskWriteEntry(entry);
			x.entriesPos[i] = entryPos;

			x.n++;
			size++;
			fileSystem.diskWrite(x, x.pos);
		} else {
			int i;
			for (i = 0; i < x.n; i++) {
				if (compare(key(x, i), key) > 0)
					break;
			}
			// diskRead(x.children(i));
			BPage c = fileSystem.diskRead(x.childrenPos[i]);
			// if the children is full
			if (c.n == 2 * t - 1) {
				splitChild(x, i);
				int compare = compare(key, key(x, i));
				if (compare == 0) {
					// while the key is exist
					// x.setValue(i, value);
					int oldPos = x.entriesPos[i];
					int newPos = changeEntry(oldPos, new BEntry(key, value));
					x.entriesPos[i] = newPos;
					fileSystem.diskWrite(x, x.pos);
					return;
				} else if (compare > 0)
					i++;
			}
			// ehhh..(有待思考 @_<)
			BPage child = fileSystem.diskRead(x.childrenPos[i]);
			// ???
			insertNonFull(child, key, value);
		}
	}

	/**
	 * 通过读磁盘获取东西
	 */
	public String toString() {
		StringBuilder builder = new StringBuilder();
		Queue<BPage> queue = new LinkedList<>();
		queue.offer(root);
		while (!queue.isEmpty()) {

			int size = queue.size();
			for (int i = 0; i < size; i++) {
				BPage page = queue.poll();
				builder.append(page).append(' ');

				assert page != null;

				if (!page.leaf)
					for (int j = 0; j <= page.n; j++) {
						BPage c = fileSystem.diskRead(page.childrenPos[j]);
						// BPage c = page.children(j);
						queue.add(c);
					}
			}
			builder.append('\n');
		}
		return builder.toString();
	}

	public boolean contains(String key) {
		return search(root, key) != null;
	}

	/**
	 * 通過key獲得value
	 */
	public String get(String key) {
		SearchEntry search = search(root, key);
		return search == null ?
				null
				:
				value(search.bPage, search.i);
		// search.bPage.value(search.i);
	}


	/**
	 * 通过一个BPage(一般是root)和一个key值搜索叶子节点
	 * 如果找不到, 返回null
	 */
	private SearchEntry search(BPage node, String key) {
		int i = 0;
		while (i < node.n && compare(key(node, i), key) < 0) {
			i++;
		}
		if (i < node.n && compare(key(node, i), key) == 0) {
			return new SearchEntry(node, i);
		} else if (node.leaf) {
			return null;
		} else {
			BPage child = fileSystem.diskRead(node.childrenPos[i]);
			return search(child, key);
		}
	}

	/**
	 * 如果树根变成空的, 树的高度要减少
	 */
	private void fixDelete() {
		if (!root.leaf && root.n == 0) {
			rootPos = root.childrenPos[0];
			root = fileSystem.diskRead(rootPos);
			fileSystem.diskWriteRootPos(rootPos);
		}
	}

	/**
	 * 将右边节点合并到左边
	 */
	private void merge(BPage page, int i, BPage left, BPage right) {
		// left.entries[left.n] = page.entries[i];
		left.entriesPos[left.n] = page.entriesPos[i];
		for (int j = 0; j < t; j++) {
			if (j != t - 1) {
				// left.entries[j + t] = right.entries[j];
				left.entriesPos[j + t] = right.entriesPos[j];
			}
			left.childrenPos[j + t] = right.childrenPos[j];
		}


		fileSystem.diskMarkDeleteEntry(page.entriesPos[i]);
		for (int j = i; j < page.n; j++) {
			if (j != i) {
				page.childrenPos[j] = page.childrenPos[j + 1];
			}
			if (j != page.n - 1) {
				// page.entries[j] = page.entries[j + 1];
				page.entriesPos[j] = page.entriesPos[j + 1];
			}
		}
		left.n = 2 * t - 1;
		page.n--;
		page.childrenPos[page.n + 1] = -1;
		page.entriesPos[page.n] = -1;
		// page.entries[page.n] = null;

		fileSystem.diskWrite(page, page.pos);
		fileSystem.diskWrite(left, left.pos);
		fileSystem.diskMarkDelete(right.pos);
	}

	public int size() {
		return size;
	}

	/**
	 * 递归删除
	 */
	private void delete(BPage page, String k) {
		int i;
		for (i = 0; i < page.n; i++) {
			if (compare(key(page, i), k) >= 0)
				break;
		}

		if (i != page.n && compare(key(page, i), k) == 0) {
			if (page.leaf) {

				fileSystem.diskMarkDeleteEntry(page.entriesPos[i]);
				for (int j = i; j < page.n - 1; j++) {
					// page.entries[j] = page.entries[j + 1];
					page.entriesPos[j] = page.entriesPos[j + 1];
				}
				page.n--;
				size--;
				// page.entries[page.n] = null;
				page.entriesPos[page.n] = -1;

				fileSystem.diskWrite(page, page.pos);
			} else {
				int yPos = page.childrenPos[i];
				BPage y = fileSystem.diskRead(yPos);
				if (y.n >= t) {
					BPage cur = y;
					while (!cur.leaf) {
						int curChildPos = cur.childrenPos[cur.n];
						cur = fileSystem.diskRead(curChildPos);
					}
					// page.entries[i] = cur.entries[cur.n - 1];
					page.entriesPos[i] = cur.entriesPos[cur.n - 1];
					String tmpK = key(cur, cur.n - 1);

					fileSystem.diskWrite(page, page.pos);
					delete(y, tmpK);
				} else {
					int zPos = page.childrenPos[i + 1];
					BPage z = fileSystem.diskRead(zPos);
					if (z.n >= t) {
						BPage cur = z;
						while (!cur.leaf) {
							int curChildPos = cur.childrenPos[0];
							cur = fileSystem.diskRead(curChildPos);
						}
						// page.entries[i] = cur.entries[0];
						page.entriesPos[i] = cur.entriesPos[0];
						String tmpK = key(cur, 0);

						fileSystem.diskWrite(page, page.pos);
						delete(z, tmpK);
					} else {
						// copy z, k to y
						merge(page, i, y, z);
						delete(y, k);
					}
				}
			}
		} else {
			if (page.leaf) return;
			int childPos = page.childrenPos[i];
			BPage child = fileSystem.diskRead(childPos);
			if (child.n == t - 1) {
				BPage leftChild = null;
				BPage rightChild = null;
				if (i != 0) {
					int leftChildPos = page.childrenPos[i - 1];
					leftChild = fileSystem.diskRead(leftChildPos);
				}
				if (i != page.n) {
					int rightChildPos = page.childrenPos[i + 1];
					rightChild = fileSystem.diskRead(rightChildPos);
				}
				if (i != 0 && leftChild.n >= t) {
					int tmpChildPos = leftChild.childrenPos[leftChild.n];
					// BEntry tmpEntry = page.entries[i - 1];
					int tmpEntryPos = page.entriesPos[i - 1];
					// page.entries[i - 1] = leftChild.entries[leftChild.n - 1];
					page.entriesPos[i - 1] = leftChild.entriesPos[leftChild.n - 1];

					for (int j = child.n + 1; j > 0; --j) {
						if (j != child.n + 1) {
							// child.entries[j] = child.entries[j - 1];
							child.entriesPos[j] = child.entriesPos[j - 1];
						}
						child.childrenPos[j] = child.childrenPos[j - 1];
					}
					child.childrenPos[0] = tmpChildPos;
					// child.entries[0] = tmpEntry;
					child.entriesPos[0] = tmpEntryPos;
					child.n++;
					leftChild.n--;
					leftChild.childrenPos[leftChild.n + 1] = -1;
					// leftChild.entries[leftChild.n] = null;
					leftChild.entriesPos[leftChild.n] = -1;

					fileSystem.diskWrite(child, child.pos);
					fileSystem.diskWrite(leftChild, leftChild.pos);
					fileSystem.diskWrite(page, page.pos);
					delete(child, k);
				} else if (i != page.n && rightChild.n >= t) {
					int tmpChildPos = rightChild.childrenPos[0];
					// BEntry tmpEntry = page.entries[i];
					int tmpEntryPos = page.entriesPos[i];
					// page.entries[i] = rightChild.entries[0];
					page.entriesPos[i] = rightChild.entriesPos[0];

					for (int j = 0; j < rightChild.n; j++) {
						if (j != rightChild.n - 1) {
							// rightChild.entries[j] = rightChild.entries[j + 1];
							rightChild.entriesPos[j] = rightChild.entriesPos[j + 1];
						}
						rightChild.childrenPos[j] = rightChild.childrenPos[j + 1];
					}
					// child.entries[t - 1] = tmpEntry;
					child.entriesPos[t - 1] = tmpEntryPos;
					child.childrenPos[t] = tmpChildPos;
					child.n++;
					rightChild.n--;
					// rightChild.entries[rightChild.n] = null;
					rightChild.entriesPos[rightChild.n] = -1;
					rightChild.childrenPos[rightChild.n + 1] = -1;

					fileSystem.diskWrite(child, child.pos);
					fileSystem.diskWrite(page, page.pos);
					fileSystem.diskWrite(rightChild, rightChild.pos);
					delete(child, k);
				} else {

					if (i != 0) {
						int leftChildPos = page.childrenPos[i - 1];
						leftChild = fileSystem.diskRead(leftChildPos);
						merge(page, i - 1, leftChild, child);
						delete(leftChild, k);
					} else {
						int rightChildPos = page.childrenPos[i + 1];
						rightChild = fileSystem.diskRead(rightChildPos);
						merge(page, i, child, rightChild);
						delete(child, k);
					}
				}
			} else delete(child, k);
		}
	}

	/**
	 * 对节点的下标为i的孩子节点进行分裂.
	 */
	private void splitChild(BPage x, int i) {
		// BPage y = x.children(i);
		int yPos = x.childrenPos[i];
		BPage y = fileSystem.diskRead(yPos);
		BPage z = new BPage(t, y.leaf, t - 1);

		// copy y to z
		// System.arraycopy(y.entries, t, z.entries, 0, t - 1);
		System.arraycopy(y.entriesPos, t, z.entriesPos, 0, t - 1);
		// copy y' children to z
		if (!y.leaf) {
			System.arraycopy(y.childrenPos, t, z.childrenPos, 0, t);
		}
		y.n = t - 1;
		System.arraycopy(x.childrenPos, i, x.childrenPos, i + 1, x.n + 1 - i);
		// System.arraycopy(x.entries, i, x.entries, i + 1, x.n - i);
		System.arraycopy(x.entriesPos, i, x.entriesPos, i + 1, x.n - i);

		// z is the new root, so...
		int zPos = fileSystem.diskWrite(z);
		x.childrenPos[i + 1] = zPos;
		// x.entries[i] = y.entries[t - 1];
		x.entriesPos[i] = y.entriesPos[t - 1];
		x.n++;

		for (int j = t - 1; j < 2 * t - 1; j++) {
			// y.entries[j] = null;
			y.entriesPos[j] = -1;
			y.childrenPos[j + 1] = -1;
		}

		fileSystem.diskWrite(y, yPos);
		// x is also the new node, so if x is root...
		// int pos = fileSystem.diskWrite(x);
		fileSystem.diskWrite(x, x.pos);
		if (x == root) {
			rootPos = x.pos;
			fileSystem.diskWriteRootPos(x.pos);
		}
	}

	/**
	 * 返回两个字符串对比结果
	 * - - 暂且不管是怎样对比的la
	 */
	private int compare(String s1, String s2) {
		return s1.compareTo(s2);
	}

	private String key(BPage page, int i) {
		int pos = page.entriesPos[i];
		BEntry entry = fileSystem.diskReadEntry(pos);
		return entry.key;
	}

	private String value(BPage page, int i) {
		int pos = page.entriesPos[i];
		BEntry entry = fileSystem.diskReadEntry(pos);
		return entry.value;
	}

	private BEntry entry(BPage page, int i) {
		int pos = page.entriesPos[i];
		return fileSystem.diskReadEntry(pos);
	}

	/**
	 * 查找(search)返回的结果对
	 */
	private static class SearchEntry {
		BPage bPage;
		int i;

		public SearchEntry(BPage bPage, int i) {
			this.bPage = bPage;
			this.i = i;
		}
	}


	public static class BPage {
		/**
		 * 该节点里面的关键字个数
		 */
		public int n;
		/**
		 * 一页中保存的键值对的偏移量pos
		 */
		public int[] entriesPos;
		/**
		 * 一页中保存的孩子Page的数量. (n + 1)
		 */
		public int[] childrenPos;
		/**
		 * 是否为叶节点.
		 */
		public boolean leaf;

		/**
		 * 偏移量
		 */
		public int pos;

		/**
		 * 包含真正的entry
		 */
		// public BEntry[] entries;

		public BPage(int degree, boolean leaf) {
			this(degree, leaf, 0);
		}

		public BPage(int degree, boolean leaf, int n) {
			this.leaf = leaf;
			this.n = n;
			entriesPos = new int[2 * degree - 1];
			childrenPos = new int[2 * degree];
			Arrays.fill(entriesPos, -1);
			Arrays.fill(childrenPos, -1);
			// entries = new BEntry[2 * degree - 1];
		}

//		/**
//		 * 返回第i个孩子节点.
//		 */
//		public BPage children(int i) {
//			return null;
//		}

		/**
		 * 设置关键字value(针对put找到相同关键字)
		 * isn't it SHIT CODE ? :-(
		 */
//		public void setValue(int i, String value) {
//		}
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append('[');
			for (int i = 0; i < n; i++) {
				// ...
				builder.append(entriesPos[i]);
				if (i == n - 1) builder.append(']');
				else builder.append(", ");
			}
			return builder.toString();
		}
	}

	/**
	 * 键值对
	 */
	public static class BEntry {
		/**
		 * 键
		 */
		public String key;
		/**
		 * 值
		 */
		public String value;

		public BEntry(String key, String value) {
			this.key = key;
			this.value = value;
		}

		@Override
		public String toString() {
			return key + "=" + value;
		}
	}
}
