#!/bin/sh
set -a
. ./.env
set +a

exec ./gradlew "${@:-bootRun}"