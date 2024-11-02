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

    fun play(card: Card, currentTrick: Trick?, heartsHaveBeenPlayed: Boolean): Pair<PlayerId, Card> {
        gameRequires(card in hand) { "$name does not have $card in their hand" }
        gameRequires(twoOfClubsIsPlayedOnFirstTurn(card)) { "$name must play ${Symbol.TWO of Suit.CLUBS} on the first turn" }
        currentTrick?.checkCardIsPlayable(card, heartsHaveBeenPlayed)
        hand.remove(card)
        return id to card
    }

    private fun Trick.checkCardIsPlayable(card: Card, heartsHaveBeenPlayed: Boolean) {
        if (card.suit == Suit.HEARTS) {
            playerToPlayNoHeartsIfTheyAreAble(this, heartsHaveBeenPlayed)
        } else {
            gameRequires(playerToFollowSuit(this, card)) { "$name must follow suit" }
        }
    }

    private fun twoOfClubsIsPlayedOnFirstTurn(card: Card) =
        Symbol.TWO of Suit.CLUBS !in hand || card == Symbol.TWO of Suit.CLUBS

    private fun playerToPlayNoHeartsIfTheyAreAble(currentTrick: Trick, heartsHaveBeenPlayed: Boolean) {
        when {
            currentTrick.isFirstTrick && currentTrick.suit == null -> gameRequires(hand.allAre(Suit.HEARTS)) { "$name cannot open with ${Suit.HEARTS} until first ${Suit.HEARTS} has been played" }
            currentTrick.isFirstTrick && currentTrick.suit != null -> gameRequires(hand.allAre(Suit.HEARTS)) { "$name cannot play ${Suit.HEARTS} on the first trick" }
            currentTrick.suit == null -> gameRequires(heartsHaveBeenPlayed) { "$name cannot play ${Suit.HEARTS} on the first trick" }
            else -> gameRequires(currentTrick.suit !in hand) { "$name cannot play ${Suit.HEARTS} on the first trick" }
        }
    }

    private fun playerToFollowSuit(currentTrick: Trick, card: Card) =
        currentTrick.suit == null || card.suit == currentTrick.suit || card.suit == Suit.HEARTS
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