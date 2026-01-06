package com.sqq.aimagine.ui.stablediffusion

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.sqq.aimagine.R

class StableDiffusionFragment : Fragment() {

    private lateinit var viewModel: StableDiffusionViewModel
    private lateinit var promptEditText: EditText
    private lateinit var widthEditText: EditText
    private lateinit var heightEditText: EditText
    private lateinit var generateButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var resultImageView: ImageView
    private lateinit var statusTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_stablediffusion, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[StableDiffusionViewModel::class.java]

        // 初始化视图
        promptEditText = view.findViewById(R.id.prompt_edit_text)
        widthEditText = view.findViewById(R.id.width_edit_text)
        heightEditText = view.findViewById(R.id.height_edit_text)
        generateButton = view.findViewById(R.id.generate_button)
        progressBar = view.findViewById(R.id.progress_bar)
        resultImageView = view.findViewById(R.id.result_image_view)
        statusTextView = view.findViewById(R.id.status_text_view)

        // 设置按钮点击事件
        generateButton.setOnClickListener {
            val prompt = promptEditText.text.toString()
            val widthStr = widthEditText.text.toString()
            val heightStr = heightEditText.text.toString()
            
            if (prompt.isEmpty()) {
                Toast.makeText(context, "Please enter a prompt", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val width = widthStr.toIntOrNull() ?: 512
            val height = heightStr.toIntOrNull() ?: 512
            
            if (width < 64 || width > 2048) {
                Toast.makeText(context, "Width must be between 64 and 2048", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (height < 64 || height > 2048) {
                Toast.makeText(context, "Height must be between 64 and 2048", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            viewModel.generateImage(prompt, width, height)
        }

        // 观察ViewModel数据
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            generateButton.isEnabled = !isLoading
        }

        viewModel.statusMessage.observe(viewLifecycleOwner) { message ->
            statusTextView.text = message
        }

        viewModel.generatedImage.observe(viewLifecycleOwner) { bitmap ->
            resultImageView.setImageBitmap(bitmap)
        }

        // 添加设置按钮到工具栏
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.stablediffusion_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_settings -> {
                        findNavController().navigate(R.id.nav_settings)
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }
}
