package org.socratesbe.hearts.domain

import org.socratesbe.hearts.vocabulary.*

class Game(private val gameEvents: GameEvents = GameEvents()) {
    init {
        gameEvents.publish(GameCreated)
    }

    val isStarted: Boolean get() = gameEvents.filterIsInstance<GameStarted>().isNotEmpty()

    private val passingIsDone: Boolean get() = gameEvents.filterIsInstance<AllPlayersPassedCards>().isNotEmpty()

    private val heartsHaveBeenPlayed: Boolean get() = gameEvents.filterIsInstance<CardPlayed>().firstOrNull { it.card.suit == Suit.HEARTS } != null

    private val players: Players get() = Players.from(gameEvents)

    private val playerThatLastPlayedACard: PlayerId get() =
        gameEvents.filterIsInstance<CardPlayed>().last().by

    private val currentTrick: Trick? get() =
        gameEvents.filterIsInstance<CardPlayed>().chunked(4)
            .mapIndexed { idx, cards -> Trick(idx + 1, cards.associate { event -> event.card to event.by }) }
            .lastOrNull()

    private val lastTrickWonBy: PlayerName? get() =
        currentTrick?.wasWonBy?.let { id -> players.getById(id) }?.name

    private val passingRule: PassingRule get() = gameEvents.filterIsInstance<GameStarted>().firstOrNull()?.passingRule ?: gameError("Trying to access passing rule when game hasn't started yet is impossible")

    private fun getPlayer(playerName: PlayerName) = players.getByName(playerName)

    fun join(player: PlayerName) {
        gameRequires(players.size < 4) { "Game already has four players" }
        gameRequires(!isStarted) { "Game has started" }
        gameEvents.publish(PlayerJoined(player))
    }

    fun start(dealer: (PlayerName) -> List<Card>, passingRule: PassingRule) {
        gameRequires(players.size == 4) { "Not enough players joined" }
        gameRequires(!isStarted) { "Game was started already" }
        gameEvents.publish(GameStarted(passingRule))
        val dealId = DealId.First
        gameEvents.publish(DealStarted(dealId))
        players.forEach { player ->
            gameEvents.publish(PlayerWasDealtHand(dealId, player.id, dealer(player.name),))
        }
        if (passingRule == NoPassing) players.forEach { passCards(it, emptySet())}
    }

    fun peekIntoHandOf(player: PlayerName): List<Card> {
        gameRequires(isStarted) { "Game hasn't started yet" }
        return getPlayer(player).hand.toList()
    }

    fun whoseTurnIsIt(): PlayerName {
        gameRequires(isStarted) { "Game has not started yet" }
        return players.playerWithStartCard()?.name
            ?: lastTrickWonBy
            ?: players.getById(playerThatLastPlayedACard.playerToTheLeft).name
    }

    fun playCard(card: Card, playedBy: PlayerName) {
        gameRequires(isStarted) { "Game has not started yet" }
        gameRequires(passingIsDone) { "It's not time to play cards yet" }
        gameRequires(playedBy == whoseTurnIsIt()) { "It's not ${playedBy}'s turn to play" }
        val player = getPlayer(playedBy)
        val (playerId, playedCard) = player.play(card, currentTrick, heartsHaveBeenPlayed)
        gameEvents.publish(CardPlayed(dealId = gameEvents.currentDealId, by = playerId, card = playedCard))
    }

    fun passCards(passedBy: PlayerName, cards: Set<Card>) {
        gameRequires(isStarted) { "Now is not the time to be passing cards" }
        passingRule.with(gameEvents).passCards(getPlayer(passedBy), cards)
    }

    private fun passCards(player: Player, cards: Set<Card>) {
        passingRule.with(gameEvents).passCards(player, cards)
    }
}

fun defaultDealerFn(deck: Deck): (PlayerName) -> List<Card> = { _ -> List(13) { deck.draw() } }

class GameException(override val message: String) : RuntimeException(message)
fun gameRequires(predicate: Boolean, message: () -> String) {
    if (!predicate) throw GameException(message())
}
fun gameError(message: String): Nothing = throw GameException(message)