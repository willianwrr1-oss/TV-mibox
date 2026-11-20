package com.willian.tvaberta.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.willian.tvaberta.MainViewModel
import kotlinx.coroutines.delay
import java.io.File

private val listExt = setOf("m3u", "m3u8", "txt")

/**
 * Devolve uma função que inicia a importação de um arquivo M3U. Usa um navegador de pastas do próprio
 * app, porque muitas TVs e TV Boxes não trazem o seletor de arquivos do Android.
 */
@Composable
fun rememberTvM3uImport(vm: MainViewModel): () -> Unit {
    val ctx = LocalContext.current
    var open by remember { mutableStateOf(false) }
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) open = true else vm.toast("Sem permissão para ler arquivos do aparelho.")
    }
    if (open) {
        TvFilePicker(
            onPick = {
                open = false
                vm.importM3uFile(it)
            },
            onClose = { open = false }
        )
    }
    return remember {
        {
            val granted = Build.VERSION.SDK_INT >= 33 ||
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED
            if (granted) open = true else perm.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
}

@Composable
private fun TvFilePicker(onPick: (File) -> Unit, onClose: () -> Unit) {
    val internal = remember { Environment.getExternalStorageDirectory() }
    val downloads = remember { File(internal, "Download") }
    var dir by remember { mutableStateOf<File>(if (downloads.canRead()) downloads else internal) }
    val kids: List<File> = remember(dir) {
        (dir.listFiles()?.toList() ?: emptyList())
            .filter { !it.name.startsWith(".") && (it.isDirectory || it.extension.lowercase() in listExt) }
            .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }
    val parent: File? = dir.parentFile?.takeIf { dir.path != "/storage" && it.canRead() }
    val first = remember { FocusRequester() }
    LaunchedEffect(dir) {
        delay(150)
        runCatching { first.requestFocus() }
    }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.82f).fillMaxHeight(0.88f),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Escolher arquivo M3U", style = MaterialTheme.typography.headlineSmall)
                Text(
                    dir.path,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TvButton("Download") { if (downloads.canRead()) dir = downloads }
                    TvButton("Memória interna") { dir = internal }
                    TvButton("USB / outros") { dir = File("/storage") }
                    TvButton("Fechar", danger = true) { onClose() }
                }
                val rows = (if (parent != null) 1 else 0) + kids.size
                if (rows == 0) {
                    Text(
                        "Nenhum arquivo .m3u, .m3u8 ou .txt nesta pasta. Se tiver acabado de copiar o arquivo, " +
                            "confira se ele está em Download.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                val items: List<File?> = (if (parent != null) listOf<File?>(null) else emptyList<File?>()) + kids
                LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(items) { i, f ->
                        val req = if (i == 0) first else null
                        if (f == null) {
                            TvButton("↑  Pasta acima", modifier = Modifier.fillMaxWidth(), requester = req) {
                                if (parent != null) dir = parent
                            }
                        } else if (f.isDirectory) {
                            TvButton("📁  " + f.name, modifier = Modifier.fillMaxWidth(), requester = req) { dir = f }
                        } else {
                            TvButton("📄  " + f.name, modifier = Modifier.fillMaxWidth(), requester = req) { onPick(f) }
                        }
                    }
                }
            }
        }
    }
}
