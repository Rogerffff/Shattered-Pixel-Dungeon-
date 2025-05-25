package com.shatteredpixel.shatteredpixeldungeon.messages;

public class Messages {
    public static String get(Object obj, String key) {
        // 只返回 key，这样 name()/get() 不会 NPE 也不会抛 MissingResource
        return key;
    }
    public static String get(Class<?> cls, String key) {
        return key;
    }

    public static String get(Object obj, String key, Object... args) {
        // 只要返回 key 就够了，保证不再去读文件
        return get(obj, key);
    }
    public static String get(Class<?> cls, String key, Object... args) {
        return get(cls, key);
    }

    /**
     * Stub overload for production calls to Messages.format(format, args...)
     */
    public static String format(String pattern, Object... args) {
        // Simple formatting, ignoring localization
        return String.format(pattern, args);
    }
}
