package com.sentinelai.mobile;

import android.content.Context;
import android.net.Uri;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

final class YaraEngine {
    private static final int MAX_BYTES = 32 * 1024 * 1024;
    private static final boolean AVAILABLE;

    static {
        boolean loaded;
        try {
            System.loadLibrary("sentinel_yara");
            loaded = true;
        } catch (UnsatisfiedLinkError error) {
            loaded = false;
        }
        AVAILABLE = loaded;
    }

    private YaraEngine() { }

    private static native String scanBytes(byte[] data);

    static String scanSelectedFile(Context context, Uri uri) {
        if (!AVAILABLE) return "YARA-X native engine unavailable in this build.";
        try (InputStream input = context.getContentResolver().openInputStream(uri);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (input == null) return "YARA-X skipped: selected file could not be reopened.";
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) {
                if (output.size() + count > MAX_BYTES) {
                    return "YARA-X skipped: file exceeds 32 MiB native scan limit.";
                }
                output.write(buffer, 0, count);
            }
            return scanBytes(output.toByteArray());
        } catch (Exception error) {
            return "YARA-X scan unavailable: " + error.getClass().getSimpleName();
        }
    }
}
