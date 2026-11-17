#!/usr/bin/env python3
"""Regression tests for create-only, exact-green-master FastMachines releases.

The tests do not contact GitHub or publish a tag.
"""
from pathlib import Path
import hashlib
import tempfile
import unittest
import zipfile

from verify_immutable_release_jar import verify_jar


ROOT = Path(__file__).resolve().parents[1]
WORKFLOW = ROOT / ".github/workflows/publish-green-master.yml"
BUILDER = ROOT / ".github/workflows/build.yml"


def check_release_workflow(source: str) -> None:
    required = (
        "workflow_run:",
        "workflows: ['Build FastMachines']",
        "types: [completed]",
        "branches: [master]",
        "github.event.workflow_run.conclusion == 'success'",
        "github.event.workflow_run.event == 'push'",
        "github.event.workflow_run.head_branch == 'master'",
        "github.event.workflow_run.head_repository.full_name == github.repository",
        "github.event.workflow_run.head_sha",
        'LIVE_SHA="$(gh api "repos/$GITHUB_REPOSITORY/git/ref/heads/master" --jq',
        '"$LIVE_SHA" != "$TESTED_SHA"',
        '"$(git rev-parse HEAD)" != "$TESTED_SHA"',
        "jobs?per_page=100",
        "matches[0]['conclusion'] != 'success'",
        "artifacts?per_page=100",
        "item.get('workflow_run', {}).get('head_sha') == sys.argv[3]",
        "sha256:[0-9a-f]{64}",
        "releases/tags/$TAG",
        "git/ref/tags/$TAG",
        "HTTP 404",
        "skip-decompress: true",
        "run-id: ${{ github.event.workflow_run.id }}",
        "python3 scripts/verify_immutable_release_jar.py",
        'gh release create "$TAG" "dist/$NAME" --verify-tag',
        'test "$(gh api "repos/$GITHUB_REPOSITORY/git/ref/tags/$TAG" --jq',
        'gh release download "$TAG" --repo "$GITHUB_REPOSITORY"',
    )
    for phrase in required:
        if phrase not in source:
            raise ValueError("Release source safety clause missing: " + phrase)
    if source.count("python3 scripts/verify_immutable_release_jar.py") != 2:
        raise ValueError("Require raw-JAR checks before and after GitHub publication")
    if source.index("releases/tags/$TAG") >= source.index('gh release create "$TAG"'):
        raise ValueError("Existing release check must occur before creation")
    if source.index("git/ref/tags/$TAG") >= source.index('gh release create "$TAG"'):
        raise ValueError("Existing tag must be rejected before release")
    for forbidden in ("--clobber", "gh release upload", "gh release edit",
                      "gh release delete", "git push --force", "git tag -f"):
        if forbidden in source:
            raise ValueError("Mutable published artifact operation: " + forbidden)


class ReleasePolicyTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.workflow = WORKFLOW.read_text(encoding="utf-8")

    def test_current_workflow_source(self):
        check_release_workflow(self.workflow)

    def test_green_master_is_required(self):
        with self.assertRaises(ValueError):
            check_release_workflow(self.workflow.replace(
                "github.event.workflow_run.conclusion == 'success'",
                "github.event.workflow_run.conclusion != 'success'", 1))

    def test_source_sha_pin_is_required(self):
        with self.assertRaises(ValueError):
            check_release_workflow(self.workflow.replace(
                "item.get('workflow_run', {}).get('head_sha') == sys.argv[3]",
                "True", 1))

    def test_no_existing_asset_overwrite(self):
        with self.assertRaises(ValueError):
            check_release_workflow(self.workflow.replace(
                'gh release create "$TAG" "dist/$NAME" --verify-tag',
                'gh release upload "$TAG" "dist/$NAME" --clobber', 1))

    def test_source_suite_runs_on_every_build(self):
        build = BUILDER.read_text(encoding="utf-8")
        self.assertIn("run: python3 -m unittest discover -s scripts -p 'test_verified_master_publisher.py' -v", build)
        self.assertIn("python3 scripts/verify_immutable_release_jar.py", build)


class RawJarIntegrityTest(unittest.TestCase):
    @staticmethod
    def fixture(folder: Path, java_major: int = 65) -> Path:
        path = folder / "SF_FastMachines1.0.5.jar"
        header = b"\xca\xfe\xba\xbe\x00\x00" + java_major.to_bytes(2, "big")
        with zipfile.ZipFile(path, "w") as jar:
            jar.writestr("plugin.yml",
                "name: FastMachines\nversion: '1.0.5'\n"
                "main: net.guizhanss.fastmachines.FastMachines\n")
            jar.writestr("net/guizhanss/fastmachines/FastMachines.class", header)
            jar.writestr(
                "net/guizhanss/fastmachines/libs/guizhanlib/slimefun/addon/AbstractAddon.class",
                header)
        return path

    def test_canonical_synthetic_jar_passes(self):
        with tempfile.TemporaryDirectory() as temp:
            file = self.fixture(Path(temp))
            sha = hashlib.sha256(file.read_bytes()).hexdigest()
            verify_jar(file, "1.0.5", sha)

    def test_tampered_raw_jar_is_rejected(self):
        with tempfile.TemporaryDirectory() as temp:
            file = self.fixture(Path(temp))
            with self.assertRaisesRegex(ValueError, "SHA-256 mismatch"):
                verify_jar(file, "1.0.5", "0" * 64)

    def test_newer_than_java21_class_is_rejected(self):
        with tempfile.TemporaryDirectory() as temp:
            file = self.fixture(Path(temp), java_major=69)
            with self.assertRaisesRegex(ValueError, "newer than 21"):
                verify_jar(file, "1.0.5", hashlib.sha256(file.read_bytes()).hexdigest())

    def test_wrong_tag_version_is_rejected(self):
        with tempfile.TemporaryDirectory() as temp:
            file = self.fixture(Path(temp))
            with self.assertRaises(ValueError):
                verify_jar(file, "1.0.4", hashlib.sha256(file.read_bytes()).hexdigest())


if __name__ == "__main__":
    unittest.main()
