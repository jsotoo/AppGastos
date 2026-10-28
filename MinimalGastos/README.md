# Gastos — gestor de gastos personales (Kotlin + Jetpack Compose)

App Android minimalista y privada: todo se guarda en el teléfono (Room + DataStore) y **no declara permiso de internet**.

## Cómo obtener el APK

### Opción A — Android Studio (la más directa)
1. Instala [Android Studio](https://developer.android.com/studio) y abre esta carpeta (`File > Open`).
2. Espera a que termine la sincronización de Gradle.
3. `Build > Build Bundle(s) / APK(s) > Build APK(s)`.
4. El archivo queda en `app/build/outputs/apk/debug/app-debug.apk`. Cópialo al teléfono y ábrelo
   (permite "instalar apps de orígenes desconocidos" si Android lo pide), o con el cable: `adb install app-debug.apk`.

### Opción B — GitHub Actions (sin instalar nada)
1. Crea un repositorio en GitHub y sube todo el contenido de esta carpeta (incluida `.github/`).
2. Entra en la pestaña **Actions → Build APK → Run workflow** (también corre solo con cada push).
3. Al terminar (~5 min), descarga el artefacto **gastos-apk** → contiene `app-debug.apk`.

### Opción C — Línea de comandos
Con JDK 17 y el Android SDK instalados: `./gradlew assembleDebug`

## Estructura

```
MinimalGastos/
├── build.gradle.kts            # plugins (AGP, Kotlin, Compose, KSP)
├── settings.gradle.kts
├── gradle.properties
├── .github/workflows/build-apk.yml
└── app/
    ├── build.gradle.kts        # dependencias
    └── src/main/
        ├── AndroidManifest.xml
        ├── res/                # tema, ícono, FileProvider
        └── java/com/minimalgastos/app/
            ├── MainActivity.kt         # navegación inferior + permiso de notificaciones
            ├── GastosApp.kt            # contenedor de dependencias
            ├── data/                   # MODEL
            │   ├── Expense.kt          # entidad Room
            │   ├── ExpenseDao.kt
            │   ├── AppDatabase.kt
            │   ├── ExpenseRepository.kt
            │   ├── SettingsRepository.kt   # presupuesto y recordatorio (DataStore)
            │   └── Category.kt         # categorías + categorización automática
            ├── viewmodel/
            │   └── ExpenseViewModel.kt # VIEWMODEL: estado, filtros, alertas, exportación
            ├── ui/                     # VIEW (Compose)
            │   ├── HomeScreen.kt       # panel, entrada rápida, gráfico
            │   ├── HistoryScreen.kt    # historial con filtros
            │   ├── SettingsScreen.kt   # presupuesto, recordatorio, CSV
            │   └── theme/Theme.kt      # claro/oscuro automático
            ├── notifications/
            │   ├── Notifier.kt
            │   └── ReminderScheduler.kt    # alarma diaria + receptor de arranque
            └── util/Format.kt          # moneda y exportación CSV
```

## Notas de diseño
- **Categorización**: diccionario de palabras clave (español/inglés) + aprendizaje local: si ya categorizaste
  "Café Juan" como Comida, la próxima vez se sugiere igual. Siempre puedes cambiarla con un toque.
- **Montos**: se ingresan en unidades enteras (sin decimales), pensado para monedas como el peso colombiano.
- **Alertas**: se notifica una vez por mes al cruzar el 80% y el 100%; el panel se pone rojo en ambos niveles.
- **Recordatorio**: usa `AlarmManager.setAndAllowWhileIdle` (sin permiso de alarmas exactas), por lo que
  puede variar unos minutos. Se reprograma tras reiniciar el teléfono.
- **CSV**: se comparte con la hoja de compartir de Android (correo, Drive, WhatsApp…). Incluye BOM para Excel.
