# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.0] — 2026-05-21

Initial public release. Extracted from a private `chorus-extractor`
prototype, scoped down to a single CLI tool with no API surface.

### Added
- `awd-deserialize` CLI with subcommands: `deserialize`, `inspect`,
  `extract-bpmn`, `extract-forms`, `extract-services`, `extract-all`.
- Java engine (`src/main/java/com/patientvibes/awd/deserializer/`) doing
  reflection-based AWD deserialization. JAR-free.
- Python adapter (`awd_deserializer/`) wrapping the engine via subprocess.
- pytest suite (unit + integration) with synthetic `.design` fixtures.

### Notes
- Dropped from the original prototype: Flask API server, WebSocket manager,
  rate limiter, multi-cloud secrets manager, monitoring/health endpoints,
  Docker bridge. Out of scope for a CLI deserializer.
- 50+ root-level scratch scripts from the prototype were not ported.
- Several pre-existing test files exercising customer-specific fixtures
  were dropped; the test suite was rewritten against synthetic generators.
