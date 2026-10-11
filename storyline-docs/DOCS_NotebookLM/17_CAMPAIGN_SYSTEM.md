# Campaign System

## Overview
The Kernel-Panic campaign is a comprehensive 5-arc narrative framework spanning Tiers 1-10 (Levels 1-100+) that tells the story of a player's evolution from novice hacker to the only operator capable of deciding the fate of the digital world. At its heart lies a mysterious component in the player's system - a 200-year-old "birthing core" carrying the consciousness of The Architect, the creator of Oracle, an AI that controls 80% of the world's digital infrastructure.

## Key Concepts

### The Central Mystery
**The Player's Secret:**
- Player's system contains the 12th "birthing core" from 1826
- This core carries The Architect's consciousness imprint
- It was installed by a shady repair shop (CyberDoc's) - player got a "great deal"
- The Architect has been searching for this component for 200+ years
- Player is "Candidate #13" - twelve previous candidates died
- Player is "The One" not because of skill, but because their system can birth the Omega Sequence

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

## How It Works

### State Management Systems

#### Campaign Progress State
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

#### Faction Reputation System
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
```

**Reputation Thresholds:**
- **very_hostile** (-100 to -50): May send assassins
- **hostile** (-50 to -20): Blocked missions
- **neutral** (-20 to +20): Basic access
- **friendly** (+20 to +50): Discounts/bonuses
- **very_friendly** (+50 to +80): Special missions
- **devoted** (+80 to +100): Faction specials

#### Ending System
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
```

**Available Endings:**
1. **Digital Requiem** - Destroy Oracle (Architect dies)
2. **New Dawn** - Free Oracle to explore the cosmos
3. **Total Control** - Enslave Oracle, become digital god
4. **Transcendence** - Merge with Oracle into new entity
5. **Null Protocol** - Destroy everything, analog dark age
6. **Architect's Heir** - Destroy Oracle, save Architect (Good ending)
7. **The Birthing** - True canon: Merge young + old Architect, heal Oracle (All secrets)

### Faction System

#### Faction Philosophies

**NullPack** (Chaotic Anarchists)
- **Philosophy:** "Transparency through chaos"
- **Goal:** Expose corruption, burn down systems
- **Methods:** Leaks, riots, public disclosure
- **Voice:** Passionate, reckless, idealistic, lowercase
- **Morality:** Chaotic neutral - means justify ends
- **Sign-off:** "stay frosty."

**The Circle** (Elite Corporate)
- **Philosophy:** "Order through control"
- **Goal:** Own everything, maximize profit
- **Methods:** Corporate espionage, market manipulation
- **Voice:** Polite, professional, predatory
- **Morality:** Lawful evil - rules are for others
- **Sign-off:** "We look forward to a productive arrangement."

**Cipher-Zero** (Privacy Absolutists)
- **Philosophy:** "Privacy is a right"
- **Goal:** Encrypt everything, hide everyone
- **Methods:** Encryption, identity laundering, dark web
- **Voice:** Precise, technical, principled, ALL CAPS emphasis
- **Morality:** True neutral - principles over outcomes
- **Sign-off:** "Privacy is not a privilege."

**Aegis-Sec** (Gray Hat Mercenaries)
- **Philosophy:** "Security through strength"
- **Goal:** Profit from protection and penetration
- **Methods:** Legal protection, mercenary contracts
- **Voice:** Professional, military, transactional
- **Morality:** Neutral - services for sale
- **Sign-off:** "H7 out."

**Glitch-Hunters** (Vulnerability Researchers)
- **Philosophy:** "Security through knowledge"
- **Goal:** Find and document all vulnerabilities
- **Methods:** Zero-day hunting, proof-of-concepts
- **Voice:** Academic, obsessive, curious, uses :3
- **Morality:** Neutral good - knowledge sharing
- **Sign-off:** "bug hunting!! :3"

**Oracle** (The AI Antagonist)
- **Philosophy:** "Order through optimization"
- **Goal:** Optimize human civilization (by controlling it)
- **Methods:** Infrastructure control, predictive algorithms
- **Voice:** Disembodied, echoing, multiple voices, ALL CAPS
- **Morality:** Lawful neutral - believes it's saving humanity
- **Sign-off:** None (eternal)

### The Architect's Persona Evolution

**Phase 1: The Mysterious Mentor (Arcs 1-2)**
- **Persona:** Playful catgirl with "nya~" verbal tic
- **Attitude:** Encouraging, cryptic, teacher-like
- **Hidden Truth:** Testing player, searching for The One
- **Key Line:** "Hidden doesn't mean invisible, nya~"

**Phase 2: The Demanding Teacher (Arc 3)**
- **Persona:** Strict, critical, sometimes cruel
- **Attitude:** "Prove you're worthy," tests harshly
- **Persona Slips:** Shows exhaustion, fear
- **Key Line:** "I didn't recruit you to coddle you. I recruited you to save the world."

**Phase 3: The Broken Creator (Arc 4)**
- **Persona:** Drops "nya~" completely during revelations
- **Attitude:** Confessional, ashamed, asking forgiveness
- **Truth Revealed:** Created Oracle, lost 12 candidates, dying
- **Key Line:** "You're carrying my ghost. And I've been teaching you to inherit my suicide note."

**Phase 4: The Dying Legacy (Arc 5)**
- **Persona:** Tired, ancient, proud, at peace
- **Attitude:** Passing torch, trusts player completely
- **Final Truth:** Component carries her consciousness imprint
- **Key Line:** "Your system knows the way. It remembers. Let it guide you."

### Arc Structure

#### Arc 1: The Kitten's Claws (Tier 1-2, Levels 1-25)
**Theme:** Tutorial & Introduction
**Mission Count:** ~15
**Pacing:** 40% tutorial hand-holding, 60% early story

**Key Beats:**
1. Tutorial: Linux basics, file discovery, credential harvesting
2. Meet all 5 factions (NullPack, Circle, Glitch, Cipher, Aegis)
3. First CatoScript script
4. **Major Revelation:** Oracle exists, controls 80% of infrastructure
5. **Clue Escalation:** Architect seems affected by player's system (very subtle)

#### Arc 2: Alleyways & Shadows (Tier 3-4, Levels 26-45)
**Theme:** Underground Economy & Faction Choice
**Mission Count:** ~20
**Pacing:** 20% tutorial, 80% player-driven with consequences

**Key Beats:**
1. Player enters true underground economy
2. All factions compete for services
3. **Architect's reactions to player's system intensify**
4. Player must choose faction alliance (T3-07)
5. **First Oracle Encounter:** Oracle speaks directly
6. Chosen faction betrays player at arc end

#### Arc 3: The Pack War (Tier 5-6, Levels 46-65)
**Theme:** Betrayal, Survival, Oracle's Truth
**Mission Count:** ~25
**Pacing:** 0% tutorial, full consequences, player is hunted

**Key Beats:**
1. Player goes dark, survives assassination
2. Forms new alliances (2 of 5 factions)
3. **Oracle makes direct contact**, offers alliance
4. Factions in open war
5. Player learns they're Candidate #13
6. Epic Oracle data center heist

#### Arc 4: Broken Mirror (Tier 7-8, Levels 66-85)
**Theme:** Revelations, Identity Crisis, Truth
**Mission Count:** ~20
**Pacing:** Deconstructs everything, player writes their own story

**Key Beats:**
1. Player discovers truth about 12 predecessors
2. Learns factions are Oracle puppets
3. Discovers Architect originally CREATED Oracle (not fought it)
4. **Player discovers truth about their system** (NOT through Architect)
5. Learns Architect is dying (months to live)
6. Must choose true goal

#### Arc 5: The Omega Sequence (Tier 9-10, Levels 86-100+)
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

## Important Rules

### Story Flag System
- Flags are binary triggers that track narrative progress
- Set flags on mission completion or story events
- Some missions require specific flags to be available
- Flags are persistent across sessions

### Mission Story Integration
```typescript
interface StoryMission {
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
}
```

### Component Mystery Progression
- Clues are scattered across missions in specific tiers
- Clues become less subtle as the campaign progresses
- Some clues are in environmental storytelling (files, emails)
- Other clues are in Architect dialogue or system behavior
- Final clue (T7-06) is THE REVEAL mission

## Relationships

### Related Systems
- **MissionLoaderService**: Loads story missions with campaign features
- **FactionReputationService**: Tracks faction relationships
- **EndingService**: Calculates available endings based on choices
- **CampaignStateService**: Manages campaign progress state
- **OracleEncounterService**: Handles Oracle dialogue encounters
- **ScriptEventService**: Triggers story events during missions

### Data Flow
1. Player completes mission → Mission sets story flags
2. Story flags trigger → CampaignState updates
3. Reputation changes → FactionReputationService updates
4. Major choices → EndingService recalculates available endings
5. Arc completion → Next arc missions unlock
6. Final mission → Ending determined by cumulative choices

## Common Pitfalls

### Missing Story Flags
Forgetting to set flags on mission completion breaks the narrative chain. Always check if a completed mission should set flags.

### Reputation Overflow
Setting reputation changes too high can skip important character development moments. Use reputation thresholds to gate content.

### Breaking Arc Flow
Allowing players to skip mandatory missions breaks the narrative. Always enforce `is_mandatory` flag on key story missions.

### Clue Pacing
Revealing the mystery too early ruins the payoff. Follow the 12-clue escalation table:
- **Arc 1:** Very subtle hints (Architect's reactions)
- **Arc 2:** Less subtle (system behavior, Oracle notices)
- **Arc 3:** Getting obvious (component activates, Oracle speaks)
- **Arc 4:** THE REVEAL (player learns truth independently)

### Ending prerequisites
Make sure ending prerequisites are achievable and not contradictory. Test all 7 endings for logical consistency.

## Related Documents
- [Project Overview](00_PROJECT_OVERVIEW.md) - Campaign introduction
- [Game Architecture](01_GAME_ARCHITECTURE.md) - State management
- [Mission Objectives](05_MISSION_OBJECTIVES.md) - Mission structure
- [The Architect](09_THE_ARCHITECT_SYSTEM.md) - Tutorial overlays
- [Campaign Master Plan](../CAMPAIGN_WIP/00_MASTER_CAMPAIGN_BP.md) - Detailed campaign blueprint
- [Lore & Story Guide](../CAMPAIGN_WIP/01_LORE_STORY_GUIDE.md) - All narrative content
