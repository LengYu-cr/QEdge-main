package me.lengyu.qedge

import android.app.Activity
import android.content.Intent
import android.os.Bundle
/**
 * @Author 冷雨
 * @Description 启动器活动
 */
class LauncherActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }
}