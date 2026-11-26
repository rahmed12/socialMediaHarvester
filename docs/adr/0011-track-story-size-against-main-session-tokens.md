# Track story size against main-session tokens

We don't yet know how big a story can be before its main session leaves the ~150k-token smart zone. So when a story is about to be closed, we add one row to the table below: how big the story was and how many tokens its main session used. Subagent tokens are not counted. Once every first-release ticket is closed, we analyse the table and turn what it shows into a sizing rule for the `to-tickets` skill.

- **Peak context**: the most tokens in the main session's context window at any point. This is the number to compare with the 150k limit.
- **Story points**: the agile effort estimate on the Fibonacci scale (1, 2, 3, 5, 8, 13), set when the ticket is written, before work starts. Anything above 13 should be split.
- **Size**: acceptance criteria, then files and lines changed against `main` (lockfiles excluded).
- **Notes**: work that wasn't the story itself (outages, design detours, rework), so a large number isn't blamed on the story alone.

| Story | Closed | Story points | Acceptance criteria | Files / lines changed | Main-session peak context | Notes |
|---|---|---|---|---|---|---|
| #28 Walking skeleton | 2026-11-25 | 8 (estimated after the fact) | 9 | 60 files, +1817 / −21 | ~145k | Peak includes unplanned work: a PostgreSQL port mix-up, restructuring to package-by-layer, writing CODING_STANDARDS.md, adding then removing ArchUnit, and a BlockHound flake. The ticket alone would likely have used much less. |
