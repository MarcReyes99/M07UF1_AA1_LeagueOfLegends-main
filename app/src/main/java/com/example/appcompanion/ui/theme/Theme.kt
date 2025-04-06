package com.example.appcompanion.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Define los colores basados en la imagen
private val LightColors = lightColorScheme(
    primary = Color(0xFFFFA726), // Naranja brillante
    onPrimary = Color.White,    // Texto sobre color primario
    secondary = Color(0xFF64B5F6), // Azul claro para botones secundarios
    onSecondary = Color.White, // Texto sobre color secundario
    background = Color(0xFFFFF3E0), // Beige claro (color de fondo)
    surface = Color(0xFFFFCC80),   // Amarillo/naranja suave para elementos de superficie
    onBackground = Color(0xFF000000), // Texto sobre fondo claro
    onSurface = Color.Black         // Texto sobre superficie
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF7043), // Naranja oscuro
    onPrimary = Color.Black,    // Texto sobre color primario oscuro
    secondary = Color(0xFF4FC3F7), // Azul oscuro
    onSecondary = Color.Black, // Texto sobre color secundario oscuro
    background = Color(0xFF212121), // Gris oscuro para fondo
    surface = Color(0xFF37474F),   // Gris azulado para elementos de superficie
    onBackground = Color.White, // Texto sobre fondo oscuro
    onSurface = Color.White     // Texto sobre superficie oscura
)

// Función del tema personalizado
@Composable
fun CompanionAppTheme(
    useDarkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = if (useDarkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colors,
        typography = Typography(),
        content = content
    )
}
