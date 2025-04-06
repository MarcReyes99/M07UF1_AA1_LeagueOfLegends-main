package com.example.appcompanion
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.appcompanion.ui.theme.CompanionAppTheme
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database



@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        setContent {
            CompanionAppTheme {
                var userState by remember { mutableStateOf(UserState()) }
                var currentScreen by remember { mutableStateOf(Screen.Login) }
                var userId by remember { mutableStateOf("") }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when(currentScreen) {
                        Screen.Login -> LoginScreen(
                            onLogin = { id ->
                                userState = UserState(isLoggedIn = true, username = id)
                                userId = id // Actualiza el userId
                                currentScreen = Screen.Home
                            },
                            onGuestLogin = {
                                userState = UserState(isLoggedIn = false, isGuest = true)
                                currentScreen = Screen.Home
                            },
                            onRegister = {

                                currentScreen = Screen.Register}
                        )
                        Screen.Home -> HomeScreen(
                            userState = userState,
                            onLogout = {
                                userState = UserState()
                                currentScreen = Screen.Login
                            },
                            onNavigateTo = { screen -> currentScreen = screen }
                        )
                        Screen.Guides -> GuidesScreen(userState = userState,onNavigateBack = { currentScreen = Screen.Home },onNavigateTo = { screen -> currentScreen = screen })
                        Screen.Profile -> ProfileScreen( userId = userId,onNavigateBack = { currentScreen = Screen.Home }, onNavigateTo = {screen -> currentScreen = screen})
                        Screen.Register -> RegisterScreen(
                            onRegisterComplete = { id ->
                                userState = UserState(isLoggedIn = true, username = id)
                                userId = id
                                currentScreen = Screen.Home
                            },
                            onNavigateBack = { currentScreen = Screen.Login }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RegisterScreen(
    onRegisterComplete: (String) -> Unit, // Callback cuando se complete el registro
    onNavigateBack: () -> Unit // Para volver a la pantalla anterior (Login)
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var profileImage by remember { mutableStateOf("") }
    val inventory = remember { mutableStateListOf<String>() }
    var inventoryItem by remember { mutableStateOf("") }

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val database = Firebase.database.reference

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text("Register", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = profileImage,
            onValueChange = { profileImage = it },
            label = { Text("Profile Image URL") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inventoryItem,
                onValueChange = { inventoryItem = it },
                label = { Text("Inventory Item") },
                modifier = Modifier.weight(1f)
            )

            Button(
                onClick = {
                    if (inventoryItem.isNotBlank()) {
                        inventory.add(inventoryItem) // Agregar al inventario
                        inventoryItem = "" // Limpiar campo
                    }
                }
            ) {
                Text("Add Item")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text("Inventory: ${inventory.joinToString()}", style = MaterialTheme.typography.bodySmall)

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val cleanEmail = email.trim() // Eliminar espacios en blanco

                if (cleanEmail.isBlank() || password.isBlank() || name.isBlank() || description.isBlank() || profileImage.isBlank()) {
                    Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
                    Toast.makeText(context, "Invalid email format", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                // Crear usuario en Firebase Authentication
                auth.createUserWithEmailAndPassword(cleanEmail, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val userId = task.result?.user?.uid ?: ""

                            // Guardar información en Firebase Database
                            val userData = mapOf(
                                "name" to name,
                                "description" to description,
                                "profileImage" to profileImage,
                                "inventory" to inventory,
                                "email" to cleanEmail,
                                "hoursPlayed" to 0
                            )

                            database.child("users").child(userId).setValue(userData)
                                .addOnCompleteListener { dbTask ->
                                    if (dbTask.isSuccessful) {
                                        Toast.makeText(context, "Registration Successful", Toast.LENGTH_SHORT).show()
                                        onRegisterComplete(userId)
                                    } else {
                                        Toast.makeText(context, "Failed to save profile", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        } else {
                            val errorMessage = when (task.exception) {
                                is FirebaseAuthWeakPasswordException -> "Weak password, use at least 6 characters"
                                is FirebaseAuthInvalidCredentialsException -> "Invalid email format"
                                is FirebaseAuthUserCollisionException -> "Email already in use"
                                else -> "Registration failed: ${task.exception?.message}"
                            }
                            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                        }
                    }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Register")
        }


        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onNavigateBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }
    }
}

@Composable
fun LoginScreen(
    onLogin: (String) -> Unit,
    onGuestLogin: () -> Unit,
    onRegister: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current
    val auth = Firebase.auth
    val database = Firebase.database.reference // Referencia a la base de datos
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Background Image
        AsyncImage(
            model = "https://i.pinimg.com/736x/79/66/d6/7966d61818891943332cb63b067dd8d0.jpg", // Replace with your image URL
            contentDescription = "Background Image",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Card(
                modifier = Modifier

                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(12.dp), // Rounded corners
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface // Secondary color
                )
            ) {
                Box(
                    modifier = Modifier

                        .padding(16.dp),
                    contentAlignment = Alignment.Center // Center the title
                ) {
                    Text(
                        text = "Login",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(12.dp), // Rounded corners
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondary // Secondary color
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Email Field
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),


                    )

                    // Password Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Login Button
                    Button(
                        onClick = {
                            if (email.isBlank() || password.isBlank()) {
                                Toast.makeText(
                                    context,
                                    "Please fill in all fields",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@Button
                            }

                            auth.signInWithEmailAndPassword(email, password)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        val userId = task.result?.user?.uid ?: ""
                                        onLogin(userId)
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Login failed: ${task.exception?.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                        },
                        modifier = Modifier.fillMaxWidth(0.6f)
                    ) {
                        Text("Login")
                    }
                }
            }

            Button(
                onClick = onRegister,
                modifier = Modifier.fillMaxWidth(0.6f).padding(bottom = 8.dp)
            ) {
                Text("Register")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { onGuestLogin() },
                modifier = Modifier.fillMaxWidth(0.6f)
            ) {
                Text("Continue as Guest")
            }
        }
    }
}



    @Composable
    fun HomeScreen(userState: UserState, onLogout: () -> Unit,onNavigateTo: (Screen) -> Unit) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // Contenido principal en el centro
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(MaterialTheme.colorScheme.primary)
                    .align(Alignment.TopCenter),

            ) {
                Text(
                    text = "Home",
                    modifier = Modifier.align(Alignment.Center), // Espaciado superior
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 64.dp) // Espaciado bajo el título
                    .align(Alignment.TopCenter)

            ) {
                GuidesPreviewCard { onNavigateTo(Screen.Guides) }
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp), // Adjust height as needed
                    shape = RoundedCornerShape(8.dp), // Rounded corners
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),

                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {

                            Text("Patch Notes", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed tristique tincidunt mauris, in placerat sapien placerat et.Maecenas convallis tortor ac elementum faucibus. Suspendisse accumsan, lacus a ullamcorper posuere, risus metus lobortis mauris, in sollicitudin sem nisi eget turpis.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimary)
                        }


                    }
                }
            }

            // Barra de navegación en la parte inferior
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp) // Standard bottom bar height
                    .background(MaterialTheme.colorScheme.primary) // Bottom bar color
                    .align(Alignment.BottomCenter) // Alinea el Row al fondo del Box
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly, // Espaciado uniforme entre botones
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = { onNavigateTo(Screen.Guides) }
                   , colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary, // Background color
                        contentColor = MaterialTheme.colorScheme.onPrimary // Text color
                    )) {
                    Text("Guides")
                }
                IconButton(onClick = { onNavigateTo(Screen.Home) }) {
                    Icon(
                        imageVector = Icons.Default.Home, // Replace with your icon
                        contentDescription = "Home",
                        tint = MaterialTheme.colorScheme.onPrimary // Icon color
                    )
                }
                if (userState.isLoggedIn) {
                    Button(onClick = { onNavigateTo(Screen.Profile) },colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary, // Background color
                        contentColor = MaterialTheme.colorScheme.onPrimary // Text color
                    )) {
                        Text("Profile")
                    }
                } else {
                    Button(onClick = { onNavigateTo(Screen.Login) },colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary, // Background color
                        contentColor = MaterialTheme.colorScheme.onPrimary // Text color
                    )) {
                        Text("Login")
                    }
                }
            }
        }
    }

@Composable
fun GuidesPreviewCard(onNavigateToGuides: () -> Unit) {
    val database = FirebaseDatabase.getInstance().getReference("guides")
    val guidesList = remember { mutableStateListOf<String>() }

    // Obtener solo 3 guías como previsualización
    LaunchedEffect(Unit) {
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                guidesList.clear()
                for (guideSnapshot in snapshot.children.take(3)) {  // Solo tomamos 3 guías
                    val title = guideSnapshot.child("title").getValue(String::class.java) ?: "No title"
                    guidesList.add(title)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error al obtener datos: ${error.message}")
            }
        })
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onNavigateToGuides() }
        ,  // Navegar a Guides cuando se haga clic
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Game Guides", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimary)
            Spacer(modifier = Modifier.height(8.dp))

            if (guidesList.isEmpty()) {
                Text("No guides available", style = MaterialTheme.typography.bodyMedium,color = MaterialTheme.colorScheme.onPrimary)
            } else {
                guidesList.forEach { title ->
                    Text("- $title", style = MaterialTheme.typography.bodySmall,color = MaterialTheme.colorScheme.onPrimary)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text("Tap to see all guides", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

@Composable
fun GuidesScreen(userState: UserState,onNavigateBack: () -> Unit,onNavigateTo: (Screen) -> Unit) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background) // Set background color
        ) {
            // Top Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp) // Standard top bar height
                    .background(MaterialTheme.colorScheme.primary) // Top bar color
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {

                Text(
                    text = "Guides",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 40.dp), // Center text with padding
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimary // Text color for contrast
                )
            }

            // Guide List
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Here are some guides:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                ListOfGuides()
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()

                    .padding(top = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp) // Standard bottom bar height
                        .align(Alignment.BottomCenter)
                        .background(MaterialTheme.colorScheme.primary) // Bottom bar color
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly, // Espaciado uniforme entre botones
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { onNavigateTo(Screen.Guides) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary, // Background color
                            contentColor = MaterialTheme.colorScheme.onPrimary // Text color
                        )
                    ) {
                        Text("Guides")
                    }
                    IconButton(onClick = { onNavigateTo(Screen.Home) }) {
                        Icon(
                            imageVector = Icons.Default.Home, // Replace with your icon
                            contentDescription = "Home",
                            tint = MaterialTheme.colorScheme.onPrimary // Icon color
                        )
                    }
                    if (userState.isLoggedIn) {
                        Button(
                            onClick = { onNavigateTo(Screen.Profile) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary, // Background color
                                contentColor = MaterialTheme.colorScheme.onPrimary // Text color
                            )
                        ) {
                            Text("Profile")
                        }
                    } else {
                        Button(
                            onClick = { onNavigateTo(Screen.Login) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary, // Background color
                                contentColor = MaterialTheme.colorScheme.onPrimary // Text color
                            )
                        ) {
                            Text("Login")
                        }
                    }
                }
            }
        }

    }


@Composable
fun ListOfGuides() {
    val database = FirebaseDatabase.getInstance().getReference("guides")
    val guidesList = remember { mutableStateListOf<Pair<String, String>>() }

    LaunchedEffect(Unit) {
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                guidesList.clear()
                for (guideSnapshot in snapshot.children) {
                    val title = guideSnapshot.child("title").getValue(String::class.java) ?: "No title"
                    val description = guideSnapshot.child("description").getValue(String::class.java) ?: "No description"
                    guidesList.add(title to description)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error al obtener datos: ${error.message}")
            }
        })
    }

    Column {
        if (guidesList.isEmpty()) {
            Text(
                text = "No guides available",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            guidesList.forEach { (title, description) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(8.dp), // Rounded corners
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface // Card background color
                    ),
                    elevation = CardDefaults.cardElevation(4.dp) // Card elevation
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimary // Title color
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary // Description color
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileScreen(userId: String, onNavigateBack: () -> Unit, onNavigateTo: (Screen) -> Unit) {
    val database = FirebaseDatabase.getInstance().getReference("users").child(userId)
    val userName = remember { mutableStateOf("Loading...") }
    val description = remember { mutableStateOf("") }
    val hoursPlayed = remember { mutableStateOf(0) }
    val inventory = remember { mutableStateListOf<String>() }
    val profileImage = remember { mutableStateOf("") } // URL de la imagen del perfil

    // Obtener los datos del usuario desde Firebase
    LaunchedEffect(userId) {
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userName.value = snapshot.child("name").getValue(String::class.java) ?: "Unknown"
                description.value = snapshot.child("description").getValue(String::class.java) ?: "No description available"
                hoursPlayed.value = snapshot.child("hoursPlayed").getValue(Int::class.java) ?: 0
                profileImage.value = snapshot.child("profileImage").getValue(String::class.java) ?: ""
                snapshot.child("inventory").children.forEach { item ->
                    inventory.add(item.getValue(String::class.java) ?: "Unknown item")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error al obtener datos: ${error.message}")
            }
        })
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top // Para que los elementos no queden tan separados
        ) {
            // Imagen del perfil e información básica
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Imagen del perfil
                AsyncImage(
                    model = profileImage.value,
                    contentDescription = "Profile Image",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.onBackground, CircleShape)
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Solo el nombre aquí
                Text(
                    text = "Hi, ${userName.value}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Información adicional del usuario
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Info", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Descripción dentro de "Info"
                    Text(description.value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Inventory:", color = MaterialTheme.colorScheme.onSurface)
                    inventory.forEach { item ->
                        Text("- $item", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            // Horas jugadas
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Hours Played", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("$hoursPlayed horas", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                }
            }

            // Espacio para empujar los botones hacia abajo
            Spacer(modifier = Modifier.weight(1f))

            // Botones de navegación manuales
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = { onNavigateTo(Screen.Guides) }) {
                    Text("Guides")
                }
                Button(onClick = { onNavigateTo(Screen.Home) }) {
                    Text("Home")
                }
            }
        }
    }
}