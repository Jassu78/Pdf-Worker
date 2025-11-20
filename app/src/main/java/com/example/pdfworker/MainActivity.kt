package com.example.pdfworker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.pdfworker.ui.features.MergePdfScreen
import com.example.pdfworker.ui.theme.PdfWorkerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PdfWorkerTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "main") {
                    composable("main") { MainScreen(navController) }
                    composable("merge_pdf") { MergePdfScreen(navController) }
                }
            }
        }
    }
}

@Composable
fun MainScreen(navController: NavController) {
    Scaffold(
        topBar = {
            GlassmorphicTopAppBar() 
        },
        bottomBar = {
            GlassmorphicBottomNavBar()
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0E1A3D))
                .padding(it)
        ) {
            FeatureGrid(navController)
        }
    }
}

@Composable
fun GlassmorphicTopAppBar() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(56.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "PDF Utility App",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp)
            )
            Icon(
                painter = painterResource(id = R.drawable.ic_user),
                contentDescription = "User",
                tint = Color.White,
                modifier = Modifier.padding(end = 16.dp)
            )
        }
    }
}

@Composable
fun GlassmorphicBottomNavBar() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(64.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Icon(painter = painterResource(id = R.drawable.ic_home), contentDescription = "Home", tint = Color.White)
            Icon(painter = painterResource(id = R.drawable.ic_files), contentDescription = "Files", tint = Color.White)
            Icon(painter = painterResource(id = R.drawable.ic_premium), contentDescription = "Premium", tint = Color.White)
        }
    }
}

@Composable
fun FeatureGrid(navController: NavController) {
    val features = listOf(
        "Merge PDF", "Split PDF", "PDF to Word",
        "Compress PDF", "PDF to JPG", "My Files"
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(features) { feature ->
            FeatureCard(feature = feature, navController = navController)
        }
    }
}

@Composable
fun FeatureCard(feature: String, navController: NavController) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(24.dp))
            .clickable { 
                if (feature == "Merge PDF") {
                    navController.navigate("merge_pdf")
                }
            },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = getIconForFeature(feature)), 
                contentDescription = feature, 
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = feature, color = Color.White, fontSize = 16.sp)
        }
    }
}

fun getIconForFeature(feature: String): Int {
    return when (feature) {
        "Merge PDF" -> R.drawable.ic_merge
        "Split PDF" -> R.drawable.ic_split
        "PDF to Word" -> R.drawable.ic_pdf_to_word
        "Compress PDF" -> R.drawable.ic_compress
        "PDF to JPG" -> R.drawable.ic_pdf_to_jpg
        "My Files" -> R.drawable.ic_my_files
        else -> R.drawable.ic_file
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    PdfWorkerTheme {
        val navController = rememberNavController()
        MainScreen(navController)
    }
}
