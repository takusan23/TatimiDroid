package io.github.takusan23.tatimidroid.NicoAPI

data class ProgramData(
    val title: String,
    val communityName: String,
    val beginAt: String,
    val endAt: String,
    val programId: String,
    val broadCaster: String,
    val lifeCycle: String,
    val isOfficial: Boolean = false
)