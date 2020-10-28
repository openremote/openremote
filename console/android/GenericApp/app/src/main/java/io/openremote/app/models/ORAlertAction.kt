package io.openremote.app.models

data class ORAlertAction(
    val url: String,
    val httpMethod: String = "GET",
    val data: Map<String, Any>?,
    val isSilent: Boolean = false,
    val isOpenInBrowser: Boolean = false,
)