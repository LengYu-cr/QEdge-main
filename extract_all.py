import re, json, sys

src = sys.argv[1] if len(sys.argv) > 1 else ""
data = open(src, encoding="utf-8", errors="replace").read()

m = re.search(r'"text":"((?:\\.|[^"\\])*)"', data, re.S)
if not m:
    print("NO MATCH")
    raise SystemExit

body = m.group(1).encode("utf-8").decode("unicode_escape", errors="ignore")
d = json.loads(body)

print("=== FIELDS ===")
for x in d["data"].get("fields", []):
    print(x["name"], ":", x["sig"])

print("=== METHODS ===")
for x in d["data"].get("methods", []):
    print(x["name"])