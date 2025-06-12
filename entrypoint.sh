#!/bin/bash

# Exit when any command fails
set -ex

# Copy env specific config.edn in use
yes | cp -f /etc/config/config.edn /srv
 
java -XX:-HeapDumpOnOutOfMemoryError -XX:+UnlockExperimentalVMOptions -XX:MaxRAMPercentage=80.0 -XX:HeapDumpPath=/srv -jar /srv/app.jar
