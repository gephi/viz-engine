# Gephi visualization engine

![stability-wip](https://img.shields.io/badge/stability-work_in_progress-lightgrey.svg)

This is a new visualization engine for Gephi based on modern OpenGL techniques.

It aims to be:

* Retro-compatible with old OpenGL versions through feature discovery, falling back to the best supported by the graphics card
* High performance using most modern OpenGL when available, specially due to instancing, manual buffer management, using simple shaders and avoiding memory allocation when possible
* Extensible with plugins (rendering and input)
* LWJGL3 version (GLFW/AWT)
* Nicely interactive with mouse, directional zooming, etc with default input handler
* Only a 2D engine for the moment
* The only gephi-related dependency is graphstore

Currently, in comparison to Gephi 0.9.2 renderer it's lacking:

* Self loops
* Node/edge text labels
