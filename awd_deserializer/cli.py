"""awd-deserialize - extract structured JSON from AWD .design / .service files.

A thin Python CLI wrapping the Java engine JAR. Supports:
  - The default deserialize flow (one file in, one JSON out).
  - Type-specific extraction subcommands (extract-bpmn, extract-forms,
    extract-services, extract-all).
  - File inspect for quick metadata.

The Docker, rate-limiting, batch-processing, and API-server surfaces from
the upstream chorus-adapter are deliberately omitted - this tool is a
file-format converter, not a service.
"""

from __future__ import annotations

import argparse
import asyncio
import json
import logging
import sys
from pathlib import Path
from typing import List, Optional

from . import __version__
from .client import AwdDeserializer
from .config import DeserializerConfig
from .exceptions import AwdDeserializerError, JavaBridgeError
from .java_bridge import (
    extract_all_files,
    extract_bpmn_files,
    extract_form_files,
    extract_service_files,
)

logging.basicConfig(level=logging.INFO, format="[%(levelname)s] %(message)s")
logger = logging.getLogger(__name__)


def create_parser() -> argparse.ArgumentParser:
    """Build the argparse parser."""
    parser = argparse.ArgumentParser(
        prog="awd-deserialize",
        description="awd-deserialize - extract structured JSON from AWD design/service files",
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    parser.add_argument(
        "--version",
        action="version",
        version=f"%(prog)s {__version__}",
    )

    subparsers = parser.add_subparsers(dest="command", help="Command to execute")

    deser = subparsers.add_parser("deserialize", help="Deserialize a single AWD file")
    deser.add_argument("input", help="Path to the .design or .service file")
    deser.add_argument("--output", "-o", help="Path to output JSON file")
    deser.add_argument("--pretty", action="store_true", help="Pretty-print JSON output")
    deser.add_argument("--heap", default="3g", help="Java heap size (default 3g)")
    deser.add_argument("--memory-limit", type=int, default=3200, help="Memory limit in MB")
    deser.add_argument("--batch-size", type=int, default=200, help="Processing batch size")
    deser.add_argument("--timeout", type=int, help="Timeout in seconds")
    deser.add_argument(
        "--no-focused-mode", action="store_true", help="Disable focused extraction"
    )
    deser.add_argument("--debug", action="store_true", help="Enable debug logging")

    inspect = subparsers.add_parser("inspect", help="Inspect a design file and show metadata")
    inspect.add_argument("input", help="Path to the .design or .service file")
    inspect.add_argument("--debug", action="store_true", help="Enable debug logging")

    extract_bpmn = subparsers.add_parser(
        "extract-bpmn", help="Extract BPMN workflow files from a design file"
    )
    extract_bpmn.add_argument("input", help="Path to the design file")
    extract_bpmn.add_argument(
        "--output-dir", "-o", required=True, help="Output directory for BPMN files"
    )
    extract_bpmn.add_argument("--source-name", help="Name for the source (defaults to filename)")
    extract_bpmn.add_argument("--timeout", type=int, default=10, help="Timeout in minutes")
    extract_bpmn.add_argument(
        "--max-concurrent", type=int, default=4, help="Max concurrent extractions"
    )
    extract_bpmn.add_argument("--debug", action="store_true", help="Enable debug logging")

    extract_forms = subparsers.add_parser(
        "extract-forms", help="Extract form definitions from a design file"
    )
    extract_forms.add_argument("input", help="Path to the design file")
    extract_forms.add_argument(
        "--output-dir", "-o", required=True, help="Output directory for form files"
    )
    extract_forms.add_argument("--source-name", help="Name for the source (defaults to filename)")
    extract_forms.add_argument(
        "--dation-format", action="store_true", help="Generate dation-compatible format"
    )
    extract_forms.add_argument("--timeout", type=int, default=10, help="Timeout in minutes")
    extract_forms.add_argument(
        "--max-concurrent", type=int, default=4, help="Max concurrent extractions"
    )
    extract_forms.add_argument("--debug", action="store_true", help="Enable debug logging")

    extract_services = subparsers.add_parser(
        "extract-services", help="Extract service configuration files from a design file"
    )
    extract_services.add_argument("input", help="Path to the design file")
    extract_services.add_argument(
        "--output-dir", "-o", required=True, help="Output directory for service files"
    )
    extract_services.add_argument(
        "--source-name", help="Name for the source (defaults to filename)"
    )
    extract_services.add_argument("--timeout", type=int, default=10, help="Timeout in minutes")
    extract_services.add_argument(
        "--max-concurrent", type=int, default=4, help="Max concurrent extractions"
    )
    extract_services.add_argument("--debug", action="store_true", help="Enable debug logging")

    extract_all = subparsers.add_parser(
        "extract-all", help="Extract all AWD artefacts (BPMN, forms, services) from a design file"
    )
    extract_all.add_argument("input", help="Path to the design file")
    extract_all.add_argument(
        "--output-dir", "-o", required=True, help="Output directory for all artefacts"
    )
    extract_all.add_argument("--source-name", help="Name for the source (defaults to filename)")
    extract_all.add_argument(
        "--dation-format", action="store_true", help="Generate dation-compatible format for forms"
    )
    extract_all.add_argument("--timeout", type=int, default=15, help="Timeout in minutes")
    extract_all.add_argument(
        "--max-concurrent", type=int, default=4, help="Max concurrent extractions"
    )
    extract_all.add_argument("--debug", action="store_true", help="Enable debug logging")

    return parser


def handle_deserialize(args: argparse.Namespace) -> int:
    """Run the default deserialize flow."""
    if args.debug:
        logging.getLogger().setLevel(logging.DEBUG)

    config = DeserializerConfig(
        heap_size=args.heap,
        memory_limit_mb=args.memory_limit,
        batch_size=args.batch_size,
        focused_mode=not args.no_focused_mode,
        default_timeout=args.timeout,
    )

    try:
        deserializer = AwdDeserializer(config=config, debug=args.debug)
        output_path = deserializer.deserialize(args.input, args.output, args.timeout)
    except AwdDeserializerError as exc:
        print(f"error: {exc}", file=sys.stderr)
        return 1

    if args.pretty and args.output is None:
        with open(output_path, "r", encoding="utf-8") as fh:
            data = json.load(fh)
        sys.stdout.write(json.dumps(data, indent=2) + "\n")
    else:
        print(f"Successfully deserialized to: {output_path}")
    return 0


def handle_inspect(args: argparse.Namespace) -> int:
    """Show basic metadata about an input file."""
    if args.debug:
        logging.getLogger().setLevel(logging.DEBUG)

    file_path = Path(args.input)
    if not file_path.exists():
        print(f"Error: File not found: {file_path}", file=sys.stderr)
        return 1

    print(f"File: {file_path.name}")
    print(f"Size: {file_path.stat().st_size:,} bytes")
    print(f"Path: {file_path.absolute()}")

    try:
        with open(file_path, "rb") as fh:
            header = fh.read(16)
    except Exception as exc:
        print(f"Error reading file: {exc}", file=sys.stderr)
        return 1

    if header[:2] == b"\x1f\x8b":
        print("Type: GZIP-compressed (typical AWD .design / .service)")
    else:
        print("Type: Unknown (not GZIP)")
    return 0


async def _handle_extract(args: argparse.Namespace, kind: str) -> int:
    """Shared async handler for the four extract-* subcommands."""
    if args.debug:
        logging.getLogger().setLevel(logging.DEBUG)

    source_name = args.source_name or Path(args.input).stem
    print(f"Extracting {kind} from: {args.input}")
    print(f"Output directory: {args.output_dir}")

    try:
        if kind == "bpmn":
            result = await extract_bpmn_files(
                source_file=args.input,
                output_dir=args.output_dir,
                source_name=source_name,
                timeout_minutes=args.timeout,
                max_concurrent=args.max_concurrent,
            )
        elif kind == "forms":
            result = await extract_form_files(
                source_file=args.input,
                output_dir=args.output_dir,
                source_name=source_name,
                dation_format=getattr(args, "dation_format", False),
                timeout_minutes=args.timeout,
                max_concurrent=args.max_concurrent,
            )
        elif kind == "services":
            result = await extract_service_files(
                source_file=args.input,
                output_dir=args.output_dir,
                source_name=source_name,
                timeout_minutes=args.timeout,
                max_concurrent=args.max_concurrent,
            )
        else:  # all
            result = await extract_all_files(
                source_file=args.input,
                output_dir=args.output_dir,
                source_name=source_name,
                dation_format=getattr(args, "dation_format", False),
                timeout_minutes=args.timeout,
                max_concurrent=args.max_concurrent,
            )
    except JavaBridgeError as exc:
        print(f"error: {exc}", file=sys.stderr)
        return 1

    if result.status == "success":
        print(
            f"\nExtraction completed: {result.total_files} files generated "
            f"in {result.processing_time_ms}ms"
        )
        for file_type, files in result.files_generated.items():
            if not files:
                continue
            print(f"\n  {file_type.upper()} files ({len(files)}):")
            for fp in files[:5]:
                print(f"    - {Path(fp).name}")
            if len(files) > 5:
                print(f"    ... and {len(files) - 5} more")
        return 0

    print("\nExtraction failed:", file=sys.stderr)
    for error in result.errors:
        print(f"  {error}", file=sys.stderr)
    return 1


def main(argv: Optional[List[str]] = None) -> int:
    """CLI entry point."""
    parser = create_parser()
    args = parser.parse_args(argv)

    if not args.command:
        parser.print_help()
        return 1

    try:
        if args.command == "deserialize":
            return handle_deserialize(args)
        if args.command == "inspect":
            return handle_inspect(args)
        if args.command == "extract-bpmn":
            return asyncio.run(_handle_extract(args, "bpmn"))
        if args.command == "extract-forms":
            return asyncio.run(_handle_extract(args, "forms"))
        if args.command == "extract-services":
            return asyncio.run(_handle_extract(args, "services"))
        if args.command == "extract-all":
            return asyncio.run(_handle_extract(args, "all"))
        parser.print_help()
        return 1
    except KeyboardInterrupt:
        print("\nOperation cancelled by user", file=sys.stderr)
        return 130
    except Exception as exc:
        logger.error("Error: %s", exc)
        if getattr(args, "debug", False):
            logger.exception("Full traceback:")
        return 1


if __name__ == "__main__":
    sys.exit(main())
