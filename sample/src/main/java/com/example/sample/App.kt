package com.example.sample

import android.content.Context
import com.jiangdg.ausbc.base.BaseApplication

class App:BaseApplication() {
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
    }

    override fun onCreate() {
        super.onCreate()
        // init bugly library
    }
}