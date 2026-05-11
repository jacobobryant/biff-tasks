# Runtime spec

## Commands covered

- `dev`
- `test`
- `nrepl`

## Dev

### Purpose

Start the app locally and provide development-time automation that used to live
in app-specific watcher code.

### Behavioral contract

- `dev` MUST start the application locally for interactive development.
- `dev` MUST ensure required generated files and dependencies are in place
  before app startup.
- `dev` MUST start a local nREPL server and write the port to `.nrepl-port`.
- `dev` MUST watch for changed Clojure source files and evaluate them.
- `dev` MUST run tests after relevant file changes.
- `dev` MUST keep CSS up to date during development.
- `dev` MUST NOT require app repos to carry their own dev-only watcher logic for
  file evaluation and test running.
- `dev` MUST NOT regenerate static HTML as part of the default Biff 2.0 flow.

### Human vs. agent use

- The default `dev` mode SHOULD optimize for fast local human feedback.
- `dev` SHOULD support an explicit flag for agent/service mode so CSS can be
  built minified when desired.
- Evaluation failures and test failures MUST be surfaced loudly in the terminal.
- A machine-readable failure signal for coding agents is desirable but is not
  yet specified.

## Test

### Purpose

Run tests with Cognitect test runner.

### Behavioral contract

- `test` MUST run the project's tests from the `test/` path with Cognitect test
  runner.
- `test` MUST exit nonzero when any test fails or errors.
- `test` SHOULD remain simple and explicit; task-specific watch behavior belongs
  in `dev`, not in `test`.

## nREPL

### Purpose

Start a local nREPL server without booting the app.

### Behavioral contract

- `nrepl` MUST start an nREPL server locally.
- `nrepl` MUST NOT start the app.
- `nrepl` MUST write the selected port to `.nrepl-port`.

## Open questions

- Exact flag name for agent/service CSS mode in `dev`.
- Whether test watching should include only changed namespaces or always run the
  full suite.
- Whether `dev` should expose a machine-readable error state for coding agents.
