---
name: crawl-the-menus
description: Open every mod screen in a throwaway dev client and press every control at its screen coordinates, checking each press did what it is for (-PmenuCrawl, client/crawl/); one [crawl] PASS/FAIL line per check, every FAIL fixed or filed. Use when the owner says "crawl the menus", "test the menus", "click every button", "test the screens".
---

Read `procedures/crawl-the-menus.txt` in full, then follow it exactly.

That file is the only copy of this routine. This wrapper exists only to register the
`/crawl-the-menus` command, so it holds no steps: if the two ever seem to disagree, the
procedure file wins. Apply any arguments the owner gave with the command to the
procedure (for example, an issue number for fix-bug).
