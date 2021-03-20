package com.nanchen.screenrecordhelper;

import android.content.Context;
import android.util.Log;
import android.view.View;

import com.netcompss.ffmpeg4android.CommandValidationException;
import com.netcompss.ffmpeg4android.GeneralUtils;
import com.netcompss.ffmpeg4android.Prefs;
import com.netcompss.loader.LoadJNI;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author 强周亮(qiangzhouliang)
 * @desc git转mp4工具类
 * @email 2538096489@qq.com
 * @time 2021/3/6 16:08
 * @class VideoGifToMp4
 * @package com.zdww.smartvideo.main.videoback.util
 */
public class VideoCutToMp4 {
    // 这个是 ffmpeg 的命令，裁剪的功能主要是 crop 这个命令，它的参数分别代表的是：输出视频的宽：输入出视频的高：裁剪的起始点的 x 的位置：裁剪起始点的 y 的位置。
    //这四个值可以根据需要去计算 960:53:0:280
    private static String mStrCmd = " -strict -2 -vf crop=";
    private static String mStrCmdPre = "ffmpeg -y -i ";
//    private static String mOutputFile = FileUtils.getPath("video");

    private static String workFolder = null;
    private static String demoVideoFolder = null;
    private static String vkLogPath = null;
    private static LoadJNI vk;
    private static boolean commandValidationFailedFlag = false;

    private int left = 0,right = 0,height,y;

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getLeft() {
        return left;
    }

    public void setLeft(int left) {
        this.left = left;
    }

    public int getRight() {
        return right;
    }

    public void setRight(int right) {
        this.right = right;
    }

    public VideoCutToMp4(Context context,int left, int right, int height, int y) {
        init(context);
        this.left = left;
        this.right = right;
        this.height = height;
        this.y = y;
    }

    public VideoCutToMp4(Context context){
        init(context);
    }
    private static void init(Context context){
        workFolder = context.getApplicationContext().getFilesDir() + "/";
        demoVideoFolder = workFolder+"QAScreenCapVideokit/";
        vkLogPath = workFolder + "vk.log";

        GeneralUtils.copyLicenseFromAssetsToSDIfNeeded(context, workFolder);
        GeneralUtils.copyDemoVideoFromAssetsToSDIfNeeded(context, demoVideoFolder);
        int rc = GeneralUtils.isLicenseValid(context.getApplicationContext(), workFolder);
    }

    /**
     * @param context 上下文
     * @param inputFile 文件
     * @param listener 监听
     */
    public void capToMp4(
            final Context context,
            View view,
            final String inputFile,
            final String mOutputFileName,
            final VideoCompressListener listener){
//        init(context);
        Runnable runnable = () -> {
            try {
                String str = getCropPara(context, view);
                String newFilename = getVideo(context, inputFile, mOutputFileName, str);
                String rc = null;
                if (commandValidationFailedFlag) {
                    rc = "Command Vaidation Failed";
                }
                else {
                    rc = GeneralUtils.getReturnCodeFromLog(vkLogPath);
                }
                final String status = rc;
                if ("Transcoding Status: Failed".equals(status)) {
                    String strFailInfo = "Check: " + vkLogPath + " for more information.";
                    listener.onFail(strFailInfo);
                }else{
                    //转换成功，做一些特殊处理
                    //1 删除转换日志
                    listener.onSuccess(mOutputFileName,newFilename, getVideoDuration(inputFile));
                }
            } catch(Exception e) {
            }
        };
        runnable.run();
    }

    /**
     * @desc 获取裁剪参数
     * @author QZL
     * @time 2021/3/20 12:25
     */
    @NotNull
    private String getCropPara(Context context, View view) {
        //宽
        if (left < 0){
            left = 0;
        }

        String str = (960 - ViewUtil.getLocation(view)[0] - left) + ":";
        //height
        str += (view.getMeasuredHeight() - ViewUtil.getViewMargin(context, view, ViewUtil.Margin.TOP) - ViewUtil.getViewMargin(context, view, ViewUtil.Margin.BOTTOM) + height) + ":";
        str += (ViewUtil.getLocation(view)[0] + left) + ":";
        //y 坐标
        str += (ViewUtil.getLocation(view)[1] - ViewUtil.getViewMargin(context, view, ViewUtil.Margin.TOP)/2 - y) + " ";
        return str;
    }

    /**
     * @desc 获取裁剪后的视频
     * @author QZL
     * @time 2021/3/20 12:25
     */
    @Nullable
    private static String getVideo(Context context, String inputFile, String mOutputFileName, String str) {
        vk = new LoadJNI();
        String newFilename = null;
        try {

            //转换执行的命令
            String cmdStr = mStrCmdPre + inputFile + mStrCmd+ str + mOutputFileName;
            vk.run(GeneralUtils.utilConvertToComplex(cmdStr), workFolder, context.getApplicationContext());

            Log.i(Prefs.TAG, "vk.run finished.");
            GeneralUtils.copyFileToFolder(vkLogPath, demoVideoFolder);
        } catch (CommandValidationException e) {
            commandValidationFailedFlag = true;
        } catch (Throwable e) {
        }
        return newFilename;
    }

    private static int getVideoDuration(String path){
        if(mVideoDuration <= 0){
            mVideoDuration = VideoUtil.getVideoDuration(path);
        }
        return mVideoDuration;
    }
    private static int mVideoDuration;
}
