intake/ - what is left of the feature queue, plus its tools and art

THE FEATURE QUEUE IS NOW GITHUB ISSUES (migrate-intake-to-issues, in progress).
- A wanted feature is an issue labelled enhancement: `idea` until write-intake-document
  scopes it, then `scoped`. Size is size:S|M|L, impact is impact:finishes|unblocks|new,
  the owner's calls are needs-owner, a unit being built is in-progress, a big treatment
  is a `tracking` parent with its pieces as sub-issues, evidence is `survey`.
- To add one:   procedures/write-intake-document.txt   (/write-intake-document)
- To build one: procedures/process-intake.txt          (/process-intake)
- A BUG never goes in the feature queue: procedures/report-bug.txt, procedures/fix-bug.txt.
- To do.txt is the owner's old inbox, now a redirect to the issue list. Never delete it.

STILL FILES HERE (not yet migrated; the migration moves them, then deletes them)
- *implementation treatment.txt and a few evidence files: the units not yet issues.
  process-intake and whats-next still read them (their lines marked FILE).
- Sizing.txt: the size, impact and owner calls of those files, dated, kept by
  process-intake. An index, not a unit. Goes with the last file.

STAYS HERE
- tools/: scripts add-a-gene calls (founders.py, fit-svg.mjs, coverage.mjs). Their paths
  are cited in gene files, so the folder keeps its name.
- potential-assets/: art for any session to use; its README.txt says how to log a use.
- discord-*/ folders hold announcement posts and their images for the Discord bot; they
  are not treatments or units.
- *-draft/ folders retire with the treatment that uses them.

Every procedure: procedures/INDEX.txt.

WHY THE QUEUE IS ISSUES AND NOT GITHUB DISCUSSIONS (owner, 2026-10-05: "not a good idea")
Discussions were considered and rejected. They are reachable only through GitHub's GraphQL
API. A cloud session's GitHub proxy blocks GraphQL and the GitHub tools it has include
nothing for discussions, so a cloud session could neither read nor write them, and scoping,
probing and most queue-reading happen in cloud sessions. Issues work over REST and the
issue and sub-issue tools. Discussions stay available to the owner for community chat and
announcements. Do not re-propose Discussions unless GitHub opens a REST route or the cloud
proxy allows GraphQL.
