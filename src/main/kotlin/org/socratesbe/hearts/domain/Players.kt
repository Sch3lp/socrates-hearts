package org.socratesbe.hearts.domain

import org.socratesbe.hearts.vocabulary.*

class DealtPlayers(private val players: List<DealtPlayer>) {

    fun getByName(playerName: PlayerName): DealtPlayer =
        players.firstOrNull { it.name == playerName }
            ?: error("There's no player with name $playerName in this game...")

    fun getById(playerId: PlayerId): DealtPlayer = players.first { it.id == playerId }

    fun playerWithStartCard() =
        players.firstOrNull { Symbol.TWO of Suit.CLUBS in it.hand }
}

data class DealtPlayer(val player: Player, val hand: Hand) {
    val id = player.id
    val name = player.name

    fun play(card: Card): Pair<PlayerId, Card> {
        gameRequires(card in hand) { "$name does not have $card in their hand" }
        if (Symbol.TWO of Suit.CLUBS in hand) gameRequires(card == Symbol.TWO of Suit.CLUBS) { "$name must play ${Symbol.TWO of Suit.CLUBS} on the first turn" }
        hand.remove(card)
        return id to card
    }
}

data class Player(val id: PlayerId, val name: PlayerName)
enum class PlayerId {
    One, Two, Three, Four;

    val playerToTheLeft: PlayerId
        get() =
        when(this) {
            One -> Two
            Two -> Three
            Three -> Four
            Four -> One
        }
}