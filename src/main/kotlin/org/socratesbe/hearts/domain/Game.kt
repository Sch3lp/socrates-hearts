package org.socratesbe.hearts.domain

import org.socratesbe.hearts.vocabulary.*
import org.socratesbe.hearts.vocabulary.Suit.CLUBS
import org.socratesbe.hearts.vocabulary.Suit.HEARTS
import org.socratesbe.hearts.vocabulary.Symbol.TWO
import kotlin.reflect.KClass

sealed interface GameEvent
data object GameCreated : GameEvent
data object GameStarted : GameEvent
data class PlayerJoined(val playerName: PlayerName) : GameEvent
data class PlayerWasDealtHand(val playerId: PlayerId, val hand: List<Card>) : GameEvent
data object NewTrickStarted : GameEvent
data class CardPlayed(val by: PlayerId, val card: Card) : GameEvent
data class TrickWon(val by: PlayerId) : GameEvent

data class Hand(private val cards: ArrayDeque<Card>) {

    operator fun contains(card: Card) = card in cards

    fun remove(card: Card) = cards.remove(card)
    fun toList(): List<Card> = cards.toList()
}

fun interface GameEventListener<GameEvent> {
    fun on(event: GameEvent)
}

class GameEvents private constructor(
    private val _events: MutableList<GameEvent>
) {
    private val listeners: MutableList<Pair<KClass<out GameEvent>, GameEventListener<out GameEvent>>> = mutableListOf()

    val events: List<GameEvent> = _events

    fun publish(gameEvent: GameEvent) {
        _events.add(gameEvent)
        listeners.forEach { (eventClass, listener) ->
            if (eventClass.isInstance(gameEvent)) {
                @Suppress("UNCHECKED_CAST")
                (listener as GameEventListener<GameEvent>).on(gameEvent)
            }
        }
    }

    fun <E : GameEvent> register(eventClass: KClass<E>, listener: GameEventListener<E>) {
        listeners.add(eventClass to listener)
    }

    inline fun <reified E : GameEvent> register(listener: GameEventListener<E>) {
        register(E::class, listener)
    }

    inline fun <reified E : GameEvent> filterIsInstance() = events.filterIsInstance<E>()

    companion object {
        operator fun invoke() = GameEvents(mutableListOf())
    }
}

class Game(private val gameEvents: GameEvents = GameEvents()) {
    init {
        gameEvents.publish(GameCreated)
        Tricks(gameEvents)
    }

    private val players: List<Player>
        get() =
            gameEvents.filterIsInstance<PlayerJoined>()
                .mapIndexed { idx, it -> Player(PlayerId.entries[idx], it.playerName) }

    private val dealtPlayers: DealtPlayers
        get() = DealtPlayers(players.map { player ->
            gameEvents.filterIsInstance<PlayerWasDealtHand>()
                .first { it.playerId == player.id }
                .let {
                    val hand = gameEvents.filterIsInstance<CardPlayed>()
                        .filter { cardPlayed -> cardPlayed.by == player.id }
                        .fold(Hand(ArrayDeque(it.hand))) { acc, cardPlayed ->
                            acc.remove(cardPlayed.card)
                            acc
                        }
                    DealtPlayer(player, hand)
                }
        })

    private val playerThatPlayedACardLast: PlayerId get() =
        gameEvents.filterIsInstance<CardPlayed>().last().by

    private val currentTrick: Trick get() =
        gameEvents.filterIsInstance<CardPlayed>().chunked(4).last()
            .let { Trick(it.associate { event -> event.card to event.by }) }

    private val trickWasJustWon get() : PlayerName? =
        if (gameEvents.events.firstOrNull{it is CardPlayed} != null) currentTrick.wasWonBy?.let { id -> dealtPlayers.getById(id) }?.name
        else null

    val isStarted: Boolean get() = gameEvents.filterIsInstance<GameStarted>().isNotEmpty()

    private fun getPlayer(playerName: PlayerName) = dealtPlayers.getByName(playerName)

    fun join(player: PlayerName) {
        gameRequires(players.size < 4) { "Game already has four players" }
        gameRequires(!isStarted) { "Game has started" }
        gameEvents.publish(PlayerJoined(player))
    }

    fun start(dealer: (PlayerName) -> List<Card>) {
        gameRequires(players.size == 4) { "Not enough players joined" }
        gameRequires(!isStarted) { "Game was started already" }
        gameEvents.publish(GameStarted)
        players.forEach { player ->
            gameEvents.publish(PlayerWasDealtHand(player.id, dealer(player.name)))
        }
        gameEvents.publish(NewTrickStarted)
    }

    fun peekIntoHandOf(player: PlayerName): List<Card> {
        gameRequires(isStarted) { "Game hasn't started yet" }
        return getPlayer(player).hand.toList()
    }

    fun whoseTurnIsIt(): PlayerName {
        gameRequires(isStarted) { "Game has not started yet" }
        return dealtPlayers.playerWithStartCard()?.name
            ?: trickWasJustWon
            ?: dealtPlayers.getById(playerThatPlayedACardLast.playerToTheLeft).name
    }

    fun playCard(card: Card, playedBy: PlayerName) {
        gameRequires(isStarted) { "Game has not started yet" }
        val player = getPlayer(playedBy)
        gameRequires(playedBy == whoseTurnIsIt()) { "It's not ${playedBy}'s turn to play" }
        val (playerId, playedCard) = player.play(card)
        gameEvents.publish(CardPlayed(by = playerId, card = playedCard))
    }

    private val PlayerId.playerToTheLeft: PlayerId get() =
        when(this) {
            PlayerId.One -> PlayerId.Two
            PlayerId.Two -> PlayerId.Three
            PlayerId.Three -> PlayerId.Four
            PlayerId.Four -> PlayerId.One
        }
}

class Tricks(private val gameEvents: GameEvents) {

    private val cards: Map<Card, PlayerId>
        get() = gameEvents.filterIsInstance<CardPlayed>().chunked(4)
            .map { playedCards -> playedCards.associate { it.card to it.by } }
            .firstOrNull() ?: emptyMap()

    private val winner: PlayerId?
        get() =
            if (!isFinished()) null
            else cards.getValue(cards.keys.maxWith(HeartsComparator))

    private val onCardPlayed: GameEventListener<CardPlayed> = GameEventListener { _ ->
        winner?.let {
            gameEvents.publish(TrickWon(by = it))
            gameEvents.publish(NewTrickStarted)
        }
    }

    init {
        gameEvents.register(onCardPlayed)
    }

    private fun isFinished() = cards.size == 4
}

class Trick(private val cards: Map<Card, PlayerId>) {
    val wasWonBy: PlayerId?
        get() =
            if (!isFinished()) null
            else cards.getValue(cards.keys.maxWith(HeartsComparator))

    private fun isFinished() = cards.size == 4

}

class DealtPlayers private constructor(private val players: List<DealtPlayer>) {

    fun getByName(playerName: PlayerName): DealtPlayer =
        players.firstOrNull { it.name == playerName }
            ?: error("There's no player with name $playerName in this game...")

    fun getById(playerId: PlayerId): DealtPlayer = players.first { it.id == playerId }

    fun playerWithStartCard() =
        players.firstOrNull { TWO of CLUBS in it.hand }

    companion object {
        operator fun invoke(players: List<DealtPlayer>) =
            DealtPlayers(players)
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

data class DealtPlayer(val player: Player, val hand: Hand) {

    val id = player.id
    val name = player.name

    fun play(card: Card): Pair<PlayerId, Card> {
        gameRequires(card in hand) { "$name does not have $card in their hand" }
        if (TWO of CLUBS in hand) gameRequires(card == TWO of CLUBS) { "$name must play ${TWO of CLUBS} on the first turn" }
        hand.remove(card)
        return id to card
    }
}

class GameException(override val message: String) : RuntimeException(message)

fun gameRequires(predicate: Boolean, message: () -> String) {
    if (!predicate) throw GameException(message())
}

data class Player(val id: PlayerId, val name: PlayerName) {
    fun deal(dealer: (PlayerName) -> List<Card>) = DealtPlayer(this, Hand(ArrayDeque(dealer(name))))
}

enum class PlayerId {
    One, Two, Three, Four
}