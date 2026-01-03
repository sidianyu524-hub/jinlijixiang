package com.example.jinlijixiang.data

/**
 * 应用更新信息
 */
data class UpdateInfo(
    val versionCode: Int,        // 版本号（用于比较）
    val versionName: String,     // 版本名称（显示用）
    val apkUrl: String,          // APK下载地址
    val updateLog: String,       // 更新日志
    val forceUpdate: Boolean = false  // 是否强制更新
)
