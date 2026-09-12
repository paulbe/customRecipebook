package com.customrecipebook.app.ui.add

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.customrecipebook.app.RecipebookApplication
import com.customrecipebook.app.data.ImportSource
import com.customrecipebook.app.ui.components.BackCircle
import com.customrecipebook.app.ui.components.CardShape
import com.customrecipebook.app.ui.theme.Clay
import com.customrecipebook.app.ui.theme.Espresso
import com.customrecipebook.app.ui.theme.Ivory
import com.customrecipebook.app.ui.theme.Sand
import com.customrecipebook.app.ui.theme.Taupe
import com.customrecipebook.app.ui.theme.Terracotta
import java.io.File

@Composable
fun AddRecipeScreen(
    initialSource: ImportSource?,
    onBack: () -> Unit,
    onManual: () -> Unit,
    onReview: () -> Unit,
    onSaved: (String) -> Unit,
    app: RecipebookApplication,
) {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val vm: AddRecipeViewModel = viewModel(activity, factory = app.container.factory)
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var pendingLaunch by remember { mutableStateOf(initialSource) }

    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importPdf(uri, onReview)
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) vm.importPhoto(uri, onReview)
    }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = pendingCameraUri
        if (ok && uri != null) vm.importPhoto(uri, onReview)
    }
    val cameraPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            val uri = createImageUri(context)
            pendingCameraUri = uri
            takePicture.launch(uri)
        } else {
            imagePicker.launch("image/*")
        }
    }

    fun startPdf() = pdfPicker.launch(arrayOf("application/pdf"))
    fun startCamera() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) {
            val uri = createImageUri(context)
            pendingCameraUri = uri
            takePicture.launch(uri)
        } else {
            cameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(pendingLaunch) {
        when (pendingLaunch) {
            ImportSource.PDF -> {
                startPdf()
                pendingLaunch = null
            }
            ImportSource.CAMERA -> {
                startCamera()
                pendingLaunch = null
            }
            ImportSource.MANUAL -> {
                pendingLaunch = null
                onManual()
            }
            else -> Unit
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbar.showSnackbar(it)
            vm.clearError()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Sand)
            .statusBarsPadding(),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BackCircle(onClick = onBack)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Add recipe", color = Espresso, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                    Text("Bring something into Custom Recipebook", color = Taupe, fontSize = 13.sp)
                }
            }

            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ImportRow(
                    title = "Upload PDF",
                    subtitle = "Parse a saved recipe file",
                    icon = Icons.Outlined.PictureAsPdf,
                    onClick = { startPdf() },
                )
                ImportRow(
                    title = "Scan with camera",
                    subtitle = "Snap a page — OCR fills the rest",
                    icon = Icons.Outlined.CameraAlt,
                    onClick = { startCamera() },
                )
                ImportRow(
                    title = "Enter manually",
                    subtitle = "Build it ingredient by ingredient",
                    icon = Icons.Outlined.Edit,
                    onClick = onManual,
                )

                if (state.isParsing) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(CardShape)
                            .background(Ivory)
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(color = Terracotta, strokeWidth = 3.dp, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Reading your file…", color = Espresso, fontSize = 15.sp)
                    }
                } else if (state.hasPreview) {
                    ImportPreviewCard(state)
                } else {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(CardShape)
                            .background(Clay)
                            .padding(20.dp),
                    ) {
                        Text(
                            "Choose a PDF to copy it into Custom Recipebook. If the file has a text layer, ingredients and steps are filled in for you to edit. Photos open an empty form with the picture attached.",
                            color = Taupe,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                        )
                    }
                }
                Spacer(Modifier.height(72.dp))
            }
        }

        if (state.hasPreview) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Sand)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Ivory)
                        .clickable(onClick = onReview)
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Review", color = Espresso, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Terracotta)
                        .clickable { vm.save(onSaved) }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Save", color = Ivory, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            }
        }

        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun ImportRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(Ivory)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Clay),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Terracotta)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Espresso, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Taupe, fontSize = 13.sp)
        }
        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = Taupe)
    }
}

@Composable
private fun ImportPreviewCard(state: AddRecipeUiState) {
    val draft = state.draft
    Column(
        Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(Ivory)
            .padding(20.dp),
    ) {
        Text("Import preview", color = Taupe, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        if (draft.attachmentName != null) {
            Text(draft.attachmentName, color = Taupe, fontSize = 12.sp)
        }
        Spacer(Modifier.height(8.dp))
        Text(draft.title.ifBlank { "Untitled recipe" }, color = Espresso, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        if (draft.parseMessage.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(draft.parseMessage, color = Taupe, fontSize = 14.sp, lineHeight = 20.sp)
        }
        if (draft.ingredients.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(
                draft.ingredients.take(4).joinToString(" · ") { it.name.substringBefore(",") },
                color = Espresso,
                fontSize = 14.sp,
            )
        }
        if (draft.titleConfidence > 0 || draft.ingredientsConfidence > 0 || draft.instructionsConfidence > 0) {
            Spacer(Modifier.height(18.dp))
            Text("Read quality", color = Taupe, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(10.dp))
            ConfidenceRow("Title", draft.titleConfidence)
            ConfidenceRow("Ingredients", draft.ingredientsConfidence)
            ConfidenceRow("Instructions", draft.instructionsConfidence)
        }
    }
}

@Composable
private fun ConfidenceRow(label: String, percent: Int) {
    Column(Modifier.padding(vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Espresso, fontSize = 14.sp)
            Text("$percent%", color = Taupe, fontSize = 13.sp)
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { percent / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(8.dp)),
            color = Terracotta,
            trackColor = Clay,
            strokeCap = StrokeCap.Round,
        )
    }
}

private fun createImageUri(context: android.content.Context): Uri {
    val dir = File(context.cacheDir, "images").apply { mkdirs() }
    val file = File(dir, "capture-${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
