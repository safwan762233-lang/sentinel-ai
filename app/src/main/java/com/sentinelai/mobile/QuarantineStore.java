package com.sentinelai.mobile;

import android.content.Context;
import android.net.Uri;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Stores a user-selected COPY in app-private storage. Original is never removed.
 * This is a holding area, not OS-level malware isolation.
 */
final class QuarantineStore {
    private QuarantineStore() {}
    static File copyIntoQuarantine(Context context, Uri source, String displayName) throws Exception {
        File dir = new File(context.getFilesDir(), "quarantine");
        if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("Cannot create quarantine folder");
        String safe = displayName == null ? "selected-file" : displayName.replaceAll("[^A-Za-z0-9._-]", "_");
        String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date());
        File target = new File(dir, stamp + "-" + safe);
        try (InputStream in = context.getContentResolver().openInputStream(source);
             OutputStream out = new FileOutputStream(target)) {
            if (in == null) throw new IllegalStateException("Cannot read selected file");
            byte[] buffer = new byte[8192]; int n; long total = 0;
            while ((n = in.read(buffer)) != -1) {
                total += n;
                if (total > 100L * 1024L * 1024L) throw new IllegalStateException("Quarantine copy limit is 100 MB");
                out.write(buffer, 0, n);
            }
        } catch (Exception e) {
            // Remove only the incomplete copy created by this operation.
            if (target.exists()) target.delete();
            throw e;
        }
        return target;
    }
    static void restore(Context context, File quarantinedCopy, Uri destination) throws Exception {
        try (InputStream in = new FileInputStream(quarantinedCopy);
             OutputStream out = context.getContentResolver().openOutputStream(destination, "w")) {
            if (out == null) throw new IllegalStateException("Cannot open restore destination");
            byte[] buffer = new byte[8192]; int n;
            while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
        }
    }
}
