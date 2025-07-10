package com.king.wechat.qrcode;

import android.content.Context;
import android.graphics.Bitmap;

import com.king.logx.LogX;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.wechat_qrcode.WeChatQRCode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

/**
 * 微信二维码检测器
 *
 * @author <a href="mailto:jenly1314@gmail.com">Jenly</a>
 * <p>
 * <a href="https://github.com/jenly1314">Follow me</a>
 */
@SuppressWarnings("unused")
public final class WeChatQRCodeDetector {

    private static final String TAG = "WeChatQRCodeDetector";

    private static final String MODEL_DIR = "models";
    private static final String DETECT_PROTO_TXT = "detect.prototxt";
    private static final String DETECT_CAFFE_MODEL = "detect.caffemodel";
    private static final String SR_PROTO_TXT = "sr.prototxt";
    private static final String SR_CAFFE_MODEL = "sr.caffemodel";

    private volatile static WeChatQRCode sWeChatQRCode;

    private WeChatQRCodeDetector() {
        throw new AssertionError();
    }

    /**
     * 初始化
     *
     * @param context {@link Context}
     */
    public static void init(Context context) {
        initWeChatQRCode(context.getApplicationContext());
    }

    /**
     * 初始化 WeChatQRCode
     *
     * @param context {@link Context}
     */
    private static void initWeChatQRCode(Context context) {
        try {
            String saveDirPath = getExternalFilesDir(context, MODEL_DIR);
            String[] models = new String[]{DETECT_PROTO_TXT, DETECT_CAFFE_MODEL, SR_PROTO_TXT, SR_CAFFE_MODEL};

            File saveDir = new File(saveDirPath);
            boolean exists = saveDir.exists();

            if (exists) {
                for (String model : models) {
                    if (!new File(saveDirPath, model).exists()) {
                        exists = false;
                        break;
                    }
                }
            }

            if (!exists) {
                // 模型文件只要有一个不存在，则遍历拷贝
                for (String model : models) {
                    InputStream inputStream = context.getAssets()
                        .open(MODEL_DIR + File.separatorChar + model);
                    File saveFile = new File(saveDir, model);
                    FileOutputStream outputStream = new FileOutputStream(saveFile);
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, len);
                    }
                    outputStream.flush();
                    inputStream.close();
                    outputStream.close();
                    LogX.d("file: %s", saveFile.getAbsolutePath());
                }
            }
            sWeChatQRCode = new WeChatQRCode(
                saveDirPath + File.separatorChar + models[0],
                saveDirPath + File.separatorChar + models[1],
                saveDirPath + File.separatorChar + models[2],
                saveDirPath + File.separatorChar + models[3]);
            LogX.d("WeChatQRCode loaded successfully");
        } catch (Exception e) {
            LogX.e(e);
        }
    }

    /**
     * 获取外部存储目录
     *
     * @param context {@link Context}
     * @param path    目录路径
     * @return 外部存储目录
     */
    private static String getExternalFilesDir(Context context, String path) {
        File[] files = context.getExternalFilesDirs(path);
        if (files != null && files.length > 0) {
            File file = files[0];
            if (file != null) {
                return file.getAbsolutePath();
            }
        }
        File file = context.getExternalFilesDir(path);
        if (file == null) {
            file = new File(context.getFilesDir(), path);
        }
        return file.getAbsolutePath();
    }

    /**
     * set scale factor
     * QR code detector use neural network to detect QR.
     * Before running the neural network, the input image is pre-processed by scaling.
     * By default, the input image is scaled to an image with an area of 160000 pixels.
     * The scale factor allows to use custom scale the input image:
     * width = scaleFactor*width
     * height = scaleFactor*width
     * <p>
     * scaleFactor valuse must be &gt; 0 and &lt;= 1, otherwise the scaleFactor value is set to -1
     * and use default scaled to an image with an area of 160000 pixels.
     *
     * @param scalingFactor automatically generated
     */
    public void setScaleFactor(float scalingFactor) {
        sWeChatQRCode.setScaleFactor(scalingFactor);
    }

    /**
     * get scale factor
     */
    public float getScaleFactor() {
        return sWeChatQRCode.getScaleFactor();
    }

    /**
     * Both detects and decodes QR code.
     * To simplify the usage, there is a only API: detectAndDecode
     *
     * @param bitmap {@link Bitmap}
     * @return list of decoded string.
     */
    public static List<String> detectAndDecode(Bitmap bitmap) {
        Mat mat = new Mat();
        try {
            Utils.bitmapToMat(bitmap, mat);
            return detectAndDecode(mat);
        } finally {
            mat.release();
        }
    }

    /**
     * Both detects and decodes QR code.
     * To simplify the usage, there is a only API: detectAndDecode
     *
     * @param bitmap {@link Bitmap}
     * @param points optional output array of vertices of the found QR code quadrangle. Will be
     *               empty if not found.
     * @return list of decoded string.
     */
    public static List<String> detectAndDecode(Bitmap bitmap, List<Mat> points) {
        Mat mat = new Mat();
        try {
            Utils.bitmapToMat(bitmap, mat);
            return detectAndDecode(mat, points);
        } finally {
            mat.release();
        }
    }

    /**
     * Both detects and decodes QR code.
     * To simplify the usage, there is a only API: detectAndDecode
     *
     * @param img supports grayscale or color (BGR) image.
     *            empty if not found.
     * @return list of decoded string.
     */
    public static List<String> detectAndDecode(Mat img) {
        return sWeChatQRCode.detectAndDecode(img);
    }

    /**
     * Both detects and decodes QR code.
     * To simplify the usage, there is a only API: detectAndDecode
     *
     * @param img    supports grayscale or color (BGR) image.
     * @param points optional output array of vertices of the found QR code quadrangle. Will be
     *               empty if not found.
     * @return list of decoded string.
     */
    public static List<String> detectAndDecode(Mat img, List<Mat> points) {
        return sWeChatQRCode.detectAndDecode(img, points);
    }

}
