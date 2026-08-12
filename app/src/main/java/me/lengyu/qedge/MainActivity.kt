package me.lengyu.qedge

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.lengyu.qedge.ui.components.dialogs.UpdateDialog
import me.lengyu.qedge.ui.core.compatibility.QEdgeCenterDialog
import me.lengyu.qedge.ui.core.theme.QEdgeTheme
import me.lengyu.qedge.ui.pages.home.HomeUpdateStatus
import me.lengyu.qedge.ui.pages.home.MainScreen
import me.lengyu.qedge.utils.LogUtils
import org.json.JSONObject
import java.lang.Runnable

class MainActivity : ComponentActivity() {

    private var updateStatus by mutableStateOf(HomeUpdateStatus.IDLE)
    private var latestVersionName = ""
    private var latestUpdateLog = ""
    private var latestDownloadUrl = ""
    private var updateLogText by mutableStateOf("")
    private var updateJob: Job? = null
    private var changelogJob: Job? = null
    private var changelogDialog: QEdgeCenterDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        setContent {
            QEdgeTheme {
                MainScreen(
                    versionName = BuildConfig.VERSION_NAME,
                    updateStatus = updateStatus,
                    onQQGroupClick = { openUrl(QQ_GROUP_URL) },
                    onTGClick = { openUrl(TELEGRAM_URL) },
                    onUserBackendClick = { openUrl(USER_BACKEND_URL) },
                    onUpdateLogClick = ::showUpdateLogDialog,
                    onCheckUpdateClick = { checkUpdate(showToast = true) },
                    onLaunchQQClick = ::launchQQ
                )
            }
        }

        checkUpdate(showToast = false)
    }

    private fun checkUpdate(showToast: Boolean) {
        if (updateJob?.isActive == true) return
        updateStatus = HomeUpdateStatus.CHECKING

        updateJob = lifecycleScope.launch {
            try {
                val json = withContext(Dispatchers.IO) {
                    requestJson(
                        url = "$UPDATE_CHECK_URL?version_code=${BuildConfig.VERSION_CODE}",
                        timeoutMillis = 10_000
                    )
                }
                if (json.getInt("code") != 200) {
                    updateStatus = HomeUpdateStatus.ERROR
                    if (showToast) {
                        Toast.makeText(
                            this@MainActivity,
                            json.optString("message", "检查更新失败"),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@launch
                }

                val data = json.getJSONObject("data")
                val hasUpdate = data.getBoolean("has_update")
                latestVersionName = data.getString("latest_version")
                latestUpdateLog = data.getString("update_log")
                latestDownloadUrl = data.getString("download_url")
                updateStatus = if (hasUpdate) {
                    HomeUpdateStatus.AVAILABLE
                } else {
                    HomeUpdateStatus.LATEST
                }

                if (hasUpdate) {
                    showUpdateDialog()
                } else if (showToast) {
                    Toast.makeText(this@MainActivity, "已是最新版本", Toast.LENGTH_SHORT).show()
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (e: Exception) {
                LogUtils.e(e)
                updateStatus = HomeUpdateStatus.ERROR
                if (showToast) {
                    Toast.makeText(
                        this@MainActivity,
                        "检查更新失败: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun showUpdateDialog() {
        val dialog = UpdateDialog(
            context = this,
            version = latestVersionName,
            updateLog = latestUpdateLog,
            downloadUrl = latestDownloadUrl,
            onIgnore = Runnable {}
        )
        dialog.show()
    }

    private fun showUpdateLogDialog() {
        if (changelogDialog?.isShowing == true) return

        updateLogText = "加载中..."
        changelogDialog = createUpdateLogDialog().also { it.show() }

        changelogJob?.cancel()
        changelogJob = lifecycleScope.launch {
            try {
                val json = withContext(Dispatchers.IO) {
                    requestJson(CHANGELOG_URL, timeoutMillis = 5_000)
                }
                if (json.getInt("code") == 200) {
                    val data = json.getJSONObject("data")
                    val changelog = data.getJSONArray("changelog")
                    val sb = StringBuilder()
                    for (i in 0 until changelog.length()) {
                        val entry = changelog.getJSONObject(i)
                        sb.append("v${entry.getString("version")} (${entry.getString("date")})\n")
                        val items = entry.getJSONArray("items")
                        for (j in 0 until items.length()) {
                            sb.append("• ${items.getString(j)}\n")
                        }
                        if (i < changelog.length() - 1) {
                            sb.append("\n")
                        }
                    }
                    updateLogText = sb.toString()
                    if (latestVersionName.isEmpty()) {
                        latestVersionName = data.getString("latest_version")
                    }
                } else {
                    updateLogText = "获取失败"
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (e: Exception) {
                updateLogText = "获取失败: ${e.message}"
            }
        }
    }

    private fun createUpdateLogDialog(): QEdgeCenterDialog {
        return QEdgeCenterDialog(this) { onDismiss ->
            val colors = QEdgeTheme.colors
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(colors.cardBackground)
                    .padding(20.dp)
            ) {
                Text(
                    "更新日志",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        updateLogText,
                        fontSize = 14.sp,
                        color = colors.textSecondary,
                        lineHeight = 20.sp
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("关闭", color = colors.textPrimary)
                    }
                }
            }
        }
    }

    private fun openUrl(url: String) {
        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }.onFailure { error ->
            Toast.makeText(this, "打开链接失败: ${error.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchQQ() {
        val launchIntent = packageManager.getLaunchIntentForPackage(QQ_PACKAGE_NAME)
        if (launchIntent == null) {
            Toast.makeText(this, "未检测到 QQ", Toast.LENGTH_SHORT).show()
            return
        }

        runCatching { startActivity(launchIntent) }
            .onFailure { error ->
                Toast.makeText(this, "启动 QQ 失败: ${error.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun requestJson(url: String, timeoutMillis: Int): JSONObject {
        val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
        connection.connectTimeout = timeoutMillis
        connection.readTimeout = timeoutMillis
        connection.requestMethod = "GET"

        return try {
            val response = connection.inputStream
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
            JSONObject(response)
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val QQ_GROUP_URL = "https://qm.qq.com/q/ElvGZvBrZC"
        const val TELEGRAM_URL = "https://t.me/+St91SS8CLNUyMDY1"
        const val USER_BACKEND_URL = "https://v.yuafeng.cn/QEdge/user/index.php"
        const val QQ_PACKAGE_NAME = "com.tencent.mobileqq"
        const val UPDATE_CHECK_URL = "https://v.yuafeng.cn/QEdge/update/check.php"
        const val CHANGELOG_URL = "https://v.yuafeng.cn/QEdge/update/changelog.php"
    }
}
