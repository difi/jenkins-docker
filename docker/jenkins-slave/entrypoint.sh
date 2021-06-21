#!/usr/bin/env bash

curl -fsS --retry 100 --retry-delay 5 http://jenkins:8080/jnlpJars/agent.jar -o /tmp/agent.jar || exit 1
mkdir ~/.ssh ||
groovy /files/create-ssh-known-hosts /config.yaml || exit 1
exec java -jar /tmp/agent.jar -jnlpUrl http://jenkins:8080/computer/${SLAVE_NAME#*_}/slave-agent.jnlp -workDir "/tmp"