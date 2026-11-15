
from pathlib import Path
import csv

BASE = Path(r"C:\Users\BHANU TEJA\Downloads")
VERSIONS = ["1.9.4", "1.9.5", "1.9.6"]


def count_java_lines(path):
    code = comments = blank = 0
    state = "normal"

    for line in path.read_text(
        encoding="utf-8", errors="replace"
    ).splitlines():

        has_code = False
        has_comment = False
        i = 0

        while i < len(line):
            if state == "block":
                end = line.find("*/", i)
                if end == -1:
                    if line[i:].strip():
                        has_comment = True
                    i = len(line)
                else:
                    if line[i:end].strip():
                        has_comment = True
                    state = "normal"
                    i = end + 2
                continue

            if state == "text_block":
                has_code = True
                end = line.find('"""', i)
                if end == -1:
                    i = len(line)
                else:
                    state = "normal"
                    i = end + 3
                continue

            if state in ("string", "char"):
                has_code = True
                quote = '"' if state == "string" else "'"

                if line[i] == "\\":
                    i += 2
                elif line[i] == quote:
                    state = "normal"
                    i += 1
                else:
                    i += 1
                continue

            if line[i].isspace():
                i += 1
                continue

            if line.startswith("//", i):
                has_comment = True
                break

            if line.startswith("/*", i):
                has_comment = True
                state = "block"
                i += 2
                continue

            if line.startswith('"""', i):
                has_code = True
                end = line.find('"""', i + 3)
                if end == -1:
                    state = "text_block"
                    i = len(line)
                else:
                    i = end + 3
                continue

            if line[i] == '"':
                has_code = True
                state = "string"
                i += 1
                continue

            if line[i] == "'":
                has_code = True
                state = "char"
                i += 1
                continue

            has_code = True
            i += 1

        if has_code:
            code += 1
        elif has_comment:
            comments += 1
        else:
            blank += 1

    return code, comments, blank


results = []

for version in VERSIONS:
    source = BASE / f"Apache-Ant-{version}" / "src" / "main"

    if not source.is_dir():
        raise FileNotFoundError(source)

    files = sorted(source.rglob("*.java"))
    totals = [0, 0, 0]

    for file in files:
        counts = count_java_lines(file)
        totals = [a + b for a, b in zip(totals, counts)]

    results.append({
        "Version": version,
        "JavaFiles": len(files),
        "CodeLines": totals[0],
        "CommentLines": totals[1],
        "BlankLines": totals[2],
        "TotalPhysicalLines": sum(totals),
    })

output = Path("ant_loc_measurements_final.csv")

with output.open("w", newline="", encoding="utf-8") as f:
    writer = csv.DictWriter(f, fieldnames=results[0].keys())
    writer.writeheader()
    writer.writerows(results)

print("\nFINAL JAVA SOURCE MEASUREMENTS")
for result in results:
    print(result)

print(f"\nSaved report: {output.resolve()}")
