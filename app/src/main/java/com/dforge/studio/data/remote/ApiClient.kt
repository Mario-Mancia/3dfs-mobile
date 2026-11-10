package com.dforge.studio.data.remote

import com.dforge.studio.BuildConfig
import com.dforge.studio.data.local.SessionStore
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    /** Gson que traduce camelCase (Kotlin) <-> snake_case (Django). */
    val gson = GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .create()

    fun crear(baseUrl: String, sesion: SessionStore): DForgeApi {
        // Agrega "Authorization: Bearer <token>" a cada petición si hay sesión.
        val autenticacion = Interceptor { chain ->
            val token = sesion.tokenEnMemoria ?: runBlocking { sesion.cargar() }
            val peticion = chain.request().newBuilder().apply {
                if (!token.isNullOrBlank()) header("Authorization", "Bearer $token")
                header("Accept", "application/json")
            }.build()
            chain.proceed(peticion)
        }

        val registro = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
            else HttpLoggingInterceptor.Level.NONE
        }

        val cliente = OkHttpClient.Builder()
            .addInterceptor(autenticacion)
            .addInterceptor(registro)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)   // subida de archivos 3D
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(cliente)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(DForgeApi::class.java)
    }
}
