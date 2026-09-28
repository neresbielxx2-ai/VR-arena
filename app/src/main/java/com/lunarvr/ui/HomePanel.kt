package com.lunarvr.ui

enum class HomeTab {
    ALL,
    INSTALLED,
    RECENT
}

data class LibraryAppItem(
    val id: String,
    val name: String,
    val category: String,
    val bgHex: String,
    val hasNotificationDot: Boolean = false
)

class HomePanel(
    private val onOpenYouTube: () -> Unit,
    private val onOpenLNMusic: () -> Unit,
    private val onOpenBrowser: () -> Unit,
    private val onOpenSettings: () -> Unit,
    private val onTabChanged: () -> Unit,
    private val onCloseHome: () -> Unit
) {
    var isVisible: Boolean = false
    var currentTab: HomeTab = HomeTab.ALL

    var currentCenterX: Float = 0f
    var currentCenterY: Float = 0.12f
    var currentCenterZ: Float = -1.35f

    val buttons = mutableListOf<AppButton>()

    val appGrid = listOf(
        LibraryAppItem("app_store", "Store", "Loja VR", "#F97316"),
        LibraryAppItem("app_music", "LN Music", "Reprodutor MP3", "#1DB954", true),
        LibraryAppItem("app_browser", "Browser", "Navegador Web", "#3B82F6"),
        LibraryAppItem("app_gods", "Gods of Gravity", "Estratégia Espacial", "#7C3AED"),
        LibraryAppItem("app_nex", "NEX | Video", "Reprodutor Vídeo", "#0284C7", true),
        LibraryAppItem("app_beat", "Beat Saber", "Jogo de Ritmo", "#DC2626"),
        LibraryAppItem("app_gallery", "Gallery", "Fotos & Mídia", "#EC4899", true),
        LibraryAppItem("app_settings", "Settings", "Ajustes do Sistema", "#475569"),
        LibraryAppItem("app_golf", "Walkabout Mini Golf", "Esporte VR", "#059669"),
        LibraryAppItem("app_golf_plus", "GOLF+", "Simulador de Golfe", "#D97706", true),
        LibraryAppItem("app_youtube", "YouTube", "Vídeos em Tela Cheia", "#FFFFFF", true),
        LibraryAppItem("app_pocket", "Pocket Lands", "Aventura VR", "#B45309")
    )

    init {
        setupButtons(0f, 0.12f, -1.35f)
    }

    fun setupButtons(centerX: Float = currentCenterX, centerY: Float = currentCenterY, centerZ: Float = currentCenterZ) {
        currentCenterX = centerX
        currentCenterY = centerY
        currentCenterZ = centerZ
        buttons.clear()

        // Bottom Pill Control Bar buttons: [...] [ Library ] [ Resize ] [ Close ]
        val bottomY = centerY - 0.44f
        buttons.add(
            AppButton("btn_close_home", "✕", centerX + 0.18f, bottomY, centerZ, 0.08f, 0.06f) {
                isVisible = false
                onCloseHome()
            }
        )

        // 4 Columns x 3 Rows App Grid
        // Grid span: width ~1.10m, height ~0.65m
        val gridStartX = centerX - 0.40f
        val gridStartY = centerY + 0.18f
        val colSpacing = 0.26f
        val rowSpacing = 0.19f
        val cardW = 0.23f
        val cardH = 0.16f

        for (i in appGrid.indices) {
            val item = appGrid[i]
            val row = i / 4
            val col = i % 4
            val ax = gridStartX + col * colSpacing
            val ay = gridStartY - row * rowSpacing

            buttons.add(
                AppButton("btn_lib_${item.id}", item.name, ax, ay, centerZ, cardW, cardH) {
                    when (item.id) {
                        "app_youtube" -> onOpenYouTube()
                        "app_music" -> onOpenLNMusic()
                        "app_browser" -> onOpenBrowser()
                        "app_settings" -> onOpenSettings()
                        else -> {
                            // Launch app or feedback
                        }
                    }
                }
            )
        }
    }
}
