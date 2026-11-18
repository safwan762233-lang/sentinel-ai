package com.sentinelai.mobile;

import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic offline evidence correlation. This is not machine learning or an AI model.
 * It produces explainable review guidance from indicators already collected by Sentinel.
 */
final class ThreatIntelligenceEngine {
    static final class Assessment {
        final String level;
        final List<String> evidence;
        Assessment(String level, List<String> evidence) { this.level = level; this.evidence = evidence; }
    }
    private ThreatIntelligenceEngine() {}
    static Assessment assess(int permissionScore, boolean hashMatch, String yaraOutput, int structuralIndicators) {
        List<String> evidence = new ArrayList<>();
        if (hashMatch) evidence.add("Exact local SHA-256 indicator matched");
        if (yaraOutput != null && yaraOutput.contains("YARA-X MATCH")) evidence.add("A bundled YARA-X rule matched");
        if (permissionScore >= 48) evidence.add("Multiple sensitive permission declarations were found");
        if (structuralIndicators > 0) evidence.add(structuralIndicators + " archive/format indicator(s) found");
        int points = (hashMatch ? 100 : 0) + (yaraOutput != null && yaraOutput.contains("YARA-X MATCH") ? 60 : 0)
                + permissionScore + Math.min(30, structuralIndicators * 10);
        String level = points >= 100 ? "Strong indicator — investigate" : points >= 48 ? "Review recommended" : "Insufficient evidence for a high-risk assessment";
        if (evidence.isEmpty()) evidence.add("No configured indicator matched; this does not prove the device or file is safe.");
        return new Assessment(level, evidence);
    }
}
