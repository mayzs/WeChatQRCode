package com.king.wechat.qrcode.app

import android.content.Intent
import android.graphics.Point
import android.widget.ImageView
import androidx.activity.addCallback
import com.king.camera.scan.AnalyzeResult
import com.king.camera.scan.CameraScan
import com.king.camera.scan.analyze.Analyzer
import com.king.camera.scan.util.PointUtils
import com.king.logx.LogX
import com.king.logx.logger.LogFormat
import com.king.wechat.qrcode.DecodeResult
import com.king.wechat.qrcode.scanning.WeChatCameraScanActivity
import com.king.wechat.qrcode.scanning.analyze.WeChatScanningAnalyzer

/**
 * 微信二维码扫描实现示例
 *
 * @author <a href="mailto:jenly1314@gmail.com">Jenly</a>
 * <p>
 * <a href="https://github.com/jenly1314">Follow me</a>
 */
class WeChatQRCodeActivity : WeChatCameraScanActivity() {

    private lateinit var ivResult: ImageView

    override fun initUI() {
        super.initUI()
        ivResult = findViewById(R.id.ivResult)

        onBackPressedDispatcher.addCallback(this) {
            // 如果是结果点显示时，用户点击了返回键，则认为是取消选择当前结果，重新开始扫码
            if (viewfinderView!!.isShowPoints) {
                ivResult.setImageResource(0)
                viewfinderView!!.showScanner()
                cameraScan.setAnalyzeImage(true)
            } else {
                finish()
            }
        }
    }

    override fun onScanResultCallback(result: AnalyzeResult<DecodeResult>) {
        // 停止分析
        cameraScan.setAnalyzeImage(false)
        val decodeResult = result.result
        LogX.d(decodeResult.toString())
        val width = result.imageWidth
        val height = result.imageHeight
        val vertexPoints = decodeResult.getVertexPoints()

        if (vertexPoints.isNotEmpty()) { // 如果需要处理结果二维码的位置信息
            //取预览当前帧图片并显示，为结果点提供参照
            ivResult.setImageBitmap(previewView.bitmap)
            val points = ArrayList<Point>()
            vertexPoints.chunked(4).forEach { quad ->
                if (quad.size < 4) {
                    return@forEach
                }
                // 扫码结果二维码的四个点（一个矩形）
                LogX.format(LogFormat.PLAIN).d("point0: ${quad[0].x}, ${quad[0].y}")
                LogX.format(LogFormat.PLAIN).d("point1: ${quad[1].x}, ${quad[1].y}")
                LogX.format(LogFormat.PLAIN).d("point2: ${quad[2].x}, ${quad[2].y}")
                LogX.format(LogFormat.PLAIN).d("point3: ${quad[3].x}, ${quad[3].y}")

                val centerX = ((quad[0].x + quad[1].x + quad[2].x + quad[3].x) / 4).toInt()
                val centerY = ((quad[0].y + quad[1].y + quad[2].y + quad[3].y) / 4).toInt()

                //将实际的结果中心点坐标转换成界面预览的坐标
                val point = PointUtils.transform(
                    centerX,
                    centerY,
                    width,
                    height,
                    viewfinderView!!.width,
                    viewfinderView!!.height
                )
                points.add(point)
            }
            //设置Item点击监听
            viewfinderView!!.setOnItemClickListener {
                //显示点击Item将所在位置扫码识别的结果返回
                val intent = Intent()
                intent.putExtra(CameraScan.SCAN_RESULT, decodeResult.texts[it])
                setResult(RESULT_OK, intent)
                finish()
            }
            //显示结果点信息
            viewfinderView!!.showResultPoints(points)

            if (decodeResult.texts.size == 1) {
                val intent = Intent()
                intent.putExtra(CameraScan.SCAN_RESULT, decodeResult.texts[0])
                setResult(RESULT_OK, intent)
                finish()
            }
        } else {
            // 一般需求都是识别一个码，所以这里取第0个就可以；有识别多个码的需求，可以取全部
            val intent = Intent()
            intent.putExtra(CameraScan.SCAN_RESULT, decodeResult.texts[0])
            setResult(RESULT_OK, intent)
            finish()
        }
    }

    override fun createAnalyzer(): Analyzer<DecodeResult> {
        // 分析器默认不会返回结果二维码的位置信息
//        return WeChatScanningAnalyzer()
        // 如果需要返回结果二维码位置信息，则初始化分析器时，isOutputVertices参数传 true 即可
        return WeChatScanningAnalyzer(true)
    }

    override fun getLayoutId(): Int {
        return R.layout.activity_wechat_qrcode
    }

}
