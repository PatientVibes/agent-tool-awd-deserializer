"""Main client for the AWD deserializer.

Invokes the Maven-built Java engine JAR via direct subprocess against
input .design / .service files. Performs path validation, size guards,
and structured error propagation.
"""

from __future__ import annotations

import json
import logging
import os
import shutil
import subprocess
import tempfile
from pathlib import Path
from typing import Any, Dict, Optional

from .config import DeserializerConfig
from .exceptions import (
    DeserializationError,
    FileNotFoundError as AwdFileNotFoundError,
    JavaBridgeError,
)
from .java_bridge import DEFAULT_JAR

logger = logging.getLogger(__name__)


class AwdDeserializer:
    """Python client for the AWD deserializer Java engine.

    Validates input files, invokes the Java JAR via subprocess, and
    streams the resulting JSON either to a file path or back to the
    caller as a dict.
    """

    # Security constants
    MAX_FILE_SIZE = 500 * 1024 * 1024  # 500 MB default limit
    VALID_EXTENSIONS = {".design", ".DESIGN", ".service", ".SERVICE"}

    def __init__(
        self,
        config: Optional[DeserializerConfig] = None,
        debug: bool = False,
        jar_path: Optional[Path] = None,
    ) -> None:
        """Initialize the deserializer.

        Args:
            config: Configuration (defaults applied if None).
            debug: Enable debug logging.
            jar_path: Path to the engine JAR; defaults to the Maven build output.
        """
        self.config = config or DeserializerConfig()
        self.jar_path = Path(jar_path) if jar_path else DEFAULT_JAR

        if debug:
            logging.basicConfig(
                level=logging.DEBUG,
                format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
            )
            logger.info("Debug logging enabled")

        if not self.jar_path.exists():
            logger.warning(
                "Java engine JAR not found at %s. Run `mvn package` from the repo root.",
                self.jar_path,
            )

    def _validate_file_path(self, file_path: str, check_extension: bool = True) -> str:
        """Validate and sanitize a file path with basic security checks."""
        if not file_path:
            raise DeserializationError("File path cannot be empty")
        try:
            abs_path = os.path.abspath(file_path)
            resolved = Path(abs_path).resolve()
        except Exception as exc:
            raise DeserializationError(f"Invalid file path: {file_path} - {exc}") from exc

        if ".." in str(resolved):
            raise DeserializationError(f"Path traversal detected: {file_path}")

        if check_extension and resolved.suffix not in self.VALID_EXTENSIONS:
            raise DeserializationError(
                f"Invalid file extension: {resolved.suffix}. "
                f"Expected one of: {', '.join(sorted(self.VALID_EXTENSIONS))}"
            )
        return str(resolved)

    def _validate_file_size(self, file_path: str, max_size: Optional[int] = None) -> None:
        """Validate file size to prevent DoS via oversize inputs."""
        if max_size is None:
            max_size = self.MAX_FILE_SIZE
        try:
            file_size = os.path.getsize(file_path)
        except OSError as exc:
            raise DeserializationError(f"Cannot check file size: {exc}") from exc

        if file_size > max_size:
            raise DeserializationError(
                f"File too large: {file_size} bytes (max: {max_size} bytes). "
                f"File: {os.path.basename(file_path)}"
            )
        if file_size == 0:
            raise DeserializationError(f"File is empty: {os.path.basename(file_path)}")

    def deserialize(
        self,
        design_file_path: str,
        output_json_path: Optional[str] = None,
        timeout: Optional[int] = None,
        max_file_size: Optional[int] = None,
    ) -> str:
        """Deserialize an AWD design/service file to JSON.

        Args:
            design_file_path: Path to the input .design or .service file.
            output_json_path: Path to write JSON output (optional; temp file if omitted).
            timeout: Subprocess timeout in seconds.
            max_file_size: Override max allowed input size in bytes.

        Returns:
            Path to the output JSON file.

        Raises:
            DeserializationError: Validation failure or unexpected error.
            JavaBridgeError: Java engine failed, timed out, or JAR missing.
            AwdFileNotFoundError: Input file not found.
        """
        design_file_path = self._validate_file_path(design_file_path)
        if not os.path.isfile(design_file_path):
            raise AwdFileNotFoundError(f"Design file not found: {design_file_path}")
        self._validate_file_size(design_file_path, max_file_size)
        logger.info("Validated input file: %s", os.path.basename(design_file_path))

        if output_json_path is None:
            temp_dir = tempfile.mkdtemp(prefix="awd_")
            filename = Path(design_file_path).stem + ".json"
            output_json_path = os.path.join(temp_dir, filename)

        output_json_path = os.path.abspath(output_json_path)
        os.makedirs(os.path.dirname(output_json_path), exist_ok=True)

        effective_timeout = timeout or self.config.default_timeout or 300

        if not self.jar_path.exists():
            raise JavaBridgeError(
                f"Java engine JAR not found at {self.jar_path}. "
                "Run `mvn package` from the repo root."
            )

        cmd = [
            "java",
            *self.config.to_java_opts(),
            "-jar", str(self.jar_path),
            design_file_path,
            output_json_path,
        ]
        logger.info("Starting deserialization of %s", os.path.basename(design_file_path))
        logger.debug("Command: %s", cmd)
        logger.debug("Timeout: %ss", effective_timeout)

        try:
            result = subprocess.run(
                cmd,
                capture_output=True,
                timeout=effective_timeout,
                check=True,
            )
        except subprocess.CalledProcessError as exc:
            stderr = exc.stderr.decode(errors="replace") if exc.stderr else ""
            stdout = exc.stdout.decode(errors="replace") if exc.stdout else ""
            raise JavaBridgeError(
                f"Java engine failed (exit {exc.returncode}). "
                f"stderr={stderr!r} stdout={stdout!r}"
            ) from exc
        except subprocess.TimeoutExpired as exc:
            raise JavaBridgeError(
                f"Java engine timed out after {effective_timeout}s"
            ) from exc

        stdout = result.stdout.decode("utf-8", errors="replace") if result.stdout else ""
        stderr = result.stderr.decode("utf-8", errors="replace") if result.stderr else ""
        if stderr:
            logger.debug("Engine stderr: %s", stderr)

        if not os.path.isfile(output_json_path):
            raise DeserializationError(
                "Deserialization did not produce expected output file.\n"
                f"Expected: {output_json_path}\n"
                f"Engine stdout: {stdout}\n"
                f"Engine stderr: {stderr}"
            )

        output_size = os.path.getsize(output_json_path)
        if output_size == 0:
            raise DeserializationError(
                f"Output file is empty. Engine stdout: {stdout!r}, stderr: {stderr!r}"
            )

        logger.info(
            "Successfully deserialized to: %s (%s)",
            output_json_path,
            self._format_bytes(output_size),
        )
        return output_json_path

    def deserialize_to_dict(
        self,
        design_file_path: str,
        timeout: Optional[int] = None,
        cleanup: bool = True,
    ) -> Dict[str, Any]:
        """Deserialize a file and return the parsed JSON as a dict."""
        temp_dir = tempfile.mkdtemp(prefix="awd_")
        try:
            filename = Path(design_file_path).stem + ".json"
            output_path = os.path.join(temp_dir, filename)
            self.deserialize(design_file_path, output_path, timeout)
            with open(output_path, "r", encoding="utf-8") as fh:
                return json.load(fh)
        except json.JSONDecodeError:
            logger.error("Failed to parse JSON output")
            raise
        finally:
            if cleanup:
                try:
                    shutil.rmtree(temp_dir)
                    logger.debug("Cleaned up temporary directory: %s", temp_dir)
                except Exception as exc:
                    logger.warning("Failed to clean up temporary directory: %s", exc)

    @staticmethod
    def _format_bytes(bytes_count: int) -> str:
        """Format bytes to a human-readable string."""
        if bytes_count < 1024:
            return f"{bytes_count} bytes"
        if bytes_count < 1024 * 1024:
            return f"{bytes_count / 1024:.2f} KB"
        if bytes_count < 1024 * 1024 * 1024:
            return f"{bytes_count / (1024 * 1024):.2f} MB"
        return f"{bytes_count / (1024 * 1024 * 1024):.2f} GB"
