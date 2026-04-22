package com.androidoctor.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.androidoctor.ui.theme.Copper

private const val SHIZUKU_SETUP_URL = "https://shizuku.rikka.app/guide/setup/"

@Composable
fun ShizukuSetupLink(
    modifier: Modifier = Modifier,
    label: String = "How to set up Shizuku →",
    onOpen: (() -> Unit)? = null,
) {
    val ctx = LocalContext.current
    TextButton(
        onClick = {
            if (onOpen != null) {
                onOpen()
            } else {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(SHIZUKU_SETUP_URL))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { ctx.startActivity(intent) }
            }
        },
        modifier = modifier.padding(vertical = 4.dp),
    ) {
        Text(label, color = Copper, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
