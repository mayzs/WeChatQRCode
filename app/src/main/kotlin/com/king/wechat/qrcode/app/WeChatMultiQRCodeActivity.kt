package com.king.wechat.qrcode.app

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Path
import android.view.View
import com.king.app.dialog.AppDialog
import com.king.app.dialog.AppDialogConfig
import com.king.camera.scan.AnalyzeResult
import com.king.camera.scan.CameraScan
import com.king.camera.scan.analyze.Analyzer
import com.king.logx.LogX
import com.king.logx.logger.LogFormat
import com.king.wechat.qrcode.DecodeResult
import com.king.wechat.qrcode.scanning.WeChatCameraScanActivity
import com.king.wechat.qrcode.scanning.analyze.WeChatScanningAnalyzer

/**
 * 微信多二维码扫描实现示例
 *
 * @author <a href="mailto:jenly1314@gmail.com">Jenly</a>
 * <p>
 * <a href="https://github.com/jenly1314">Follow me</a>
 */
class WeChatMultiQRCodeActivity : WeChatCameraScanActivity() {

    override fun onScanResultCallback(result: AnalyzeResult<DecodeResult>) {
        // 停止分析
        cameraScan.setAnalyzeImage(false)
        val decodeResult = result.result
        val vertexPoints = decodeResult.getVertexPoints()
        LogX.d(decodeResult.toString())
        if (vertexPoints.isNotEmpty()) { // 如果需要处理结果二维码的位置信息

            val buffer = StringBuilder()
            val bitmap = result.bitmap?.drawRect { canvas, paint ->
                // 扫码结果可能有多个
                decodeResult.texts.forEachIndexed { index, data ->
                    buffer.append("[$index] ").append(data).append("\n")
                }

                vertexPoints.chunked(4).forEach { quad ->
                    if (quad.size < 4) {
                        return@forEach
                    }
                    // 扫码结果二维码的四个点（一个矩形）
                    LogX.format(LogFormat.PLAIN).d("point0: ${quad[0].x}, ${quad[0].y}")
                    LogX.format(LogFormat.PLAIN).d("point1: ${quad[1].x}, ${quad[1].y}")
                    LogX.format(LogFormat.PLAIN).d("point2: ${quad[2].x}, ${quad[2].y}")
                    LogX.format(LogFormat.PLAIN).d("point3: ${quad[3].x}, ${quad[3].y}")

                    val path = Path()
                    path.moveTo(quad[0].x.toFloat(), quad[0].y.toFloat())
                    path.lineTo(quad[1].x.toFloat(), quad[1].y.toFloat())
                    path.lineTo(quad[2].x.toFloat(), quad[2].y.toFloat())
                    path.lineTo(quad[3].x.toFloat(), quad[3].y.toFloat())
                    path.lineTo(quad[0].x.toFloat(), quad[0].y.toFloat())
                    // 将二维码位置在图片上框出来
                    canvas.drawPath(path, paint)
                }

            }

            val config = AppDialogConfig(this, R.layout.qrcode_result_dialog).apply {
                content = buffer
                onClickConfirm = View.OnClickListener {
                    AppDialog.dismissDialog()
                    // 继续扫码分析
                    cameraScan.setAnalyzeImage(true)
                }
                onClickCancel = View.OnClickListener {
                    AppDialog.dismissDialog()
                    finish()
                }
                viewHolder.setImageBitmap(R.id.ivDialogContent, bitmap)
            }
            AppDialog.showDialog(config, false)

        } else {
            // 一般需求都是识别一个码，所以这里取第0个就可以；有识别多个码的需求，可以取全部
            val text = decodeResult.texts[0]
            val intent = Intent()
            intent.putExtra(CameraScan.SCAN_RESULT, text)
            setResult(RESULT_OK, intent)
            finish()
        }
    }

    override fun createAnalyzer(): Analyzer<DecodeResult> {
        // 分析器默认不会返回结果二维码的位置信息
//        return WeChatScanningAnalyzer()
        // 如果需要返回结果二维码位置信息，则初始化分析器时，参数传 true 即可
        return WeChatScanningAnalyzer(true)
    }

}
