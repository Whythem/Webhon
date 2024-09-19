package com.example.webhonlocal

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.*
import coil.compose.rememberAsyncImagePainter
import coil.compose.rememberImagePainter
import com.example.webhonlocal.ui.theme.WebhonLocalTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

// Data classes
data class MangaResponse(val data: List<Manga>)
data class Manga(
    val mal_id: Int,
    val url: String,
    val images: Images,
    val titles: List<Title>,
    val title: String,
    val type: String,
    val status: String,
    val score: Double,
    val synopsis: String
)
data class Images(val jpg: ImageUrls)
data class ImageUrls(
    val image_url: String
)
data class Title(val title: String)

// Main Activity
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApp()
        }
    }
}

// App Composable
@Composable
fun MyApp() {
    val navController = rememberNavController()
    Scaffold(
        topBar = { Header() },
        bottomBar = { Footer(navController) }
    ) {
        // Set the background color of the entire app to #011724
        Box(modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF011724)) // Set the color here
            .padding(it)
        ) {
            NavHost(navController, startDestination = "grid") {
                composable("grid") { RectangleGrid(navController) }
                composable("details/{mangaId}") { backStackEntry ->
                    val mangaId = backStackEntry.arguments?.getString("mangaId")?.toIntOrNull()
                    mangaId?.let { MangaDetailsScreen(mangaId, navController) }
                }
            }
        }
    }
}


@Composable
fun Header() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.DarkGray)
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Add your logo or text here
            Text("Bibliothèque", color = Color.White, style = MaterialTheme.typography.headlineMedium)
            // Add any other elements (e.g. a search icon or menu)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Text("Last update", color = Color.LightGray)
            Text("Most follow", color = Color.LightGray)
            Text("New manga", color = Color.LightGray)
        }
    }
}

@Composable
fun Footer(navController: NavController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.DarkGray)
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        FooterButton(iconText = "Accueil", onClick = { /* Handle Accueil click */ })
        FooterButton(iconText = "Follow", onClick = { /* Handle Follow click */ })
        FooterButton(iconText = "Search", onClick = { /* Handle Search click */ })
        FooterButton(iconText = "Login", onClick = { /* Handle Login click */ })
    }
}

@Composable
fun FooterButton(iconText: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        // You can replace Text with Icon if you have appropriate icons
        Text(text = iconText, color = Color.White, style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(Color.White.copy(alpha = 0.2f)) // Placeholder for an icon
                .clickable { onClick() }
        )
    }
}

// Rectangle Grid Composable
@Composable
fun RectangleGrid(navController: NavController, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val mangaData by remember { mutableStateOf(loadJsonFromAssets(context, "data.json")) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(8.dp),
        modifier = modifier.fillMaxSize()
    ) {
        items(mangaData.data) { manga ->
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .height(100.dp)
                    .fillMaxWidth()
                    .clickable {
                        navController.navigate("details/${manga.mal_id}")
                    }
            ) {
                Image(
                    painter = rememberImagePainter(manga.images.jpg.image_url),
                    contentDescription = manga.title,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}


// Manga Details Screen Composable
@Composable
fun MangaDetailsScreen(mangaId: Int, navController: NavController) {
    val context = LocalContext.current
    val mangaData = loadJsonFromAssets(context, "data.json")
    val manga = mangaData.data.find { it.mal_id == mangaId }

    manga?.let {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Text(text = it.title, style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Image(
                painter = rememberImagePainter(it.images.jpg.image_url),
                contentDescription = it.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = it.synopsis, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { navController.popBackStack() }) {
                Text("Back")
            }
        }
    }
}

// Load JSON Function
private fun loadJsonFromAssets(context: Context, fileName: String): MangaResponse {
    return try {
        val inputStream = context.assets.open(fileName)
        val reader = InputStreamReader(inputStream)
        val gson = Gson()
        val type = object : TypeToken<MangaResponse>() {}.type
        gson.fromJson(reader, type)
    } catch (e: Exception) {
        e.printStackTrace()
        MangaResponse(emptyList())
    }
}
