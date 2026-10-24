# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.0.0] - 2026-10-23

### Added

- Entity tick culling (off by default): skips the client-side update of entities that are currently culled or outside
  the field of view. Nearby, glowing and ridden entities and anything on the skip lists keep updating.
- Vegetation thinning (Sodium only): hides half of the grass, flowers and small plants in chunks far from the camera,
  with a configurable start distance.
- Debug options: show only culled objects, and show the depth and culling textures.
- The debug key now cycles off / counters / counters with timings.
- Tooltips for every config option.

### Changed

- Ported the latest improvements from the 1.20.1 version.
- Culling results are read back from the GPU, so reading them no longer stalls the render thread.
- The chunk culling map is rebuilt when the render distance or world height changes.
- Entity culling now samples the side of an entity facing the camera, culling fewer entities by mistake near walls.
- The Warden is no longer culled by default.
- Lower memory use and fewer allocations in the culling maps.

### Fixed

- Chunks flickering out of view when sprinting around corners.
- Thin gaps in walls being missed by the depth downsampling, culling chunks that could be seen through them.
- Everything counting as culled until the first culling results arrive.
- Culling freezing while the debug profiler (F3 pie chart) was open.
- Entities behind the camera disappearing for a moment when turning around quickly with Sodium.
- Changes to the skip lists only taking effect after a restart.
- Culling breaking after resizing the window or changing render scale with Iris shaders enabled.

## [1.0.0] - 2026-08-23

- Initial release.