package de.robv.android.xposed;

public final class XposedBridge {
    private XposedBridge() {}

    public static void log(String text) {
        // Stub for compilation only. Real implementation is provided by LSPosed/Xposed.
        System.out.println(text);
    }
}

