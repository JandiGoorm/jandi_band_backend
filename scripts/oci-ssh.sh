#!/usr/bin/env bash
set -euo pipefail
[[ $OCI_HOST =~ ^[a-zA-Z0-9][a-zA-Z0-9.-]*$ && $OCI_USER =~ ^[a-z_][a-z0-9_-]*$ ]]
umask 077
dir=$(mktemp -d)
trap 'rm -rf -- "$dir"' EXIT
printf '%s\n' "$OCI_SSH_KEY" > "$dir/key"
printf '%s\n' "$OCI_KNOWN_HOSTS" > "$dir/hosts"
ssh -F /dev/null -i "$dir/key" -o IdentitiesOnly=yes -o BatchMode=yes \
    -o StrictHostKeyChecking=yes -o UserKnownHostsFile="$dir/hosts" -o ConnectTimeout=15 \
    "$OCI_USER@$OCI_HOST" "$@"
