package com.example.webhon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.*
import com.example.webhon.ui.theme.WebhonTheme
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.clickable
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WebhonTheme {
                // Set up Navigation
                val navController = rememberNavController()
                NavHost(navController, startDestination = "grid") {
                    composable("grid") { RectangleGrid(navController) }
                    composable("details/{itemId}") { backStackEntry ->
                        val itemId = backStackEntry.arguments?.getString("itemId")
                        if (itemId != null) {
                            EmptyPage(itemId, navController)
                        }
                    }
                }
            }
        }
        fetchTopAnimeData()
    }
}

private fun fetchTopAnimeData() {
    // Launch a coroutine on the Main thread
    CoroutineScope(Dispatchers.Main).launch {
        // Call the API using our service
        val topAnimeData = AnimeApiService.getTopAnime()

        // Log or update the UI with the result
        if (topAnimeData != null) {
            // Log the entire JSON response
            Log.d("MainActivity", "Top Anime: $topAnimeData")

            // You can also parse and use specific data here as needed
        } else {
            Log.d("MainActivity", "Failed to fetch data")
        }
    }
}

@Composable
fun RectangleGrid(navController: NavController, modifier: Modifier = Modifier) {
    val items = List(51) { "$it" } // Adjust the number of items as needed

    LazyVerticalGrid(
        columns = GridCells.Fixed(3), // Number of columns
        contentPadding = PaddingValues(8.dp),
        modifier = modifier
            .fillMaxSize()
    ) {
        items(items) { name ->
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .height(100.dp)
                    .fillMaxWidth()
                    .background(Color.Gray)
                    .clickable {
                        navController.navigate("details/$name")
                    }
            )
        }
    }
}

@Composable
fun EmptyPage(itemId: String, navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Empty Page
        Text(
            text = "This is an empty page for item $itemId",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { navController.popBackStack() }) {
            Text("Back to List")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RectangleGridPreview() {
    WebhonTheme {
        val navController = rememberNavController()
        NavHost(navController, startDestination = "grid") {
            composable("grid") { RectangleGrid(navController) }
            composable("details/{itemId}") { backStackEntry ->
                val itemId = backStackEntry.arguments?.getString("itemId")
                if (itemId != null) {
                    EmptyPage(itemId, navController)
                }
            }
        }
    }
}
