package org.socratesbe.hearts

import org.socratesbe.hearts.application.api.command.Command
import org.socratesbe.hearts.application.api.command.MakePlayerJoinGame
import org.socratesbe.hearts.application.api.command.PassCards
import org.socratesbe.hearts.application.api.command.PassedCards
import org.socratesbe.hearts.application.api.command.PlayCard
import org.socratesbe.hearts.application.api.command.PlayedCard
import org.socratesbe.hearts.application.api.command.StartGame
import org.socratesbe.hearts.application.api.query.CardsInHandOf
import org.socratesbe.hearts.application.api.query.HasGameEnded
import org.socratesbe.hearts.application.api.query.Query
import org.socratesbe.hearts.application.api.query.WhatIsScoreOfPlayer
import org.socratesbe.hearts.application.api.query.WhoseTurnIsIt
import org.socratesbe.hearts.domain.Deck
import org.socratesbe.hearts.domain.NoPassing
import org.socratesbe.hearts.domain.defaultDealerFn
import org.socratesbe.hearts.vocabulary.Card
import org.socratesbe.hearts.vocabulary.PlayerName

fun main() {
    Application.run()
}

object Application {
    private val context = Context()
    private var round = 1

    fun run() {
        val players = listOf("Joe", "Mary", "Bob", "Sue")
        players.forEach { joinGame(it) }
        startGame()

        while (!hasGameEnded()) {
            passAllCardsFor(players)
            playRoundWith(players)
        }

        println("=== Final Scores ===")
        players.forEach { player ->
            println("$player: ${scoreOf(player)}")
        }
    }

    private fun passAllCardsFor(players: List<String>) {
        players.forEach { passFirstThreeCards(it) }
    }

    private fun scoreOf(player: PlayerName) = execute(WhatIsScoreOfPlayer(player))

    private fun playRoundWith(players: List<String>) {
        println("=== Round $round ===")
        for (turn in 0 until 13) {
            for (player in players) {
                val currentPlayer = whoseTurnIsIt()
                val playedCard = cardsInHandOf(currentPlayer)
                    .first { playCard(it, currentPlayer) == PlayedCard }
                println("$currentPlayer played $playedCard")
            }
            println("------------")
        }
        round++
    }

    private fun hasGameEnded() = execute(HasGameEnded)

    private fun playCard(card: Card, playedBy: PlayerName) = execute(PlayCard(card, playedBy))

    private fun whoseTurnIsIt() = execute(WhoseTurnIsIt)

    private fun passFirstThreeCards(player: PlayerName) {
        val hand = cardsInHandOf(player)
        val firstThreeCards = hand.subList(0, 3).toSet()
        val result = passCards(firstThreeCards, player)
        if (result == PassedCards) {
            println("$player passed cards: ${firstThreeCards.joinToString(", ")}")
        }
    }

    private fun passCards(cards: Set<Card>, passedBy: PlayerName) = execute(PassCards(cards, passedBy))

    private fun joinGame(player: PlayerName) {
        execute(MakePlayerJoinGame(player))
    }

    private fun startGame() {
        execute(StartGame(defaultDealerFn(Deck().shuffle()), NoPassing))
    }

    private fun cardsInHandOf(player: PlayerName) = execute(CardsInHandOf(player))

    private fun <Result> execute(query: Query<Result>) = context.queryExecutor.execute(query)

    private fun <Result> execute(command: Command<Result>) = context.commandExecutor.execute(command)
}
