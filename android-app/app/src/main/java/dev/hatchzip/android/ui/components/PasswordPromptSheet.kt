package dev.hatchzip.android.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hatchzip.android.ui.theme.*
import dev.hatchzip.android.viewmodel.PasswordPromptState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordPromptSheet(
    state: PasswordPromptState,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Horizontal shake offset on invalid password attempts
    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(state.failedAttempts) {
        if (state.failedAttempts > 0) {
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 350
                    0f at 0
                    (-24f) at 50
                    24f at 100
                    (-18f) at 150
                    18f at 200
                    (-10f) at 250
                    10f at 300
                    0f at 350
                }
            )
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF0F0F14),
        scrimColor = Color.Black.copy(alpha = 0.65f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title Header with Lock Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = HatchPrimary.copy(alpha = 0.15f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = "Lock",
                            tint = HatchPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (state.isHeaderEncryption) "Encrypted Archive" else "Decryption Required",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = state.archiveName,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Safety Reassurance Callout
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = EmeraldGreenContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = "Shield",
                        tint = EmeraldGreenText,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Pre-Truncation Guarantee: HatchDroid verifies your password and validates checksums before modifying or truncating a single byte of your archive.",
                        style = MaterialTheme.typography.bodySmall,
                        color = EmeraldGreenText
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Input Field (Monospace for unambiguous characters 0 vs O, 1 vs l)
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Archive Password", color = TextSecondary) },
                placeholder = { Text("Enter passphrase", color = TextTertiary) },
                singleLine = true,
                isError = state.errorMessage != null,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (password.isNotBlank()) {
                            keyboardController?.hide()
                            onSubmit(password)
                        }
                    }
                ),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = if (passwordVisible) MaterialTheme.typography.bodyLarge.letterSpacing else 2.sp,
                    color = TextPrimary
                ),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = TextSecondary
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(x = shakeOffset.value.dp)
                    .focusRequester(focusRequester),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = HatchPrimary,
                    unfocusedBorderColor = Color(0xFF262633),
                    errorBorderColor = CoralRed
                )
            )

            // Error display
            if (state.errorMessage != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = state.errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = CoralRed,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF262633))
                ) {
                    Text("Cancel", style = MaterialTheme.typography.labelMedium)
                }

                Button(
                    onClick = {
                        if (password.isNotBlank()) {
                            keyboardController?.hide()
                            onSubmit(password)
                        }
                    },
                    enabled = password.isNotBlank(),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HatchPrimary,
                        contentColor = Color(0xFF0A0A0C),
                        disabledContainerColor = Color(0xFF222230),
                        disabledContentColor = Color(0xFF555566)
                    )
                ) {
                    Text(
                        text = if (state.isHeaderEncryption) "Unlock" else "Extract",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
