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

    fun play(card: Card, currentTrick: Trick?): Pair<PlayerId, Card> {
        gameRequires(card in hand) { "$name does not have $card in their hand" }
        gameRequires(twoOfClubsIsPlayedOnFirstTurn(card)) { "$name must play ${Symbol.TWO of Suit.CLUBS} on the first turn" }
        gameRequires(playerToPlayNoHeartsIfTheyAreAble(currentTrick, card)) { "$name cannot play ${card.suit} on the first trick" }
        gameRequires(playerToFollowSuit(currentTrick, card)) { "$name must follow suit" }
        hand.remove(card)
        return id to card
    }

    private fun twoOfClubsIsPlayedOnFirstTurn(card: Card) =
        Symbol.TWO of Suit.CLUBS !in hand || card == Symbol.TWO of Suit.CLUBS

    private fun playerToPlayNoHeartsIfTheyAreAble(currentTrick: Trick?, card: Card) =
        currentTrick?.suit == null || card.suit != Suit.HEARTS

    private fun playerToFollowSuit(currentTrick: Trick?, card: Card) =
        currentTrick?.suit == null || card.suit == currentTrick.suit
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