package org.socratesbe.hearts.domain

import org.socratesbe.hearts.vocabulary.Card
import org.socratesbe.hearts.vocabulary.PlayerName
import kotlin.reflect.KClass

@JvmInline value class DealId(private val value: Int) {
    fun next() = DealId(value + 1)
    companion object {
        val First = DealId(1)
    }
}

sealed interface GameEvent
data object GameCreated : GameEvent
data class PlayerJoined(val playerName: PlayerName) : GameEvent
data class GameStarted(val passingRule: PassingRule) : GameEvent

sealed interface DealEvent : GameEvent {
    val dealId: DealId
}
data class DealStarted(override val dealId: DealId) : DealEvent
data class PlayerWasDealtHand(override val dealId: DealId, val playerId: PlayerId, val hand: List<Card>) : DealEvent
data class CardPlayed(override val dealId: DealId, val by: PlayerId, val card: Card) : DealEvent
data class AllPlayersPassedCards(override val dealId: DealId) : DealEvent
data class PlayerPassedCards(val by: PlayerId, val cards: Set<Card>, val to: PlayerId) : GameEvent


fun interface GameEventListener<GameEvent> {
    fun on(event: GameEvent)
}

class GameEvents private constructor(
    private val _events: MutableList<GameEvent>
) {
    private val listeners: MutableList<Pair<KClass<out GameEvent>, GameEventListener<out GameEvent>>> = mutableListOf()

    val events: List<GameEvent> = _events

    val currentDealId: DealId get() = _events.filterIsInstance<DealStarted>().lastOrNull()?.dealId ?: gameError("Cannot fetch current deal, probably because the game hasn't started yet")

    val currentDealEvents: List<GameEvent> get() = _events.filter { (it as? DealEvent)?.dealId == currentDealId }

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