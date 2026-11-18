package com.sentinelai.mobile;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.util.Scanner;

/** Exact SHA-256 matching against the bundled local indicator pack. */
final class LocalHashSignatures {
    private LocalHashSignatures() { }

    static String lookup(Context context, String sha256) {
        try (InputStream input = context.getAssets().open("hash-signatures.json");
             Scanner scanner = new Scanner(input, "UTF-8").useDelimiter("\\A")) {
            String json = scanner.hasNext() ? scanner.next() : "{}";
            JSONObject root = new JSONObject(json);
            JSONArray entries = root.getJSONArray("signatures");
            for (int i = 0; i < entries.length(); i++) {
                JSONObject entry = entries.getJSONObject(i);
                if (entry.optString("sha256", "").equalsIgnoreCase(sha256)) {
                    return entry.optString("name", "Unnamed indicator")
                            + " [" + entry.optString("classification", "unknown") + "] — "
                            + entry.optString("note", "Exact SHA-256 match");
                }
            }
        } catch (Exception ignored) {
            // Missing or malformed local intelligence must not crash file analysis.
        }
        return null;
    }
}
