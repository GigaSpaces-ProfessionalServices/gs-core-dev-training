#!/usr/bin/env bash

SCRIPTS_DIR="`dirname \"$0\"`"
SCRIPTS_DIR="`( cd \"$SCRIPTS_DIR\" && pwd )`"

cd $SCRIPTS_DIR/../rest-application

mvn clean spring-boot:run -Dspring-boot.run.profiles=localhost

