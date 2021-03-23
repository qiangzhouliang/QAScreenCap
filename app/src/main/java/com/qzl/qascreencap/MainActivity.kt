package com.qzl.qascreencap

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.nanchen.screenrecordhelper.ScreenRecordService
import com.nanchen.screenrecordhelper.VideoCompressListener
import com.nanchen.screenrecordhelper.VideoCutToMp4
import java.io.File

class MainActivity : AppCompatActivity(), View.OnClickListener {
    private var start: Button? = null
    private var end:Button? = null
    val et:EditText by lazy {findViewById(R.id.et)}
    private var isStart = false

    private var service: ScreenRecordService? = null
    private var isBind = false

    private val conn: ServiceConnection = object : ServiceConnection {
        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        override fun onServiceConnected(name: ComponentName?, binder: IBinder) {
            isBind = true
            val myBinder: ScreenRecordService.MyBinder = binder as ScreenRecordService.MyBinder
            service = myBinder.service
            //设置初始化参数
            service?.init(this@MainActivity,
                    object : ScreenRecordService.OnVideoRecordListener {
                        override fun onBeforeRecord() {
                            println("onBeforeRecord")
                        }

                        override fun onStartRecord() {
                            println("onStartRecord")
                        }

                        override fun onCancelRecord() {
                            println("onCancelRecord")
                        }

                        override fun onEndRecord() {
                            println("onEndRecord")
                        }

                        override fun onComplate(file: File) {
                            VideoCutToMp4(this@MainActivity).capToMp4(
                                    this@MainActivity,
                                    et,
                                    file.path,
                                    Environment.getExternalStorageDirectory().path + "/nanchen/111.mp4",
                                    object : VideoCompressListener {
                                        override fun onSuccess(
                                                outputFile: String?,
                                                filename: String?,
                                                duration: Long
                                        ) {
                                            println("chenggong:$outputFile")
                                        }

                                        override fun onFail(reason: String?) {

                                        }

                                        override fun onProgress(progress: Int) {

                                        }

                                    })
                        }
                    },
                    Environment.getExternalStorageDirectory().path + "/nanchen"
            )
            Log.i("Kathy", "ActivityA - onServiceConnected")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBind = false
            Log.i("Kathy", "ActivityA - onServiceDisconnected")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initView()
        initListener()
    }

    private fun initView() {
        start = findViewById<View>(R.id.start) as Button
        end = findViewById<View>(R.id.end) as Button
    }

    private fun initListener() {
        start!!.setOnClickListener(this)
        end?.setOnClickListener(this)
        val intent = Intent(this, ScreenRecordService::class.java)
        bindService(intent, conn, BIND_AUTO_CREATE)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onClick(view: View) {
        when (view.id) {
            R.id.start -> if (!isStart) { //不可连续点多次开始录屏
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    service?.apply {
                        if (!isRecording) {
                            // 如果你想录制音频（一定会有环境音量），你可以打开下面这个限制,并且使用不带参数的 stopRecord()
                            recordAudio = true
                            startRecord()
                        }
                    }
                } else {
                    Toast.makeText(
                            this@MainActivity.applicationContext,
                            "sorry,your phone does not support recording screen",
                            Toast.LENGTH_LONG
                    ).show()
                }
                isStart = true
                Toast.makeText(this, "开始录屏录音", Toast.LENGTH_SHORT).show()
            }
            R.id.end -> if (isStart) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    service?.apply {
                        if (isRecording){
                            stopRecord()
                        }
                    }
                }
                isStart = false
                Toast.makeText(this, "停止录屏录音", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && data != null) {
            service?.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onDestroy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            service?.clearAll()
        }
        super.onDestroy()
    }

}