#!/usr/bin/env python3
"""Regression tests for tag-only, create-only FastMachines release publishing.

All shell scenarios use a local fake gh executable. They never contact GitHub.
"""
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]
WORKFLOW = ROOT / ".github/workflows/build.yml"
STEP = "      - name: Publish distinct immutable version tag only\n"
POLICY_RUN = "run: python3 -m unittest discover -s scripts -p 'test_immutable_release_policy.py' -v"


def release_shell(workflow: str) -> str:
    if workflow.count(STEP) != 1:
        raise ValueError("release step must be unique")
    before, release = workflow.split(STEP, 1)
    if POLICY_RUN not in before:
        raise ValueError("policy tests must execute before building")
    if "    tags: ['v*']" not in before or "    branches: ['**']" not in before:
        raise ValueError("retain branch builds and version-tag build triggers")
    if not release.startswith("        if: startsWith(github.ref, 'refs/tags/v')\n"):
        raise ValueError("publication must be version-tag-only")
    required = (
        'TAG="v${VERSION}"',
        'test "$GITHUB_REF" = "refs/tags/${TAG}"',
        'JAR="dist/${OUTPUT_NAME}"',
        'test -s "$JAR"',
        'if gh release view "$TAG" --repo "$GITHUB_REPOSITORY" >/dev/null 2>&1; then',
        'exit 1',
        'fi',
        'gh release create "$TAG" "$JAR" --verify-tag --repo "$GITHUB_REPOSITORY"',
    )
    for phrase in required:
        if phrase not in release:
            raise ValueError("missing fail-closed release guard: " + phrase)
    forbidden = ("gh release upload", "gh release delete-asset", "gh release edit",
                 "--clobber", "workflow_dispatch", "default_branch")
    if any(phrase in release for phrase in forbidden):
        raise ValueError("release mutation or branch/manual publishing forbidden")
    view = release.index(required[4])
    reject = release.index("exit 1", view)
    close = release.index("\n          fi", reject)
    create = release.index(required[7], close)
    if not view < reject < close < create:
        raise ValueError("existing release must be rejected before creation")
    marker = "        run: |\n"
    if release.count(marker) != 1:
        raise ValueError("release shell must be unique")
    block = release.split(marker, 1)[1]
    lines = block.splitlines()
    if any(line and not line.startswith("          ") for line in lines):
        raise ValueError("unexpected release shell indentation")
    return "\n".join(line[10:] for line in lines) + "\n"


def run_fake_release(shell: str, ref: str, exists: bool, jar_present: bool = True):
    with tempfile.TemporaryDirectory() as directory:
        root = Path(directory)
        binary = root / "bin"
        binary.mkdir()
        fake = binary / "gh"
        fake.write_text(
            '#!/bin/sh\n'
            'printf "%s\\n" "$*" >> "$GH_CALLS"\n'
            'if [ "$1" = release ] && [ "$2" = view ]; then\n'
            '  [ "$FAKE_RELEASE_EXISTS" = 1 ]\n'
            'elif [ "$1" = release ] && [ "$2" = create ]; then\n'
            '  exit 0\n'
            'else\n'
            '  exit 99\n'
            'fi\n',
            encoding="utf-8",
        )
        fake.chmod(0o755)
        (root / "dist").mkdir()
        if jar_present:
            (root / "dist/SF_FastMachines1.0.4.jar").write_bytes(b"test fixture, not a plugin")
        log = root / "calls.txt"
        env = dict(os.environ)
        env.update({
            "PATH": str(binary) + os.pathsep + os.environ["PATH"],
            "GH_CALLS": str(log),
            "FAKE_RELEASE_EXISTS": "1" if exists else "0",
            "GITHUB_REF": ref,
            "GITHUB_REPOSITORY": "wickidcow/SF_FastMachines",
            "VERSION": "1.0.4",
            "OUTPUT_NAME": "SF_FastMachines1.0.4.jar",
        })
        result = subprocess.run(["bash", "-c", shell], cwd=root, env=env,
                                text=True, capture_output=True, check=False)
        calls = log.read_text(encoding="utf-8").splitlines() if log.exists() else []
        return result.returncode, calls


class ImmutableReleasePolicyTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.workflow = WORKFLOW.read_text(encoding="utf-8")

    def mutate(self, before: str, after: str):
        self.assertEqual(self.workflow.count(before), 1, before)
        with self.assertRaises(ValueError):
            release_shell(self.workflow.replace(before, after, 1))

    def test_current_policy(self):
        self.assertIn("set -euo pipefail", release_shell(self.workflow))

    def test_manual_dispatch_cannot_publish(self):
        self.mutate("if: startsWith(github.ref, 'refs/tags/v')",
                    "if: github.event_name == 'workflow_dispatch' || startsWith(github.ref, 'refs/tags/v')")

    def test_default_branch_cannot_publish(self):
        self.mutate("if: startsWith(github.ref, 'refs/tags/v')",
                    "if: github.ref_name == github.event.repository.default_branch")

    def test_mismatched_tag_must_fail(self):
        self.mutate('test "$GITHUB_REF" = "refs/tags/${TAG}"',
                    'test "$GITHUB_REF" != "refs/tags/${TAG}"')

    def test_existing_release_guard_cannot_invert(self):
        self.mutate('if gh release view "$TAG" --repo "$GITHUB_REPOSITORY" >/dev/null 2>&1; then',
                    'if ! gh release view "$TAG" --repo "$GITHUB_REPOSITORY" >/dev/null 2>&1; then')

    def test_existing_release_must_exit(self):
        self.mutate("            exit 1\n", "            true\n")

    def test_existing_release_cannot_be_overwritten(self):
        self.mutate('gh release create "$TAG" "$JAR" --verify-tag --repo "$GITHUB_REPOSITORY"',
                    'gh release upload "$TAG" "$JAR" --clobber --repo "$GITHUB_REPOSITORY"')

    def test_existing_release_assets_cannot_be_deleted(self):
        self.mutate('gh release create "$TAG" "$JAR" --verify-tag --repo "$GITHUB_REPOSITORY"',
                    'gh release delete-asset "$TAG" "$OUTPUT_NAME" --repo "$GITHUB_REPOSITORY"')

    def test_tag_must_preexist(self):
        self.mutate('gh release create "$TAG" "$JAR" --verify-tag --repo "$GITHUB_REPOSITORY"',
                    'gh release create "$TAG" "$JAR" --repo "$GITHUB_REPOSITORY"')

    def test_jar_must_exist(self):
        self.mutate('test -s "$JAR"', 'test -n "$JAR"')

    def test_policy_suite_cannot_be_skipped(self):
        self.mutate(POLICY_RUN, "run: echo release-policy-tests-skipped")

    def test_new_matching_tag_can_create_once(self):
        code, calls = run_fake_release(release_shell(self.workflow), "refs/tags/v1.0.4", False)
        self.assertEqual(code, 0)
        self.assertEqual(len(calls), 2)
        self.assertTrue(calls[0].startswith("release view v1.0.4 "))
        self.assertIn("release create v1.0.4 dist/SF_FastMachines1.0.4.jar --verify-tag", calls[1])

    def test_existing_release_is_never_modified(self):
        code, calls = run_fake_release(release_shell(self.workflow), "refs/tags/v1.0.4", True)
        self.assertNotEqual(code, 0)
        self.assertEqual(len(calls), 1)
        self.assertTrue(calls[0].startswith("release view "))

    def test_mismatched_tag_never_calls_gh(self):
        code, calls = run_fake_release(release_shell(self.workflow), "refs/tags/v1.0.5", False)
        self.assertNotEqual(code, 0)
        self.assertEqual(calls, [])

    def test_branch_ref_never_calls_gh(self):
        code, calls = run_fake_release(release_shell(self.workflow), "refs/heads/master", False)
        self.assertNotEqual(code, 0)
        self.assertEqual(calls, [])

    def test_missing_jar_never_calls_gh(self):
        code, calls = run_fake_release(release_shell(self.workflow), "refs/tags/v1.0.4", False, False)
        self.assertNotEqual(code, 0)
        self.assertEqual(calls, [])


if __name__ == "__main__":
    unittest.main()
