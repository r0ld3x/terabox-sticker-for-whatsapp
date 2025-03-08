package com.r0ld3x.telegramsticker

object Helper {

    fun extractStickerName(input: String): String {
        val regex = "/addstickers/([^/]+)/?$|^([^/]+)$".toRegex()
        return regex.find(input)?.let { matchResult ->
            matchResult.groupValues[1].takeIf { it.isNotEmpty() } ?: matchResult.groupValues[2]
        } ?: ""
    }
}