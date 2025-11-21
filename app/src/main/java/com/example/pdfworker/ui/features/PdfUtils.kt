package com.example.pdfworker.ui.features

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

internal fun downloadPdf(context: Context, pdfFile: File) {
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

internal fun sharePdf(context: Context, pdfFile: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", pdfFile)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share PDF"))
}

internal fun getFileName(context: Context, uri: Uri): String {
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
