package org.socratesbe.hearts.domain

import org.socratesbe.hearts.vocabulary.Card

sealed interface PassingRule {
    fun with(gameEvents: GameEvents): InitiatedPassingRule = when(this) {
        NoPassing -> InitiatedNoPassingRule(gameEvents)
        AlwaysPassLeft -> InitiatedAlwaysPassLeft(gameEvents)
        FourWayPassing -> InitiatedFourWayPassing(gameEvents)
    }
}

abstract class InitiatedPassingRule(protected val gameEvents: GameEvents) {
    abstract fun passCards(passedBy: Player, cards: Set<Card>)
}

data object NoPassing : PassingRule
class InitiatedNoPassingRule(gameEvents: GameEvents) : InitiatedPassingRule(gameEvents) {
    override fun passCards(passedBy: Player, cards: Set<Card>) {
        gameEvents.publish(AllPlayersPassedCards(gameEvents.currentDealId))
    }
}

data object AlwaysPassLeft : PassingRule
class InitiatedAlwaysPassLeft(gameEvents: GameEvents) : InitiatedPassingRule(gameEvents) {
    override fun passCards(passedBy: Player, cards: Set<Card>) {
        val playerHasAlreadyPassed = gameEvents.filterIsInstance<PlayerPassedCards>().any { it.by == passedBy.id }
        gameRequires(!playerHasAlreadyPassed) {"${passedBy.name} already passed cards during this deal"}
        validateOrError(passedBy, cards)
        gameEvents.publish(PlayerPassedCards(by = passedBy.id, cards = cards, to = passedBy.id.playerToTheLeft))

        if (gameEvents.filterIsInstance<PlayerPassedCards>().size == 4) gameEvents.publish(AllPlayersPassedCards(gameEvents.currentDealId))
    }

    private fun validateOrError(player: Player, cards: Set<Card>) {
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

data object FourWayPassing : PassingRule
class InitiatedFourWayPassing(gameEvents: GameEvents) : InitiatedPassingRule(gameEvents) {
    override fun passCards(passedBy: Player, cards: Set<Card>) {
        TODO("Not yet implemented")
    }
}