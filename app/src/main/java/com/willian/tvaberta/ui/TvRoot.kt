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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.willian.tvaberta.MainViewModel
import com.willian.tvaberta.data.SettingsStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private class RailTab(val label: String, val icon: ImageVector)

private val railTabs = listOf(
    RailTab("Canais", Icons.Default.LiveTv),
    RailTab("Guia", Icons.Default.Schedule),
    RailTab("Favoritos", Icons.Default.Star),
    RailTab("Gerenciar", Icons.Default.Tune),
    RailTab("Ajustes", Icons.Default.Settings),
)

@Composable
fun TvRoot(vm: MainViewModel) {
    val s by SettingsStore.flow.collectAsState()
    AppTheme(s) {
        val playing = vm.player.current != null
        var toast by remember { mutableStateOf<String?>(null) }
        var railPick by remember { mutableIntStateOf(vm.tab) }

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
        // Passar pelo menu lateral troca a tela (com um pequeno atraso para não piscar).
        LaunchedEffect(railPick) {
            delay(250)
            if (vm.tab != railPick) vm.tab = railPick
        }

        var exitAsk by remember { mutableStateOf(false) }
        val ctx = LocalContext.current
        BackHandler(enabled = !playing) {
            if (vm.tab != 0) {
                vm.tab = 0
                railPick = 0
                vm.focusPending = true
            } else {
                exitAsk = true
            }
        }

        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (playing) {
                TvPlayerScreen(vm)
            } else {
                Row(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 40.dp, vertical = 24.dp)
                ) {
                    Rail(vm, onPick = { railPick = it })
                    Spacer(Modifier.width(28.dp))
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        key(vm.tab) {
                            when (vm.tab) {
                                0 -> TvChannelsScreen(vm, favoritesOnly = false)
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
private fun Rail(vm: MainViewModel, onPick: (Int) -> Unit) {
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val requesters = remember { List(railTabs.size) { FocusRequester() } }
    Column(
        Modifier
            .width(190.dp)
            .fillMaxHeight()
            // Ao voltar para o menu pela esquerda, o foco cai no item da tela atual.
            .focusProperties { enter = { requesters[vm.tab.coerceIn(0, requesters.lastIndex)] } }
            .focusGroup(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 6.dp, bottom = 14.dp)) {
            Icon(Icons.Default.LiveTv, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
            Spacer(Modifier.width(10.dp))
            Text("TV Aberta", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        railTabs.forEachIndexed { i, t ->
            val selected = vm.tab == i
            val shape = RoundedCornerShape(12.dp)
            Row(
                Modifier
                    .fillMaxWidth()
                    .focusRequesterOf(requesters[i])
                    .tvClickable(shape, onFocus = { if (it) onPick(i) }) {
                        onPick(i)
                        vm.tab = i
                        scope.launch {
                            delay(150)
                            focusManager.moveFocus(FocusDirection.Right)
                        }
                    }
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                        else MaterialTheme.colorScheme.surfaceVariant,
                        shape
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(t.icon, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    t.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

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
