package com.example.pdfworker.ui.features

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyGridState
import org.burnoutcrew.reorderable.reorderable
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageToPdfScreen(navController: NavController) {
    var selectedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var pdfFile by remember { mutableStateOf<File?>(null) }

    when {
        pdfFile != null -> {
            ImageToPdfSuccessScreen(navController, pdfFile!!) { pdfFile = null; selectedImages = emptyList() }
        }
        selectedImages.isNotEmpty() -> {
            var currentSelectedImages by remember { mutableStateOf(selectedImages) }
            ArrangeImagesScreen(
                selectedImages = currentSelectedImages,
                onContinue = { pdfFile = it },
                onCancel = { selectedImages = emptyList() },
                onReorder = { from, to ->
                    currentSelectedImages = currentSelectedImages.toMutableList().apply {
                        add(to.index, removeAt(from.index))
                    }
                }
            )
        }
        else -> {
            SelectImagesScreen { selectedImages = it }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectImagesScreen(onImagesSelected: (List<Uri>) -> Unit) {
    val pickImagesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
        onResult = onImagesSelected
    )

    Scaffold(
        topBar = { TopAppBar(title = { Text("Img to PDF") }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF87CEFA))) }
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0xFF0E1A3D)).padding(it),
            contentAlignment = Alignment.Center
        ) {
            Button(onClick = { pickImagesLauncher.launch("image/*") }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF90EE90))) {
                Text("Select Images", color = Color.Black)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArrangeImagesScreen(
    selectedImages: List<Uri>,
    onContinue: (File?) -> Unit,
    onCancel: () -> Unit,
    onReorder: (from: org.burnoutcrew.reorderable.ItemPosition, to: org.burnoutcrew.reorderable.ItemPosition) -> Unit
) {
    val context = LocalContext.current
    val state = rememberReorderableLazyGridState(onMove = onReorder)

    Scaffold(
        topBar = { TopAppBar(title = { Text("Img to PDF") }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF87CEFA))) }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().background(Color(0xFF0E1A3D)).padding(it).padding(horizontal = 16.dp)
        ) {
            Text("Arrange your images", modifier = Modifier.padding(vertical = 16.dp), color = Color.White)
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                state = state.gridState,
                modifier = Modifier.weight(1f).reorderable(state).detectReorderAfterLongPress(state)
            ) {
                items(selectedImages, { it }) { item ->
                    ReorderableItem(state, key = item) {
                        Image(
                            painter = rememberAsyncImagePainter(item),
                            contentDescription = null,
                            modifier = Modifier.aspectRatio(1f).padding(4.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(onClick = onCancel, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF08080))) {
                    Text("Cancel", color = Color.White)
                }
                Button(onClick = { onContinue(createPdfFromImages(context, selectedImages)) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF90EE90))) {
                    Text("Continue", color = Color.Black)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageToPdfSuccessScreen(navController: NavController, pdfFile: File, onBackToHome: () -> Unit) {
    val context = LocalContext.current
    Scaffold(
        topBar = { TopAppBar(title = { Text("Img to PDF") }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF87CEFA))) }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().background(Color(0xFF0E1A3D)).padding(it).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Created PDF Successfully.", color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(pdfFile.name, color = Color.White)
                Spacer(modifier = Modifier.width(16.dp))
                Button(onClick = { sharePdf(context, pdfFile) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF87CEFA))) {
                    Text("Share", color = Color.Black)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = { downloadPdf(context, pdfFile) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF90EE90))) {
                Text("Save", color = Color.Black)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { onBackToHome(); navController.popBackStack() }, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)) {
                Text("Back to home")
            }
        }
    }
}

private fun createPdfFromImages(context: Context, imageUris: List<Uri>): File? {
    return try {
        val outputDir = File(context.filesDir, "image_to_pdf")
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
        val pdfFile = File(outputDir, "Img2PDF_${System.currentTimeMillis()}.pdf")

        PDDocument().use { document ->
            imageUris.forEach { uri ->
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    val page = PDPage()
                    document.addPage(page)
                    PDPageContentStream(document, page).use { contentStream ->
                        val image = PDImageXObject.createFromByteArray(document, bitmapToByteArray(bitmap), "image")
                        contentStream.drawImage(image, 0f, 0f, page.mediaBox.width, page.mediaBox.height)
                    }
                }
            }
            document.save(pdfFile)
        }
        pdfFile
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun bitmapToByteArray(bitmap: android.graphics.Bitmap): ByteArray {
    val stream = java.io.ByteArrayOutputStream()
    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, stream)
    return stream.toByteArray()
}
