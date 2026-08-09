package com.chenyang.cywms.data.model

data class PageResult<T>(
    val records: List<T> = emptyList(),
    val total: Int = 0,
    val current: Int = 0,
    val size: Int = 0
)

data class FileDownloadRecord(
    val id: String? = null,
    val filename: String? = null,
    val fileurl: String? = null,
    val fileversion: String? = null,
    val remark: String? = null,
    val createTime: String? = null
)
