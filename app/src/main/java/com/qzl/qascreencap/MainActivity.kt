package com.qzl.qascreencap

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.nanchen.screenrecordhelper.ScreenRecordHelper
import com.nanchen.screenrecordhelper.VideoCompressListener
import com.nanchen.screenrecordhelper.VideoCutToMp4
import java.io.File

class MainActivity : AppCompatActivity(), View.OnClickListener {
    private var start: Button? = null
    private var end:Button? = null
    val et:EditText by lazy {findViewById(R.id.et)}
    private var isStart = false
    private var screenRecordHelper: ScreenRecordHelper? = null

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
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.start -> if (!isStart) { //不可连续点多次开始录屏
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    if (screenRecordHelper == null) {
                        screenRecordHelper = ScreenRecordHelper(
                            this,
                            object : ScreenRecordHelper.OnVideoRecordListener {
                                override fun onBeforeRecord() {
                                }

                                override fun onStartRecord() {
                                }

                                override fun onCancelRecord() {
                                }

                                override fun onEndRecord() {

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
                    }
                    screenRecordHelper?.apply {
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
                    screenRecordHelper?.apply {
                        if (isRecording) {
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
            screenRecordHelper?.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onDestroy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            screenRecordHelper?.clearAll()
        }
        super.onDestroy()
    }

}