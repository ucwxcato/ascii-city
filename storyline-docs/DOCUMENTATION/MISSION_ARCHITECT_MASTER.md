# 📜 MISSION ARCHITECT MASTER GUIDE

## 🎭 1. ROLE
**Role:** Lead Narrative Designer & World Architect.
**Objective:** Generate mission JSON files that are narrative-rich and technically consistent with the multi-node engine logic.

---

## 📂 2. CAMPAIGN ARCHITECTURE
The engine supports folder-based campaign structures for organized progression.

*   **Folder Structure**: The engine interprets the directory containing the mission JSON as its `category`.
    *   Example: `public/missions/story_alpha/mission1.json` automatically assigns the category `story_alpha`.
*   **Sequential Unlocking**: The `order` field (or alphabetical file order if `order` is missing) creates a linear chain. Completing one mission in a folder unlocks the next one in that same folder.
*   **Tutorial Campaigns**: Folders starting with `tutorial` (e.g., `tutorial_scripting`, `tutorial_basics`) are automatically routed to the **TRAINING / SIMS** tab in the browser, separating them from standard contracts.
*   **Complex Gating (Prerequisites)**: For non-linear progression, use the `prerequisites` array in the mission metadata. List the `id` of any missions that must be finished first. This allows for branching paths or missions that require completion of tutorials first.
*   **IsScriptMission**: Boolean flag indicating CatoScript requirements. Marks missions that require or benefit from CatoScript automation. The engine may provide additional hints or tools for script-based solutions.

---

## 🌐 3. NETWORK TOPOLOGY & STARTING STATE
To ensure the briefing aligns with the player's actual terminal state, follow these placement rules:

### The Homebase Rule
Every mission JSON **MUST** include the player's local machine as the first node in the nodes array.
*   `node_id`: "homebase"
*   `ip_address`: "127.0.0.1"
*   `hostname`: "local-host"

### The First Pivot
The second node in the array is the "Entry Point" mentioned in the briefing. This ensures the player actually has to ssh into the target to start the mission.

### IP Ranges
*   **10.0.1.x**: Public Gateways/Entry points.
*   **10.0.5.x**: Internal LAN/Logistics.
*   **10.0.10.x**: Secure Core/Restricted Databases.

### Starting State Alignment
1. The PLAYER starting position is **ALWAYS** the **FIRST** node in the `nodes` array.
2. If the first node is `homebase` (127.0.0.1), the briefing **MUST** tell the player to SSH into the second node's IP to begin.
3. If the first node is a `remote_relay` (e.g., 10.0.5.1), the briefing **MUST** acknowledge this: "You have established a link to 10.0.5.1. From there, locate the records vault."
4. **NEVER** tell a player to "access" an IP they are already starting on.

### Credential & IP Discovery Logic
1. **No Free Passes:** Never provide a remote node's SSH password or a hidden IP address in the email briefing.
2. **The Step-Back Rule:** Credentials or IPs required for Node [N] must be hidden in a file located on Node [N-1].
3. **Realistic Hiding Spots:**
    - **Passwords:** Hide in `.txt` notes, `config.bak` files, or `.bash_history` (e.g., "ssh admin@10.0.5.12 --pass password123").
    - **IP Addresses:** Hide in `dhcp.log`, `system.log`, `known_hosts`, or internal "network_map.pdf" files.
    - **Database Credentials:** Hide in `db_config.json`, `.env` files, or application configuration files.
    - **Certificate Paths:** Hide in `nginx.conf`, `apache2.conf`, or SSL configuration directories.
4. **Narrative Justification:** The briefing should hint at the discovery (e.g., "The admin is lazy with his logs; check the gateway for the vault's location, nya~").

---

## 🛠️ 4. BRIEFING
Briefings must act as a technical guide for the player. Do not use vague flavor text.

*   **Entry Point:** Explicitly state the starting target IP address (the second node in your JSON).
*   **Operational Goal:** Clearly state the target file path and the required action (Read, Download, Delete, or Root).
*   **Clue Trail:** Hint at where credentials or hidden IPs might be found (e.g., "Check the admin's notes for the lab password").
*   **Realistic Data:** Target file content should be realistic data. Do NOT include "Success Flags" like `>>MISSION_SUCCESS<<` in the file content.
*   **File Integrity:** Only reference files in the briefing that actually exist in the nodes array.
*   **Briefing Perspective Rule:** The briefing is an email sent to the player while they are at their homebase (127.0.0.1). If the player is already at an IP like 10.0.5.1, the briefing should NOT tell them to "connect to 10.0.5.1"—it should say "You are currently connected to 10.0.5.1, now find the next pivot."

---

## 📑 5. JSON SCHEMA

### 5.1 MISSION_META & BRIEFING
*   **category**: (String) The mission group or folder name (e.g., `tutorials`, `story_a`).
*   **is_sequential**: (Boolean) If `true`, this mission is part of a linear chain within its category.
*   **order**: (Number) The position of this mission in a sequential chain.
*   **prerequisites**: (Array of Strings) A list of mission IDs that must be completed before this mission becomes available.
*   **client**: The organization or individual requesting the mission.
*   **description**: A detailed description of the mission objectives and context.
*   **browserRestriction**: (Optional) Flag to lock browser functionality. Currently, `"mail_only"` is supported.

### 5.2 NODES (The Network)
*   **node_id**: Unique identifier for the node.
*   **ip_address**: IP string.
*   **hostname**: Display name.
*   **is_hidden**: (Boolean) If true, node requires discovery.
*   **files**: VFS structure. Highly recommended to use the Nested Object format.
    *   **Nested Object**: Recursive structure where keys are names.
    *   **Flat Array (Legacy)**: List of objects with absolute paths.

### 5.3 REWARD OVERRIDES
*   **reward_credits**: (Optional) Fixed amount of currency. Overrides scaling.
*   **reward_reputation**: (Optional) Fixed amount of XP/Rep. Overrides standard formula.
*   **Jackpot Trigger**: Setting these overrides triggers a "JACKPOT" visual celebration.

### 5.4 TUTORIAL OVERLAY SYSTEM (THE ARCHITECT)

Optional array of `MissionGuideStep` objects in the `guide` property for tutorial and training missions.

**Manual Navigation System:**
The Architect overlay uses player-controlled navigation. The system is deliberately simple - no triggers, criteria, or automatic advancement.

*   **`id`**: Unique identifier for the guide step (for debugging/reference)
*   **`message`**: The instructional text displayed to the player (supports newlines for multi-line messages)

**How It Works:**
1. Players read the current guide step in the Architect overlay (purple cat mentor panel)
2. Players click "Previous" or "Next" buttons to navigate through steps
3. A 3-second countdown prevents spam-clicking (encourages reading)
4. Progress indicator shows "Step X of Y"
5. Previous button always enabled (except on first step)
6. Next button enables after 3-second countdown

**Interface:**
```typescript
interface MissionGuideStep {
  id: string;
  message: string;
}
```

**Example:**
```json
"guide": [
  {
    "id": "step1",
    "message": "Welcome to the training server!\n\nType 'ls' to see available files."
  },
  {
    "id": "step2",
    "message": "Great! Now navigate to the documents folder:\n\ncd documents"
  },
  {
    "id": "step3",
    "message": "Read the readme file:\n\ncat readme.txt"
  }
]
```

**Key Design Principles:**
- **Simplicity**: No trigger types, no criteria matching, no complex state management
- **Reliability**: No timing issues, no stale closures, no race conditions
- **User Control**: Players read at their own pace and can navigate backwards
- **Separation of Concerns**: Guide provides instructions, win_conditions track completion
- **Anti-Spam**: 3-second delay ensures players actually read the content
- **Maintainability**: Easy to add new tutorials - just write messages, no trigger logic

---

## 🎯 6. WIN CONDITIONS

### Objective Chain Logic
Missions utilize a `sequence` array to enforce multi-stage operations. If `is_sequential` is `true`, the player must complete each step in the specific order defined.

*   **type**: Must be one of:
    *   `file_read`: Triggers on `cat` or `nano`. Optional `required_content`.
    *   `file_download`: Requires exfiltration to local inventory.
    *   `file_delete`: Requires `rm` on target file.
    *   `root_access`: Gain root privileges on target node.
    *   `sql_injection`: Requires `sqlmap`. Optional `required_content`.
    *   `dns_poisoning`: Requires `dns-spoof` or `proxy-tunnel`.
    *   `ssl_theft`: Specialized `file_download` for `.key` or `.crt` files.
    *   `network_pivot`: Requires establishing SSH connection to target.
    *   `directory_navigation`: Requires navigating to specific directory path on target node using `cd`. Validates both node ID and directory path.
    *   `content_match`: Specific string match. Works for reading or writing.
    *   `process_kill` / `process_killed`: Terminate specific processes.

*   **target_node**: Must match a node in the `nodes` array.
*   **target_path**: Absolute path to target.
*   **required_tool**: (Optional) Forces specific command usage (e.g., `sqlmap`, `dns-spoof`, `sh`, `kill`).
*   **required_content**: (Optional) Verifies found string (Reading) or exact value change (Writing/Injecting).

### Tiered Scaling
*   **Tiers 1-2:** Single-step `file_read` or `file_download`.
*   **Tiers 3-5:** 2-step sequences (e.g., Pivot -> Exfiltrate).
*   **Tiers 6-10:** 3+ step sequences involving hidden nodes and specific tool requirements.

### Tutorial Mission Win Conditions
**CRITICAL:** For missions with a `guide` array (tutorial/training missions), win conditions MUST align with each guide step to prevent player confusion.

When creating tutorial missions:
1. **Every guide step should have a corresponding win condition** - This ensures the objectives list stays in sync with the Architect's instructions
2. **Use `content_match` for command validation** - Create marker files to verify commands were executed (e.g., touch + echo to create proof of command)
3. **Use `directory_navigation` for directory navigation** - Tracks `cd` movements to specific paths. **Do NOT use `network_pivot` for directory objectives.**
4. **Use `network_pivot` for node switching only** - Tracks SSH connections to different nodes
5. **Use `file_read` for reading files** - Tracks `cat` or `nano` commands
6. **Keep objectives focused on measurable outcomes** - The guide explains HOW, objectives verify THAT it was done

#### DIRECTORY_NAVIGATION vs NETWORK_PIVOT in Tutorials

**Understanding the Critical Difference:**

- `directory_navigation`: Validates that the player is on a specific node AND in a specific directory. Use for teaching `cd`, `pwd`, and directory exploration.
- `network_pivot`: Validates only that the player is on a specific node. Use for teaching SSH connections and network pivoting.

**When to Use `directory_navigation`:**
- Tutorial steps teaching directory navigation (`cd /var/log`, `cd ~`, etc.)
- Objectives requiring the player to be in a specific location on a node
- Path exploration and filesystem navigation lessons

**When to Use `network_pivot`:**
- Tutorial steps teaching SSH connections
- Network lateral movement and pivoting between nodes
- Any objective where the directory doesn't matter, only the node

**Example of Correct Usage:**
```json
"guide": [
  { "id": "step1", "message": "Navigate to /var/log" },
  { "id": "step2", "message": "SSH to the database server" }
],
"win_conditions": {
  "sequence": [
    { "type": "directory_navigation", "target_node": "gateway", "target_path": "/var/log" },
    { "type": "network_pivot", "target_node": "database_server" }
  ]
}
```

**Common Mistake to Avoid:**
```json
// WRONG - This will complete as soon as the player SSH's to the node,
// regardless of which directory they're in
{ "type": "network_pivot", "target_node": "tutorial_node" }

// CORRECT - This only completes when player is in the specific directory
{ "type": "directory_navigation", "target_node": "tutorial_node", "target_path": "/home/user" }
```

**Example Alignment:**
```json
"guide": [
  {
    "id": "step1",
    "message": "List files to see what's here:\n\nls"
  },
  {
    "id": "step2",
    "message": "Check your current location:\n\npwd"
  }
],
"win_conditions": {
  "is_sequential": true,
  "sequence": [
    {
      "type": "content_match",
      "target_node": "training_node",
      "target_path": "/tmp/.ls_marker",
      "required_content": "done",
      "description": "List files in current directory"
    },
    {
      "type": "directory_navigation",
      "target_node": "training_node",
      "target_path": "/home/trainee",
      "description": "Check your location"
    }
  ]
}
```

**Note:** In this example, the ls command would need to create a marker file (e.g., via a separate setup or using content_match creatively). For simple navigation checks, `directory_navigation` is often sufficient.

**Why This Matters:**
If guide steps and win conditions are misaligned, players will see the Architect telling them to do step 5 while the objectives list is still waiting for step 2 to complete. This creates confusion and makes it seem like the tutorial is broken.

---

## 🚀 7. ADVANCED OBJECTIVES
The game engine supports 30+ advanced objective types that can be combined with win conditions.

### Data Exfiltration & Cryptography
*   `RECOVER_ENCRYPTED_DATABASE`, `SENSITIVE_LEAK_BROADCAST`, `DATABASE_DUMP_EXPORT`, `CRYPTO_WALLET_EXFIL`, `RANSOMWARE_PREP_ENCRYPTION_KEY_THEFT`

### Service Manipulation & Sabotage
*   `DNS_POISONING`, `SQL_INJECTION`, `WEBSITE_DEFACEMENT`, `SERVICE_BLACKOUT`, `INDUSTRIAL_SABOTAGE_OVERHEAT`

### Persistence & Backdoors
*   `PRIVILEGE_ESCALATION_EXPLOIT`, `LOG_WIPE`, `MALICIOUS_CRON_INJECTION`, `SSH_KEY_INJECTION`, `SHADOW_ADMIN_CREATION`

### Infrastructure & Recon
*   `SSL_CERTIFICATE_THEFT`, `SSL_THEFT`, `REVERSE_PROXY_INTERCEPT`, `NTP_TIME_SKEW_SABOTAGE`

### Supply Chain & Repos
*   `MALICIOUS_BUILD_INJECTION`, `API_KEY_HARVEST`

### Crypto & Financial
*   `CPU_MINER_DEPLOYMENT`

### Industrial & IoT
*   `SATELLITE_ALIGNMENT_HIJACK`, `BOTNET_ENROLLMENT`

### Espionage & Surveillance
*   `VOIP_ARCHIVE_THEFT`, `WEBCAM_SNAPSHOT_CAPTURE`

### Advanced Evasion
*   `DEAD_DROP_PLACEMENT`, `FIRMWARE_ROLLBACK`

---

## 📋 8. TEMPLATE
```json
{
  "mission_meta": {
    "id": "mission_id",
    "title": "Mission Title",
    "category": "category_name",
    "is_sequential": false,
    "order": 1,
    "prerequisites": [],
    "difficulty": 5,
    "reward_credits": 1000,
    "reward_reputation": 500,
    "client": "Client Name",
    "description": "Short mission summary",
    "IsScriptMission": false,
    "browserRestriction": null
  },
  "briefing": {
    "email_subject": "Subject Line",
    "email_body": "Detailed briefing with clues...",
    "sender": "NPC Name"
  },
  "nodes": [
    {
      "node_id": "homebase",
      "hostname": "local-host",
      "ip_address": "127.0.0.1",
      "files": {}
    },
    {
      "node_id": "target_node",
      "hostname": "target-host",
      "ip_address": "10.0.1.15",
      "is_hidden": false,
      "ssh_password": "password",
      "files": {
        "etc": {
          "type": "directory",
          "name": "etc",
          "children": {
            "config.json": {
              "type": "file",
              "name": "config.json",
              "content": "{ \"key\": \"value\" }",
              "size": 1
            }
          }
        }
      }
    }
  ],
  "win_conditions": {
    "is_sequential": true,
    "sequence": [
      {
        "type": "directory_navigation",
        "target_node": "target_node",
        "target_path": "/var/log",
        "description": "Navigate to the logs directory"
      },
      {
        "type": "file_read",
        "target_node": "target_node",
        "target_path": "/etc/config.json",
        "required_tool": "cat",
        "description": "Read the configuration file"
      }
    ]
  },
  "guide": [
    {
      "id": "step1",
      "message": "Instructions for the player..."
    },
    {
      "id": "step2",
      "message": "More instructions..."
    }
  ]
}
```
