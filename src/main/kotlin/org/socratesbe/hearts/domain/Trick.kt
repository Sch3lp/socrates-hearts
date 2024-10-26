package org.socratesbe.hearts.domain

import org.socratesbe.hearts.vocabulary.Card

class Trick(private val cards: Map<Card, PlayerId>) {
    val wasWonBy: PlayerId?
        get() =
            if (!isFinished()) null
            else cards.getValue(cards.keys.maxWith(HeartsComparator))

    private fun isFinished() = cards.size == 4
}