// Sentinel AI starter rules.
// These are harmless validation rules, not production malware signatures.
// Expand only with vetted rules and document each rule's source and license.

rule Sentinel_EICAR_Antivirus_Test_Pattern
{
    meta:
        description = "Recognizes the harmless EICAR antivirus test string"
        classification = "test_only"
        severity = "informational"
        reference = "https://www.eicar.org/download-anti-malware-testfile/"

    strings:
        $eicar = "X5O!P%@AP[4\\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*"

    condition:
        $eicar
}

rule Sentinel_Internal_YARA_Validation_Marker
{
    meta:
        description = "Harmless marker used to validate the YARA-X engine"
        classification = "test_only"
        severity = "informational"

    strings:
        $marker = "SENTINEL_YARA_TEST_PATTERN"

    condition:
        $marker
}
