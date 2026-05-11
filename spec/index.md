# biff.tasks spec index

## Current spec set

- `bootstrap.md` — setup-time repo generation and updates
- `assets.md` — CSS compilation and code formatting
- `runtime.md` — local development, test running, and local nREPL
- `ops.md` — deploy and production SSH tasks
- `build.md` — clean and uberjar

## Cross-cutting decisions

- All task config keys MUST use `:biff.tasks/*`.
- The new task surface does not preserve old task names by default.
- SSH-based production tasks SHOULD use a `prod-` prefix, except for `deploy`.
- The default workflow should optimize for explicit, reviewable behavior rather
  than hidden automation.
- Deployment is manual from a trusted shell; there is no default deploy GitHub
  Action.

## Planned task surface

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
- `clean`
- `uberjar`

## Commands currently not planned

- `prod-dev`
- `soft-deploy` as a separate command

## Open questions

- How should `dev` surface evaluation/test failures so that coding agents notice
  them quickly?
- What should the exact flag be for agent-friendly/minified CSS in `dev`?
- Exact config contract for `prod-install`.
