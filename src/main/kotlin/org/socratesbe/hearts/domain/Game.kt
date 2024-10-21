package org.socratesbe.hearts.domain

import org.socratesbe.hearts.application.api.command.*
import org.socratesbe.hearts.vocabulary.*
import org.socratesbe.hearts.vocabulary.Suit.CLUBS
import org.socratesbe.hearts.vocabulary.Suit.HEARTS
import org.socratesbe.hearts.vocabulary.Symbol.TWO
import java.util.Comparator

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

    class Started private constructor(
        private val players: DealtPlayers,
        private val currentTrick: Trick,
    ) : GameState {
        private val currentPlayer = players.currentPlayer

        fun whoseTurnIsIt(): PlayerName = currentPlayer.name

        fun peekIntoHandOf(playerName: PlayerName): List<Card> =
            getPlayer(playerName).hand

        fun playCard(card: Card, playedBy: PlayerName): Started {
            val player = getPlayer(playedBy)
            gameRequires(currentPlayer == player) { "It's not ${playedBy}'s turn to play" }
            player.play(card)
            return addToTrick(card, player.id).nextPlayer()
        }

        private fun addToTrick(card: Card, playerId: PlayerId) =
            Started(
                players = players,
                currentTrick = currentTrick.add(card, playerId)
            )

        private fun nextPlayer(): Started {
            val winner = currentTrick.winner
            val (nextPlayer, trick) =
                if (winner != null) players.startWith(winner) to Trick()
                else players.next() to currentTrick
            return Started(nextPlayer, trick)
        }

        private fun getPlayer(playerName: PlayerName): DealtPlayer = players.getByName(playerName)

        companion object {
            fun Started(players: List<Player>, dealer: (PlayerName) -> List<Card>): Started {
                val dealtPlayers = players.map { player -> player.deal(dealer) }
                return Started(DealtPlayers(dealtPlayers), Trick())
            }
        }
    }
}

class Trick(private val cards: Map<Card, PlayerId> = emptyMap()) {
    val winner: PlayerId?
        get() =
            if (!isFinished()) null
            else cards.getValue(cards.keys.maxWith(HeartsComparator))

    fun add(card: Card, playerId: PlayerId) =
        Trick(cards + listOf(card to playerId))

    private fun isFinished() = cards.size == 4
}

class DealtPlayers private constructor(private val players: List<DealtPlayer>) {
    val currentPlayer get() = players.first()

    fun next() = DealtPlayers((1..5).map { players[it % 4] })

    fun startWith(playerId: PlayerId) =
        DealtPlayers(
            players.indexOfFirst { it.id == playerId }
                .let { indexToCycleFrom ->
                    (indexToCycleFrom..indexToCycleFrom + 3).map { players[it % 4] }
                }
        )

    fun getByName(playerName: PlayerName): DealtPlayer =
        players.firstOrNull { it.name == playerName }
            ?: error("There's no player with name $playerName in this game...")

    fun getById(playerId: PlayerId): DealtPlayer =
        players.firstOrNull { it.id == playerId }
            ?: error("There's no player with id $playerId in this game...")

    companion object {
        operator fun invoke(players: List<DealtPlayer>) =
            DealtPlayers(players)
                .startWith(players.firstOrNull { TWO of CLUBS in it.hand }?.id ?: players.first().id)
    }
}

object HeartsComparator : Comparator<Card> {
    override fun compare(left: Card, right: Card): Int =
        when {
            left.suit == right.suit -> left.symbol.compareTo(right.symbol)
            left.suit == HEARTS -> 1
            right.suit == HEARTS -> -1
            else -> 1 //means right is of a different suit than the first card in the trick
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

    val id = player.id
    val name = player.name

    fun play(card: Card) {
        gameRequires(card in hand) { "$name does not have $card in their hand" }
        if (TWO of CLUBS in hand) gameRequires(card == TWO of CLUBS) { "$name must play ${TWO of CLUBS} on the first turn" }
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