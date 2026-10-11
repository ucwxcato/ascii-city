# Walking Storyline Game Development Plan

> **Status:** Planned. The first-person world, map travel, character interactions, linear dialogue, and ambient traffic are present. Campaign progress, choice handling, clue tracking, and saves are not implemented.
>
> **Purpose:** Adapt the story in `storyline-docs/` into a first-person exploration game that advances through walking, conversations, discoveries, and choices.
>
> **Authority:** This plan owns the implementation of the campaign as an exploration game. The storyline files own plot, characters, and lore. The [ASCII City development plan](ascii-city-development-plan.md) owns the existing engine foundation and its verified history.
>
> **Target:** Kotlin/JVM, LibGDX desktop, using the existing `core`, `desktop`, maps, content loader, and UI.

## 0. Outcome

The player walks through the city, meets characters, finds story clues, and makes choices that affect later scenes. The campaign keeps its central mystery and character conflicts while the player experiences them through the existing first-person world.

Complete the first campaign slice when:

```text
Start a new run -> explore an authored route -> meet a character -> discover clues ->
make a choice -> travel to another map and return -> see the choice reflected in later dialogue ->
finish the slice without losing progress or blocking movement
```

## 1. Locked decisions

- Gameplay is walking exploration. The player moves, looks, interacts, reads, and chooses dialogue responses.
- Do not add hacking gameplay: no command entry, shell, virtual file system, network scanning, code-writing, credential puzzles, or hacking minigames. Technology may still matter to the plot and appear as readable scenery or dialogue.
- Use the plot and characters in `storyline-docs/`; translate mission events into walkable scenes and discoveries.
- Keep authored ambient traffic as scenery. The player does not drive cars or race.
- Keep the first-person ASCII renderer, semantic collision, fixed authored maps, existing interaction targeting, and `DialogueOverlay`.
- Use a small campaign progress model with stable IDs. Do not introduce a general quest graph, scripting language, ECS, or external framework.
- Start with one Arc 1 slice. Expand only after its full walk-through works.
- Leave faction reputation, combat, inventory, currencies, timers, procedural contracts, and multiple ending calculations out of the first slice.
- Keep campaign progress in memory across map changes. Add versioned save/load after the first slice is playable and verified.

## 2. Goals and non-goals

### Goals

- Turn the early campaign into a short, complete exploration chapter.
- Deliver the mystery through people, places, found evidence, and the player's system being noticed by others.
- Track completed story beats, discovered clue IDs, and explicit choice IDs.
- Let players revisit characters and receive context-aware dialogue.
- Show one current objective and a journal of discovered clues and important choices.
- Preserve story progress while `AsciiCityGame` replaces `GameScreen` during map travel.
- Validate story references and data with useful file-and-path errors.

### Explicitly out of scope

- Any playable hacking or terminal-command system.
- Recreating the TypeScript/browser mission runtime described by the campaign mission guide.
- Implementing all 95+ missions, six faction reputation tracks, or seven endings before the first chapter is proven.
- Driving, racing, removing ambient traffic, changing the camera, or replacing the ASCII renderer.
- A generic quest engine, arbitrary user-authored scripts, multiplayer, procedural maps, or online services.

## 3. Player flow

1. Start a new campaign and receive one clear first objective.
2. Walk to a character or story object and use the existing center-screen interaction prompt.
3. Read a short dialogue or inspect a clue. The game records the beat or clue once.
4. Choose a response at a small dialogue choice prompt. The game records the choice by stable ID.
5. Continue exploring. Later scenes use the recorded clue and choice IDs to select the appropriate dialogue.
6. Open the journal to review the current objective and discovered story information.
7. Change maps through authored doors. Campaign progress remains available in the same run.
8. Complete the slice at a clear chapter ending. On a later launch, resume from a validated save once save/load is implemented.

Invalid, early, or repeated interactions must show an appropriate response without skipping progress, duplicating rewards, or crashing.

## 4. Architecture and ownership

Existing implementation points include `AsciiCityGame`, `GameScreen`, `InteractionSystem`, `DialogueOverlay`, `ContentLoader`, and the map object and dialogue JSON files.

| Component | Owns | Must not own |
|---|---|---|
| `AsciiCityGame` | One `CampaignSession` per run and passing it into each new `GameScreen` during map travel. | Story rules, dialogue rendering, or save-file parsing. |
| Proposed `CampaignSession` | Completed beat IDs, discovered clue IDs, selected choice IDs, and current objective ID. | LibGDX objects, map geometry, or rendered state. |
| Proposed `CampaignStorySystem` | Resolving an interaction against current progress, returning dialogue/choice outcomes, and applying an accepted outcome once. | Keyboard polling, widgets, rendering, or arbitrary script execution. |
| `InteractionSystem` | Finding and validating the visible, reachable interaction target. | Campaign progression. |
| `DialogueOverlay` | Displaying dialogue and offering available responses through the existing input command route. | Directly mutating world or campaign state. |
| `ContentLoader` and registries | Validating dialogue, choice, beat, clue, and referenced interaction IDs before play. | Silently repairing invalid story data. |
| `GameScreen` | Composing story outcomes with interaction, HUD, journal, and map travel. | Owning progress that must survive screen replacement. |

Use a short, explicit set of story outcome types, such as `ShowDialogue`, `OfferChoices`, `RevealClue`, and `SetObjective`. Keep the first story route in ordinary Kotlin definitions and prose in the existing data files. Do not build a condition-expression language. If later chapters need substantially more story data, move the proven beat definitions into a small validated JSON format in a separate change.

## 5. Data, lifecycle, and failure handling

### First-slice progress

Use stable strings for IDs so story content can refer to them without relying on array positions or enum ordinals.

| Data | Example shape | Rule |
|---|---|---|
| Completed beats | `completedBeatIds: Set<String>` | Add only after the expected story interaction is accepted. |
| Clues | `discoveredClueIds: Set<String>` | A clue is recorded once; revisiting it can replay short context. |
| Choices | `choices: Map<String, String>` | A choice ID maps to the selected option ID and cannot change silently on replay. |
| Objective | `currentObjectiveId: String?` | At most one active objective is shown in the HUD. |
| Save version | `schemaVersion: Int` | Introduce with save/load. Reject unsupported versions with a clear recovery message. |

The first playable slice should cover the Arc 1 introduction and a small number of the subtle system-component clues described in the campaign structure. The slice ends at a deliberate reveal or chapter boundary chosen from the source story before implementation. Do not invent missing canon in code; mark unresolved plot details for the story owner to decide.

Save/load is a later phase. When added, store only stable campaign IDs and the selected map/spawn needed to resume. Write to a temporary file, validate the complete candidate save, then replace the prior save. A bad save must offer a clear new-game/recovery path and must never partially mutate the active session.

## 6. Content and map integration

- Keep dialogue prose in `assets/data/dialogue.json` where the existing format fits.
- Keep character and clue placement in authored map object files with stable IDs.
- Add a small validated campaign definition only if a requirement cannot be represented clearly by the existing dialogue and object formats.
- Use existing fixed maps for the first slice where their locations support the scenes. Add or revise map content only when a specific story beat needs a location.
- Story clues should be readable or spoken discoveries. A terminal may present a message or record; it must not require command entry or simulated access steps.
- Keep ambient traffic rendering and its authored routes unchanged as story features are introduced.

## 7. Verification matrix

| Scenario | Expected result | Evidence required |
|---|---|---|
| Start the first slice | First objective and entry scene are available. | Focused campaign state test and manual walk-through. |
| Interact with a required person or clue | Correct dialogue appears and the expected beat/clue is recorded once. | Focused story-system tests. |
| Interact too early or repeat an interaction | Contextual response appears; progress remains valid and unchanged where appropriate. | Tests for early, repeated, and out-of-order interactions. |
| Make each available choice | Selected choice is retained and a later scene reflects it. | Choice-state tests and a manual route for each first-slice option. |
| Travel to another map and return | Same campaign session remains active. | Test or runtime walk-through across a paired map door. |
| Load invalid story content | Startup reports the source file and invalid ID/path. | Content validation test. |
| Open and close the journal/dialogue | Input focus returns to walking; modal input does not move the player. | Focused UI/input tests and manual check. |
| Save and resume | Versioned progress loads completely or reports a safe recovery message. | Save round-trip, invalid-save, and migration/version tests. |
| Walk through the city with story content loaded | Ambient traffic continues as scenery and player driving controls remain absent. | Existing content validation and focused runtime smoke check. |

## 8. Phased checklist

### Phase 0: Adapt the opening story

- [ ] Select the first Arc 1 chapter boundary from `storyline-docs/` and list its people, locations, clues, dialogue, and choices.
- [ ] Mark each source beat as walk, talk, inspect, choice, or cut from gameplay because it depends on hacking mechanics.
- [ ] Identify required map changes and confirm they fit the fixed-map authoring model.
- [ ] **Verify:** The slice can be described as a linear walk-through with explicit optional discoveries and choice outcomes.

### Phase 1: Campaign session across maps

- [ ] Add a small `CampaignSession` owned by `AsciiCityGame` and pass it to each `GameScreen` created during travel.
- [ ] Add focused tests for session mutation, idempotency, and screen/map travel continuity.
- [ ] **Verify:** Progress survives travel between two maps within one run and resets when a new campaign is started.

### Phase 2: First playable story slice

- [ ] Add `CampaignStorySystem` for the selected story beats and typed interaction outcomes.
- [ ] Connect existing NPC/object interaction IDs and dialogue definitions to story progression.
- [ ] Add a small choice prompt through existing UI/input handling, with each option stored by stable ID.
- [ ] Add the story scenes and clue placements to the existing authored maps.
- [ ] **Verify:** Complete the chapter start to finish, exercise each choice, revisit interactions, and confirm no hacking action is required.

### Phase 3: Journal and save/resume

- [ ] Add a journal screen or panel for the active objective, discovered clues, and important choices.
- [ ] Add a versioned save DTO and atomic local save/load path.
- [ ] Add recovery behavior for missing, corrupt, or unsupported save files.
- [ ] **Verify:** Journal matches session data; save/resume and invalid-save cases pass focused tests and manual checks.

### Phase 4: Expand the campaign

- [ ] Adapt later campaign arcs into walkable scenes using the proven interaction and progress model.
- [ ] Add faction reactions and ending conditions only when an authored chapter needs them, using explicit stable choices and flags.
- [ ] Add maps or map revisions only for authored story needs.
- [ ] **Verify:** Each chapter has a start, objective, authored route, discoverable story content, safe repeat behavior, and an ending or handoff before its content is considered complete.

## 9. Open decisions

- Choose the first Arc 1 chapter boundary and its target length before Phase 1 story implementation.
- Confirm whether existing map locations can carry the opening chapter or whether one new story location is required.
- Decide which source-doc choices should have visible consequences in the first slice. Keep the choice count small enough to verify fully.
- Decide save location and user-facing new-game/resume flow during Phase 3, following the existing desktop platform conventions.
