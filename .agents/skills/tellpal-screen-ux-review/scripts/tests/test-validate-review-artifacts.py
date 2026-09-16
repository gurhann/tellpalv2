import importlib.util
import json
import tempfile
import unittest
from pathlib import Path


SCRIPT = Path(__file__).parents[1] / "validate-review-artifacts.py"
SPEC = importlib.util.spec_from_file_location("review_validator", SCRIPT)
MODULE = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(MODULE)


class ReviewArtifactValidationTests(unittest.TestCase):
    def write_artifacts(self, folder: Path, verified: bool = True) -> None:
        payloads = {
            "function-map.json": {"functions": [{"id": "save", "purpose": "Save", "trigger": "button", "result": "saved", "evidence_ids": ["save-rule"]}]},
            "domain-evidence.json": {"evidence": [{"id": "save-rule", "source": "test", "claim": "save exists", "verdict": "VERIFIED" if verified else "UNVERIFIED"}]},
            "findings.json": {"findings": [{"id": "crowded", "severity": "medium", "user_impact": "hard to scan", "affected_function_ids": ["save"]}]},
            "change-plan.json": {"changes": [{"id": "space", "finding_id": "crowded", "scope": "mockup", "preserved_function_ids": ["save"], "evidence_ids": ["save-rule"], "change": "group controls"}]},
        }
        for name, payload in payloads.items():
            (folder / name).write_text(json.dumps(payload), encoding="utf-8")

    def test_accepts_verified_function_preserving_change(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            folder = Path(temporary)
            self.write_artifacts(folder)
            self.assertTrue(MODULE.validate(folder)["ok"])

    def test_rejects_unverified_change_evidence(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            folder = Path(temporary)
            self.write_artifacts(folder, verified=False)
            result = MODULE.validate(folder)
            self.assertFalse(result["ok"])
            self.assertIn("is not VERIFIED", result["errors"][0])

