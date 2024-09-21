package com.example.webhonlocal

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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.*
import coil.compose.rememberImagePainter
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import androidx.compose.ui.res.painterResource

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
    var mangaList by remember { mutableStateOf<List<Manga>>(emptyList()) }

    LaunchedEffect(Unit) {
        mangaList = AnimeApiService.getTopManga()
    }

    Scaffold(
        topBar = { Header() },
        bottomBar = { Footer(navController) }
    ) {
        Box(modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF011724))
            .padding(it)
        ) {
            NavHost(navController, startDestination = "grid") {
                composable("grid") { RectangleGrid(navController, mangaList) }
                composable("details/{mangaId}") { backStackEntry ->
                    val mangaId = backStackEntry.arguments?.getString("mangaId")?.toIntOrNull()
                    mangaId?.let { MangaDetailsScreen(it, navController) }
                }
            }
        }
    }
}

// Rectangle Grid Composable
@Composable
fun RectangleGrid(navController: NavController, mangaList: List<Manga>, modifier: Modifier = Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(4.dp),
        modifier = modifier.fillMaxSize()
    ) {
        items(mangaList) { manga ->
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .height(160.dp)
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

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(7.dp)
                ) {
                    Text(
                        text = manga.title,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// MangaDetailsScreen Composable
@Composable
fun MangaDetailsScreen(mangaId: Int, navController: NavController) {
    var manga by remember { mutableStateOf<Manga?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(mangaId) {
        try {
            manga = AnimeApiService.getMangaDetails(mangaId)
        } catch (e: Exception) {
            errorMessage = "Erreur lors du chargement des détails."
        } finally {
            isLoading = false
        }
    }

    if (isLoading) {
        CircularProgressIndicator()
    } else if (errorMessage != null) {
        Text(text = errorMessage!!, color = Color.Red)
    } else {
        manga?.let {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = it.title, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Image(
                    painter = rememberImagePainter(it.images.jpg.image_url),
                    contentDescription = it.title,
                    modifier = Modifier.fillMaxWidth().height(200.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = it.synopsis, color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { navController.popBackStack() }) {
                    Text("Back")
                }
            }
        } ?: run {
            Text(text = "Manga non trouvé", color = Color.Red)
        }
    }
}

// API Service
object AnimeApiService {
    private val client = OkHttpClient()

    suspend fun getTopManga(): List<Manga> {
        val url = "https://api.jikan.moe/v4/top/manga?sfw=true"
        val request = Request.Builder().url(url).build()

        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    println("Unexpected code $response")
                    return@withContext emptyList()
                }

                val responseBody = response.body?.string()
                responseBody?.let {
                    val gson = Gson()
                    val mangaResponse = gson.fromJson(it, MangaResponse::class.java)
                    return@withContext mangaResponse.data
                }
                emptyList()
            }
        }
    }

    suspend fun getMangaDetails(mangaId: Int): Manga? {
        val url = "https://api.jikan.moe/v4/manga/$mangaId"
        val request = Request.Builder().url(url).build()

        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    println("Unexpected code $response")
                    return@withContext null
                }

                val responseBody = response.body?.string()
                responseBody?.let {
                    val gson = Gson()
                    val mangaResponse = gson.fromJson(it, MangaDetailsResponse::class.java)
                    return@withContext mangaResponse.data
                }
                null
            }
        }
    }
}

// Data classes
data class MangaResponse(val data: List<Manga>)
data class MangaDetailsResponse(val data: Manga)
data class Manga(
    val mal_id: Int,
    val url: String,
    val images: Images,
    val title: String,
    val type: String,
    val status: String,
    val score: Double,
    val synopsis: String
)
data class Images(val jpg: ImageUrls)
data class ImageUrls(val image_url: String)

// Header and Footer Composables
@Composable
fun Header() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF011724))
            .padding(8.dp)
    ) {
        Text("Bibliothèque", color = Color(0xFFD4F5F5), style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Text("Last update", color = Color(0xFFD4F5F5))
            Text("Most follow", color = Color(0xFFD4F5F5))
            Text("New manga", color = Color(0xFFD4F5F5))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Divider(color = Color.White, thickness = 2.dp)
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun Footer(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF011724))
            .padding(8.dp)
    ) {
        Divider(color = Color.White, thickness = 2.dp)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            FooterButton(iconRes = R.drawable.ic_home, onClick = { navController.navigate("grid") })
            FooterButton(iconRes = R.drawable.ic_follow, onClick = { /* Handle Follow click */ })
            FooterButton(iconRes = R.drawable.ic_search, onClick = { /* Handle Search click */ })
            FooterButton(iconRes = R.drawable.ic_login, onClick = { /* Handle Login click */ })
        }
    }
}

@Composable
fun FooterButton(iconRes: Int, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = rememberImagePainter(iconRes),
            contentDescription = null,
            modifier = Modifier
                .size(24.dp)
                .clickable { onClick() }
        )
        Spacer(modifier = Modifier.height(4.dp))
    }
}
