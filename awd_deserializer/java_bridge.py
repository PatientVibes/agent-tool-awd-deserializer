"""Java-Python integration bridge for AWD file extraction.

Invokes the Maven-built Java engine JAR via direct subprocess. No Docker, no
shell wrapper — just `java -jar`. Higher-level extractors (BPMN, forms,
services) wrap this with their own per-type request/result dataclasses.
"""

from __future__ import annotations

import asyncio
import json
import logging
import os
import shutil
import subprocess
import time
from contextlib import asynccontextmanager
from dataclasses import dataclass
from pathlib import Path
from typing import Any

from .exceptions import JavaBridgeError

logger = logging.getLogger(__name__)


# Default JAR location (set by Maven `mvn package` in the repo root).
DEFAULT_JAR = Path(__file__).resolve().parent.parent / "target" / "awd-deserializer-engine-0.1.0.jar"


def run_engine(input_path: Path, *args: str, jar: Path = DEFAULT_JAR, timeout: int = 300) -> bytes:
    """Run the Java engine against an input file, return stdout bytes.

    Args:
        input_path: Path to the .design or .service file.
        *args: Additional CLI args passed to the engine.
        jar: Path to the engine JAR (defaults to the Maven build output).
        timeout: Subprocess timeout in seconds.

    Raises:
        JavaBridgeError: JAR not found, non-zero exit, or timeout.
    """
    jar = Path(jar)
    if not jar.exists():
        raise JavaBridgeError(
            f"Java engine JAR not found at {jar}. Run `mvn package` from the repo root."
        )
    cmd = ["java", "-jar", str(jar), str(input_path), *args]
    try:
        result = subprocess.run(cmd, capture_output=True, timeout=timeout, check=True)
    except subprocess.CalledProcessError as exc:
        raise JavaBridgeError(
            f"Java engine failed (exit {exc.returncode}): {exc.stderr.decode(errors='replace')}"
        ) from exc
    except subprocess.TimeoutExpired as exc:
        raise JavaBridgeError(f"Java engine timed out after {timeout}s") from exc
    return result.stdout


@dataclass
class ExtractionRequest:
    """Configuration for an AWD file extraction request."""
    source_file: str
    output_directory: str
    extraction_types: list[str]  # bpmn, forms, services, all
    source_name: str | None = None
    dation_format: bool = False
    include_metadata: bool = True
    max_concurrent: int = 4
    timeout_minutes: int = 10


@dataclass
class ExtractionResult:
    """Results from an AWD file extraction."""
    job_id: str
    status: str  # success, error, timeout
    source_name: str
    output_directory: str
    files_generated: dict[str, list[str]]
    total_files: int
    processing_time_ms: int
    errors: list[str]
    metadata: dict[str, Any]


@dataclass
class JavaProcessConfig:
    """Configuration for Java process execution."""
    heap_size: str = "2g"
    max_memory: str = "3g"
    timeout_seconds: int = 600
    jar_path: str | None = None
    java_opts: list[str] = None
    working_directory: str | None = None


class ProcessManager:
    """Manages Java process lifecycle and monitoring."""

    def __init__(self):
        self.active_processes: dict[str, subprocess.Popen] = {}

    async def run_java_process(
        self,
        command: list[str],
        config: JavaProcessConfig,
        job_id: str
    ) -> tuple[str, str, int]:
        """Run Java process with monitoring and timeout control."""

        logger.info(f"Starting Java process for job {job_id}")
        logger.debug(f"Command: {command}")

        try:
            # Create process with proper configuration
            process = await asyncio.create_subprocess_exec(
                *command,
                stdout=asyncio.subprocess.PIPE,
                stderr=asyncio.subprocess.PIPE,
                cwd=config.working_directory,
                env=dict(os.environ, **self._get_java_env(config))
            )

            self.active_processes[job_id] = process

            # Wait for completion with timeout
            try:
                stdout, stderr = await asyncio.wait_for(
                    process.communicate(),
                    timeout=config.timeout_seconds
                )

                exit_code = process.returncode
                stdout_str = stdout.decode('utf-8') if stdout else ""
                stderr_str = stderr.decode('utf-8') if stderr else ""

                logger.info(f"Java process completed with exit code {exit_code}")
                return stdout_str, stderr_str, exit_code

            except asyncio.TimeoutError as exc:
                logger.error(f"Java process timed out after {config.timeout_seconds} seconds")
                process.kill()
                await process.wait()
                raise JavaBridgeError(
                    f"Java process timed out after {config.timeout_seconds} seconds"
                ) from exc

        except JavaBridgeError:
            raise
        except Exception as e:
            logger.error(f"Error running Java process: {e}")
            raise JavaBridgeError(f"Failed to execute Java process: {e}") from e

        finally:
            self.active_processes.pop(job_id, None)

    def _get_java_env(self, config: JavaProcessConfig) -> dict[str, str]:
        """Get environment variables for Java process."""
        env = {}

        if config.java_opts:
            env['JAVA_OPTS'] = ' '.join(config.java_opts)

        # Add memory settings
        env['JAVA_HEAP_INITIAL'] = config.heap_size.replace('g', 'G')
        env['JAVA_HEAP_MAX'] = config.max_memory.replace('g', 'G')

        return env

    def terminate_process(self, job_id: str) -> bool:
        """Terminate a running Java process."""
        process = self.active_processes.get(job_id)
        if process:
            try:
                process.terminate()
                return True
            except Exception as e:
                logger.error(f"Error terminating process {job_id}: {e}")
        return False


class ResultProcessor:
    """Process and validate Java extraction results."""

    @staticmethod
    def parse_extraction_result(stdout: str, stderr: str, exit_code: int, job_id: str) -> ExtractionResult:
        """Parse Java extraction output into structured result."""

        if exit_code != 0:
            error_msg = f"Java process failed with exit code {exit_code}"
            if stderr:
                error_msg += f": {stderr}"

            return ExtractionResult(
                job_id=job_id,
                status="error",
                source_name="unknown",
                output_directory="",
                files_generated={},
                total_files=0,
                processing_time_ms=0,
                errors=[error_msg],
                metadata={"exit_code": exit_code, "stderr": stderr}
            )

        try:
            # Look for JSON output in stdout
            result_data = ResultProcessor._extract_json_from_output(stdout)

            if not result_data:
                # Fallback: parse text output
                result_data = ResultProcessor._parse_text_output(stdout)

            return ExtractionResult(
                job_id=job_id,
                status=result_data.get("status", "success"),
                source_name=result_data.get("sourceName", "unknown"),
                output_directory=result_data.get("outputDirectory", ""),
                files_generated=result_data.get("filesGenerated", {}),
                total_files=result_data.get("totalFilesGenerated", 0),
                processing_time_ms=result_data.get("totalProcessingTime", 0),
                errors=result_data.get("errors", []),
                metadata=result_data.get("metadata", {})
            )

        except Exception as e:
            logger.error(f"Error parsing extraction result: {e}")
            return ExtractionResult(
                job_id=job_id,
                status="error",
                source_name="unknown",
                output_directory="",
                files_generated={},
                total_files=0,
                processing_time_ms=0,
                errors=[f"Failed to parse result: {e}"],
                metadata={"stdout": stdout, "stderr": stderr}
            )

    @staticmethod
    def _extract_json_from_output(output: str) -> dict[str, Any] | None:
        """Extract JSON result from Java output."""
        lines = output.split('\n')

        for line in lines:
            line = line.strip()
            if line.startswith('{') and line.endswith('}'):
                try:
                    return json.loads(line)
                except json.JSONDecodeError:
                    continue

        return None

    @staticmethod
    def _parse_text_output(output: str) -> dict[str, Any]:
        """Parse text-based output from Java (fallback)."""
        result = {
            "status": "success",
            "filesGenerated": {},
            "totalFilesGenerated": 0,
            "errors": []
        }

        lines = output.split('\n')
        for line in lines:
            line = line.strip()

            if "files generated" in line.lower():
                # Try to extract file count
                try:
                    count = int(''.join(filter(str.isdigit, line)))
                    result["totalFilesGenerated"] = count
                except ValueError:
                    pass

            elif "error" in line.lower() or "exception" in line.lower():
                result["errors"].append(line)

        return result


class AwdExtractorBridge:
    """Bridge to the Java engine for AWD file extraction operations."""

    def __init__(self, jar_path: str | None = None):
        self.process_manager = ProcessManager()
        self.jar_path = jar_path or self._find_jar_path()

    async def extract_files(self, request: ExtractionRequest) -> ExtractionResult:
        """Extract AWD files using the Java engine."""

        job_id = f"awd_{int(time.time() * 1000)}"
        logger.info(f"Starting AWD extraction job {job_id}")

        # Validate request
        self._validate_request(request)

        # Prepare command
        command = self._build_extraction_command(request)
        config = JavaProcessConfig(
            timeout_seconds=request.timeout_minutes * 60,
            jar_path=self.jar_path
        )

        try:
            # Execute Java process
            stdout, stderr, exit_code = await self.process_manager.run_java_process(
                command, config, job_id
            )

            # Process results
            result = ResultProcessor.parse_extraction_result(stdout, stderr, exit_code, job_id)

            logger.info(f"AWD extraction completed: {result.total_files} files generated")
            return result

        except JavaBridgeError:
            raise
        except Exception as e:
            logger.error(f"AWD extraction failed: {e}")
            raise JavaBridgeError(f"AWD extraction failed: {e}") from e

    async def extract_bpmn_files(self, source_file: str, output_dir: str, **kwargs) -> ExtractionResult:
        """Extract only BPMN files."""
        request = ExtractionRequest(
            source_file=source_file,
            output_directory=output_dir,
            extraction_types=["bpmn"],
            **kwargs
        )
        return await self.extract_files(request)

    async def extract_form_files(self, source_file: str, output_dir: str, dation_format: bool = False, **kwargs) -> ExtractionResult:
        """Extract only form files."""
        request = ExtractionRequest(
            source_file=source_file,
            output_directory=output_dir,
            extraction_types=["forms"],
            dation_format=dation_format,
            **kwargs
        )
        return await self.extract_files(request)

    async def extract_service_files(self, source_file: str, output_dir: str, **kwargs) -> ExtractionResult:
        """Extract only service configuration files."""
        request = ExtractionRequest(
            source_file=source_file,
            output_directory=output_dir,
            extraction_types=["services"],
            **kwargs
        )
        return await self.extract_files(request)

    def _validate_request(self, request: ExtractionRequest) -> None:
        """Validate extraction request parameters."""
        if not os.path.exists(request.source_file):
            raise JavaBridgeError(f"Source file not found: {request.source_file}")

        if not request.source_file.lower().endswith(('.design', '.service')):
            raise JavaBridgeError(
                f"Invalid file format. Expected .design or .service file: {request.source_file}"
            )

        # Create output directory if it doesn't exist
        os.makedirs(request.output_directory, exist_ok=True)

        if not request.extraction_types:
            raise JavaBridgeError("At least one extraction type must be specified")

        valid_types = {"bpmn", "forms", "services", "all"}
        for extraction_type in request.extraction_types:
            if extraction_type not in valid_types:
                raise JavaBridgeError(f"Invalid extraction type: {extraction_type}")

    def _build_extraction_command(self, request: ExtractionRequest) -> list[str]:
        """Build Java command for AWD extraction."""

        command = [
            "java",
            "-Xms1g",
            "-Xmx2g",
            "-jar", self.jar_path,
            "extract-awd",
            "--input", request.source_file,
            "--output", request.output_directory,
            "--types", ",".join(request.extraction_types)
        ]

        if request.source_name:
            command.extend(["--source-name", request.source_name])

        if request.dation_format:
            command.append("--dation-format")

        if not request.include_metadata:
            command.append("--no-metadata")

        command.extend(["--max-concurrent", str(request.max_concurrent)])

        return command

    def _find_jar_path(self) -> str:
        """Find the AWD deserializer JAR file."""

        # Check common locations
        possible_paths = [
            "target/awd-deserializer-engine-0.1.0.jar",
            "../target/awd-deserializer-engine-0.1.0.jar",
            "/app/awd-deserializer-engine.jar",
            os.path.expanduser("~/awd-deserializer-engine.jar")
        ]

        for path in possible_paths:
            if os.path.exists(path):
                return os.path.abspath(path)

        raise JavaBridgeError(
            "AWD deserializer JAR file not found. "
            "Please build the project or specify jar_path"
        )


class JavaBridge:
    """Main Java-Python integration bridge."""

    def __init__(self):
        self.awd_extractor = AwdExtractorBridge()

    async def health_check(self) -> dict[str, Any]:
        """Check if Java components are available and working."""

        try:
            # Try to run a simple Java command
            process = await asyncio.create_subprocess_exec(
                "java", "-version",
                stdout=asyncio.subprocess.PIPE,
                stderr=asyncio.subprocess.PIPE
            )

            stdout, stderr = await asyncio.wait_for(process.communicate(), timeout=10)

            java_version = stderr.decode('utf-8') if stderr else stdout.decode('utf-8')

            return {
                "status": "healthy",
                "java_available": True,
                "java_version": java_version.split('\n')[0] if java_version else "unknown",
                "jar_path": self.awd_extractor.jar_path,
                "jar_exists": os.path.exists(self.awd_extractor.jar_path)
            }

        except Exception as e:
            return {
                "status": "unhealthy",
                "java_available": False,
                "error": str(e)
            }

    @asynccontextmanager
    async def managed_extraction(self, request: ExtractionRequest):
        """Context manager for safe extraction with cleanup."""
        temp_dirs = []

        try:
            # Create temporary directories if needed
            if not os.path.exists(request.output_directory):
                temp_dirs.append(request.output_directory)
                os.makedirs(request.output_directory, exist_ok=True)

            yield self.awd_extractor

        finally:
            # Cleanup temporary directories if extraction failed
            for temp_dir in temp_dirs:
                if os.path.exists(temp_dir) and not os.listdir(temp_dir):
                    try:
                        shutil.rmtree(temp_dir)
                    except Exception as e:
                        logger.warning(f"Failed to cleanup temp directory {temp_dir}: {e}")


# Global bridge instance for reuse
_bridge_instance = None

def get_java_bridge() -> JavaBridge:
    """Get or create global Java bridge instance."""
    global _bridge_instance
    if _bridge_instance is None:
        _bridge_instance = JavaBridge()
    return _bridge_instance


# Convenience functions for direct use
async def extract_bpmn_files(source_file: str, output_dir: str, **kwargs) -> ExtractionResult:
    """Extract BPMN files using Java bridge."""
    bridge = get_java_bridge()
    return await bridge.awd_extractor.extract_bpmn_files(source_file, output_dir, **kwargs)


async def extract_form_files(source_file: str, output_dir: str, dation_format: bool = False, **kwargs) -> ExtractionResult:
    """Extract form files using Java bridge."""
    bridge = get_java_bridge()
    return await bridge.awd_extractor.extract_form_files(source_file, output_dir, dation_format, **kwargs)


async def extract_service_files(source_file: str, output_dir: str, **kwargs) -> ExtractionResult:
    """Extract service files using Java bridge."""
    bridge = get_java_bridge()
    return await bridge.awd_extractor.extract_service_files(source_file, output_dir, **kwargs)


async def extract_all_files(source_file: str, output_dir: str, **kwargs) -> ExtractionResult:
    """Extract all AWD files using Java bridge."""
    bridge = get_java_bridge()
    request = ExtractionRequest(
        source_file=source_file,
        output_directory=output_dir,
        extraction_types=["all"],
        **kwargs
    )
    return await bridge.awd_extractor.extract_files(request)
