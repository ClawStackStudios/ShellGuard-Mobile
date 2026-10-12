---
description: A workflow defining the strict initialization protocol for beginning any new development task.
---

# 🚀 Task Initialization Workflow

> **Use When:** Commencing ANY new feature, bugfix, or refactor.
> **Pair With:** `.agents/rules/git-hygiene.md` and `.agents/rules/brain.md`.

---

## 🛑 Step 1: Pre-Flight Context Alignment
Before writing a single line of code, you MUST establish context:
1. Read `.agents/brain/activeContext.md` to understand the current work focus and recent changes.
2. Read `.agents/brain/project/systemPatterns.md` or related architectural docs (`attractorBeacon.md`) if the task touches core state or logic.
3. Check `ROADMAP.md` to confirm the chronological execution sequence. If you are asked to implement a phase out of sequence (e.g., jumping ahead or filling in a skipped phase), you MUST pause and formulate a plan for how to handle versioning without breaking the subsequent roadmap milestones.

## 🌿 Step 2: Git Hygiene Check & Branch Creation
You must NEVER work directly on `main`.
1. Run `git status` to ensure the working tree is clean. If dirty with uncommitted work, stop and coordinate with the user.
2. Run `git pull origin main` to ensure you are up to date.
3. Create an isolated branch based on the task description:
   ```bash
   git checkout -b <type>/<short-descriptive-name>
   ```
   *Valid types: `feat`, `fix`, `refactor`, `docs`, `chore`.*

## 🗺️ Step 3: Tactical Planning
1. **Research:** Use `grep_search` and `view_file` to map the exact files and dependencies involved in the task.
2. **Document:** If the task requires architectural changes, involves extensive logic, or deviates from existing patterns, create or update the implementation plan artifact using a task-scoped timestamped filename (e.g. `implementation_plan_<task>_YYYY_MM_DD.md`):
   > [!IMPORTANT]
   > **🛡️ Artifact Datestamp & Anti-Overwrite Invariant:**
   > - **Title Datestamp**: Heading must be `# Implementation Plan — [Task] - MM/DD/YYYY - H:MM am/pm`.
   > - **Timestamped Filename**: Always write to a discrete task/date-stamped file (e.g. `implementation_plan_<task>_YYYY_MM_DD.md`) rather than overwriting prior plans.
3. **Preview & Approval:**
   > [!IMPORTANT]
   > **🛑 The Preview Mandate:**  
   > Always **"Show me what you're going to add, where you will add it, and to what files please."**  
   > Present the full preview, and await the user's explicit approval before proceeding to Execution.

## ⚙️ Step 4: Execution
- Only once Steps 1-3 are complete may you begin modifying source code.
- If the task is simple and bypasses Step 3, proceed directly to modifying code on the isolated branch.
