package com.manichord.viperedit.files

sealed class Result
data class Success
(
        val fileText: String? = null,
        val fileName: String? = null,
        val fileExtension: String? = null,
        val encoding: String? = null
) : Result()
object Failure : Result()