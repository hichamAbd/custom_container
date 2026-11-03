# Git Guestbook: a web app whose database is git

A static page served by **GitHub Pages**. Everything users type is saved as a
**commit** to `docs/data/entries.json` in this repo, so the data lives in the
public git history.

## How it works

| Action | How |
| --- | --- |
| **Read** | The page calls the GitHub REST API (`GET /contents/...`) to load `entries.json`. Public repo, so no login is needed. |
| **Write, with a token** | The page `PUT`s the updated file through the Contents API. Each save or delete becomes one commit. |
| **Write, without a token** | "Submit via GitHub issue" opens a pre-filled issue. `.github/workflows/guestbook-issue.yml` reads it, commits the entry, then comments on and closes the issue. Visitors only need a GitHub account. |
| **History** | The page lists the recent commits that touched the data file. |

## Setup

1. Merge this into `master`. The issue workflow only runs from the default branch.
2. **Settings → Pages**: Source "Deploy from a branch", branch `master`, folder `/docs`.
3. Open `https://hichamabd.github.io/custom_container/`.
4. Optional: to write directly, create a
   [fine-grained token](https://github.com/settings/personal-access-tokens/new)
   for this repo only, with **Contents: Read and write**, and paste it in the page's Settings.

Change the target repo, branch or file in `docs/config.js`.

## Notes and limits

- Each write is a commit, and committing to the Pages branch triggers a Pages rebuild. That's fine for low traffic. For heavier use, point `config.js` and `DATA_PATH` at another file or branch.
- Unauthenticated API reads are limited to 60 requests per hour per IP.
- Anyone can file an issue, so anyone with a GitHub account can add entries. Deleted entries stay in git history.
- Never put a token in the code. It is kept only in your browser's localStorage.
