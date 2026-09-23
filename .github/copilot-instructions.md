# Copilot instructions for this repo

The standing rules for working on this codebase live in `CLAUDE.md` at the
repo root (written for Claude Code, but they apply the same to any agent).
Read it first.

**Search `wiki/text/`, not `wiki/*.html`, when looking something up.**
`wiki/text/` is a baked, markup-stripped mirror of the wiki - one file per page
or per tab (gameplay/coding/science), plus `wiki/text/index.txt` mapping every
heading to a `file#anchor`. It is cheaper to grep and read than the HTML.
Open the `.html` itself only when you are about to edit that page, or when
looking up `session-log.html`, `releases.html` or `making-a-gene.html`, which
are excluded from the bake (see `wiki/tools/bake-agent-text.mjs` for why).

If you edit any wiki page's prose, re-run `node wiki/tools/bake-agent-text.mjs`
so `wiki/text/` doesn't go stale - `--check` reports staleness without writing.
