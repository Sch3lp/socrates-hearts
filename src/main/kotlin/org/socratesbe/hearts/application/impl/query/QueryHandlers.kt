package org.socratesbe.hearts.application.impl.query

import org.socratesbe.hearts.application.api.query.CardsInHandOf
import org.socratesbe.hearts.application.api.query.HasGameEnded
import org.socratesbe.hearts.application.api.query.HasGameStarted
import org.socratesbe.hearts.application.api.query.Query
import org.socratesbe.hearts.application.api.query.WhatIsScoreOfPlayer
import org.socratesbe.hearts.application.api.query.WhoseTurnIsIt
import org.socratesbe.hearts.domain.Game
import org.socratesbe.hearts.vocabulary.Card
import org.socratesbe.hearts.vocabulary.PlayerName

fun interface QueryHandler<Result, Q : Query<Result>> {
    fun execute(query: Q): Result
}

class HasGameStartedHandler(private val game: Game) : QueryHandler<Boolean, HasGameStarted> {
    override fun execute(query: HasGameStarted): Boolean = game.isStarted
}

class CardsInHandOfHandler(private val game: Game) : QueryHandler<List<Card>, CardsInHandOf> {
    override fun execute(query: CardsInHandOf): List<Card> {
        return game.peekIntoHandOf(query.player)
    }
}

class WhoseTurnIsItHandler(private val game: Game) : QueryHandler<PlayerName, WhoseTurnIsIt> {
    override fun execute(query: WhoseTurnIsIt): PlayerName {
        return game.whoseTurnIsIt()
    }
}

class WhatIsScoreOfPlayerHandler(private val game: Game) : QueryHandler<Int, WhatIsScoreOfPlayer> {
    override fun execute(query: WhatIsScoreOfPlayer): Int {
        TODO()
    }
}

class HasGameEndedHandler(private val game: Game) : QueryHandler<Boolean, HasGameEnded> {
    override fun execute(query: HasGameEnded): Boolean {
        TODO()
    }
}