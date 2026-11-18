use jni::objects::{JByteArray, JClass};
use jni::sys::jstring;
use jni::JNIEnv;
use std::time::Duration;
use yara_x::{Compiler, Scanner};

const MAX_SCAN_BYTES: usize = 32 * 1024 * 1024;

#[no_mangle]
pub extern "system" fn Java_com_sentinelai_mobile_YaraEngine_scanBytes(
    mut env: JNIEnv,
    _class: JClass,
    input: JByteArray,
) -> jstring {
    let result = scan(&mut env, input);
    match env.new_string(result) {
        Ok(value) => value.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}

fn scan(env: &mut JNIEnv, input: JByteArray) -> String {
    let bytes = match env.convert_byte_array(input) {
        Ok(value) => value,
        Err(_) => return "YARA-X error: unable to read file bytes".to_string(),
    };

    if bytes.len() > MAX_SCAN_BYTES {
        return "YARA-X skipped: file exceeds the 32 MiB native scan limit".to_string();
    }

    let mut compiler = Compiler::new();
    if let Err(error) = compiler.add_source(include_str!("../rules/starter_rules.yar")) {
        return format!("YARA-X rule compilation error: {error}");
    }

    let rules = compiler.build();
    let mut scanner = Scanner::new(&rules);
    scanner.set_timeout(Duration::from_secs(3));
    scanner.max_scan_size(MAX_SCAN_BYTES);

    match scanner.scan(&bytes) {
        Ok(results) => {
            let names: Vec<String> = results
                .matching_rules()
                .map(|rule| rule.identifier().to_string())
                .collect();

            if names.is_empty() {
                format!("YARA-X active; no starter rule matched ({} bytes scanned)", bytes.len())
            } else {
                format!("YARA-X MATCH: {} ({} bytes scanned)", names.join(", "), bytes.len())
            }
        }
        Err(error) => format!("YARA-X scan error: {error}"),
    }
}
