# Plan: Create Campaign Master Plan Documents

## Overview

Create two comprehensive master plan documents for the Kernel-Panic campaign based on `00_MASTER_CAMPAIGN_STRUCTURE.md`:
1. **01_LORE_STORY_PLAN.md** - Complete narrative content bible (dialogue, endings, emails, faction voices)
2. **02_MISSION_CREATION_PLAN.md** - Systematic mission production workflow (templates, checklists, testing)

## Context from Codebase Exploration

**Existing Infrastructure:**
- ✅ Well-defined mission JSON schema (mission_meta, briefing, nodes, win_conditions, guide)
- ✅ Faction persona system in `PersonaConfig.ts` (architect, nullpack, circle, cipher-zero, aegis-sec, glitch-hunters)
- ✅ Tier-based reward system in `gameConfig.ts` (Tiers 1-10 with credit/reputation ranges)
- ✅ Mission loading service (`MissionLoaderService.ts`) for JSON-to-TypeScript mapping
- ✅ Existing tutorial missions demonstrating guide system and Architect dialogue

**Campaign Blueprint (from 00_MASTER_CAMPAIGN_STRUCTURE.md):**
- 5 Arcs spanning Tiers 1-10 (Levels 1-100+)
- 95+ story missions + procedural contracts
- 12-step component mystery escalation (very subtle → obvious reveal)
- Architect's 4-phase persona evolution (mentor → teacher → broken creator → dying legacy)
- 6 factions with unique voices and philosophies
- 7 unique endings with specific prerequisites
- All story beats documented by mission ID (T1-01 through T10-01)

---

## Part 1: Create `01_LORE_STORY_PLAN.md`

### Document Structure (10 Parts, 17 Chapters)

**Part I: Character Voice Libraries** (Chapters 1-3)
- Chapter 1: The Architect's Complete Dialogue System
  - Phase 1 (Arcs 1-2): "The Mysterious Mentor" - playful, "nya~" verbal tic, encouraging
  - Phase 2 (Arc 3): "The Demanding Teacher" - strict, critical, exhausted
  - Phase 3 (Arc 4): "The Broken Creator" - drops "nya~", confessional, ashamed
  - Phase 4 (Arc 5): "The Dying Legacy" - tired, ancient, proud, at peace
  - Special: Component Ghost (young Architect from 1826)

- Chapter 2: Oracle Encounter Scripts
  - First Contact (T4-01): "I KNOW THAT FREQUENCY"
  - Direct Communication (T5-03): Utopia tour, philosophy reveal
  - Final Confrontations (T8-03, T9-03): Last offers

- Chapter 3: Faction Communication Templates
  - All 6 factions (NullPack, Circle, Cipher-Zero, Aegis-Sec, Glitch-Hunters, Oracle)
  - Email templates, mission briefings, reputation responses
  - Character-specific dialogue (ZeroCool, Vesper, Entropy, Handler-7, Buffer)

**Part II: The Component Mystery** (Chapter 4-7)
- Chapter 4: Arc 1 Clues (Very Subtle) - T1-01, T1-03, T2-01, T2-04
- Chapter 5: Arc 2 Clues (Less Subtle) - T3-01, T3-04, T4-01, T4-03
- Chapter 6: Arc 3 Clues (Getting Obvious) - T5-01, T5-03, T5-05, T5-06
- Chapter 7: Arc 4 Clue (The Reveal) - T7-06 THE REVEAL MISSION

**Part III: Email Chain Library** (Chapter 8)
- Faction-specific email templates for all 5 arcs
- Story progression emails (Oracle warnings, Candidate #1-#12 lore)
- Reputation response scripts (hostile, neutral, friendly)

**Part IV: Ending Scripts** (Chapter 9)
- All 7 endings with complete dialogue:
  1. Digital Requiem (Destroy Oracle)
  2. New Dawn (Free Oracle)
  3. Total Control (Villain)
  4. Transcendence (Merge - Secret)
  5. Null Protocol (Destroy Everything)
  6. Architect's Heir (Architect Lives - Good)
  7. The Birthing (True Canon - All secrets)

**Part V: Character Evolution Guides** (Chapters 10-12)
- The Architect's Complete Journey (phase transitions, emotional arcs)
- Player's Internal Journey (novice → professional → survivor → leader → legend)
- Oracle Evolution (threat → sympathetic antagonist)

**Part VI: Environmental Storytelling** (Chapter 13)
- Email format standards (headers, timestamps, attachments)
- Log file templates (system, auth, application, Oracle detection)
- Document templates (corporate memos, research papers, manifestos)
- Journal templates (Architect's 1826 journal, Candidate final logs)

**Part VII: Mission-Specific Content** (Chapter 14)
- Story beat scripts for all 95+ missions (T1-01 through T10-01)
- Architect overlay scripts for tutorial missions
- Environmental file content for each mission
- Win condition scripts, failure responses

**Part VIII: Dialogue Trees** (Chapter 15)
- Major decision point scripts:
  - T3-07: Faction alliance choice
  - T4-05: Betrayal response
  - T5-03: Oracle alliance
  - T7-05: True goal choice
  - T8-04: Architect's fate
  - T9-04: Final choice

**Part IX: Quality Standards** (Chapter 16)
- Voice consistency guidelines (faction rules, Architect persona rules)
- Pacing guidelines (clue escalation, emotional beat timing)
- Quality checklists (pre-content, during creation, pre-integration)

**Part X: Reference Materials** (Chapter 17 + Appendices)
- Quick reference tables (factions, characters, timeline)
- Mission ID cross-reference
- Character name index
- Content tracking spreadsheet

### Key Content to Include

**Email Format Example:**
```
From: [Sender] <[faction]@[domain].net>
To: [Recipient]
Subject: [Action-oriented summary]
Date: [Timestamp]
Attachments: [optional]

[Body with faction-specific voice]
```

**Log File Format:**
```
[YYYY-MM-DD HH:MM:SS] [LEVEL] [System/Process] Message
[realistic technical content]
[lived-in details: typos, incomplete entries]
```

**Document Format:**
```
[Header/Title]
[Author/Department]
[Date]

[Body with appropriate tone]
[Realistic boilerplate, no "TODO"]
```

---

## Part 2: Create `02_MISSION_CREATION_PLAN.md`

### Document Structure (11 Parts)

**Part 1: Mission Type Taxonomy** (5 mission types)
- Tutorial Missions (Arc 1, 40% of arc)
- Faction Introduction Missions (showcase philosophy)
- Story Progression Missions (advance narrative, deliver clues)
- Faction Storyline Missions (deepen relationships)
- Climax Missions (major arc turning points)

**Part 2: File Content Standards**
- Email formatting (faction voice guides with examples)
- Log file standards (system, auth, application formats)
- Document standards (corporate memos, technical specs, journals)
- Quality rules (timestamps, technical accuracy, lived-in details)

**Part 3: Arc-by-Arc Production Schedule** (25-26 weeks total)
- Week 1-6: Arc 1 (15 missions) - HIGH PRIORITY (foundation)
- Week 7-11: Arc 2 (20 missions) - HIGH PRIORITY (systems)
- Week 12-17: Arc 3 (25 missions) - MEDIUM (experienced players)
- Week 18-22: Arc 4 (20 missions) - MEDIUM (narrative complexity)
- Week 23-26: Arc 5 (15 missions) - LOW (polish and payoff)

**Part 4: Quality Checklist System**
- Mission JSON validation (structure, nodes, win conditions, guide)
- Narrative quality (story integration, character voice, pacing)
- Gameplay quality (difficulty, mechanics, rewards)
- Technical quality (performance, compatibility, edge cases)

**Part 5: Testing Procedures**
- Unit testing (per mission): automated validation + manual testing
- Integration testing (per arc): sequential flow, branching paths
- Full campaign testing: complete playthroughs, ending testing
- Regression testing: replay previous arcs after each completion

**Part 6: Integration with Core Services**
- CampaignState.ts integration (story flags, arc progression)
- FactionReputationService.ts integration (reputation rewards, alliances)
- EndingService.ts integration (choice tracking, prerequisite calculation)

**Part 7: Step-by-Step Creation Process**
1. Pre-creation planning (review mission place in arc, identify type)
2. Create mission JSON (fill fields, write briefing, design nodes)
3. Write file content (logs, documents, emails with faction voices)
4. Validate mission (JSON validation, quality checklist, manual test)
5. Integrate with campaign (add to index, configure prerequisites)

**Part 8: Production Tracking**
- Mission production spreadsheet (status tracking)
- Story flag tracking (flags per arc)
- Content completion tracking

**Part 9: Common Pitfalls & Solutions**
- VFS structure pitfalls (arrays vs Record, absolute paths)
- Narrative pitfalls (obvious clues, breaking character)
- Gameplay pitfalls (tutorial fatigue, difficulty spikes, unfair consequences)

**Part 10: Automation & Tooling**
- Mission generation scripts (template-based)
- Validation scripts (JSON, references, flags)
- Content asset libraries (reusable file components)

**Part 11: Final Production Checklist**
- Pre-launch checklist (structure, narrative, gameplay, technical, polish)

### Mission Templates

**Standard Template Fields:**
```json
{
  "mission_meta": {
    "id": "t{tier}_{sequence}_{mission_slug}",
    "title": "Mission Title",
    "category": "story_arc_{arc_number}",
    "is_sequential": true,
    "order": 1,
    "prerequisites": ["previous_mission_id"],
    "difficulty": 1,
    "reward_credits": 100,
    "reward_reputation": 50,
    "client": "Faction Name",
    "description": "One-line mission brief",
    "is_mandatory": true,
    "story_flags": {
      "sets": ["flag_to_set"],
      "requires": ["flag_needed"]
    }
  },
  "briefing": {
    "email_subject": "Action-oriented subject",
    "email_body": "Detailed brief with faction voice",
    "sender": "faction@domain.net"
  },
  "nodes": [...],
  "win_conditions": {...},
  "guide": [...],
  "reputation_rewards": [
    {"faction": "nullpack", "amount": 10}
  ],
  "architect_persona": "mentor",
  "files": [...]
}
```

### Critical VFS Rules (from AGENT.md)

⚠️ **CRITICAL:** Directory children MUST be `Record<string, VFSNode>`, NEVER arrays:
```typescript
// CORRECT
children: { 'hosts': { type: 'file', ... } }

// WRONG - This will crash!
children: [ { name: 'hosts', ... } ]
```

⚠️ **CRITICAL:** Always use absolute paths, reset `currentDirectory` on node switch

---

## Implementation Steps

### Step 1: Create Directory Structure
```bash
# Directory already exists:
game/plans/CAMPAIGN_WIP/

# Files to create:
game/plans/CAMPAIGN_WIP/01_LORE_STORY_PLAN.md
game/plans/CAMPAIGN_WIP/02_MISSION_CREATION_PLAN.md
```

### Step 2: Create `01_LORE_STORY_PLAN.md`

Using the comprehensive design from the Plan agent, create a markdown document organized into:
- 10 Parts (Character Voices → Component Mystery → Emails → Endings → Evolution → Environmental → Mission Content → Dialogue → Quality → Reference)
- 17 Chapters with detailed subsections
- Appendices for cross-references and tracking

**Key sections to populate with actual content:**
1. All 12 component mystery clues with full reveals
2. All 7 endings with complete dialogue and post-credits scenes
3. Architect dialogue examples for all 4 phases
4. Faction email templates for all 5 arcs (6 factions × 5 arcs = 30+ template sets)
5. Oracle encounter scripts (T4-01, T5-03, T8-03, T9-03)

### Step 3: Create `02_MISSION_CREATION_PLAN.md`

Using the systematic workflow design from the Plan agent, create a markdown document organized into:
- 11 Parts (Taxonomy → Standards → Schedule → Quality → Testing → Integration → Creation Process → Tracking → Pitfalls → Automation → Checklist)
- Detailed workflow with 25-26 week production schedule
- Mission templates for each type (tutorial, faction, story, climax)
- Quality checklists for validation
- Testing procedures (unit, integration, campaign)
- Integration guidelines with CampaignState.ts, FactionReputationService.ts, EndingService.ts

**Key sections to include:**
1. Mission type taxonomy with template JSONs
2. File content formatting standards (emails, logs, documents)
3. Arc-by-arc production schedule with weekly breakdowns
4. Comprehensive quality checklist system
5. Testing procedures (automated + manual)
6. Service integration code examples
7. Step-by-step creation process
8. Production tracking spreadsheet structure

### Step 4: Cross-Reference with Master Document

Ensure both documents reference `00_MASTER_CAMPAIGN_STRUCTURE.md` as the source of truth:
- All story beats should map to mission IDs (T1-01 through T10-01)
- All 12 clues should map to specific missions
- All 7 endings should include prerequisite requirements
- Architect persona phases should map to arcs
- Faction reputation rewards should align with gameConfig.ts ranges

---

## Critical Files Referenced

**Source Documents:**
- `game/plans/CAMPAIGN_WIP/00_MASTER_CAMPAIGN_STRUCTURE.md` - Campaign blueprint (READ)
- `game/DOCUMENTATION/LORE/character_sheets.md` - Faction/character profiles (READ)
- `game/DOCUMENTATION/LORE/Operators_Laptop_Created_Omega_exe.md` - Component mystery concept (READ)

**Configuration Files:**
- `game/data/PersonaConfig.ts` - Faction personas with email addresses and themes (READ)
- `game/data/gameConfig.ts` - Tier reward ranges and mission configuration (READ)

**Service Files (for integration):**
- `game/services/MissionLoaderService.ts` - Mission loading and JSON mapping (EXPLORED)
- `game/services/CampaignState.ts` - TO BE CREATED (story flags, arc progression)
- `game/services/FactionReputationService.ts` - TO BE CREATED (faction relationships)
- `game/services/EndingService.ts` - TO BE CREATED (ending prerequisites)

**Example Mission:**
- `game/public/missions/tutorials_basic/tutorial_linux_basics_001.json` - Example structure (EXPLORED)

---

## Success Criteria

**For 01_LORE_STORY_PLAN.md:**
✅ All 6 faction voices documented with examples
✅ Architect's 4 phases fully characterized with dialogue samples
✅ All 12 component mystery clues documented with reveals
✅ All 7 endings with complete dialogue and post-credits scenes
✅ Email templates for all factions across all 5 arcs
✅ Oracle encounter scripts for all encounters
✅ Character evolution guides for all major characters
✅ File content formatting standards (emails, logs, documents)

**For 02_MISSION_CREATION_PLAN.md:**
✅ Mission type taxonomy with 5 types and templates
✅ File content standards (emails, logs, documents)
✅ Arc-by-arc production schedule (25-26 weeks)
✅ Comprehensive quality checklist system
✅ Testing procedures (unit, integration, campaign)
✅ Service integration guidelines (CampaignState, FactionReputation, Ending)
✅ Step-by-step creation process
✅ Production tracking system
✅ Common pitfalls and solutions

**Both Documents:**
✅ Reference `00_MASTER_CAMPAIGN_STRUCTURE.md` as source of truth
✅ Include examples from existing codebase (PersonaConfig.ts, gameConfig.ts)
✅ Provide clear, actionable guidance for content creation
✅ Cross-reference each other (lore plan informs mission creation)
✅ Maintain consistency with existing systems (VFS structure, mission loading)

---

## Verification

After creating both documents:

1. **Check that all story beats are covered:**
   - Review 00_MASTER_CAMPAIGN_STRUCTURE.md mission list (T1-01 through T10-01)
   - Verify 01_LORE_STORY_PLAN.md has content for all major story beats
   - Verify 02_MISSION_CREATION_PLAN.md has workflow for all mission types

2. **Check that all characters are represented:**
   - Architect (4 phases)
   - Oracle (encounters)
   - 6 factions (NullPack, Circle, Cipher-Zero, Aegis-Sec, Glitch-Hunters, Oracle)
   - All named NPCs (ZeroCool, Vesper, Entropy, Handler-7, Buffer)

3. **Check that all endings are achievable:**
   - Review prerequisite matrix for each ending
   - Verify lore plan has all dialogue for each ending
   - Verify mission plan has workflow to implement prerequisites

4. **Check integration with existing systems:**
   - PersonaConfig.ts faction IDs match lore plan
   - gameConfig.ts reward ranges match mission plan
   - VFS structure rules (from AGENT.md) are documented in mission plan
   - MissionLoaderService.ts patterns are referenced

---

## Notes

- Both documents are **planning/reference documents**, not implementation
- They will be used during actual mission creation to populate mission JSONs
- 01_LORE_STORY_PLAN.md is the "narrative bible" - all story content lives here
- 02_MISSION_CREATION_PLAN.md is the "production manual" - all workflows live here
- Together with 00_MASTER_CAMPAIGN_STRUCTURE.md, they form the complete campaign blueprint
- Documents should be detailed enough that any content creator can follow them independently
- Include plenty of examples from existing codebase to show patterns
- Maintain markdown formatting for readability and version control
