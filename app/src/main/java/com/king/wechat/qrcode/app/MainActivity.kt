package com.king.wechat.qrcode.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.lifecycleScope
import com.king.camera.scan.CameraScan
import com.king.logx.LogX
import com.king.opencv.qrcode.OpenCVQRCodeDetector
import com.king.wechat.qrcode.WeChatQRCodeDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.opencv.OpenCV

/**
 * 示例
 * @author <a href="mailto:jenly1314@gmail.com">Jenly</a>
 * <p>
 * <a href="https://github.com/jenly1314">Follow me</a>
 */
class MainActivity : AppCompatActivity() {

    /**
     * OpenCVQRCodeDetector
     */
    private val openCVQRCodeDetector by lazy {
        OpenCVQRCodeDetector()
    }

    /**
     * 是否使用 WeChatQRCodeDetector 进行检测二维码
     */
    private var useWeChatDetect = false

    private val startActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            processQRCodeResult(result.data)
        }
    }

    private val pickPhotoLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.also {
            processPickPhotoResult(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // 初始化OpenCV
        OpenCV.initOpenCV()
        // 初始化WeChatQRCodeDetector
        WeChatQRCodeDetector.init(this)
    }

    private fun getContext() = this

    /**
     * 处理选择图片后，从图片中检测二维码结果
     */
    private fun processPickPhotoResult(uri: Uri) {
        lifecycleScope.launch {
            try {
                val bitmap = getBitmapFromUri(getContext(), uri)
                if (useWeChatDetect) {
                    val result = withContext(Dispatchers.IO) {
                        // 通过WeChatQRCodeDetector识别图片中的二维码
                        WeChatQRCodeDetector.detectAndDecode(bitmap)
                    }
                    if (result.isNotEmpty()) {// 不为空，则表示识别成功
                        // 打印所有结果
                        for ((index, text) in result.withIndex()) {
                            LogX.d("result$index:$text")
                        }
                        // 一般需求都是识别一个码，所以这里取第0个就可以；如有识别多个码的需求，也可以取全部
                        Toast.makeText(getContext(), result[0], Toast.LENGTH_SHORT).show()
                    } else {
                        // 为空表示识别失败
                        LogX.d("result = null")
                    }
                } else {
                    val result = withContext(Dispatchers.IO) {
                        // 通过OpenCVQRCodeDetector识别图片中的二维码
                        openCVQRCodeDetector.detectAndDecode(bitmap)
                    }

                    if (!result.isNullOrEmpty()) {// 不为空，则表示识别成功
                        LogX.d("result: $result")
                        Toast.makeText(getContext(), result, Toast.LENGTH_SHORT).show()
                    } else {
                        // 为空表示识别失败
                        LogX.d("result = null")
                    }
                }
            } catch (e: Exception) {
                LogX.w(e)
            }
        }
    }

    private fun processQRCodeResult(intent: Intent?) {
        // 扫码结果
        CameraScan.parseScanResult(intent)?.let {
            LogX.d("result: %s", it)
            Toast.makeText(getContext(), it, Toast.LENGTH_SHORT).show()
        }
    }

    private fun pickPhoto(useWeChatDetect: Boolean) {
        this.useWeChatDetect = useWeChatDetect
        pickPhotoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }


    private fun startActivityForResult(clazz: Class<*>) {
        val options = ActivityOptionsCompat.makeCustomAnimation(
            this, R.anim.alpha_in, R.anim.alpha_out
        )
        startActivityLauncher.launch(Intent(this, clazz), options)
    }

    fun onClick(view: View) {
        when (view.id) {
            R.id.btnWeChatQRCodeScan -> startActivityForResult(WeChatQRCodeActivity::class.java)
            R.id.btnWeChatMultiQRCodeScan -> startActivityForResult(WeChatMultiQRCodeActivity::class.java)
            R.id.btnWeChatQRCodeDecode -> pickPhoto(true)
            R.id.btnOpenCVQRCodeScan -> startActivityForResult(OpenCVQRCodeActivity::class.java)
            R.id.btnOpenCVQRCodeDecode -> pickPhoto(false)
        }
    }

}
