package dev.hatchzip.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderShared
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.hatchzip.android.ui.theme.*
import dev.hatchzip.android.util.PermissionUtil

@Composable
fun PermissionScreen(
    onPermissionGranted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = HatchBackground,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = HatchPrimary.copy(alpha = 0.15f),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, HatchPrimary.copy(alpha = 0.3f)),
                modifier = Modifier.size(88.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.FolderShared,
                        contentDescription = null,
                        tint = HatchPrimary,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "HatchDroid Storage Access",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Unlike standard extractors that duplicate files before deleting, HatchDroid operates in-place: shrinking the archive from the tail as each file hatches.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(20.dp))

            Surface(
                color = Color(0xFF14141C),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF222230)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = null,
                        tint = EmeraldGreen,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Requires All Files Access (MANAGE_EXTERNAL_STORAGE) to execute POSIX ftruncate and fallocate directly on disk.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    val intent = PermissionUtil.requestAllFilesAccessIntent(context)
                    context.startActivity(intent)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = HatchPrimary,
                    contentColor = Color(0xFF0A0A0C)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = "Grant All Files Access",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
