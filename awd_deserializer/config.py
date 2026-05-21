"""Configuration for the AWD deserializer."""

from __future__ import annotations

from dataclasses import dataclass
from typing import List, Optional


@dataclass
class DeserializerConfig:
    """Runtime configuration for the AWD deserializer.

    Knobs control the Java engine subprocess (heap size, memory limit,
    batch size, focused-mode toggle) and the default timeout. Defaults
    match the upstream chorus-deserializer behaviour.
    """

    # Engine memory settings
    memory: str = "4g"
    heap_size: str = "3g"

    # Processing settings
    memory_limit_mb: int = 3200
    batch_size: int = 200
    focused_mode: bool = True
    safe_reflection: bool = True

    # Timeout settings
    default_timeout: Optional[int] = None

    def to_java_opts(self) -> List[str]:
        """Convert configuration to Java system properties / heap flags."""
        opts: List[str] = []
        if self.heap_size:
            opts.append(f"-Xmx{self.heap_size}")
        if self.memory_limit_mb > 0:
            opts.append(f"-Dmemory.limit.mb={self.memory_limit_mb}")
        opts.append(f"-Dbatch.size={self.batch_size}")
        opts.append(f"-Dfocused.mode={str(self.focused_mode).lower()}")
        opts.append(f"-Dsafe.reflection={str(self.safe_reflection).lower()}")
        return opts
