# Sentinel AI

**Mobile-first, privacy-focused security toolkit** — Android app first, enterprise services later.

> **Status:** early MVP scaffold. This is not a certified antivirus, EDR, or spyware-removal product. Findings are heuristic indicators and must not be treated as proof of malware.

## Current working features
- Inventory installed apps visible through Android's package visibility API.
- Review selected high-impact permission declarations and explain why they deserve review.
- Select a file through Android's Storage Access Framework and calculate SHA-256 locally.
- Show a limited warning for executable/script-like file extensions. An extension alone does not establish maliciousness.
- No account, no upload, no automatic deletion.

## Get the Android debug APK
1. Open the repository's **Actions** tab.
2. Select the latest successful **Android debug build** workflow run.
3. Download the `sentinel-ai-debug-apk` artifact.
4. Extract the ZIP and install the APK on your Android phone. Android may ask you to allow installation from that browser/file manager.

The debug APK is for testing, not a production-signed release. Do not distribute it as an enterprise security product.

## Build locally
Requirements: JDK 17, Android SDK Platform 35, and Gradle 8.9.

```bash
gradle assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`.

## Important Android limits
- Android sandboxing prevents a normal app from inspecting other apps' private data or reliably observing all runtime behavior.
- A declared permission is a review indicator, not evidence of spying.
- The file picker only grants access to files the user selects.
- This MVP does not delete files, kill apps, inspect encrypted app data, or guarantee detection.
- `QUERY_ALL_PACKAGES` is a sensitive permission subject to Google Play policy review. Distribution may need a narrower app-visibility design.
- Enterprise EDR/SIEM capabilities require separately deployed, authorized agents and backend services.

## Roadmap
1. Android app inventory and permission review (MVP).
2. Signed local hash/signature intelligence packs and safe quarantine workflow.
3. Behavior signals supported by Android APIs, with explicit consent.
4. Enterprise backend, device enrollment, RBAC, audit trail, alert pipeline.
5. Independent security review, privacy review, and production release signing.

## Open-source policy
Dependencies and engines must be reviewed for license, provenance, maintenance, and security before integration. Third-party components retain their original licenses and notices.
