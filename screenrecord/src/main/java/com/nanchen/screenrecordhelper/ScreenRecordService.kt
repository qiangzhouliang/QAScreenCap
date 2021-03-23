package com.nanchen.screenrecordhelper

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.AssetFileDescriptor
import android.graphics.Color
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.*
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.*
import android.util.DisplayMetrics
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.blankj.utilcode.constant.PermissionConstants
import com.blankj.utilcode.util.PermissionUtils
import java.io.File
import java.nio.ByteBuffer


/**
 * @author qiangzhouliang
 * @desc
 * @email 2538096489@qq.com
 * @time 2021/3/23 20:58
 * @class QAScreenCap
 * @package com.nanchen.screenrecordhelper
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class ScreenRecordService : Service() {
    val NOTIFICATION_ID = 0x11
    private lateinit var activity:Activity
    private lateinit var listener:OnVideoRecordListener
    private lateinit var saveName:String
    private lateinit var savePath:String
    private val mediaProjectionManager by lazy { activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager }
    private var mediaRecorder: MediaRecorder? = null
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private val displayMetrics by lazy { DisplayMetrics() }
    private var saveFile: File? = null
    var isRecording = false
    var recordAudio = false

    companion object {
        private const val VIDEO_FRAME_RATE = 30
        private const val REQUEST_CODE = 1024
        private const val TAG = "ScreenRecordHelper"
    }

    //client 可以通过Binder获取Service实例
    inner class MyBinder : Binder() {
        val service: ScreenRecordService
            get() = this@ScreenRecordService
    }

    //通过binder实现调用者client与Service之间的通信
    private val binder = MyBinder()
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    override fun onCreate() {
        Log.i("Kathy", "TestTwoService - onCreate - Thread = " + Thread.currentThread().name)
        super.onCreate()
        val CHANNEL_ONE_ID = "CHANNEL_ONE_ID"
        val CHANNEL_ONE_NAME = "CHANNEL_ONE_ID"
        val builder:Notification.Builder = Notification.Builder(this)
        //进行8.0的判断,使用前台服务时，需要加入通道管理
        //进行8.0的判断,使用前台服务时，需要加入通道管理
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                CHANNEL_ONE_ID,
                CHANNEL_ONE_NAME,
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.RED
            notificationChannel.setShowBadge(true)
            notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(notificationChannel)
            builder.setChannelId(CHANNEL_ONE_ID)
        }
        builder.setSmallIcon(R.drawable.ic_launcher_background)
        startForeground(NOTIFICATION_ID, builder.build())
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.i(
            "Kathy",
            "TestTwoService - onStartCommand - startId = " + startId + ", Thread = " + Thread.currentThread().name
        )
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        Log.i("Kathy", "TestTwoService - onBind - Thread = " + Thread.currentThread().name)
        return binder
    }

    override fun onUnbind(intent: Intent): Boolean {
        Log.i("Kathy", "TestTwoService - onUnbind - from = " + intent.getStringExtra("from"))
        return false
    }


    override fun onDestroy() {
        Log.i("Kathy", "TestTwoService - onDestroy - Thread = " + Thread.currentThread().name)
        super.onDestroy()
    }

    fun setPara() {
        println("3333333333333333333333333333333333333333333")
    }

    fun init(
        activity: Activity,
        listener: OnVideoRecordListener,
        savePath: String = Environment.getExternalStorageDirectory().absolutePath + File.separator
                     + "DCIM" + File.separator + "Camera",
        saveName: String = "nanchen_${System.currentTimeMillis()}") {
        this.activity = activity
        this.listener = listener
        this.savePath = savePath
        this.saveName = saveName
        activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
    }

    fun startRecord() {
        if (mediaProjectionManager == null) {
            showToast(R.string.phone_not_support_screen_record)
            return
        }
        PermissionUtils.permission(PermissionConstants.STORAGE, PermissionConstants.MICROPHONE)
            .callback(object : PermissionUtils.SimpleCallback {
                override fun onGranted() {
                    mediaProjectionManager?.apply {
                        listener?.onBeforeRecord()
                        val intent = this.createScreenCaptureIntent()
                        if (activity.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
                            activity.startActivityForResult(intent, REQUEST_CODE)
                        } else {
                            showToast(R.string.phone_not_support_screen_record)
                        }
                    }

                }

                override fun onDenied() {
                    showToast(R.string.permission_denied)
                }
            })
            .request()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun resume() {
        mediaRecorder?.resume()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun pause() {
        mediaRecorder?.pause()
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (requestCode == REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                mediaProjection = mediaProjectionManager!!.getMediaProjection(resultCode, data)
                // 实测，部分手机上录制视频的时候会有弹窗的出现
                Handler().postDelayed({
                    if (initRecorder()) {
                        isRecording = true
                        mediaRecorder?.start()
                        listener?.onStartRecord()
                    } else {
                        showToast(R.string.phone_not_support_screen_record)

                    }
                }, 150)
            } else {
                showToast(R.string.phone_not_support_screen_record)
            }
        }
    }

    fun cancelRecord() {
        stopRecord()
        saveFile?.delete()
        saveFile = null
        listener?.onCancelRecord()
    }

    private fun showToast(resId: Int) {
        Toast.makeText(activity.applicationContext, activity.applicationContext.getString(resId), Toast.LENGTH_SHORT).show()
    }

    private fun stop() {
        if (isRecording) {
            isRecording = false
            try {
                mediaRecorder?.apply {
                    setOnErrorListener(null)
                    setOnInfoListener(null)
                    setPreviewDisplay(null)
                    stop()
                }
            } catch (e: Exception) {
            } finally {
                mediaRecorder?.reset()
                virtualDisplay?.release()
                mediaProjection?.stop()
                listener?.onEndRecord()

            }


        }
    }

    /**
     * if you has parameters, the recordAudio will be invalid
     */
    fun stopRecord(videoDuration: Long = 0, audioDuration: Long = 0, afdd: AssetFileDescriptor? = null) {
        stop()
        if (audioDuration != 0L && afdd != null) {
            syntheticAudio(videoDuration, audioDuration, afdd)
        } else {
            // saveFile
            if (saveFile != null) {
                val newFile = File(savePath, "$saveName.mp4")
                // 录制结束后修改后缀为 mp4
                saveFile!!.renameTo(newFile)
                refreshVideo(newFile)
            }
            saveFile = null
        }

    }

    private fun refreshVideo(newFile: File) {
        if (newFile.length() > 5000) {
            val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            intent.data = Uri.fromFile(newFile)
            activity.sendBroadcast(intent)
//            showToast(R.string.save_to_album_success)
            listener?.onComplate(newFile)

        } else {
            newFile.delete()
            showToast(R.string.phone_not_support_screen_record)
        }
    }

    private fun initRecorder(): Boolean {
        var result = true
        val f = File(savePath)
        if (!f.exists()) {
            var resule = f.mkdir()
        }
        saveFile = File(savePath, "$saveName.tmp")
        saveFile?.apply {
            if (exists()) {
                delete()
            }
        }
        mediaRecorder = MediaRecorder()
        val width = displayMetrics.widthPixels.coerceAtMost(1080)
        val height = displayMetrics.heightPixels.coerceAtMost(1920)
        mediaRecorder?.apply {
            if (recordAudio) {
                setAudioSource(MediaRecorder.AudioSource.MIC)
            }
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            if (recordAudio){
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            }
            setOutputFile(saveFile!!.absolutePath)
            setVideoSize(width, height)
            setVideoEncodingBitRate(8388608)
            setVideoFrameRate(VIDEO_FRAME_RATE)
            try {
                prepare()
                virtualDisplay = mediaProjection?.createVirtualDisplay("MainScreen", width, height, displayMetrics.densityDpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, surface, null, null)
            } catch (e: Exception) {
                e.printStackTrace()
                result = false
            }
        }
        return result
    }

    fun clearAll() {
        mediaRecorder?.release()
        mediaRecorder = null
        virtualDisplay?.release()
        virtualDisplay = null
        mediaProjection?.stop()
        mediaProjection = null
    }


    /**
     * https://stackoverflow.com/questions/31572067/android-how-to-mux-audio-file-and-video-file
     */
    private fun syntheticAudio(audioDuration: Long, videoDuration: Long, afdd: AssetFileDescriptor) {
        val newFile = File(savePath, "$saveName.mp4")
        if (newFile.exists()) {
            newFile.delete()
        }
        try {
            newFile.createNewFile()
            val videoExtractor = MediaExtractor()
            videoExtractor.setDataSource(saveFile!!.absolutePath)
            val audioExtractor = MediaExtractor()
            afdd.apply {
                audioExtractor.setDataSource(fileDescriptor, startOffset, length * videoDuration / audioDuration)
            }
            val muxer = MediaMuxer(newFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            videoExtractor.selectTrack(0)
            val videoFormat = videoExtractor.getTrackFormat(0)
            val videoTrack = muxer.addTrack(videoFormat)

            audioExtractor.selectTrack(0)
            val audioFormat = audioExtractor.getTrackFormat(0)
            val audioTrack = muxer.addTrack(audioFormat)

            var sawEOS = false
            var frameCount = 0
            val offset = 100
            val sampleSize = 1000 * 1024
            val videoBuf = ByteBuffer.allocate(sampleSize)
            val audioBuf = ByteBuffer.allocate(sampleSize)
            val videoBufferInfo = MediaCodec.BufferInfo()
            val audioBufferInfo = MediaCodec.BufferInfo()

            videoExtractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            audioExtractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

            muxer.start()

            // 每秒多少帧
            // 实测 OPPO R9em 垃圾手机，拿出来的没有 MediaFormat.KEY_FRAME_RATE
            val frameRate = if (videoFormat.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                videoFormat.getInteger(MediaFormat.KEY_FRAME_RATE)
            } else {
                31
            }
            // 得出平均每一帧间隔多少微妙
            val videoSampleTime = 1000 * 1000 / frameRate
            while (!sawEOS) {
                videoBufferInfo.offset = offset
                videoBufferInfo.size = videoExtractor.readSampleData(videoBuf, offset)
                if (videoBufferInfo.size < 0) {
                    sawEOS = true
                    videoBufferInfo.size = 0
                } else {
                    videoBufferInfo.presentationTimeUs += videoSampleTime
                    videoBufferInfo.flags = videoExtractor.sampleFlags
                    muxer.writeSampleData(videoTrack, videoBuf, videoBufferInfo)
                    videoExtractor.advance()
                    frameCount++
                }
            }
            var sawEOS2 = false
            var frameCount2 = 0
            while (!sawEOS2) {
                frameCount2++
                audioBufferInfo.offset = offset
                audioBufferInfo.size = audioExtractor.readSampleData(audioBuf, offset)

                if (audioBufferInfo.size < 0) {
                    sawEOS2 = true
                    audioBufferInfo.size = 0
                } else {
                    audioBufferInfo.presentationTimeUs = audioExtractor.sampleTime
                    audioBufferInfo.flags = audioExtractor.sampleFlags
                    muxer.writeSampleData(audioTrack, audioBuf, audioBufferInfo)
                    audioExtractor.advance()
                }
            }
            muxer.stop()
            muxer.release()
            videoExtractor.release()
            audioExtractor.release()

            // 删除无声视频文件
            saveFile?.delete()
        } catch (e: Exception) {
            Log.e("ScreenRecordHelper.TAG", "Mixer Error:${e.message}")
            // 视频添加音频合成失败，直接保存视频
            saveFile?.renameTo(newFile)

        } finally {
            afdd.close()
            Handler().post {
                refreshVideo(newFile)
                saveFile = null
            }
        }
    }

    interface OnVideoRecordListener {
        /**
         * 录制开始时隐藏不必要的UI
         */
        fun onBeforeRecord()

        /**
         * 开始录制
         */
        fun onStartRecord()

        /**
         * 取消录制
         */
        fun onCancelRecord()

        /**
         * 结束录制
         */
        fun onEndRecord()

        /**
         * 文件保存成功
         */
        fun onComplate(file: File)
    }
}