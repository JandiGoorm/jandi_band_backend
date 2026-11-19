#!/usr/bin/env bash
set -euo pipefail
export TEST_IMAGE=${1:?local runtime image required}
temp=$(mktemp -d)
export COMPOSE_PROJECT_NAME=spring-diag-$(basename "$temp" | tr -cd '[:alnum:]' | tr '[:upper:]' '[:lower:]')
export TEST_CONTAINER="$COMPOSE_PROJECT_NAME-app"
compose=(docker compose -f "$temp/compose.yaml")
cleanup() {
    "${compose[@]}" down --timeout 1 >/dev/null
    rm -rf "$temp"
}
export MODE=failed
trap cleanup EXIT
cat >"$temp/compose.yaml" <<'YAML'
services:
  peer:
    image: ${TEST_IMAGE:?}
    container_name: ${TEST_CONTAINER:?}-peer
    entrypoint: [sleep, "120"]
  app:
    image: ${TEST_IMAGE:?}
    container_name: ${TEST_CONTAINER:?}
    entrypoint: [sh, -c, 'echo "${MODE:?}-startup"; exec sleep 60']
    healthcheck:
      test: [CMD, sh, -c, 'test "${MODE:?}" = restored']
      interval: 2s
      start_interval: 1s
      start_period: 1s
      timeout: 1s
      retries: 4
YAML
"${compose[@]}" up -d --pull never peer
run() {
    python3 scripts/record_startup.py "$temp/diagnostics/release.test0001/$1" "$TEST_CONTAINER" "$TEST_IMAGE" \
        "${compose[@]}" up -d --pull never --wait --wait-timeout 30 app
}
if run candidate; then exit 1; fi
export MODE=restored
run rollback
python3 - "$temp/diagnostics/release.test0001" <<'PY'
import json
import os
from pathlib import Path
import subprocess
import sys

root = Path(sys.argv[1])
ids = []
for role, marker, success in [("candidate", "failed-startup", False), ("rollback", "restored-startup", True)]:
    folder = root / role
    meta = json.loads((folder / "container.json").read_text())
    ids.append(meta["id"])
    assert (json.loads((folder / "result.json").read_text())["compose_exit"] == 0) == success
    assert marker in (folder / "container.log").read_text()
    rows = [json.loads(line) for line in (folder / "samples.jsonl").read_text().splitlines()]
    assert all(row["id"] == ids[-1] for row in rows)
    assert any(row.get("cpu_usage_us", 0) > 0 and row["memory_bytes"] > 0 and row["swap_bytes"] >= 0 for row in rows)
    peers = [json.loads(line) for line in (folder / "peers.jsonl").read_text().splitlines()]
    assert any(p["name"] == os.environ["TEST_CONTAINER"] + "-peer" and p["memory_bytes"] > 0 for p in peers)
assert ids[0] != ids[1]
assert subprocess.run(["docker", "container", "inspect", ids[0]], capture_output=True).returncode != 0
print("Failed container removed; its logs and cgroup samples are readable alongside the restored container's records.")
PY
