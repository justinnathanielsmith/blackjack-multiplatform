package io.github.smithjustinn.blackjack.services

import io.github.smithjustinn.blackjack.services.AudioService.Companion.toResource
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
import kotlin.test.Test
import kotlin.test.assertEquals

class AudioServiceTest {
    @Test
    fun toResourceMapsAllSoundEffectsCorrectly() {
        val mappings =
            mapOf(
                AudioService.SoundEffect.FLIP to Res.string.audio_flip,
                AudioService.SoundEffect.MATCH to Res.string.audio_match,
                AudioService.SoundEffect.MISMATCH to Res.string.audio_mismatch,
                AudioService.SoundEffect.THE_NUTS to Res.string.audio_nuts,
                AudioService.SoundEffect.WIN to Res.string.audio_win,
                AudioService.SoundEffect.LOSE to Res.string.audio_lose,
                AudioService.SoundEffect.HIGH_SCORE to Res.string.audio_highscore,
                AudioService.SoundEffect.CLICK to Res.string.audio_click,
                AudioService.SoundEffect.DEAL to Res.string.audio_deal,
                AudioService.SoundEffect.PLINK to Res.string.audio_plink,
                AudioService.SoundEffect.PUSH to Res.string.audio_push,
                AudioService.SoundEffect.TENSION to Res.string.audio_tension,
            )

        // Ensure every enum value is accounted for in our mappings map
        assertEquals(
            AudioService.SoundEffect.entries.size,
            mappings.size,
            "Not all SoundEffect entries are mapped in the test.",
        )

        mappings.forEach { (effect, expectedResource) ->
            assertEquals(
                expectedResource,
                effect.toResource(),
                "Effect $effect did not map to the expected resource.",
            )
        }
    }
}
