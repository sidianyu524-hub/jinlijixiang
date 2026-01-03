package com.example.jinlijixiang.utils

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.FileProvider
import com.example.jinlijixiang.data.UpdateInfo
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * 应用更新管理器
 * 注意：jinlijixiang 使用不同的OSS路径和文件名
 */
object AppUpdateManager {
    private const val TAG = "AppUpdateManager"

    // 更新服务器地址（使用同一个OSS，但不同的文件夹）
    private const val UPDATE_BASE_URL = "https://xiaoyongbaojia.oss-cn-beijing.aliyuncs.com/jinlijixiang"
    private const val VERSION_FILE = "version_jlx.json"

    // 下载ID
    private var downloadId: Long = -1

    /**
     * 获取当前应用版本号
     */
    fun getCurrentVersionCode(context: Context): Int {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取版本号失败", e)
            1
        }
    }

    /**
     * 获取当前应用版本名称
     */
    fun getCurrentVersionName(context: Context): String {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            packageInfo.versionName ?: "1.0"
        } catch (e: Exception) {
            Log.e(TAG, "获取版本名失败", e)
            "1.0"
        }
    }

    /**
     * 检查更新
     * @return UpdateInfo 如果有更新返回更新信息，否则返回 null
     */
    suspend fun checkUpdate(context: Context): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$UPDATE_BASE_URL/$VERSION_FILE")
            Log.d(TAG, "检查更新: $url")

            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.requestMethod = "GET"

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d(TAG, "版本信息: $response")

                val updateInfo = Gson().fromJson(response, UpdateInfo::class.java)
                val currentVersionCode = getCurrentVersionCode(context)

                Log.d(TAG, "当前版本: $currentVersionCode, 服务器版本: ${updateInfo.versionCode}")

                if (updateInfo.versionCode > currentVersionCode) {
                    return@withContext updateInfo
                }
            } else {
                Log.w(TAG, "检查更新失败: HTTP ${connection.responseCode}")
            }

            null
        } catch (e: Exception) {
            Log.e(TAG, "检查更新异常", e)
            null
        }
    }

    /**
     * 下载并安装 APK
     * 注意：由于阿里云OSS禁止直接下载APK，需要把APK改名为.zip上传，下载后再重命名
     */
    fun downloadAndInstall(context: Context, updateInfo: UpdateInfo, onProgress: (Int) -> Unit, onComplete: () -> Unit, onError: (String) -> Unit) {
        val mainHandler = Handler(Looper.getMainLooper())

        try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

            // 删除旧文件
            val zipFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "update_jlx.zip")
            val apkFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "update_jlx.apk")
            if (zipFile.exists()) zipFile.delete()
            if (apkFile.exists()) apkFile.delete()

            Log.d(TAG, "开始下载: ${updateInfo.apkUrl}")

            // 下载为zip文件（OSS不限制zip下载）
            val request = DownloadManager.Request(Uri.parse(updateInfo.apkUrl))
                .setTitle("金利机箱更新")
                .setDescription("正在下载 v${updateInfo.versionName}")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "update_jlx.zip")
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            downloadId = downloadManager.enqueue(request)
            Log.d(TAG, "下载任务ID: $downloadId")

            // 注册下载完成广播接收器
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context?, intent: Intent?) {
                    val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (id == downloadId) {
                        Log.d(TAG, "下载完成广播: $downloadId")
                        try {
                            context.unregisterReceiver(this)
                        } catch (e: Exception) {
                            Log.e(TAG, "注销广播失败", e)
                        }

                        // 重命名zip为apk
                        if (zipFile.exists()) {
                            val renamed = zipFile.renameTo(apkFile)
                            Log.d(TAG, "重命名zip->apk: $renamed")
                        }

                        mainHandler.post {
                            onComplete()
                        }
                        // 安装 APK
                        installApk(context, apkFile)
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    receiver,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                    Context.RECEIVER_EXPORTED
                )
            } else {
                context.registerReceiver(
                    receiver,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
                )
            }

            // 启动进度监听线程
            Thread {
                var downloading = true
                var lastProgress = -1
                while (downloading && downloadId != -1L) {
                    try {
                        val query = DownloadManager.Query().setFilterById(downloadId)
                        val cursor = downloadManager.query(query)
                        if (cursor != null && cursor.moveToFirst()) {
                            val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                            val bytesIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                            val totalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                            val reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)

                            if (statusIndex >= 0) {
                                val status = cursor.getInt(statusIndex)
                                val bytesDownloaded = if (bytesIndex >= 0) cursor.getLong(bytesIndex) else 0L
                                val bytesTotal = if (totalIndex >= 0) cursor.getLong(totalIndex) else 0L

                                Log.d(TAG, "下载状态: status=$status, bytes=$bytesDownloaded/$bytesTotal")

                                when (status) {
                                    DownloadManager.STATUS_PENDING -> {
                                        mainHandler.post { onProgress(0) }
                                    }
                                    DownloadManager.STATUS_RUNNING -> {
                                        if (bytesTotal > 0) {
                                            val progress = ((bytesDownloaded * 100) / bytesTotal).toInt()
                                            if (progress != lastProgress) {
                                                lastProgress = progress
                                                mainHandler.post { onProgress(progress) }
                                            }
                                        }
                                    }
                                    DownloadManager.STATUS_SUCCESSFUL -> {
                                        downloading = false
                                        mainHandler.post { onProgress(100) }
                                        Log.d(TAG, "下载成功")
                                    }
                                    DownloadManager.STATUS_FAILED -> {
                                        downloading = false
                                        val reason = if (reasonIndex >= 0) cursor.getInt(reasonIndex) else -1
                                        Log.e(TAG, "下载失败, reason=$reason")
                                        mainHandler.post { onError("下载失败 (错误码: $reason)") }
                                    }
                                    DownloadManager.STATUS_PAUSED -> {
                                        val reason = if (reasonIndex >= 0) cursor.getInt(reasonIndex) else -1
                                        Log.w(TAG, "下载暂停, reason=$reason")
                                    }
                                }
                            }
                            cursor.close()
                        } else {
                            downloading = false
                            Log.w(TAG, "查询下载状态失败")
                        }
                        Thread.sleep(300)
                    } catch (e: Exception) {
                        Log.e(TAG, "监控下载进度异常", e)
                        downloading = false
                        mainHandler.post { onError("下载异常: ${e.message}") }
                    }
                }
            }.start()

        } catch (e: Exception) {
            Log.e(TAG, "启动下载失败", e)
            mainHandler.post { onError("下载失败: ${e.message}") }
        }
    }

    /**
     * 安装 APK
     */
    private fun installApk(context: Context, apkFile: File) {
        try {
            if (!apkFile.exists()) {
                Log.e(TAG, "APK文件不存在")
                return
            }

            val intent = Intent(Intent.ACTION_VIEW)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

            val apkUri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
            } else {
                Uri.fromFile(apkFile)
            }

            intent.setDataAndType(apkUri, "application/vnd.android.package-archive")
            context.startActivity(intent)

            Log.d(TAG, "启动安装: $apkUri")
        } catch (e: Exception) {
            Log.e(TAG, "安装APK失败", e)
        }
    }

    /**
     * 取消下载
     */
    fun cancelDownload(context: Context) {
        if (downloadId != -1L) {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.remove(downloadId)
            downloadId = -1
            Log.d(TAG, "取消下载")
        }
    }
}
