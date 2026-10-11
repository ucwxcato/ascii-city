# PHASE 0: Foundation Checklist
## Week 1 - Infrastructure Setup

> **Before writing a single mission, the systems must exist to support them.**

**Status:** [✓] NOT STARTED | [ ] IN PROGRESS | [✓] COMPLETE

---

## 📋 Overview

Phase 0 establishes all core infrastructure required for the campaign system. No missions should be written until these systems are in place and verified.

**Estimated Duration:** 1 week
**Deliverables:** 4 services, 1 extended service, 4 type files, 1 debug component, 7 directories

---

## 0.1 Core Services

### [x] Service 1: CampaignStateService.ts

**File:** `game/services/campaign/CampaignState.ts`

**Purpose:** Track arc progression, story flags, completed missions

**Interface Definition:**

```typescript
export interface CampaignState {
  // Progression
  currentArc: 1 | 2 | 3 | 4 | 5
  currentTier: number
  completedMissions: string[]
  activeMission?: string

  // Story Flags
  storyFlags: {
    metArchitect: boolean
    learnedAboutOracle: boolean
    firstOracleEncounter: boolean
    discoveredComponentClue_count: number
    componentFullyAwakened: boolean
    architectIdentityRevealed: boolean
    choseFactionAlliance: string | null
    wasBetrayedByFaction: boolean
    oracleFinalContact: boolean
    omegaSequenceCreated: boolean
  }

  // Player Identity
  candidateNumber: 13  // Player is Candidate #13
  predecessorsFailed: 12
  componentCluesDiscovered: string[]
}
```

**Functions to Implement:**
- [x] `setStoryFlag(flag: string, value: boolean)`
- [x] `getStoryFlag(flag: string): boolean`
- [x] `completeMission(missionId: string)`
- [x] `isMissionCompleted(missionId: string): boolean`
- [x] `advanceArc()`
- [x] `getCurrentProgress(): number` (0-100 percentage)

**Verification:**
- [x] TypeScript compiles without errors
- [x] State persists between sessions (save/load)
- [x] Story flags can be set and retrieved
- [x] Mission completion tracking works

---

### [x] Service 2: FactionReputationService.ts

**File:** `game/services/campaign/FactionReputationService.ts`

**Purpose:** Track faction reputation (-100 to +100), handle alliances

**Interface Definition:**

```typescript
export interface FactionReputation {
  // All 6 factions
  nullpack: number        // -100 to +100
  the_circle: number
  cipher_zero: number
  aegis_sec: number
  glitch_hunters: number
  oracle: number

  // Alliance tracking
  currentAlliance: 'none' | 'nullpack' | 'circle' | 'cipher' | 'aegis' | 'glitch'
  formerAlliance?: string
  allianceLocked: boolean
}

export interface ReputationThresholds {
  very_hostile: -100 to -50   // May send assassins
  hostile: -50 to -20          // Blocked missions
  neutral: -20 to +20          // Basic access
  friendly: +20 to +50         // Discount/bonuses
  very_friendly: +50 to +80    // Special missions
  devoted: +80 to +100         // Faction specials
}

export type FactionStatus =
  | 'very_hostile'
  | 'hostile'
  | 'neutral'
  | 'friendly'
  | 'very_friendly'
  | 'devoted';
```

**Functions to Implement:**
- [x] `getReputation(faction: string): number`
- [x] `setReputation(faction: string, value: number)`
- [x] `modifyReputation(faction: string, amount: number)`
- [x] `getStatus(faction: string): FactionStatus`
- [x] `getAlliance(): string`
- [x] `setAlliance(faction: string)`
- [x] `breakAlliance()` (moves to formerAlliance)
- [x] `canAccessMissions(faction: string): boolean`

**Verification:**
- [x] Reputation changes apply correctly (clamped -100 to +100)
- [x] Threshold events trigger at right values
- [x] Alliance choice gates missions properly
- [x] Breaking alliance moves to formerAlliance

---

### [x] Service 3: EndingService.ts

**File:** `game/services/campaign/EndingService.ts`

**Purpose:** Track ending prerequisites, calculate achievable endings

**Interface Definition:**

```typescript
export type EndingType =
  | 'digital_requiem'      // Destroy Oracle
  | 'new_dawn'             // Free Oracle
  | 'total_control'        // Control Oracle (Villain)
  | 'transcendence'        // Merge with Oracle (Secret)
  | 'null_protocol'        // Destroy everything
  | 'architects_heir'      // Architect lives
  | 'the_birthing';        // True canon

interface EndingState {
  architectRelationship: number  // -100 to +100
  oracleSympathy: number         // -100 to +100
  moralityScore: number          // -100 to +100
  highCryptography: boolean      // Skill level 50+
  secretPathUnlocked: boolean
  allNullPackComplete: boolean
  t8_04Forgave: boolean
  architectSaved: boolean
  allComponentCluesFound: number // 0-12
  componentFullyAwakened: boolean

  achievableEndings: EndingType[]
  finalEnding: EndingType | null
}

interface EndingPrerequisite {
  variable: string;
  current: any;
  required: any;
  met: boolean;
}
```

**Functions to Implement:**
- [x] `calculateEndings(): EndingType[]`
- [x] `isEndingAchievable(ending: EndingType): boolean`
- [x] `getEndingPrerequisites(ending: EndingType): EndingPrerequisite[]`
- [x] `getMissingPrerequisites(ending: EndingType): string[]`
- [x] `triggerEnding(ending: EndingType)`

**Verification:**
- [x] All 7 endings can be calculated
- [x] Prerequisites checked correctly
- [x] Missing prerequisites reported accurately
- [x] Ending triggers work

---

### [x] Service 4: OracleEncounterService.ts

**File:** `game/services/campaign/OracleEncounterService.ts`

**Purpose:** Handle Oracle dialogue system, random encounters

**Interface Definition:**

```typescript
interface OracleDialogue {
  speaker: 'ORACLE' | 'SYSTEM';
  text: string;
  typingEffect?: boolean;
  color?: string;
}

interface OracleEncounter {
  id: string;
  trigger: string;  // What triggers this encounter
  dialogues: OracleDialogue[];
  choices?: OracleChoice[];
  flagsSet?: string[];
}

interface OracleChoice {
  text: string;
  consequence: string;  // What happens
  flags?: string[];
  reputationChanges?: { faction: string; amount: number }[];
}
```

**Functions to Implement:**
- [x] `triggerEncounter(encounterId: string)`
- [x] `getRandomEncounter(): OracleEncounter | null`
- [x] `hasMetOracle(): boolean`
- [x] `setOracleEncountered(value: boolean)`

**Verification:**
- [x] Encounters trigger correctly
- [x] Dialogue system works
- [x] Choices affect game state

---

### [x] Service Extension: MissionLoaderService.ts

**File:** `game/services/mission/MissionLoaderService.ts` (EXTEND EXISTING)

**Purpose:** Add campaign-specific property mapping

**Additions Required:**

```typescript
// Add imports
import { CampaignState } from '../campaign/CampaignState';
import { FactionReputationService } from '../campaign/FactionReputationService';

// Add to mission completion handler
const handleMissionCompletion = (mission: Mission, gameState: GameState) => {
  // 1. Set story flags
  if (mission.mission_meta.story_flags?.sets) {
    mission.mission_meta.story_flags.sets.forEach(flag => {
      gameState.campaignState.storyFlags[flag] = true;
    });
  }

  // 2. Apply reputation rewards
  if (mission.reputation_rewards) {
    mission.reputation_rewards.forEach(reward => {
      const currentRep = playerState.factionReputation[reward.faction] || 0;
      playerState.factionReputation[reward.faction] =
        Math.max(-100, Math.min(100, currentRep + reward.amount));
    });
  }

  // 3. Track completion
  gameState.campaignState.completedMissions.push(mission.mission_meta.id);

  // 4. Award credits
  playerState.credits += mission.reward_credits || 0;
};
```

**Verification:**
- [x] Story flags set on mission complete
- [x] Reputation applies (clamped correctly)
- [x] Completion tracked
- [x] Credits awarded

---

## 0.2 Type Definitions

### [x] Create types/campaign/ directory

**Directory:** `game/types/campaign/`

**Files to Create:**

#### [x] state.ts
```typescript
export interface CampaignState {
  currentArc: 1 | 2 | 3 | 4 | 5;
  currentTier: number;
  completedMissions: string[];
  activeMission?: string;
  storyFlags: StoryFlags;
  candidateNumber: number;
  predecessorsFailed: number;
  componentCluesDiscovered: string[];
}

export interface StoryFlags {
  metArchitect: boolean;
  learnedAboutOracle: boolean;
  firstOracleEncounter: boolean;
  discoveredComponentClue_count: number;
  componentFullyAwakened: boolean;
  architectIdentityRevealed: boolean;
  choseFactionAlliance: string | null;
  wasBetrayedByFaction: boolean;
  oracleFinalContact: boolean;
  omegaSequenceCreated: boolean;
}
```

#### [x] factions.ts
```typescript
export type FactionId = 'nullpack' | 'the_circle' | 'cipher_zero' | 'aegis_sec' | 'glitch_hunters' | 'oracle';

export type AllianceType = 'none' | FactionId;

export type FactionStatus =
  | 'very_hostile'    // -100 to -50
  | 'hostile'         // -50 to -20
  | 'neutral'         // -20 to +20
  | 'friendly'        // +20 to +50
  | 'very_friendly'   // +50 to +80
  | 'devoted';        // +80 to +100

export interface FactionReputation {
  nullpack: number;
  the_circle: number;
  cipher_zero: number;
  aegis_sec: number;
  glitch_hunters: number;
  oracle: number;
  currentAlliance: AllianceType;
  formerAlliance?: AllianceType;
  allianceLocked: boolean;
}

export interface ReputationReward {
  faction: FactionId;
  amount: number;
}
```

#### [x] endings.ts
```typescript
export type EndingType =
  | 'digital_requiem'
  | 'new_dawn'
  | 'total_control'
  | 'transcendence'
  | 'null_protocol'
  | 'architects_heir'
  | 'the_birthing';

export interface EndingState {
  architectRelationship: number;
  oracleSympathy: number;
  moralityScore: number;
  highCryptography: boolean;
  secretPathUnlocked: boolean;
  allNullPackComplete: boolean;
  t8_04Forgave: boolean;
  architectSaved: boolean;
  allComponentCluesFound: number;
  componentFullyAwakened: boolean;
  achievableEndings: EndingType[];
  finalEnding: EndingType | null;
}

export interface EndingPrerequisite {
  variable: string;
  current: any;
  required: any;
  met: boolean;
}

export interface EndingDefinition {
  id: EndingType;
  name: string;
  description: string;
  prerequisites: EndingPrerequisite[];
  isSecret: boolean;
}
```

#### [x] story-flags.ts
```typescript
export type StoryFlagId =
  | 'metArchitect'
  | 'learnedAboutOracle'
  | 'firstOracleEncounter'
  | 'discoveredComponentClue_count'
  | 'componentFullyAwakened'
  | 'architectIdentityRevealed'
  | 'choseFactionAlliance'
  | 'wasBetrayedByFaction'
  | 'oracleFinalContact'
  | 'omegaSequenceCreated'
  | 'completedTutorial'
  | 'candidateNumberRevealed'
  | 'knowsComponentIsBirthingCore'
  | 'finalChoiceMade';

export interface StoryFlagDefinition {
  id: StoryFlagId;
  name: string;
  description: string;
  type: 'boolean' | 'number' | 'string';
  defaultValue: any;
  arcIntroduced?: number;
}
```

---

## 0.3 Directory Structure

### [x] Create campaign directories

```
game/
├─ services/
│  └─ campaign/                    ← Campaign-specific services
│     ├─ CampaignState.ts
│     ├─ FactionReputationService.ts
│     ├─ EndingService.ts
│     └─ OracleEncounterService.ts
│
├─ types/
│  └─ campaign/                    ← Campaign TypeScript types
│     ├─ state.ts
│     ├─ factions.ts
│     ├─ endings.ts
│     └─ story-flags.ts
│
└─ public/
   └─ missions/
      ├─ campaign/                 ← Story campaign missions
      │  ├─ arc_01/               (Tier 1-2, Levels 1-25)
      │  ├─ arc_02/               (Tier 3-4, Levels 26-45)
      │  ├─ arc_03/               (Tier 5-6, Levels 46-65)
      │  ├─ arc_04/               (Tier 7-8, Levels 66-85)
      │  └─ arc_05/               (Tier 9-10, Levels 86-100+)
      │
      └─ contracts/                ← Procedural missions (existing)
```

**Verification:**
- [x] All directories created
- [x] `npm run predev` runs successfully
- [x] `mission_index.json` regenerates correctly

---

## 0.4 CRITICAL VFS RULES

> **READ THIS BEFORE WRITING ANY MISSIONS**

### [x] VFS Rule #1: NEVER Use Arrays for Children

```json
// ❌ WRONG - Will crash the system!
"children": [
  { "name": "file.txt", "type": "file" }
]

// ✅ CORRECT - Always use objects
"children": {
  "file.txt": {
    "type": "file",
    "name": "file.txt",
    "content": "..."
  }
}
```

### [x] VFS Rule #2: ALWAYS Use Absolute Paths

```json
// ❌ WRONG
"target_path": "home/user/file.txt"

// ✅ CORRECT
"target_path": "/home/user/file.txt"
```

### [x] VFS Rule #3: Node Transitions Reset currentDirectory

- Use `MissionManager.handleNodeConnection` middleware for SSH/Teleport
- Prevents "Node Ghosting" (VFS crossover between nodes)

```typescript
// Example: When switching nodes via SSH
const handleSSH = (targetNode: string) => {
  // Reset currentDirectory to prevent ghosting
  gameState.currentDirectory = getNodeHome(targetNode);
  gameState.activeNode = targetNode;
};
```

**Verification:**
- [x] Rules documented in personal notes
- [x] No arrays used for children in any VFS structure
- [x] All paths start with `/`
- [x] Node transitions reset currentDirectory

---

## 0.5 Campaign Debug Menu

### [x] Create CampaignDebugMenu component

**File:** `game/components/debug/CampaignDebugMenu.tsx`

**Window Specs:**
- Size: 1280x720 fixed
- Position: Centered
- Style: Modal overlay with semi-transparent backdrop
- Hotkey: F8 to toggle

**Interface:**

```typescript
interface CampaignDebugMenuProps {
  campaignState: CampaignState;
  factionReputation: FactionReputation;
  endingState: EndingState;
  playerState: PlayerState;
  onClose: () => void;
}
```

---

### [x] Tab 1: State Overview

**Features:**
- Display current arc, tier, level, active mission
- Progress percentage
- Buttons:
  - [Jump to Arc 1-5]
  - [Set Tier] (input 1-10)
  - [Set Level] (input 1-100)
  - [Complete Current Mission]
  - [Reset Campaign]

**Verification:**
- [x] All fields display correctly
- [x] Jump to Arc works
- [x] Set Tier/Level applies correctly
- [x] Reset Campaign clears all state

---

### [x] Tab 2: Faction Reputation

**Features:**
- Display all 6 factions with reputation bars
- Per-faction controls:
  - [◀◀] [◀10] [◀] [ VALUE ] [▶] [▶10] [▶▶]
  - Alliance radio: [○ None] [● Current] [○ Former]
- Global controls:
  - [Current Alliance: None ▼]
  - [Lock Alliance]
  - Bulk operations (Max All, Min All, Reset, Randomize)
  - Preset save/load

**Visual:**
- Color-coded gradient: -100 (red) → -50 (orange) → 0 (gray) → +50 (green) → +100 (bright green)

**Verification:**
- [x] Reputation sliders/buttons work
- [x] Reputation clamped -100 to +100
- [x] Alliance controls work
- [x] Bulk operations work
- [x] Presets save/load

---

### [x] Tab 3: Story Flags

**Features:**
- Display all story flags with current values
- [Toggle] button next to each flag
- [Reset All Flags]
- [Set All True]
- Component Clue Progress [◀▶] 0 / 12

**Flags to Display:**
- metArchitect
- learnedAboutOracle
- firstOracleEncounter
- discoveredComponentClue_count (number)
- componentFullyAwakened
- architectIdentityRevealed
- choseFactionAlliance
- wasBetrayedByFaction
- oracleFinalContact
- omegaSequenceCreated

**Verification:**
- [x] All flags toggleable
- [x] Flags persist correctly
- [x] Clue stepper works 0-12

---

### [x] Tab 4: Ending Calculations

**Features:**
- Show ALL ending prerequisites
- Editable variables (sliders + inputs):
  - Architect Relationship [-100 to +100]
  - Oracle Sympathy [-100 to +100]
  - Morality Score [-100 to +100]
  - Component Clues Found [0-12]
- Checkboxes:
  - High Cryptography
  - Secret Path Unlocked
  - All NullPack Complete
  - T8-04 Forgave
  - Architect Saved
  - Component Fully Awakened
- Per-ending actions:
  - [▶ PLAY ENDING]
  - [MAKE ACHIEVABLE]
  - [MAKE UNACHIEVABLE]
- Quick presets:
  - [Digital Requiem Ready]
  - [New Dawn Ready]
  - [Total Control Ready]
  - [Transcendence Ready]
  - [Null Protocol Ready]
  - [Architect's Heir Ready]
  - [The Birthing Ready]
  - [Unlock ALL Endings]

**Visual:**
- Green checkmarks for met prerequisites
- Red X for missing prerequisites

**Verification:**
- [x] All variables editable
- [x] Sliders work
- [x] Auto-fill presets work
- [x] Play ending triggers correctly
- [x] Calculations update in real-time

---

### [x] Tab 5: Mission Tracking

**Features:**
- Show ALL missions (created + not_created)
- Per-mission actions:
  - [▶ Teleport] - Auto-completes prereqs, sets level/tier, loads mission
  - [✓ Complete] - Mark complete (grants rewards)
  - [↺ Reset] - Mark incomplete
  - [📄 View JSON] - Open in editor
  - [✏ Edit] - Inline edit properties
- Bulk actions:
  - Arc select dropdown
  - Filter (All / Created Only / Not Created Only)
  - [Complete All in Arc]
  - [Reset Arc]
  - [Unlock All Prerequisites]

**Visual Indicators:**
- 🟢 Green = Completed
- 🟡 Yellow = Available
- 🔴 Red = Blocked (shows which prereqs)
- ⚪ Gray = Not Created
- 🔵 Blue = Currently Active

**Teleport Behavior:**
1. Find mission and ALL prerequisites recursively
2. Mark all prereqs as completed (skip rewards)
3. Set appropriate level/tier
4. Set story flags from prereqs
5. Load mission

**Verification:**
- [x] ALL missions listed (created + not_created)
- [x] Teleport completes prereqs correctly
- [x] Inline editor saves changes
- [x] Visual indicators correct

---

### [x] Tab 6: Player State

**Features:**
- Core stats (editable):
  - Credits, XP, Level, Tier
  - [Set] [Add] [Max] buttons
- Global Deck:
  - Show all scripts
  - [×] delete each
  - [+ Add New Script]
- Inventory:
  - Show all items with quantities
  - [×] [-] [+] controls
  - [+ Add Item ▼] dropdown
- Emails:
  - Unread/Total count
  - [View All] [Send Test] [Mark All Read] [Clear All]
- Cosmetics:
  - Theme dropdown
  - [Unlock All Themes]
  - [Unlock All Cosmetics]
- Save operations:
  - [📤 Export Save to JSON]
  - [📥 Import Save from JSON]
  - [💾 Quick Save]
  - [📂 Quick Load]
  - [🗑 Delete Save]
  - Save templates (Early Game, Mid Game, End Game)

**Verification:**
- [x] All fields editable
- [x] Deck/inventory modifications persist
- [x] Save export/import works

---

### [x] Tab 7: Dev Tools

**Features:**
- Quick Actions:
  - [Reset Entire Campaign]
  - [Skip to Arc 2-5]
  - [Enable God Mode]
  - [Reveal All Map Nodes]
  - [Unlock All Themes]
  - [Unlock All Items]
- Testing:
  - [Trigger Oracle Encounter]
  - [Trigger Betrayal Scene]
  - [Trigger Component Awakening]
  - [Test All Ending Sequences]
- Console Output: Integrated terminal showing debug logs

**Verification:**
- [x] Quick actions work
- [x] Scene triggers function
- [x] Console logs display

---

### [x] Styling (CSS)

**File:** `game/components/debug/CampaignDebugMenu.css`

```css
.campaign-debug-overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0, 0, 0, 0.85);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}

.campaign-debug-window {
  width: 1280px;
  height: 720px;
  background: var(--bg-dark);
  border: 2px solid var(--accent-color);
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  font-family: 'Fira Code', monospace;
  font-size: 13px;
}

.debug-tabs {
  display: flex;
  border-bottom: 1px solid var(--border-color);
}

.debug-tab {
  padding: 10px 20px;
  cursor: pointer;
  border: none;
  background: var(--bg-medium);
  color: var(--text-secondary);
}

.debug-tab.active {
  background: var(--bg-dark);
  color: var(--accent-color);
  border-bottom: 2px solid var(--accent-color);
}

.debug-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.debug-button {
  padding: 4px 12px;
  margin: 2px;
  background: var(--bg-medium);
  border: 1px solid var(--accent-color);
  color: var(--text-primary);
  cursor: pointer;
  font-size: 11px;
}

.debug-button:hover {
  background: var(--accent-color);
  color: var(--bg-dark);
}

.reputation-bar {
  width: 200px;
  height: 16px;
  background: linear-gradient(to right,
    #ff4444 0%,    /* -100 hostile */
    #ff8800 25%,   /* -50 unfriendly */
    #888888 50%,   /* 0 neutral */
    #88ff88 75%,   /* +50 friendly */
    #44ff44 100%   /* +100 devoted */
  );
  border: 1px solid var(--border-color);
}
```

---

### [x] F8 Hotkey Integration

**File:** `game/components/dashboard/Dashboard.tsx`

**Add to Dashboard component:**

```typescript
import { CampaignDebugMenu } from '@/debug/CampaignDebugMenu';

export function Dashboard() {
  const [debugOpen, setDebugOpen] = useState(false);

  useEffect(() => {
    const handleKeyPress = (e: KeyboardEvent) => {
      if (e.key === 'F8') {
        e.preventDefault();
        setDebugOpen(prev => !prev);
      }
    };

    window.addEventListener('keydown', handleKeyPress);
    return () => window.removeEventListener('keydown', handleKeyPress);
  }, []);

  return (
    <>
      {/* existing dashboard content */}
      {debugOpen && (
        <CampaignDebugMenu
          campaignState={gameState.campaignState}
          factionReputation={playerState.factionReputation}
          endingState={endingService.calculateEndings()}
          playerState={playerState}
          onClose={() => setDebugOpen(false)}
        />
      )}
    </>
  );
}
```

**Development Build Only:**
```typescript
// vite.config.ts or tauri.conf.json
define: {
  __DEV_DEBUG__: JSON.stringify(process.env.NODE_ENV === 'development')
}

// Usage: {__DEV_DEBUG__ && <CampaignDebugMenu />}
```

**Verification:**
- [x] F8 toggles debug menu
- [x] Menu closes with Escape or X button
- [x] Only visible in development builds

---

## 0.6 Final Verification Checklist

### [x] All Services Created
- [x] `services/campaign/CampaignStateService.ts`
- [x] `services/campaign/FactionReputationService.ts`
- [x] `services/campaign/EndingService.ts`
- [x] `services/campaign/OracleEncounterService.ts`
- [x] `services/mission/MissionLoaderService.ts` extended

### [x] All Types Defined
- [x] `types/campaign/state.ts`
- [x] `types/campaign/factions.ts`
- [x] `types/campaign/endings.ts`
- [x] `types/campaign/story-flags.ts`

### [x] All Directories Created
- [x] `services/campaign/`
- [x] `types/campaign/`
- [x] `public/missions/campaign/arc_01/`
- [x] `public/missions/campaign/arc_02/`
- [x] `public/missions/campaign/arc_03/`
- [x] `public/missions/campaign/arc_04/`
- [x] `public/missions/campaign/arc_05/`

### [x] Debug Menu Complete
- [x] `components/debug/CampaignDebugMenu.tsx` created
- [x] F8 toggles debug menu
- [x] All 7 tabs display correctly
- [x] Tab 1: State Overview - Arc/tier/level jump works
- [x] Tab 2: Factions - Sliders, buttons, alliance controls work
- [x] Tab 3: Story Flags - All flags toggle, persist
- [x] Tab 4: Endings - Variables editable, presets work
- [x] Tab 5: Missions - All listed, teleport works, editor saves
- [x] Tab 6: Player - All fields editable, save/export works
- [x] Tab 7: Dev Tools - Actions work, triggers function

### [x] Mission System Ready
- [x] `npm run predev` regenerates mission index
- [x] `mission_index.json` has correct paths (`campaign/arc_01/t1_01.json`)
- [x] `category` field set correctly
- [x] `order` field correct for sequential missions
- [ ] Sample mission in `campaign/arc_01/` appears in index

### [x] Development Environment
- [x] `npx tauri dev` launches without campaign-related errors
- [x] No TypeScript errors
- [x] VFS rules understood and documented

---

## ✅ Phase 0 Complete When...

- [x] All 4 services created and tested
- [x] All type definitions complete
- [x] MissionLoaderService extended
- [x] CampaignDebugMenu with all 7 tabs functional
- [x] F8 hotkey works (dev builds only)
- [x] All directories created
- [x] Mission index regeneration works
- [x] VFS rules documented and understood
- [x] `npx tauri dev` runs without errors

---

## 📝 Notes

```
CURRENT STATUS: COMPLETE

BLOCKING ISSUES:
-

QUESTIONS:
-

COMPLETED:
✓ CampaignStateService - Arc progression, story flags, mission tracking
✓ FactionReputationService - Faction reputation, alliances, thresholds
✓ EndingService - 7 endings, prerequisite checking, achievable calculations
✓ OracleEncounterService - Oracle dialogue system, encounter triggers
✓ Type definitions - state.ts, factions.ts, endings.ts, story-flags.ts
✓ MissionLoaderService extension - Campaign completion handling
✓ Directory structure - All 5 arc directories created
✓ Mission index regeneration - Works with new structure
```

---

*Phase 0 Foundation - Complete this before writing any missions.*

**Next Phase:** Phase 1 - Arc 1: The Kitten's Claws
