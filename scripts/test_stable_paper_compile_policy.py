#!/usr/bin/env python3
"""Prevent FastMachines' primary Paper 26.3 source lane reverting to an alpha API."""
from pathlib import Path
import unittest

WORKFLOW = Path(__file__).resolve().parents[1] / ".github/workflows/build.yml"
START = "      - name: Verify Paper/Purpur 1.21.11 build\n"
END = "      - name: Build universal release JAR from 1.21.11 baseline\n"
POLICY_RUN = "run: python3 -m unittest discover -s scripts -p 'test_stable_paper_compile_policy.py' -v"
MINIMUM = "-PpaperVersion=1.21.11-R0.1-SNAPSHOT -PtargetJvm=21 -PprojectVersion=1.0.4"
SUPPORTED = "-PpaperVersion=26.2.build.+ -PtargetJvm=25 -PprojectVersion=1.0.4"
PRIMARY = "-PpaperVersion=26.3.build.230-stable -PtargetJvm=25 -PprojectVersion=1.0.4"


def verify(workflow: str) -> None:
    if workflow.count(START) != 1 or workflow.count(END) != 1:
        raise ValueError("all source compile and release baseline steps must remain")
    before, rest = workflow.split(START, 1)
    compile_steps, release = rest.split(END, 1)
    if POLICY_RUN not in before:
        raise ValueError("policy regression must execute on every CI build")
    if "test_immutable_release_policy.py' -v" not in before:
        raise ValueError("existing immutable publisher tests must remain")
    if not compile_steps.startswith("        run: ./gradlew clean build "):
        raise ValueError("minimum baseline must compile actual sources")
    for fragment in (MINIMUM, SUPPORTED, PRIMARY):
        if compile_steps.count(fragment) != 1:
            raise ValueError("missing or duplicated supported API compile: " + fragment)
    if not (compile_steps.index(MINIMUM) < compile_steps.index(SUPPORTED) < compile_steps.index(PRIMARY)):
        raise ValueError("compile lanes must preserve minimum, supported, primary order")
    if "      - name: Verify stable Paper 26.3 primary API source compatibility\n" not in compile_steps:
        raise ValueError("stable primary API step is required")
    if any(x in compile_steps for x in ("26.3-rc-", "26.3.build.140-beta", "26.3.build.+", "26.3.build.230-alpha")):
        raise ValueError("alpha, beta, or unbounded 26.3 source selection is forbidden")
    if compile_steps.count("./gradlew clean build ") != 3:
        raise ValueError("all three API lanes must execute Gradle build")
    if "run: ./gradlew clean build " not in release or MINIMUM not in release:
        raise ValueError("Java21/1.21.11 universal release baseline must remain")
    if "test -s \"$JAR\"" not in release or "sha256sum \"dist/" not in release:
        raise ValueError("actual canonical JAR checks must remain")
    if "if: startsWith(github.ref, 'refs/tags/v')" not in release:
        raise ValueError("tag-only immutable publisher gate must remain")


class StablePaperCompilePolicyTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.workflow = WORKFLOW.read_text(encoding="utf-8")

    def mutate(self, old: str, new: str):
        self.assertEqual(self.workflow.count(old), 1, old)
        with self.assertRaises(ValueError):
            verify(self.workflow.replace(old, new, 1))

    def test_current_policy(self):
        verify(self.workflow)

    def test_alpha_is_rejected(self):
        self.mutate(PRIMARY, "-PpaperVersion=26.3-rc-3.build.1-alpha -PtargetJvm=25 -PprojectVersion=1.0.4")

    def test_beta_is_rejected(self):
        self.mutate(PRIMARY, "-PpaperVersion=26.3.build.140-beta -PtargetJvm=25 -PprojectVersion=1.0.4")

    def test_unbounded_primary_is_rejected(self):
        self.mutate(PRIMARY, "-PpaperVersion=26.3.build.+ -PtargetJvm=25 -PprojectVersion=1.0.4")

    def test_minimum_api_floor_must_remain(self):
        self.mutate(START, "      - name: Skip minimum Paper 1.21.11 compile\n")

    def test_262_supported_lane_must_remain(self):
        self.mutate(SUPPORTED, "-PpaperVersion=26.1.2.build.+ -PtargetJvm=25 -PprojectVersion=1.0.4")

    def test_primary_lane_must_execute_build(self):
        self.mutate("      - name: Verify stable Paper 26.3 primary API source compatibility\n        run: ./gradlew clean build ",
                    "      - name: Verify stable Paper 26.3 primary API source compatibility\n        run: echo ")

    def test_universal_release_baseline_must_remain(self):
        self.mutate(END, "      - name: Skip universal release baseline\n")

    def test_immutable_publisher_must_remain(self):
        self.mutate("if: startsWith(github.ref, 'refs/tags/v')",
                    "if: github.ref_name == github.event.repository.default_branch")

    def test_policy_suite_must_execute(self):
        self.mutate(POLICY_RUN, "run: echo stable-paper-policy-skipped")


if __name__ == "__main__":
    unittest.main()
