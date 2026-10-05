---
name: review-server-logs
description: Review the live server's dated PCH log exports that have not been reviewed yet - bugs to GitHub issues, player wants to idea issues, death patterns and operator-script errors to the report. Read only. Use when the owner says "check the server logs", "review the PCH logs", "read the server logs".
---

Read `procedures/review-server-logs.txt` in full, then follow it exactly.

That file is the only copy of this routine. This wrapper exists only to register the
`/review-server-logs` command, so it holds no steps: if the two ever seem to disagree,
the procedure file wins. The paths and the record of reviewed folders are in the local
memory `pch-server-logs`.
