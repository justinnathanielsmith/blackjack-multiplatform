package io.github.smithjustinn.blackjack.services

import org.jetbrains.compose.resources.StringResource
import sharedui.generated.resources.Res
import sharedui.generated.resources.audio_click
import sharedui.generated.resources.audio_deal
import sharedui.generated.resources.audio_flip
import sharedui.generated.resources.audio_highscore
import sharedui.generated.resources.audio_lose
import sharedui.generated.resources.audio_match
import sharedui.generated.resources.audio_mismatch
import sharedui.generated.resources.audio_nuts
import sharedui.generated.resources.audio_plink
import sharedui.generated.resources.audio_push
import sharedui.generated.resources.audio_tension
import sharedui.generated.resources.audio_win

interface AudioService {
    var isMuted: Boolean

    fun playEffect(effect: SoundEffect)

    fun release()

    enum class SoundEffect {
        FLIP,
        MATCH,
        MISMATCH,
        THE_NUTS,
        WIN,
        LOSE,
        HIGH_SCORE,
        CLICK,
        DEAL,
        PLINK,
        PUSH,
        TENSION,
    }

    companion object {
        fun SoundEffect.toResource(): StringResource {
            return when (this) {
                SoundEffect.FLIP -> Res.string.audio_flip
                SoundEffect.MATCH -> Res.string.audio_match
                SoundEffect.MISMATCH -> Res.string.audio_mismatch
                SoundEffect.THE_NUTS -> Res.string.audio_nuts
                SoundEffect.WIN -> Res.string.audio_win
                SoundEffect.LOSE -> Res.string.audio_lose
                SoundEffect.HIGH_SCORE -> Res.string.audio_highscore
                SoundEffect.CLICK -> Res.string.audio_click
                SoundEffect.DEAL -> Res.string.audio_deal
                SoundEffect.PLINK -> Res.string.audio_plink
                SoundEffect.PUSH -> Res.string.audio_push
                SoundEffect.TENSION -> Res.string.audio_tension
            }
        }
    }
}
