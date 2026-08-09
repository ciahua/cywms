package com.chenyang.cywms.ui.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.SystemUpdateAlt
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import com.chenyang.cywms.R
import com.chenyang.cywms.ui.theme.Amber500
import com.chenyang.cywms.ui.theme.DangerRed
import com.chenyang.cywms.ui.theme.GlassFill
import com.chenyang.cywms.ui.theme.GlassStroke
import com.chenyang.cywms.ui.theme.Navy900
import com.chenyang.cywms.ui.theme.Slate200
import com.chenyang.cywms.ui.theme.Slate400
import com.chenyang.cywms.ui.theme.SuccessGreen

@Composable
fun LoginRoute(
    viewModel: LoginViewModel,
    onLoggedIn: () -> Unit
) {
    val state by viewModel.ui.collectAsStateWithLifecycle()
    LaunchedEffect(state.loggedIn) {
        if (state.loggedIn) onLoggedIn()
    }
    // 用户确认下载完成后自动调起安装
    LaunchedEffect(state.apkReady) {
        if (state.apkReady != null) {
            viewModel.installDownloaded()
        }
    }
    LoginScreen(
        state = state,
        onHost = viewModel::onHost,
        onPort = viewModel::onPort,
        onUseHttps = viewModel::onUseHttps,
        onProxy = viewModel::onProxy,
        onUsername = viewModel::onUsername,
        onPassword = viewModel::onPassword,
        onRemember = viewModel::onRemember,
        onToggleAdvanced = viewModel::toggleAdvanced,
        onTest = viewModel::testConnection,
        onUpdateIconClick = viewModel::onUpdateIconClick,
        onConfirmDownload = viewModel::confirmDownload,
        onDismissUpdateDialog = viewModel::dismissUpdateDialog,
        onDismissToast = viewModel::dismissToast,
        onLogin = viewModel::login
    )
}

@Composable
fun LoginScreen(
    state: LoginUiState,
    onHost: (String) -> Unit,
    onPort: (String) -> Unit,
    onUseHttps: (Boolean) -> Unit,
    onProxy: (String) -> Unit,
    onUsername: (String) -> Unit,
    onPassword: (String) -> Unit,
    onRemember: (Boolean) -> Unit,
    onToggleAdvanced: () -> Unit,
    onTest: () -> Unit,
    onUpdateIconClick: () -> Unit,
    onConfirmDownload: () -> Unit,
    onDismissUpdateDialog: () -> Unit,
    onDismissToast: () -> Unit,
    onLogin: () -> Unit
) {
    var showPassword by remember { mutableStateOf(false) }
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    // 短暂提示：约 2 秒后自动消失
    LaunchedEffect(state.showToast, state.toastMessage) {
        if (state.showToast && state.toastMessage.isNotBlank()) {
            delay(2000)
            onDismissToast()
        }
    }

    if (state.showUpdateDialog) {
        AlertDialog(
            onDismissRequest = onDismissUpdateDialog,
            title = { Text("发现新版本") },
            text = {
                Column {
                    Text("当前：${state.localVersion}")
                    Text("最新：${state.remoteVersion.orEmpty()}")
                    state.remoteRemark?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = Color(0xFF666666), fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("是否下载并安装？取消则继续使用当前版本。")
                }
            },
            confirmButton = {
                TextButton(onClick = onConfirmDownload) {
                    Text("下载更新", color = Amber500, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissUpdateDialog) {
                    Text("暂不更新", color = Slate400)
                }
            },
            containerColor = Color(0xFF1A2438),
            titleContentColor = Slate200,
            textContentColor = Slate200
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.bg_warehouse_charger),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xCC0B1220),
                            Color(0xB30B1220),
                            Color(0xE60B1220)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(
                visible = entered,
                enter = fadeIn() + slideInVertically { it / 6 },
                exit = fadeOut()
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Image(
                        painter = painterResource(R.drawable.ic_logo_111),
                        contentDescription = "辰阳电子",
                        modifier = Modifier
                            .padding(bottom = 14.dp)
                            .size(72.dp)
                    )
                    Text(
                        text = "江苏辰阳电子 WMS",
                        color = Slate200,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "PDA 仓储作业终端",
                        color = Slate400,
                        fontSize = 15.sp
                    )
                    Text(
                        text = state.localVersion,
                        color = Slate400.copy(alpha = 0.75f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                    Spacer(Modifier.height(28.dp))

                    GlassPanel {
                        Column(modifier = Modifier.padding(18.dp)) {
                            GlassField(
                                value = state.username,
                                onValueChange = onUsername,
                                label = "账号",
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Next
                                )
                            )
                            Spacer(Modifier.height(12.dp))
                            GlassField(
                                value = state.password,
                                onValueChange = onPassword,
                                label = "密码",
                                visualTransformation = if (showPassword) {
                                    VisualTransformation.None
                                } else {
                                    PasswordVisualTransformation()
                                },
                                trailing = {
                                    IconButton(onClick = { showPassword = !showPassword }) {
                                        Icon(
                                            imageVector = if (showPassword) {
                                                Icons.Outlined.VisibilityOff
                                            } else {
                                                Icons.Outlined.Visibility
                                            },
                                            contentDescription = null,
                                            tint = Slate400
                                        )
                                    }
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = { onLogin() })
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Checkbox(
                                    checked = state.rememberAccount,
                                    onCheckedChange = onRemember,
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Amber500,
                                        uncheckedColor = Slate400
                                    )
                                )
                                Text("记住账号", color = Slate200)
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(onClick = onToggleAdvanced)
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "服务器与代理设置",
                                    color = Amber500,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = if (state.advancedOpen) {
                                        Icons.Outlined.ExpandLess
                                    } else {
                                        Icons.Outlined.ExpandMore
                                    },
                                    contentDescription = null,
                                    tint = Amber500
                                )
                            }

                            AnimatedVisibility(visible = state.advancedOpen) {
                                Column {
                                    Spacer(Modifier.height(4.dp))
                                    GlassField(
                                        value = state.host,
                                        onValueChange = onHost,
                                        label = "服务器 IP / 域名"
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    GlassField(
                                        value = state.port,
                                        onValueChange = onPort,
                                        label = "端口",
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Number
                                        )
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = state.useHttps,
                                            onCheckedChange = onUseHttps,
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = Amber500,
                                                uncheckedColor = Slate400
                                            )
                                        )
                                        Text("使用 HTTPS（直连时）", color = Slate200)
                                    }
                                    GlassField(
                                        value = state.proxyUrl,
                                        onValueChange = onProxy,
                                        label = "代理地址（ngrok，优先）",
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Uri
                                        )
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    OutlinedButton(
                                        onClick = onTest,
                                        enabled = !state.testing && !state.loading,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = Slate200
                                        )
                                    ) {
                                        if (state.testing) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                strokeWidth = 2.dp,
                                                color = Amber500
                                            )
                                            Spacer(Modifier.size(8.dp))
                                        }
                                        Text(if (state.testing) "测试中…" else "测试连接")
                                    }
                                    state.testMessage?.let { msg ->
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            text = msg,
                                            color = if (msg.startsWith("失败")) DangerRed else SuccessGreen,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }

                            state.error?.let { err ->
                                Spacer(Modifier.height(10.dp))
                                Text(text = err, color = DangerRed, fontSize = 13.sp)
                            }

                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = onLogin,
                                enabled = !state.loading,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Amber500,
                                    contentColor = Navy900
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (state.loading) {
                                    CircularProgressIndicator(
                                        color = Navy900,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(22.dp)
                                    )
                                } else {
                                    Text("登 录", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 底部居中的更新入口，不占用主布局
        QuietUpdateIcon(
            state = state,
            onClick = onUpdateIconClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 12.dp)
        )

        // Toast 风格提示：暗红半透明，区别于登录页深蓝背景
        AnimatedVisibility(
            visible = state.showToast && state.toastMessage.isNotBlank(),
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 36.dp)
                    .widthIn(max = 300.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xCC6B1D24))
                    .border(1.dp, Color(0x66FF8A80), RoundedCornerShape(10.dp))
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = state.toastMessage,
                    color = Color(0xFFFFEBEE),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun QuietUpdateIcon(
    state: LoginUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasUpdate = state.updateAvailable || state.apkReady != null
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(enabled = !state.downloading, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        when {
            state.downloading -> {
                CircularProgressIndicator(
                    progress = {
                        val p = state.downloadPercent
                        if (p in 0..100) p / 100f else 0f
                    },
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp,
                    color = Amber500,
                    trackColor = Slate400.copy(alpha = 0.25f)
                )
            }
            state.checkingUpdate -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = Slate400.copy(alpha = 0.7f)
                )
            }
            else -> {
                Icon(
                    imageVector = Icons.Outlined.SystemUpdateAlt,
                    contentDescription = "检查更新",
                    tint = Slate400.copy(alpha = if (hasUpdate) 0.95f else 0.45f),
                    modifier = Modifier.size(22.dp)
                )
                if (hasUpdate) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 6.dp, end = 6.dp)
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Amber500)
                    )
                }
            }
        }
    }
}

@Composable
private fun GlassPanel(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(GlassFill)
            .border(1.dp, GlassStroke, RoundedCornerShape(20.dp))
    ) {
        content()
    }
}

@Composable
private fun GlassField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailing: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        visualTransformation = visualTransformation,
        trailingIcon = trailing,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Slate200,
            unfocusedTextColor = Slate200,
            focusedBorderColor = Amber500,
            unfocusedBorderColor = GlassStroke,
            focusedLabelColor = Amber500,
            unfocusedLabelColor = Slate400,
            cursorColor = Amber500,
            focusedContainerColor = Color(0x33000000),
            unfocusedContainerColor = Color(0x22000000)
        ),
        shape = RoundedCornerShape(12.dp)
    )
}
