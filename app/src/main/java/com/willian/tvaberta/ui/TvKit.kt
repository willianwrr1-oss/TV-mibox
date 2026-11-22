package com.willian.tvaberta.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

/** Tamanhos de texto do novo visual da TV (nada de letras gigantes). */
object TvType {
    val pageTitle = 22.sp
    val section = 16.sp
    val item = 12.sp
    val meta = 11.sp
}

fun hueColors(seed: String): Pair<Color, Color> {
    val h = ((seed.hashCode() and 0x7fffffff) % 360).toFloat()
    return Pair(Color.hsv(h, 0.38f, 0.30f), Color.hsv(h, 0.42f, 0.16f))
}

fun initialsOf(name: String): String =
    name.trim().split(' ').filter { it.isNotEmpty() }.take(2).joinToString("") { it.first().uppercase() }

@Composable
fun TvSectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier.padding(top = 18.dp, bottom = 8.dp),
        fontSize = TvType.section,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground
    )
}

/** Cartão de canal ou filme: miniatura 16:9 com logo, título e linha de apoio. O foco fica na miniatura. */
@Composable
fun TvMediaCard(
    title: String,
    subtitle: String,
    logo: String?,
    width: Dp,
    modifier: Modifier = Modifier,
    progress: Float? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    val (c1, c2) = hueColors(title)
    Column(modifier.width(width)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(width * 9f / 16f)
                .tvClickable(shape, onLongClick = onLongClick, onClick = onClick)
                .clip(shape)
                .background(Brush.linearGradient(listOf(c1, c2))),
            contentAlignment = Alignment.Center
        ) {
            if (!logo.isNullOrBlank()) {
                AsyncImage(
                    model = logo,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().padding(14.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                Text(initialsOf(title), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xB8FFFFFF))
            }
            if (progress != null) {
                Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(3.dp).background(Color(0x66000000))) {
                    Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(3.dp).background(MaterialTheme.colorScheme.primary))
                }
            }
        }
        Text(
            title,
            Modifier.padding(top = 6.dp),
            fontSize = TvType.item,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (subtitle.isNotEmpty()) {
            Text(
                subtitle,
                fontSize = TvType.meta,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
