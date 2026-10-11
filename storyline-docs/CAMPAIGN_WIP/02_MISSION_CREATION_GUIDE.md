# 02 MISSION CREATION PLAN
## Systematic Mission Production Workflow

**Purpose:** This document is the production manual for creating all Kernel-Panic campaign missions. It provides templates, checklists, and workflows to ensure consistent, high-quality mission content.

**Source of Truth:** [`00_MASTER_CAMPAIGN_STRUCTURE.md`](./00_MASTER_CAMPAIGN_STRUCTURE.md)
**Companion Document:** [`01_LORE_STORY_PLAN.md`](./01_LORE_STORY_PLAN.md) (narrative content)

**Last Updated:** 2025-02-14
**Status:** Active - Updated for current mission system

## Recent Updates (2025-02-14)

**Mission JSON Structure Changes:**
- Updated to reflect current MissionLoaderService.ts implementation
- `node_id` maps to `NetworkNode.id` (property name change handled internally)
- `ip_address` maps to `NetworkNode.ip` (property name change handled internally)
- VFS structure uses `Record<string, VFSNode>` for directory children (CRITICAL - never use arrays)
- Objective types use lowercase in JSON, normalized to uppercase enums internally

**Script Events System:**
- Added new simplified dialogue format (recommended)
- Dialogue: `{ "speaker": "architect", "text": "Message" }`
- FX: `{ "fx": "effect_id" }`
- Oracle: `{ "speaker": "oracle", "text": "MESSAGE", "phase": "menacing" }`
- Faction: `{ "speaker": "faction", "faction": "nullpack", "speakerName": "ZeroCool", "text": "Message" }`
- Legacy format still supported for backward compatibility

**Win Condition Updates:**
- Added `node_connected` objective type (replaces legacy `network_pivot`)
- Added `player_choice` objective type for auto-triggered choice modals
- Added `command_executed` objective type for command completion tracking
- Updated all objective type references to match current implementation
- MissionLoaderService handles lowercase-to-uppercase normalization

**VFS Structure Rules:**
- Emphasized CRITICAL rule: Directory `children` MUST be objects, never arrays
- Added examples of correct vs incorrect VFS structure
- Documented automatic FHS structure creation by MissionLoaderService
- Added path resolution rules (absolute paths required)

---

## Table of Contents

1. [Part 1: Mission Type Taxonomy](#part-1-mission-type-taxonomy)
2. [Part 2: File Content Standards](#part-2-file-content-standards)
3. [Part 3: Arc-by-Arc Production Schedule](#part-3-arc-by-arc-production-schedule)
4. [Part 4: Quality Checklist System](#part-4-quality-checklist-system)
5. [Part 5: Testing Procedures](#part-5-testing-procedures)
6. [Part 6: Integration with Core Services](#part-6-integration-with-core-services)
7. [Part 7: Step-by-Step Creation Process](#part-7-step-by-step-creation-process)
8. [Part 8: Production Tracking](#part-8-production-tracking)
9. [Part 9: Common Pitfalls & Solutions](#part-9-common-pitfalls--solutions)
10. [Part 10: Automation & Tooling](#part-10-automation--tooling)
11. [Part 11: Final Production Checklist](#part-11-final-production-checklist)

---

## Part 1: Mission Type Taxonomy

All campaign missions fall into one of five types. Each type has specific structural requirements and pacing guidelines.

### Mission JSON Structure Overview

All mission JSON files follow this core structure:

```json
{
  "mission_meta": {
    "id": "t1_01_example_mission",
    "title": "Mission Title: Subtitle",
    "category": "arc_01",
    "is_sequential": true,
    "order": 1,
    "prerequisites": [],
    "difficulty": 1,
    "reward_credits": 200,
    "reward_reputation": 75,
    "client": "The Architect",
    "is_mandatory": true,
    "architect_persona": "mentor",
    "description": "One-line mission brief",
    "story_flags": {
      "sets": ["flag_to_set"],
      "requires": ["flag_needed"]
    },
    "reputation_rewards": []
  },
  "briefing": {
    "email_subject": "Action-oriented subject line",
    "email_body": "Mission brief with faction voice",
    "sender": "faction@domain.net"
  },
  "nodes": [
    {
      "node_id": "homebase",
      "hostname": "kitten-terminal",
      "ip_address": "127.0.0.1",
      "ssh_password": "",
      "files": { /* VFS structure */ }
    }
  ],
  "guide": [
    { "id": "step1", "message": "Instruction text", "trigger": "COMMAND_NAME", "criteria": "ls" }
  ],
  "win_conditions": {
    "is_sequential": true,
    "sequence": [ /* array of objectives */ ]
  },
  "script_events": {
    "on_start": [],
    "on_objective_complete": { "0": [], "1": [] },
    "on_complete": []
  }
}
```

**CRITICAL: VFS Structure Rules**
- Directory `children` MUST be `Record<string, VFSNode>` (object), NEVER arrays
- Example: `"children": { "file.txt": { "type": "file", "name": "file.txt" } }`
- All file paths must be absolute (start with `/`)
- Node IDs must be unique and referenced correctly in objectives

### 1. Tutorial Missions

**Purpose:** Teach core mechanics while advancing narrative
**Distribution:** ~40% of Arc 1, tapering to 0% by Arc 3
**Characteristics:**
- Heavy Architect overlay guidance (`guide` array)
- Small network (1-2 nodes)
- Low complexity objectives
- Explicit step-by-step instructions
- Forgiving difficulty

**Template Pattern:**
```json
{
  "mission_meta": {
    "id": "t1_{sequence}_{slug}",
    "category": "arc_01",
    "is_sequential": true,
    "order": 1,
    "difficulty": 1,
    "is_mandatory": true,
    "architect_persona": "mentor",
    "description": "Learn basics of terminal navigation"
  },
  "guide": [
    {
      "id": "step1",
      "message": "Type 'ls' to see directory contents",
      "trigger": "COMMAND_NAME",
      "criteria": "ls",
      "allow_continue": false
    }
  ],
  "nodes": [{ /* 1-2 simple nodes */ }],
  "win_conditions": {
    "is_sequential": true,
    "sequence": [
      {
        "type": "command_executed",
        "command": "ls",
        "description": "List directory contents"
      }
    ]
  }
}
```

**Available Objective Types:**
- `command_executed` - Player executes specific command
- `file_read` - Player reads file with `cat`
- `file_download` - Player downloads file with `download`
- `file_delete` - Player deletes file with `rm`
- `node_connected` - Player connects to node via `ssh`
- `directory_navigation` - Player navigates to specific directory
- `content_match` - Player modifies file content to match required value
- `player_choice` - Auto-triggers choice modal

**Examples:**
- `t1_01_hello_world` - Linux basics
- `t1_02_curiosity_killed_cat` - File discovery
- `t1_03_sticky_note` - Credential harvesting
- `t1_06_job_board` - Browser tutorial

### 2. Faction Introduction Missions

**Purpose:** Showcase faction philosophy and initial reputation earning
**Distribution:** 6 missions (one per faction) in Arc 1
**Characteristics:**
- Faction-specific email voice
- Moral choice embedded in objectives
- Reputation rewards for target faction
- Small-medium network (2-3 nodes)
- Introduces faction-themed mechanics

**Template Pattern:**
```json
{
  "mission_meta": {
    "id": "t1_{07-12}_{faction_slug}",
    "category": "arc_01",
    "is_sequential": false,
    "order": 7,
    "prerequisites": ["t1_06_job_board"],
    "difficulty": 1,
    "reward_credits": 300,
    "reward_reputation": 150,
    "client": "NullPack",
    "is_mandatory": false,
    "architect_persona": "mentor",
    "description": "Sabotage corporate server"
  },
  "briefing": {
    "email_subject": "Action-oriented subject",
    "email_body": "Faction-voiced brief with philosophy embedded",
    "sender": "nullpack@retro-sim.net"
  },
  "nodes": [
    {
      "node_id": "homebase",
      "hostname": "kitten-terminal",
      "ip_address": "127.0.0.1",
      "ssh_password": "",
      "files": { /* VFS structure */ }
    },
    {
      "node_id": "target_server",
      "hostname": "corp-target",
      "ip_address": "10.0.1.10",
      "ssh_password": "password123",
      "files": { /* Target files */ }
    }
  ],
  "win_conditions": {
    "is_sequential": false,
    "sequence": [
      { "type": "file_download", "target_node": "target_server", "target_path": "/data/blueprints.pdf" },
      { "type": "content_match", "required_content": "choice_option", "target_node": "target_server", "target_path": "/tmp/choice.txt" }
    ]
  },
  "script_events": {
    "on_complete": [
      { "speaker": "faction", "faction": "nullpack", "speakerName": "ZeroCool", "text": "Nice work, rookie." }
    ]
  }
}
```

**Faction Voice Examples:**

**NullPack** (anarchist, passionate):
```
Subject: BURN IT DOWN

yo. we found something juicy. corp x is sitting on a dirt sheet that would
DESKYRO... DESTROY them if it went public.

we can either:
A) leak it to the press (chaos, public good)
B) sell it back to them (profit, but they learn nothing)

your call. the payload is on the server.

stay frosty.
- ZeroCool
```

**The Circle** (corporate, polite, predatory):
```
Subject: Consultancy Opportunity: Project Chimera

Greetings,

Your recent activities have come to our attention. While technically
unsanctioned, your efficiency suggests... potential.

We require acquisition of competitor R&D documentation. Compensation:
750 credits, with potential for ongoing contract work.

Target specifications attached.

We look forward to a productive arrangement.

Regards,
Vesper
The Circle Acquisition Division
```

**Cipher-Zero** (privacy absolutist, technical):
```
Subject: ILLEGAL SURVEILLANCE EXPOSE

Entropy here. Our monitoring division intercepted something disturbing.

Aegis-Sec is operating an illegal domestic surveillance program.
They're tracking innocent citizens. This cannot stand.

Target: aegis-surveillance-01.local
Objective: Expose the database, expose the truth.
Reward: Our gratitude + 600 credits

Privacy is not a privilege. It is a right.

Encrypt,
Entropy
```

**Aegis-Sec** (mercenary, professional, transactional):
```
Subject: Contract: PEN-TEST Opportunity #4419

Operative,

Handler-7 here. We have a penetration test contract available.

Client: First National Bank
Scope: External perimeter assessment
Compensation: 1000 credits
Terms: Full disclosure of findings only

Standard Aegis engagement rules apply. No destruction. No exfiltration
beyond proof of access.

Accept via completing the attached pre-mission questionnaire.

H7 out.
```

**Glitch-Hunters** (academic, obsessive, curious):
```
Subject: INTERESTING VULNERABILITY DETECTED

hey hey!! buffer here :3

so i was poking around this old industrial control system and found
THE MOST INTERESTING buffer overflow in the firmware update service!!

like, it's SUPER old and probably nobody uses it but it's SO COOL
how the return address overwrite works through a malformed packet

anyway i need someone to document it properly for the vuln database.
you in?? :3

target details in attachment!
```

### 3. Story Progression Missions

**Purpose:** Advance main narrative, deliver mystery clues
**Distribution:** ~60% of Arc 2-4 missions
**Characteristics:**
- Mandatory (`is_mandatory: true`)
- Sets story flags
- Contains environmental file content with lore
- Architect persona evolution
- Medium complexity (3-5 nodes)

**Template Pattern:**
```json
{
  "mission_meta": {
    "id": "t3_{sequence}_{slug}",
    "category": "arc_02",
    "is_sequential": true,
    "order": 1,
    "prerequisites": ["t2_05_whispers_of_omega"],
    "difficulty": 3,
    "reward_credits": 3500,
    "reward_reputation": 2500,
    "client": "The Architect",
    "is_mandatory": true,
    "architect_persona": "teacher",
    "description": "Investigate Oracle detection",
    "story_flags": {
      "sets": ["learned_about_oracle"],
      "requires": ["met_architect"]
    }
  },
  "briefing": {
    "email_subject": "Strange Signals",
    "email_body": "Oracle activity detected...",
    "sender": "architect@retro-sim.net"
  },
  "nodes": [
    {
      "node_id": "target_server",
      "hostname": "oracle-monitor",
      "ip_address": "10.0.3.50",
      "ssh_password": "password",
      "files": {
        "var": {
          "type": "directory",
          "name": "var",
          "children": {
            "log": {
              "type": "directory",
              "name": "log",
              "children": {
                "syslog": {
                  "type": "file",
                  "name": "syslog",
                  "content": "[2025-03-15 10:02:44] [ERROR] [oracle_monitor] Unknown frequency detected. Pattern: UNKNOWN_RES"
                }
              }
            }
          }
        }
      }
    }
  }
  ],
  "win_conditions": {
    "is_sequential": true,
    "sequence": [
      { "type": "file_read", "target_node": "target_server", "target_path": "/var/log/syslog" }
    ]
  },
  "script_events": {
    "on_start": [
      { "speaker": "architect", "text": "This mission is critical. Oracle's getting closer." }
    ]
  }
}
```

**Environmental File Content Types:**

| Type | Purpose | Format |
|------|---------|--------|
| Email | Faction communications, story breadcrumbs | Standard email format |
| Log | System events, Oracle detection, trails | `[timestamp] [level] [process] message` |
| Document | Corporate memos, research papers, manifestos | Header/Author/Date + body |
| Journal | Character logs, Candidate final entries | Dated entries, emotional content |

### 4. Faction Storyline Missions

**Purpose:** Deepen faction relationships, unlock special content
**Distribution:** Variable based on player alliance choice
**Characteristics:**
- Reputation-gated (requires specific faction rep level)
- Branching availability based on T3-07 choice
- Faction-specific objectives
- Special rewards (software, exploits, blueprints)

**Reputation Thresholds:**
```typescript
const FACTION_ACCESS_LEVELS = {
  // All missions available
  neutral: -20 to 20,
  // Discount/bonus missions unlock
  friendly: 20 to 50,
  // Special storyline missions unlock
  very_friendly: 50 to 80,
  // Faction exclusives, secret missions
  devoted: 80 to 100,
  // Missions blocked, may send assassins
  hostile: -50 to -20,
  very_hostile: -100 to -50
};
```

### 5. Climax Missions

**Purpose:** Major arc turning points, emotional peaks
**Distribution:** 1-2 per arc (end of arc)
**Characteristics:**
- Mandatory story beats
- Multi-phase objectives
- Significant narrative consequences
- Architect persona shifts
- High reward tiers

**Arc Climax Template:**
```json
{
  "mission_meta": {
    "id": "t2_05_whispers_of_omega",
    "category": "arc_01",
    "is_sequential": true,
    "order": 5,
    "prerequisites": ["t2_04_first_signature"],
    "difficulty": 2,
    "reward_credits": 1200,
    "reward_reputation": 1000,
    "client": "The Architect",
    "is_mandatory": true,
    "architect_persona": "mentor",
    "description": "Major revelation about Oracle"
  },
  "win_conditions": {
    "is_sequential": true,
    "sequence": [
      { "type": "node_connected", "target_node": "phase_1_gateway", "description": "Connect to gateway" },
      { "type": "file_download", "target_node": "phase_1_gateway", "target_path": "/intel/key_data" },
      { "type": "node_connected", "target_node": "phase_2_core", "description": "Breach core system" },
      { "type": "content_match", "required_content": "clue_reveal", "target_node": "phase_2_core", "target_path": "/secret/revelation.txt" }
    ]
  },
  "script_events": {
    "on_complete": [
      { "fx": "oracle_reveal" }
    ]
  }
}
```

**Note:** Script events support new simplified format:
- Dialogue: `{ "speaker": "architect", "text": "Message" }`
- FX: `{ "fx": "effect_id" }`
- Oracle: `{ "speaker": "oracle", "text": "MESSAGE", "phase": "menacing" }`
- Faction: `{ "speaker": "faction", "faction": "nullpack", "speakerName": "ZeroCool", "text": "Message" }`

---

## Part 2: File Content Standards

All environmental storytelling files must follow these formatting standards to maintain immersion and consistency.

### Email Format

**Standard Template:**
```
From: [Sender Name] <[faction]@[domain].net>
To: [Recipient]
Subject: [Action-oriented summary]
Date: [YYYY-MM-DD HH:MM:SS +TZ]
Attachments: [optional: filename.ext (size)]

[Body with faction-specific voice and appropriate sign-off]
```

**Date Formatting Rules:**
- Use ISO 8601 format: `2025-03-15 14:23:11 -0500`
- Timeline: Campaign starts in 2025, progresses over ~2 years
- Timestamps should be realistic for mission tier (older logs for higher tiers)

**Domain Patterns by Faction:**
| Faction | Domain Pattern | Example |
|---------|---------------|---------|
| Architect | `@retro-sim.net` | `architect@retro-sim.net` |
| NullPack | `@retro-sim.net` | `nullpack@retro-sim.net` |
| The Circle | `@the-circle.io` | `vesper@the-circle.io` |
| Cipher-Zero | `@c0.onion` | `entropy@c0.onion` |
| Aegis-Sec | `@aegis-sec.com` | `h7@aegis-sec.com` |
| Glitch-Hunters | `@gh.labs` | `buffer@gh.labs` |
| System | `@localhost` | `admin@localhost` |

### Log File Format

**System Log Template:**
```
[YYYY-MM-DD HH:MM:SS] [LEVEL] [System/Process] Message
[technical details, realistic boilerplate]
[lived-in details: occasional typos, incomplete entries]
```

**Log Levels:** `DEBUG`, `INFO`, `WARN`, `ERROR`, `CRITICAL`

**Example System Log:**
```
[2025-03-15 09:14:22] [INFO] [ssh_server] Connection accepted from 10.0.1.50:54321
[2025-03-15 09:14:23] [INFO] [auth] User 'trainee' authenticated from 10.0.1.50
[2025-03-15 09:14:25] [DEBUG] [ssh_server] Session ID: SSH-8X7Q-Q9Z2
[2025-03-15 09:22:17] [WARN] [auth] Failed root login attempt from 10.0.1.50
[2025-03-15 09:45:01] [INFO] [cron] Daily backup completed: backup_20250315.tar.gz
[2025-03-15 10:02:44] [ERROR] [oracle_monitor] Unknown frequency detected. Pattern: UNKNOWN_RES
[2025-03-15 10:02:45] [CRITICAL] [oracle_monitor] ALERT: Possible birthing component signature.
```

**Auth Log Template:**
```
[YYYY-MM-DD HH:MM:SS] [hostname] [service]: [action] [details]
```

**Example Auth Log:**
```
[2025-03-15 14:23:11] training-basic sshd[1234]: Accepted password for trainee from 10.0.1.50 port 54321 ssh2
[2025-03-15 14:25:33] training-basic sudo[1235]: trainee : TTY=pts/0 ; PWD=/home/trainee ; USER=root ; COMMAND=/bin/cat
[2025-03-15 14:30:01] training-base CRON[1236]: (root) CMD (run-parts /etc/cron.hourly)
```

### Document Format

**Corporate Memo Template:**
```
[COMPANY LETTERHEAD]
Internal Memorandum

To: [Department/Recipient]
From: [Author]
Date: [YYYY-MM-DD]
Subject: [Subject Line]

[Body - corporate speak, some jargon]
[Realistic boilerplate, disclaimers]

[Author signature]
[Additional recipients]
```

**Example Corporate Memo:**
```
OMNICORP INTERNAL // CONFIDENTIAL
===============================

To: Dr. Sarah Chen, R&D Division
From: Marcus Webb, VP of Operations
Date: 2025-01-22
Subject: RE: Project Chimera Timeline Acceleration

Sarah,

Per Board directive, Chimera deployment timeline has been accelerated
by 18 months. I understand your concerns about the neural integration
protocols, but Oracle has assured us that the risk projections are
within acceptable parameters.

Please have the revised integration specs on my desk by EOD Friday.
The consultants will need them for the deployment phase.

Regards,

Marcus Webb
Vice President, Operations
OMNICORP Global

cc: Executive Committee
cc: Oracle Liaison Office
CONFIDENTIALITY NOTICE: This message contains proprietary information.
Unauthorized distribution or disclosure is prohibited.
```

**Research Paper Template:**
```
[Title]
[Authors/Institution]
[Date]

Abstract
[Brief summary]

1. Introduction
[Background, problem statement]

2. Methodology
[Technical approach]

3. Results
[Findings, data]

4. Discussion
[Analysis, implications]

5. Conclusion
[Summary, future work]

References
[...]
```

**Journal Template:**
```
[Date - could be approximate for character journals]

[Entry content - first-person, emotional, lived-in]
[Casual language, personal reflections]
```

**Example Candidate Journal:**
```
Day 47

I don't know how many more of these I can write.

The dreams are getting worse. Every time I close my eyes, I see it--
the code. It's beautiful and terrible all at once. And it's CHANGING.
Every time I run the simulation, it's different. Like it's... learning.

The Architect says I'm making progress. She says I'm "special."
She says I'm the closest anyone has ever come.

But sometimes I look at her and I wonder if she even knows what
she created. Sometimes I wonder if she's scared of it too.

Tomorrow I try the full integration. If this works, everything changes.
If it doesn't...

Well. There's a reason I'm Candidate #12.

- J
```

### Quality Rules

1. **Lived-in Details:**
   - Occasional typos in informal communications
   - Incomplete log entries (crashes, interruptions)
   - Realistic clutter in directories
   - Mixed case and formatting

2. **Technical Accuracy:**
   - Proper command syntax in logs
   - Realistic IP addresses (use private ranges: 10.x.x.x, 192.168.x.x)
   - Believable timestamps relative to mission date
   - Appropriate technical jargon for context

3. **World-Building:**
   - References to events outside current mission
   - Faction presence in environmental content
   - Consistent naming conventions
   - Layered revelations (surface meaning + deeper meaning)

4. **No TODO Markers:**
   - Never include "[TODO]" or similar placeholders
   - All content must feel complete
   - Use ellipses (...) or truncation for incomplete content

---

## Part 3: Arc-by-Arc Production Schedule

### Production Timeline Overview

| Arc | Weeks | Mission Count | Priority | Focus |
|-----|-------|--------------|----------|-------|
| 1 | 1-6 | 15 | HIGH | Tutorial foundation, faction introductions |
| 2 | 7-11 | 20 | HIGH | Systems integration, player choice |
| 3 | 12-17 | 25 | MEDIUM | Complex narratives, experienced players |
| 4 | 18-22 | 20 | MEDIUM | Deep lore, emotional beats |
| 5 | 23-26 | 15 | LOW | Endgame polish, payoff |

### Arc 1: The Kitten's Claws (Weeks 1-6)

**Mission Count:** 15
**Tier Range:** 1-2
**Player Level:** 1-25

**Weekly Breakdown:**

**Week 1: Tutorial Foundation (4 missions)**
- `t1_01_hello_world` - Linux basics
- `t1_02_curiosity_killed_cat` - File discovery
- `t1_03_sticky_note` - Credential harvesting
- `t1_04_copycat` - File download

**Deliverables:**
- All mission JSON files created
- Architect overlay guides complete
- Network structures simple (1-2 nodes)
- Win conditions are sequential and clear

**Week 2: Tutorial Completion + First Faction (4 missions)**
- `t1_05_clean_paws` - Log wiping
- `t1_06_job_board` - Market tutorial
- `t1_07_nullpack_intro` - First faction mission
- `t1_08_circle_intro` - Second faction mission

**Deliverables:**
- Market system integration tested
- Faction email voices established
- Reputation reward system functional
- Choice mechanics in faction missions

**Week 3: Remaining Factions (3 missions)**
- `t1_09_glitch_intro` - Glitch-Hunters
- `t1_10_cipher_intro` - Cipher-Zero
- `t1_11_aegis_intro` - Aegis-Sec

**Deliverables:**
- All 6 faction voices complete
- Faction philosophy clearly differentiated
- Email templates for each faction

**Week 4: Tier 2 Story (2 missions)**
- `t2_01_architect_test` - Skill assessment
- `t2_02_cipher_invitation` - Cipher storyline

**Deliverables:**
- Difficulty increase (Tier 2 mechanics)
- Network complexity increases (2-3 nodes)
- First component mystery clues (subtle)

**Week 5: Tier 2 Completion (2 missions)**
- `t2_03_aegis_recruitment` - Aegis storyline
- `t2_04_first_signature` - CatoScript intro

**Deliverables:**
- CatoScript mission type functional
- Script file in mission for player to find
- Tutorial for script execution

**Week 6: Arc 1 Climax (1 mission)**
- `t2_05_whispers_of_omega` - Major revelation

**Deliverables:**
- Oracle revealed as antagonist
- Architect's quest established
- Player identified as Candidate #13
- Arc 1 story flags complete

### Arc 2: Alleyways & Shadows (Weeks 7-11)

**Mission Count:** 20
**Tier Range:** 3-4
**Player Level:** 26-45

**Weekly Breakdown:**

**Week 7: Arc 2 Opening + Oracle First Contact (4 missions)**
- `t3_01_architect_gift` - Advanced CatoScript
- `t3_02_nullpack_storyline` - NullPack deepening
- `t3_03_circle_storyline` - Circle deepening
- `t4_01_oracle_gaze` - **First Oracle encounter (MANDATORY)**

**Deliverables:**
- Oracle encounter system implemented
- Component mystery intensifies
- Faction choice setup begins

**Week 8: Faction Storylines (4 missions)**
- `t3_04_cipher_storyline` - Cipher deepening
- `t3_05_glitch_storyline` - Glitch deepening
- `t3_06_aegis_storyline` - Aegis deepening
- `t3_07_fork_in_road` - **MAJOR DECISION POINT**

**Deliverables:**
- All faction storylines playable pre-choice
- T3-07 choice mission with full consequences
- Alliance locking system functional

**Week 9: Tier 4 Faction-Specific (4 missions)**
- `t4_02_corporate_warfare` - Based on alliance
- `t4_03_architect_secret` - Lore deep dive
- `t4_04_zero_day_auction` - Economic mission
- Alternative versions for non-allied factions

**Deliverables:**
- Branching mission availability
- Alliance-specific content
- Economic choice mechanics

**Week 10: Arc 2 Pacing (4 missions)**
- `t4_05_betrayal` - **THE BETRAYAL (ARC 2 CLIMAX)**
- Recovery missions (post-betrayal)
- Reputation repair opportunities

**Deliverables:**
- Emotional betrayal implemented
- All factions turn hostile temporarily
- Survival mechanics

**Week 11: Polish and Integration**
- Test all T3-07 branches
- Validate betrayal consequences
- Ensure Arc 3 hooks are in place

### Arc 3: The Pack War (Weeks 12-17)

**Mission Count:** 25
**Tier Range:** 5-6
**Player Level:** 46-65

**Weekly Breakdown:**

**Week 12-13: Recovery Phase (6 missions)**
- `t5_01_dead_cat_bounce` - Survival
- `t5_02_new_alliances` - Reputation recovery
- Faction re-engagement missions
- Player goes "dark" mechanics

**Week 14-15: Oracle Truth Phase (6 missions)**
- `t5_03_oracle_embrace` - **Oracle direct contact (MAJOR REVELATION)**
- Oracle utopia tour
- Philosophy reveal
- Player's true goal choice

**Week 16-17: Heist Phase (8 missions)**
- `t5_06_the_heist` - **Arc 3 climax part 1**
- `t5_07_fallout` - **Arc 3 climax part 2**
- Multi-phase infiltration
- Escape sequences
- Oracle data center mechanics

**Deliverables:**
- Oracle sympathetic antagonist established
- Multi-faction war mechanics
- Epic heist feel
- Tier 6 access reward

### Arc 4: Broken Mirror (Weeks 18-22)

**Mission Count:** 20
**Tier Range:** 7-8
**Player Level:** 66-85

**Weekly Breakdown:**

**Week 18-19: Identity Crisis (7 missions)**
- `t7_01_mirror_image` - 12 candidates reveal
- `t7_02_oracle_prisoner` - Oracle sympathy
- `t7_03_circle_puppet` - Faction revelation
- `t7_04_architect_betrayal` - Architect's secret

**Week 20-21: Truth Phase (7 missions)**
- `t7_05_free_will` - Player agency
- `t7_06_ghost_machine` - **THE REVEAL MISSION**
- Component full awakening
- Architect's identity confirmed
- Player's system explained

**Week 22: Arc 4 Climax (6 missions)**
- `t8_03_oracle_final_offer` - Last chance
- `t8_04_architect_fate` - Emotional climax
- `t8_05_perfect_script` - Final preparation

**Deliverables:**
- All major revelations delivered
- Emotional beats land
- Player agency emphasized
- Ending paths clarified

### Arc 5: The Omega Sequence (Weeks 23-26)

**Mission Count:** 15
**Tier Range:** 9-10
**Player Level:** 86-100+

**Weekly Breakdown:**

**Week 23-24: Endgame Setup (8 missions)**
- `t9_01_last_calm` - Final goodbyes
- `t9_02_outer_defenses` - Epic breach
- `t9_03_inner_sanctum` - Core approach
- `t9_04_the_choice` - **Final decision**

**Week 25-26: Omega Creation (7 missions)**
- `t10_01_rebirth` - **Create Omega Sequence**
- All 7 endings implemented
- Post-credits scenes
- Epilogue generation

**Deliverables:**
- Epic feel maintained
- All endings achievable
- Player choices feel impactful
- Satisfying conclusions

---

## Part 4: Quality Checklist System

### Mission JSON Validation

**Structure Checklist:**
- [ ] All required fields present in `mission_meta`
- [ ] `id` follows naming convention: `t{tier}_{sequence}_{slug}`
- [ ] `category` matches directory location
- [ ] `difficulty` matches tier content
- [ ] `prerequisites` array correctly formatted
- [ ] `is_sequential` matches win condition structure

**Node Structure Checklist:**
- [ ] At least one node (homebase) defined
- [ ] All `node_id` values unique
- [ ] All `ip_address` values unique
- [ ] SSH credentials properly defined
- [ ] VFS uses `Record<string, VFSNode>` for children (NOT arrays)
- [ ] Standard FHS structure present on all nodes

**Win Condition Checklist:**
- [ ] All objective types valid values (command_executed, file_read, file_download, file_delete, node_connected, directory_navigation, content_match, player_choice)
- [ ] `target_node` references valid `node_id`
- [ ] `target_path` is absolute path (starts with `/`)
- [ ] Sequential missions have `is_sequential: true`
- [ ] Non-sequential missions have all objectives listed
- [ ] No contradictory objectives (e.g., delete AND download same file)
- [ ] `player_choice` objectives include `choice_prompt` and `choice_options`
- [ ] `command_executed` objectives include `command` property

**Guide System Checklist (Tutorial Missions):**
- [ ] `guide` array present
- [ ] Each step has unique `id`
- [ ] `message` content is clear and actionable
- [ ] `trigger` values are valid
- [ ] `criteria` matches expected values
- [ ] Steps progress logically

### Narrative Quality

**Story Integration:**
- [ ] Mission advances overall narrative
- [ ] Appropriate for current arc position
- [ ] Respects player's current knowledge
- [ ] Sets up future developments

**Character Voice:**
- [ ] Faction email voice is consistent
- [ ] Architect persona matches arc phase
- [ ] NPC dialogue is character-appropriate
- [ ] Environmental files enhance worldbuilding

**Pacing:**
- [ ] Difficulty appropriate for tier
- [ ] Clue revelation matches arc progression
- [ ] Emotional beats land at right moments
- [ ] No info dumps without gameplay

### Gameplay Quality

**Difficulty:**
- [ ] Challenge level matches tier
- [ ] Network complexity scales appropriately
- [ ] Objectives are clear and achievable
- [ ] No unfair punishment

**Mechanics:**
- [ ] Required tools are available to player
- [ ] Tutorial introduces new mechanics properly
- [ ] Player has agency in approach
- [ ] Multiple valid solutions where appropriate

**Rewards:**
- [ ] Credit reward matches tier (from `gameConfig.ts`)
- [ ] Reputation reward appropriate for difficulty
- [ ] Story progression meaningful
- [ ] Unlockables are valuable

### Technical Quality

**Performance:**
- [ ] File content sizes reasonable
- [ ] No memory leaks in network structure
- [ ] Asset loading optimized

**Compatibility:**
- [ ] Works with existing command set
- [ ] Compatible with mission loader
- [ ] No breaking changes to state management

**Edge Cases:**
- [ ] Player can't soft-lock mission
- [ ] Aborting mission works correctly
- [ ] Re-entry after abort handled
- [ ] Network pivoting doesn't break state

---

## Part 5: Testing Procedures

### Unit Testing (Per Mission)

**Automated Validation:**
```bash
# Run JSON schema validation
npm run validate:mission <mission_file>

# Run VFS structure validation
npm run validate:vfs <mission_file>

# Run win condition validation
npm run validate:objectives <mission_file>
```

**Manual Testing Checklist:**
- [ ] Mission loads correctly from mail client
- [ ] Network nodes are reachable
- [ ] All files are accessible
- [ ] Win conditions trigger correctly
- [ ] Mission completes successfully
- [ ] Rewards are applied correctly
- [ ] Story flags are set/unset
- [ ] Mission abort works cleanly

### Integration Testing (Per Arc)

**Sequential Flow:**
- [ ] Missions unlock in correct order
- [ ] Prerequisites are enforced
- [ ] Story flags propagate correctly
- [ ] Reputation changes affect later missions
- [ ] Faction alliance gates work

**Branching Paths:**
- [ ] T3-07 choice branches properly
- [ ] Each alliance path is playable
- [ ] Betrayal consequences work
- [ ] Rejoining factions possible

**State Persistence:**
- [ ] Progress saves between sessions
- [ ] Completed missions stay completed
- [ ] Story flags persist
- [ ] Reputation saves correctly

### Full Campaign Testing

**Complete Playthroughs:**
- [ ] All endings achievable
- [ ] Each ending has unique path
- [ ] Prerequisites work correctly
- [ ] No blocked endings without reason

**Choice Tracking:**
- [ ] Major choices are recorded
- [ ] Choices affect ending availability
- [ ] Moral score calculates correctly
- [ ] Architect relationship tracks

### Regression Testing

**After Arc Completion:**
- [ ] Replay previous arcs
- [ ] Verify no broken changes
- [ ] Check story flag conflicts
- [ ] Test earlier missions still work

---

## Part 6: Integration with Core Services

### CampaignState Integration

**Story Flag Management:**
```typescript
// In MissionLoaderService.ts - extend completion handler
const handleMissionCompletion = (mission: Mission, gameState: GameState) => {
  // Set story flags
  if (mission.mission_meta.story_flags?.sets) {
    gameState.campaignState.storyFlags.push(...mission.mission_meta.story_flags.sets);
  }

  // Update arc progression
  if (mission.mission_meta.arc > gameState.campaignState.currentArc) {
    gameState.campaignState.currentArc = mission.mission_meta.arc;
  }

  // Track completed missions
  gameState.campaignState.completedMissions.push(mission.mission_meta.id);
};
```

**Flag Definitions (to be added to CampaignState.ts):**
```typescript
interface CampaignStoryFlags {
  // Arc 1 flags
  metArchitect: boolean;
  completedTutorial: boolean;
  learnedAboutOracle: boolean;
  discoveredComponentClue_1: boolean;  // T1-01
  discoveredComponentClue_2: boolean;  // T1-03
  discoveredComponentClue_3: boolean;  // T2-01
  discoveredComponentClue_4: boolean;  // T2-04

  // Arc 2 flags
  firstOracleEncounter: boolean;
  choseFactionAlliance: boolean;
  factionAlliance: 'nullpack' | 'circle' | 'cipher' | 'aegis' | 'glitch' | null;
  wasBetrayedByFaction: boolean;

  // Arc 3 flags
  oracleDirectContact: boolean;
  learnedArchitectCreatedOracle: boolean;
  discoveredComponentClue_5_12: boolean;  // Remaining clues

  // Arc 4 flags
  architectIdentityRevealed: boolean;
  knowsComponentIsBirthingCore: boolean;
  knowsArchitectIsDying: boolean;
  componentFullyAwakened: boolean;

  // Arc 5 flags
  omegaSequenceCreated: boolean;
  finalChoiceMade: boolean;
  endingAchieved: string;
}
```

### FactionReputationService Integration

**Reputation Reward Application:**
```typescript
// In MissionLoaderService.ts
const applyReputationRewards = (mission: Mission, playerState: PlayerState) => {
  if (!mission.reputation_rewards) return;

  mission.reputation_rewards.forEach(reward => {
    const currentRep = playerState.factionReputation[reward.faction] || 0;
    const newRep = Math.max(-100, Math.min(100, currentRep + reward.amount));
    playerState.factionReputation[reward.faction] = newRep;
  });

  // Trigger faction-specific events based on new reputation
  FactionReputationService.checkThresholdEvents(playerState);
};
```

**Faction Relationship Effects (to be implemented):**
```typescript
// Faction-specific mission availability
const getAvailableFactionMissions = (
  playerRep: FactionReputation,
  mission: Mission
): boolean => {
  const requiredFaction = mission.mission_meta.faction_requirement;
  if (!requiredFaction) return true;

  const playerRepWithFaction = playerRep[requiredFaction];
  return playerRepWithFaction >= FACTION_ACCESS_LEVELS.neutral;
};
```

### EndingService Integration

**Prerequisite Tracking (to be implemented in EndingService.ts):**
```typescript
interface EndingPrerequisites {
  digital_requiem: {
    architectRep: 50,
    oracleSympathy: -50
  };
  new_dawn: {
    architectRep: 30,
    oracleSympathy: 80,
    moralityScore: 50,
    cryptographySkill: 'high'
  };
  total_control: {
    architectRep: -50,
    moralityScore: -80
  };
  // ... etc for all endings
}

const checkEndingAvailability = (
  endingType: EndingType,
  playerState: PlayerState,
  campaignState: CampaignState
): boolean => {
  const prereqs = EndingPrerequisites[endingType];
  // Check all prerequisite conditions
  // Return true if player qualifies
};
```

---

## Part 7: Step-by-Step Creation Process

### Step 1: Pre-Creation Planning

**Review Mission Context:**
1. Check `00_MASTER_CAMPAIGN_STRUCTURE.md` for mission's place in arc
2. Identify mission type (Tutorial, Faction, Story, Climax)
3. Note required story flags to set and require
4. Confirm tier and difficulty
5. Check prerequisite missions

**Define Mission Scope:**
1. Number of nodes (1-5 depending on tier)
2. Primary objective type
3. Secondary objectives (if any)
4. Environmental content needed
5. Reputation rewards

### Step 2: Create Mission JSON

**Fill Required Fields:**
```json
{
  "mission_meta": {
    "id": "t{tier}_{sequence}_{descriptive_slug}",
    "title": "Mission Title: Subtitle",
    "category": "story_arc_{arc_number}",
    "is_sequential": true/false,
    "order": {position_in_arc},
    "prerequisites": ["previous_mission_id"],
    "difficulty": {tier},
    "reward_credits": {from_gameConfig},
    "reward_reputation": {from_gameConfig},
    "client": "Faction Name or The Architect",
    "description": "One-line mission brief",
    "is_mandatory": true/false,
    "IsScriptMission": false,
    "story_flags": {
      "sets": ["flag_to_set"],
      "requires": ["flag_needed"]
    },
    "architect_persona": "mentor|teacher|confessional|legacy"
  },
  "briefing": {
    "email_subject": "Action-oriented subject line",
    "email_body": "Detailed brief with faction voice\n\nInclude:\n- Mission objectives\n- Target details\n- Reward info\n- Character voice",
    "sender": "faction@domain.net"
  }
}
```

**Design Network Nodes:**
```json
"nodes": [
  {
    "node_id": "homebase",
    "hostname": "kitten-terminal",
    "ip_address": "127.0.0.1",
    "ssh_password": "",
    "files": {
      "home": {
        "type": "directory",
        "name": "home",
        "children": {
          "user": {
            "type": "directory",
            "name": "user",
            "children": {
              "readme.txt": {
                "type": "file",
                "name": "readme.txt",
                "content": "Welcome to the system"
              }
            }
          }
        }
      }
    }
  },
  {
    "node_id": "target_gateway",
    "hostname": "corporate-server",
    "ip_address": "10.0.1.50",
    "ssh_password": "password123",
    "files": {
      // Standard FHS structure created automatically by MissionLoaderService
      // Mission-specific files defined here
    }
  }
]
```

**CRITICAL VFS Structure Rules:**
1. Directory `children` MUST be `Record<string, VFSNode>` (object), NEVER arrays
   ```typescript
   // CORRECT
   "children": { "file.txt": { "type": "file", "name": "file.txt" } }

   // WRONG - This will crash the system!
   "children": [ { "name": "file.txt", "type": "file" } ]
   ```

2. All file paths must be absolute (start with `/`)

3. `node_id` values must be unique across all nodes

4. `ip_address` values must be unique across all nodes

5. `ssh_password` is optional for homebase (no password required)

6. MissionLoaderService automatically creates standard FHS structure (`/home`, `/root`, `/etc`, `/var`, `/tmp`)

**Define Win Conditions:**
```json
"win_conditions": {
  "is_sequential": true,
  "sequence": [
    {
      "type": "node_connected",
      "target_node": "target_gateway",
      "description": "Connect to the target server"
    },
    {
      "type": "file_read",
      "target_node": "target_gateway",
      "target_path": "/path/to/objective/file",
      "description": "Read the target file"
    },
    {
      "type": "player_choice",
      "target_node": "target_gateway",
      "choice_prompt": "What do you do with the evidence?",
      "choice_options": [
        { "id": "leak_publicly", "description": "Leak to the press" },
        { "id": "sell_back", "description": "Sell to the corporation" }
      ]
    }
  ]
}
```

**Available Objective Types:**
- `command_executed` - Player executes specific command (use `command` property)
- `file_read` - Player reads file with `cat`
- `file_download` - Player downloads file with `download`
- `file_delete` - Player deletes file with `rm`
- `node_connected` - Player connects to node via `ssh`
- `directory_navigation` - Player navigates to specific directory
- `content_match` - Player modifies file content to match required value
- `player_choice` - Auto-triggers choice modal (uses `choice_prompt` and `choice_options`)

**Win Condition Properties:**
- `type` - Objective type (see list above)
- `target_node` - ID of node where objective is completed
- `target_path` - Absolute path to target file (for file objectives)
- `description` - Human-readable objective description
- `command` - Command string to execute (for `command_executed`)
- `required_content` - Content string to match (for `content_match`)
- `choice_prompt` - Prompt text for choice modal
- `choice_options` - Array of choice objects with `id` and `description`

**Add Guide (Tutorial Missions):**
```json
"guide": [
  {
    "id": "step1",
    "message": "First instruction with Architect voice~",
    "trigger": "COMMAND_NAME",
    "criteria": "ssh"
  },
  {
    "id": "step2",
    "message": "Next instruction~",
    "trigger": "VFS_PATH_EXISTS",
    "criteria": "/path/to/check"
  }
]
```

**Add Script Events:**
```json
"script_events": {
  "on_start": [
    { "speaker": "architect", "text": "Mission start message" }
  ],
  "on_objective_complete": {
    "0": [
      { "speaker": "architect", "text": "First objective complete" }
    ],
    "1": [
      { "speaker": "oracle", "text": "DID YOU SEE THAT?", "phase": "menacing" }
    ]
  },
  "on_choice": {
    "leak_publicly": [
      { "speaker": "faction", "faction": "nullpack", "speakerName": "ZeroCool", "text": "Nice choice!" }
    ]
  },
  "on_complete": [
    { "speaker": "architect", "text": "Mission complete!" },
    { "fx": "mission_complete" }
  ]
}
```

**Script Event Format (New Simplified Format - Recommended):**
The dialogue system supports a new simplified format where properties determine event type:

**Dialogue Events** (have `speaker` property):
```json
{ "speaker": "architect", "text": "Welcome, nya~" }
{ "speaker": "oracle", "text": "CANDIDATE THIRTEEN.", "phase": "menacing" }
{ "speaker": "oracle", "text": "I understand.", "phase": "sympathetic" }
{ "speaker": "faction", "faction": "nullpack", "speakerName": "ZeroCool", "text": "yo." }
{ "speaker": "faction", "faction": "the_circle", "speakerName": "Vesper", "text": "Greetings." }
```

**FX Events** (have `fx` property):
```json
{ "fx": "birthing_core_flash_01" }
{ "fx": "oracle_reveal" }
{ "fx": "tutorial_complete" }
```

**Legacy Format (Still Supported):**
```json
{ "type": "architect_dialogue", "dialogue": "Welcome, nya~" }
{ "type": "oracle_dialogue", "dialogue": "CANDIDATE THIRTEEN.", "phase": "menacing" }
{ "type": "faction_contact_dialogue", "faction": "nullpack", "speaker": "ZeroCool", "dialogue": "yo." }
{ "type": "system_event", "event": "birthing_core_flash_01" }
```

**Script Event Properties:**
- `on_start` - Array of events when mission loads
- `on_objective_complete` - Object mapping objective index to events (string keys: "0", "1", etc.)
- `on_choice` - Object mapping choice IDs to events
- `on_complete` - Array of events when mission completes

**Typewriter Effect Pauses:**
Use `[pause N]` tags to control timing:
```json
{ "speaker": "architect", "text": "[pause 3000] Oh! [pause 1000] A connection?" }
```

### Step 3: Write File Content

**Create Environmental Files:**

1. **Emails** (in `/home/user/mail/` or `/var/mail/`):
   - Faction-specific voice
   - Proper header format
   - Meaningful content (worldbuilding + clues)

2. **Logs** (in `/var/log/`):
   - System events
   - Auth trails
   - Oracle detection entries (for higher tiers)

3. **Documents** (in appropriate directories):
   - Research papers
   - Corporate memos
   - Personal journals

4. **Configuration Files** (in `/etc/`):
   - Realistic boilerplate
   - Mission-specific settings
   - Hidden credentials

**Apply Faction Voices:**
- Refer to `01_LORE_STORY_PLAN.md` for faction voice guides
- Maintain consistency across all files
- Include lived-in details

### Step 4: Validate Mission

**Automated Checks:**
```bash
# Validate JSON structure
npx jsonlint game/public/missions/story_arc_01/t1_01_hello_world.json

# Run mission indexer
npm run predev

# Check for VFS structure errors
npm run validate:vfs
```

**Manual Checklist:**
1. [ ] JSON is valid and properly formatted
2. [ ] All node IDs are unique
3. [ ] All IP addresses are unique
4. [ ] VFS children are objects, not arrays
5. [ ] Win conditions reference valid nodes/paths
6. [ ] Story flags are correctly defined
7. [ ] Rewards match tier configuration

### Step 5: Integrate with Campaign

**Add to Mission Index:**
- Run `npm run predev` to regenerate index
- Verify mission appears in correct category

**Configure Prerequisites:**
- Ensure previous missions exist
- Set story flag dependencies

**Test Sequential Flow:**
- Verify mission unlocks after prerequisites
- Check next mission unlocks after completion

---

## Part 8: Production Tracking

### Mission Production Spreadsheet

**Column Structure:**
| ID | Title | Arc | Tier | Type | Status | Writer | Tester | Notes |
|----|-------|-----|------|------|--------|--------|--------|-------|
| t1_01_hello_world | Terminal Foundations | 1 | 1 | Tutorial | Complete | Name | Name | - |
| t1_02_curiosity | File Discovery | 1 | 1 | Tutorial | In Progress | Name | - | Needs testing |
| t1_03_sticky_note | Credential Harvest | 1 | 1 | Tutorial | Not Started | - | - | - |

**Status Values:**
- `Not Started` - No work begun
- `Outlining` - Structure being planned
- `Writing` - Content being created
- `Review` - Initial review pending
- `Testing` - In QA
- `Complete` - Final approved
- `Blocked` - Waiting on dependency

### Story Flag Tracking

**Flag Assignment by Mission:**

| Mission ID | Sets | Requires | Notes |
|------------|------|----------|-------|
| t1_01_hello_world | met_architect | - | First Architect contact |
| t1_03_sticky_note | component_clue_1 | - | First subtle clue |
| t2_01_architect_test | component_clue_3 | component_clue_1 | Follows clue chain |

### Content Completion Tracking

**Arc Completion Dashboard:**
```markdown
## Arc 1: The Kitten's Claws
- [ ] Tutorial Missions (6)
- [ ] Faction Introductions (6)
- [ ] Tier 2 Story (4)
- [ ] Environmental Content (all missions)
- [ ] Testing Complete

## Arc 2: Alleyways & Shadows
- [ ] Story Progression (10)
- [ ] Faction Storylines (10)
- [ ] ...
```

---

## Part 9: Common Pitfalls & Solutions

### VFS Structure Pitfalls

**Pitfall 1: Array Children (CRITICAL)**
```json
// WRONG - Will crash the system!
"children": [
  { "name": "file.txt", "type": "file" }
]

// CORRECT
"children": {
  "file.txt": { "type": "file", "name": "file.txt" }
}
```

**Solution:** Always use `Record<string, VFSNode>` for directory children. The key should match the filename.

**Pitfall 2: Missing Absolute Paths**
```json
// WRONG - Relative path
"target_path": "home/user/file.txt"

// CORRECT - Absolute path
"target_path": "/home/user/file.txt"
```

**Solution:** Always use absolute paths starting with `/`.

**Pitfall 3: Node Ghosting**
When switching nodes via SSH, the `currentDirectory` must be reset to prevent VFS crossover.

**Solution:** The system handles this automatically in `MissionManager.handleNodeConnection`. Always use this middleware for node transitions.

**Pitfall 4: Incorrect Objective Type References**
```json
// WRONG - Using lowercase type
{ "type": "file_read", "target_node": "server", "target_path": "/data/file.txt" }

// CORRECT - MissionLoaderService normalizes to uppercase internally
{ "type": "file_read", "target_node": "server", "target_path": "/data/file.txt" }
```

**Solution:** Use lowercase objective types in JSON (e.g., `file_read`, `node_connected`, `player_choice`). MissionLoaderService automatically normalizes to uppercase enum values (e.g., `FILE_READ`, `NETWORK_PIVOT`, `PLAYER_CHOICE`).

### Narrative Pitfalls

**Pitfall 1: Obvious Clues**
The component mystery is meant to be subtle. Early clues should be easily missed.

**Solution:**
- Arc 1 clues: Architect reactions, strange symbols
- Arc 2 clues: System anomalies, subtle mentions
- Arc 3 clues: Direct Oracle references
- Arc 4 clue: The reveal

**Pitfall 2: Breaking Character**
Factions have distinct voices. Breaking immersion hurts the experience.

**Solution:** Use the voice templates in `01_LORE_STORY_PLAN.md`. Have writers review faction voice before writing.

**Pitfall 3: Info Dumps**
Large exposition blocks break pacing and feel unnatural.

**Solution:** Deliver information through:
- Environmental files (emails, logs, journals)
- Architect dialogue in guide system
- Oracle encounters
- Small reveals across multiple missions

### Gameplay Pitfalls

**Pitfall 1: Tutorial Fatigue**
Too many hand-holding tutorials bore experienced players.

**Solution:**
- Tutorials concentrated in Arc 1 (~40%)
- Taper off through Arc 2 (~20%)
- Gone by Arc 3

**Pitfall 2: Difficulty Spikes**
Sudden increases in difficulty frustrate players.

**Solution:**
- Use gameConfig tier rewards as baseline
- Scale network complexity gradually
- Test each mission sequentially

**Pitfall 3: Unfair Consequences**
Player choices should have consequences, but not be punitive.

**Solution:**
- Always provide recovery paths
- Make consequences clear before choice
- Allow redemption arcs

---

## Part 10: Automation & Tooling

### Mission Generation Scripts

**Template Generator:**
```bash
# Generate new mission from template
npm run generate:mission -- --tier 1 --arc 1 --type tutorial
```

**Template Structure:**
```json
{
  "mission_meta": {
    "id": "{{tier}}_{{sequence}}_{{slug}}",
    "title": "{{title}}",
    "category": "story_arc_{{arc}}",
    ...
  },
  "nodes": [
    {{> homebase_node}},
    {{> target_node}}
  ],
  "win_conditions": {
    {{> win_conditions_template}}
  }
}
```

### Validation Scripts

**JSON Schema Validation:**
```bash
npm run validate:all-missions
```

**VFS Structure Validator:**
```bash
npm run validate:vfs-structure
```

**Story Flag Validator:**
```bash
npm run validate:story-flags
```

### Content Asset Libraries

**Reusable File Components:**

1. **Log Boilerplates:**
   - `/var/log/syslog` - Standard system log
   - `/var/log/auth.log` - Authentication log
   - `/var/log/apache2/access.log` - Web server log

2. **Document Templates:**
   - Corporate memo template
   - Research paper template
   - Email template per faction

3. **VFS Clutter Pools:**
   - Home directory files
   - Desktop files
   - Downloads directory
   - Documents folder

**Usage:**
```json
"files": {
  "home": {
    "type": "directory",
    "name": "home",
    "children": {
      "{{include: home_clutter}}": {},
      "user": {
        "type": "directory",
        "name": "user",
        "children": {
          "{{include: user_desktop}}": {},
          "{{include: user_documents}}": {}
        }
      }
    }
  }
}
```

---

## Part 11: Final Production Checklist

### Pre-Launch Mission Checklist

**Structure:**
- [ ] JSON validates against schema
- [ ] All required fields present
- [ ] Node IDs unique and valid
- [ ] IP addresses unique and valid
- [ ] VFS uses correct structure (objects, not arrays)
- [ ] File paths are absolute
- [ ] Win conditions reference valid targets

**Narrative:**
- [ ] Mission advances story appropriately
- [ ] Faction voice consistent
- [ ] Architect persona matches arc phase
- [ ] Environmental files add depth
- [ ] No placeholder content
- [ ] Clues delivered at appropriate subtlety

**Gameplay:**
- [ ] Objectives clear and achievable
- [ ] Difficulty matches tier
- [ ] Network complexity appropriate
- [ ] Tools required are available
- [ ] Multiple solutions where possible
- [ ] No soft-locks possible

**Rewards:**
- [ ] Credits match `gameConfig.TIERS[tier].creditReward`
- [ ] Reputation matches `gameConfig.MISSIONS.TIERS[tier].rewardRange`
- [ ] Story flags set correctly
- [ ] Faction reputation rewards valid

**Technical:**
- [ ] No memory leaks
- [ ] Asset loading optimized
- [ ] Compatible with mission loader
- [ ] Compatible with state management
- [ ] Error handling in place

**Testing:**
- [ ] Unit tested (automated validation passed)
- [ ] Integration tested (sequential flow works)
- [ ] Manually tested (playable from start to finish)
- [ ] Edge cases tested (abort, retry, etc.)
- [ ] Performance tested (no lag)

**Documentation:**
- [ ] Mission ID recorded in tracking spreadsheet
- [ ] Story flags documented
- [ ] Dependencies noted
- [ ] Tester feedback recorded

---

## Appendix A: Quick Reference

### Mission ID Convention

```
t{tier}_{sequence}_{descriptive_slug}

Examples:
t1_01_hello_world
t3_07_fork_in_road
t7_06_ghost_machine
t10_01_rebirth
```

### Tier Reward Quick Reference

| Tier | Credits | Reputation | Min Rep |
|------|---------|------------|---------|
| 1 | 600 | 500 | 0 |
| 2 | 1,200 | 1,000 | 2,000 |
| 3 | 3,500 | 2,500 | 7,000 |
| 4 | 12,000 | 5,000 | 19,500 |
| 5 | 35,000 | 10,000 | 60,000 |
| 6 | 120,000 | 25,000 | 150,000 |
| 7 | 350,000 | 60,000 | 400,000 |
| 8 | 1,000,000 | 150,000 | 1,180,000 |
| 9 | 3,500,000 | 350,000 | 3,300,000 |
| 10 | 10,000,000 | 500,000 | 8,500,000 |

### Faction Email Domains

| Faction | Domain | Example |
|---------|--------|---------|
| Architect | retro-sim.net | architect@retro-sim.net |
| NullPack | retro-sim.net | nullpack@retro-sim.net |
| The Circle | the-circle.io | vesper@the-circle.io |
| Cipher-Zero | c0.onion | entropy@c0.onion |
| Aegis-Sec | aegis-sec.com | h7@aegis-sec.com |
| Glitch-Hunters | gh.labs | buffer@gh.labs |

### Architect Persona Phases

| Arc | Phase | Characteristics | Verbal Tic |
|-----|-------|-----------------|------------|
| 1-2 | Mentor | Playful, encouraging | "nya~" |
| 3 | Teacher | Strict, demanding, exhausted | "nya~" (slips) |
| 4 | Confessional | Ashamed, broken | Drops completely |
| 5 | Legacy | Tired, proud, at peace | Returns occasionally |

---

## Appendix B: Mission JSON Template

```json
{
  "mission_meta": {
    "id": "t1_01_example_mission",
    "title": "Mission Title: Subtitle",
    "category": "arc_01",
    "is_sequential": true,
    "order": 1,
    "prerequisites": [],
    "difficulty": 1,
    "reward_credits": 200,
    "reward_reputation": 75,
    "client": "The Architect",
    "is_mandatory": true,
    "architect_persona": "mentor",
    "description": "Learn basics of terminal navigation",
    "story_flags": {
      "sets": ["metArchitect"],
      "requires": []
    },
    "reputation_rewards": []
  },
  "briefing": {
    "email_subject": "Mission Subject",
    "email_body": "Mission brief with faction voice",
    "sender": "architect@retro-sim.net"
  },
  "nodes": [
    {
      "node_id": "homebase",
      "hostname": "kitten-terminal",
      "ip_address": "127.0.0.1",
      "ssh_password": "",
      "files": {
        "home": {
          "type": "directory",
          "name": "home",
          "children": {
            "user": {
              "type": "directory",
              "name": "user",
              "children": {
                "readme.txt": {
                  "type": "file",
                  "name": "readme.txt",
                  "content": "File content here"
                }
              }
            }
          }
        }
      }
    }
  ],
  "guide": [
    {
      "id": "step1",
      "message": "First instruction",
      "trigger": "COMMAND_NAME",
      "criteria": "ls"
    }
  ],
  "win_conditions": {
    "is_sequential": true,
    "sequence": [
      {
        "type": "command_executed",
        "command": "ls",
        "description": "List directory contents"
      },
      {
        "type": "file_read",
        "target_node": "homebase",
        "target_path": "/home/user/readme.txt",
        "description": "Read readme file"
      }
    ]
  },
  "script_events": {
    "on_start": [
      { "speaker": "architect", "text": "Mission start message" }
    ],
    "on_objective_complete": {
      "0": [
        { "speaker": "architect", "text": "Good work!" }
      ]
    },
    "on_complete": [
      { "speaker": "architect", "text": "Mission complete!" }
    ]
  }
}
```

**Key Structure Notes:**
1. `files` uses object-based VFS structure where `children` is `Record<string, VFSNode>`
2. `node_id` maps to `NetworkNode.id` internally (property name change handled by MissionLoaderService)
3. `ip_address` maps to `NetworkNode.ip` internally
4. Objective types use lowercase in JSON, normalized to uppercase enums internally
5. `script_events` uses new simplified format (speaker-based for dialogue, fx-based for effects)

---

*"The static never sleeps, nya~. But a well-built mission always runs."* - The Architect
