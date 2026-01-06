package com.sqq.aimagine.ui.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.sqq.aimagine.R
import com.sqq.aimagine.api.RetrofitClient
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private lateinit var viewModel: SettingsViewModel
    private lateinit var apiUrlEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var testConnectionButton: Button
    private lateinit var connectionStatusText: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[SettingsViewModel::class.java]

        // 初始化视图
        apiUrlEditText = view.findViewById(R.id.api_url_edit_text)
        saveButton = view.findViewById(R.id.save_button)
        testConnectionButton = view.findViewById(R.id.test_connection_button)
        connectionStatusText = view.findViewById(R.id.connection_status_text)

        // 加载已保存的设置
        loadSettings()

        // 保存按钮点击事件
        saveButton.setOnClickListener {
            saveSettings()
        }

        // 测试连接按钮点击事件
        testConnectionButton.setOnClickListener {
            testConnection()
        }
    }

    private fun loadSettings() {
        val sharedPrefs = requireContext().getSharedPreferences("aimagine_prefs", Context.MODE_PRIVATE)
        val apiUrl = sharedPrefs.getString("sd_api_url", "http://127.0.0.1:7860/")
        apiUrlEditText.setText(apiUrl)
    }

    private fun saveSettings() {
        val apiUrl = apiUrlEditText.text.toString()
        if (apiUrl.isEmpty()) {
            Toast.makeText(context, "Please enter API URL", Toast.LENGTH_SHORT).show()
            return
        }

        val sharedPrefs = requireContext().getSharedPreferences("aimagine_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().apply {
            putString("sd_api_url", apiUrl)
            apply()
        }

        Toast.makeText(context, "Settings saved", Toast.LENGTH_SHORT).show()
    }

    private fun testConnection() {
        val apiUrl = apiUrlEditText.text.toString()
        if (apiUrl.isEmpty()) {
            connectionStatusText.text = "Please enter API URL first"
            return
        }

        connectionStatusText.text = "Testing connection..."
        testConnectionButton.isEnabled = false

        lifecycleScope.launch {
            try {
                val baseUrl = if (apiUrl.endsWith("/")) apiUrl else "$apiUrl/"
                val api = RetrofitClient.getApi(baseUrl)
                val response = api.getOptions()

                if (response.isSuccessful) {
                    connectionStatusText.text = "✓ Connection successful!"
                    connectionStatusText.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
                } else {
                    connectionStatusText.text = "✗ Connection failed: ${response.code()}"
                    connectionStatusText.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
                }
            } catch (e: Exception) {
                connectionStatusText.text = "✗ Error: ${e.message}"
                connectionStatusText.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
            } finally {
                testConnectionButton.isEnabled = true
            }
        }
    }
}
