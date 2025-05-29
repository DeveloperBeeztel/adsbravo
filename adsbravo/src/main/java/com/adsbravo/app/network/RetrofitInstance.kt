package com.adsbravo.app.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("http://feed01.feedgarum.com/") // Dominio base (se sobreescribe con @Url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val adService: AdService by lazy {
        retrofit.create(AdService::class.java)
    }
}
