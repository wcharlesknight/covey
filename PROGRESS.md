# Covey Project Progress & Memory

## 📍 Quick Status

**Current WBS:** 1.6 - Lambda Java Classpath Resolution  
**Status:** Java deployed, fixing ClassNotFoundException  
**Branch:** `feature/lambda-classpath-fix` (PR #16)

---

## 📚 Full Progress Documentation

**Location:** `/Users/charlieknight/.claude/projects/-Users-charlieknight/memory/covey_wbs_progress.md`

This file contains:
- ✅ Completed WBS items
- ⏳ Current work in progress
- 📋 Next steps and how to resume
- 🔗 Key files and branches
- 🏗️ Infrastructure reference

---

## 🚀 Quick Resume Guide

To pick up where we left off:

```bash
# 1. Update your repo
cd /Users/charlieknight/covey
git checkout main && git pull

# 2. Switch to active branch
git checkout feature/lambda-classpath-fix

# 3. Test current Lambda status
aws lambda invoke --function-name covey-weekly-spot-dev --region us-west-2 \
  --cli-binary-format raw-in-base64-out \
  --payload '{"path":"/me","httpMethod":"GET"}' /tmp/test.json && cat /tmp/test.json

# 4. Expected: ClassNotFoundException (issue to fix in PR #16)
```

---

## 🔑 Key Context

**What's Working:**
- ✅ Java 17 runtime active on Lambda
- ✅ 49.9MB fat JAR deployed
- ✅ LambdaRouter handler configured
- ✅ Bootstrap script included
- ✅ All core backend code in place (67 tests)

**What Needs Fixing:**
- ❌ ClassNotFoundException when Lambda tries to load Java classes
- ⏳ PR #16 pending merge (bootstrap + improved tests)
- 📌 Need to resolve JAR classpath/structure issue

---

## 📖 Documentation & Memory

**Progress Memory** (persistent across sessions):
- **Path:** `/Users/charlieknight/.claude/projects/-Users-charlieknight/memory/covey_wbs_progress.md`
- **Auto-loaded:** Claude Code reads this at session start
- **Contains:** Full WBS status, branches, files, infrastructure details

**Complete Documentation** (all in covey repo - `/covey/docs/`):

**Project Management:**
- `docs/pm/wbs.md` - Work Breakdown Structure
- `docs/pm/charter.md` - Project Charter
- `docs/pm/schedule.md` - Timeline and Milestones
- `docs/pm/risk-register.csv` - Risk Register

**Architecture & Design:**
- `docs/arch/` - System architecture, API specs, data models, ADRs
- `docs/arch/api/openapi.yaml` - API specification
- `docs/arch/data-model/` - ERD and Firebase schema

**Requirements:**
- `docs/req/` - Use cases, scenarios, activity diagrams

**Security:**
- `docs/security/` - Threat model, auth design, security checklist

**Testing & Quality:**
- `docs/test/` - Test strategy
- `docs/quality/` - Quality model and gates

**UX & Design:**
- `docs/ux/` - Personas, journeys, prototypes, design tokens

**DevOps & Business Analysis:**
- `docs/devops/` - Deployment and infrastructure docs
- `docs/ba/` - Business analysis docs

**SDLC State:**
- `docs/sdlc.state.json` - Planning state tracking

All 39 docs files now in covey project for easy access!

---

## 💡 Remember for Next Session

Before diving in, read the memory file to understand:
1. What's been completed
2. What PR #16 is about (classpath fix)
3. How to test the current Lambda state
4. What specifically needs to be fixed

**The memory is your guide - reference it first!**
