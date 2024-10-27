package org.socratesbe.hearts.domain

import org.socratesbe.hearts.vocabulary.Card
import org.socratesbe.hearts.vocabulary.Suit

class Trick(private val trickNumber: Int, private val cards: Map<Card, PlayerId>) {
    val isFirstTrick: Boolean = trickNumber == 1
    private val isFinished = cards.size == 4

    val suit: Suit? = if(!isFinished) cards.keys.firstOrNull()?.suit else null
    val wasWonBy: PlayerId? = if (!isFinished) null else cards.getValue(cards.keys.maxWith(HeartsComparator))

}