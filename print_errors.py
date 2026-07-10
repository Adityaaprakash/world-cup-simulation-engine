import os

log_path = 'startup.log'
if not os.path.exists(log_path):
    with open('clean_errors.txt', 'w', encoding='utf-8') as out:
        out.write("startup.log not found!")
    exit(1)

with open(log_path, 'rb') as f:
    content = f.read().decode('utf-16', errors='ignore')

lines = content.splitlines()

# Write output to clean_errors.txt in UTF-8
with open('clean_errors.txt', 'w', encoding='utf-8') as out:
    out.write(f"Total lines: {len(lines)}\n")
    for idx, line in enumerate(lines):
        if 'Exception' in line or 'ERROR' in line or 'Caused by' in line:
            start = max(0, idx - 5)
            end = min(len(lines), idx + 45)
            out.write(f"--- Context around line {idx} ---\n")
            for i in range(start, end):
                out.write(f"{i}: {lines[i]}\n")
            out.write("--------------------------------\n")
