# Project Charter: Location Notifier - Social Gathering App

**Date**: 2026-06-27
**Project Manager**: William Knight
**Sponsor**: William Knight (Self-funded)

---

## Executive Summary

Location Notifier is a mobile application that helps friend groups discover and gather at curated local venues on a predictable weekly cadence. Each week, the system selects a "weekly spot" in a supported city, notifies invited users via push notification and email, and allows them to RSVP so attendees know who plans to show up.

The MVP targets iOS users in Seattle and Tacoma. It solves the coordination overhead of "where should we meet this week?" by automating venue selection and notification, giving users a reliable, low-friction way to gather with their social circle.

The project is a solo or small-team effort with a target of approximately 11-14 weeks from kickoff to App Store launch. Success is defined by a stable, working app with real users RSVPing to real weekly spots.

---

## Business Case

Social coordination today relies on ad hoc group chats, fragmented calendar invites, and repeated "where do you want to go?" conversations. This friction causes plans to fall apart and reduces the frequency with which friend groups actually gather in person.

Location Notifier removes this friction by making the venue decision automatic and the invitation proactive. Users receive a weekly push notification with a curated spot already selected. They tap to RSVP, see who else is coming, and show up. No negotiation, no coordination overhead.

The app targets the underserved space between "spontaneous meetups" (too much planning) and "recurring events" (too rigid). A weekly venue cadence hits a predictable rhythm that trains habits without locking users into a fixed commitment.

---

## Project Objectives

1. Deliver a production-ready iOS app on the Apple App Store by the end of Week 14 (target: 2026-10-04).
2. Support the full RSVP workflow: sign-in, receive invite, view weekly spot and 4-week history, respond yes/no/interested.
3. Operate within the AWS and Firebase free tiers for MVP traffic (target: under $20/month at 100 active users).
4. Achieve a weekly job reliability rate of 99%+ (spot selection, invite creation, push delivery run without manual intervention).
5. Reach 100+ registered users across Seattle and Tacoma within 30 days of launch.

---

## Success Criteria

- App successfully passes Apple App Store review on first or second submission.
- Weekly Lambda job runs automatically without errors for four consecutive weeks post-launch.
- At least 50 users complete the full flow (sign-in, receive invite, RSVP) in the first month.
- Zero critical security incidents (unauthorized data access, authentication bypass) in the first 90 days.
- Crash-free session rate of 98%+ as measured by Firebase Crashlytics.

---

## Scope

### In Scope

- iOS native app (SwiftUI or UIKit)
- Firebase Authentication (Apple Sign-In, Google Sign-In)
- User profile management (GET /me, PATCH /me)
- City selection: Seattle and Tacoma
- Weekly spot feed: current spot plus 4-week history with RSVP counts
- RSVP functionality: yes, no, interested
- Push notification delivery (Firebase Cloud Messaging)
- Email notification delivery
- AWS Lambda backend with Java runtime
- Weekly scheduled Lambda job: spot selection, invite creation, notification dispatch
- Firestore as primary datastore
- Google Places API integration for venue data
- Backend unit and integration tests
- iOS unit and UI tests
- TestFlight beta testing
- App Store submission and launch

### Out of Scope

- Android app
- Web application or admin dashboard
- Payments or subscription billing
- In-app social features (comments, reactions, direct messaging, friend requests within the app)
- Venue operator portal or business accounts
- Custom venue submission by users
- Multi-language / internationalization
- Cities beyond Seattle and Tacoma (architecture supports expansion; not in MVP delivery)
- Analytics dashboard or reporting UI

---

## Key Stakeholders

| Stakeholder | Role | Interest | Influence |
|---|---|---|---|
| William Knight | Product Owner + Engineer | High - sole builder and decision-maker | High |
| Beta users (Seattle/Tacoma) | Early adopters / testers | Medium - want a working, useful app | Medium |
| Apple App Store Review | Gatekeeper | Low - enforces App Store guidelines | High |
| Venue operators | Eventual customers (post-MVP) | Low at MVP stage | Low |
| Google Places API | External dependency | Low - passive dependency | Medium |
| AWS / Firebase | Infrastructure providers | Low - passive dependency | Medium |

---

## Constraints

- **Time**: MVP in approximately 11-14 weeks; no fixed deadline but earlier is better.
- **Budget**: AWS and Firebase free tiers preferred. Google Places API costs must stay within low-usage pricing. Apple Developer Program fee ($99/year) already assumed.
- **Resources**: Solo engineer or very small team; no parallel development tracks until backend API is stable.
- **Technical**: iOS only (no cross-platform framework). Java Lambda runtime. Firebase Auth as the identity provider. Firestore as the database.
- **External**: App Store submission review time (typically 24-72 hours but can vary). Google Places API key must be provisioned before venue data integration.

---

## Assumptions

- Firebase project is created and configured before development begins.
- AWS account with Lambda and appropriate IAM permissions is ready.
- Apple Developer Program membership is active (required for TestFlight and App Store distribution).
- Google Places API key is provisioned with sufficient quota for MVP usage.
- Push notification certificates (APNs) are configured in Firebase before iOS notification testing.
- The weekly spot selection logic (curating venues from Google Places) is implemented by the engineer; there is no external curator or editorial team at MVP.
- Venue exclusion list is managed manually via Firestore at MVP (no admin UI).
- Users invited to the app are sourced from the developer's personal and professional network for the initial beta.

---

## Milestones

| Milestone | Target Week | Key Deliverables |
|---|---|---|
| M0: Kickoff | Week 1 | Charter, WBS, monorepo structure, Firebase + AWS environments configured |
| M1: Backend Feature-Complete | Week 5 | All API endpoints implemented, weekly job working, unit + integration tests passing |
| M2: iOS Feature-Complete | Week 10 | All screens built and wired to backend, push notifications working, unit + UI tests passing |
| M3: Integration and Security Review | Week 12 | End-to-end flows validated, security review complete, performance acceptable |
| M4: Beta (TestFlight) | Week 13 | App distributed to beta testers, feedback collected and critical issues resolved |
| M5: App Store Launch | Week 14 | App Store submission approved, public launch announced |

---

## High-Level Risks

| Risk | Probability | Impact | Mitigation |
|---|---|---|---|
| App Store review rejection | Medium | High | Follow Human Interface Guidelines closely; test submission checklist before first submission |
| Weekly Lambda job failure | Low | High | Implement retry logic, CloudWatch alarms, and alerting on failure |
| Google Places API quota exhaustion | Medium | High | Monitor usage; request quota increase before launch; cache venue data |
| Firebase/AWS configuration errors causing data loss | Low | High | Firestore security rules reviewed; backups enabled; dev/prod environments separated |
| Scope creep delaying launch | Medium | Medium | Hard scope boundaries enforced; out-of-scope items deferred to post-MVP backlog |
| Solo engineer unavailability | Medium | Medium | Modular architecture; documented code; no single point of failure in deployment |

---

## Budget Summary

- **Total Budget**: Under $50/month at MVP scale
- **Personnel**: Self-funded (no cash outlay)
- **Infrastructure (AWS Lambda + Firestore)**: ~$0/month at free tier (< 1M Lambda invocations/month)
- **Firebase**: Free Spark plan for MVP; upgrade to Blaze pay-as-you-go if usage warrants
- **Google Places API**: ~$0-5/month at low query volume
- **Apple Developer Program**: $99/year (already assumed active)
- **Contingency**: $20/month buffer for unexpected API costs

---

## Approval

**Product Owner / Engineer**: _____________________ Date: _______
