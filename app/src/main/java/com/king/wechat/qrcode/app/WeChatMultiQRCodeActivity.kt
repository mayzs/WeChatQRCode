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

    @SuppressLint("LongLogTag")
    override fun onScanResultCallback(result: AnalyzeResult<List<String>>) {
        // 停止分析
        cameraScan.setAnalyzeImage(false)
        LogX.d(result.result.toString())
        // 当初始化 WeChatScanningAnalyzer 时，如果是需要二维码的位置信息，则可通过 WeChatScanningAnalyzer.QRCodeAnalyzeResult 获取
        if (result is WeChatScanningAnalyzer.QRCodeAnalyzeResult) { // 如果需要处理结果二维码的位置信息

            val buffer = StringBuilder()
            val bitmap = result.bitmap?.drawRect { canvas, paint ->
                // 扫码结果可能有多个
                result.result.forEachIndexed { index, data ->
                    buffer.append("[$index] ").append(data).append("\n")
                }

                result.points?.forEach { mat ->
                    // 扫码结果二维码的四个点（一个矩形）
                    LogX.format(LogFormat.PLAIN).d("point0: ${mat[0, 0][0]}, ${mat[0, 1][0]}")
                    LogX.format(LogFormat.PLAIN).d("point1: ${mat[1, 0][0]}, ${mat[1, 1][0]}")
                    LogX.format(LogFormat.PLAIN).d("point2: ${mat[2, 0][0]}, ${mat[2, 1][0]}")
                    LogX.format(LogFormat.PLAIN).d("point3: ${mat[3, 0][0]}, ${mat[3, 1][0]}")

                    val path = Path()
                    path.moveTo(mat[0, 0][0].toFloat(), mat[0, 1][0].toFloat())
                    path.lineTo(mat[1, 0][0].toFloat(), mat[1, 1][0].toFloat())
                    path.lineTo(mat[2, 0][0].toFloat(), mat[2, 1][0].toFloat())
                    path.lineTo(mat[3, 0][0].toFloat(), mat[3, 1][0].toFloat())
                    path.lineTo(mat[0, 0][0].toFloat(), mat[0, 1][0].toFloat())
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
            val text = result.result[0]
            val intent = Intent()
            intent.putExtra(CameraScan.SCAN_RESULT, text)
            setResult(RESULT_OK, intent)
            finish()
        }
    }

    override fun createAnalyzer(): Analyzer<MutableList<String>> {
        // 分析器默认不会返回结果二维码的位置信息
//        return WeChatScanningAnalyzer()
        // 如果需要返回结果二维码位置信息，则初始化分析器时，参数传 true 即可
        return WeChatScanningAnalyzer(true)
    }

}
