package com.sentinelai.mobile;

import android.content.pm.PackageInfo;
import java.util.ArrayList;
import java.util.List;

/** Explainable indicator scoring from declared permissions only; never a spyware verdict. */
final class PermissionRiskAnalyzer {
    static final class Result {
        final int score;
        final String level;
        final List<String> evidence;
        Result(int score, String level, List<String> evidence) {
            this.score = score; this.level = level; this.evidence = evidence;
        }
    }
    private PermissionRiskAnalyzer() {}
    static Result analyze(PackageInfo info) {
        List<String> evidence = new ArrayList<>();
        if (info.requestedPermissions != null) {
            for (String p : info.requestedPermissions) {
                if (p == null) continue;
                if (p.endsWith("RECORD_AUDIO")) evidence.add("Microphone access declared");
                else if (p.endsWith("CAMERA")) evidence.add("Camera access declared");
                else if (p.endsWith("READ_SMS") || p.endsWith("RECEIVE_SMS")) evidence.add("SMS access declared");
                else if (p.endsWith("READ_CONTACTS")) evidence.add("Contacts access declared");
                else if (p.endsWith("READ_CALL_LOG")) evidence.add("Call-log access declared");
                else if (p.endsWith("ACCESS_FINE_LOCATION") || p.endsWith("ACCESS_BACKGROUND_LOCATION")) evidence.add("Precise/background location declared");
                else if (p.endsWith("REQUEST_INSTALL_PACKAGES")) evidence.add("Can request installation of other packages");
                else if (p.endsWith("SYSTEM_ALERT_WINDOW")) evidence.add("Can request display-over-other-apps capability");
            }
        }
        int score = Math.min(100, evidence.size() * 12);
        String level = score >= 48 ? "Elevated review priority" : score >= 24 ? "Review recommended" : evidence.isEmpty() ? "No selected indicators" : "Low indicator count";
        return new Result(score, level, evidence);
    }
}
