package com.minimalgastos.app.data

import androidx.compose.ui.graphics.Color
import java.text.Normalizer

/** Categorías fijas. `keywords` alimentan la categorización automática. */
enum class Category(val label: String, val color: Color, val keywords: List<String>) {
    FOOD(
        "Comida", Color(0xFFE8833A),
        listOf(
            "comida", "almuerzo", "desayuno", "cena", "restaurante", "cafe", "pizza", "hamburguesa",
            "mercado", "supermercado", "exito", "carulla", "jumbo", "d1", "ara", "olimpica", "rappi",
            "domicilio", "panaderia", "pan", "helado", "bebida", "cerveza", "starbucks", "juan valdez",
            "mcdonalds", "kfc", "frisby", "crepes", "fruta", "verdura", "carne", "snack", "lunch",
            "dinner", "breakfast", "groceries", "coffee", "restaurant"
        )
    ),
    TRANSPORT(
        "Transporte", Color(0xFF3A86E8),
        listOf(
            "uber", "didi", "cabify", "indriver", "taxi", "bus", "metro", "transmilenio", "sitp",
            "tm", "gasolina", "combustible", "parqueadero", "peaje", "pasaje", "vuelo", "tiquete",
            "avianca", "latam", "moto", "bicicleta", "soat", "tecnomecanica", "gas", "fuel", "parking",
            "flight", "train"
        )
    ),
    BILLS(
        "Servicios", Color(0xFF8E5CE0),
        listOf(
            "luz", "agua", "internet", "celular", "telefono", "arriendo", "alquiler", "administracion",
            "energia", "gas natural", "factura", "claro", "movistar", "tigo", "wom", "etb", "epm",
            "seguro", "impuesto", "predial", "rent", "electricity", "water", "phone", "bill", "insurance"
        )
    ),
    ENTERTAINMENT(
        "Entretenimiento", Color(0xFFE05C8E),
        listOf(
            "netflix", "spotify", "cine", "pelicula", "concierto", "juego", "videojuego", "steam",
            "playstation", "xbox", "disney", "hbo", "max", "prime video", "youtube", "fiesta", "bar",
            "discoteca", "teatro", "evento", "boleta", "entrada", "salida", "movie", "game", "party"
        )
    ),
    HEALTH(
        "Salud", Color(0xFF3BB273),
        listOf(
            "medico", "doctor", "farmacia", "droguería", "drogueria", "medicina", "medicamento",
            "dentista", "odontologo", "hospital", "clinica", "eps", "gimnasio", "gym", "terapia",
            "psicologo", "examen", "laboratorio", "pharmacy", "medicine", "dentist"
        )
    ),
    SHOPPING(
        "Compras", Color(0xFFE0B23A),
        listOf(
            "ropa", "zapatos", "tenis", "camisa", "pantalon", "amazon", "mercadolibre", "falabella",
            "tienda", "regalo", "celular nuevo", "electrodomestico", "mueble", "zara", "shein",
            "clothes", "shoes", "gift", "shopping"
        )
    ),
    OTHER("Otros", Color(0xFF8A8F98), emptyList());

    companion object {
        fun fromName(name: String): Category = entries.firstOrNull { it.name == name } ?: OTHER
    }
}

/** Sugiere una categoría a partir de la descripción, sin conexión y sin IA externa. */
object Categorizer {

    fun suggest(description: String): Category? {
        val text = normalize(description)
        if (text.isBlank()) return null
        val tokens = text.split(Regex("[^a-z0-9]+")).filter { it.isNotEmpty() }

        var best: Category? = null
        var bestScore = 0
        for (category in Category.entries) {
            var score = 0
            for (keyword in category.keywords) {
                val key = normalize(keyword)
                val hit = if (key.contains(' ')) {
                    text.contains(key)
                } else {
                    // Coincidencia exacta, o prefijo para plurales/variantes (restaurantes, cafeteria…).
                    tokens.any { it == key || (key.length >= 5 && it.startsWith(key)) }
                }
                if (hit) score += key.length
            }
            if (score > bestScore) {
                bestScore = score
                best = category
            }
        }
        return best
    }

    private fun normalize(s: String): String =
        Normalizer.normalize(s.lowercase().trim(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
}
