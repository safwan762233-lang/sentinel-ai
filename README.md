# Sentinel AI

**Mobile-first, privacy-focused security toolkit** — Android app first, enterprise services later.

> **Status:** early MVP. This is not a certified antivirus, EDR, or spyware-removal product. Findings are heuristic indicators and are not proof of malware.

## Current Android features
- Redesigned mobile-first dashboard with clear scan actions, readable status, and a dedicated latest-result panel. It intentionally avoids fabricated security scores or threat counters.
- Inventory installed apps visible through Android's package visibility API.
- Review selected high-impact permission declarations.
- Select a file through Android's Storage Access Framework and calculate SHA-256 locally.
- Run the bundled YARA-X native engine against user-selected files up to 32 MiB using starter validation rules (EICAR test string and a harmless internal marker). These are test rules, not production malware signatures.
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
Requirements: JDK 17, Android SDK Platform 35, Android NDK 27.2.12479018, Rust stable with Android targets, cargo-ndk, and Gradle 8.9.

```bash
gradle assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`.

## Important Android and detection limits
- Android sandboxing prevents a normal app from inspecting other apps' private data or reliably observing all runtime behavior.
- A declared permission is a review indicator, not evidence of spying.
- The file picker only grants access to files the user selects.
- ZIP/APK structure checks are heuristic. A valid-looking archive may still be malicious; an unusual archive is not automatically malicious.
- The starter YARA-X pack currently contains only harmless validation rules (EICAR test string and a Sentinel marker); it is not a real malware rule pack. YARA-X is a separate engine from classic YARA, designed for high compatibility, not perfect compatibility.
- The starter hash pack currently contains only the EICAR antivirus test-file hash. EICAR is a harmless test pattern, not malware; this validates the matching engine but is not a real malware threat feed. The app has no dynamic sandbox, cloud reputation lookup, or guaranteed detection.
- `QUERY_ALL_PACKAGES` is sensitive and subject to Google Play policy review. Distribution may need a narrower app-visibility design.
- Enterprise EDR/SIEM capabilities require separately deployed, authorized agents and backend services.

## Roadmap and implementation status
- [x] Android app inventory and selected permission-declaration review.
- [x] User-selected file SHA-256 and basic ZIP/APK structure inspection.
- [x] YARA-X native integration with starter validation rules (not production threat rules).
- [x] Mobile dashboard and scan-result UI refresh.
- [ ] Production-grade malware signature and YARA rule packs from vetted, licensed sources; signed updates and expiry checks.
- [ ] Spyware risk review based only on Android-supported, consented signals; no claims to inspect other apps' private data.
- [ ] User-approved quarantine for files the app can access, with restore and integrity checks. No automatic deletion.
- [ ] Background monitoring using Android-supported APIs and foreground-service restrictions; requires battery/privacy design.
- [ ] Actual on-device threat-intelligence model or clearly defined rules-based analysis. No AI model is currently shipped.
- [ ] Enterprise SOC dashboard, device enrollment, RBAC, audit trail, alert pipeline and authorized device-management agent. This requires a separately deployed backend and managed-device permissions.
- [ ] Independent security/privacy review and production release signing.

Features marked [ ] are not implemented yet and must not be represented as available in the app.

## Open-source policy
Dependencies and engines must be reviewed for license, provenance, maintenance, and security before integration. Third-party components retain their original licenses and notices.
