#!/bin/sh
if  [[ "$OSTYPE" == "darwin"* ]]; then
  # on Macos, lwjgl expect to be launched with -XstartOnFirstThread on jvm
  if  [[ "$1" == "GLFW" ]]; then
    java -XstartOnFirstThread -jar -Xmx2048m target/viz-engine-lwjgl-demo-1.0.0-SNAPSHOT.jar "$@"
  else
    java -Dsun.java2d.metal=true -Dsun.java2d.noddraw=true -Dsun.awt.noerasebackground=true -jar -Xmx2048m target/viz-engine-lwjgl-demo-1.0.0-SNAPSHOT.jar "$@"
  fi
else
  java -jar -Xmx2048m target/viz-engine-lwjgl-demo-1.0.0-SNAPSHOT.jar "$@"
fi
