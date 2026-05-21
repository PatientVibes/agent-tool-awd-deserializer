"""Unit tests for the AwdDeserializer client.

Adapted from the upstream chorus-deserializer tests:
  - tests/test_deserializer.py
  - tests/test_client.py

The Docker client surface and the api_server / websocket / rate-limiting
modules were dropped from this fork; tests that depended on them have
been removed rather than mocked into existence.
"""

import json
import shutil
import tempfile
from pathlib import Path
from unittest.mock import patch

import pytest

from awd_deserializer.client import AwdDeserializer
from awd_deserializer.config import DeserializerConfig
from awd_deserializer.exceptions import (
    AwdDeserializerError,
    DeserializationError,
    JavaBridgeError,
)
from awd_deserializer.exceptions import FileNotFoundError as AwdFileNotFoundError


class TestAwdDeserializerInit:
    """Initialization and configuration."""

    def setup_method(self):
        self.temp_dir = tempfile.mkdtemp()
        self.temp_path = Path(self.temp_dir)
        self.config = DeserializerConfig()

    def teardown_method(self):
        shutil.rmtree(self.temp_dir, ignore_errors=True)

    def test_initialization_with_config(self):
        deserializer = AwdDeserializer(config=self.config)
        assert deserializer.config is self.config

    def test_initialization_without_config(self):
        deserializer = AwdDeserializer()
        assert deserializer.config is not None
        assert isinstance(deserializer.config, DeserializerConfig)

    def test_initialization_with_debug(self):
        deserializer = AwdDeserializer(debug=True)
        assert deserializer.config is not None

    def test_initialization_with_jar_path(self):
        jar = self.temp_path / "custom.jar"
        deserializer = AwdDeserializer(jar_path=jar)
        assert deserializer.jar_path == jar


class TestFilePathValidation:
    """File path validation behavior."""

    def setup_method(self):
        self.temp_dir = tempfile.mkdtemp()
        self.temp_path = Path(self.temp_dir)
        self.client = AwdDeserializer()

    def teardown_method(self):
        shutil.rmtree(self.temp_dir, ignore_errors=True)

    def test_validate_file_path_valid_files(self):
        valid_files = [
            self.temp_path / "test.design",
            self.temp_path / "TEST.DESIGN",
            self.temp_path / "valid_file.design",
        ]
        for file_path in valid_files:
            file_path.write_text("test content")
            result = self.client._validate_file_path(str(file_path))
            assert result == str(file_path.resolve())

    def test_validate_file_path_invalid_extensions(self):
        invalid_files = [
            self.temp_path / "test.txt",
            self.temp_path / "test.json",
            self.temp_path / "test.xml",
        ]
        for file_path in invalid_files:
            file_path.write_text("test content")
            with pytest.raises(DeserializationError, match="Invalid file extension"):
                self.client._validate_file_path(str(file_path))

    def test_validate_file_path_empty_input(self):
        with pytest.raises(DeserializationError, match="File path cannot be empty"):
            self.client._validate_file_path("")

    def test_validate_file_path_none_input(self):
        with pytest.raises(DeserializationError, match="File path cannot be empty"):
            self.client._validate_file_path(None)

    def test_validate_file_path_does_not_check_existence(self):
        nonexistent_file = str(self.temp_path / "does_not_exist.design")
        # _validate_file_path only checks path format; existence is checked downstream
        result = self.client._validate_file_path(nonexistent_file)
        assert result is not None

    def test_unicode_file_handling(self):
        unicode_file = self.temp_path / "test_文件_🎨.design"
        unicode_file.write_text("unicode content")
        result = self.client._validate_file_path(str(unicode_file))
        assert result == str(unicode_file.resolve())


class TestFileSizeValidation:
    """File size validation behavior."""

    def setup_method(self):
        self.temp_dir = tempfile.mkdtemp()
        self.temp_path = Path(self.temp_dir)
        self.client = AwdDeserializer()

    def teardown_method(self):
        shutil.rmtree(self.temp_dir, ignore_errors=True)

    def test_validate_file_size_within_limits(self):
        test_file = self.temp_path / "small.design"
        test_file.write_text("small content")
        # Should not raise
        self.client._validate_file_size(str(test_file))

    def test_validate_file_size_exceeds_limits(self):
        large_file = self.temp_path / "large.design"
        large_file.write_text("X" * 1024)
        with pytest.raises(DeserializationError, match="File too large"):
            self.client._validate_file_size(str(large_file), max_size=512)

    def test_validate_file_size_empty_file(self):
        empty_file = self.temp_path / "empty.design"
        empty_file.write_text("")
        with pytest.raises(DeserializationError, match="File is empty"):
            self.client._validate_file_size(str(empty_file))


class TestFormatBytes:
    """Static helper for human-readable byte counts."""

    def test_format_bytes_small(self):
        assert AwdDeserializer._format_bytes(500) == "500 bytes"

    def test_format_bytes_kb(self):
        assert "KB" in AwdDeserializer._format_bytes(1500)

    def test_format_bytes_mb(self):
        assert "MB" in AwdDeserializer._format_bytes(1_500_000)

    def test_format_bytes_gb(self):
        assert "GB" in AwdDeserializer._format_bytes(1_500_000_000)

    def test_format_bytes_exact_kb(self):
        assert AwdDeserializer._format_bytes(1024) == "1.00 KB"

    def test_format_bytes_exact_mb(self):
        assert AwdDeserializer._format_bytes(1024 * 1024) == "1.00 MB"


class TestDeserializeErrorPaths:
    """End-to-end deserialize() validation/error handling (no Java engine needed)."""

    def setup_method(self):
        self.temp_dir = tempfile.mkdtemp()
        self.temp_path = Path(self.temp_dir)
        self.client = AwdDeserializer()

    def teardown_method(self):
        shutil.rmtree(self.temp_dir, ignore_errors=True)

    def test_deserialize_nonexistent_file(self):
        nonexistent = self.temp_path / "nonexistent.design"
        with pytest.raises(AwdFileNotFoundError, match="Design file not found"):
            self.client.deserialize(str(nonexistent))

    def test_deserialize_empty_file(self):
        empty_file = self.temp_path / "empty.design"
        empty_file.write_text("")
        with pytest.raises(DeserializationError, match="File is empty"):
            self.client.deserialize(str(empty_file))

    def test_deserialize_invalid_extension(self):
        bad = self.temp_path / "bad.txt"
        bad.write_text("nope")
        with pytest.raises(DeserializationError, match="Invalid file extension"):
            self.client.deserialize(str(bad))

    def test_deserialize_missing_jar_raises_java_bridge_error(self):
        """When the JAR is missing, deserialize() should surface JavaBridgeError."""
        input_file = self.temp_path / "input.design"
        input_file.write_text("test content")
        # Force the client to point at a JAR that does not exist
        self.client.jar_path = self.temp_path / "no-such-jar.jar"
        with pytest.raises(JavaBridgeError, match="Java engine JAR not found"):
            self.client.deserialize(str(input_file))


class TestConfigConversion:
    """DeserializerConfig -> Java opts."""

    def test_config_to_java_opts(self):
        config = DeserializerConfig(
            memory_limit_mb=2048,
            batch_size=100,
            focused_mode=False,
            safe_reflection=True,
        )
        opts = config.to_java_opts()
        assert "-Dmemory.limit.mb=2048" in opts
        assert "-Dbatch.size=100" in opts
        assert "-Dfocused.mode=false" in opts
        assert "-Dsafe.reflection=true" in opts

    def test_config_to_java_opts_includes_heap(self):
        config = DeserializerConfig(heap_size="2g")
        opts = config.to_java_opts()
        assert "-Xmx2g" in opts


class TestExceptionHierarchy:
    """Sanity-check the exception hierarchy."""

    def test_deserialization_error_is_base_subclass(self):
        assert issubclass(DeserializationError, AwdDeserializerError)

    def test_java_bridge_error_is_base_subclass(self):
        assert issubclass(JavaBridgeError, AwdDeserializerError)

    def test_awd_file_not_found_is_base_subclass(self):
        assert issubclass(AwdFileNotFoundError, AwdDeserializerError)


class TestConcurrentAccess:
    """Validation helpers should be safe to call from multiple threads."""

    def setup_method(self):
        self.temp_dir = tempfile.mkdtemp()
        self.temp_path = Path(self.temp_dir)
        self.client = AwdDeserializer()

    def teardown_method(self):
        shutil.rmtree(self.temp_dir, ignore_errors=True)

    def test_concurrent_validate_file_path(self):
        import threading

        test_file = self.temp_path / "concurrent.design"
        test_file.write_text("test")

        errors: list = []

        def validate():
            try:
                self.client._validate_file_path(str(test_file))
            except Exception as exc:  # pragma: no cover - failure path
                errors.append(exc)

        threads = [threading.Thread(target=validate) for _ in range(10)]
        for t in threads:
            t.start()
        for t in threads:
            t.join()

        assert errors == []
