# Sentinel AI

**Mobile-first, privacy-focused security toolkit** — Android app first, enterprise services later.

> **Status:** early MVP. This is not a certified antivirus, EDR, or spyware-removal product. Findings are heuristic indicators and are not proof of malware.

## Current Android features
- Redesigned mobile-first dashboard with clear scan actions, readable status, and a dedicated latest-result panel. It intentionally avoids fabricated security scores or threat counters.
- Inventory installed apps visible through Android's package visibility API.
- Review selected high-impact permission declarations and produce an explainable indicator score from declarations only (not a spyware verdict).
- Select a file through Android's Storage Access Framework and calculate SHA-256 locally.
- Run the bundled YARA-X native engine against user-selected files up to 32 MiB using starter validation rules (EICAR test string and a harmless internal marker). These are test rules, not production malware signatures.
- Check basic ZIP/APK archive structure, including extension/signature mismatch, APK-like manifest/DEX entries, nested executable archives, and path-traversal-like names.
- Apply file size and archive-entry limits to reduce resource-exhaustion risk.
- Opt-in periodic permission-declaration review using Android WorkManager; Android may defer execution and this is not real-time runtime monitoring.
- User-confirmed quarantine COPY into app-private storage, with user-selected restore/export. The original is never removed; this is not OS-level isolation, and uninstalling the app removes the private copy.
- Deterministic offline evidence correlation combines configured hash/YARA/structure indicators into an explainable review assessment. This is rules-based logic, not a trained AI model.
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
- [x] Explainable spyware-risk review indicators based on declared permissions only; no runtime spyware detection claim.
- [x] User-selected file SHA-256 and basic ZIP/APK structure inspection.
- [x] YARA-X native integration with harmless starter validation rules (not a production malware rule pack).
- [x] User-approved quarantine COPY to app-private storage and user-selected restore/export. Original files are never deleted; private copies are removed if the app is uninstalled.
- [x] Opt-in periodic permission-declaration review using WorkManager. Android battery/background limits may delay checks; it does not inspect all app behavior.
- [x] Local deterministic evidence correlation for explainable risk assessment. This is not a trained AI model or self-learning threat intelligence.
- [ ] Production-grade malware signatures and YARA rules from vetted, licensed sources; signed rule-pack updates, expiry and provenance checks.
- [ ] Actual on-device AI model and validated threat-intelligence ingestion pipeline. No ML model or live threat feed is shipped.
- [ ] Enterprise SOC dashboard, server API, tenant isolation, RBAC, audit trail, alert pipeline and secure device enrollment. Requires a separately deployed and secured backend.
- [ ] Enterprise device management and incident response agent. Full device controls require Android Enterprise managed-device/device-owner provisioning and organizational authorization; this consumer app does not have those privileges.
- [ ] Independent security/privacy review and production release signing.

Features marked [ ] are not implemented and must not be represented as available.
## Open-source policy
Dependencies and engines must be reviewed for license, provenance, maintenance, and security before integration. Third-party components retain their original licenses and notices.
