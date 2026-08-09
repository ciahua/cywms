package com.chenyang.cywms.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chenyang.cywms.R
import com.chenyang.cywms.ui.theme.Navy800
import com.chenyang.cywms.ui.theme.Navy900
import com.chenyang.cywms.ui.theme.Slate200
import com.chenyang.cywms.ui.theme.Slate400
import kotlinx.coroutines.launch

/** 海康 5204：6.2 寸，1520×720（竖屏按 720 宽适配），首页一行三格 */
private const val HOME_COLUMNS = 3

@Composable
fun HomeScreen(
    displayName: String,
    onLogout: () -> Unit,
    onModuleClick: (HomeModule) -> Unit = {}
) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Navy900, Navy800, Color(0xFF0E1626))
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_logo_111),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(32.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "辰阳电子 WMS",
                            color = Slate200,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = displayName.ifBlank { "操作员" },
                            color = Slate400,
                            fontSize = 12.sp
                        )
                    }
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Logout,
                            contentDescription = "退出",
                            tint = Slate200
                        )
                    }
                }
                Text(
                    text = "选择作业模块",
                    color = Slate400,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            itemsIndexed(HomeGroups) { _, group ->
                AnimatedVisibility(
                    visible = entered,
                    enter = fadeIn() + slideInVertically { it / 8 }
                ) {
                    Column(modifier = Modifier.padding(bottom = 12.dp)) {
                        Text(
                            text = group.title,
                            color = group.accent.accent,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        ModuleGrid(
                            modules = group.modules,
                            accent = group.accent,
                            onClick = { module ->
                                onModuleClick(module)
                                scope.launch {
                                    snackbar.showSnackbar("「${module.title}」业务页将在后续迭代开放")
                                }
                            }
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(20.dp)) }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(12.dp)
        )
    }
}

@Composable
private fun ModuleGrid(
    modules: List<HomeModule>,
    accent: GroupAccent,
    onClick: (HomeModule) -> Unit
) {
    val rows = modules.chunked(HOME_COLUMNS)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { module ->
                    ModuleTile(
                        module = module,
                        accent = accent,
                        onClick = { onClick(module) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(HOME_COLUMNS - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ModuleTile(
    module: HomeModule,
    accent: GroupAccent,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .aspectRatio(0.92f)
            .clip(RoundedCornerShape(10.dp))
            .background(accent.tileBg)
            .border(1.dp, accent.border, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(accent.iconWell),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = module.icon,
                contentDescription = null,
                tint = accent.accent,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = module.title,
            color = Slate200,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
