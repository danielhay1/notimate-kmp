package com.hayduck.notemate.domain.agent

enum class AgentOutcome {
    IGNORE,
    DRAFT,
    REVIEW,
    CONFIRM_AND_WRITE,
    DEFER,
    FAILED,
}
