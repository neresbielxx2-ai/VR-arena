package com.lunarvr.ui

import com.lunarvr.music.MusicManager
import com.lunarvr.music.MusicTrack

class MusicPanel(
    val musicManager: MusicManager,
    private val onPickFileClicked: () -> Unit,
    private val onCloseClicked: () -> Unit,
    private val onRenameRequested: (MusicTrack) -> Unit
) {
    var isVisible: Boolean = false

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
            AppButton("btn_close_music", "✕", centerX - 0.52f, centerY + 0.35f, centerZ, 0.08f, 0.08f) {
                isVisible = false
                onCloseClicked()
            }
        )

        // Add Music button at top-right or center
        val addBtnW = 0.32f
        val addBtnH = 0.075f
        buttons.add(
            AppButton("btn_add_music", "+ Adicionar Música", centerX + 0.36f, centerY + 0.34f, centerZ, addBtnW, addBtnH) {
                onPickFileClicked()
            }
        )

        // Player controls at the bottom: [-10s] [ Play/Pause ] [+10s]
        val ctrlY = centerY - 0.31f
        val ctrlSize = 0.10f

        buttons.add(
            AppButton("btn_music_prev10", "-10s", centerX - 0.16f, ctrlY, centerZ, 0.10f, 0.08f) {
                musicManager.seekBackward10s()
            }
        )

        val playPauseLabel = if (musicManager.isPlaying) "Pause" else "Play"
        buttons.add(
            AppButton("btn_music_playpause", playPauseLabel, centerX, ctrlY, centerZ, 0.16f, 0.08f) {
                musicManager.togglePlayPause()
                setupButtons()
            }
        )

        buttons.add(
            AppButton("btn_music_next10", "+10s", centerX + 0.16f, ctrlY, centerZ, 0.10f, 0.08f) {
                musicManager.seekForward10s()
            }
        )

        // Track items list (up to 4 visible tracks on the card)
        val startY = centerY + 0.16f
        val itemH = 0.10f
        val itemW = 0.90f

        val tracks = musicManager.tracks
        for (i in 0 until minOf(tracks.size, 4)) {
            val track = tracks[i]
            val itemY = startY - i * 0.12f

            // Main track play button
            buttons.add(
                AppButton("btn_track_${track.id}", track.name, centerX - 0.08f, itemY, centerZ, itemW - 0.16f, itemH) {
                    musicManager.playTrack(track)
                    setupButtons()
                }
            )

            // Edit name button on the side
            buttons.add(
                AppButton("btn_edit_${track.id}", "Nome", centerX + 0.42f, itemY, centerZ, 0.12f, itemH) {
                    onRenameRequested(track)
                }
            )
        }
    }
}
