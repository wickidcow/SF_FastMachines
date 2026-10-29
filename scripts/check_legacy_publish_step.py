#!/usr/bin/env python3
"""Fail closed if FastMachines' historical release-upload step can execute.

Only review-gated distinct new versions may ever be published. An ordinary
build must produce a CI artifact without updating existing public releases.
"""
from __future__ import annotations

import sys
from pathlib import Path

PUBLISH_STEP = '      - name: Publish canonical raw JAR release asset\n'
DISABLED_IF = '        if: false\n'


def violations(workflow: str) -> list[str]:
    errors: list[str] = []
    if workflow.count(PUBLISH_STEP) != 1:
        return ['Expected exactly one historical release-upload step']
    before, after = workflow.split(PUBLISH_STEP)
    if not after.startswith(DISABLED_IF):
        errors.append('Historical release-upload step must be guarded by immediate literal if: false')
    if 'gh release upload' in before or 'gh release create' in before or 'gh release delete-asset' in before:
        errors.append('Build steps preceding the retired publisher must not directly mutate releases')
    if '      - name: ' in after.split('        run:', 1)[0]:
        errors.append('Historical publisher step must remain a single normal step')
    return errors


def main(argv: list[str] | None = None) -> int:
    args = argv if argv is not None else sys.argv[1:]
    if len(args) != 1:
        print('Usage: check_legacy_publish_step.py <workflow.yml>', file=sys.stderr)
        return 2
    try:
        data = Path(args[0]).read_text(encoding='utf-8')
    except (OSError, UnicodeError) as exc:
        print(f'Cannot inspect workflow: {exc}', file=sys.stderr)
        return 2
    failures = violations(data)
    if failures:
        for failure in failures:
            print('FAIL: ' + failure, file=sys.stderr)
        return 1
    print('PASS: historical FastMachines release mutation is disabled for every build event')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
