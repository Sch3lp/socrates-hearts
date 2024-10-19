package org.socratesbe.hearts.domain

import org.socratesbe.hearts.application.api.command.*
import org.socratesbe.hearts.vocabulary.*
import org.socratesbe.hearts.vocabulary.Suit.CLUBS
import org.socratesbe.hearts.vocabulary.Symbol.TWO

class Game(private var state: GameState = GameState.Open()) {

    fun join(player: PlayerName): PlayerJoinResponse =
        when (state) {
            is GameState.Open -> {
                state = (state as GameState.Open).join(player)
                PlayerJoined
            }

            is GameState.Full -> PlayerCouldNotJoin(player, "Game already has four players")

            is GameState.Started -> PlayerCouldNotJoin(player, "Game has started")
        }

    fun start(dealer: (PlayerName) -> List<Card>): StartGameResponse =
        when (state) {
            is GameState.Full -> {
                state = (state as GameState.Full).deal(dealer)
                GameHasStarted
            }

            is GameState.Open -> GameHasNotStarted("Not enough players joined")

            is GameState.Started -> GameHasNotStarted("Game was started already")
        }

    fun isStarted() = state is GameState.Started

    fun peekIntoHandOf(player: PlayerName): List<Card> =
        when (state) {
            is GameState.Started -> (state as GameState.Started).peekIntoHandOf(player)
            is GameState.Full -> error("Game hasn't started yet")
            is GameState.Open -> error("Game hasn't started yet")
        }

    fun whoseTurnIsIt(): PlayerName =
        when (state) {
            is GameState.Started -> (state as GameState.Started).whoseTurnIsIt()
            is GameState.Open -> error("Game hasn't started yet")
            is GameState.Full -> error("Game hasn't started yet")
        }

    fun playCard(card: Card, playedBy: PlayerName) =
        when (state) {
            is GameState.Started -> {
                state = (state as GameState.Started).playCard(card = card, playedBy = playedBy)
            }

            is GameState.Open -> error("Game hasn't started yet")
            is GameState.Full -> error("Game hasn't started yet")
        }
}

class GameException(override val message: String) : RuntimeException(message)

sealed interface GameState {
    class Open(private val players: List<Player> = emptyList()) : GameState {

        fun join(player: PlayerName): GameState =
            when (players.size) {
                0 -> Open(players + Player(PlayerId.One, player))
                1 -> Open(players + Player(PlayerId.Two, player))
                2 -> Open(players + Player(PlayerId.Three, player))
                else -> Full(players + Player(PlayerId.Four, player))
            }
    }

    class Full(private val players: List<Player>) : GameState {
        fun deal(dealer: (PlayerName) -> List<Card>): GameState = Started.Started(players, dealer)
    }

    class Started private constructor(private val players: List<DealtPlayer>) : GameState {
        fun peekIntoHandOf(playerName: PlayerName): List<Card> {
            return getPlayer(playerName).hand
        }

        fun whoseTurnIsIt(): PlayerName {
            return players.first().name
        }

        fun playCard(card: Card, playedBy: PlayerName): Started {
            gameRequires(whoseTurnIsIt() == playedBy) { "It's not ${playedBy}'s turn to play" }
            getPlayer(playedBy).play(card)
            return nextPlayer()
        }

        private fun nextPlayer(): Started {
            return Started(players.cycleClockwise())
        }

        private fun List<DealtPlayer>.cycleClockwise() = (1..5).map { this[it % 4] }

        private fun getPlayer(playerName: PlayerName): DealtPlayer =
            players.firstOrNull { it.name == playerName }
                ?: error("There's no player with name $playerName in this game...")

        companion object {
            fun Started(players: List<Player>, dealer: (PlayerName) -> List<Card>): Started {
                val dealtPlayers = players.map { player -> player.deal(dealer) }
                val firstPlayerId = dealtPlayers.indexOfFirst { TWO of CLUBS in it.hand }
                if (firstPlayerId == -1) error("Nobody has the $TWO of $CLUBS")
                return Started((firstPlayerId..firstPlayerId + 3).map { dealtPlayers[it % 4] })
            }
        }
    }
}

class Deck private constructor(private val cards: ArrayDeque<Card>) {
    constructor() : this(ArrayDeque(Suit.entries.flatMap { suit ->
        Symbol.entries.map { symbol -> symbol of suit }
    }))

    fun shuffle() = Deck(ArrayDeque(cards.apply { shuffle() }))

    fun draw() = cards.removeFirstOrNull() ?: error("Deck is empty")
}

fun defaultDealerFn(deck: Deck): (PlayerName) -> List<Card> = { _ ->
    List(13) { deck.draw() }
}

data class DealtPlayer(val player: Player, val hand: ArrayDeque<Card>) {

    val name = player.name

    fun play(card: Card) {
        gameRequires(card in hand) { "$name does not have $card in their hand" }
        gameRequires(TWO of CLUBS in hand && card == TWO of CLUBS) { "$name must play ${TWO of CLUBS} on the first turn" }
        hand.remove(card)
    }
}

fun gameRequires(predicate: Boolean, message: () -> String) {
    if (!predicate) throw GameException(message())
}

data class Player(val id: PlayerId, val name: PlayerName) {
    fun deal(dealer: (PlayerName) -> List<Card>) = DealtPlayer(this, ArrayDeque(dealer(name)))
}

enum class PlayerId {
    One, Two, Three, Four
}