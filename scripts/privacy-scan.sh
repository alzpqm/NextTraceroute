#!/usr/bin/env bash

set -euo pipefail

repo_root="$(git rev-parse --show-toplevel)"
cd "$repo_root"

failed=0

fail_with_matches() {
    local title="$1"
    local matches="$2"
    if [[ -n "$matches" ]]; then
        printf 'FAIL: %s\n%s\n' "$title" "$matches"
        failed=1
    else
        printf 'PASS: %s\n' "$title"
    fi
}

# ripgrep observes .gitignore, so local-only files such as local.properties and build
# outputs are excluded from source checks. Release artifacts are inspected separately.
local_paths="$(
    rg -n -i --hidden \
        --glob '!.git/**' \
        --glob '!scripts/privacy-scan.sh' \
        '(/Users/[^/[:space:]]+|/home/[^/[:space:]]+|[A-Z]:\\Users\\[^\\[:space:]]+)' \
        . || true
)"
fail_with_matches "no local user paths in publishable source" "$local_paths"

private_material="$(
    rg -n -i --hidden \
        --glob '!.git/**' \
        --glob '!scripts/privacy-scan.sh' \
        'BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY|BEGIN PRIVATE KEY' \
        . || true
)"
fail_with_matches "no private key material in publishable source" "$private_material"

sensitive_files="$(
    rg --files --hidden --glob '!.git/**' \
        | rg -i '(^|/)(\.env($|\.)|[^/]+\.(jks|keystore|p12|pfx|pem|key))$' \
        || true
)"
fail_with_matches "no credential or signing files are publishable" "$sensitive_files"

duplicate_files="$(
    rg --files --hidden --glob '!.git/**' \
        | rg '(^|/).+ [0-9]+\.[^/]+$' \
        || true
)"
fail_with_matches "no sync-conflict duplicate files are publishable" "$duplicate_files"

all_emails="$(
    rg -o -I --no-filename --hidden --glob '!.git/**' \
        '[[:alnum:]._%+-]+@[[:alnum:].-]+\.[A-Za-z]{2,}' \
        . | sort -u || true
)"
unexpected_emails="$(
    printf '%s\n' "$all_emails" \
        | rg -v '^([0-9]+\+[A-Za-z0-9-]+@users\.noreply\.github\.com|noreply@github\.com)$' \
        || true
)"
fail_with_matches "only approved public or GitHub noreply email addresses appear" "$unexpected_emails"

if git remote get-url origin >/dev/null 2>&1 && git rev-parse --verify origin/master >/dev/null 2>&1; then
    outgoing_identities="$(
        git log origin/master..HEAD --format='%an%x09%ae%x09%cn%x09%ce' \
            | rg -v '^alzpqm\t284669565\+alzpqm@users\.noreply\.github\.com\talzpqm\t284669565\+alzpqm@users\.noreply\.github\.com$' \
            || true
    )"
    fail_with_matches "maintainer commits use the anonymous GitHub identity" "$outgoing_identities"
else
    printf 'SKIP: origin/master is unavailable; commit identity needs manual verification\n'
fi

if [[ "${1:-}" == "--artifacts" ]]; then
    apk="app/build/outputs/apk/release/app-release.apk"
    aab="app/build/outputs/bundle/release/app-release.aab"
    if [[ ! -f "$apk" || ! -f "$aab" ]]; then
        printf 'FAIL: release APK and AAB must exist before artifact privacy scanning\n'
        failed=1
    else
        scan_root="$(mktemp -d "${TMPDIR:-/tmp}/nexttraceroute-privacy.XXXXXX")"
        trap 'rm -rf "$scan_root"' EXIT
        mkdir "$scan_root/apk" "$scan_root/aab"
        unzip -qq "$apk" -d "$scan_root/apk"
        unzip -qq "$aab" -d "$scan_root/aab"

        artifact_leaks="$(
            rg -n -a -i \
                '(/Users/[^/[:space:]]+|/home/[^/[:space:]]+|[A-Z]:\\Users\\[^\\[:space:]]+|@gmail\.com|@icloud\.com|@me\.com|@outlook\.com|@hotmail\.com|BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY)' \
                "$scan_root/apk" "$scan_root/aab" || true
        )"
        fail_with_matches "release artifacts contain no private paths, email or keys" "$artifact_leaks"

        sdk_dir="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
        if [[ -z "$sdk_dir" && -f local.properties ]]; then
            sdk_dir="$(sed -n 's/^sdk\.dir=//p' local.properties | sed 's/\\:/:/g; s/\\\\/\\/g')"
        fi
        if [[ -z "$sdk_dir" && -n "${HOME:-}" && -d "$HOME/Library/Android/sdk" ]]; then
            sdk_dir="$HOME/Library/Android/sdk"
        elif [[ -z "$sdk_dir" && -n "${HOME:-}" && -d "$HOME/Android/Sdk" ]]; then
            sdk_dir="$HOME/Android/Sdk"
        fi
        apksigner=""
        if command -v apksigner >/dev/null 2>&1; then
            apksigner="$(command -v apksigner)"
        elif [[ -n "$sdk_dir" ]]; then
            apksigner="$(find "$sdk_dir/build-tools" -type f -name apksigner 2>/dev/null | sort -V | tail -1)"
        fi

        if [[ -z "$apksigner" ]]; then
            printf 'FAIL: apksigner unavailable; certificate identity was not checked\n'
            failed=1
        else
            certificate_subject="$(
                "$apksigner" verify --print-certs "$apk" \
                    | sed -n 's/^Signer #1 certificate DN: //p; s/^V2 Signer: certificate DN: //p'
            )"
            if [[ "$certificate_subject" == "CN=alzpqm, O=alzpqm" ]]; then
                printf 'PASS: APK certificate uses the anonymous GitHub identity\n'
            else
                printf 'FAIL: unexpected APK certificate subject: %s\n' "$certificate_subject"
                failed=1
            fi
        fi
    fi
fi

if [[ "$failed" -ne 0 ]]; then
    printf 'Privacy scan failed. Do not push, tag or publish a release.\n'
    exit 1
fi

printf 'Privacy scan passed.\n'
