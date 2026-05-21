"""End-to-end: build JAR, deserialize a fixture, assert JSON shape.

This test requires a built Java engine JAR (`target/awd-deserializer-engine-0.1.0.jar`).
The `jar_built` session fixture in `conftest.py` will attempt to build it via
`mvn package -DskipTests` on first run and `pytest.skip` if Maven is unavailable.
"""
import json
import subprocess

import pytest


@pytest.mark.integration
def test_cli_deserialize_small_fixture(jar_built, small_design_file, tmp_path):
    """Smoke-test the full pipeline: CLI -> Java engine -> JSON output."""
    out = tmp_path / "out.json"
    result = subprocess.run(
        [
            "awd-deserialize",
            "deserialize",
            str(small_design_file),
            "-o",
            str(out),
        ],
        capture_output=True,
        text=True,
        timeout=60,
    )
    assert result.returncode == 0, f"stderr: {result.stderr}\nstdout: {result.stdout}"
    assert out.exists(), f"Output JSON not produced at {out}"
    payload = json.loads(out.read_text(encoding="utf-8"))
    # Minimal shape assertion — the synthetic fixture exercises only the
    # AWD reader's framing, so the payload structure is engine-defined.
    # Adjust this if/when we tighten the JSON contract.
    assert isinstance(payload, (dict, list)), (
        f"Expected JSON object or array, got {type(payload).__name__}"
    )
