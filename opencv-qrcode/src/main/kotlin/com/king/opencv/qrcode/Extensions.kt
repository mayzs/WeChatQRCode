package com.king.opencv.qrcode

import org.opencv.core.Mat
import org.opencv.core.Point

/**
 * 扩展函数：将检测的结果点 [Mat] 转换为二维码顶点 [Point] 列表（适用于：OpenCVQRCodeDetector）
 *
 * @author <a href="mailto:jenly1314@gmail.com">Jenly</a>
 * <p>
 * <a href="https://github.com/jenly1314">Follow me</a>
 */
fun Mat.toVertexPoints() : List<Point> {
    val list = mutableListOf<Point>()
    val rows = rows()
    for (i in 0 until rows) {
        val mat = row(i)
        val cols = mat.cols()
        for (j in 0 until cols) {
            val x = mat[0, j][0]
            val y = mat[0, j][1]
            list.add(Point(x, y))
        }
    }
    return list
}
