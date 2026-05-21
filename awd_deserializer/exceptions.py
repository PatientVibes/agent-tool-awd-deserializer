"""Custom exceptions for the AWD deserializer."""


class AwdDeserializerError(Exception):
    """Base exception for all AWD deserializer errors."""
    pass


class DeserializationError(AwdDeserializerError):
    """Raised when design file deserialization fails."""
    pass


class JavaBridgeError(AwdDeserializerError):
    """Raised when the Java subprocess bridge fails (process error, missing JAR, timeout, etc.)."""
    pass


class ConfigurationError(AwdDeserializerError):
    """Raised when configuration is invalid."""
    pass


class FileNotFoundError(AwdDeserializerError):
    """Raised when a required file is not found."""
    pass
