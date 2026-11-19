"""Change the image of this host's existing gateway; keep its mounts and TLS configuration."""
import argparse
from contextlib import ExitStack
import fcntl
import json
import os
from pathlib import Path
import re
import signal
import subprocess
import tempfile
import time

APPS = ["jandi-band"]
CERTIFICATE = "ssl/origin/cloudflare-origin-ca.pem"
HEALTH_URLS = ["https://rhythmeet-be.yeonjae.kr/actuator/health"]
IMAGE = re.compile(r"nginx:(\d+\.\d+\.\d+-alpine)@sha256:[0-9a-f]{64}")
FIELD = re.compile(rb"(?m)^(\s*image:[ \t]+)nginx(?::[\w.-]+)?(?:@sha256:[0-9a-f]{64})?([ \t]*)(?=\r?$)")

def run(*args):
    return subprocess.check_output(args, text=True, timeout=120).strip()

def probe(urls, ca):
    from urllib.parse import urlsplit
    for url in urls:
        origin = urlsplit(url)
        host = origin.hostname
        status = run("curl", "--noproxy", "*", "--cacert", str(ca), "--resolve", f"{host}:{origin.port or 443}:127.0.0.1",
                     "--silent", "--show-error", "--output", "/dev/null", "--write-out", "%{http_code}",
                     "--connect-timeout", "3", "--max-time", "10", url)
        if status != "200":
            raise RuntimeError(f"{host} returned HTTP {status}")

def change(image, directory, locks, ca, urls):
    if not IMAGE.fullmatch(image):
        raise ValueError("Expected an immutable official nginx Alpine image")
    base = directory / "compose.gateway.yaml"
    with ExitStack() as stack:
        for path in [directory / ".image-update.lock", *locks]:
            handle = stack.enter_context(path.open("a"))
            fcntl.flock(handle, fcntl.LOCK_EX)
        original = base.read_bytes()
        new, count = FIELD.subn(lambda m: m[1] + image.encode() + m[2], original)
        if count != 1:
            raise ValueError("Expected one nginx image directive")
        def compose(*args):
            return run("docker", "compose", "--project-directory", str(directory), "-f", str(base), *args)
        model = json.loads(compose("config", "--format", "json"))
        if set(model["services"]) != {"nginx"}:
            raise ValueError("Unexpected gateway services")
        name = model["services"]["nginx"]["container_name"]
        info = json.loads(run("docker", "container", "inspect", name))[0]
        if not info["State"]["Running"] or info["Config"]["Labels"]["com.docker.compose.project"] != model["name"]:
            raise ValueError("Unexpected running gateway")
        old_id = info["Image"]
        prior = next(ref for ref in json.loads(run("docker","image","inspect",old_id))[0]["RepoDigests"]
                     if re.fullmatch(r"(?:docker.io/library/)?nginx@sha256:[0-9a-f]{64}",ref))
        probe(urls, ca)
        run("docker", "pull", image)
        def temp_file(content, mode):
            f = stack.enter_context(tempfile.NamedTemporaryFile(dir=directory, delete=False))
            path = Path(f.name)
            stack.callback(path.unlink, missing_ok=True)
            f.write(content)
            f.flush()
            os.fsync(f.fileno())
            os.chmod(path, mode)
            return path
        candidate = temp_file(new, base.stat().st_mode & 0o777)
        previous = temp_file(FIELD.sub(lambda m: m[1] + prior.encode() + m[2], original), base.stat().st_mode & 0o777)
        def use(file, *args):
            return run("docker", "compose", "-p", model["name"], "--project-directory", str(directory),
                       "-f", str(file), *args)
        use(candidate, "run", "--rm", "--no-deps", "--pull", "never", "nginx", "nginx", "-t")
        new_id = run("docker","image","inspect","--format","{{.Id}}",image)
        def verify(expected):
            if run("docker","inspect","--format","{{.Image}}",name) != expected:
                raise RuntimeError("Gateway has a different image")
            run("docker","exec",name,"nginx","-t")
            for attempt in range(6):
                try:
                    probe(urls, ca)
                    return
                except (RuntimeError, subprocess.SubprocessError):
                    if attempt == 5:
                        raise
                    time.sleep(2)
        if base.read_bytes() != original:
            raise RuntimeError("Gateway Compose changed during validation")
        if new == original and old_id == new_id:
            verify(new_id)
            return
        try:
            os.replace(candidate, base)
            compose("up","-d","--no-deps","--pull","never","nginx")
            verify(new_id)
        except BaseException:
            os.replace(previous, base)
            compose("up","-d","--no-deps","--pull","never","nginx")
            verify(old_id)
            raise

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("image")
    args = parser.parse_args()
    directory = Path("/opt/gateway")
    def interrupted(signum, frame):
        raise InterruptedError(f"Signal {signum}")
    signal.signal(signal.SIGTERM, interrupted)
    change(args.image, directory, [Path("/opt") / app / "production/deploy.lock" for app in APPS],
           directory / CERTIFICATE, HEALTH_URLS)
