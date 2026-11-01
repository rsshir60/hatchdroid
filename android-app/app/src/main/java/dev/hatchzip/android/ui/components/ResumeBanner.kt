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
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Outlined.History,
                    contentDescription = null,
                    tint = AmberWarningText,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Interrupted Extraction Found",
                        style = MaterialTheme.typography.titleSmall,
                        color = AmberWarningText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$archiveName was partially hatched before an OS stop or power loss. You can safely resume without restarting or data loss.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AmberWarningText
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

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
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Restore,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Revert Archive", style = MaterialTheme.typography.labelSmall)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = { onResumeClick(interruptedArchivePath) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmberWarning,
                        contentColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Resume Safely", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
