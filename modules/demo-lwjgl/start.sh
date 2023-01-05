#!/bin/sh

mode=${1:-awt}
mode=$(echo $mode | tr '[:upper:]' '[:lower:]')
echo $mode

if  [[ "$1" == "glfw" ]]; then
  java -XstartOnFirstThread -jar -Xmx2048m target/viz-engine-lwjgl-demo-1.0.0-SNAPSHOT.jar "$@"
else
  java -jar -Xmx2048m target/viz-engine-lwjgl-demo-1.0.0-SNAPSHOT.jar "$@"
fi
