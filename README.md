# Sentinel AI

**Mobile-first, privacy-focused security toolkit** — Android app first, enterprise services later.

> Current status: early MVP scaffold. This is not a certified antivirus, EDR, or spyware-removal product. Findings are heuristic indicators and must not be treated as proof of malware.

## MVP features
- Inventory installed apps using Android's package visibility API.
- Review declared high-impact permissions and explain why they matter.
- Select a file through Android's Storage Access Framework and calculate SHA-256.
- Show transparent heuristic warnings for suspicious file extensions. No file is deleted or modified.
- Offline operation for the implemented checks.

## Android build
Requirements: JDK 17 and Android SDK Platform 35.

```bash
./gradlew assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`.

On Windows use `gradlew.bat assembleDebug`. GitHub Actions builds the debug APK on pushes and pull requests.

## Important Android limits
- Android sandboxing prevents a normal app from inspecting other apps' private data or reliably observing all their runtime behavior.
- A declared permission is a risk indicator, not evidence of spying.
- The file picker only grants access to files the user selects.
- This MVP does not delete files, kill apps, inspect encrypted app data, or guarantee detection.
- Enterprise EDR/SIEM capabilities require separately deployed, authorized agents and backend services.

## Roadmap
1. Android app inventory and permission review (current).
2. Signed local hash/signature intelligence packs and safe quarantine workflow.
3. Behavior signals supported by Android APIs, with explicit consent.
4. Enterprise backend, device enrollment, RBAC, audit trail, alert pipeline.
5. Independent security review, privacy review, and release signing.

## Open-source policy
Dependencies and engines must be reviewed for license, provenance, maintenance, and security before integration. Third-party components retain their original licenses and notices.
