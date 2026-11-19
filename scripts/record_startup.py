"""Run Compose and retain startup evidence on the Docker host before rollback."""

from datetime import datetime, timezone
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import time


def utc():
    return datetime.now(timezone.utc).isoformat()


def write_json(path, data):
    temporary = path.with_suffix(".tmp")
    temporary.write_text(json.dumps(data) + "\n")
    temporary.replace(path)


def inspect(name):
    fmt = ('{{json .Id}}\t{{json .Config.Image}}\t'
           '{{json (index .Config.Labels "org.opencontainers.image.revision")}}\t'
           '{{json .State}}\t{{json .RestartCount}}')
    result = subprocess.run(["docker", "inspect", "--format", fmt, name],
                            capture_output=True, text=True, timeout=5)
    if result.returncode:
        return None
    cid, image, revision, state, restarts = map(json.loads, result.stdout.strip().split("\t"))
    return dict(id=cid, image=image, revision=revision, state=state, restarts=restarts)


def counters(path):
    return {key: int(value) for key, value in
            (line.split() for line in path.read_text().splitlines())}


def resources(pid, cid):
    membership = Path(f"/proc/{pid}/cgroup").read_text().splitlines()
    relative = next((line[3:] for line in membership if line.startswith("0::")), "")
    if cid not in relative or ".." in Path(relative).parts:
        raise ValueError("container cgroup v2 unavailable")
    group = Path("/sys/fs/cgroup") / relative.lstrip("/")
    cpu = counters(group / "cpu.stat")
    memory = counters(group / "memory.stat")
    data = {"cpu_usage_us": cpu["usage_usec"], "cpu_throttled_us": cpu.get("throttled_usec"),
            "memory_bytes": int((group / "memory.current").read_text()),
            "swap_bytes": int((group / "memory.swap.current").read_text()),
            "swap_in_pages": memory.get("pswpin"), "swap_out_pages": memory.get("pswpout"),
            "major_faults": memory.get("pgmajfault"), "oom_kill": counters(group / "memory.events")["oom_kill"]}
    for kind in ("cpu", "memory", "io"):
        pressure = group / (kind + ".pressure")
        if pressure.exists():
            line = next(line for line in pressure.read_text().splitlines() if line.startswith("some "))
            data[kind + "_stall_us"] = int(line.split("total=")[1])
    return data


def save_logs(folder):
    record = json.loads((folder / "container.json").read_text())
    temporary = folder / "container.log.partial"
    with temporary.open("wb") as output:
        result = subprocess.run(["docker", "logs", "--timestamps", "--since", record["since"],
                                 record["id"]], stdout=output, stderr=subprocess.STDOUT, timeout=20)
    if result.returncode:
        raise OSError("docker logs failed")
    temporary.replace(folder / "container.log")


def sample(folder, container, expected, since, previous, final):
    info = inspect(container)
    if not info or info["image"] != expected or (info["id"] == previous and not final):
        return False
    cid, state = info["id"], info.pop("state")
    info.update(since=since, started_at=state["StartedAt"], page_bytes=os.sysconf("SC_PAGE_SIZE"),
                status=state["Status"], oom_killed=state["OOMKilled"], exit_code=state["ExitCode"],
                health=state.get("Health", {}))
    write_json(folder / "container.json", info)
    row = dict(time=utc(), id=cid, pid=state["Pid"], health=info["health"].get("Status"),
               restarts=info["restarts"], status=state["Status"])
    if state["Pid"]:
        try:
            row.update(resources(state["Pid"], cid))
        except (OSError, ValueError, KeyError) as error:
            row["resource_error"] = str(error)
    with (folder / "samples.jsonl").open("a") as output:
        print(json.dumps(row), file=output)
    return True


def prepare(folder):
    folder.mkdir(mode=0o700, parents=True, exist_ok=True)
    if folder.parent.parent.name != "startup-diagnostics":
        return
    cutoff = time.time() - 14 * 86400
    for release in folder.parent.parent.iterdir():
        if (re.fullmatch(r"release\.[a-zA-Z0-9]{8}", release.name)
                and release != folder.parent and not release.is_symlink()
                and (release / "candidate").is_dir() and release.stat().st_mtime < cutoff):
            shutil.rmtree(release)


def observe(folder, container, expected, command):
    since = utc()
    try:
        prepare(folder)
        old = inspect(container)
    except (OSError, subprocess.SubprocessError, ValueError) as error:
        print(f"Startup diagnostics unavailable: {error}", file=sys.stderr)
        return subprocess.call(command)
    previous = old["id"] if old else None
    process = subprocess.Popen(command)
    while True:
        done = process.poll() is not None
        try:
            found = sample(folder, container, expected, since, previous, done)
            if done:
                if (folder / "container.json").exists():
                    save_logs(folder)
                write_json(folder / "result.json", dict(since=since, finished=utc(),
                           compose_exit=process.returncode, expected_image=expected))
        except (OSError, subprocess.SubprocessError, ValueError, KeyError) as error:
            print(f"Startup diagnostics incomplete: {error}", file=sys.stderr)
            found = False
        if done:
            return process.returncode
        try:
            process.wait(timeout=5 if found else 1)
        except subprocess.TimeoutExpired:
            pass


if __name__ == "__main__":
    os.umask(0o077)
    if sys.argv[1] == "--save-logs":
        save_logs(Path(sys.argv[2]))
    else:
        sys.exit(observe(Path(sys.argv[1]), sys.argv[2], sys.argv[3], sys.argv[4:]))
