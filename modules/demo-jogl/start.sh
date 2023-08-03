#!/bin/sh

mode=${1:-awt}
mode=$(echo "$mode" | tr '[:upper:]' '[:lower:]')

extra_jvm_opt=""

java -jar $extra_jvm_opt -Xmx2048m target/viz-engine-jogl-demo-1.0.0-SNAPSHOT.jar "$@"
