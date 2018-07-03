package com.vondear.camera.tool;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;

import com.vondear.camera.RxCameraView;
import com.vondear.rxtool.RxExifTool;
import com.vondear.rxtool.RxFileTool;
import com.vondear.rxtool.RxTool;
import com.vondear.rxtool.RxVibrateTool;
import com.vondear.rxtool.interfaces.OnRxCamera;
import com.vondear.rxtool.interfaces.OnSimpleListener;
import com.vondear.rxtool.module.photomagic.OnCompressListener;
import com.vondear.rxtool.module.photomagic.RxMagic;
import com.vondear.rxtool.view.RxToast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 *
 * @author Vondear
 * @date 2017/9/22
 */

public class RxCameraTool {

    private static Camera camera;

    /**
     * 打开闪光灯
     *
     * @return
     */
    public static void openFlashLight() {
        try {
            if (camera == null) {
                camera = Camera.open();
                camera.startPreview();
            }
            Camera.Parameters parameters = camera.getParameters();
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            camera.setParameters(parameters);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 关闭闪光灯
     *
     * @return
     */
    public static void closeFlashLight() {
        try {
            if (camera == null) {

            } else {
                Camera.Parameters parameters = camera.getParameters();
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                camera.setParameters(parameters);
                camera.release();
                camera = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void takePic(Context mContext, final RxCameraView mCameraView) {
        if (RxTool.isFastClick(1000)) {
            RxToast.normal("请不要重复点击拍照按钮");
            return;
        } else {
            try {
                if (mCameraView.isCameraOpened()) {
                    RxVibrateTool.vibrateOnce(mContext, 150);
                    RxToast.normal("正在拍照..");
                    if (mCameraView != null) {
                        mCameraView.takePicture();
                    }
                } else {
                    mCameraView.start();
                    RxVibrateTool.vibrateOnce(mContext, 150);
                    RxToast.normal("正在拍照..");
                    RxTool.delayToDo(500, new OnSimpleListener() {
                        @Override
                        public void doSomething() {
                            try {
                                if (mCameraView != null) {
                                    mCameraView.takePicture();
                                }
                            }catch (Exception e) {
//                                Logger.d(e);
                            }
                        }
                    });
                }
            } catch (Exception e) {
//                Logger.d(e);
            }
        }
    }

    public static void initCameraEvent(final Context mContext,
                                       final RxCameraView mCameraView,
                                       final byte[] data,
                                       final String fileDir,
                                       final String picName,
                                       final double mLongitude,
                                       final double mLatitude,
                                       final boolean isEconomize,
                                       final OnRxCamera OnRxCamera) {
        OnRxCamera.onBefore();
        RxTool.getBackgroundHandler().post(new Runnable() {
            @Override
            public void run() {
                File fileParent = new File(fileDir);
                File cacheParent = new File(RxFileTool.getCecheFolder(mContext) + File.separator + "cache" + File.separator + "picture");
                if (!cacheParent.exists()) {
                    cacheParent.mkdirs();
                }
                if (!fileParent.exists()) {
                    fileParent.mkdirs();
                }

                final File cachefile = new File(cacheParent, picName);
                final File compressFile = new File(fileParent, picName);
                OutputStream os = null;
                try {
                    os = new FileOutputStream(cachefile);
                    os.write(data);
                    os.close();

                    RxMagic.with(mContext).
                            load(cachefile).
                            setCompressListener(new OnCompressListener() {
                                @Override
                                public void onStart() {
                                    Log.d("图片压缩", "开始压缩");
                                }

                                @Override
                                public void onSuccess(File file) {
                                    if (RxFileTool.copyOrMoveFile(file, compressFile, true)) {
                                        Log.d("图片压缩", "压缩完成");
                                        OnRxCamera.onSuccessCompress(compressFile);
                                        if (mLongitude != 0 || mLatitude != 0) {
                                            RxExifTool.writeLatLonIntoJpeg(compressFile.getAbsolutePath(), mLatitude, mLongitude);
                                            OnRxCamera.onSuccessExif(compressFile);
                                            RxToast.normal("拍照成功");
                                        } else {
                                            RxToast.error("请先获取定位信息");
                                        }
                                    }
                                }

                                @Override
                                public void onError(Throwable e) {
                                    Log.d("图片压缩", "压缩异常");
//                                    Logger.d(e);
                                }
                            }).launch();

                } catch (IOException e) {
                    Log.w("onPictureTaken", "Cannot write to " + compressFile, e);
                } finally {
                    if (isEconomize) {
                        mCameraView.stop();
                    }
                    if (os != null) {
                        try {
                            os.close();
                        } catch (IOException e) {
                            // Ignore
                        }
                    }
                }
            }
        });
    }
}
