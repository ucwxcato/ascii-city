package com.asciicity.content

data class DialogueDefinition(
    val id: String,
    val speaker: String,
    val lines: List<String>,
)

class DialogueRegistry private constructor(
    dialogues: List<DialogueDefinition>,
) {
    private val byId = dialogues.associateBy(DialogueDefinition::id)

    val size: Int
        get() = byId.size

    fun contains(id: String): Boolean = id in byId

    fun dialogue(id: String): DialogueDefinition = byId[id]
        ?: throw IllegalArgumentException("Unknown dialogue '$id'")

    fun all(): List<DialogueDefinition> = byId.values.toList()

    companion object {
        fun create(dialogues: List<DialogueDefinition>, source: String): DialogueRegistry {
            val ids = mutableSetOf<String>()
            for ((index, dialogue) in dialogues.withIndex()) {
                val path = "$.dialogues[$index]"
                ContentIds.requireStable(dialogue.id, source, "$path.id")
                if (!ids.add(dialogue.id)) {
                    throw ContentValidationException("$source:$path.id: duplicate dialogue ID '${dialogue.id}'")
                }
                if (dialogue.speaker.isBlank()) {
                    throw ContentValidationException("$source:$path.speaker: must not be blank")
                }
                if (dialogue.lines.isEmpty() || dialogue.lines.any(String::isBlank)) {
                    throw ContentValidationException("$source:$path.lines: must contain non-blank dialogue lines")
                }
            }
            return DialogueRegistry(dialogues)
        }
    }
}
