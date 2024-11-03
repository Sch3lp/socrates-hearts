package org.socratesbe.hearts.domain

import org.socratesbe.hearts.vocabulary.Card

interface PassingRule {
    fun with(gameEvents: GameEvents): InitiatedPassingRule
}

abstract class InitiatedPassingRule(protected val gameEvents: GameEvents) {
    abstract fun passCards(passedBy: DealtPlayer, cards: Set<Card>)
}

object NoPassing : PassingRule {
    override fun with(gameEvents: GameEvents): InitiatedPassingRule = InitiatedNoPassingRule(gameEvents)
}
class InitiatedNoPassingRule(gameEvents: GameEvents) : InitiatedPassingRule(gameEvents) {
    override fun passCards(passedBy: DealtPlayer, cards: Set<Card>) {
        //noop
    }
}

object AlwaysPassLeft : PassingRule {
    override fun with(gameEvents: GameEvents) = InitiatedAlwaysPassLeft(gameEvents)
}

class InitiatedAlwaysPassLeft(gameEvents: GameEvents) : InitiatedPassingRule(gameEvents) {
    override fun passCards(passedBy: DealtPlayer, cards: Set<Card>) {
        val playerHasAlreadyPassed = gameEvents.filterIsInstance<PlayerPassedCards>().any { it.by == passedBy.id }
        gameRequires(!playerHasAlreadyPassed) {"${passedBy.name} already passed cards during this deal"}
        validateOrError(passedBy, cards)
        gameEvents.publish(PlayerPassedCards(by = passedBy.id, cards = cards, to = passedBy.id.playerToTheLeft))

        if (gameEvents.filterIsInstance<PlayerPassedCards>().size == 4) gameEvents.publish(AllPlayersPassedCards)
    }

    private fun validateOrError(player: DealtPlayer, cards: Set<Card>) {
        val cardsNotInHand: Set<Card> = cards - player.hand.toList().toSet()
        gameRequires(cardsNotInHand.isEmpty()) {
            cardsNotInHand.joinToString(
                prefix = "${player.name} does not have ",
                separator = ", nor "
            ) { it.toString() }
        }
        gameRequires(cards.size == 3) { "${player.name} needs to pass exactly three cards" }
    }
}

object FourWayPassing : PassingRule {
    override fun with(gameEvents: GameEvents) = InitiatedFourWayPassing(gameEvents)
}
class InitiatedFourWayPassing(gameEvents: GameEvents) : InitiatedPassingRule(gameEvents) {
    override fun passCards(passedBy: DealtPlayer, cards: Set<Card>) {
        TODO("Not yet implemented")
    }
}