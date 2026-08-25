package com.asciicity.world

@JvmInline
value class EntityId(val value: String) : Comparable<EntityId> {
    init {
        require(value.matches(ID_PATTERN)) {
            "Entity ID '$value' must contain only lowercase letters, numbers, dots, underscores, or hyphens"
        }
    }

    override fun compareTo(other: EntityId): Int = value.compareTo(other.value)

    override fun toString(): String = value

    companion object {
        private val ID_PATTERN = Regex("[a-z0-9][a-z0-9._-]*")
    }
}
