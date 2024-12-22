package com.example.deathstar_v001

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.rememberAsyncImagePainter
import coil.request.CachePolicy
import com.android.volley.toolbox.ImageRequest
import com.example.deathstar_v001.ui.theme.DeathStar_v0Theme
import androidx.compose.foundation.Image
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.painterResource
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DeathStar_v0Theme {

                val navController = rememberNavController()
                val towerStateHandler  = TowerStateHandler(this)
                Navigation(towerStateHandler)
            }
        }
    }
}



@Composable
fun Navigation(towerStateHandler: TowerStateHandler) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "game/{initialLevel}") {
        composable(
            route = "game/{initialLevel}",
            arguments = listOf(navArgument("initialLevel") { type = NavType.StringType })
        ) { backStackEntry ->
            val initialLevel = backStackEntry.arguments?.getString("initialLevel")
            GameScreen(navController, towerStateHandler, initialLevel)
        }
        composable("room_selection") {
            RoomSelectionScreen(navController)
        }
    }

    // Navigate to "game" with initialLevel on app start
    LaunchedEffect(key1 = Unit) {
        navController.navigate("game/STARTING") // Replace "level1" with your desired initial level
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    navController: NavHostController,
    towerStateHandler: TowerStateHandler,
    initialLevel: String?
) {
    val gifPainter = rememberAsyncImagePainter(
        model = R.drawable.animation, // Replace with your GIF resource
        contentScale = ContentScale.Crop // Adjust content scale as needed
    )

    var towerLevels by remember { mutableStateOf<List<LevelType>>(emptyList()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(key1 = initialLevel){
        if(initialLevel != null) {
            towerStateHandler.addRoom(initialLevel?.let { LevelType.valueOf(it) }
                ?: LevelType.STARTING)
        }
        towerLevels = towerStateHandler.towerLevels
        towerStateHandler.saveTowerProgress(towerLevels)
    }
    Log.d("Rofl", "towerlevels: $towerLevels")


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent) // Set a transparent background
    ) {
        Image(
            painter = gifPainter,
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.FillBounds // Adjust content scale as needed

        )

        var showMenu by remember { mutableStateOf(false) }
        val scrollState = rememberScrollState()
        val configuration = LocalConfiguration.current

        Scaffold(
        ) {
            innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Color.Transparent)
                    .padding(horizontal = 0.dp, vertical = 16.dp), // Remove horizontal padding
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val buttonHeight = 48.dp // Approximate button height
                val menuHeight = if (showMenu) 200.dp else 0.dp // Approximate menu height

                val availableHeight =
                    configuration.screenHeightDp.dp - innerPadding.calculateTopPadding() -
                            innerPadding.calculateBottomPadding() - 16.dp * 2 - buttonHeight - menuHeight

                Button(onClick = { navController.navigate("room_selection") }) {
                    Text("Build New Floor")
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    LevelType.values().filterNot { it == LevelType.STARTING }.forEach { levelType ->
                        DropdownMenuItem(
                            text = { Text(levelType.displayName) },
                            onClick = {
                                navController.navigate("room_construction/${levelType.name}") // Navigate to room construction screen
                                showMenu = false
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier
                    .height(16.dp)
                    .background(Color.Transparent))


                // Tower visualization with screen-height scrolling
                Box(
                    modifier = Modifier
                        .fillMaxSize() // Fill the entire screen
                        .height(availableHeight)
                        .verticalScroll(scrollState)
                        .background(Color.Transparent)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize() // Fill the entire screen
                            .height(IntrinsicSize.Min)
                            .background(Color.Transparent),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Lift
                        Box(
                            modifier = Modifier
                                .width(20.dp)
                                .height(towerLevels.size * 50.dp)
                                .background(Color.Gray)
                        )

                        // Rooms
                        Column(modifier = Modifier.weight(1f)) {
                            towerLevels.forEach { levelType ->
                                Room(levelType, Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            }
        }
    }

}

@Composable
fun Room(levelType: LevelType, modifier: Modifier = Modifier) {
    val color = when (levelType) {
        LevelType.STARTING -> Color.Green
        LevelType.LIVING -> Color.LightGray
        LevelType.FOOD -> Color.Yellow
        LevelType.PRODUCTION -> Color.Cyan
        LevelType.IMPERIAL -> Color.Red
    }



    Box(
        modifier = modifier
            .height(50.dp)
            .background(color)
            .border(1.dp, Color.Black)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomSelectionScreen(
    navController: NavHostController
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Room Type") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) { // Cancel button
                        Icon(Icons.Filled.Close, contentDescription = "Cancel")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LevelType.values().filterNot { it == LevelType.STARTING }.forEach { levelType ->
                Button(onClick = { navController.navigate("game/$levelType") }) { // Pass initialLevel here
                    Text("${levelType.displayName}")
                }
            }
        }
    }
}



val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tower_progress")

class TowerStateHandler(private val context: Context) {
    var towerLevels by mutableStateOf<List<LevelType>>(emptyList())
        private set

    suspend fun addRoom(levelType: LevelType) {
        towerLevels = loadTowerProgress()
        if(towerLevels.size > 0 && levelType == LevelType.STARTING){}
        else{
            towerLevels = when (levelType) {
                LevelType.IMPERIAL -> towerLevels + levelType
                else -> listOf(levelType) + towerLevels
            }
        }
        saveTowerProgress(towerLevels)
    }

    // Serialization and deserialization functions
    private fun serializeTowerLevels(towerLevels: List<LevelType>): String {
        return towerLevels.joinToString(",") { it.name }
    }

    private fun deserializeTowerLevels(towerLevelsString: String): List<LevelType> {
        return towerLevelsString.split(",").mapNotNull { LevelType.valueOf(it) }
    }

    // Save and load functions
    suspend fun saveTowerProgress(towerLevels: List<LevelType>) {
        context.dataStore.edit { preferences ->
            preferences[stringPreferencesKey("tower_levels")] = serializeTowerLevels(towerLevels)
        }
    }

    suspend fun loadTowerProgress(): List<LevelType> {
        val preferences = context.dataStore.data.first()
        val towerLevelsString = preferences[stringPreferencesKey("tower_levels")] ?: ""
        Log.d("Lolading", "$towerLevelsString")
        return deserializeTowerLevels(towerLevelsString)
    }

}

enum class LevelType(val displayName: String) {
    STARTING("Starting Room"), // Add starting room type
    LIVING("Living Quarters"),
    FOOD("Food Court"),
    PRODUCTION("Production Facility"),
    IMPERIAL("Secret Imperial Level")
}


