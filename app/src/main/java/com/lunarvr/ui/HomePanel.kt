package com.lunarvr.ui

enum class HomeTab {
    APPS,
    JOGOS,
    PC_SHARE
}

class HomePanel(
    private val onOpenYouTube: () -> Unit,
    private val onOpenLNMusic: () -> Unit,
    private val onTabChanged: () -> Unit,
    private val onRegeneratePin: () -> Unit,
    private val onCloseHome: () -> Unit
) {
    var isVisible: Boolean = false
    var currentTab: HomeTab = HomeTab.APPS

    var currentCenterX: Float = 0f
    var currentCenterY: Float = 0.12f
    var currentCenterZ: Float = -1.35f

    val buttons = mutableListOf<AppButton>()

    init {
        setupButtons(0f, 0.12f, -1.35f)
    }

    fun setupButtons(centerX: Float = currentCenterX, centerY: Float = currentCenterY, centerZ: Float = currentCenterZ) {
        currentCenterX = centerX
        currentCenterY = centerY
        currentCenterZ = centerZ
        buttons.clear()

        // Close button at top-left
        buttons.add(
            AppButton("btn_close_home_top_left", "✕", centerX - 0.52f, centerY + 0.35f, centerZ, 0.08f, 0.08f) {
                isVisible = false
                onCloseHome()
            }
        )

        // 3 Tab Selector Buttons at the top: [ Apps ]  [ Jogos ]  [ Conexão PC ]
        val tabW = 0.34f
        val tabH = 0.075f
        val tabY = centerY + 0.32f

        buttons.add(
            AppButton("btn_tab_apps", "Apps", centerX - 0.38f, tabY, centerZ, tabW, tabH) {
                currentTab = HomeTab.APPS
                setupButtons()
                onTabChanged()
            }
        )

        buttons.add(
            AppButton("btn_tab_jogos", "Jogos", centerX, tabY, centerZ, tabW, tabH) {
                currentTab = HomeTab.JOGOS
                setupButtons()
                onTabChanged()
            }
        )

        buttons.add(
            AppButton("btn_tab_pc_share", "Conexão PC", centerX + 0.38f, tabY, centerZ, tabW, tabH) {
                currentTab = HomeTab.PC_SHARE
                setupButtons()
                onTabChanged()
            }
        )

        // Tab-specific interactive elements
        when (currentTab) {
            HomeTab.APPS -> {
                val cardW = 0.38f
                val cardH = 0.26f
                val cardY = centerY + 0.04f

                buttons.add(
                    AppButton("btn_app_youtube", "YouTube VR", centerX - 0.24f, cardY, centerZ, cardW, cardH) {
                        onOpenYouTube()
                    }
                )

                buttons.add(
                    AppButton("btn_app_ln_music", "LN Music", centerX + 0.24f, cardY, centerZ, cardW, cardH) {
                        onOpenLNMusic()
                    }
                )
            }
            HomeTab.PC_SHARE -> {
                val btnW = 0.42f
                val btnH = 0.08f
                buttons.add(
                    AppButton("btn_regen_pin", "Novo Código Conexão", centerX, centerY - 0.22f, centerZ, btnW, btnH) {
                        onRegeneratePin()
                        setupButtons()
                        onTabChanged()
                    }
                )
            }
            HomeTab.JOGOS -> {
                // No games available yet
            }
        }
    }
}
