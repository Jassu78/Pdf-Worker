package com.example.pdfworker.ui.features

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MergePdfScreen(navController: NavController) {
    var selectedPdfs by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var mergedPdf by remember { mutableStateOf<File?>(null) }

    when {
        mergedPdf != null -> {
            MergeSuccessScreen(navController, mergedPdf!!) { mergedPdf = null; selectedPdfs = emptyList() }
        }
        selectedPdfs.isNotEmpty() -> {
            var currentSelectedPdfs by remember { mutableStateOf(selectedPdfs) }
            OrderPdfScreen(
                selectedPdfs = currentSelectedPdfs,
                onMerge = { mergedPdf = it },
                onCancel = { selectedPdfs = emptyList() },
                onReorder = { from, to ->
                    currentSelectedPdfs = currentSelectedPdfs.toMutableList().apply {
                        add(to, removeAt(from))
                    }
                }
            )
        }
        else -> {
            SelectPdfScreen { selectedPdfs = it }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectPdfScreen(onPdfsSelected: (List<Uri>) -> Unit) {
    val pickPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
        onResult = onPdfsSelected
    )

    Scaffold(
        topBar = { MergePdfTopAppBar() }
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0xFF0E1A3D)).padding(it),
            contentAlignment = Alignment.Center
        ) {
            Button(onClick = { pickPdfLauncher.launch("application/pdf") }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF90EE90))) {
                Text("Select Pdf to Merge", color = Color.Black)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderPdfScreen(
    selectedPdfs: List<Uri>,
    onMerge: (File?) -> Unit,
    onCancel: () -> Unit,
    onReorder: (from: Int, to: Int) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var draggedItemIndex by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = { MergePdfTopAppBar() }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().background(Color(0xFF0E1A3D)).padding(it).padding(horizontal = 16.dp)
        ) {
            Text("Order PDF:", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 16.dp), color = Color.White)
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(selectedPdfs, key = { _, uri -> uri }) { index, uri ->
                    val fileName = getFileName(context, uri)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onLongPress = { _ ->
                                        draggedItemIndex = index
                                    },
                                    onTap = { 
                                        draggedItemIndex?.let { fromIndex ->
                                            onReorder(fromIndex, index)
                                            draggedItemIndex = null
                                        }
                                    }
                                )
                            },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (draggedItemIndex == index) Color.White else Color.Gray.copy(alpha = 0.5f)),
                        color = if (draggedItemIndex == index) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = fileName,
                            modifier = Modifier.padding(16.dp),
                            color = Color.White
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
                Button(onClick = { 
                    coroutineScope.launch {
                        val result = mergePdfs(context, selectedPdfs)
                        onMerge(result)
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF90EE90))) {
                    Text("Merge", color = Color.Black)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MergeSuccessScreen(navController: NavController, mergedPdf: File, onBackToHome: () -> Unit) {
    val context = LocalContext.current

    Scaffold(
        topBar = { MergePdfTopAppBar() }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().background(Color(0xFF0E1A3D)).padding(it).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("PDF Merged Successfully! Cheers!", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))
            Surface(border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)), shape = RoundedCornerShape(8.dp), color = Color.White.copy(alpha = 0.1f)) {
                Text("${mergedPdf.name} (${mergedPdf.length() / 1024} KB)", modifier = Modifier.padding(16.dp), color = Color.White)
            }
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = { downloadPdf(context, mergedPdf) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF90EE90))) {
                Text("Download", color = Color.Black)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { sharePdf(context, mergedPdf) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF87CEFA))) {
                Text("Share", color = Color.Black)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { onBackToHome(); navController.popBackStack() }, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black), border = BorderStroke(1.dp, Color.Black)) {
                Text("Back to home")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MergePdfTopAppBar() {
    TopAppBar(
        title = { Text("PDF Merger", color = Color.White) },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White.copy(alpha = 0.1f))
    )
}

private fun getFileName(context: Context, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (displayNameIndex != -1) {
                    result = cursor.getString(displayNameIndex)
                }
            }
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/')
        if (cut != -1) {
            if (cut != null) {
                result = result.substring(cut + 1)
            }
        }
    }
    return result ?: "Unknown"
}

private fun mergePdfs(context: Context, pdfUris: List<Uri>): File? {
    PDFBoxResourceLoader.init(context)
    val merger = PDFMergerUtility()
    val outputDir = File(context.filesDir, "merged_pdfs")
    if (!outputDir.exists()) {
        outputDir.mkdirs()
    }
    val mergedPdfFile = File(outputDir, "merged_pdf_${System.currentTimeMillis()}.pdf")
    merger.destinationFileName = mergedPdfFile.absolutePath

    val inputStreams = mutableListOf<InputStream>()

    try {
        for (uri in pdfUris) {
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream != null) {
                inputStreams.add(inputStream)
                merger.addSource(inputStream)
            } else {
                throw Exception("Could not open input stream for a URI")
            }
        }

        merger.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly())
        return mergedPdfFile
    } catch (e: Exception) {
        Log.e("MergePdfScreen", "Error merging PDFs", e)
        e.printStackTrace()
        return null
    } finally {
        inputStreams.forEach {
            try {
                it.close()
            } catch (ioe: java.io.IOException) {
                Log.e("MergePdfScreen", "Error closing stream", ioe)
            }
        }
    }
}

private fun downloadPdf(context: Context, pdfFile: File) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        downloadPdfForQ(context, pdfFile)
    } else {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val newFile = File(downloadsDir, pdfFile.name)
        FileOutputStream(newFile).use { outputStream ->
            pdfFile.inputStream().use { inputStream ->
                inputStream.copyTo(outputStream)
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
private fun downloadPdfForQ(context: Context, pdfFile: File) {
    val resolver = context.contentResolver
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, pdfFile.name)
        put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
    }

    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
    if (uri != null) {
        resolver.openOutputStream(uri).use { outputStream ->
            pdfFile.inputStream().use { inputStream ->
                inputStream.copyTo(outputStream!!)
            }
        }
    }
}

private fun sharePdf(context: Context, pdfFile: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", pdfFile)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share PDF"))
}
