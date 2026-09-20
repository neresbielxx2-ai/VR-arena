package com.lunarvr.ui

import com.lunarvr.handtracking.InteractableElement

enum class HomeTab {
    APPS,
    JOGOS
}

class HomePanel(
    private val onOpenYouTube: () -> Unit,
    private val onTabChanged: () -> Unit
) {
    var isVisible: Boolean = true
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

        // Tab Selector Buttons at the top: [ Apps ]  [ Jogos ]
        val tabW = 0.28f
        val tabH = 0.075f
        val tabY = centerY + 0.32f

        buttons.add(
            AppButton("btn_tab_apps", "Apps", centerX - 0.18f, tabY, centerZ, tabW, tabH) {
                currentTab = HomeTab.APPS
                setupButtons()
                onTabChanged()
            }
        )

        buttons.add(
            AppButton("btn_tab_jogos", "Jogos", centerX + 0.18f, tabY, centerZ, tabW, tabH) {
                currentTab = HomeTab.JOGOS
                setupButtons()
                onTabChanged()
            }
        )

        // If on Apps tab, add the YouTube App card button
        if (currentTab == HomeTab.APPS) {
            // YouTube Card in grid (large comfortable card button)
            val cardW = 0.42f
            val cardH = 0.26f
            val cardX = centerX - 0.32f
            val cardY = centerY + 0.04f

            buttons.add(
                AppButton("btn_app_youtube", "YouTube VR", cardX, cardY, centerZ, cardW, cardH) {
                    onOpenYouTube()
                }
            )
        }
    }
}
