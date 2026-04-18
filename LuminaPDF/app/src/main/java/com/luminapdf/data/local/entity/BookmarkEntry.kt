package com.luminapdf.data.local.entity

import kotlinx.serialization.Serializable

@Serializable
data class BookmarkEntry(
    val id: String,
    val pageIndex: Int,
    val label: String,
    val createdAt: Long = System.currentTimeMillis()
)
