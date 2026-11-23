package com.shin.depthflow.data

import com.shin.depthflow.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Process-wide Retrofit client so every repository/ViewModel shares one OkHttp pool.
 * The base URL comes from BuildConfig.BASE_URL (prod, or a local override in debug builds).
 */
object ApiClient {
    val moshi: Moshi by lazy { Moshi.Builder().add(KotlinJsonAdapterFactory()).build() }

    val service: DepthFlowApiService? by lazy {
        try {
            val builder = OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                // Generation can take up to ~5 min server side.
                .readTimeout(300, TimeUnit.SECONDS)
            if (BuildConfig.DEBUG) {
                // Bodies contain bearer tokens: log them in debug builds only.
                builder.addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                })
            }
            Retrofit.Builder()
                .baseUrl(BuildConfig.BASE_URL)
                .client(builder.build())
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(DepthFlowApiService::class.java)
        } catch (e: Exception) {
            null
        }
    }
}
