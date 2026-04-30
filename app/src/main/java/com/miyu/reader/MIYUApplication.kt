package com.miyu.reader

import android.app.Application
import com.tencent.mmkv.MMKV
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MIYUApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MMKV.initialize(this)
    }
}