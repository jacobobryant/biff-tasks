# Assets and formatting spec

## Commands covered

- `css`
- `format`

## CSS

### Purpose

Compile the app stylesheet from `resources/tailwind.css` into the configured
 output path.

### Behavioral contract

- `css` MUST compile CSS to the configured output path.
- `css` MUST continue to support the existing Tailwind installation modes:
  local binary, global binary, npm, and bun.
- `css` MUST support `:biff.tasks/tailwind-version`.
- If the local-bin Tailwind binary is selected and the installed binary version
  does not match `:biff.tasks/tailwind-version`, `css` MUST re-download the
  pinned version.
- If npm, bun, or a global binary is selected, `css` MUST ignore
  `:biff.tasks/tailwind-version`.
- `css` SHOULD continue to pass through watch/minify-style Tailwind flags rather
  than inventing a second flag layer unless a Biff-specific flag adds real
  value.

## Format

### Purpose

Apply the default Biff 2.0 formatting rules with `cljfmt`.

### Behavioral contract

- `format` MUST format the repo's Clojure source and test files.
- `format` SHOULD format common Clojure-adjacent config files that `cljfmt`
  supports.
- `format` MUST enable aligned map bindings and aligned `let` bindings.
- `format` MUST be safe to run repeatedly without introducing further diffs once
  the repo is formatted.
- `format` MUST exit nonzero if formatting fails.

## Non-goals

- `format` does not need to format markdown specs.

## Open questions

- Exact default file globs for `format`.
- Whether `setup` should also write a default `cljfmt.edn` or equivalent config
  file when missing.
