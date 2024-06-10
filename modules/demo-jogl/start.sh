#!/bin/sh

extra_jvm_opt="--add-exports java.base/java.lang=ALL-UNNAMED  --add-exports java.desktop/sun.awt=ALL-UNNAMED --add-exports java.desktop/sun.java2d=ALL-UNNAMED"

java -jar $extra_jvm_opt -Xmx2048m target/viz-engine-jogl-demo-1.0.0-SNAPSHOT.jar "$@"
