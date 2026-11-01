package dev.hatchzip.android.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.hatchzip.android.ui.theme.*
import java.io.File

@Composable
fun ResumeBanner(
    interruptedArchivePath: String,
    onResumeClick: (String) -> Unit,
    onRestoreClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val archiveName = File(interruptedArchivePath).name

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = AmberWarningContainer,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AmberWarning.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Outlined.History,
                    contentDescription = null,
                    tint = AmberWarningText,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Interrupted Extraction Found",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = AmberWarningText
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$archiveName was partially hatched before an OS stop or power loss. Safe to resume with zero data loss.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AmberWarningText.copy(alpha = 0.9f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { onRestoreClick(interruptedArchivePath) },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = AmberWarningText
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AmberWarningText.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Restore,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Revert Archive", style = MaterialTheme.typography.labelSmall)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = { onResumeClick(interruptedArchivePath) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmberWarning,
                        contentColor = Color(0xFF0A0A0C)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("Resume Safely", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
