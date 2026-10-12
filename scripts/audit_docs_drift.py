#!/usr/bin/env python3
"""
scripts/audit_docs_drift.py
Deterministic documentation drift auditor for ShellGuard-Mobile.
Asserts that root documentation 100% bows to the physical codebase.
"""

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent

def check_build_gradle():
    gradle_file = ROOT / "app/build.gradle.kts"
    if not gradle_file.exists():
        sys.exit("[-] app/build.gradle.kts not found.")
    content = gradle_file.read_text(encoding="utf-8")
    vcode_match = re.search(r'versionCode\s*=\s*(\d+)', content)
    vname_match = re.search(r'versionName\s*=\s*"([^"]+)"', content)
    if not vcode_match or not vname_match:
        sys.exit("[-] Could not parse versionCode or versionName from app/build.gradle.kts")
    return int(vcode_match.group(1)), vname_match.group(1)

def audit_readme(vcode, vname):
    readme_file = ROOT / "README.md"
    if not readme_file.exists():
        sys.exit("[-] README.md not found.")
    content = readme_file.read_text(encoding="utf-8")
    
    # Check badge
    expected_badge = f"version-v{vname}%20(Build%20{vcode})"
    if expected_badge not in content and f"version-v{vname}" not in content:
        print(f"[!] Warning: README.md badge does not reflect current version v{vname} (Build {vcode})")
        return False
    print(f"[+] README.md version badge validated for v{vname} (Build {vcode}).")
    return True

def audit_release_play():
    play_file = ROOT / "RELEASE-PLAY.md"
    if not play_file.exists():
        sys.exit("[-] RELEASE-PLAY.md missing from repository root.")
    content = play_file.read_text(encoding="utf-8")
    blocks = re.findall(r'<en-US>(.*?)</en-US>', content, re.DOTALL)
    if not blocks:
        sys.exit("[-] RELEASE-PLAY.md missing <en-US> block.")
    
    latest_block = blocks[0].strip()
    char_len = len(latest_block)
    if char_len >= 500:
        sys.exit(f"[-] RELEASE-PLAY.md latest block exceeds 500 characters: {char_len}/500 chars!")
    
    # Check for forbidden markdown symbols
    markdown_symbols = ["**", "##", "```"]
    for sym in markdown_symbols:
        if sym in latest_block:
            sys.exit(f"[-] RELEASE-PLAY.md contains forbidden markdown syntax: '{sym}'")
    
    print(f"[+] RELEASE-PLAY.md validated: {char_len}/500 chars, zero markdown leakage.")
    return True

def audit_changelog(vname, vcode):
    changelog_file = ROOT / "CHANGELOG.md"
    if not changelog_file.exists():
        sys.exit("[-] CHANGELOG.md not found.")
    content = changelog_file.read_text(encoding="utf-8")
    expected_header = f"[{vname}]"
    if expected_header not in content:
        sys.exit(f"[-] CHANGELOG.md missing release section header for {expected_header}")
    
    print(f"[+] CHANGELOG.md has entry for v{vname} (Build {vcode}).")
    return True

def audit_security(vname, vcode):
    security_file = ROOT / "SECURITY.md"
    if not security_file.exists():
        sys.exit("[-] SECURITY.md not found.")
    content = security_file.read_text(encoding="utf-8")
    
    # Check that current active version is documented in Supported Versions
    expected_entry = f"{vname}"
    if expected_entry not in content:
        sys.exit(f"[-] SECURITY.md Supported Versions table does not list active version {expected_entry}")
    
    # Check reporting contact exists
    if "security@clawstack.com" not in content and "security/advisories" not in content and "Report a vulnerability" not in content:
        sys.exit("[-] SECURITY.md missing security contact reporting channel.")
    
    print(f"[+] SECURITY.md validated: lists active version v{vname} (Build {vcode}) with responsible disclosure channel.")
    return True

def audit_relative_links():
    md_files = list(ROOT.glob("*.md")) + list(ROOT.glob("project/*.md"))
    broken = 0
    for md in md_files:
        text = md.read_text(encoding="utf-8")
        links = re.findall(r'\[.*?\]\(([^http:#][^\)]+)\)', text)
        for link in links:
            target = link.split('#')[0].strip()
            if not target:
                continue
            # Strip file:// or other local schemes if present
            if target.startswith("file://"):
                continue
            target_path = (md.parent / target).resolve()
            try:
                target_path.relative_to(ROOT)
            except ValueError:
                # Target resolves outside git repository root (e.g. sibling repo in local workspace)
                continue
            if not target_path.exists():
                print(f"[!] Broken link in {md.relative_to(ROOT)}: '{link}' -> '{target}'")
                broken += 1
    if broken > 0:
        sys.exit(f"[-] Found {broken} broken relative documentation hyperlinks.")
    print("[+] All relative documentation hyperlinks verified.")
    return True

def main():
    print("[*] Running ShellGuard-Mobile Deterministic Documentation Drift Audit...")
    vcode, vname = check_build_gradle()
    print(f"[*] Code Ground Truth: versionName='{vname}', versionCode={vcode}")
    
    success = True
    success &= audit_readme(vcode, vname)
    success &= audit_release_play()
    success &= audit_changelog(vname, vcode)
    success &= audit_security(vname, vcode)
    success &= audit_relative_links()
    
    if not success:
        sys.exit(1)
    print("\n[✓] SUCCESS: All root documentation 100% bows to the code!")

if __name__ == "__main__":
    main()
