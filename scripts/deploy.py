"""Upload deployment files; keep runtime secrets in the server's .env."""

import io
import os
from pathlib import Path
import re
import shlex
import subprocess
import tarfile
import tempfile


APP = "jandi-band"


def main():
    branch = os.environ["GITHUB_REF_NAME"]
    if branch not in {"main", "dev"}:
        raise ValueError("Only main/dev may deploy")
    environment = "production" if branch == "main" else "development"
    image = os.environ["APP_IMAGE"]
    if not re.fullmatch(rf"ghcr.io/jandigoorm/{APP}@sha256:[0-9a-f]{{64}}", image):
        raise ValueError("Expected an immutable application image")
    revision = os.environ["GITHUB_SHA"]
    if not re.fullmatch(r"[0-9a-f]{40}", revision):
        raise ValueError("Invalid revision")
    host, user = os.environ["OCI_HOST"], os.environ["OCI_USER"]
    if not re.fullmatch(r"[a-zA-Z0-9][a-zA-Z0-9.-]*", host):
        raise ValueError("Invalid SSH host")
    if not re.fullmatch(r"[a-z_][a-z0-9_-]*", user):
        raise ValueError("Invalid SSH user")
    root = f"/opt/{APP}/{environment}"
    registry_user = os.environ["GHCR_USER"]
    if not re.fullmatch(r"[A-Za-z0-9_-]+(?:\[bot\])?", registry_user):
        raise ValueError("Invalid registry user")

    payload = io.BytesIO()
    with tarfile.open(fileobj=payload, mode="w:gz") as archive:
        files = [Path("compose.yaml"), Path("compose.redis.yaml"), Path("scripts/deploy.sh"),
                 Path("scripts/prepare-redis.py"), Path("config/redis.conf")]
        files.extend(sorted(Path("config").glob("*.properties")))
        for path in files:
            if path.is_symlink():
                raise ValueError("Deployment files must not be symlinks")
            archive.add(path, arcname=path.as_posix(), recursive=False)
        token = os.environ["GHCR_TOKEN"].encode()
        if not token:
            raise ValueError("Missing registry token")
        entry = tarfile.TarInfo(".registry-token")
        entry.size, entry.mode = len(token), 0o600
        archive.addfile(entry, io.BytesIO(token))

    with tempfile.TemporaryDirectory(prefix="oci-deploy-") as folder:
        key, known_hosts = Path(folder) / "key", Path(folder) / "known_hosts"
        key.write_text(os.environ["OCI_SSH_KEY"].rstrip() + "\n")
        key.chmod(0o600)
        known_hosts.write_text(os.environ["OCI_KNOWN_HOSTS"].rstrip() + "\n")
        if key.stat().st_size < 100 or known_hosts.stat().st_size < 50:
            raise ValueError("Missing SSH key or pinned host key")
        args = " ".join(shlex.quote(value) for value in (
            APP, environment, image, revision, registry_user
        ))
        command = (
            "set -eu; umask 077; "
            f"test -d {shlex.quote(root)}; "
            f"mkdir -p {shlex.quote(root)}/releases; "
            f"release=$(mktemp -d {shlex.quote(root)}/releases/release.XXXXXXXX); "
            'trap \'rm -f "$release/.registry-token"\' EXIT; '
            'tar -xzf - --no-same-owner --no-same-permissions -C "$release"; '
            f'bash "$release/scripts/deploy.sh" {args}'
        )
        subprocess.run([
            "ssh", "-i", str(key), "-o", "IdentitiesOnly=yes",
            "-o", "BatchMode=yes", "-o", "ConnectTimeout=20",
            "-o", "StrictHostKeyChecking=yes",
            "-o", f"UserKnownHostsFile={known_hosts}", f"{user}@{host}", command,
        ], input=payload.getvalue(), check=True, timeout=720)


if __name__ == "__main__":
    main()
