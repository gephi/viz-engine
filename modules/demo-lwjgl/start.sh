#!/usr/bin/env bash

mode=${1:-awt}
mode=$(echo "$mode" | tr '[:upper:]' '[:lower:]')

extra_jvm_opt=""
if  [[ "$OSTYPE" == "darwin"* ]]; then
  if  [[ "$1" == "glfw" ]]; then
    extra_jvm_opt="-XstartOnFirstThread"
  fi
fi

java -jar $extra_jvm_opt -Xmx2048m target/viz-engine-lwjgl-demo-1.0.0-SNAPSHOT.jar "$@"
