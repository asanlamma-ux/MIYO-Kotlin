package com.miyu.reader.domain.model

data class OpdsCatalog(
    val id: String,
    val title: String,
    val url: String,
    val addedAt: String,
    val isDefault: Boolean = false,
)

data class OpdsLink(
    val href: String,
    val rel: String = "alternate",
    val type: String? = null,
    val title: String? = null,
)

data class OpdsEntry(
    val id: String,
    val title: String,
    val author: String,
    val summary: String? = null,
    val coverUrl: String? = null,
    val thumbnailUrl: String? = null,
    val acquisitionLinks: List<OpdsLink> = emptyList(),
    val navigationLinks: List<OpdsLink> = emptyList(),
)

data class OpdsFeed(
    val title: String,
    val url: String,
    val entries: List<OpdsEntry>,
    val selfUrl: String? = null,
    val nextUrl: String? = null,
    val previousUrl: String? = null,
)

data class AddOpdsCatalogResult(
    val catalogs: List<OpdsCatalog>,
    val addedCatalog: OpdsCatalog,
    val alreadySaved: Boolean,
)
