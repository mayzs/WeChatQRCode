package com.king.opencv.qrcode.scanning.analyze;

import android.graphics.ImageFormat;

import com.king.camera.scan.AnalyzeResult;
import com.king.camera.scan.FrameMetadata;
import com.king.camera.scan.analyze.Analyzer;
import com.king.camera.scan.util.ImageUtils;
import com.king.logx.LogX;
import com.king.opencv.qrcode.OpenCVQRCodeDetector;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.ImageProxy;

/**
 * OpenCV二维码分析器：分析相机预览的帧数据，从中检测识别二维码
 *
 * @author <a href="mailto:jenly1314@gmail.com">Jenly</a>
 * <p>
 * <a href="https://github.com/jenly1314">Follow me</a>
 */
public class OpenCVScanningAnalyzer implements Analyzer<List<String>> {

    private static final int ROTATION_90 = 90;
    private static final int ROTATION_180 = 180;
    private static final int ROTATION_270 = 270;

    private final Queue<byte[]> queue = new ConcurrentLinkedQueue<>();

    private final AtomicBoolean joinQueue = new AtomicBoolean(false);

    private final OpenCVQRCodeDetector mDetector = new OpenCVQRCodeDetector();
    /**
     * 是否需要输出二维码的各个顶点
     */
    private final boolean isOutputVertices;
    /**
     * 是否检测多个二维码
     */
    private final boolean isDetectMultiple;

    public OpenCVScanningAnalyzer() {
        this(false);
    }

    /**
     * 构造
     *
     * @param isOutputVertices 是否需要返回二维码的各个顶点
     */
    public OpenCVScanningAnalyzer(boolean isOutputVertices) {
        this(isOutputVertices, false);
    }

    /**
     * 构造
     *
     * @param isOutputVertices 是否需要返回二维码的各个顶点
     * @param isDetectMultiple 是否需要检测多个二维码
     */
    public OpenCVScanningAnalyzer(boolean isOutputVertices, boolean isDetectMultiple) {
        this.isOutputVertices = isOutputVertices;
        this.isDetectMultiple = isDetectMultiple;
    }

    @Override
    public void analyze(@NonNull ImageProxy imageProxy, @NonNull Analyzer.OnAnalyzeListener<List<String>> listener) {
        if (!joinQueue.get()) {
            int imageSize = imageProxy.getWidth() * imageProxy.getHeight();
            byte[] bytes = new byte[imageSize + 2 * (imageSize / 4)];
            queue.add(bytes);
            joinQueue.set(true);
        }
        final byte[] nv21Data = queue.poll();
        if(nv21Data == null) {
            return;
        }
        AnalyzeResult<List<String>> result = null;
        try {
            ImageUtils.yuv_420_888toNv21(imageProxy, nv21Data);
            FrameMetadata frameMetadata = new FrameMetadata(
                    imageProxy.getWidth(),
                    imageProxy.getHeight(),
                    imageProxy.getImageInfo().getRotationDegrees());
            result = detectAndDecode(nv21Data, frameMetadata);
        } catch (Exception e) {
            LogX.w(e);
        }
        if (result != null) {
            joinQueue.set(false);
            listener.onSuccess(result);
        } else {
            queue.add(nv21Data);
            listener.onFailure(null);
        }
    }

    /**
     * 检测并识别二维码
     *
     * @param nv21 nv21帧数据
     * @param frameMetadata {@link FrameMetadata}
     * @return 返回识别的二维码结果
     */
    @Nullable
    private AnalyzeResult<List<String>> detectAndDecode(byte[] nv21, FrameMetadata frameMetadata) {
        Mat mat = new Mat(frameMetadata.getHeight() + frameMetadata.getHeight() / 2, frameMetadata.getWidth(), CvType.CV_8UC1);
        mat.put(0,0, nv21);
        Mat bgrMat = new Mat();
        Imgproc.cvtColor(mat, bgrMat, Imgproc.COLOR_YUV2BGR_NV21);
        mat.release();
        rotation(bgrMat, frameMetadata.getRotation());
        if (isOutputVertices) {
            // 如果需要返回二维码的各个顶点
            final Mat points = new Mat();

            List<String> list = null;
            if(isDetectMultiple) {
                list = new ArrayList<>();
                mDetector.detectAndDecodeMulti(bgrMat, list, points);
            } else {
                String result = mDetector.detectAndDecode(bgrMat, points);
                if (result != null && !result.isEmpty()) {
                    list = new ArrayList<>();
                    list.add(result);
                }
            }
            bgrMat.release();

            if(list != null && !list.isEmpty()) {
                return new QRCodeAnalyzeResult<>(nv21, ImageFormat.NV21, frameMetadata, list, points);
            }
        } else {
            // 反之则需识别结果即可
            List<String> list = null;
            if(isDetectMultiple) {
                list = new ArrayList<>();
                mDetector.detectAndDecodeMulti(bgrMat, list);
            } else {
                String result = mDetector.detectAndDecode(bgrMat);
                if (result != null && !result.isEmpty()) {
                    list = new ArrayList<>();
                    list.add(result);
                }
            }
            bgrMat.release();

            if(list != null && !list.isEmpty()) {
                return new QRCodeAnalyzeResult<>(nv21, ImageFormat.NV21, frameMetadata, list);
            }
        }
        return null;
    }

    /**
     * 旋转指定角度
     * @param mat {@link Mat}
     * @param rotation 旋转角度
     */
    private void rotation(Mat mat, int rotation) {
        //  旋转90°
        if (rotation == ROTATION_90) {
            // 将图像逆时针旋转90°，然后再关于x轴对称
            Core.transpose(mat, mat);
            // 然后再绕Y轴旋转180° （顺时针）
            Core.flip(mat, mat, 1);
        } else if (rotation == ROTATION_180) {
            //将图片绕X轴旋转180°（顺时针）
            Core.flip(mat, mat, 0);
            //将图片绕Y轴旋转180°（顺时针）
            Core.flip(mat, mat, 1);
        } else if (rotation == ROTATION_270) {
            // 将图像逆时针旋转90°，然后再关于x轴对称
            Core.transpose(mat, mat);
            // 将图片绕X轴旋转180°（顺时针）
            Core.flip(mat, mat, 0);
        }
    }

    /**
     * 二维码分析结果
     *
     * @param <T>
     */
    public static class QRCodeAnalyzeResult<T> extends AnalyzeResult<T> {

        /**
         * 二维码的位置点信息
         */
        private Mat points;

        public QRCodeAnalyzeResult(@NonNull byte[] imageData, int imageFormat, @NonNull FrameMetadata frameMetadata, @NonNull T result) {
            super(imageData, imageFormat, frameMetadata, result);
        }

        public QRCodeAnalyzeResult(@NonNull byte[] imageData, int imageFormat, @NonNull FrameMetadata frameMetadata, @NonNull T result, @Nullable Mat points) {
            super(imageData, imageFormat, frameMetadata, result);
            this.points = points;
        }

        /**
         * 获取二维码的位置点信息
         *
         * @return 通过 {@link Mat} 返回二维码的位置点信息
         */
        public Mat getPoints() {
            return points;
        }

    }
}
