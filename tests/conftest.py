"""Shared pytest fixtures."""
import shutil
import subprocess
from pathlib import Path

import pytest

REPO_ROOT = Path(__file__).resolve().parents[1]
JAR_PATH = REPO_ROOT / "target" / "awd-deserializer-engine-0.1.0.jar"
FIXTURES = Path(__file__).resolve().parent / "fixtures" / "data"


@pytest.fixture(scope="session")
def jar_built() -> Path:
    """Build the Java engine JAR if it isn't already present."""
    if JAR_PATH.exists():
        return JAR_PATH
    if not shutil.which("mvn"):
        pytest.skip("Maven not on PATH — cannot build Java engine for integration tests")
    subprocess.run(
        ["mvn", "-B", "-q", "package", "-DskipTests"],
        cwd=REPO_ROOT,
        check=True,
    )
    if not JAR_PATH.exists():
        pytest.fail(f"JAR not produced at {JAR_PATH} after mvn package")
    return JAR_PATH


@pytest.fixture
def small_design_file() -> Path:
    p = FIXTURES / "small.design"
    if not p.exists():
        pytest.skip(f"fixture missing: {p} — run tests/fixtures/generate_fixtures.py")
    return p
