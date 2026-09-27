---
description: Observability workflow to poll and tail GitHub Actions cloud release pipeline until completion.
---

<follow-the-build>
The user invoked /follow-the-build to monitor and tail the remote GitHub Actions release workflow after pushing a commit or release tag to `origin`.

## Objective
Monitor the running GitHub Actions workflow for the current repository, trace step progression, report state transitions, and verify published release assets without blocking the developer.

## Workflow Execution Steps

### 1. Fetch the Latest Workflow Run ID
Query the GitHub Actions API to retrieve the most recent run for the `Release Pipeline 📦`:

```bash
curl -s "https://api.github.com/repos/ClawStackStudios/ShellGuard-Mobile/actions/runs?per_page=1"
```
Extract:
- `id` (e.g. `36336248479`)
- `status` (`queued`, `in_progress`, `completed`)
- `conclusion` (`success`, `failure`, `cancelled`, or `null`)
- `html_url`

### 2. Tail Step Progression
While `status` is `in_progress` or `queued`:
1. Inspect the running jobs and individual steps:
   ```bash
   curl -s "https://api.github.com/repos/ClawStackStudios/ShellGuard-Mobile/actions/runs/<RUN_ID>/jobs"
   ```
2. Parse the step list (`name`, `status`, `conclusion`).
3. Summarize active and completed steps for the user:
   - ✅ Completed steps
   - ⏳ In-progress step (e.g., *Build & Sign Release AAB & APK*)
4. If still running:
   - Schedule a 30-second timer using the `schedule` tool:
     `schedule(DurationSeconds=30, Prompt="Check GitHub Actions status for run <RUN_ID>")`
   - Yield control and await the reactive timer notification.

### 3. Verify Completion & Artifact Publication
Once `status == "completed"`:
1. If `conclusion == "success"`:
   - Query the GitHub Releases API to verify the release tag and asset download URLs:
     ```bash
     curl -s "https://api.github.com/repos/ClawStackStudios/ShellGuard-Mobile/releases/tags/<VERSION_TAG>"
     ```
   - Confirm presence of:
     - `shellguard-mobile-v<VERSION>.aab` (Google Play App Bundle)
     - `shellguard-mobile-v<VERSION>.apk` (FOSS Sideload APK)
   - Report final completion with direct clickable links to the user.
2. If `conclusion == "failure"`:
   - Inspect failed job logs using the GitHub API to identify the exact step that failed.
   - Surface the failure logs clearly so the issue can be diagnosed.
</follow-the-build>
