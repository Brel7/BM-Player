# BM Player

Lecteur de musique locale Android, hors ligne, sans publicité et sans suivi.

BM Player lit les fichiers audio déjà présents sur l’appareil. Le projet vise une expérience moderne proche des lecteurs Android spécialisés, tout en conservant une identité visuelle propre et une approche respectueuse de la vie privée.

## Fonctionnalités

- Scan MediaStore des fichiers audio locaux.
- Exclusion des notes vocales WhatsApp, Telegram, Messenger et Signal.
- Lecture en arrière-plan avec Media3 et `MediaSessionService`.
- Contrôles notification, écran verrouillé et Bluetooth/AVRCP.
- Queue de lecture, lecture aléatoire, répétition et navigation par gestes.
- Crossfade réglable jusqu’à 15 secondes.
- Pochettes intégrées et pochettes MediaStore.
- Favoris persistants.
- Playlists persistantes avec ajout, lecture et suppression confirmée.
- Import/export M3U et M3U8.
- Paroles LRC locales.
- Minuteur d’arrêt avec presets et durée personnalisée.
- Égaliseur et effets audio Android disponibles selon l’appareil.
- Thème clair/sombre, roue de couleur et formes de pochettes personnalisables.
- Widget Glance de base.
- Interface française et anglaise pour les principales sections.

## Stack technique

- Kotlin
- Jetpack Compose et Material 3
- Android Media3 / ExoPlayer
- Room
- DataStore Preferences
- MediaStore API
- Hilt
- Kotlin Coroutines et Flow
- Glance
- Coil

## Compatibilité

- Android 8.0 ou supérieur, API 26+
- Compilation avec Android SDK 35
- JDK 17

## Démarrage rapide

1. Ouvrir le projet dans Android Studio.
2. Laisser Android Studio synchroniser Gradle.
3. Connecter un appareil Android ou démarrer un émulateur API 26+.
4. Lancer la configuration `app`.
5. Autoriser l’accès aux fichiers audio lorsque l’application le demande.

En ligne de commande :

```bash
./gradlew :app:assembleDebug
```

Sous Windows PowerShell :

```powershell
.\gradlew.bat :app:assembleDebug
```

L’APK debug est générée dans :

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Architecture du projet

```text
app/src/main/java/com/bmplayer/
├── data/
│   ├── local/          Room : playlists, favoris, historique, overrides
│   ├── lyrics/         Parser LRC
│   ├── mediastore/     Scan et filtrage audio
│   ├── playlist/       Import/export M3U
│   ├── preferences/    Réglages DataStore
│   └── repository/     Agrégation des données
├── domain/             Modèles métier
├── playback/           MediaSessionService et effets audio
├── ui/                 ViewModels et thème Compose
└── widget/             Widget Glance
```

## Confidentialité

BM Player ne nécessite aucun compte et n’intègre ni publicité, ni tracker, ni télémétrie tierce. Les fonctions cœur sont hors ligne. Les fichiers audio et les réglages restent sur l’appareil.

Consulter [LICENSE](LICENSE) pour la licence du code et la section À propos de l’application pour les conditions d’utilisation et la politique de confidentialité présentées à l’utilisateur.

## État du projet

Version affichée : **1.0.0 · 2026**.

Le projet est en développement actif. Les fonctionnalités dépendant du matériel, notamment certains effets audio, le Bluetooth, les widgets et le crossfade, doivent être vérifiées sur un appareil Android réel.

## Contribution

1. Créer une branche dédiée.
2. Effectuer les changements avec des commits ciblés.
3. Vérifier `./gradlew :app:assembleDebug` avant une pull request.
4. Décrire le comportement testé et l’appareil utilisé.

La CI GitHub se trouve dans `.github/workflows/android.yml` et compile automatiquement l’APK debug sur les push et pull requests.

## Licence

Le projet est distribué sous licence MIT. Voir [LICENSE](LICENSE).
