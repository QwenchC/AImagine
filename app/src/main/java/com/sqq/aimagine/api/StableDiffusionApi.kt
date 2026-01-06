package com.sqq.aimagine.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface StableDiffusionApi {
    
    @POST("sdapi/v1/txt2img")
    suspend fun textToImage(@Body request: TextToImageRequest): Response<TextToImageResponse>
    
    @GET("sdapi/v1/options")
    suspend fun getOptions(): Response<Any>
}

data class TextToImageRequest(
    val prompt: String,
    val negative_prompt: String = "",
    val steps: Int = 20,
    val cfg_scale: Double = 7.0,
    val width: Int = 512,
    val height: Int = 512,
    val sampler_name: String = "Euler",
    val batch_size: Int = 1,
    val n_iter: Int = 1,
    val seed: Long = -1
)

data class TextToImageResponse(
    val images: List<String>,
    val parameters: Map<String, Any>?,
    val info: String?
)
