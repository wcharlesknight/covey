# Claude Code Project Instructions

## Branch Workflow

### BEFORE YOU START CODING:
- ✅ **Verify you're NOT on main**: `git branch` should show a feature branch, never `* main`
- ❌ **NEVER code directly on main** — always use feature branches
- ❌ **NEVER push directly to main** — use PRs for all changes

### For Every New WBS Item:
1. **Sync with main first**
   ```bash
   git checkout main
   git pull origin main
   ```

2. **Create a new feature branch**
   - Branch naming: `feature/<wbs-item-name>` or `feature/<feature-name>`
   - Examples: `feature/signin-authentication`, `feature/weekly-spot-list`, `feature/user-profile`
   - Avoid reusing old branches even if they still exist
   - Verify: `git branch` shows your feature branch name with `*`

3. **Work on the branch**, then:
   ```bash
   git push -u origin feature/<name>
   gh pr create --title "..." --body "..."
   ```

4. **PR Requirements**:
   - Descriptive title under 70 characters
   - Summary section in body
   - Feature list (what was added/changed)
   - Test plan section with checkboxes
   - Link to Claude Code at end

### After PR is merged:
1. **Update PROGRESS.md** — document what was completed
2. **Check off items in docs/pm/wbs.md** — mark completed WBS items
3. **Update docs/sdlc.state.json** if planning artifacts were modified

---

## Development Environment

### iOS Setup
- Node 22.23.1 required (Expo SDK 51)
- `npx expo run:ios` to build and launch on simulator
- Metro config and Babel preset aligned with Expo standards

### Firebase
- Client config in `ios/.env` (git-ignored)
- Environment variables: `EXPO_PUBLIC_*`
- Auth methods: Apple, Google, Email/Password
- Firestore for real-time data

### API Backend
- Lambda deployed at: `https://lal06351qg.execute-api.us-west-2.amazonaws.com/dev`
- Local dev: set `EXPO_PUBLIC_API_BASE_URL=http://localhost:3000` in `.env`
- Auth: Firebase ID tokens required

### Lambda Deployment
- ❌ **NEVER use AWS CLI to update Lambda directly** — always go through CI/CD
- Backend changes → commit to feature branch → create PR → merge to main
- GitHub Actions workflow (`.github/workflows/deploy-nonprod.yml`) automatically:
  1. Builds Lambda JAR with Gradle
  2. Uploads to S3 (size > 70MB)
  3. Updates Lambda function
  4. Runs smoke tests
- Watch deployment in GitHub Actions tab after merging to main

---

## Code Standards

- No comments unless WHY is non-obvious
- One-liner comments max
- Clean up unused code entirely (no `// removed` placeholders)
- TypeScript strict mode enabled
- ESLint and Prettier aligned to Expo defaults

---

## Testing Locally

### iOS Simulator
```bash
# Fresh build with cache clear
npx expo run:ios

# Or start dev server and connect manually
npx expo start --clear
# Then press 'i' in terminal
```

### API Testing
```bash
# Check your Lambda is running
curl https://lal06351qg.execute-api.us-west-2.amazonaws.com/dev/me \
  -H "Authorization: Bearer <firebase-id-token>"
```

---

## Useful Commands

```bash
# Check branch status before starting
git status

# List remotes and branches
git branch -a

# Clear Metro cache if you hit odd build errors
pkill -9 node
rm -rf $TMPDIR/metro-cache*
npx expo start --clear
```
