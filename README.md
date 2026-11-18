# Sentinel AI

**Mobile-first, privacy-focused security toolkit** — Android app first, enterprise services later.

> **Status:** early MVP. This is not a certified antivirus, EDR, or spyware-removal product. Findings are heuristic indicators and are not proof of malware.

## Current Android features
- Inventory installed apps visible through Android's package visibility API.
- Review selected high-impact permission declarations.
- Select a file through Android's Storage Access Framework and calculate SHA-256 locally.
- Check basic ZIP/APK archive structure, including extension/signature mismatch, APK-like manifest/DEX entries, nested executable archives, and path-traversal-like names.
- Apply file size and archive-entry limits to reduce resource-exhaustion risk.
- No account, no upload, no automatic deletion.

## Get the Android debug APK
1. Open the repository's **Actions** tab.
2. Select the latest successful **Android debug build** workflow run.
3. Download the `sentinel-ai-debug-apk` artifact.
4. Extract the ZIP and install the APK on your Android phone.

The debug APK is for testing, not a production-signed release. Do not distribute it as an enterprise security product.

## Build locally
Requirements: JDK 17, Android SDK Platform 35, and Gradle 8.9.

```bash
gradle assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`.

## Important Android and detection limits
- Android sandboxing prevents a normal app from inspecting other apps' private data or reliably observing all runtime behavior.
- A declared permission is a review indicator, not evidence of spying.
- The file picker only grants access to files the user selects.
- ZIP/APK structure checks are heuristic. A valid-looking archive may still be malicious; an unusual archive is not automatically malicious.
- The starter hash pack currently contains only the EICAR antivirus test-file hash. EICAR is a harmless test pattern, not malware; this validates the matching engine but is not a real malware threat feed. The app has no dynamic sandbox, cloud reputation lookup, or guaranteed detection.
- `QUERY_ALL_PACKAGES` is sensitive and subject to Google Play policy review. Distribution may need a narrower app-visibility design.
- Enterprise EDR/SIEM capabilities require separately deployed, authorized agents and backend services.

## Roadmap
1. Android app inventory and local file structure checks (current MVP).
2. Expand the hash/signature intelligence pack using vetted, licensed indicators; add pack authenticity/expiry checks and a user-approved safe quarantine workflow.
3. Behavior signals supported by Android APIs, with explicit consent.
4. Enterprise backend, device enrollment, RBAC, audit trail, alert pipeline.
5. Independent security review, privacy review, and production release signing.

## Open-source policy
Dependencies and engines must be reviewed for license, provenance, maintenance, and security before integration. Third-party components retain their original licenses and notices.
