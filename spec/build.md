# Build spec

## Commands covered

- `clean`
- `uberjar`

## clean

- `clean` MUST delete generated build output.
- `clean` SHOULD remain narrow and predictable rather than deleting broad swaths
  of project state.

## uberjar

- `uberjar` MUST remain available as an optional packaging path.
- `uberjar` MUST generate minified CSS before building.
- `uberjar` MUST compile the configured main namespace and package app resources.
- `uberjar` SHOULD continue to support skipping the pre-build clean step via an
  explicit flag.

## Non-goals

- Uberjar/Docker workflows are not the primary Biff 2.0 deployment path.
- These specs do not require additional Docker-specific tasks.
