package com.adsbravo.app.network

import com.adsbravo.app.model.AdData
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Url

interface AdService {
    @GET
    suspend fun getAd(@Url fullUrl: String): Response<AdData>

}
