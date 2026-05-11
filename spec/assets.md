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

- `format` MUST derive its main formatting scope from `deps.edn`.
- `format` MUST format files under every path listed in the top-level `:paths`
  vector in `deps.edn`.
- `format` MUST also format files under paths listed in alias-specific
  `:extra-paths` vectors in `deps.edn`.
- `format` SHOULD also format top-level `.clj`, `.cljs`, `.cljc`, and `.edn`
  files in the repo root.
- `format` MUST enable aligned map bindings and aligned `let` bindings.
- `format` MUST be safe to run repeatedly without introducing further diffs once
  the repo is formatted.
- `format` MUST exit nonzero if formatting fails.

## Non-goals

- `format` does not need to format markdown specs.
- `setup` does not need to generate `cljfmt` config for the first pass.

## Notes

- Starter apps may include their own checked-in `cljfmt` config as part of the
  app template rather than having `setup` generate it.
