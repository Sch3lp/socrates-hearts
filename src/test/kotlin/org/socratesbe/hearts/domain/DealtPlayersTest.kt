package org.socratesbe.hearts.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.socratesbe.hearts.vocabulary.Suit
import org.socratesbe.hearts.vocabulary.Symbol
import org.socratesbe.hearts.vocabulary.of

class DealtPlayersTest {
    @Test
    fun `Player With Two of Clubs starts`() {
        val dealtPlayers = listOf(
            Player(PlayerId.One, "Snarf"),
            Player(PlayerId.Two, "Lion-O"),
            Player(PlayerId.Three, "Panthro"),
            Player(PlayerId.Four, "Mumra"),
        ).zip(Suit.entries.map { suit -> Symbol.entries.map { symbol -> symbol of suit } })
            .map { (player, hand) -> DealtPlayer(player, ArrayDeque(hand)) }

        val actual = DealtPlayers(dealtPlayers).currentPlayer

        assertThat(actual.name).isEqualTo("Panthro")
    }

    @Test
    fun `startWith retains order, but shifts the result of startWith to the top, making it the currentPlayer`() {
        val dealtPlayers = listOf(
            Player(PlayerId.One, "Snarf"),
            Player(PlayerId.Two, "Lion-O"),
            Player(PlayerId.Three, "Panthro"),
            Player(PlayerId.Four, "Mumra"),
        ).zip(Suit.entries.map { suit -> (Symbol.entries - Symbol.TWO).map { symbol -> symbol of suit } })
            .map { (player, hand) -> DealtPlayer(player, ArrayDeque(hand)) }

        DealtPlayers(dealtPlayers).also { assertThat(it.currentPlayer.name).isEqualTo("Snarf") }
            .startWith(PlayerId.Four).also { assertThat(it.currentPlayer.name).isEqualTo("Mumra") }
            .startWith(PlayerId.Three).also { assertThat(it.currentPlayer.name).isEqualTo("Panthro") }
            .next().also { assertThat(it.currentPlayer.name).isEqualTo("Mumra") }
    }
}