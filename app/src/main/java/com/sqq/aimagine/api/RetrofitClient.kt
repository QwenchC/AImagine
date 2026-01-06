package com.sqq.aimagine.api

import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object RetrofitClient {
    
    private var retrofit: Retrofit? = null
    private var currentBaseUrl: String = ""
    
    fun getClient(baseUrl: String): Retrofit {
        // 如果 baseUrl 改变了，重新创建 Retrofit 实例
        if (retrofit == null || currentBaseUrl != baseUrl) {
            currentBaseUrl = baseUrl
            
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            
            val clientBuilder = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(600, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .callTimeout(600, TimeUnit.SECONDS)
                // 配置连接池：最多5个空闲连接，保持5分钟
                .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
                // 启用重试
                .retryOnConnectionFailure(true)
            
            // 如果是HTTPS，添加信任所有证书的配置（仅用于开发环境）
            if (baseUrl.startsWith("https://", ignoreCase = true)) {
                val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                })
                
                val sslContext = SSLContext.getInstance("TLS")
                sslContext.init(null, trustAllCerts, SecureRandom())
                
                clientBuilder.sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                clientBuilder.hostnameVerifier { _, _ -> true }
            }
            
            val client = clientBuilder.build()
            
            retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        
        return retrofit!!
    }
    
    fun getApi(baseUrl: String): StableDiffusionApi {
        return getClient(baseUrl).create(StableDiffusionApi::class.java)
    }
    
    // 重置客户端，用于强制重新创建连接
    fun reset() {
        retrofit = null
        currentBaseUrl = ""
    }
}
