---
description: Inspect the current workspace and determine whether the pending work stage is structurally complete or requires changes, producing a rigorous Implementation Plan artifact.
---

# 🔍 Work Stage Reflection & Structural Completeness Workflow

> **Core Philosophy:** Structure IS persistence. Before crossing any architectural bridge or declaring work complete, map both sides. Inspect the physical codebase, git tree, and multi-agent stage branches to determine whether a stage is structurally complete or still has gaps.
> **Zero-Touch Mandate:** Do **NOT** edit any application code, access production, or run destructive operations during this phase. This is an epistemic assessment and cartography pass, not an execution pass.

---

## 🧭 Execution Triggers

This workflow activates when the user or orchestrator triggers:
- `/plan` or `/plan [stage/milestone description]`
- `/work-stage-reflection`
- *"Inspect the current workspace and determine whether the pending work stage is structurally complete or still requires changes."*
- Phase boundary checks when evaluating multi-agent PRs (e.g. Jules or peer agent submissions) before merging.

---

## 🔬 Phase 1: Physical Workspace & Branch Inventory

Before authoring the plan, inspect the physical state of the repository without assuming or guessing:

1. **Git State & Diff Inspection:**
   ```bash
   git status
   git log -n 5 --oneline
   git diff --stat origin/main...HEAD
   ```
2. **Multi-Agent Branch Audit (if PR is pending):**
   ```bash
   git fetch origin
   gh pr list
   gh pr view <PR_NUMBER> --json commits,comments,updatedAt
   git diff --stat <base_branch>...<head_branch>
   ```
3. **Verify Boundary Invariants:**
   - Confirm no unauthorized changes occurred in `.agents/` (orchestrator home) or `.jules/` (agent home).
   - Check if automated CI workflows (`.github/workflows/ci.yml`) exist on the branch.

---

## 📋 Phase 2: The 6-Section Analytical Framework

Every Work Stage Reflection must answer all six sections thoroughly and concretely:

### 1. Scope
- **Exact Additions & Changes**: What specifically is being added, modified, or removed in this stage?
- **Contribution Matrix**: What does this stage contribute across:
  - *Capabilities* (business logic, features, operations)
  - *API Surface* (routes, verbs, query parameters, payloads, headers)
  - *UI Surfaces* (views, modals, panes, responsive layouts, components)
  - *Data Layer* (tables, columns, types, defaults, migrations, constraints)
- **Downstream Dependencies**: What future work depends on or tails off from this stage? List each upcoming phase or task.
- **Explicit Exclusions**: What is explicitly **OUT of scope** for this stage?

### 2. Architecture & Integration
- **Structural Fit**: How does this change fit into the existing modular architecture?
- **Entry Points & Seams**: Which modules/files are the entry points it connects to?
- **New Files**: List all new files to be created. Detail the single architectural responsibility of each.
- **Modified Files**: List all existing files to be modified. For each: what exact changes are made, and why?
- **Change Ordering & Sequencing**: What is the correct dependency ordering of steps (e.g., migration → backend models → route validation → client adapter → UI components)?

### 3. Configuration & Settings
- **Environment Variables & Configuration**: What settings, environment variables, or config files are needed?
- **Specification Table**: For each setting: what value, where it lives (e.g. `.env`, process env, config file), and why it is required.
- **Defaults & Fallbacks**: Are there safe defaults? What occurs if they are missing (fail closed vs. fallback)?

### 4. Security & Trust Boundaries
- **Attack Surface Analysis**: What attack surface does this introduce or touch (inputs, endpoints, storage, authentication, authorization)?
- **Specific Mitigations**: What concrete mitigations are in place for each identified threat (e.g. Zod validation, parameterization, permission middleware, rate limits)?
- **Least-Privilege Boundary**: What is the least-privilege boundary for permissions or file/database access?
- **Secret & Cryptographic Handling**: Are secrets handled correctly? Verify:
  - Zero-Knowledge invariant: The server never inspects or decrypts sensitive user payload ciphertext.
  - No hardcoded keys, tokens, or credentials.
  - Proper environment injection and constant-time comparisons.

### 5. Verification & Completion Criteria
- **Definition of "Structurally Complete"**: List concrete, testable conditions—never accept *"it works"*.
- **Proof Matrix**: What tests, linters, or build checks prove each condition is satisfied?
- **Verification Sequence**: Detail the execution sequence:
  1. *Unit Tests* (isolated pure functions, schemas, utility logic)
  2. *Integration / Contract Tests* (database transactions, API routes, authentication gates)
  3. *Cloud CI Build Gate* (push to branch and tail via `gh run watch` — **never heavy builds or tests locally**)
  4. *Live Handshake* (manual verification checklist in running app)
- **Rollback Strategy**: What is the exact rollback path if verification fails at any stage?

### 6. Risks & Decisions Needed
- **Falsifiable Assumptions**: What assumptions are being made that could be invalid?
- **User Approval Gates**: What architectural decisions or trade-offs require human approval before proceeding?
- **Migration & Compatibility Risks**: Are there backward compatibility issues, orphan data states, or database lock risks?

---

## 📄 Phase 3: Artifact Generation Standards

The reflection produces a single markdown artifact: `implementation_plan.md` (or `stage_<N>_implementation_plan.md`) located in the active artifact directory.

The artifact **must** include:
1. **Mermaid Diagrams**: Include high-contrast, visually aesthetic Mermaid diagrams illustrating architecture, state transitions, or sequence flows (`RULE: mermaid-plans.md`).
2. **File-by-File Change Table**:
   | Step | File | Action | Summary of Change | Shared/Public Code? |
   |---|---|---|---|---|
   | 1 | `path/to/file` | `Create` / `Edit` / `Revert` | Specific function, schema, or route modified | `Yes` / `No` |
3. **Numbered Steps**: Execution steps strictly ordered by topological dependency.
4. **Public/Shared Flags**: Every step touching public-facing APIs, shared libraries, or user-facing UI must be flagged.
5. **One-Line Proceed Boundary**: Conclude with a clear, single-line boundary statement indicating what must be approved before execution begins.

---

## 🚫 Anti-Patterns to Strictly Avoid
- **Never Modify Code During Reflection**: Do not write, edit, or delete application code while running this workflow.
- **Never Run Heavy Local Builds or Tests**: Adhere strictly to `.agents/rules/git-hygiene.md` and `.agents/rules/cadence-and-lifecycle-prompts.md`. Tests and production builds must run in GitHub Actions cloud runners.
- **Never Assume Completeness Without Witnessing Code**: Inspect the actual AST, git diff, or file contents. An agent claiming "all tests pass" in a PR comment is not sufficient proof.
- **Never Omit the Proceed Boundary**: Always stop and request explicit user confirmation before initiating the execution phase.
