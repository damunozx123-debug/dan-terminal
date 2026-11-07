# Public GitHub handoff

Prepared for `damunozx123-debug/dan-terminal`, public visibility, MIT licence.
Publication has not happened in this environment: the GitHub connector lacks
repository creation, and the cloud browser currently requires sign-in.
There is no remote, PR, protected branch or published release to verify yet.

The review commit is 671d633. The baseline commit is af88abb.
The source archive and git bundle include the full working branch and history.
No tag has been created. A human approval is required before any merge.

## Publish the complete local repository with GitHub CLI

Run from the prepared repository with GitHub CLI authenticated as the intended
owner. These commands are provided for execution; they have NOT RUN here.

```sh
gh repo create damunozx123-debug/dan-terminal --public --source=. --remote=origin --description 'Android terminal and device integration: unverified M0/M1 source baseline'
git push -u origin main develop fix/shizuku-permission-flow
gh pr create --repo damunozx123-debug/dan-terminal --base develop --head fix/shizuku-permission-flow --title 'M0/M1 baseline and Shizuku permission-flow review' --body-file docs/PR_SHIZUKU_REVIEW.md --draft
```

If the repository already exists, do not rerun creation or replace its contents:
inspect its branches and history, then add its verified remote URL.

## Apply the format-patch bundle instead

Use a checkout of this repository's `develop` base (47dea687b23e8c97923bd291c9713db3a227aa48).
The patch contains the baseline import, permission review and publication prep.
Do not apply it to an unrelated source tree without reviewing conflicts.

```sh
git checkout -b fix/shizuku-permission-flow develop
git am /absolute/path/dan-terminal-reviewed.patch
git push -u origin fix/shizuku-permission-flow
```

The git bundle is preferable when starting from an empty repository:

```sh
git clone /absolute/path/dan-terminal-reviewed.bundle dan-terminal
cd dan-terminal
git remote rename origin source-bundle
git checkout fix/shizuku-permission-flow
```

Then create/add the real GitHub remote and publish as above.

## Repository settings after creation

Require pull requests and at least one human approval for `develop` and `main`.
Disable force pushes and branch deletion for both. Select required build/test
checks only after their real workflow names/results exist. No branch rule is
claimed to have been configured by this document.
