"""Test that an old release cannot be clobbered by a normal FastMachines build."""
from __future__ import annotations

import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

from check_legacy_publish_step import PUBLISH_STEP, violations

CHECKER = Path(__file__).with_name('check_legacy_publish_step.py')
WORKFLOW = Path(__file__).resolve().parents[1] / '.github' / 'workflows' / 'build.yml'
FIXTURE = '''name: Build FastMachines
jobs:
  build-fastmachines:
    steps:
      - name: Compile
        run: echo ok
      - name: Publish canonical raw JAR release asset
        if: false
        run: |
          gh release upload v1.0.4 build.jar --clobber
'''


class FastMachinesPublisherDisabled(unittest.TestCase):
    def test_dormant_historical_step_passes(self):
        self.assertEqual([], violations(FIXTURE))

    def test_old_default_branch_trigger_fails(self):
        trigger = "github.event_name == 'workflow_dispatch' || startsWith(github.ref, 'refs/tags/') || github.ref_name == github.event.repository.default_branch"
        self.assertTrue(violations(FIXTURE.replace('        if: false', f'        if: {trigger}')))

    def test_unconditional_publish_fails(self):
        self.assertTrue(violations(FIXTURE.replace('        if: false\n', '')))
        self.assertTrue(violations(FIXTURE.replace('        if: false', '        if: true')))

    def test_dispatch_only_is_not_sufficient(self):
        self.assertTrue(violations(FIXTURE.replace('        if: false', "        if: github.event_name == 'workflow_dispatch'")))

    def test_duplicate_or_missing_publish_step_fails(self):
        self.assertTrue(violations(FIXTURE + PUBLISH_STEP + '        if: false\n'))
        self.assertTrue(violations(FIXTURE.replace(PUBLISH_STEP, '')))

    def test_other_release_mutation_before_disabled_step_fails(self):
        for verb in ('upload', 'create', 'delete-asset'):
            with self.subTest(verb=verb):
                changed = FIXTURE.replace('        run: echo ok', f'        run: gh release {verb} v1.0.4')
                self.assertTrue(violations(changed))

    def test_literal_if_must_be_immediately_after_step_header(self):
        self.assertTrue(violations(FIXTURE.replace('        if: false\n', '        # no-op\n        if: false\n')))

    def test_real_workflow_and_cli_fail_closed_on_mutation(self):
        actual = WORKFLOW.read_text(encoding='utf-8')
        self.assertEqual([], violations(actual))
        with tempfile.TemporaryDirectory() as dirname:
            path = Path(dirname) / 'workflow.yml'
            path.write_text(actual, encoding='utf-8')
            good = subprocess.run([sys.executable, str(CHECKER), str(path)], capture_output=True, text=True)
            self.assertEqual(0, good.returncode, good.stderr)
            path.write_text(actual.replace('        if: false', '        if: true'), encoding='utf-8')
            bad = subprocess.run([sys.executable, str(CHECKER), str(path)], capture_output=True, text=True)
            self.assertEqual(1, bad.returncode, bad.stdout + bad.stderr)

    def test_missing_file_fails_closed(self):
        bad = subprocess.run([sys.executable, str(CHECKER), '/missing/never/a/workflow.yml'], capture_output=True, text=True)
        self.assertNotEqual(0, bad.returncode)


if __name__ == '__main__':
    unittest.main()
