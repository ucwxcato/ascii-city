# The Architect System

## Overview
The Architect is a data-driven tutorial and guidance system that provides contextual, step-by-step instructions to players. It operates by attaching tutorial arrays to mission definitions, allowing for highly specific, trigger-based overlays without hardcoding tutorial logic into components.

## Key Concepts

### Data-Driven Design
Tutorial content is defined in mission JSON files, not in game code. This separates narrative content from implementation logic.

### Trigger-Based Advancement
Tutorials advance automatically when players perform specific actions (commands, file creation, delays), creating natural gameplay flow.

### Contextual Messages
The Architect appears exactly when needed, providing relevant instructions based on current game state and player actions.

### Browser Restriction
Tutorial steps can limit browser functionality to keep players focused on current objectives.

## How It Works

### Mission Integration

#### guide Array
Both Mission and ActiveMission interfaces support an optional `guide: TutorialStep[]` array.

When a mission with this array loads:
- The Architect system activates automatically
- Overlay appears with first tutorial step
- Player progress tracked via `activeMission.currentGuideIndex`

#### State Tracking
The active mission object tracks tutorial progress:
- `currentGuideIndex`: Points to current step in guide array
- Increments when player completes current step's trigger
- Reaches end when tutorial is complete

### The TutorialStep Object

Each tutorial step is an object with these properties:

#### id
Unique identifier for the step (e.g., "step_01", "ssh_connection").

#### message
The instructional text displayed to the player. This is what actually appears in the Architect overlay.

#### trigger
The type of event that advances to the next step (TutorialTrigger enum):
- `EXACT_COMMAND`: Player must type exact command string
- `COMMAND_NAME`: Player uses command name (arguments ignored)
- `VFS_PATH_EXISTS`: File/directory appears at specified path
- `DELAY`: Automatic advance after time passes

#### criteria
The specific value the trigger listens for:
- For `EXACT_COMMAND`: The exact command string (e.g., "ls -la")
- For `COMMAND_NAME`: The command name (e.g., "ssh")
- For `VFS_PATH_EXISTS`: The file path to check (e.g., "/home/user/test.txt")
- For `DELAY`: Milliseconds to wait (e.g., "3000")

#### browserRestriction (Optional)
Restricts browser functionality:
- `"mail_only"`: Only mail client accessible, general browser disabled
- `undefined` or absent: No restrictions

### Advancement Triggers

#### EXACT_COMMAND
Tutorial advances only when player's input string is an exact match.

**Example**:
```json
{
  "id": "list_files",
  "message": "Type 'ls' to see what files are in this directory.",
  "trigger": "EXACT_COMMAND",
  "criteria": "ls"
}
```

Only advances if player types exactly "ls" (not "ls -la", not "LS").

**Use Cases**: Teaching exact command syntax, ensuring players learn precise input.

#### COMMAND_NAME
Tutorial advances when the executed command's name matches, regardless of arguments.

**Example**:
```json
{
  "id": "connect_ssh",
  "message": "Use SSH to connect to user@10.0.1.30",
  "trigger": "COMMAND_NAME",
  "criteria": "ssh"
}
```

Advances whether player types "ssh user@10.0.1.30" or "ssh root@192.168.1.1" - any ssh command works.

**Use Cases**: Teaching command concepts without enforcing exact syntax, allowing flexibility.

#### VFS_PATH_EXISTS
Tutorial advances when a file or directory exists at the specified VFS path.

**Example**:
```json
{
  "id": "create_file",
  "message": "Use 'touch test.txt' to create a new file.",
  "trigger": "VFS_PATH_EXISTS",
  "criteria": "/home/user/test.txt"
}
```

Advances automatically when the file appears, whether from `touch`, `echo`, or download.

**Use Cases**: File creation objectives, download tasks, any operation that creates VFS entries.

#### DELAY
Tutorial advances automatically after specified milliseconds.

**Example**:
```json
{
  "id": "read_message",
  "message": "Read this carefully...",
  "trigger": "DELAY",
  "criteria": "3000"
}
```

Advances after 3 seconds, regardless of player actions.

**Use Cases**: Reading time, dramatic pauses, allowing players to digest information.

### System Components

#### ArchitectOverlay Component
Located in `components/ArchitectOverlay.tsx`.

**Features**:
- Monitors `activeMission.currentGuideIndex`
- Renders current step's message
- Displays overlay box titled "The Architect"
- Typewriter effect for text display
- Automatically dismisses when tutorial completes

#### Terminal Command Interception
Located in `components/Terminal.tsx` processCommand function.

**Logic**:
1. Player executes command
2. Command runs normally
3. Terminal checks if tutorial is active (guide array exists)
4. If active, compares command against current TutorialStep
5. Checks trigger type and criteria
6. On match, increments currentGuideIndex
7. ArchitectOverlay renders next step

#### Browser Restriction Implementation
The `browser` and `mail` commands check for `browserRestriction` in the active tutorial step.

**Logic**:
1. Player types `browser` or `mail`
2. Command checks activeMission.currentGuideIndex
3. Retrieves current TutorialStep
4. Checks browserRestriction property
5. If set to "mail_only", blocks general browser access
6. Allows mail client to open normally

**Purpose**: Keeps players focused on tutorial objectives without distractions.

## Important Rules

### Tutorial Design
- Tutorials should be concise and clear
- Each step should teach one concept
- Progression should feel natural, not forced
- Messages should be helpful, not annoying

### Trigger Selection
- Use `EXACT_COMMAND` when syntax matters (teaching specific commands)
- Use `COMMAND_NAME` when concept matters (teaching command categories)
- Use `VFS_PATH_EXISTS` for file operations (creation, downloads)
- Use `DELAY` for reading time or pacing

### Browser Restrictions
- Only use `"mail_only"` restriction when necessary
- Don't block access needed for objectives
- Lift restrictions as soon as appropriate

### Mission Integration
- Tutorials are optional in mission JSON
- Not all missions need tutorials
- Tutorial missions use guide array extensively
- Sandbox missions typically don't include tutorials

## Relationships

### Related Systems
- **Mission System**: Provides guide array in mission definitions
- **Terminal**: Executes commands and checks tutorial triggers
- **VFS**: Provides file existence checking for VFS_PATH_EXISTS
- **Browser/Mail Client**: Respects browser restrictions
- **UI Overlay**: Renders tutorial messages

### Tutorial Flow
1. Mission loads with guide array
2. ArchitectOverlay appears with first step
3. Player performs action
4. Terminal intercepts and checks trigger
5. If match, increment currentGuideIndex
6. Overlay shows next step
7. Repeat until tutorial complete

## Common Pitfalls

### Over-Tutorializing
Too many tutorial steps overwhelm players. Keep tutorials focused on essential mechanics.

### Wrong Trigger Type
Using EXACT_COMMAND when COMMAND_NAME is more appropriate makes tutorials frustrating. Choose the right trigger for the situation.

### Missing Criteria
Forgetting to specify criteria makes triggers impossible to match. Always include criteria for the trigger type.

### Permanent Restrictions
Forgetting to lift browser restrictions blocks necessary features. Restrictions should be temporary and lifted when appropriate.

### Vague Messages
Unclear tutorial messages confuse players. Each message should have one clear action or concept.

### Skipping Context
Assuming players know things they haven't learned yet. Tutorials should build knowledge progressively.

## Related Documents
- [Project Overview](00_PROJECT_OVERVIEW.md) - Game introduction
- [Game Architecture](01_GAME_ARCHITECTURE.md) - Mission structure
- [Mission Objectives](05_MISSION_OBJECTIVES.md) - Tutorial mission design
- [User Interface](08_USER_INTERFACE.md) - Overlay systems
