package org.socratesbe.hearts.domain

import org.socratesbe.hearts.vocabulary.Card
import org.socratesbe.hearts.vocabulary.Suit

object HeartsComparator : Comparator<Card> {
    override fun compare(left: Card, right: Card): Int =
        when {
            left.suit == right.suit -> left.symbol.compareTo(right.symbol)
            left.suit == Suit.HEARTS -> 1
            right.suit == Suit.HEARTS -> -1
            else -> 1 //means right is of a different suit than the first card in the trick
        }
}