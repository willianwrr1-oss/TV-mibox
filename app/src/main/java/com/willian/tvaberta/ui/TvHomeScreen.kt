package com.willian.tvaberta.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.willian.tvaberta.MainViewModel
import kotlinx.coroutines.delay

private fun fmt(n: Int): String = java.text.NumberFormat.getIntegerInstance(java.util.Locale("pt", "BR")).format(n)

@Composable
fun TvHomeScreen(vm: MainViewModel) {
    val c by vm.homeCounts.collectAsState()
    val cont by vm.continueWatching.collectAsState()
    val favs by vm.favorites.collectAsState()
    val first = remember { FocusRequester() }

    LaunchedEffect(c != null) {
        delay(200)
        if (vm.focusPending) {
            runCatching { first.requestFocus() }
            vm.focusPending = false
        }
    }

    val feat = cont.firstOrNull()
    val (h1, h2) = hueColors(feat?.title ?: "TV Aberta")

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        Row(Modifier.fillMaxWidth().height(196.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Destaque: o que você estava vendo
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(h1, h2)))
            ) {
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.horizontalGradient(listOf(Color(0xEB0E1116), Color(0x260E1116)))
                    )
                )
                Column(Modifier.align(Alignment.BottomStart).padding(start = 24.dp, bottom = 20.dp, end = 24.dp)) {
                    Text(
                        if (feat != null) "CONTINUAR ASSISTINDO" else "BEM-VINDO",
                        fontSize = TvType.meta,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        feat?.title ?: "TV Aberta",
                        Modifier.padding(top = 4.dp),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        if (feat != null) feat.ch.groups.firstOrNull().orEmpty().let { catLabel(it) }
                        else "Escolha uma área para começar.",
                        Modifier.padding(top = 2.dp, bottom = 14.dp),
                        fontSize = TvType.item,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (feat != null) {
                        TvButton("Assistir", compact = true, requester = first) { vm.play(feat.ch, cont.map { it.ch }) }
                    } else {
                        TvButton("Abrir TV ao vivo", compact = true, requester = first) { vm.openArea(0) }
                    }
                }
            }
            // Atalhos das áreas
            Column(Modifier.width(250.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AreaCard(
                    Icons.Default.LiveTv, "TV ao vivo",
                    if (c == null) "" else fmt(c!!.live) + " canais · " + c!!.liveCats + " categorias",
                    Modifier.weight(1f)
                ) { vm.openArea(0) }
                AreaCard(Icons.Default.Movie, "Filmes", if (c == null) "" else fmt(c!!.movies) + " títulos", Modifier.weight(1f)) { vm.openArea(1) }
                AreaCard(Icons.Default.Tv, "Séries", if (c == null) "" else fmt(c!!.series) + " séries", Modifier.weight(1f)) { vm.openArea(2) }
            }
        }

        if (cont.isNotEmpty()) {
            TvSectionTitle("Continuar assistindo")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 6.dp, horizontal = 4.dp)) {
                items(cont, key = { "c:" + it.ch.key }) { it ->
                    TvMediaCard(it.title, catLabel(it.ch.groups.firstOrNull().orEmpty()), it.ch.logo, 168.dp) {
                        vm.play(it.ch, cont.map { x -> x.ch })
                    }
                }
            }
        }
        if (favs.isNotEmpty()) {
            TvSectionTitle("Favoritos")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 6.dp, horizontal = 4.dp)) {
                items(favs.take(40), key = { "f:" + it.ch.key }) { it ->
                    TvMediaCard(it.title, catLabel(it.ch.groups.firstOrNull().orEmpty()), it.ch.logo, 168.dp) {
                        vm.play(it.ch, favs.map { x -> x.ch })
                    }
                }
            }
        }
    }
}

@Composable
private fun AreaCard(icon: ImageVector, title: String, sub: String, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier
            .fillMaxWidth()
            .tvClickable(shape, onClick = onClick)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.padding(start = 12.dp)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            if (sub.isNotEmpty()) Text(sub, fontSize = TvType.meta, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}
