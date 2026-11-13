# Milaavo: product vision

*Version 0.1, 24 Oct 2026. Owner: Murali Putta. Status: draft.*

## Problem

When someone loses a wallet, a phone, keys, a bag or a document, the person who finds it usually has no good way to reach the owner. Lost-and-found information is scattered across police stations, transit offices, social-media posts and word of mouth. The owner does not know where to look. The finder does not know who to give it to, and often does not want to share a personal phone number with a stranger. Most found items never go back to their owners.

## Vision

> **Milaavo reunites people with what they lost.** Owners report what they lost, finders report what they found, and Milaavo matches the two. It connects owner and finder safely and privately, and helps them complete the return.

"Milaavo" comes from *milāvo*, "bring together".

## Who it is for

| Persona | Need |
|---|---|
| **Owner** (lost something) | Report it fast, ideally from a photo. Get told when a likely match appears. Prove ownership without fuss |
| **Finder** (found something) | Report it in under a minute. Do the right thing without exposing a phone number or being exploited by false claimants |
| **Custodian** (later: shop, office, transit desk) | Log many found items and hand them over with an audit trail |
| **Moderator / admin** | Remove abuse, resolve disputes, watch for fraud |

## Value proposition

1. **Two-sided matching.** Lost and found reports are matched automatically on category, description, place, time and image similarity. Users do not have to search through listings themselves.
2. **Photo-first reporting.** Upload a photo and AI suggests the category, colour, brand and a description, so a report takes seconds.
3. **Privacy by default.** Phone numbers are never shown. People communicate through in-app messages until both sides agree to share contact details.
4. **Trustworthy handover.** The finder keeps back a hidden detail of the item. The claimant must describe it, which filters out false claims before anyone meets.

## Goals

### Product goals

- G1: Report a lost or found item in **under 60 seconds** on a phone browser.
- G2: Show the owner relevant candidate matches **within minutes** of a matching found report.
- G3: Never expose a user's phone number or exact address without that user's explicit consent.
- G4: Every completed return leaves an auditable trail: match → claim → verification → handover.

### Engineering learning goals

The project is also a deliberate learning exercise. Each feature is chosen because it also exercises an important engineering skill:

| Learning goal | Where the product forces it |
|---|---|
| Backend depth (transactions, concurrency, security) | OTP, outbox, claim state machine, ownership checks |
| System design | Matching pipeline, async processing, scaling plan |
| **DevOps and cloud** | Real deployment: containers, CI/CD, infrastructure as code, TLS, monitoring, backups |
| **Applied AI** | Image understanding, embeddings, vector search, human-in-the-loop matching, AI cost and failure handling |
| Frontend (existing strength) | Responsive React web app with camera upload |

## Constraints

| Constraint | Consequence |
|---|---|
| **Zero running cost** | No paid SMS, no paid AI API by default, free-tier or self-hosted infrastructure only. Every external dependency sits behind an interface so a paid provider can be added later without redesign |
| **Solo developer, about 10 h per week** | Modular monolith. Small vertical slices |
| **Web first** | One responsive web app that works well in mobile browsers. Native apps are out of scope |
| **Real deployment** | It must run on the public internet with HTTPS, so DevOps practice is real and not simulated |

## Non-goals (for now)

- Native iOS or Android apps.
- Payments, rewards or bounties.
- Courier or shipping integration.
- Integration with police or government databases.
- Real SMS delivery at scale. OTP delivery runs in a free demo mode (see [requirements](requirements.md#36-notifications-fr-notif)).
- Multi-language UI. English first, with i18n-ready strings.

## Guiding principles

1. **Privacy over convenience.** When in doubt, reveal less.
2. **Humans confirm, AI suggests.** AI never auto-closes a match or hands over an item. It ranks and pre-fills, and people decide.
3. **Degrade gracefully.** If the AI service is down, reporting and rule-based matching still work.
4. **Explainable matches.** Every suggested match says *why*, for example "same category, 1.2 km apart, 2 days apart, image similarity 0.87".
5. **Boring infrastructure, interesting problems.** Use PostgreSQL for everything it can do (queue, full-text search, vectors with `pgvector`) before adding new systems.
6. **Every feature is an interview story.** Document the decision, the alternatives and the failure modes as you build it.

## Success measures

Product (measured once it is deployed with test users):

- Median time to submit a report.
- Share of found reports that receive a candidate match.
- Match precision: the share of suggested matches that users mark "this is it".
- Completed returns.
- Zero incidents of personal data being exposed.

Learning:

- The deployed app is reachable over HTTPS, with CI/CD deploying on merge.
- You can explain the matching pipeline, the AI fallback and the claim state machine in a system-design discussion.
- Infrastructure is reproducible from code (Terraform/Ansible) and documented in a runbook.

## Phases

| Phase | Outcome | Status |
|---|---|---|
| **0. Foundation** | Auth + OTP, lost reports, SMS outbox, docs | Mostly done. Gaps in [09](../design/09-gaps-and-next-actions.md) |
| **1. Two-sided reports** | Lost *and* found reports with owners, photos, location, dates. Ownership (G-06) ✅ done | Next |
| **2. Rule-based matching + claim** | Candidate matching without AI, in-app messaging, claim with hidden-detail verification, handover | Planned |
| **3. Web UI + deployment** | Responsive React app. Containerized, CI/CD, IaC, HTTPS, monitoring on free-tier cloud | Planned |
| **4. AI assistance** | Attributes from photos, image and text embeddings, vector similarity in matching | Planned |
| **5. Hardening** | Moderation, abuse limits, custodian accounts, retention, load test | Later |

Phases 3 and 4 can overlap. Deploy early, even before AI, so every later feature ships through the real pipeline.

## Risks

| Risk | Mitigation |
|---|---|
| Scope creep on a solo project | Phase gates, small vertical slices, time-boxed weeks |
| False claims or fraud | Hidden-detail verification, claim limits, reporting and moderation |
| Personal data in photos (ID cards, faces, addresses) | Warn at upload, keep exact details private, plan for blur/redaction |
| Physical safety at handover | Suggest public places or police stations, never share an address automatically |
| Free tiers change or end | Infrastructure as code, so it can move to another host. Keep backups outside the host |
| Self-hosted AI is too slow on free hardware | Run it asynchronously, use a small model, keep the provider swappable, fall back to rule-based matching |

Detailed requirements: [requirements.md](requirements.md).
