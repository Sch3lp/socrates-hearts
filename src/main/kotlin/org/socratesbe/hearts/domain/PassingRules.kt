package org.socratesbe.hearts.domain

import org.socratesbe.hearts.vocabulary.Card

interface PassingRule {
    fun validated(player: DealtPlayer, cards: Set<Card>, block: () -> Unit) {
        validateOrError(player,cards)
        block()
    }

    fun validateOrError(player: DealtPlayer, cards: Set<Card>)
}

object NoPassing : PassingRule {
    override fun validateOrError(player: DealtPlayer, cards: Set<Card>) = Unit
}

object AlwaysPassLeft : PassingRule {
    override fun validateOrError(player: DealtPlayer, cards: Set<Card>) {
        val cardsNotInHand: Set<Card> = cards - player.hand.toList().toSet()
        gameRequires(cardsNotInHand.isEmpty()) {
            cardsNotInHand.joinToString(
                prefix = "${player.name} does not have ",
                separator = ", nor "
            ) { it.toString() }
        }
    }
}

object FourWayPassing : PassingRule {
    override fun validateOrError(player: DealtPlayer, cards: Set<Card>) {
        TODO("implement me")
    }
}
