#!/usr/bin/env python3
"""Reject newly introduced CJK text in player-facing source and default resources."""
from pathlib import Path
import re
import sys

root = Path(sys.argv[1] if len(sys.argv) > 1 else ".").resolve()
cjk = re.compile(r"[\u3400-\u4DBF\u4E00-\u9FFF\uF900-\uFAFF]")
violations: list[str] = []

for path in (root / "src" / "main" / "kotlin").rglob("*.kt"):
    for number, line in enumerate(path.read_text(encoding="utf-8", errors="replace").splitlines(), 1):
        if cjk.search(line):
            violations.append(f"{path.relative_to(root)}:{number}: {line.strip()}")

resources = root / "src" / "main" / "resources"
for path in resources.rglob("*"):
    if not path.is_file():
        continue
    relative = path.relative_to(resources).as_posix()
    if relative.startswith("lang/") and relative != "lang/en-US.yml":
        continue
    text = path.read_text(encoding="utf-8", errors="replace")
    for number, line in enumerate(text.splitlines(), 1):
        if cjk.search(line):
            violations.append(f"{path.relative_to(root)}:{number}: {line.strip()}")

if violations:
    print("Player-facing CJK text was found:")
    print("\n".join(violations))
    raise SystemExit(1)

print("English verification passed: no player-facing hard-coded CJK text was found.")
