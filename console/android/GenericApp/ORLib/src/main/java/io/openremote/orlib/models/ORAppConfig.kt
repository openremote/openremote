package io.openremote.orlib.models

data class ORAppConfig(
    val id: Int,
    val realm: String,
    val initialUrl: String,
    val url: String,
    val menuEnabled: Boolean = false,
    val menuPosition: String = "BOTTOM_LEFT",
    val menuImage: String?,
    val primaryColor: String?,
    val secondaryColor: String?,
    val links: List<ORLinkConfig>?
)