#!/usr/bin/env bash
set -euo pipefail
container=${1:?container required}
repository=${2:?image repository required}
[[ $container =~ ^[a-z0-9-]+$ && $repository =~ ^[a-z0-9./-]+$ ]]
bash "$(dirname "$0")/oci-ssh.sh" "bash -s -- $container $repository" <<'REMOTE'
set -euo pipefail
read -r running id ref < <(docker inspect --type container -f '{{.State.Running}} {{.Image}} {{.Config.Image}}' "$1")
[[ $running == true ]]
repository=$2
if [[ $1 == nginx-gateway && $repository == nginx ]]; then
    repository=${ref%@*}
    repository=${repository%%:*}
    repository=${repository#docker.io/library/}
    [[ $repository == nginx || $repository == ghcr.io/kyj0503/backtest-console ]]
fi
digests=$(docker image inspect -f '{{range .RepoDigests}}{{println .}}{{end}}' "$id" | sed 's|^docker\.io/library/||')
for candidate in "$ref" $digests; do
    candidate=${candidate#docker.io/library/}
    if [[ $1 == nginx-gateway && $ref == *@* && ${candidate#*@} != "${ref#*@}" ]]; then
        continue
    fi
    if [[ ${candidate%@*} == "$repository" && ${candidate#*@} =~ ^sha256:[0-9a-f]{64}$ ]] &&
       grep -Fxq -- "$candidate" <<< "$digests"; then
        printf '%s\n' "$candidate"
        exit 0
    fi
done
echo "Cannot resolve running $1 to a registry digest in $repository" >&2
exit 1
REMOTE
