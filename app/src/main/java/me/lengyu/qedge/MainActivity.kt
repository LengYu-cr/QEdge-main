package me.lengyu.qedge

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import me.lengyu.qedge.ui.pages.home.MainScreen
import me.lengyu.qedge.ui.core.theme.QEdgeTheme
import me.lengyu.qedge.ui.components.dialogs.UpdateDialog
import me.lengyu.qedge.ui.core.compatibility.QEdgeCenterDialog

import org.json.JSONObject

class MainActivity : ComponentActivity() {

    private var isLatestVersion by mutableStateOf(true)
    private var isCheckingUpdate by mutableStateOf(false)
    private var latestVersionName = ""
    private var latestUpdateLog = ""
    private var latestDownloadUrl = ""
    private var updateLogText = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        setContent {
            QEdgeTheme {
                MainScreen(
                    versionName = BuildConfig.VERSION_NAME,
                    versionCode = BuildConfig.VERSION_CODE,
                    isLatestVersion = isLatestVersion,
                    isCheckingUpdate = isCheckingUpdate,
                    onQQGroupClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://qm.qq.com/q/ElvGZvBrZC"))
                        startActivity(intent)
                    },
                    onTGClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/+St91SS8CLNUyMDY1"))
                        startActivity(intent)
                    },
                    onUserBackendClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://v.yuafeng.cn/QEdge/user/index.php"))
                        startActivity(intent)
                    },
                    onQFunClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/oneQAQone/QFun"))
                        startActivity(intent)
                    },
                    onUpdateLogClick = {
                        showUpdateLogDialog()
                    },
                    onCheckUpdateClick = {
                        checkUpdate(showToast = true)
                    }
                )
            }
        }

        checkUpdate(showToast = false)
    }

    private fun checkUpdate(showToast: Boolean) {
        if (isCheckingUpdate) return
        isCheckingUpdate = true

        Thread {
            try {
                val url = java.net.URL(
                    "https://v.yuafeng.cn/QEdge/update/check.php?version_code=${BuildConfig.VERSION_CODE}"
                )
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.requestMethod = "GET"

                val reader = java.io.BufferedReader(
                    java.io.InputStreamReader(connection.inputStream, "UTF-8")
                )
                val response = java.lang.StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()

                val json = JSONObject(response.toString())
                if (json.getInt("code") == 200) {
                    val data = json.getJSONObject("data")
                    val hasUpdate = data.getBoolean("has_update")
                    latestVersionName = data.getString("latest_version")
                    latestUpdateLog = data.getString("update_log")
                    latestDownloadUrl = data.getString("download_url")

                    isLatestVersion = !hasUpdate

                    if (hasUpdate) {
                        runOnUiThread {
                            showUpdateDialog()
                        }
                    } else if (showToast) {
                        runOnUiThread {
                            Toast.makeText(this, "已是最新版本", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (showToast) {
                    runOnUiThread {
                        Toast.makeText(this, "检查更新失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } finally {
                isCheckingUpdate = false
            }
        }.start()
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
        updateLogText = "加载中..."
        Thread {
            try {
                val url = java.net.URL("https://v.yuafeng.cn/QEdge/update/changelog.php")
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.requestMethod = "GET"

                val reader = java.io.BufferedReader(
                    java.io.InputStreamReader(connection.inputStream, "UTF-8")
                )
                val response = java.lang.StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()

                val json = JSONObject(response.toString())
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
            } catch (e: Exception) {
                updateLogText = "获取失败: ${e.message}"
            }

            runOnUiThread {
                QEdgeCenterDialog(this) { onDismiss ->
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
                }.show()
            }
        }.start()
    }
}