package com.example.pdfworker.ui.features

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.launch
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfToWordScreen(navController: NavController) {
    var selectedPdf by remember { mutableStateOf<Uri?>(null) }
    var docxFile by remember { mutableStateOf<File?>(null) }

    when {
        docxFile != null -> {
            PdfToWordSuccessScreen(navController, docxFile!!) { docxFile = null; selectedPdf = null }
        }
        selectedPdf != null -> {
            ConvertPdfToWordScreen(
                pdfUri = selectedPdf!!,
                onConvert = { docxFile = it },
                onCancel = { selectedPdf = null }
            )
        }
        else -> {
            SelectPdfForConversionScreen { selectedPdf = it }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectPdfForConversionScreen(onPdfSelected: (Uri) -> Unit) {
    val pickPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = onPdfSelected
    )

    Scaffold(
        topBar = { TopAppBar(title = { Text("PDF to Word") }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF87CEFA))) }
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0xFF0E1A3D)).padding(it),
            contentAlignment = Alignment.Center
        ) {
            Button(onClick = { pickPdfLauncher.launch("application/pdf") }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF90EE90))) {
                Text("Select PDF to Convert", color = Color.Black)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConvertPdfToWordScreen(pdfUri: Uri, onConvert: (File?) -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = { TopAppBar(title = { Text("PDF to Word") }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF87CEFA))) }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().background(Color(0xFF0E1A3D)).padding(it).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Ready to convert:", color = Color.White)
            Text(getFileName(context, pdfUri), color = Color.White)
            Spacer(modifier = Modifier.height(32.dp))
            Row {
                Button(onClick = onCancel, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF08080))) {
                    Text("Cancel", color = Color.White)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(onClick = { 
                    coroutineScope.launch {
                        onConvert(convertPdfToWord(context, pdfUri))
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF90EE90))) {
                    Text("Convert", color = Color.Black)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfToWordSuccessScreen(navController: NavController, docxFile: File, onBackToHome: () -> Unit) {
    val context = LocalContext.current
    Scaffold(
        topBar = { TopAppBar(title = { Text("PDF to Word") }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF87CEFA))) }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().background(Color(0xFF0E1A3D)).padding(it).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Successfully converted PDF to Word!", color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))
            Text(docxFile.name, color = Color.White)
            Spacer(modifier = Modifier.height(32.dp))
            Row {
                Button(onClick = { downloadPdf(context, docxFile) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF90EE90))) {
                    Text("Save", color = Color.Black)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(onClick = { sharePdf(context, docxFile) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF87CEFA))) {
                    Text("Share", color = Color.Black)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { onBackToHome(); navController.popBackStack() }, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)) {
                Text("Back to home")
            }
        }
    }
}

private fun convertPdfToWord(context: Context, pdfUri: Uri): File? {
    return try {
        val outputDir = File(context.filesDir, "pdf_to_word")
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
        val docxFile = File(outputDir, "${getFileName(context, pdfUri)}.docx")

        context.contentResolver.openInputStream(pdfUri)?.use { inputStream ->
            PDDocument.load(inputStream).use { document ->
                val textStripper = PDFTextStripper()
                val text = textStripper.getText(document)

                XWPFDocument().use { docx ->
                    docx.createParagraph().createRun().setText(text)
                    FileOutputStream(docxFile).use { outputStream ->
                        docx.write(outputStream)
                    }
                }
            }
        }
        docxFile
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
