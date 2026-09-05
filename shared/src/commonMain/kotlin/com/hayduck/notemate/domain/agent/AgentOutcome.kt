package com.hayduck.notemate.domain.agent

enum class AgentOutcome {
    IGNORE,
    PROPOSE,
    REVIEW,
    CONFIRM_AND_HANDOFF,
    UNAVAILABLE,
}
