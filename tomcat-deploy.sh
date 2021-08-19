#!/usr/bin/env bash
set -euxo pipefail

export CATALINA_PID=/tmp/catalina.pid

catalina start || true  # if tomcat was already running ignore the error

mvn clean package -Dmaven.test.skip=true

curl --upload-file ./target/*.war "http://root:root@localhost:8080/manager/text/deploy?path=/tvr&update=true"
