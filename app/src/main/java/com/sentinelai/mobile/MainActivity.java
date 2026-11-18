package com.sentinelai.mobile;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MainActivity extends Activity {
    private static final int PICK_FILE = 41;
    private static final long MAX_FILE_BYTES = 100L * 1024L * 1024L;
    private static final int MAX_ARCHIVE_ENTRIES = 3000;
    private LinearLayout content;
    private TextView status;

    private static final String[] REVIEW_PERMISSIONS = {
        "android.permission.RECORD_AUDIO",
        "android.permission.CAMERA",
        "android.permission.READ_SMS",
        "android.permission.RECEIVE_SMS",
        "android.permission.READ_CONTACTS",
        "android.permission.READ_CALL_LOG",
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.ACCESS_BACKGROUND_LOCATION",
        "android.permission.READ_PHONE_STATE",
        "android.permission.REQUEST_INSTALL_PACKAGES",
        "android.permission.SYSTEM_ALERT_WINDOW"
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        buildUi();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(36, 42, 36, 48);
        content.setBackgroundColor(0xFF0B1020);
        scroll.addView(content);
        setContentView(scroll);

        content.addView(text("SENTINEL AI", 28, 0xFFFFFFFF));
        content.addView(text("Mobile Security • Local-first MVP", 14, 0xFF9EACC7));
        status = text("Ready. Checks run on this device.", 14, 0xFF9FE3C0);
        status.setPadding(0, 24, 0, 24);
        content.addView(status);
        addButton("Scan installed apps", v -> scanApps());
        addButton("Analyze a file (user-selected)", v -> pickFile());
        addButton("About detection limits", v -> showLimits());
        content.addView(text("Privacy: no account, no upload, no automatic deletion. Findings are indicators, not proof of malware.", 13, 0xFFB7C2D8));
    }

    private TextView text(String value, int size, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setPadding(0, 8, 0, 8);
        return t;
    }

    private void addButton(String label, View.OnClickListener listener) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setOnClickListener(listener);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, 12, 0, 12);
        content.addView(b, p);
    }

    private void heading(String value) {
        content.addView(text(value, 18, 0xFF78B7FF));
    }

    private void scanApps() {
        heading("Installed app permission review");
        status.setText("Scanning installed app declarations…");
        new Thread(() -> {
            List<String> findings = new ArrayList<>();
            PackageManager pm = getPackageManager();
            List<PackageInfo> packages;
            try {
                if (Build.VERSION.SDK_INT >= 33) {
                    packages = pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS));
                } else {
                    //noinspection deprecation
                    packages = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS);
                }
                for (PackageInfo info : packages) {
                    if (info.requestedPermissions == null) continue;
                    List<String> matched = new ArrayList<>();
                    for (String permission : info.requestedPermissions) {
                        for (String sensitive : REVIEW_PERMISSIONS) {
                            if (sensitive.equals(permission)) matched.add(permission.substring(permission.lastIndexOf('.') + 1));
                        }
                    }
                    if (!matched.isEmpty()) {
                        String label;
                        try {
                            label = pm.getApplicationLabel(info.applicationInfo).toString();
                        } catch (Exception e) {
                            label = info.packageName;
                        }
                        findings.add(label + " (" + info.packageName + ")\nDeclared sensitive permissions: " + join(matched)
                                + "\nCheck whether these match the app's purpose. Declaration alone is not proof of misuse.");
                    }
                }
                Collections.sort(findings);
                runOnUiThread(() -> {
                    status.setText("Review complete: " + packages.size() + " visible packages; " + findings.size() + " app(s) to review.");
                    if (findings.isEmpty()) content.addView(text("No matching permission declarations found.", 14, 0xFF9FE3C0));
                    for (String item : findings) content.addView(text("• " + item, 13, 0xFFE8D7A8));
                });
            } catch (Exception e) {
                runOnUiThread(() -> status.setText("Scan unavailable: " + e.getClass().getSimpleName()));
            }
        }).start();
    }

    private String join(List<String> values) {
        StringBuilder b = new StringBuilder();
        for (String value : values) {
            if (b.length() > 0) b.append(", ");
            b.append(value);
        }
        return b.toString();
    }

    private void pickFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, PICK_FILE);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_FILE || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        status.setText("Analyzing selected file locally…");
        new Thread(() -> analyzeFile(uri)).start();
    }

    private String getDisplayName(Uri uri) {
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) return cursor.getString(index);
            }
        } catch (Exception ignored) { }
        return "Selected file";
    }

    private void analyzeFile(Uri uri) {
        try {
            String name = getDisplayName(uri);
            String lower = name.toLowerCase(Locale.ROOT);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            long total = 0;
            byte[] buffer = new byte[8192];
            int firstByte = -1;
            int secondByte = -1;
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                if (in == null) throw new IllegalStateException("Cannot open selected file");
                int n;
                while ((n = in.read(buffer)) != -1) {
                    if (total == 0 && n > 0) {
                        firstByte = buffer[0] & 0xff;
                        if (n > 1) secondByte = buffer[1] & 0xff;
                    }
                    total += n;
                    if (total > MAX_FILE_BYTES) throw new IllegalStateException("File exceeds 100 MB MVP limit");
                    digest.update(buffer, 0, n);
                }
            }
            StringBuilder hex = new StringBuilder();
            for (byte b : digest.digest()) hex.append(String.format(Locale.US, "%02x", b));
            String signatureMatch = LocalHashSignatures.lookup(this, hex.toString());
            String yaraResult = YaraEngine.scanSelectedFile(this, uri);

            boolean zipMagic = firstByte == 0x50 && secondByte == 0x4b;
            boolean apkName = lower.endsWith(".apk");
            boolean zipName = lower.endsWith(".zip");
            List<String> findings = new ArrayList<>();
            if (apkName && !zipMagic) findings.add("Extension says APK, but ZIP signature was not found.");
            if (zipName && !zipMagic) findings.add("Extension says ZIP, but ZIP signature was not found.");
            if (!apkName && !zipName && zipMagic) findings.add("File content looks like a ZIP archive although its extension is different.");
            int entries = 0;
            boolean hasManifest = false;
            boolean hasDex = false;
            boolean traversal = false;
            boolean nestedExecutable = false;
            long expandedBytes = 0;

            if (zipMagic) {
                try (InputStream raw = getContentResolver().openInputStream(uri);
                     ZipInputStream zip = new ZipInputStream(new BufferedInputStream(raw))) {
                    if (raw == null) throw new IllegalStateException("Cannot reopen selected file");
                    ZipEntry entry;
                    while ((entry = zip.getNextEntry()) != null) {
                        entries++;
                        if (entries > MAX_ARCHIVE_ENTRIES) {
                            findings.add("Archive entry limit exceeded; deeper inspection stopped.");
                            break;
                        }
                        String path = entry.getName();
                        String normalized = path.replace('\\', '/');
                        if (normalized.startsWith("/") || normalized.contains("../") || normalized.equals("..")) traversal = true;
                        String pathLower = normalized.toLowerCase(Locale.ROOT);
                        if (pathLower.equals("androidmanifest.xml")) hasManifest = true;
                        if (pathLower.matches("classes([0-9]+)?\\.dex")) hasDex = true;
                        if (pathLower.endsWith(".apk") || pathLower.endsWith(".dex") || pathLower.endsWith(".jar")) nestedExecutable = true;
                        int read;
                        while ((read = zip.read(buffer)) != -1) {
                            expandedBytes += read;
                            if (expandedBytes > MAX_FILE_BYTES) {
                                findings.add("Expanded archive data exceeded 100 MB; inspection stopped to limit resource use.");
                                break;
                            }
                        }
                        zip.closeEntry();
                        if (expandedBytes > MAX_FILE_BYTES) break;
                    }
                } catch (Exception archiveError) {
                    findings.add("Archive could not be fully inspected: " + archiveError.getClass().getSimpleName());
                }
                if (traversal) findings.add("Archive contains a path-traversal-like entry name.");
                if (nestedExecutable) findings.add("Archive contains nested executable or application package files.");
                if (apkName && hasManifest && hasDex) findings.add("APK-like structure found (AndroidManifest.xml and DEX file). This is not a malware verdict.");
                else if (apkName) findings.add("APK structure was not fully confirmed by the basic archive check.");
                findings.add("Archive entries inspected: " + entries);
            }

            String level = findings.isEmpty() ? "No basic structural warning found" : "Review recommended";
            StringBuilder result = new StringBuilder();
            result.append("Local file analysis complete\nName: ").append(name)
                    .append("\nSize: ").append(total).append(" bytes")
                    .append("\nSHA-256: ").append(hex)
                    .append("\nContent signature: ").append(zipMagic ? "ZIP-compatible" : "Not recognized as ZIP");
            result.append("\nLocal hash signature database: ").append(signatureMatch == null ? "No match" : "MATCH — " + signatureMatch);
            result.append("\nYARA-X rule scan: ").append(yaraResult);
            result.append("\nAssessment: ").append(level);
            for (String finding : findings) result.append("\n• ").append(finding);
            if (findings.isEmpty()) result.append("\nNo basic structural indicators were found. This does NOT prove the file is safe.");
            result.append("\nNo upload, modification, or deletion was performed.");

            String finalResult = result.toString();
            runOnUiThread(() -> {
                heading("Selected file result");
                content.addView(text(finalResult, 13, findings.isEmpty() ? 0xFFB7C2D8 : 0xFFFFD28A));
                status.setText("Local file analysis finished.");
            });
        } catch (Exception e) {
            runOnUiThread(() -> status.setText("File check failed: " + e.getMessage()));
        }
    }

    private void showLimits() {
        heading("Detection limits");
        content.addView(text("This version can list visible installed apps, review declared permissions, calculate a selected file's SHA-256, compare it with a bundled local hash-signature database, and inspect basic ZIP/APK archive structure. The bundled database currently contains an EICAR antivirus test-file indicator only; it is not a real malware feed and the EICAR test file is not malware. It does not execute files in a sandbox. It cannot inspect other apps' private data, prove spyware is absent, or monitor every runtime action. Android's app sandbox restricts these capabilities. Findings are informational and require human review. No files are deleted.", 14, 0xFFB7C2D8));
    }
}
