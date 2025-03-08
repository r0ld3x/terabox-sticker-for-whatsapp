package com.r0ld3x.telegramsticker.services

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object OkHttpClientSingleton {

    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}