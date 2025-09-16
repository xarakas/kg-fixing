#!/usr/bin/env python3
import re
from pathlib import Path

# Input file (the one you uploaded)
in_path = Path("sampled.txt")   # change to your actual path
# Output file with unique IRIs
out_path = Path("iris.txt")

# Regex to match IRIs in angle brackets
iri_pattern = re.compile(r"<([^>]+)>")

iris = set()

with in_path.open("r", encoding="utf-8", errors="ignore") as f:
    for line in f:
        for m in iri_pattern.finditer(line):
            iris.add(m.group(1))

# Sort for reproducibility
sorted_iris = sorted(iris)

# Write one IRI per line
out_path.write_text("\n".join(sorted_iris), encoding="utf-8")

print(f"Extracted {len(sorted_iris)} unique IRIs -> {out_path}")
