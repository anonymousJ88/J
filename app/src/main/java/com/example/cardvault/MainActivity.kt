package com.example.cardvault

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

@Serializable
enum class VaultType {
    CARD,
    CERTIFICATION,
    TITLE
}

@Serializable
data class VaultItem(
    val id: String,
    val name: String,
    val type: VaultType,
    val issuer: String,
    val issueDate: String,
    val details: String,
    val comments: String,
    val digitalVersion: String,
    val photoPath: String,
    val createdAt: Long
)

class VaultRepository(private val rootDir: File) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val dataFile = File(rootDir, "vault_items.json")

    fun loadAll(): List<VaultItem> {
        if (!dataFile.exists()) return emptyList()
        return runCatching {
            json.decodeFromString<List<VaultItem>>(dataFile.readText())
        }.getOrDefault(emptyList())
    }

    fun saveAll(items: List<VaultItem>) {
        dataFile.writeText(json.encodeToString(items))
    }

    fun add(item: VaultItem) {
        val items = loadAll().toMutableList()
        items.add(0, item)
        saveAll(items)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = VaultRepository(filesDir)
        setContent {
            MaterialTheme {
                CardVaultApp(repository)
            }
        }
    }
}

@Composable
fun CardVaultApp(repository: VaultRepository) {
    val navController = rememberNavController()
    var items by remember { mutableStateOf(repository.loadAll()) }

    NavHost(navController = navController, startDestination = "list") {
        composable("list") {
            VaultListScreen(items = items, onAdd = { navController.navigate("add") }) {
                navController.navigate("detail/${it.id}")
            }
        }
        composable("add") {
            AddItemScreen(
                onBack = { navController.popBackStack() },
                onSave = { item ->
                    repository.add(item)
                    items = repository.loadAll()
                    navController.popBackStack()
                }
            )
        }
        composable(
            route = "detail/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id")
            val item = items.firstOrNull { it.id == id }
            item?.let {
                DetailScreen(item = it, onBack = { navController.popBackStack() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultListScreen(items: List<VaultItem>, onAdd: () -> Unit, onOpen: (VaultItem) -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Card Vault") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Image(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { padding ->
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No items yet. Tap + to add your first card/certification/title.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items) { item ->
                    Card(modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpen(item) }) {
                        Row(modifier = Modifier.padding(12.dp)) {
                            AsyncImage(
                                model = File(item.photoPath),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(item.name, fontWeight = FontWeight.Bold)
                                Text(item.type.name)
                                Text("Issuer: ${item.issuer}")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemScreen(onBack: () -> Unit, onSave: (VaultItem) -> Unit) {
    val context = LocalContext.current
    val imagesDir = remember {
        File(context.filesDir, "photos").apply { mkdirs() }
    }
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(VaultType.CARD) }
    var issuer by remember { mutableStateOf("") }
    var issueDate by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    var comments by remember { mutableStateOf("") }
    var digitalVersion by remember { mutableStateOf("") }
    var photoPath by remember { mutableStateOf("") }
    var photoPreview by remember { mutableStateOf<Bitmap?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            val file = File(imagesDir, "${UUID.randomUUID()}.jpg")
            file.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            photoPath = file.absolutePath
            photoPreview = bitmap
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Add Document") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(onClick = { launcher.launch(null) }) {
                Text(if (photoPath.isEmpty()) "Take Photo" else "Retake Photo")
            }
            photoPreview?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
            TypePicker(selected = type, onSelected = { type = it })
            OutlinedTextField(value = issuer, onValueChange = { issuer = it }, label = { Text("Issuer / Organization") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = issueDate, onValueChange = { issueDate = it }, label = { Text("Issue Date") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = details, onValueChange = { details = it }, label = { Text("Detailed information") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
            OutlinedTextField(value = digitalVersion, onValueChange = { digitalVersion = it }, label = { Text("Digital version (typed text)") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
            OutlinedTextField(value = comments, onValueChange = { comments = it }, label = { Text("Comments") }, modifier = Modifier.fillMaxWidth(), minLines = 2)

            Button(
                onClick = {
                    if (name.isNotBlank() && photoPath.isNotEmpty()) {
                        onSave(
                            VaultItem(
                                id = UUID.randomUUID().toString(),
                                name = name,
                                type = type,
                                issuer = issuer,
                                issueDate = issueDate,
                                details = details,
                                comments = comments,
                                digitalVersion = digitalVersion,
                                photoPath = photoPath,
                                createdAt = System.currentTimeMillis()
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
            Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel")
            }
        }
    }
}

@Composable
fun TypePicker(selected: VaultType, onSelected: (VaultType) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        VaultType.entries.forEach { entry ->
            val selectedBg = if (entry == selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(selectedBg)
                    .clickable { onSelected(entry) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(entry.name.lowercase().replaceFirstChar { it.uppercase() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(item: VaultItem, onBack: () -> Unit) {
    val bitmap = remember(item.photoPath) {
        runCatching { BitmapFactory.decodeFile(item.photoPath) }.getOrNull()
    }

    Scaffold(topBar = { TopAppBar(title = { Text(item.name) }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            bitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            Text("Type: ${item.type}", fontWeight = FontWeight.SemiBold)
            Text("Issuer: ${item.issuer}")
            Text("Issue date: ${item.issueDate}")
            Text("Photo path: ${item.photoPath}")
            Text("Digital version", fontWeight = FontWeight.Bold)
            Text(item.digitalVersion.ifBlank { "No digital version provided." })
            Text("Detailed info", fontWeight = FontWeight.Bold)
            Text(item.details.ifBlank { "No details provided." })
            Text("Comments", fontWeight = FontWeight.Bold)
            Text(item.comments.ifBlank { "No comments provided." })

            Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Back")
            }
        }
    }
}
