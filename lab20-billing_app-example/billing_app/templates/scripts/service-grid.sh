#!/usr/bin/env bash

SCRIPTS_DIR="`dirname \"$0\"`"
SCRIPTS_DIR="`( cd \"$SCRIPTS_DIR\" && pwd )`"

source $SCRIPTS_DIR/settings.sh

$GS_HOME/bin/gs.sh host run-agent --auto --gsc=1

