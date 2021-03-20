package com.qzl.qascreencapl

import android.content.Context
import android.os.Environment
import cn.sharerec.recorder.Recorder
import cn.sharerec.recorder.impl.SystemRecorder
import java.io.File

/**
 * @desc 录制视频工具类
 * @author qiangzhouliang
 * @email 2538096489@qq.com
 * @time 2021/3/19 21:23
 * @class QAScreenCap
 * @package com.qzl.qascreencap
 */
object QARecord {
    val APP_KEY = "1bd81b4808ec4"
    val APP_SECRET = "91a1c41c80cbc11aba920eaa83992ffb"
    @JvmField
    var VIDEO_PATH = "/ShareREC/video"
    @JvmField
    val SOUND_PATH = "/ShareREC/sound"
    /**
     * 初始化录屏
     */
    @JvmStatic
    fun initRecord(context: Context, outPath:String = "/sdcard$VIDEO_PATH"): SystemRecorder {
        val recorder = SystemRecorder(context, APP_KEY, APP_SECRET)
        // 设置视频的最大尺寸
        recorder.setMaxFrameSize(Recorder.LevelMaxFrameSize.LEVEL_1920_1080)
        // 设置视频的质量（高、中、低）
        recorder.setVideoQuality(Recorder.LevelVideoQuality.LEVEL_SUPER_HIGH)
        // 设置视频的最短时长
        recorder.minDuration = (1 * 1000).toLong()
        // 设置视频的输出路径
        recorder.setCacheFolder(outPath)
        // 设置是否强制使用软件编码器对视频进行编码（兼容性更高）
        // 但都设置为true后会导致最后1-2秒的丢失，可能是由于开启软件编码器压缩视频会导致丢失
        recorder.setForceSoftwareEncoding(false, false)
        return recorder
        // 设置监听回调 有问题：会导致文件打不开
//        recorder.setMediaOutput(output);
    }

    /**
     * 初始化录音文件
     */
    @JvmStatic
    fun initSoundData() {
        var soundFileName  = Environment.getExternalStorageDirectory().path + QARecord.SOUND_PATH
        val file = File(soundFileName)
        if (!file.exists()) {
            file.mkdirs()
        }
        soundFileName += "/sound1.mp4"
    }
}