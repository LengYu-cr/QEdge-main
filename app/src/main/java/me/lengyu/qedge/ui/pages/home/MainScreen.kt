package me.lengyu.qedge.ui.pages.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import me.lengyu.qedge.R

enum class HomeUpdateStatus {
    IDLE,
    CHECKING,
    LATEST,
    AVAILABLE,
    ERROR
}

@Composable
fun MainScreen(
    versionName: String,
    updateStatus: HomeUpdateStatus,
    onQQGroupClick: () -> Unit,
    onTGClick: () -> Unit,
    onUserBackendClick: () -> Unit,
    onUpdateLogClick: () -> Unit,
    onCheckUpdateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSupportDialog by rememberSaveable { mutableStateOf(false) }
    val currentTime = rememberHomeTime()

    val railActions = listOf(
        HomeRailAction(
            iconRes = R.drawable.ic_logo_qq,
            label = "QQ 交流群",
            emphasized = true,
            onClick = onQQGroupClick
        ),
        HomeRailAction(
            iconRes = R.drawable.ic_logo_telegram,
            label = "Telegram",
            onClick = onTGClick
        ),
        HomeRailAction(
            iconRes = R.drawable.ic_user,
            label = "用户后台",
            onClick = onUserBackendClick
        ),
        HomeRailAction(
            iconRes = R.drawable.book,
            label = "更新日志",
            showDividerBefore = true,
            onClick = onUpdateLogClick
        ),
        HomeRailAction(
            iconRes = R.drawable.category,
            label = "适配列表",
            onClick = { showSupportDialog = true }
        )
    )

    Box(modifier = modifier.fillMaxSize()) {
        HomeScaffold(
            currentTime = currentTime,
            actions = railActions
        ) { toggleDrawer ->
            HomeContentPanel(
                versionName = versionName,
                updateStatus = updateStatus,
                currentTime = currentTime,
                onMenuClick = toggleDrawer,
                onCheckUpdateClick = onCheckUpdateClick
            )
        }

        if (showSupportDialog) {
            HomeSupportDialog(onDismiss = { showSupportDialog = false })
        }
    }
}
