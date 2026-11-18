package com.sentinelai.mobile;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.io.File;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MainActivity extends Activity {
    private static final int PICK_FILE = 41;
    private static final long MAX_FILE_BYTES = 100L * 1024L * 1024L;
    private static final int MAX_ARCHIVE_ENTRIES = 3000;

    private static final int BG = 0xFF071321;
    private static final int PANEL = 0xFF102538;
    private static final int PANEL_ALT = 0xFF142D42;
    private static final int TEXT = 0xFFF4F8FC;
    private static final int MUTED = 0xFFA9BACB;
    private static final int CYAN = 0xFF39D6D0;
    private static final int GREEN = 0xFF79E0B2;
    private static final int AMBER = 0xFFFFD18A;

    private LinearLayout content;
    private LinearLayout resultsContainer;
    private TextView status;
    private ProgressBar progress;
    private Button appsButton;
    private Button fileButton;
    private File pendingRestoreFile;

    private static final String[] REVIEW_PERMISSIONS = {
        "android.permission.RECORD_AUDIO", "android.permission.CAMERA",
        "android.permission.READ_SMS", "android.permission.RECEIVE_SMS",
        "android.permission.READ_CONTACTS", "android.permission.READ_CALL_LOG",
        "android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_BACKGROUND_LOCATION",
        "android.permission.READ_PHONE_STATE", "android.permission.REQUEST_INSTALL_PACKAGES",
        "android.permission.SYSTEM_ALERT_WINDOW"
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        buildUi();
    }

    private GradientDrawable shape(int color, int radiusDp, int strokeColor) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radiusDp * getResources().getDisplayMetrics().density);
        if (strokeColor != 0) d.setStroke((int)(1 * getResources().getDisplayMetrics().density), strokeColor);
        return d;
    }

    private int dp(int value) {
        return (int)(value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private LinearLayout column() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private LinearLayout row() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    private TextView label(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setIncludeFontPadding(true);
        return t;
    }

    private void gap(LinearLayout parent, int height) {
        View v = new View(this);
        parent.addView(v, new LinearLayout.LayoutParams(1, dp(height)));
    }

    private LinearLayout card() {
        LinearLayout c = column();
        c.setPadding(dp(18), dp(16), dp(18), dp(16));
        c.setBackground(shape(PANEL, 20, 0xFF244258));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.bottomMargin = dp(14);
        content.addView(c, p);
        return c;
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        content = column();
        content.setPadding(dp(20), dp(22), dp(20), dp(30));
        scroll.addView(content);
        setContentView(scroll);

        LinearLayout top = row();
        TextView brand = label("SENTINEL", 27, TEXT, true);
        top.addView(brand);
        TextView ai = label("  AI", 27, CYAN, true);
        top.addView(ai);
        content.addView(top);
        gap(content, 3);
        content.addView(label("MOBILE SECURITY  •  LOCAL-FIRST", 11, MUTED, true));
        gap(content, 18);

        LinearLayout statusCard = card();
        LinearLayout statusRow = row();
        TextView dot = label("●", 17, GREEN, true);
        statusRow.addView(dot);
        TextView statusTitle = label("  DEVICE CHECKS READY", 12, GREEN, true);
        statusRow.addView(statusTitle);
        statusCard.addView(statusRow);
        gap(statusCard, 7);
        status = label("Your scans run on this phone. Files are not uploaded.", 13, MUTED, false);
        status.setLineSpacing(dp(2), 1f);
        statusCard.addView(status);

        LinearLayout section = row();
        TextView sectionTitle = label("Security centre", 21, TEXT, true);
        section.addView(sectionTitle);
        content.addView(section);
        gap(content, 5);
        content.addView(label("Choose a check to run. Results explain what was inspected.", 13, MUTED, false));
        gap(content, 14);

        LinearLayout scanCard = card();
        scanCard.setBackground(shape(PANEL_ALT, 22, 0xFF2C6374));
        scanCard.addView(label("QUICK ACTIONS", 11, CYAN, true));
        gap(scanCard, 8);
        appsButton = actionButton("Scan installed apps", "Review visible apps and selected sensitive permission declarations", true, v -> scanApps());
        scanCard.addView(appsButton);
        gap(scanCard, 10);
        fileButton = actionButton("Analyze a file", "Check a file you choose: SHA-256, YARA-X and archive structure", false, v -> pickFile());
        scanCard.addView(fileButton);

        LinearLayout monitorCard = card();
        monitorCard.addView(label("PROTECTION TOOLS", 11, CYAN, true));
        gap(monitorCard, 8);
        monitorCard.addView(actionButton("Enable periodic review", "Permission-declaration checks; Android may delay background work", false, v -> enableMonitoring()));
        gap(monitorCard, 8);
        monitorCard.addView(actionButton("Restore quarantined copy", "Export a held copy to a location you choose", false, v -> chooseQuarantineCopy()));

        LinearLayout info = card();
        info.addView(label("WHAT THIS VERSION CAN CHECK", 11, CYAN, true));
        gap(info, 10);
        addCapability(info, "01", "App permission review", "Find apps declaring selected sensitive permissions.");
        addCapability(info, "02", "Local file analysis", "Calculate SHA-256 and inspect selected files.");
        addCapability(info, "03", "YARA-X test rules", "Run the bundled validation rules on supported files.");
        addCapability(info, "04", "ZIP / APK structure", "Check basic archive structure and suspicious paths.");

        LinearLayout privacy = card();
        privacy.addView(label("PRIVATE BY DESIGN", 14, TEXT, true));
        gap(5);
        privacy.addView(label("No account • No cloud upload • No automatic deletion", 12, MUTED, false));
        gap(8);
        privacy.addView(label("A clean result does not prove a device or file is malware-free. Android limits what a normal app can inspect.", 12, MUTED, false));

        TextView limits = label("Detection limits & app information  →", 13, CYAN, true);
        limits.setPadding(0, dp(8), 0, dp(18));
        limits.setOnClickListener(v -> showLimits());
        content.addView(limits);

        TextView resultHeading = label("LATEST RESULT", 11, CYAN, true);
        content.addView(resultHeading);
        gap(content, 8);
        resultsContainer = column();
        content.addView(resultsContainer);
        resultsContainer.addView(label("No scan run yet. Start with one of the checks above.", 13, MUTED, false));
    }

    private void addCapability(LinearLayout parent, String number, String title, String detail) {
        LinearLayout r = row();
        TextView n = label(number, 12, CYAN, true);
        r.addView(n, new LinearLayout.LayoutParams(dp(34), -2));
        LinearLayout c = column();
        c.addView(label(title, 14, TEXT, true));
        c.addView(label(detail, 12, MUTED, false));
        r.addView(c, new LinearLayout.LayoutParams(0, -2, 1));
        parent.addView(r);
        gap(parent, 13);
    }

    private Button actionButton(String title, String detail, boolean primary, View.OnClickListener listener) {
        Button b = new Button(this);
        b.setText(title + "\n" + detail);
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setTextColor(primary ? 0xFF06202A : TEXT);
        b.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
        b.setPadding(dp(16), dp(9), dp(16), dp(9));
        b.setMinHeight(dp(68));
        b.setBackground(shape(primary ? CYAN : 0xFF203B50, 16, primary ? 0 : 0xFF31546A));
        b.setOnClickListener(listener);
        return b;
    }

    private void setBusy(String message) {
        status.setText(message);
        if (progress == null) {
            progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            progress.setIndeterminate(true);
            progress.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(CYAN));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(4));
            p.topMargin = dp(10);
            content.addView(progress, Math.max(0, content.indexOfChild(resultsContainer) - 1), p);
        }
        progress.setVisibility(View.VISIBLE);
        if (appsButton != null) appsButton.setEnabled(false);
        if (fileButton != null) fileButton.setEnabled(false);
    }

    private void finishBusy(String message) {
        status.setText(message);
        if (progress != null) progress.setVisibility(View.GONE);
        if (appsButton != null) appsButton.setEnabled(true);
        if (fileButton != null) fileButton.setEnabled(true);
    }

    private void showResult(String title, String body, boolean warning) {
        resultsContainer.removeAllViews();
        LinearLayout c = column();
        c.setPadding(dp(16), dp(14), dp(16), dp(14));
        c.setBackground(shape(PANEL, 16, warning ? 0xFF8B7045 : 0xFF31546A));
        c.addView(label(title, 15, warning ? AMBER : GREEN, true));
        gap(c, 8);
        TextView detail = label(body, 12, TEXT, false);
        detail.setTextIsSelectable(true);
        detail.setLineSpacing(dp(3), 1f);
        c.addView(detail);
        resultsContainer.addView(c);
    }

    private void scanApps() {
        setBusy("Reviewing installed app declarations…");
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
                    PermissionRiskAnalyzer.Result risk = PermissionRiskAnalyzer.analyze(info);
                    List<String> matched = new ArrayList<>();
                    for (String permission : info.requestedPermissions) {
                        for (String sensitive : REVIEW_PERMISSIONS) {
                            if (sensitive.equals(permission)) matched.add(permission.substring(permission.lastIndexOf('.') + 1));
                        }
                    }
                    if (!matched.isEmpty()) {
                        String appLabel;
                        try { appLabel = pm.getApplicationLabel(info.applicationInfo).toString(); }
                        catch (Exception e) { appLabel = info.packageName; }
                        findings.add(appLabel + " (" + info.packageName + ")\nDeclared: " + join(matched)
                                + "\nEvidence review score: " + risk.score + "/100 (" + risk.level + ")"
                                + "\nThis score counts selected declarations only; it is not a spyware verdict.");
                    }
                }
                Collections.sort(findings);
                StringBuilder report = new StringBuilder();
                report.append("Visible packages checked: ").append(packages.size())
                      .append("\nApps needing permission review: ").append(findings.size());
                for (String item : findings) report.append("\n\n").append(item);
                if (findings.isEmpty()) report.append("\nNo matching declarations found in the visible app list.");
                report.append("\n\nThis check reviews declarations only. It does not inspect private app data or actual runtime use.");
                String result = report.toString();
                runOnUiThread(() -> {
                    finishBusy("App permission review complete.");
                    showResult("App review complete", result, !findings.isEmpty());
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    finishBusy("App review could not be completed.");
                    showResult("Scan unavailable", e.getClass().getSimpleName() + ": " + String.valueOf(e.getMessage()), true);
                });
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
        if (requestCode == 42 && resultCode == RESULT_OK && data != null && data.getData() != null && pendingRestoreFile != null) {
            File file = pendingRestoreFile;
            pendingRestoreFile = null;
            try {
                QuarantineStore.restore(this, file, data.getData());
                Toast.makeText(this, "Copy restored. Private quarantine copy retained.", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(this, "Restore failed: " + e.getClass().getSimpleName(), Toast.LENGTH_LONG).show();
            }
            return;
        }
        if (requestCode != PICK_FILE || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        setBusy("Analyzing your selected file locally…");
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
            int firstByte = -1, secondByte = -1;
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
            boolean apkName = lower.endsWith(".apk"), zipName = lower.endsWith(".zip");
            List<String> findings = new ArrayList<>();
            if (apkName && !zipMagic) findings.add("APK extension but ZIP signature not found.");
            if (zipName && !zipMagic) findings.add("ZIP extension but ZIP signature not found.");
            if (!apkName && !zipName && zipMagic) findings.add("ZIP content found with a different extension.");
            int entries = 0;
            boolean hasManifest = false, hasDex = false, traversal = false, nestedExecutable = false;
            long expandedBytes = 0;
            if (zipMagic) {
                try (InputStream raw = getContentResolver().openInputStream(uri)) {
                    if (raw == null) throw new IllegalStateException("Cannot reopen selected file");
                    try (ZipInputStream zip = new ZipInputStream(new BufferedInputStream(raw))) {
                        ZipEntry entry;
                        while ((entry = zip.getNextEntry()) != null) {
                            entries++;
                            if (entries > MAX_ARCHIVE_ENTRIES) {
                                findings.add("Archive entry limit exceeded; deeper inspection stopped.");
                                break;
                            }
                            String path = entry.getName().replace('\\', '/');
                            if (path.startsWith("/") || path.contains("../") || path.equals("..")) traversal = true;
                            String pathLower = path.toLowerCase(Locale.ROOT);
                            if (pathLower.equals("androidmanifest.xml")) hasManifest = true;
                            if (pathLower.matches("classes([0-9]+)?\\.dex")) hasDex = true;
                            if (pathLower.endsWith(".apk") || pathLower.endsWith(".dex") || pathLower.endsWith(".jar")) nestedExecutable = true;
                            int read;
                            while ((read = zip.read(buffer)) != -1) {
                                expandedBytes += read;
                                if (expandedBytes > MAX_FILE_BYTES) {
                                    findings.add("Expanded archive data exceeded 100 MB; inspection stopped.");
                                    break;
                                }
                            }
                            zip.closeEntry();
                            if (expandedBytes > MAX_FILE_BYTES) break;
                        }
                    }
                } catch (Exception archiveError) {
                    findings.add("Archive could not be fully inspected: " + archiveError.getClass().getSimpleName());
                }
                if (traversal) findings.add("Archive contains a path-traversal-like entry name.");
                if (nestedExecutable) findings.add("Archive contains nested executable or application package files.");
                if (apkName && hasManifest && hasDex) findings.add("APK-like structure found; this is not a malware verdict.");
                else if (apkName) findings.add("APK structure was not fully confirmed by the basic archive check.");
                findings.add("Archive entries inspected: " + entries);
            }

            StringBuilder result = new StringBuilder();
            result.append("File: ").append(name).append("\nSize: ").append(total).append(" bytes")
                  .append("\nSHA-256: ").append(hex)
                  .append("\nLocal hash indicators: ").append(signatureMatch == null ? "No exact match" : "MATCH — " + signatureMatch)
                  .append("\nYARA-X: ").append(yaraResult)
                  .append("\nStructural indicators: ").append(findings.size());
            for (String finding : findings) result.append("\n• ").append(finding);
            if (findings.isEmpty()) result.append("\nNo basic structural warning found. This does not prove the file is safe.");
            result.append("\n\nNo upload, modification or deletion was performed.");
            String finalResult = result.toString();
            runOnUiThread(() -> {
                finishBusy("File analysis finished on this device.");
                showResult("File analysis result", finalResult, !findings.isEmpty() || (signatureMatch != null));
                addQuarantineAction(uri, name);
            });
        } catch (Exception e) {
            runOnUiThread(() -> {
                finishBusy("File analysis could not be completed.");
                showResult("File check failed", e.getClass().getSimpleName() + ": " + String.valueOf(e.getMessage()), true);
            });
        }
    }


    private void enableMonitoring() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 77);
            Toast.makeText(this, "Allow notifications, then tap Enable periodic review again.", Toast.LENGTH_LONG).show();
            return;
        }
        new AlertDialog.Builder(this).setTitle("Enable periodic review?")
            .setMessage("Sentinel periodically reviews visible installed-app permission declarations and may send a notification. Android can delay this work. It does not monitor every app action.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Enable", (dialog, which) -> {
                PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(ThreatMonitorWorker.class, 15, TimeUnit.MINUTES).build();
                WorkManager.getInstance(this).enqueueUniquePeriodicWork("sentinel-periodic-review", ExistingPeriodicWorkPolicy.UPDATE, request);
                Toast.makeText(this, "Periodic review enabled. Android may delay checks.", Toast.LENGTH_LONG).show();
            }).show();
    }

    private void addQuarantineAction(Uri source, String name) {
        Button b = actionButton("Quarantine a copy", "Save a private copy; original will NOT be deleted", false, v ->
            new AlertDialog.Builder(this).setTitle("Save quarantine copy?")
                .setMessage("This copies the selected file into Sentinel private storage. The original stays where it is. This is not OS-level malware isolation. Continue?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save copy", (dialog, which) -> new Thread(() -> {
                    try {
                        File saved = QuarantineStore.copyIntoQuarantine(this, source, name);
                        runOnUiThread(() -> {
                            finishBusy("Private copy saved.");
                            showResult("Quarantine copy saved", "Stored privately: " + saved.getName() + "\\nOriginal file was not moved or deleted. Use Restore quarantined copy to export it. Uninstalling Sentinel removes this copy.", false);
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> {
                            finishBusy("Could not save copy.");
                            showResult("Quarantine failed", e.getClass().getSimpleName() + ": " + String.valueOf(e.getMessage()), true);
                        });
                    }
                }).start()).show());
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(10);
        resultsContainer.addView(b, p);
    }

    private void chooseQuarantineCopy() {
        File dir = new File(getFilesDir(), "quarantine");
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            Toast.makeText(this, "No quarantined copies found.", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] names = new String[files.length];
        for (int i = 0; i < files.length; i++) names[i] = files[i].getName();
        new AlertDialog.Builder(this).setTitle("Choose a copy to restore").setItems(names, (dialog, which) -> {
            pendingRestoreFile = files[which];
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_TITLE, files[which].getName());
            startActivityForResult(intent, 42);
        }).setNegativeButton("Cancel", null).show();
    }

    private void showLimits() {
        resultsContainer.removeAllViews();
        String limits = "CURRENT CAPABILITIES\n• Visible installed-app inventory and selected permission-declaration review.\n• User-selected file SHA-256 and exact local hash matching.\n• Bundled YARA-X validation rules for files up to 32 MiB.\n• Basic ZIP/APK structure inspection.\n\nIMPORTANT LIMITS\n• The bundled hash database currently contains only the harmless EICAR test-file indicator.\n• Starter YARA rules are validation rules, not a production malware rule pack.\n• EICAR is a harmless antivirus test pattern, not malware.\n• Android sandboxing prevents this normal app from inspecting other apps' private data or all runtime behavior.\n• Permission declarations are review indicators, not proof of spying.\n• No real-time background threat monitor, quarantine, AI model, cloud threat feed or enterprise SOC backend is implemented yet.\n\nPrivacy: no account, no upload and no automatic deletion. Findings require human review.";
        showResult("Detection limits & app information", limits, false);
    }
}
