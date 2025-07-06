package com.example.localpokedex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LocalPokedexTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val pokemon = remember { mutableStateOf<List<Pokemon>>(emptyList()) }
                    
                    LaunchedEffect(Unit) {
                        pokemon.value = loadPokemonData()
                    }
                    
                    PokemonList(
                        pokemons = pokemon.value,
                        modifier = Modifier.padding(innerPadding)
                    )
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
fun PokemonList(pokemons: List<Pokemon>, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(pokemons) { pokemon ->
            PokemonCard(pokemon = pokemon)
        }
    }
}

@Composable
fun PokemonCard(pokemon: Pokemon, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatColumn("HP", pokemon.base.HP)
                StatColumn("ATK", pokemon.base.Attack)
                StatColumn("DEF", pokemon.base.Defense)
                StatColumn("SP.ATK", pokemon.base.SpAttack)
                StatColumn("SP.DEF", pokemon.base.SpDefense)
                StatColumn("SPD", pokemon.base.Speed)
            }
        }
    }
}

@Composable
fun StatColumn(label: String, value: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}