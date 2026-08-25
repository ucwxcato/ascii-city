# ASCII City Implementation Workflow

> Authority: `LEARNING_STRATEGY.md` remains the complete learning contract. This file routes that contract into this project.

## Default session shape

1. Select one unchecked development-plan task and name its Kotlin concept.
2. Declare Babysteps, Bridge, or Shadow-typing mode.
3. Explain what the task contributes to the visible game before discussing syntax.
4. Show only a small syntax skeleton with blanks for unfamiliar Kotlin.
5. The user explains unfamiliar syntax in plain English, then types the implementation.
6. Reach at least three micro-wins: source typed, build/test green, and runtime evidence or a tattoo note.
7. Record evidence before checking the development-plan task.
8. Ask for the murkiest sentence and update `DOCS/kotlin-brain-tattoos/` when a concept lands.

## Kotlin style while learning

- Prefer explicit types where the type is being learned.
- Prefer named variables and lambda parameters.
- Prefer ordinary `if`/`else` and `for` loops over compressed chains.
- Avoid `!!`; teach nullable branches explicitly.
- Keep one statement per line and choose descriptive names.
- Deconstruct unfamiliar Kotlin shorthand after the code is green.

## Direct implementation override

If the user explicitly asks to “vibecode,” “just write it,” or otherwise delegates implementation, the assistant may write the requested scoped code directly. Tests, verification evidence, plan scope, and preservation of unrelated user changes still apply.

## Safe intentional failures

Intentional breakage requires opt-in after a green state, must be isolated and reversible, and must end with the build green again. Do not use intentional failures for code with external side effects.
