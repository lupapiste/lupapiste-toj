#!/bin/bash

set -eu

# Let Karma know that we are running in CI server
export CI_BUILD=1

lein npm install

echo "Compiling for tests, running tests"
lein clean
lein cljsbuild once prod
lein cljsbuild test ci
lein test2junit

echo "Making uberjar"
lein uberjar
