package io.openremote.orlib.models

data class ORConsoleConfig (
    val showAppTextInput: Boolean,
    val showRealmTextInput: Boolean,
    val app: String?,
    val allowedApps: List<String>?,
    val apps: Map<String, ORAppInfo>?
)
