package com.appswithlove.floaat

data class SelectableProject(
    val projectId: Int,
    val name: String,
    val color: String?
)

data class SelectablePhase(
    val phaseId: Int,
    val projectId: Int,
    val name: String,
    val color: String?
)
