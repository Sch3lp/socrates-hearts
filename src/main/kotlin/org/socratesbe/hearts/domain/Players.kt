package org.socratesbe.hearts.domain

import org.socratesbe.hearts.vocabulary.*

class Players private constructor(private val players: List<Player>) {

    val size: Int get() = players.size

    fun getByName(playerName: PlayerName): Player =
        players.firstOrNull { it.name == playerName }
            ?: error("There's no player with name $playerName in this game...")

    fun getById(playerId: PlayerId): Player = players.first { it.id == playerId }

    fun playerWithStartCard() =
        players.firstOrNull { Symbol.TWO of Suit.CLUBS in it.hand }

    fun forEach(block: (Player) -> Unit) {
        players.forEach(block)
    }

    companion object {
        fun from(gameEvents: GameEvents): Players {
            val players = gameEvents.filterIsInstance<PlayerJoined>().mapIndexed { idx, playerJoined ->
                val playerId = PlayerId.entries[idx]
                val playerName = playerJoined.playerName
                val currentHand = currentHand(gameEvents, playerId)
                Player(playerId, playerName, currentHand)
            }
            return Players(players)
        }

        private fun currentHand(gameEvents: GameEvents, playerId: PlayerId): Hand {
            val dealtHand = gameEvents.forPlayerOrEmpty<PlayerWasDealtHand>(PlayerWasDealtHand::hand) { it.playerId == playerId }
            return Hand(dealtHand).apply {
                pass(gameEvents.forPlayerOrEmpty<PlayerPassedCards>(PlayerPassedCards::cards) { it.by == playerId })
                receive(gameEvents.forPlayerOrEmpty<PlayerPassedCards>(PlayerPassedCards::cards) { it.to == playerId })
                play(gameEvents.filterIsInstance<CardPlayed>().filter { cardPlayed -> cardPlayed.by == playerId }.map { it.card }.toSet())
            }
        }

        private inline fun <reified E: GameEvent> GameEvents.forPlayerOrEmpty(cardSelector: E.() -> Collection<Card>, playerSelector: (E) -> Boolean): Set<Card> =
            filterIsInstance<E>().firstOrNull { playerSelector(it) }?.cardSelector()?.toSet() ?: emptySet()
    }
}

data class Player(val id: PlayerId, val name: PlayerName, val hand: Hand) {

    fun play(card: Card, currentTrick: Trick?, heartsHaveBeenPlayed: Boolean): Pair<PlayerId, Card> {
        gameRequires(card in hand) { "$name does not have $card in their hand" }
        gameRequires(twoOfClubsIsPlayedOnFirstTurn(card)) { "$name must play ${Symbol.TWO of Suit.CLUBS} on the first turn" }
        currentTrick?.checkCardIsPlayable(card, heartsHaveBeenPlayed)
        hand.play(card)
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

enum class PlayerId {
    One, Two, Three, Four;

    val playerToTheLeft: PlayerId
        get() =
            when (this) {
                One -> Two
                Two -> Three
                Three -> Four
                Four -> One
            }
}

class Hand(dealtCards: Set<Card>) {
    private val cards: ArrayDeque<Card> = ArrayDeque(dealtCards)

    operator fun contains(card: Card) = card in cards
    operator fun contains(suit: Suit) = suit in cards.map { it.suit }

    fun play(card: Card) = cards.remove(card)
    fun play(cards: Set<Card>) = cards.forEach(::play)
    fun pass(card: Card) = cards.remove(card)
    fun pass(cards: Set<Card>) = cards.forEach(::pass)
    fun receive(card: Card) = cards.add(card)
    fun receive(cards: Set<Card>) = cards.forEach(::receive)

    fun toList(): List<Card> = cards.toList()
    fun allAre(suit: Suit) = cards.all { card -> card.suit == suit }
}