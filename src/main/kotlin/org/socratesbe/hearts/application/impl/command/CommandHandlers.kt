package org.socratesbe.hearts.application.impl.command

import org.socratesbe.hearts.application.api.command.*
import org.socratesbe.hearts.domain.Game
import org.socratesbe.hearts.domain.GameException
import org.socratesbe.hearts.domain.GameStarted
import org.socratesbe.hearts.domain.PlayerJoined

internal fun interface CommandHandler<Result, C : Command<Result>> {
    fun execute(command: C): Result
}

internal class MakePlayerJoinGameHandler(private val game: Game) :
    CommandHandler<PlayerJoinResponse, MakePlayerJoinGame> {
    override fun execute(command: MakePlayerJoinGame): PlayerJoinResponse =
        try {
            game.join(command.player)
            PlayerJoined
        } catch (e: GameException) {
            PlayerCouldNotJoin(command.player, e.message)
        }
}

internal class StartGameHandler(private val game: Game) : CommandHandler<StartGameResponse, StartGame> {
    override fun execute(command: StartGame): StartGameResponse {
        return try {
            game.start(command.dealer)
            GameHasStarted
        }
        catch(e: GameException) {
            GameHasNotStarted(e.message)
        }
    }
}

internal class PlayCardHandler(private val game: Game) : CommandHandler<PlayCardResponse, PlayCard> {
    override fun execute(command: PlayCard): PlayCardResponse =
        try {
            game.playCard(card = command.card, playedBy = command.playedBy)
            PlayedCard
        } catch (e: GameException) {
            CouldNotPlayCard(e.message)
        }
}

internal class PassCardsHandler(private val game: Game) : CommandHandler<PassCardsResponse, PassCards> {
    override fun execute(command: PassCards): PassCardsResponse {
        TODO()
    }
}
