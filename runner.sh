#!/bin/bash
set +x
set -e

mvn clean install -DskipTests=true -Dcheckstyle.skip=true