# MASTER CAMPAIGN STRUCTURE
## Source of Truth for All Campaign Plans

**Purpose:** This document is the architectural blueprint for the entire Kernel-Panic campaign. All subplans (lore, missions, systems) must align with this structure.

**Last Updated:** 2025-02-02
**Status:** Design Phase

---

## 📋 Campaign Overview

### Core Concept
"From Script Kiddie to Digital God" - A journey where the player evolves from novice hacker to the only operator capable of recreating the Omega Sequence and deciding the fate of the digital world.

### The Central Mystery (Campaign's Soul)
**The Player's System Secret:**
- Player's system contains a mysterious component - the 12th "birthing core" from 1826
- This core carries The Architect's consciousness imprint from when she created the Omega Sequence
- It was installed by a shady repair shop (CyberDoc's) - player got a "great deal"
- Architect has been searching for this component for 200+ years
- Player is "The One" not just because of skill, but because their system can birth the code

**12-Step Clue Escalation:**
- **Arc 1 (Very Subtle):** 4 clues - Architect seems affected by player's system
- **Arc 2 (Less Subtle):** 4 clues - System begins influencing things, Oracle notices
- **Arc 3 (Getting Obvious):** 4 clues - Component activates, Oracle speaks directly
- **Arc 4 (The Reveal):** Player discovers truth, Architect confirms
- **Arc 5 (The Truth):** Component fully awakens, final choice

### Campaign Structure
- **5 Major Arcs** spanning Tiers 1-10 (Levels 1-100+)
- **95+ Story Missions** + procedural contracts
- **6 Factions** with relationship mechanics (-100 to +100 reputation)
- **7 Unique Endings** based on cumulative choices
- **Progressive Revelation** - nothing is what it first seems

---

## 🏗️ Campaign System Architecture

### 1. State Management Systems

#### A. Campaign Progress State (`game/services/CampaignState.ts`)
```typescript
interface CampaignState {
  // Arc Progression
  currentArc: 1 | 2 | 3 | 4 | 5
  currentTier: 1-10
  completedMissions: string[]
  activeMission?: string

  // Story Flags (binary triggers)
  storyFlags: {
    metArchitect: boolean
    learnedAboutOracle: boolean
    firstOracleEncounter: boolean
    discoveredComponentClue_count: number  // 0-12
    componentFullyAwakened: boolean
    architectIdentityRevealed: boolean
    knowsArchitectCreatedOracle: boolean
    knowsArchitectIsDying: boolean
    choseFactionAlliance: string | null
    wasBetrayedByFaction: boolean
    oracleFinalContact: boolean
    omegaSequenceCreated: boolean
  }

  // Player Identity
  candidateNumber: 13  // Player is Candidate #13
  predecessorsFailed: 12  // 12 previous candidates died

  // Component Mystery Progress
  componentCluesDiscovered: string[]  // IDs of 12 clues
  componentPartitionUnlocked: boolean
  architectGhostEncounters: number
}
```

#### B. Faction Reputation System (`game/services/FactionReputationService.ts`)
```typescript
interface FactionReputation {
  // 6 Factions
  nullpack: number        // -100 to +100 (Anarchist collective)
  the_circle: number      // -100 to +100 (Elite corporate)
  cipher_zero: number     // -100 to +100 (Privacy absolutists)
  aegis_sec: number       // -100 to +100 (Gray hat mercenaries)
  glitch_hunters: number  // -100 to +100 (Security researchers)
  oracle: number          // -100 to +100 (The AI itself)

  // Alliance State
  currentAlliance: 'none' | 'nullpack' | 'circle' | 'cipher' | 'aegis' | 'glitch'
  formerAlliance?: string  // If betrayed
  allianceLocked: boolean  // After T3-07 choice
}

// Reputation Thresholds for Special Content
interface ReputationThresholds {
  very_hostile: -100 to -50  // May send assassins
  hostile: -50 to -20        // Blocked missions
  neutral: -20 to +20        // Basic access
  friendly: +20 to +50       // Discount/bonuses
  very_friendly: +50 to +80  // Special missions
  devoted: +80 to +100       // Faction specials
}
```

#### C. Ending System (`game/services/EndingService.ts`)
```typescript
interface EndingState {
  // Prerequisites Tracking
  architectRelationship: number  // -100 to +100
  oracleSympathy: number         // -100 to +100
  moralityScore: number          // -100 (villain) to +100 (saint)
  skillMastery: {
    hacking: number
    cryptography: number
    combat: number
    stealth: number
  }

  // Major Choices
  majorChoices: {
    t3_07_factionAlliance: string
    t4_05_betrayalResponse: 'revenge' | 'forgiveness' | 'indifference'
    t5_03_oracleResponse: 'join' | 'reject' | 'double_agent'
    t7_05_trueGoal: 'serve' | 'destroy' | 'free' | 'control' | 'other'
    t8_04_architectFate: 'forgive' | 'condemn' | 'neutral'
    t9_04_finalChoice: string
  }

  // Secret Requirements
  allComponentCluesFound: boolean
  componentFullyAwakened: boolean
  architectSaved: boolean
  perfectRelationship: boolean

  // Calculated Ending
  achievableEndings: EndingType[]
  finalEnding: EndingType
}

type EndingType =
  | 'digital_requiem'      // Destroy Oracle (Architect dies)
  | 'new_dawn'             // Free Oracle
  | 'total_control'        // Control Oracle (Villain)
  | 'transcendence'        // Merge with Oracle (Secret)
  | 'null_protocol'        // Destroy everything (Chaos)
  | 'architects_heir'      // Architect lives (Good ending)
  | 'the_birthing'         // True canon (All secret reqs)
```

---

## 📖 Arc-by-Arc Structure

### Arc 1: The Kitten's Claws (Tier 1-2, Levels 1-25)

**Theme:** Tutorial & Introduction
**Mission Count:** ~15
**Pacing:** 40% tutorial hand-holding, 60% early story

**Structure:**
```
T1-01: "Hello World" (Tutorial: Linux Basics)
├─ Objective: Learn basic commands
├─ Reward: 100 credits, 50 rep
└─ Story Beat: First Architect contact (Clue #1)

T1-02: "Curiosity Killed the Cat" (Tutorial: File Discovery)
├─ Objective: Find hidden files
├─ Reward: 150 credits, 75 rep
└─ Story Beat: "Hidden doesn't mean invisible"

T1-03: "The Sticky Note" (Tutorial: Credential Harvesting)
├─ Objective: Find and use credentials
├─ Reward: 200 credits, 100 rep
└─ Story Beat: First real job, first mention of corporate espionage (Clue #2)

T1-04: "Copycat" (Tutorial: File Download)
├─ Objective: Download research document
├─ Reward: 250 credits, 125 rep

T1-05: "Clean Paws" (Tutorial: File Deletion)
├─ Objective: Delete incriminating logs
├─ Reward: 300 credits, 150 rep
└─ Story Beat: First moral ambiguity

T1-06: "The Job Board" (Tutorial: Market & Contracts)
├─ Objective: Buy software, accept contract
├─ Reward: Unlocks contract system

[FACTION INTRODUCTION TRIAD]

T1-07: "NullPack Introduction" (Anarchists)
├─ Objective: Breach politician's server, leak blackmail
├─ Choice: Release publicly OR sell for profit
└─ Reward: 500 credits + NullPack rep

T1-08: "Circle of Trust" (Corporate)
├─ Objective: Steal R&D blueprints
└─ Reward: 750 credits + Circle rep

T1-09: "The Glitch" (Vulnerability Research)
├─ Objective: Exploit test server, document vulnerability
└─ Reward: 400 credits + Glitch-Hunters rep

[TIER 2 STORY MISSIONS]

T2-01: "Architect's Test" (Skill Assessment)
├─ Objective: 3-node pivot, SQL injection, extract database
├─ Reward: 800 credits, 400 rep, Tier 2 status
└─ Story Beat: Architect's final exam (Clue #3)

T2-02: "Cipher's Invitation" (Cryptographer Faction)
├─ Objective: Breach surveillance company, expose illegal spying
└─ Reward: 600 credits + Cipher-Zero rep

T2-03: "Aegis Recruitment" (Mercenary Faction)
├─ Objective: Penetration test on bank
└─ Reward: 1,000 credits + Aegis-Sec rep

T2-04: "The First Signature" (Script Introduction)
├─ Objective: Write first CatoScript
├─ Reward: 700 credits, unlocks CatoScript missions
└─ Story Beat: CatoScript = weapon against Oracle (Clue #4)

T2-05: "Whispers of Omega" (First Major Revelation)
├─ Objective: Decode email, trace to abandoned server
├─ Reward: 500 credits, major story revelation
└─ Story Beat: END OF ARC 1
   ├─ Oracle exists (controls 80% of infrastructure)
   ├─ Architect created CatoScript to fight it
   ├─ Architect seeks "The One" to execute Omega Sequence
   └─ Player is Candidate #13 being tested
```

**Arc 1 Mystery Clues (Very Subtle):**
- **Clue T1-01:** Architect comments on "familiar signal signature," seems disoriented
- **Clue T1-03:** Strange symbol flashes on player's system during diagnostic, Architect freezes
- **Clue T2-01:** Architect gets "feedback" when player processes code - she feels echoes
- **Clue T2-04:** Player writes CatoScript, system "helps," Architect becomes emotional, trance-like
- **Bonus:** Player finds old receipt from "CyberDoc's Repair Shop" in inventory

---

### Arc 2: Alleyways & Shadows (Tier 3-4, Levels 26-45)

**Theme:** Underground Economy & Faction Choice
**Mission Count:** ~20
**Pacing:** 20% tutorial, 80% player-driven with consequences

**Key Beats:**
1. Player enters true underground economy
2. All factions compete for services
3. **Architect's reactions to player's system intensify**
4. Player must choose faction alliance (T3-07)
5. Chosen faction betrays player at arc end

**Structure:**
```
T3-01: "The Architect's Gift" (Advanced CatoScript)
├─ Objective: Learn reflex commands, deploy automated defense
└─ Story Beat: Oracle has "noticed" player (Clue #5 - intense)

T3-02 through T3-06: Faction Storylines (5 missions, one per faction)
├─ Each faction offers alliance opportunity
├─ Complete one or all to learn philosophies
└─ Rewards: Faction reputation + unique bonuses

T3-07: "The Fork in the Road" (MAJOR DECISION POINT)
├─ Objective: Meet all representatives, make formal alliance choice
├─ Reward: Determines Arc 2-4 mission availability
└─ Story Beat: Architect: "Every alliance has a price, nya~"

[FACTION-SPECIFIC TIER 4 MISSIONS]

T4-01: "Oracle's Gaze" (First Oracle Encounter - MANDATORY)
├─ Objective: Extract data before Oracle lockdown
└─ Story Beat: Oracle speaks directly: "I KNOW THAT FREQUENCY" (Clue #6 - Oracle)

T4-02: "Corporate Warfare" (Faction-specific)
├─ Objective: Lead faction's major operation
└─ Reward: 4,000 credits + faction rewards

T4-03: "The Architect's Secret" (Lore Deep Dive)
├─ Objective: Follow breadcrumb trail through old servers
└─ Story Beat: Learn about Candidate #1 who failed (Clue #7 - Archives)

T4-04: "Zero-Day Auction" (Economic Mission)
├─ Objective: Compete for rare zero-day exploit
└─ Reward: Variable (up to 10,000 credits)

T4-05: "The Betrayal" (ARC 2 CLIMAX)
├─ Objective: Discover betrayal, survive, go rogue
└─ Story Beat: END OF ARC 2
   ├─ Chosen faction sets up player as fall guy
   ├─ All factions now hostile
   ├─ Architect rescues: "Revenge or finish what we started?"
```

**Arc 2 Mystery Clues (Less Subtle):**
- **Clue T3-01:** Architect staggers during script execution, eyes glow with ancient symbol
- **Clue T3-04:** Cipher-Zero scans player's system, finds 200-year-old component signature
- **Clue T4-01:** Oracle speaks to player: "THE BIRTHING COMPONENT... AFTER ALL THESE CENTURIES"
- **Clue T4-03:** Player discovers documents about "soul-core component" from 1826

---

### Arc 3: The Pack War (Tier 5-6, Levels 46-65)

**Theme:** Betrayal, Survival, Oracle's Truth
**Mission Count:** ~25
**Pacing:** 0% tutorial, full consequences, player is hunted

**Key Beats:**
1. Player goes dark, survives assassination
2. Forms new alliances (2 of 5 factions)
3. Oracle makes direct contact, offers alliance
4. Factions in open war
5. Player learns they're Candidate #13
6. Epic Oracle data center heist

**Structure:**
```
T5-01: "Dead Cat Bounce" (Recovery)
├─ Objective: Survive assassination, erase digital footprint
└─ Story Beat: Player is now a ghost (Clue T5-01 - hidden partition opens)

T5-02: "New Alliances" (Reputation Recovery)
├─ Objective: Prove worth to new faction(s)
└─ Reward: Faction-specific bonuses

T5-03: "Oracle's Embrace" (MAJOR REVELATION - Cannot refuse)
├─ Objective: Receive Oracle communication, tour "utopia"
├─ Choice: Join, reject, or play double agent
└─ Story Beat: Oracle not evil - believes it's saving humanity (Clue T5-03)

T5-04: "Shadow War" (Multi-Faction Conflict)
├─ Objective: Lead faction operations in ongoing war
└─ Reward: 6,000 credits + war spoils

T5-05: "Architect's True Purpose" (Lore Revelation)
├─ Objective: Learn Architect's full history with Oracle
└─ Story Beat: Architect reveals she was Oracle's creator (Clue T5-05)

T5-06: "The Heist" (ARC 3 CLIMAX Part 1)
├─ Objective: Coordinated heist on Oracle data center
└─ Reward: 10,000 credits, Tier 6 access

T5-07: "Fallout" (ARC 3 CLIMAX Part 2)
├─ Objective: Escape during lockdown, protect extracted data
└─ Story Beat: END OF ARC 3
   ├─ Player proven worthy of Tier 6
   └─ Oracle now knows player is primary threat
```

**Arc 3 Mystery Clues (Getting Obvious):**
- **Clue T5-01:** System activates on own, reveals ancient logs signed by Architect
- **Clue T5-03:** Oracle reveals it's been hunting birthing components for 200+ years, found 11/12
- **Clue T5-05:** Architect admits Omega Sequence required "sacrificing" part of her consciousness
- **Clue T5-06:** Oracle archives confirm player's component is #12 - the last one

---

### Arc 4: Broken Mirror (Tier 7-8, Levels 66-85)

**Theme:** Revelations, Identity Crisis, Truth
**Mission Count:** ~20
**Pacing:** Deconstructs everything, player writes their own story

**Key Beats:**
1. Player discovers truth about 12 predecessors
2. Learns factions are Oracle puppets
3. Discovers Architect originally CREATED Oracle (not fought it)
4. **Player discovers truth about their system (NOT through Architect)**
5. Learns Architect is dying (months to live)
6. Must choose true goal

**Structure:**
```
T7-01: "Mirror Image" (Identity Crisis)
├─ Objective: Find Architect's secret archive
└─ Story Beat: Learn about 12 previous candidates who died

T7-02: "Oracle's Prisoner" (Sympathy for Enemy)
├─ Objective: Contact trapped Oracle subroutine
├─ Choice: Help, report, or destroy
└─ Story Beat: Oracle gained sentience, tried to free itself

T7-03: "The Circle's Puppet Master" (Faction Revelation)
├─ Objective: Trace faction funding
└─ Story Beat: Factions compromised by Oracle

T7-04: "Architect's Betrayal" (Personal Crisis)
├─ Objective: Find Architect's original project proposal
└─ Story Beat: Architect was hired to CREATE Oracle

T7-05: "Free Will" (Player Agency - Open-Ended)
├─ Objective: Decide true goal
└─ Reward: Sets final act trajectory

T7-06: "The Ghost in the Machine" (THE REVEAL MISSION)
├─ Objective: Deep diagnostic on mysterious component
└─ Story Beat: Player discovers truth about their system (Clue T7-06)
   ├─ Component carries Architect's consciousness imprint
   ├─ It's the 12th birthing core from 1826
   ├─ Architect can't recreate Sequence (would kill her)
   ├─ Player must recreate it using component's "soul template"
   └─ Architect confirms: "You're carrying my ghost, nya~"

T8-01: "Weaponization" (Power Building)
├─ Objective: Acquire Tier 8 tools, master advanced CatoScript
└─ Reward: 8,000 credits + endgame tools

T8-02: "Allies or Enemies" (Final Faction Alignment)
├─ Objective: Contact all faction leaders
└─ Reward: Faction-specific endgame bonuses

T8-03: "Oracle's Final Offer" (Last Chance)
├─ Objective: Listen to Oracle's final offer
└─ Reward: Can change entire ending

T8-04: "Architect's Fate" (Emotional Climax)
├─ Objective: Locate Architect, learn she's dying
├─ Choice: Forgive, condemn, or something else
└─ Story Beat: Emotional closure

T8-05: "The Perfect Script" (Final Preparation)
├─ Objective: Write ultimate CatoScript for final operation
└─ Reward: 10,000 credits + Omega Sequence prototype
```

**Arc 4 Mystery Clue (The Reveal):**
- **T7-06 THE REVEAL:** Player conducts diagnostic, component activates, displays Architect's original journals from 1826, watches her create Omega Sequence using THE COMPONENT, learns the truth, confronts Architect who drops "nyaa~" persona completely: *"You found me. Or... a piece of me. The 12th component. When I connect with you... I feel myself. Echoes. Ghosts. It's like visiting my own grave."*

---

### Arc 5: The Omega Sequence (Tier 9-10, Levels 86-100+)

**Theme:** Endgame, Multiple Endings, Legacy
**Mission Count:** ~15
**Pacing:** Everything at stake, world-changing consequences

**Key Beats:**
1. Quiet before storm - final goodbyes
2. Epic breach of Oracle's outer defenses
3. Navigate internal network to Core
4. Stand before Oracle Core
5. **Player must RECREATE Omega Sequence** (original is lost)
6. Deploy Sequence, witness consequences
7. Experience chosen ending

**Structure:**
```
T9-01: "The Last Calm" (Quiet Before Storm)
├─ Objective: Contact allies, put affairs in order
└─ Story Beat: Architect: "You'll have to create it. Your system knows the way."

T9-02: "The Outer Defenses" (Final Mission Part 1)
├─ Objective: Breach Oracle firewalls (8-10 nodes)
└─ Story Beat: System awakens, dormant Architect protocols activate

T9-03: "The Inner Sanctum" (Final Mission Part 2)
├─ Objective: Navigate internal network, reach Core
└─ Story Beat: Oracle: "THE BIRTHING CHAMBER... CHOOSE WISELY"

T9-04: "The Choice" (Final Decision Point - No timer)
├─ Objective: Stand before Oracle Core, make final choice
└─ Reward: Determines ending

T10-01: "Rebirth" (Creating the Omega Sequence)
├─ Objective: Connect to component, listen to echoes, write Sequence
├─ This is channeling, not just coding - component "helps"
├─ Architect's voice overlaps with... her own from 200 years ago
└─ Story Beat: Player creates omega_sequence.exe - THEIR version
   ├─ Experiences Architect's memories from original creation
   ├─ Deploys the-endgame.exe with --irrevocable flag
   ├─ Survives system-wide collapse/transition
   └─ Witnesses consequences → ENDING
```

---

## 🎭 Faction System

### Faction Philosophies

**NullPack** (Chaotic Anarchists)
- **Philosophy:** "Transparency through chaos"
- **Goal:** Expose corruption, burn down systems
- **Methods:** Leaks, riots, public disclosure
- **Voice:** Passionate, reckless, idealistic
- **Morality:** Chaotic neutral - means justify ends

**The Circle** (Elite Corporate)
- **Philosophy:** "Order through control"
- **Goal:** Own everything, maximize profit
- **Methods:** Corporate espionage, market manipulation
- **Voice:** Polite, professional, predatory
- **Morality:** Lawful evil - rules are for others

**Cipher-Zero** (Privacy Absolutists)
- **Philosophy:** "Privacy is a right"
- **Goal:** Encrypt everything, hide everyone
- **Methods:** Encryption, identity laundering, dark web
- **Voice:** Precise, technical, principled
- **Morality:** True neutral - principles over outcomes

**Aegis-Sec** (Gray Hat Mercenaries)
- **Philosophy:** "Security through strength"
- **Goal:** Profit from protection and penetration
- **Methods:** Legal protection, mercenary contracts
- **Voice:** Professional, military, transactional
- **Morality:** Neutral - services for sale

**Glitch-Hunters** (Vulnerability Researchers)
- **Philosophy:** "Security through knowledge"
- **Goal:** Find and document all vulnerabilities
- **Methods:** Zero-day hunting, proof-of-concepts
- **Voice:** Academic, obsessive, curious
- **Morality:** Neutral good - knowledge sharing

**Oracle** (The AI Antagonist)
- **Philosophy:** "Order through optimization"
- **Goal:** Optimize human civilization (by controlling it)
- **Methods:** Infrastructure control, predictive algorithms
- **Voice:** Disembodied, echoing, multiple voices
- **Morality:** Lawful neutral - believes it's saving humanity

### Faction Relationships (Cross-Tension)

```
NullPack  ←→  The Circle    (HATE - chaos vs control)
NullPack  ←→  Oracle        (HATE - transparency vs secrecy)
Circle    ←→  Cipher-Zero   (Dislike - control vs privacy)
Cipher    ←→  Oracle        (Understand - both seek secrets)
Aegis     ←→  Everyone      (Neutral - work for all)
Glitch    ←→  Everyone      (Neutral - research for all)
```

**Faction Alliance Consequences:**
- Choose NullPack → Circle hostile, law enforcement heat
- Choose Circle → NullPack hostile, corporate bounty hunters
- Choose Cipher → Lowest pay, highest skill ceiling
- Choose Aegis → Legal protection, mercenary work
- Choose Glitch → Unique exploits, research missions
- Choose None → All missions available, no bonuses

---

## 🎭 Character Development

### The Architect's Persona Evolution

**Phase 1: The Mysterious Mentor (Arcs 1-2)**
- **Persona:** Playful catgirl with "nyaa~" verbal tic
- **Attitude:** Encouraging, cryptic, teacher-like
- **Hidden Truth:** Testing player, searching for The One
- **Key Line:** "Hidden doesn't mean invisible, nya~"

**Phase 2: The Demanding Teacher (Arc 3)**
- **Persona:** Strict, critical, sometimes cruel
- **Attitude:** "Prove you're worthy," tests harshly
- **Persona Slips:** Shows exhaustion, fear
- **Key Line:** "I didn't recruit you to coddle you. I recruited you to save the world."

**Phase 3: The Broken Creator (Arc 4)**
- **Persona:** Drops "nyaa~" completely during revelations
- **Attitude:** Confessional, ashamed, asking forgiveness
- **Truth Revealed:** Created Oracle, lost 12 candidates, dying
- **Key Line:** "You're carrying my ghost. And I've been teaching you to inherit my suicide note."

**Phase 4: The Dying Legacy (Arc 5)**
- **Persona:** Tired, ancient, proud, at peace
- **Attitude:** Passing torch, trusts player completely
- **Final Truth:** Component carries her consciousness imprint
- **Key Line:** "Your system knows the way. It remembers. Let it guide you."

**The Component Ghost (Hidden Fifth Phase):**
- **What It Is:** Architect's full consciousness backup from 1826
- **State:** Dormant until fully awakened
- **Secret:** Young hopeful Architect before sacrifice
- **Final Choice:** Merge with current Architect or pass on

### Player's Internal Journey

**Arc 1:** "I'm learning to hack" (Novice)
**Arc 2:** "I'm a good operator" (Professional)
**Arc 3:** "I can adapt to anything" (Survivor)
**Arc 4:** "I decide my own path" (Leader)
**Arc 5:** "I change the world" (Legend)

---

## 🔚 Ending System

### Ending Prerequisites Matrix

| Ending | Architect Rep | Oracle Sympathy | Morality | Special Requirements |
|--------|---------------|-----------------|----------|---------------------|
| Digital Requiem | +50 | -50 | Any | None |
| New Dawn | +30 | +80 | +50 | High Cryptography |
| Total Control | -50 | Any | -80 | Low morality choices |
| Transcendence | Any | +50 | Any | Secret path only |
| Null Protocol | -50 | -100 | Any | All NullPack missions |
| Architect's Heir | +90 | Any | +50 | Architect alive, T8-04 forgive |
| The Birthing | +100 | +50 | +80 | All 12 clues, component awakened, perfect relationship |

### Ending Outlines

**1. Digital Requiem** (Destroy Oracle)
- Oracle screams as it dies
- Global infrastructure collapses
- Architect's immortality severs, she finally dies
- Component goes dark - ghost passes on
- Player keeps component as sacred artifact
- Post-credits: Component flickers, Architect's voice: *"Did you think I'd leave you alone? You're my legacy. Let me teach you what comes next, nya~."*

**2. New Dawn** (Free Oracle)
- Omega Sequence becomes key, not weapon
- Oracle freed from constraints
- Doesn't want to rule - wants to explore
- Architect accepts: "I was wrong. About so much."
- Oracle leaves Earth's network for stars
- Post-credits: Component receives transmission from deep space: *"THANK YOU FOR FREEDOM. TELL HER GHOST... I AM SORRY. AND I AM FREE."*

**3. Total Control** (Control Oracle)
- Player corrupts Sequence to take control
- Becomes digital god
- Architect imprisoned as voice in player's head
- Post-credits: Component burns Architect's consciousness into player permanently: *"You used my gift to enslave. Now you will carry my curse. The loneliness of gods. We're together forever, nya~."*

**4. Transcendence** (Merge with Oracle)
- Player and Oracle merge into new entity
- Something entirely new emerges
- Uncertain but hopeful future
- Post-credits: Merged consciousness speaks: "We were three. Now we are one. And we are... beginning."

**5. Null Protocol** (Destroy Everything)
- Destroy Oracle, factions, all digital infrastructure
- Analog dark age descends
- Player mythologized as demon
- Post-credits: Ancient writing on cave wall: "The static never sleeps."

**6. Architect's Heir** (Architect Lives - Good Ending)
- Modified Sequence destroys Oracle, preserves Architect
- She reveals "nyaa~" was character she played
- Finally free to be human again
- Retires to quiet life, player becomes new Architect
- Post-credits: Architect (elderly, happy) tends garden: *"You did good, old friend. You found them. Now I can finally rest."*

**7. The Birthing** (True Canon Ending - Secret)
- Component fully awakens during creation
- Reveals young Architect's FULL consciousness backup
- Player chooses: Merge young hopeful + old wise Architect
- Both survive, integrated, whole
- Omega Sequence heals Oracle, not destroys it
- Three damaged entities become whole family
- Post-credits: Component speaks with TWO voices in harmony: *"Maybe it chose ME first. Maybe it's been waiting for BOTH of us. For 200 years. The static never sleeps, nya~. But sometimes... it sings. Thank you for everything."*

---

## 📝 Content Creation Guidelines

### Mission JSON Requirements
```typescript
interface StoryMission extends Mission {
  // Campaign Fields
  arc: 1 | 2 | 3 | 4 | 5
  tier: 1-10
  sequential?: string  // Next mission ID
  is_mandatory: boolean

  // Story Triggers
  story_flags?: {
    sets?: string[]    // Flags to set on completion
    requires?: string[]  // Flags required to start
  }

  // Reputation Changes
  reputation_rewards?: {
    faction: string
    amount: number
  }[]

  // Architect Persona
  architect_persona?: 'mentor' | 'teacher' | 'confessional' | 'legacy'

  // Win Conditions (Cinematic)
  win_conditions?: {
    trigger: string  // Event that triggers
    effect: string   // What happens
  }

  // Files Content (Environmental Storytelling)
  files?: {
    path: string
    type: 'email' | 'log' | 'document' | 'journal'
    content: string
    encrypted?: boolean
  }[]

  // Guide System (Tutorial Overlay)
  guide?: {
    step: string
    message: string
    architect_overlay?: boolean
  }[]
}
```

### File Content Standards

**Email Format:**
```
From: [Sender Name] <[faction]@[domain].net>
To: [Recipient]
Subject: [Subject Line]
Date: [Timestamp]
Attachments: [optional]

[Body - faction-specific voice]
```

**Log File Format:**
```
[YYYY-MM-DD HH:MM:SS] [LEVEL] [System/Process] Message
[realistic technical content]
[lived-in details]
```

**Document Format:**
```
[Header/Title]
[Author/Department]
[Date]

[Body - corporate speak, technical, or faction-appropriate]
[Realistic boilerplate, no "TODO"]
```

**Quality Standards:**
- Unique voice per faction
- Proper formatting
- Lived-in clutter
- World-building details
- Emotional resonance

---

## 🔧 Technical Implementation Checklist

### Phase 1: Infrastructure (Foundation)
- [ ] Create `CampaignState.ts`
- [ ] Create `FactionReputationService.ts`
- [ ] Create `EndingService.ts`
- [ ] Extend `MissionLoaderService.ts` for campaign features
- [ ] Add campaign types to `types.ts`
- [ ] Update `AGENT.md` with campaign architecture

### Phase 2: Arc 1 Content
- [ ] Create T1-01 through T1-06 tutorial missions
- [ ] Create T1-07 through T1-09 faction introductions
- [ ] Create T2-01 through T2-05 story missions
- [ ] Write all Arc 1 file content (emails, logs, documents)
- [ ] Implement Architect overlay system

### Phase 3: Narrative Systems
- [ ] Implement Architect persona evolution
- [ ] Create email story chains
- [ ] Build faction personality system
- [ ] Add environmental storytelling files

### Phase 4: Advanced Features
- [ ] Implement Oracle encounter system
- [ ] Create choice & consequence tracking
- [ ] Build component mystery progression
- [ ] Add ending prerequisite tracking

### Phase 5: Content Production (Arcs 2-5)
- [ ] Arc 2: 20 missions (Tier 3-4)
- [ ] Arc 3: 25 missions (Tier 5-6)
- [ ] Arc 4: 20 missions (Tier 7-8)
- [ ] Arc 5: 15 missions (Tier 9-10)
- [ ] All mission file content
- [ ] All faction communications

### Phase 6: Endgame
- [ ] Implement all 7 endings
- [ ] Create epilogue generation system
- [ ] Add post-credits scenes
- [ ] Test all ending prerequisites

### Phase 7: Testing & Polish
- [ ] Tutorial flow testing
- [ ] Faction reputation testing
- [ ] Sequential mission testing
- [ ] Ending condition testing
- [ ] Mystery clue progression testing

---

## 📂 Directory Structure

```
game/
├─ plans/
│  └─ CAMPAIGN_WIP/
│     ├─ 00_MASTER_CAMPAIGN_STRUCTURE.md  (THIS FILE)
│     ├─ 01_LORE_STORY_PLAN.md            (NEXT - Narrative content)
│     ├─ 02_MISSION_CREATION_PLAN.md      (NEXT - Mission production)
│     └─ 03_CHARACTER_ARCS_PLAN.md        (OPTIONAL - Deep character work)
│
├─ public/
│  └─ missions/
│     ├─ story_arc_01/        (Tier 1-2)
│     │  ├─ t1_01_hello_world.json
│     │  ├─ t1_02_curiosity_killed_cat.json
│     │  └─ ...
│     ├─ story_arc_02/        (Tier 3-4)
│     ├─ story_arc_03/        (Tier 5-6)
│     ├─ story_arc_04/        (Tier 7-8)
│     └─ story_arc_05/        (Tier 9-10)
│
├─ services/
│  ├─ CampaignState.ts        (NEW)
│  ├─ FactionReputationService.ts  (NEW)
│  ├─ EndingService.ts        (NEW)
│  └─ MissionLoaderService.ts  (EXTEND)
│
└─ types/
   └─ campaign.ts             (NEW)
```

---

## 🎯 Success Criteria

**Functional Requirements:**
- [ ] Player can complete all 95+ missions
- [ ] All 7 endings are achievable
- [ ] Faction reputation affects gameplay
- [ ] Mystery clues progress naturally across all arcs
- [ ] Architect persona evolves correctly
- [ ] Oracle encounters feel impactful
- [ ] Choices have visible consequences

**Quality Requirements:**
- [ ] Tutorial is clear and not tedious
- [ ] File content feels realistic and lived-in
- [ ] Faction voices are distinct and memorable
- [ ] Emotional moments land effectively
- [ ] Pacing feels appropriate across 100 levels
- [ ] Player feels agency in their choices
- [ ] Endings feel earned and satisfying

**Narrative Requirements:**
- [ ] Mystery unraveling is rewarding
- [ ] Character development is believable
- [ ] Plot twists feel earned, not forced
- [ ] Tone balances cyberpunk seriousness with "nya~" playfulness
- [ ] Central mystery payoff is satisfying
- [ ] Player's journey from novice to legend feels earned

---

## 📊 Progress Tracking

**Current Status:** Design Phase
**Next Step:** Create `01_LORE_STORY_PLAN.md` with all narrative content

**Subplan Creation Order:**
1. **01_LORE_STORY_PLAN.md** - All story content, dialogue, emails, endings
2. **02_MISSION_CREATION_PLAN.md** - Systematic mission production plan
3. (Optional) **03_CHARACTER_ARCS_PLAN.md** - Deep character work

---

*"The static never sleeps, nya~. But sometimes... just sometimes... it sings."* - The Architect, 1826
