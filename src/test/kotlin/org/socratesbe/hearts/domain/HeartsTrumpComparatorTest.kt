package org.socratesbe.hearts.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.socratesbe.hearts.vocabulary.Suit.*
import org.socratesbe.hearts.vocabulary.Symbol
import org.socratesbe.hearts.vocabulary.Symbol.ACE
import org.socratesbe.hearts.vocabulary.Symbol.TWO
import org.socratesbe.hearts.vocabulary.of

class HeartsComparatorTest {
    @Test
    fun `Hearts is not better than any other suit`() {
        val card = listOf(ACE of SPADES, TWO of HEARTS).maxWith(HeartsComparator)
        assertThat(card).isEqualTo(ACE of SPADES)
    }

    @Test
    fun `Ace of Hearts is the highest out of all the Hearts`() {
        val card = Symbol.entries.map { it of HEARTS }.maxWith(HeartsComparator)
        assertThat(card).isEqualTo(ACE of HEARTS)
    }

    @Test
    fun `First Suit is always higher than other Suits (even Hearts)`() {
        val card = listOf(TWO of SPADES, ACE of CLUBS, ACE of DIAMONDS).maxWith(HeartsComparator)
        assertThat(card).isEqualTo(TWO of SPADES)
    }

    @Test
    fun `When all cards have the same Suit, the highest Symbol counts`() {
        val card = listOf(TWO of SPADES, ACE of SPADES, Symbol.KING of SPADES).maxWith(HeartsComparator)
        assertThat(card).isEqualTo(ACE of SPADES)
    }
}

class HeartsTrumpComparatorTest {
    @Test
    fun `Hearts are always better than any other suit`() {
        val card = listOf(ACE of SPADES, TWO of HEARTS).maxWith(HeartsTrumpComparator)
        assertThat(card).isEqualTo(TWO of HEARTS)
    }

    @Test
    fun `Ace of Hearts is the highest out of all the Hearts`() {
        val card = Symbol.entries.map { it of HEARTS }.maxWith(HeartsTrumpComparator)
        assertThat(card).isEqualTo(ACE of HEARTS)
    }

    @Test
    fun `First Suit is always higher than other Suits (other than Hearts)`() {
        val card = listOf(TWO of SPADES, ACE of CLUBS, ACE of DIAMONDS).maxWith(HeartsTrumpComparator)
        assertThat(card).isEqualTo(TWO of SPADES)
    }

    @Test
    fun `When all cards have the same Suit, the highest Symbol counts`() {
        val card = listOf(TWO of SPADES, ACE of SPADES, Symbol.KING of SPADES).maxWith(HeartsTrumpComparator)
        assertThat(card).isEqualTo(ACE of SPADES)
    }
}