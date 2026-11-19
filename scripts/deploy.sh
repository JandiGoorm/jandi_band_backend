#!/usr/bin/env bash
set -Eeuo pipefail
umask 077

app=${1:?}
environment=${2:?}
image=${3:?}
revision=${4:?}
registry_user=${5:?}
case "$app" in jandi-band|jandi-band-py) ;; *) exit 2 ;; esac
[[ "$environment" == production ]] || exit 2
[[ "$image" =~ ^ghcr\.io/jandigoorm/$app@sha256:[a-f0-9]{64}$ ]] || exit 2
[[ "$revision" =~ ^[a-f0-9]{40}$ ]] || exit 2

root="/opt/$app/$environment"
release=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd -P)
[[ "$release" == "$root"/releases/release.* ]] || exit 2
container="$app"
profile=prod
project="$app-$environment"
exec 9>"$root/deploy.lock"
flock -w 300 9
previous=$(readlink -f "$root/current" || true)
legacy=""
changed=false
wait_timeout=180
startup_override=()
if [[ "$app" == jandi-band ]]; then
    wait_timeout=300
    # 이전 릴리스를 복구할 때도 현재 기동 유예 시간을 적용한다.
    startup_override=(-f "$release/compose.override.yaml")
fi

compose() {
    local directory=$1 role=$2
    shift 2
    local -a observer=()
    if [[ "$app" == jandi-band && -n "$role" ]]; then
        local expected
        expected=$(sed -n 's/^APP_IMAGE=//p' "$directory/deployment.env")
        observer=(python3 "$release/scripts/record_startup.py" "$diagnostics/$role" "$container" "$expected")
    fi
    "${observer[@]}" docker compose --env-file "$directory/deployment.env" -f "$directory/compose.yaml" "${startup_override[@]}" "$@"
}
up() {
    compose "$1" "$2" up -d --no-deps --pull never --wait --wait-timeout "$wait_timeout" app
}
reload_gateway() {
    docker exec nginx-gateway nginx -t
    docker exec nginx-gateway nginx -s reload
}
cleanup() {
    rm -f "$release/.registry-token"
    rm -rf "$release/.docker"
}
failed() {
    local code=$?
    trap - ERR
    echo "Deployment failed (exit $code)." >&2
    if [[ "$changed" == true ]]; then
        if [[ "$app" == jandi-band && -f "$diagnostics/candidate/container.json" ]]; then
            python3 "$release/scripts/record_startup.py" --save-logs "$diagnostics/candidate" || echo "Could not refresh candidate logs" >&2
        fi
        if [[ -n "$legacy" ]] && docker container inspect "$legacy" >/dev/null 2>&1; then
            compose "$release" "" rm -s -f app
            docker rename "$legacy" "$container"
            docker update --restart unless-stopped "$container" >/dev/null
            docker start "$container" >/dev/null
        elif [[ -n "$legacy" ]]; then
            docker start "$container" >/dev/null
        elif [[ -n "$previous" && -f "$previous/compose.yaml" ]]; then
            up "$previous" rollback
        else
            compose "$release" "" rm -s -f app
        fi
        reload_gateway
        echo "Previous deployment restored; this run remains failed." >&2
    fi
    exit "$code"
}
trap cleanup EXIT
trap failed ERR

if [[ "$app" == jandi-band ]]; then
    diagnostics="$root/startup-diagnostics/$(basename "$release")"
    mkdir -p -m 700 "$diagnostics"
    echo "Spring startup diagnostics: $diagnostics"
    test -s "$root/.env"
    test -s "$root/tls/mysql-truststore.p12"
    chmod 755 "$release/config"
    chmod 644 "$release"/config/*.properties
fi
docker network inspect backend-net >/dev/null
mkdir -m 700 "$release/.docker"
export DOCKER_CONFIG="$release/.docker"
docker login ghcr.io -u "$registry_user" --password-stdin < "$release/.registry-token"
rm -f "$release/.registry-token"
docker pull "$image"
test "$(docker image inspect "$image" --format '{{.Architecture}}')" = amd64
test "$(docker image inspect "$image" --format '{{index .Config.Labels "org.opencontainers.image.revision"}}')" = "$revision"
printf 'APP_ENV=%s\nAPP_CONTAINER=%s\nAPP_PROFILE=%s\nAPP_IMAGE=%s\nAPP_ENV_FILE=%s/.env\n' \
    "$environment" "$container" "$profile" "$image" "$root" > "$release/deployment.env"
compose "$release" "" config --quiet

if docker container inspect "$container" >/dev/null 2>&1; then
    owner=$(docker inspect "$container" --format '{{index .Config.Labels "com.docker.compose.project"}}')
    if [[ "$owner" != "$project" ]]; then
        legacy="$container-pre-actions-$(date +%s)"
        changed=true
        docker stop "$container" >/dev/null
        docker rename "$container" "$legacy"
        docker update --restart no "$legacy" >/dev/null
    elif [[ ! -f "$previous/compose.yaml" ]]; then
        echo "Current release metadata missing; refusing an untracked replacement." >&2
        exit 1
    fi
fi
changed=true
up "$release" candidate
test "$(docker inspect "$container" --format '{{.Config.Image}}')" = "$image"
if [[ "$app" == jandi-band-py ]]; then
    status=$(docker exec "$container" curl -sS -o /dev/null -w '%{http_code}' 'http://localhost:8000/timetable?url=https://example.com')
    test "$status" = 400
fi
reload_gateway
ln -sfn "$release" "$root/current"
if [[ -n "$legacy" ]]; then
    printf '%s\n' "$legacy" > "$release/previous-container"
fi
echo "Deployed $container at $image (revision $revision)."
