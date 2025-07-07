package com.example.localpokedex

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.localpokedex.ui.theme.LocalPokedexTheme
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PokemonName(
    val english: String,
    val japanese: String,
    val chinese: String,
    val french: String
)

data class PokemonStats(
    val HP: Int,
    val Attack: Int,
    val Defense: Int,
    @SerializedName("Sp. Attack")
    val SpAttack: Int,
    @SerializedName("Sp. Defense")
    val SpDefense: Int,
    val Speed: Int
)

data class Pokemon(
    val id: Int,
    val name: PokemonName,
    val type: List<String>,
    val base: PokemonStats
)

class MainActivity : ComponentActivity() {
    companion object {
        fun saveNote(context: Context, pokemonId: Int, note: String) {
            val sharedPref = context.getSharedPreferences("pokemon_notes", Context.MODE_PRIVATE)
            with(sharedPref.edit()) {
                putString("note_$pokemonId", note)
                apply()
            }
        }

        fun loadNote(context: Context, pokemonId: Int): String {
            val sharedPref = context.getSharedPreferences("pokemon_notes", Context.MODE_PRIVATE)
            return sharedPref.getString("note_$pokemonId", "") ?: ""
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LocalPokedexTheme {
                val pokemon = remember { mutableStateOf<List<Pokemon>>(emptyList()) }
                
                LaunchedEffect(Unit) {
                    pokemon.value = loadPokemonData()
                }

                val navController = rememberNavController()
                
                NavHost(
                    navController = navController,
                    startDestination = "pokemonList"
                ) {
                    composable("pokemonList") {
                        Scaffold(
                            topBar = {
                                TopAppBar(
                                    title = { Text("Pokédex") },
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        ) { innerPadding ->
                            PokemonList(
                                pokemons = pokemon.value,
                                modifier = Modifier.padding(innerPadding),
                                onPokemonClick = { pokemonId ->
                                    navController.navigate("pokemonDetail/$pokemonId")
                                }
                            )
                        }
                    }
                    composable(
                        route = "pokemonDetail/{pokemonId}",
                        arguments = listOf(navArgument("pokemonId") { type = NavType.IntType })
                    ) { backStackEntry ->
                        val pokemonId = backStackEntry.arguments?.getInt("pokemonId") ?: return@composable
                        val selectedPokemon = pokemon.value.find { it.id == pokemonId }
                        
                        if (selectedPokemon != null) {
                            Scaffold(
                                topBar = {
                                    TopAppBar(
                                        title = { Text(selectedPokemon.name.english) },
                                        navigationIcon = {
                                            IconButton(onClick = { navController.navigateUp() }) {
                                                Icon(Icons.Default.ArrowBack, "Back")
                                            }
                                        },
                                        colors = TopAppBarDefaults.topAppBarColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    )
                                }
                            ) { innerPadding ->
                                PokemonDetailScreen(
                                    pokemon = selectedPokemon,
                                    modifier = Modifier.padding(innerPadding)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun loadPokemonData(): List<Pokemon> = withContext(Dispatchers.IO) {
        val json = assets.open("pokemon.json-master/pokedex.json").bufferedReader().use { it.readText() }
        val type = object : TypeToken<List<Pokemon>>() {}.type
        return@withContext Gson().fromJson(json, type)
    }
}

@Composable
fun PokemonList(
    pokemons: List<Pokemon>,
    modifier: Modifier = Modifier,
    onPokemonClick: (Int) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(pokemons) { pokemon ->
            PokemonCard(
                pokemon = pokemon,
                onClick = { onPokemonClick(pokemon.id) }
            )
        }
    }
}

@Composable
fun PokemonCard(
    pokemon: Pokemon,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val imageFileName = String.format("%03d", pokemon.id)
            val painter = rememberAsyncImagePainter(
                ImageRequest.Builder(LocalContext.current)
                    .data("file:///android_asset/pokemon.json-master/thumbnails/$imageFileName.png")
                    .build()
            )
            
            Image(
                painter = painter,
                contentDescription = pokemon.name.english,
                modifier = Modifier.size(80.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = "#${String.format("%03d", pokemon.id)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = pokemon.name.english,
                    style = MaterialTheme.typography.titleLarge
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    pokemon.type.forEach { type ->
                        Text(
                            text = type,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokemonDetailScreen(
    pokemon: Pokemon,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var note by remember(pokemon.id) {
        mutableStateOf(MainActivity.loadNote(context, pokemon.id))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val imageFileName = String.format("%03d", pokemon.id)
        val painter = rememberAsyncImagePainter(
            ImageRequest.Builder(LocalContext.current)
                .data("file:///android_asset/pokemon.json-master/thumbnails/$imageFileName.png")
                .build()
        )
        
        Image(
            painter = painter,
            contentDescription = pokemon.name.english,
            modifier = Modifier.size(200.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "#${String.format("%03d", pokemon.id)} ${pokemon.name.english}",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            pokemon.type.forEach { type ->
                Text(
                    text = type,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Base Stats",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        StatBar("HP", pokemon.base.HP, MaterialTheme.colorScheme.primary)
        StatBar("Attack", pokemon.base.Attack, MaterialTheme.colorScheme.error)
        StatBar("Defense", pokemon.base.Defense, MaterialTheme.colorScheme.tertiary)
        StatBar("Sp. Attack", pokemon.base.SpAttack, MaterialTheme.colorScheme.secondary)
        StatBar("Sp. Defense", pokemon.base.SpDefense, MaterialTheme.colorScheme.tertiary)
        StatBar("Speed", pokemon.base.Speed, MaterialTheme.colorScheme.primary)

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Notes",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = note,
            onValueChange = { newNote ->
                note = newNote
                MainActivity.saveNote(context, pokemon.id, newNote)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            label = { Text("Add notes about ${pokemon.name.english}") },
            textStyle = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun StatBar(
    label: String,
    value: Int,
    color: Color,
    maxValue: Int = 255
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.width(100.dp)
            )
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.End,
                modifier = Modifier.width(50.dp)
            )
        }
        LinearProgressIndicator(
            progress = value.toFloat() / maxValue,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = color
        )
    }
}