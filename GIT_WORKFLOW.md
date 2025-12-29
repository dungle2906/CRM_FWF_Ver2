# Git Workflow Guide for Team Collaboration

This document outlines the Git workflow and best practices for our development team. Follow these guidelines to ensure smooth collaboration and maintainable codebase.

---

## Table of Contents

1. [Repository Structure](#1-repository-structure)
2. [Branching Strategy](#2-branching-strategy)
3. [Feature Branch Workflow](#3-feature-branch-workflow)
4. [Creating a Branch](#4-creating-a-branch)
5. [Pull Request Rules](#5-pull-request-rules)
6. [Commit Message Style](#6-commit-message-style)
7. [Environment Variables](#7-environment-variables)
8. [Conflict Resolution](#8-conflict-resolution)
9. [Reverting Code & Common Issues](#9-reverting-code--common-issues)

---

## 1. Repository Structure

Our repository follows a monorepo structure optimized for enterprise-grade maintainability and scalability:

```
.
├── frontend/              # Next.js / React application (UI)
├── backend/               # API, business logic, database access
├── poc/                   # Proof of concept, a small test demo to prove an idea is impossible
├── docs/                  # Architecture diagrams, design notes, API documentation
├── deployment/            # Infrastructure as Code (IaC)
│   ├── docker/            # Docker-related files
│   │   ├── Dockerfile.frontend
│   │   ├── Dockerfile.backend
│   │   └── docker-compose.yml
│   ├── caddy/             # Caddy server configuration
│   │   └── Caddyfile
│   ├── kubernetes/        # K8s manifests (if applicable)
│   └── scripts/           # Deployment scripts
├── .github/               # CI/CD workflows, issue templates, PR templates
│   ├── workflows/
│   ├── ISSUE_TEMPLATE/
│   └── PULL_REQUEST_TEMPLATE.md
├── .gitignore
├── README.md
└── LICENSE
```

### Why This Structure?

- **Separation of Concerns**: Frontend, backend, and deployment configurations are clearly separated
- **Deployment Folder**: All infrastructure files (Docker, Caddy, K8s) are centralized in `deployment/` for:
  - Easy maintenance and version control
  - Clear ownership and responsibility
  - Simplified CI/CD pipeline configuration
  - Better security (can restrict access to deployment configs)
- **Scalability**: Easy to add new services or microservices
- **Maintainability**: Clear structure makes onboarding new team members easier

### Docker & Caddy Files Location

**Recommendation**: Place all Docker and Caddy files in `deployment/` folder.

**Why?**
- ✅ Source code remains clean and focused
- ✅ Deployment configurations are version-controlled separately
- ✅ CI/CD pipelines can reference deployment files easily
- ✅ Follows Infrastructure as Code (IaC) best practices
- ✅ Easier to manage different environments (dev, staging, prod)

**Example Dockerfile paths:**
```
deployment/docker/Dockerfile.frontend  → Builds from frontend/
deployment/docker/Dockerfile.backend    → Builds from backend/
```

**Example Caddyfile path:**
```
deployment/caddy/Caddyfile  → References frontend/ and backend/ paths
```

---

## 2. Branching Strategy

We use a **GitFlow-lite** approach with two long-lived branches:

### Long-lived Branches

- **`main`** → Production-ready code (protected, no direct commits)
- **`dev`** → Staging/integration branch (protected, no direct commits)

### Workflow Overview

```
main (production)
  ↑
  │ (after testing)
  │
dev (staging/integration)
  ↑
  │ (after PR approval)
  │
feat/fe/BAM-40/dashboard (feature branch)
```

### Development Process

1. **Feature Development**: Each collaborator creates their own feature branch from `dev`
   - Example: Collaborator A working on authentication → `feat/be/BAM-30/authentication`
   - Example: Collaborator B working on dashboard → `feat/fe/BAM-40/dashboard`

2. **Incremental Commits**: Make small, focused commits as you complete each part:
   - ✅ Complete API login endpoint → commit
   - ✅ Complete UI login form → commit
   - ✅ Add validation → commit
   - This makes it easier to track progress and revert if needed

3. **Role-based Development**: If team has Backend/Frontend roles:
   - **Backend**: Develop one API at a time, commit after each API
   - **Frontend**: Develop one UI feature at a time, commit after each feature

4. **Merge Process**:
   - Owner/Lead reviews the PR
   - Merge into `dev` for final testing
   - After testing passes, merge `dev` → `main` for production deployment

---

## 3. Feature Branch Workflow

Every task must be completed in its own branch created from `dev`.

### Naming Conventions

#### Features
```
feat/<fe|be>/<JIRA-TICKET-ID>/<short-description>
```

**Examples:**
- `feat/fe/BAM-40/dashboard`
- `feat/be/BAM-30/authentication-api`
- `feat/fe/BAM-45/user-profile-page`

#### Bug Fixes
```
fix/<fe|be>/<JIRA-TICKET-ID>/<short-description>
```

**Examples:**
- `fix/be/BAM-10/auth-api-error`
- `fix/fe/BAM-25/login-button-styling`
- `fix/be/BAM-15/database-connection-timeout`

#### Chores / Cleanup
```
chore/<fe|be>/<JIRA-TICKET-ID>/<short-description>
```

**Examples:**
- `chore/be/BAM-10/update-env-docs`
- `chore/fe/BAM-20/refactor-components`
- `chore/be/BAM-5/update-dependencies`

### Important Notes

- **Role Clarity**: Even though Jira tickets specify BE/FE/BA/DevOps roles, include `<fe|be>` in branch names for clarity
- **Keep Descriptions Short**: Limit description to 3-4 words
- **Be Specific**: Avoid generic names like `feat/update-ui` or `fix/bug`. Be specific about what changed
- **Delete After Merge**: Always select "Delete source branch" after PR is approved to keep branch list clean

### Jira Integration Tip

When you include Jira ticket ID (e.g., `BAM-40`) in your branch name, Jira automatically links the branch status to the ticket, making it easier for PMs to track progress.

---

## 4. Creating a Branch

Follow these steps to create a new feature branch:

```bash
# 1. Switch to dev branch
git checkout dev

# 2. Pull latest changes from remote
git pull origin dev

# 3. Create and switch to new feature branch
git checkout -b feat/fe/BAM-40/dashboard

# 4. Verify you're on the correct branch
git branch
```

**Before starting work, always ensure:**
- ✅ You're on the latest `dev` branch
- ✅ Your local `dev` is synced with `origin/dev`
- ✅ You've created the branch from `dev`, not from `main`

---

## 5. Pull Request Rules

All changes must go through a Pull Request (PR). Direct commits to `main` or `dev` are not allowed.

### PR Requirements

#### 1. PR Must Be Small and Focused
- One feature or one bug fix per PR
- If a feature is large, break it into smaller PRs
- Easier to review = faster approval

#### 2. PR Title Format
```
<type>(<scope>): <JIRA-ID> - <description>
```

**Examples:**
- `feat(frontend): BAM-40 - Dashboard implementation`
- `fix(backend): BAM-10 - Authentication API error handling`
- `chore(backend): BAM-5 - Update environment documentation`

#### 3. PR Description Template

Use this template for all PRs:

```markdown
## Description
Brief description of what this PR does.

## Jira Ticket
- [BAM-XXX](link-to-jira-ticket)

## Type of Change
- [ ] New feature
- [ ] Bug fix
- [ ] Documentation update
- [ ] Refactoring
- [ ] Other (please specify)

## What Changed
- List specific changes made
- API endpoints added/modified
- UI components added/modified
- Configuration changes

## Why It Changed
- Business requirement
- Bug fix reason
- Performance improvement
- Code quality improvement

## Environment Variables
- [ ] No new environment variables
- [ ] New environment variables added (list below):
  - `NEW_VAR_NAME`: Description
  - `ANOTHER_VAR`: Description

## Testing
- [ ] Unit tests added/updated
- [ ] Integration tests added/updated
- [ ] Manual testing completed
- [ ] Tested on local environment
- [ ] Tested on staging environment

## Screenshots (if applicable)
[Add screenshots for UI changes]

## Checklist
- [ ] Code follows project style guidelines
- [ ] Self-review completed
- [ ] Comments added for complex logic
- [ ] Documentation updated
- [ ] No new warnings generated
- [ ] Tests pass locally
- [ ] Dependencies updated (if applicable)
```

#### 4. Where to Merge

- **Features** → Merge into `dev`
- **Emergency hotfixes** → Merge into `main`, then merge `main` → `dev` to keep them synced

#### 5. Review Process

- At least one approval required (Owner/Lead)
- All CI/CD checks must pass
- No merge conflicts
- Branch must be up-to-date with target branch

---

## 6. Commit Message Style

Use the following format for commit messages:

```
<type>(<scope>): <description>
```

### Types

- `feat` — New feature
- `fix` — Bug fix
- `chore` — Cleanup, configuration changes
- `docs` — Documentation updates
- `refactor` — Code improvements without behavioral change
- `test` — Adding or updating tests
- `style` — Code style changes (formatting, missing semicolons, etc.)
- `perf` — Performance improvements
- `hotfix` — Emergency fix in production

### Scope (Optional but Recommended)

- `frontend` — Frontend changes
- `backend` — Backend changes
- `api` — API-specific changes
- `auth` — Authentication-related
- `ui` — UI components
- `db` — Database changes
- `config` — Configuration changes

### Examples

**Good Commit Messages:**
```
feat(backend): BAM-30 - Add JWT authentication endpoint
fix(frontend): BAM-25 - Fix login button disabled state
chore(backend): BAM-10 - Update environment variable documentation
docs(api): Add API endpoint documentation for user management
refactor(frontend): BAM-40 - Extract dashboard components
hotfix(backend): BAM-50 - Fix critical security vulnerability
```

**Bad Commit Messages (Avoid):**
```
❌ update
❌ fix
❌ final
❌ temporary
❌ WIP
❌ asdf
❌ fixed bug
❌ changes
```

### Best Practices

- Use present tense: "Add feature" not "Added feature"
- Be specific: "Fix login validation error" not "Fix bug"
- Reference Jira ticket if applicable
- Keep first line under 72 characters
- Add detailed description in body if needed (separate with blank line)

---

## 7. Environment Variables

### Rules

1. **Never commit `.env` files**
   - `.env` files contain sensitive information
   - Already added to `.gitignore`

2. **Never commit API keys or secrets**
   - Check `.gitignore` includes all sensitive files
   - Use environment variables or secret management tools

3. **Always update `.env.example`**
   - When adding new environment variables, update `.env.example`
   - Document what each variable does
   - Provide default values (if safe) or placeholder values

4. **Document in PR Description**
   - If your PR adds new environment variables, list them in the PR description
   - Explain what each variable is for
   - Mention if it's required or optional

### Example `.env.example` Format

```env
# Database Configuration
JDBC_CONNECTION_DB_URL=jdbc:mysql://localhost:3306/database_name
JDBC_CONNECTION_DB_NAME=username
JDBC_CONNECTION_DB_PASSWORD=password

# API Keys (Required)
JWT_SECRET_KEY=your_secret_key_here
API_KEY=your_api_key_here

# Optional Configuration
DEBUG_MODE=false
LOG_LEVEL=info
```

---

## 8. Conflict Resolution

### When Conflicts Occur

Conflicts happen when:
- Multiple people modify the same file
- Your branch is behind the target branch
- Someone else merged changes that conflict with yours

### Resolution Process

#### Option 1: Resolve on GitHub (For Simple Conflicts)

**When to use:** Few conflicts, simple changes, easy to resolve via web interface

**Steps:**

1. **Update your branch:**
   ```bash
   git checkout your-feature-branch
   git pull origin dev
   ```

2. **Push to trigger conflict:**
   ```bash
   git push origin your-feature-branch
   ```

3. **On GitHub:**
   - Go to your PR
   - Click "Resolve conflicts" button
   - GitHub will show conflicted files

4. **Resolve in GitHub editor:**
   - Look for conflict markers:
     ```
     <<<<<<< HEAD
     Your changes
     =======
     Incoming changes
     >>>>>>> branch-name
     ```
   - Choose which version to keep or combine both
   - Delete conflict markers (`<<<<<<<`, `=======`, `>>>>>>>`)

5. **Mark as resolved:**
   - Click "Mark as resolved" for each file
   - Click "Commit merge" when all conflicts are resolved

6. **Continue with PR:**
   - Conflicts are now resolved
   - PR can be merged

#### Option 2: Resolve in IDE (For Complex Conflicts)

**When to use:** Many conflicts, complex changes, need better tooling (VSCode/Cursor/IntelliJ)

**Steps:**

1. **Fetch latest changes:**
   ```bash
   git checkout dev
   git pull origin dev
   ```

2. **Switch back to your branch:**
   ```bash
   git checkout your-feature-branch
   ```

3. **Merge or rebase dev into your branch:**
   ```bash
   # Option A: Merge (creates merge commit)
   git merge dev
   
   # Option B: Rebase (cleaner history, but rewrites commits)
   git rebase dev
   ```

4. **IDE will show conflicts:**
   - **VSCode/Cursor**: Conflicts appear in "Source Control" panel
   - **IntelliJ**: Conflicts highlighted in editor with conflict markers

5. **Resolve conflicts in IDE:**
   - **VSCode/Cursor**:
     - Click on conflicted file
     - Use "Accept Current Change", "Accept Incoming Change", or "Accept Both Changes"
     - Or manually edit to combine changes
   - **IntelliJ**:
     - Click conflict markers
     - Choose "Accept Yours", "Accept Theirs", or "Merge"
     - Or use 3-way merge view

6. **Stage resolved files:**
   ```bash
   git add <resolved-file-1>
   git add <resolved-file-2>
   # Or stage all resolved files
   git add .
   ```

7. **Complete merge/rebase:**
   ```bash
   # If merging:
   git commit -m "Merge dev into feature branch"
   
   # If rebasing:
   git rebase --continue
   ```

8. **Push changes:**
   ```bash
   # If you merged:
   git push origin your-feature-branch
   
   # If you rebased (force push required):
   git push origin your-feature-branch --force-with-lease
   ```
   ⚠️ **Warning**: Only use `--force-with-lease` if you're sure no one else is working on this branch

### Conflict Resolution Best Practices

- **Communicate**: If you see conflicts, coordinate with team members
- **Understand changes**: Don't just accept one side blindly. Understand what changed and why
- **Test after resolution**: Always test your code after resolving conflicts
- **Small PRs**: Smaller PRs = fewer conflicts
- **Regular updates**: Regularly pull from `dev` to stay up-to-date

---

## 9. Reverting Code & Common Issues

### When to Revert

- Critical bug introduced to production
- Feature causing issues and needs to be rolled back
- Security vulnerability discovered
- Performance degradation

### Revert Process

#### Option 1: Revert a Single Commit

```bash
# 1. Find the commit hash to revert
git log --oneline

# 2. Revert the commit (creates a new commit that undoes the changes)
git revert <commit-hash>

# 3. Push the revert commit
git push origin <branch-name>
```

#### Option 2: Revert a Merge Commit

```bash
# 1. Find the merge commit hash
git log --oneline --merges

# 2. Revert the merge commit
git revert -m 1 <merge-commit-hash>
# -m 1 specifies which parent to revert to (usually main branch)

# 3. Push the revert
git push origin <branch-name>
```

#### Option 3: Reset to Previous State (Use with Caution)

⚠️ **Warning**: Only use this if the problematic code hasn't been pushed or if you're working alone on the branch.

```bash
# Soft reset (keeps changes in working directory)
git reset --soft <commit-hash>

# Hard reset (discards all changes)
git reset --hard <commit-hash>
```

### Common Issues & Solutions

#### Issue 1: Accidentally Committed Sensitive Data

**Problem:** API keys, passwords, or secrets were committed to Git.

**Solution:**

1. **Remove from Git history:**
   ```bash
   # If not yet pushed:
   git reset HEAD~1
   # Then add to .gitignore and recommit
   
   # If already pushed, use git-filter-repo or BFG Repo-Cleaner:
   git filter-repo --invert-paths --path <file-with-secret>
   ```

2. **Rotate credentials:**
   - Change all exposed API keys/passwords immediately
   - Notify team members

3. **Prevent future issues:**
   - Add file to `.gitignore`
   - Use `.env.example` for templates
   - Consider using secret management tools

#### Issue 2: Pushed to Wrong Branch

**Problem:** Committed changes to `main` or `dev` instead of feature branch.

**Solution:**

```bash
# 1. Create a new branch from current state (saves your work)
git branch backup-branch

# 2. Reset the wrong branch to previous state
git checkout main  # or dev
git reset --hard HEAD~1  # or specific commit

# 3. Switch to your feature branch
git checkout your-feature-branch

# 4. Cherry-pick your commits
git cherry-pick <commit-hash>

# 5. Force push to fix remote (coordinate with team first!)
git push origin main --force-with-lease
```

#### Issue 3: Lost Local Changes

**Problem:** Accidentally ran `git reset --hard` or lost uncommitted work.

**Solution:**

```bash
# Try to recover from reflog
git reflog

# Find the commit where you had your changes
git checkout <commit-hash>

# Create a new branch to save the work
git checkout -b recovered-changes

# Or if you had uncommitted changes, check:
git fsck --lost-found
```

#### Issue 4: Merge Conflict Hell

**Problem:** Too many conflicts, branch is too far behind.

**Solution:**

```bash
# Option 1: Rebase interactively to clean up commits
git rebase -i dev

# Option 2: Create a new branch from latest dev and cherry-pick your changes
git checkout dev
git pull origin dev
git checkout -b new-feature-branch
git cherry-pick <commit-hash-1> <commit-hash-2> ...

# Option 3: Merge dev and resolve conflicts systematically
git merge dev
# Resolve conflicts file by file
git add .
git commit -m "Resolve merge conflicts with dev"
```

#### Issue 5: Force Pushed and Lost Team's Work

**Problem:** Someone force-pushed and overwrote commits.

**Solution:**

```bash
# Recover from reflog
git reflog

# Find the commit before force push
git checkout <commit-hash>

# Create recovery branch
git checkout -b recovery-branch

# Communicate with team to coordinate recovery
```

**Prevention:**
- Always use `--force-with-lease` instead of `--force`
- Coordinate with team before force pushing
- Consider branch protection rules

#### Issue 6: Large File Committed

**Problem:** Accidentally committed a large file (video, binary, etc.).

**Solution:**

```bash
# Remove from Git history
git filter-branch --tree-filter 'rm -f path/to/large-file' HEAD

# Or use git-filter-repo (recommended)
git filter-repo --path path/to/large-file --invert-paths

# Add to .gitignore
echo "large-file" >> .gitignore

# Force push (coordinate with team!)
git push origin --force --all
```

#### Issue 7: Wrong Files in Commit

**Problem:** Committed files that shouldn't be in the commit.

**Solution:**

```bash
# If not yet pushed:
git reset HEAD~1
# Unstage unwanted files
git reset HEAD <unwanted-file>
# Recommit only wanted files
git commit -m "feat: correct commit message"

# If already pushed:
git commit --amend
# Remove unwanted files, then force push (if working alone)
git push origin branch-name --force-with-lease
```

#### Issue 8: Branch Out of Sync

**Problem:** Branch is many commits behind `dev`, causing constant conflicts.

**Solution:**

```bash
# Option 1: Rebase onto latest dev
git checkout your-feature-branch
git rebase dev
# Resolve conflicts as they appear
git push origin your-feature-branch --force-with-lease

# Option 2: Merge dev into your branch
git checkout your-feature-branch
git merge dev
# Resolve conflicts
git push origin your-feature-branch
```

### Emergency Hotfix Process

If a critical bug is found in production:

1. **Create hotfix branch from main:**
   ```bash
   git checkout main
   git pull origin main
   git checkout -b hotfix/BAM-XXX/critical-bug-fix
   ```

2. **Fix the issue and commit:**
   ```bash
   # Make fixes
   git add .
   git commit -m "hotfix(backend): BAM-XXX - Fix critical production bug"
   ```

3. **Merge to main:**
   ```bash
   git checkout main
   git merge hotfix/BAM-XXX/critical-bug-fix
   git push origin main
   ```

4. **Merge main to dev:**
   ```bash
   git checkout dev
   git merge main
   git push origin dev
   ```

5. **Delete hotfix branch:**
   ```bash
   git branch -d hotfix/BAM-XXX/critical-bug-fix
   git push origin --delete hotfix/BAM-XXX/critical-bug-fix
   ```

---

## Quick Reference Checklist

### Before Starting Work
- [ ] Pull latest `dev` branch
- [ ] Create feature branch from `dev`
- [ ] Verify branch naming follows convention

### During Development
- [ ] Make small, focused commits
- [ ] Write clear commit messages
- [ ] Regularly pull from `dev` to stay updated
- [ ] Never commit `.env` files or secrets

### Before Creating PR
- [ ] Code is tested locally
- [ ] All conflicts resolved
- [ ] Branch is up-to-date with `dev`
- [ ] Commit messages follow style guide
- [ ] `.env.example` updated (if needed)
- [ ] Documentation updated (if needed)

### PR Review
- [ ] PR title follows format
- [ ] PR description is complete
- [ ] All CI/CD checks pass
- [ ] At least one approval obtained
- [ ] Ready to merge

### After Merge
- [ ] Delete feature branch
- [ ] Update local `dev` branch
- [ ] Verify changes in `dev`

---

## Additional Resources

- [Git Documentation](https://git-scm.com/doc)
- [GitHub Flow](https://guides.github.com/introduction/flow/)
- [Conventional Commits](https://www.conventionalcommits.org/)
- [Atlassian Git Tutorials](https://www.atlassian.com/git/tutorials)

---

## Questions or Issues?

If you encounter any issues or have questions about the Git workflow:

1. Check this document first
2. Ask in team chat/Slack
3. Contact the repository owner or project lead
4. Create an issue in the repository

---

**Last Updated:** [Date]
**Maintained by:** [Team/Lead Name]

