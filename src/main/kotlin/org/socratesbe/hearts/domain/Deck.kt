package org.socratesbe.hearts.domain

import org.socratesbe.hearts.vocabulary.Card
import org.socratesbe.hearts.vocabulary.Suit
import org.socratesbe.hearts.vocabulary.Symbol
import org.socratesbe.hearts.vocabulary.of

class Deck private constructor(private val cards: ArrayDeque<Card>) {
    constructor() : this(ArrayDeque(Suit.entries.flatMap { suit ->
        Symbol.entries.map { symbol -> symbol of suit }
    }))

    fun shuffle() = Deck(ArrayDeque(cards.apply { shuffle() }))

    fun draw() = cards.removeFirstOrNull() ?: error("Deck is empty")
}