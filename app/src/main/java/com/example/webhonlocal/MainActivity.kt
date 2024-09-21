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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

// Main Activity
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApp()
        }
    }
}

@Composable
fun MyApp() {
    val navController = rememberNavController()
    var mangaList by remember { mutableStateOf<List<Manga>>(emptyList()) }

    LaunchedEffect(Unit) {
        mangaList = AnimeApiService.getTopManga()
    }

    Scaffold(
        topBar = { Header(navController) },
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
                composable("recent_updates") { RectangleGrid(navController, mangaList) }
                composable("most_followed") { MostFollowedScreen(navController) }
                composable("new_manga") { NewMangaScreen(navController) }
                composable("search") { SearchScreen(navController) }
                composable("settings") { SettingsScreen(navController) }
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

// Manga Details Screen
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

    suspend fun getRecommendedManga(): List<RecommendedManga> {
        val url = "https://api.jikan.moe/v4/recommendations/manga"
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
                    val recommendationResponse = gson.fromJson(it, RecommendationResponse::class.java)
                    // Extraire les données des entrées
                    return@withContext recommendationResponse.data.flatMap { rec ->
                        rec.entry.map { entry ->
                            RecommendedManga(
                                mal_id = entry.mal_id,
                                title = entry.title,
                                images = entry.images
                            )
                        }
                    }
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
data class RecommendationResponse(
    val data: List<Recommendation>
)
data class MangaResponse(val data: List<Manga>)
data class Recommendation(
    val mal_id: String,
    val entry: List<Entry>,
    val content: String,
    val date: String,
    val user: User
)
data class RecommendedManga(
    val mal_id: Int,
    val title: String,
    val images: Images
)
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

data class User(
    val url: String,
    val username: String
)

data class Entry(
    val mal_id: Int,
    val url: String,
    val images: Images,
    val title: String
)

// Most Followed Screen
@Composable
fun MostFollowedScreen(navController: NavController) {
    var recommendedMangaList by remember { mutableStateOf<List<RecommendedManga>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            recommendedMangaList = AnimeApiService.getRecommendedManga()
        } catch (e: Exception) {
            errorMessage = "Erreur lors du chargement des recommandations."
        } finally {
            isLoading = false
        }
    }

    if (isLoading) {
        CircularProgressIndicator()
    } else if (errorMessage != null) {
        Text(text = errorMessage!!, color = Color.Red)
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(4.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(recommendedMangaList) { manga ->
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
}

// New Manga Screen
@Composable
fun NewMangaScreen(navController: NavController) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("New Manga Releases", color = Color.White)
    }
}

// Search Screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(navController: NavController) {
    var query by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(Color(0xFF011724)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Search Manga",
            color = Color.White,
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Enter manga title") },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedLabelColor = Color.White,
                unfocusedLabelColor = Color.Gray,
                cursorColor = Color.White
            )
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                // Logique de recherche ici
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Search")
        }
    }
}

// Header Composable
@Composable
fun Header(navController: NavController) {
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
            Text(
                "Last update",
                color = Color(0xFFD4F5F5),
                modifier = Modifier.clickable {
                    navController.navigate("recent_updates")
                }
            )
            Text(
                "Most follow",
                color = Color(0xFFD4F5F5),
                modifier = Modifier.clickable {
                    navController.navigate("most_followed")
                }
            )
            Text(
                "New manga",
                color = Color(0xFFD4F5F5),
                modifier = Modifier.clickable {
                    navController.navigate("new_manga")
                }
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Divider(color = Color.White, thickness = 2.dp)
        Spacer(modifier = Modifier.height(8.dp))
    }
}

// Footer Composable
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
            FooterButton(iconRes = R.drawable.ic_search, onClick = { navController.navigate("search") })
            FooterButton(iconRes = R.drawable.ic_login, onClick = { navController.navigate("settings") })
        }
    }
}

// Footer Button Composable
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
    }
}

// Settings Screen Composable
@Composable
fun SettingsScreen(navController: NavController) {
    var notificationsEnabled by remember { mutableStateOf(true) }
    var darkModeEnabled by remember { mutableStateOf(false) }
    var languageSelected by remember { mutableStateOf("English") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(Color(0xFF011724)),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Settings",
            color = Color.White,
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))

        SettingItem(
            label = "Enable Notifications",
            isChecked = notificationsEnabled,
            onCheckedChange = { notificationsEnabled = it }
        )
        SettingItem(
            label = "Dark Mode",
            isChecked = darkModeEnabled,
            onCheckedChange = { darkModeEnabled = it }
        )
        LanguageSettingItem(
            selectedLanguage = languageSelected,
            onLanguageChange = { languageSelected = it }
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                // Logique pour sauvegarder les paramètres
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Settings")
        }
    }
}

// Setting Item Composable
@Composable
fun SettingItem(label: String, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onCheckedChange(!isChecked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.Green, uncheckedThumbColor = Color.Red)
        )
    }
}

// Language Setting Item Composable
@Composable
fun LanguageSettingItem(selectedLanguage: String, onLanguageChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Button(
            onClick = { onLanguageChange("English") },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (selectedLanguage == "English") Color.Green else Color.Gray
            )
        ) {
            Text("English", color = Color.White)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = { onLanguageChange("French") },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (selectedLanguage == "French") Color.Green else Color.Gray
            )
        ) {
            Text("French", color = Color.White)
        }
    }
}
