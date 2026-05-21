"""CLI surface — argument parsing, version, error handling."""
import pytest

from awd_deserializer.cli import main


def test_version_flag(capsys):
    """`awd-deserialize --version` should print the package version and exit 0."""
    with pytest.raises(SystemExit) as exc:
        main(["--version"])
    assert exc.value.code == 0
    captured = capsys.readouterr()
    assert "awd-deserialize" in captured.out
    assert "0.1.0" in captured.out


def test_missing_input_file_returns_nonzero(tmp_path, capsys):
    """`awd-deserialize deserialize <missing>` should fail with nonzero exit + stderr."""
    bogus = tmp_path / "does-not-exist.design"
    rc = main(["deserialize", str(bogus)])
    assert rc != 0
    captured = capsys.readouterr()
    assert "error" in captured.err.lower()


def test_no_subcommand_prints_help_and_returns_nonzero(capsys):
    """Invoking with no subcommand prints help and returns nonzero."""
    rc = main([])
    assert rc != 0
    captured = capsys.readouterr()
    # argparse prints help to stdout
    assert "awd-deserialize" in captured.out.lower() or "usage" in captured.out.lower()


def test_inspect_missing_file_returns_nonzero(tmp_path, capsys):
    """`awd-deserialize inspect <missing>` should fail with nonzero exit + stderr."""
    bogus = tmp_path / "does-not-exist.design"
    rc = main(["inspect", str(bogus)])
    assert rc != 0
    captured = capsys.readouterr()
    assert "not found" in captured.err.lower() or "error" in captured.err.lower()
