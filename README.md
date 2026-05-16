# Biff tasks

A collection of default [cljrun](https://github.com/biffweb/cljrun) tasks for Biff projects. If you
need to customize any tasks further than what you can do with the supported options, it's fine to
copy them into your own project.

## Task surface

The current public task surface is:

- `setup`
- `css`
- `format`
- `dev`
- `test`
- `nrepl`
- `deploy`
- `prod-install`
- `prod-restart`
- `prod-nrepl`
- `prod-logs`
- `uberjar`

Behavioral details for these tasks live under `spec/`.
