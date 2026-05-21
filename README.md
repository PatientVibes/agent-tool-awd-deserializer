# agent-tool-awd-deserializer

Deserialize AWD `.design` and `.service` files into structured JSON.

These are the gzip-compressed Java-serialized `DeploymentPackage` files
Chorus Design Studio exports — BPMN XML blocks, embedded JSON form
definitions, field mappings, custom data types, and model metadata
in a single binary artefact. This tool extracts them as JSON.

**Boundary:** local file format work only. Anything that talks to a
running Chorus server lives in [`chorus-mcp-server`](https://github.com/PatientVibes/chorus-mcp-server).

## Install

Requires Python >= 3.10 and Java >= 17 on PATH.

```bash
git clone https://github.com/PatientVibes/agent-tool-awd-deserializer
cd agent-tool-awd-deserializer
mvn -B package -DskipTests   # build the Java engine
uv tool install --editable .
```

Verify:

```bash
awd-deserialize --version
# awd-deserialize 0.1.0
```

## Usage

```bash
# Deserialize a .design file to a JSON file
awd-deserialize deserialize path/to/Process.design -o process.json

# Pretty-print to stdout (omit -o)
awd-deserialize deserialize path/to/Process.design --pretty

# Inspect (lighter — metadata only, no Java engine call)
awd-deserialize inspect path/to/Process.design

# Extract individual components (-o / --output-dir is required)
awd-deserialize extract-bpmn     path/to/Process.design -o bpmn/
awd-deserialize extract-forms    path/to/Process.design -o forms/
awd-deserialize extract-services path/to/Process.design -o services/
awd-deserialize extract-all      path/to/Process.design -o output/
```

The deserialize output JSON contains the full `DeploymentPackage` graph — see
the [Chorus design-studio-files concept page](https://github.com/PatientVibes/chorus-wiki/blob/main/wiki/concepts/design-studio-files.md)
for the structure.

## Architecture

| Layer | What | Where |
|---|---|---|
| Java engine | Reflection-based `ObjectInputStream` deserialization + Avro WorkObject support. JAR-free — no proprietary AWD JARs vendored. | `src/main/java/com/patientvibes/awd/deserializer/` |
| Python adapter | Thin CLI + library that drives the engine via subprocess. | `awd_deserializer/` |

## Development

```bash
pip install -e .[dev]
mvn -B package -DskipTests   # required for integration tests
pytest                        # unit + integration
pytest -m unit                # just unit (integration skips without Maven)
ruff check awd_deserializer/
mypy awd_deserializer/
```

## License

MIT — see [LICENSE](LICENSE). Copyright (c) 2026 Chris Moore.

## Status

v0.1.0 — initial public extraction. Phase 1 of a three-phase plan
(see the [`ai-agents` catalog](https://github.com/PatientVibes/ai-agents)
for the surrounding agent ecosystem; future phases layer skills under
`agent-skills/` and a `agent-harness-chorus-design-analyzer` harness
on top of this tool).
