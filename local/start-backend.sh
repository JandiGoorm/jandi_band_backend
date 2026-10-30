#!/bin/sh
set -eu

set -- java -Xmx512m \
  -Dspring.config.location=file:/app/config/application.properties \
  -Dspring.jpa.hibernate.ddl-auto=validate \
  -Dspring.sql.init.mode=never \
  -Dinvite.club.link.prefix=http://localhost:19090/invite/club/accept \
  -Dinvite.team.link.prefix=http://localhost:19090/invite/team/accept \
  "-Dkakao.redirect-url=${KAKAO_REDIRECT_URL:-http://localhost:19090/callback}"

case "${LOCAL_AUTH_MODE:-mock}" in
  mock)
    python3 /app/local-oauth.py &
    oauth_pid=$!
    "$@" \
      -Dkakao.token-url=http://127.0.0.1:9092/oauth/token \
      -Dkakao.user-info-url=http://127.0.0.1:9092/v2/user/me \
      -Dkakao.user-unlink-url=http://127.0.0.1:9092/v1/user/unlink \
      -jar /app/app.jar &
    app_pid=$!
    trap 'kill "$app_pid" "$oauth_pid" 2>/dev/null || true' EXIT
    trap 'exit 0' TERM INT
    wait "$app_pid"
    ;;
  kakao)
    if [ -z "${KAKAO_REST_API_KEY:-}" ]; then
      echo 'kakao mode requires KAKAO_REST_API_KEY' >&2
      exit 2
    fi
    exec "$@" -jar /app/app.jar
    ;;
  *)
    echo 'LOCAL_AUTH_MODE must be mock or kakao' >&2
    exit 2
    ;;
esac
