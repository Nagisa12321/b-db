package utils;

import index.BTreeMap;

/**
 * @author jtchen
 * @version 1.0
 * @date 2021/6/13 22:37
 */
public class DBUtils {

	private static final BTreeMap map = new BTreeMap(500, "");

	public static String get(String key) {
		return map.get(key);
	}

	public static void put(String key, String value) {
		map.put(key, value);
	}

	public static boolean contains(String key) {
		return map.contains(key);
	}

	public static void delete(String key) {
		map.delete(key);
	}
}
