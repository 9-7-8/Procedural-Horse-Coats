intake/ - the queue of NEW FEATURES waiting to be built

Each *implementation treatment.txt here is one feature (a big one may keep its pieces in a
folder named in that file, e.g. four-jar-split/), scoped with the owner and ready
for a building session. Nothing in this folder is a source of truth once it has been
built: the wiki is.

- To add one:   procedures/write-intake-document.txt   (/write-intake-document)
- To build one: procedures/process-intake.txt          (/process-intake)
- A BUG never goes here. It is a GitHub issue: procedures/report-bug.txt (/report-bug),
  fixed by procedures/fix-bug.txt (/fix-bug).
- discord-*/ folders hold announcement posts and their images for the Discord bot; they are
  not treatments or units.
- To do.txt is the owner's free-form inbox. Never delete it.
- Sizing.txt is every unit's size, impact and owner calls, dated, kept by process-intake
  so a run re-sizes only what changed. It is an index, not a unit.
- potential-assets/ is art for any session to use; its README.txt says how to log a use.

Every procedure: procedures/INDEX.txt.

WHY INTAKE IS FILES AND NOT GITHUB DISCUSSIONS (owner, 2026-10-05: "not a good idea")
Considered and rejected. Discussions are reachable only through GitHub's GraphQL API. A cloud session's GitHub proxy blocks GraphQL and the GitHub tools it has include nothing
for discussions, so a cloud session could neither read nor write them, and scoping, probing and most queue-reading happen in cloud sessions. Treatments are also long and
multi-file (the four-jar split is nine files), carry diagrams and live next to Sizing.txt and the code; a discussion post has no folders, no real file history and about a 65,000
character limit, and its images cannot be uploaded through the API. Offered and not taken: a GitHub Action mirroring discussions into the repo as read-only Markdown (cloud sessions
could read it; extra machinery that could not be tested from a cloud session), and a hybrid (ideas in Discussions, treatments in files). Discussions stay available to the owner
for community chat and announcements. Do not re-propose moving intake into Discussions unless GitHub opens a REST route or the cloud proxy allows GraphQL.
