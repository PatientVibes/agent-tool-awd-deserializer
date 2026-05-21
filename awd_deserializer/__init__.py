"""AWD .design / .service file deserializer.

A thin Python CLI/library wrapping a JAR-free Java engine that performs
reflection-based ObjectInputStream deserialization of AWD design and
service artefacts into structured JSON.

This tool deals ONLY with local file formats. Anything that talks to a
running Chorus server belongs in chorus-mcp-server, not here.
"""

from awd_deserializer.client import AwdDeserializer
from awd_deserializer.config import DeserializerConfig
from awd_deserializer.exceptions import DeserializationError, JavaBridgeError

__version__ = "0.1.0"
__all__ = ["AwdDeserializer", "DeserializerConfig", "DeserializationError", "JavaBridgeError"]
