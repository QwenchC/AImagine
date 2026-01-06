package com.sqq.aimagine.ui.stablediffusion

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sqq.aimagine.api.RetrofitClient
import com.sqq.aimagine.api.TextToImageRequest
import kotlinx.coroutines.launch

class StableDiffusionViewModel(application: Application) : AndroidViewModel(application) {

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _statusMessage = MutableLiveData<String>()
    val statusMessage: LiveData<String> = _statusMessage

    private val _generatedImage = MutableLiveData<Bitmap>()
    val generatedImage: LiveData<Bitmap> = _generatedImage

    init {
        _isLoading.value = false
        _statusMessage.value = "Ready to generate"
    }

    fun generateImage(prompt: String, width: Int, height: Int) {
        viewModelScope.launch {
            var retryCount = 0
            val maxRetries = 2
            
            while (retryCount <= maxRetries) {
                try {
                    _isLoading.value = true
                    if (retryCount > 0) {
                        _statusMessage.value = "Retrying... (${retryCount}/${maxRetries})"
                    } else {
                        _statusMessage.value = "Generating image..."
                    }

                    // 获取保存的API地址
                    val sharedPrefs = getApplication<Application>().getSharedPreferences(
                        "aimagine_prefs",
                        Context.MODE_PRIVATE
                    )
                    val apiUrl = sharedPrefs.getString("sd_api_url", "http://127.0.0.1:7860/") ?: "http://127.0.0.1:7860/"

                    // 确保 URL 以 / 结尾
                    val baseUrl = if (apiUrl.endsWith("/")) apiUrl else "$apiUrl/"

                    // 如果是重试，重置客户端
                    if (retryCount > 0) {
                        RetrofitClient.reset()
                    }

                    // 调用 API
                    val api = RetrofitClient.getApi(baseUrl)
                    val request = TextToImageRequest(
                        prompt = prompt,
                        negative_prompt = "",
                        steps = 20,
                        cfg_scale = 7.0,
                        width = width,
                        height = height,
                        sampler_name = "Euler",
                        batch_size = 1,
                        n_iter = 1
                    )
                    
                    val response = api.textToImage(request)
                    
                    if (response.isSuccessful && response.body() != null) {
                        val imageData = response.body()!!
                        if (imageData.images.isNotEmpty()) {
                            // 解码 Base64 图片
                            val base64Image = imageData.images[0]
                            val bitmap = base64ToBitmap(base64Image)
                            _generatedImage.value = bitmap
                            _statusMessage.value = "Image generated successfully!"
                        } else {
                            _statusMessage.value = "No image returned from API"
                        }
                        _isLoading.value = false
                        return@launch // 成功，退出重试循环
                    } else {
                        throw Exception("API Error: ${response.code()} - ${response.message()}")
                    }

                } catch (e: Exception) {
                    if (retryCount < maxRetries && 
                        (e.message?.contains("connection", ignoreCase = true) == true ||
                         e.message?.contains("timeout", ignoreCase = true) == true ||
                         e.message?.contains("closed", ignoreCase = true) == true)) {
                        retryCount++
                        kotlinx.coroutines.delay(1000) // 等待1秒后重试
                    } else {
                        _statusMessage.value = "Error: ${e.message}"
                        _isLoading.value = false
                        e.printStackTrace()
                        return@launch
                    }
                }
            }
            
            // 所有重试都失败
            _statusMessage.value = "Failed after $maxRetries retries. Please try again."
            _isLoading.value = false
        }
    }

    private fun base64ToBitmap(base64String: String): Bitmap {
        val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    }
}
