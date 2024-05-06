#!/usr/bin/env bash

JAVA_HOME="/usr/lib/jvm/java-11-openjdk-amd64"

# Example basic install command that we will expand upon
# java -jar ganymede-2.1.2.20230910.jar -i

GANYMEDE_JAR="ganymede-2.1.2.20230910.jar"

$JAVA_HOME/bin/java \
      -jar $GANYMEDE_JAR \
      -i \
      --id-suffix=gs-dev-lab-1.0 --display-name-suffix="with gs-dev-lab 1.0"

# run jupyter notebook from the same directory where gs-dev-jupyter-lab is located
