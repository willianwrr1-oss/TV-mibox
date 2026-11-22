package com.willian.tvaberta.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.willian.tvaberta.MainViewModel
import com.willian.tvaberta.data.SettingsStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private class TopTab(val label: String, val tab: Int, val area: Int?)

private val topTabs = listOf(
    TopTab("Início", 0, null),
    TopTab("TV ao vivo", 0, 0),
    TopTab("Filmes", 0, 1),
    TopTab("Séries", 0, 2),
    TopTab("Guia", 1, null),
    TopTab("Favoritos", 2, null),
    TopTab("Gerenciar", 3, null),
    TopTab("Ajustes", 4, null),
)

@Composable
fun TvRoot(vm: MainViewModel) {
    val s by SettingsStore.flow.collectAsState()
    AppTheme(if (s.theme == 0) s.copy(theme = 2) else s) {
        val playing = vm.player.current != null && vm.fullscreen
        var toast by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(Unit) {
            vm.events.collect { e -> toast = e.text }
        }
        LaunchedEffect(toast) {
            if (toast != null) {
                delay(3500)
                toast = null
            }
        }
        // Ao voltar do player, o foco volta para o último canal.
        LaunchedEffect(playing) {
            if (!playing) vm.focusPending = true
        }
        var exitAsk by remember { mutableStateOf(false) }
        val ctx = LocalContext.current
        LaunchedEffect(vm.tab) {
            if (vm.tab != 0 && vm.previewMode && !vm.fullscreen) vm.stopPreview()
        }
        BackHandler(enabled = !playing) {
            if (vm.tab == 0 && vm.previewMode && vm.player.current != null) {
                vm.stopPreview()
            } else if (vm.tab != 0) {
                vm.tab = 0
                vm.focusPending = true
            } else if (vm.area != null) {
                vm.openArea(null)
                vm.focusPending = true
            } else {
                exitAsk = true
            }
        }

        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                if (playing) {
                    TvPlayerScreen(vm)
                } else {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = 44.dp, vertical = 16.dp)
                    ) {
                        TopBar(vm)
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            key(vm.tab) {
                                when (vm.tab) {
                                    0 -> if (vm.area == null) TvHomeScreen(vm) else TvLiveScreen(vm)
                                    1 -> TvGuideScreen(vm)
                                    2 -> TvChannelsScreen(vm, favoritesOnly = true)
                                    3 -> TvManageScreen(vm)
                                    else -> TvSettingsScreen(vm)
                                }
                            }
                        }
                    }
                }

                val msg = toast
                if (msg != null) {
                    Text(
                        msg,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 40.dp)
                            .background(Color(0xE6000000), RoundedCornerShape(12.dp))
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }

        RenameDialog(vm)
        DeleteDialog(vm)
        if (exitAsk) {
            val stay = remember { FocusRequester() }
            LaunchedEffect(Unit) {
                delay(150)
                runCatching { stay.requestFocus() }
            }
            AlertDialog(
                onDismissRequest = { exitAsk = false },
                title = { Text("Fechar o aplicativo?") },
                text = { Text("Deseja realmente sair do TV Aberta?") },
                confirmButton = {
                    TvButton("Sim, fechar", danger = true) { ctx.findActivity()?.finish() }
                },
                dismissButton = {
                    TvButton("Cancelar", requester = stay) { exitAsk = false }
                }
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun TopBar(vm: MainViewModel) {
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val requesters = remember { List(topTabs.size) { FocusRequester() } }
    val selected = topTabs.indexOfFirst { it.tab == vm.tab && (it.tab != 0 || it.area == vm.area) }.coerceAtLeast(0)
    var clock by remember { mutableStateOf(clockText()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(20_000)
            clock = clockText()
        }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .height(44.dp)
            // Ao subir do conteúdo, o foco cai na aba da tela atual.
            .focusProperties { enter = { requesters[selected.coerceIn(0, requesters.lastIndex)] } }
            .focusGroup(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(Icons.Default.LiveTv, contentDescription = "TV Aberta", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Text("TV Aberta", Modifier.padding(start = 8.dp, end = 18.dp), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        topTabs.forEachIndexed { i, t ->
            val sel = i == selected
            val shape = RoundedCornerShape(50)
            Box(
                Modifier
                    .focusRequesterOf(requesters[i])
                    .tvClickable(shape) {
                        vm.tab = t.tab
                        if (t.tab == 0) vm.openArea(t.area)
                        vm.focusPending = true
                        scope.launch {
                            delay(150)
                            focusManager.moveFocus(FocusDirection.Down)
                        }
                    }
                    .background(if (sel) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent, shape)
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Text(
                    t.label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (sel) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Text(clock, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun clockText(): String =
    java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())

private fun Modifier.focusRequesterOf(r: FocusRequester): Modifier =
    this.then(Modifier.focusRequester(r))

@Composable
private fun RenameDialog(vm: MainViewModel) {
    val target = vm.renameTarget ?: return
    var text by remember(target.ch.key) { mutableStateOf(target.title) }
    AlertDialog(
        onDismissRequest = { vm.renameTarget = null },
        title = { Text("Renomear canal") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                label = { Text("Nome") },
                supportingText = { Text("Deixe em branco para voltar ao nome original.") }
            )
        },
        confirmButton = {
            TextButton(onClick = {
                vm.rename(target.ch.key, text)
                vm.renameTarget = null
            }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = { vm.renameTarget = null }) { Text("Cancelar") } }
    )
}

@Composable
private fun DeleteDialog(vm: MainViewModel) {
    val keys = vm.pendingDelete
    if (keys.isEmpty()) return
    AlertDialog(
        onDismissRequest = { vm.pendingDelete = emptyList() },
        title = { Text(if (keys.size == 1) "Excluir este canal?" else "Excluir ${keys.size} canais?") },
        text = {
            Text(
                "O canal sai da lista e não volta com as atualizações. " +
                    "Você pode restaurá-lo depois em Gerenciar > Excluídos."
            )
        },
        confirmButton = {
            TextButton(onClick = {
                vm.pendingDelete = emptyList()
                vm.delete(keys)
            }) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = { vm.pendingDelete = emptyList() }) { Text("Cancelar") } }
    )
}
