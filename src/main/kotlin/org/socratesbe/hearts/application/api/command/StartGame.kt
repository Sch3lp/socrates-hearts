package org.socratesbe.hearts.application.api.command

import org.socratesbe.hearts.domain.PassingRule
import org.socratesbe.hearts.vocabulary.Card
import org.socratesbe.hearts.vocabulary.PlayerName

class StartGame(val dealer: (PlayerName) -> List<Card>, val passingRule: PassingRule) : Command<StartGameResponse>

sealed interface StartGameResponse
data object GameHasStarted : StartGameResponse
data class GameHasNotStarted(val reason: String) : StartGameResponse
