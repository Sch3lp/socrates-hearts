package org.socratesbe.hearts.domain

import org.socratesbe.hearts.vocabulary.Card
import org.socratesbe.hearts.vocabulary.PlayerName
import org.socratesbe.hearts.vocabulary.Suit
import kotlin.reflect.KClass

sealed interface GameEvent
data object GameCreated : GameEvent
data class GameStarted(val passingRule: PassingRule) : GameEvent
data class PlayerJoined(val playerName: PlayerName) : GameEvent
data class PlayerWasDealtHand(val playerId: PlayerId, val hand: List<Card>) : GameEvent
data class CardPlayed(val by: PlayerId, val card: Card) : GameEvent
data object AllPlayersPassedCards : GameEvent
data class PlayerPassedCards(val by: PlayerId, val cards: Set<Card>, val to: PlayerId) : GameEvent


data class Hand(private val cards: ArrayDeque<Card>) {
    operator fun contains(card: Card) = card in cards
    operator fun contains(suit: Suit) = suit in cards.map { it.suit }

    fun remove(card: Card) = cards.remove(card)
    fun toList(): List<Card> = cards.toList()
    fun allAre(suit: Suit) = cards.all { card -> card.suit == suit }
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