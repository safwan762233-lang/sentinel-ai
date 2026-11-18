package com.sentinelai.mobile;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int PICK_FILE = 41;
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
        "android.permission.SYSTEM_ALERT_WINDOW",
        "android.permission.BIND_ACCESSIBILITY_SERVICE"
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

        TextView title = text("SENTINEL AI", 28, 0xFFFFFFFF);
        content.addView(title);
        content.addView(text("Mobile Security • Local-first MVP", 14, 0xFF9EACC7));
        status = text("Ready. Checks run on this device.", 14, 0xFF9FE3C0);
        status.setPadding(0, 24, 0, 24);
        content.addView(status);

        addButton("Scan installed apps", v -> scanApps());
        addButton("Check a file (user-selected)", v -> pickFile());
        addButton("About detection limits", v -> showLimits());
        content.addView(text("Privacy: no account, no upload, no automatic deletion. App-permission findings are indicators, not proof of malware.", 13, 0xFFB7C2D8));
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
        TextView h = text(value, 18, 0xFF78B7FF);
        h.setPadding(0, 26, 0, 8);
        content.addView(h);
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
                    String[] requested = info.requestedPermissions;
                    if (requested == null || requested.length == 0) continue;
                    List<String> matched = new ArrayList<>();
                    for (String permission : requested) {
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
                                + "\nReview whether these permissions match the app's purpose. This is not a malware verdict.");
                    }
                }
                Collections.sort(findings);
                runOnUiThread(() -> {
                    status.setText("Review complete: " + packages.size() + " visible packages; " + findings.size() + " app(s) have permissions to review.");
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

    private void analyzeFile(Uri uri) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            long total = 0;
            byte[] buffer = new byte[8192];
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                if (in == null) throw new IllegalStateException("Cannot open selected file");
                int n;
                while ((n = in.read(buffer)) != -1) {
                    total += n;
                    if (total > 100L * 1024L * 1024L) throw new IllegalStateException("File exceeds 100 MB MVP limit");
                    digest.update(buffer, 0, n);
                }
            }
            StringBuilder hex = new StringBuilder();
            for (byte b : digest.digest()) hex.append(String.format(Locale.US, "%02x", b));
            String name = uri.getLastPathSegment() == null ? "Selected file" : uri.getLastPathSegment();
            String lower = name.toLowerCase(Locale.ROOT);
            boolean risky = lower.endsWith(".apk") || lower.endsWith(".dex") || lower.endsWith(".jar") || lower.endsWith(".js");
            String result = "File check complete\nName: " + name + "\nSize: " + total + " bytes\nSHA-256: " + hex
                    + (risky ? "\nReview: executable/script-like extension. This alone does NOT mean malware." : "\nReview: no extension-based warning. This does NOT prove the file is safe.")
                    + "\nNo upload, modification, or deletion was performed.";
            runOnUiThread(() -> {
                heading("Selected file result");
                content.addView(text(result, 13, risky ? 0xFFFFD28A : 0xFFB7C2D8));
                status.setText("Local file analysis finished.");
            });
        } catch (Exception e) {
            runOnUiThread(() -> status.setText("File check failed: " + e.getMessage()));
        }
    }

    private void showLimits() {
        heading("What this version can and cannot do");
        content.addView(text("Can: list visible installed apps, review declared permissions, and hash a file you select.\n\nCannot: inspect other apps' private data, prove spyware is absent, monitor every runtime action, or remove malware. Android's app sandbox restricts these capabilities.\n\nNo files are deleted. Findings are informational and require human review.", 14, 0xFFB7C2D8));
    }
}
