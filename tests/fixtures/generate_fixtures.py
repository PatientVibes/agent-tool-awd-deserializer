"""
Module: generate_fixtures - Script to generate all test fixture files

Summary:
    Utility script to generate all test fixture files for the Chorus Deserializer.
    Creates sample .design files and their expected JSON outputs.

Keywords: generate, fixtures, test, files, design, json, chorus, deserializer
Dependencies: mock_data_generator, test_constants
"""

import sys
from pathlib import Path

# Add parent directory to path for imports
sys.path.insert(0, str(Path(__file__).parent.parent.parent))

from tests.fixtures.mock_data_generator import create_all_test_fixtures
from tests.fixtures.test_constants import DATA_DIR, EXPECTED_DIR


def main():
    """Generate all test fixtures."""
    print("Generating test fixtures for Chorus Deserializer...")
    
    # Ensure directories exist
    DATA_DIR.mkdir(parents=True, exist_ok=True)
    EXPECTED_DIR.mkdir(parents=True, exist_ok=True)
    
    # Generate fixtures
    fixtures = create_all_test_fixtures(DATA_DIR, EXPECTED_DIR)
    
    print(f"\nGenerated {len(fixtures)} fixture files:")
    for name, path in fixtures.items():
        print(f"  - {name}: {path}")
    
    print("\nFixture generation complete!")


if __name__ == "__main__":
    main()