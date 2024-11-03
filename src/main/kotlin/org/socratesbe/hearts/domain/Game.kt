package org.socratesbe.hearts.domain

import org.socratesbe.hearts.vocabulary.*

class Game(private val gameEvents: GameEvents = GameEvents()) {
    init {
        gameEvents.publish(GameCreated)
    }

    val isStarted: Boolean get() = gameEvents.filterIsInstance<GameStarted>().isNotEmpty()
    private val passingIsDone: Boolean get() = gameEvents.filterIsInstance<PlayersPassedCards>().isNotEmpty()

    private val heartsHaveBeenPlayed: Boolean get() = gameEvents.filterIsInstance<CardPlayed>().firstOrNull { it.card.suit == Suit.HEARTS } != null

    private val players: List<Player>
        get() =
            gameEvents.filterIsInstance<PlayerJoined>()
                .mapIndexed { idx, it -> Player(PlayerId.entries[idx], it.playerName) }

    private val dealtPlayers: DealtPlayers
        get() = DealtPlayers(players.map { player ->
            gameEvents.filterIsInstance<PlayerWasDealtHand>()
                .first { it.playerId == player.id }
                .let {
                    val hand = gameEvents.filterIsInstance<CardPlayed>()
                        .filter { cardPlayed -> cardPlayed.by == player.id }
                        .fold(Hand(ArrayDeque(it.hand))) { acc, cardPlayed ->
                            acc.remove(cardPlayed.card)
                            acc
                        }
                    DealtPlayer(player, hand)
                }
        })

    private val playerThatLastPlayedACard: PlayerId get() =
        gameEvents.filterIsInstance<CardPlayed>().last().by

    private val currentTrick: Trick? get() =
        gameEvents.filterIsInstance<CardPlayed>().chunked(4)
            .mapIndexed { idx, cards -> Trick(idx + 1, cards.associate { event -> event.card to event.by }) }
            .lastOrNull()

    private val lastTrickWonBy get() : PlayerName? =
        currentTrick?.wasWonBy?.let { id -> dealtPlayers.getById(id) }?.name

    private fun getPlayer(playerName: PlayerName) = dealtPlayers.getByName(playerName)

    fun join(player: PlayerName) {
        gameRequires(players.size < 4) { "Game already has four players" }
        gameRequires(!isStarted) { "Game has started" }
        gameEvents.publish(PlayerJoined(player))
    }

    fun start(dealer: (PlayerName) -> List<Card>, passingRule: PassingRule) {
        gameRequires(players.size == 4) { "Not enough players joined" }
        gameRequires(!isStarted) { "Game was started already" }
        gameEvents.publish(GameStarted(passingRule))
        if (passingRule == NoPassing) gameEvents.publish(PlayersPassedCards)
        players.forEach { player ->
            gameEvents.publish(PlayerWasDealtHand(player.id, dealer(player.name)))
        }
    }

    fun peekIntoHandOf(player: PlayerName): List<Card> {
        gameRequires(isStarted) { "Game hasn't started yet" }
        return getPlayer(player).hand.toList()
    }

    fun whoseTurnIsIt(): PlayerName {
        gameRequires(isStarted) { "Game has not started yet" }
        return dealtPlayers.playerWithStartCard()?.name
            ?: lastTrickWonBy
            ?: dealtPlayers.getById(playerThatLastPlayedACard.playerToTheLeft).name
    }

    fun playCard(card: Card, playedBy: PlayerName) {
        gameRequires(isStarted) { "Game has not started yet" }
        gameRequires(passingIsDone) { "It's not time to play cards yet" }
        gameRequires(playedBy == whoseTurnIsIt()) { "It's not ${playedBy}'s turn to play" }
        val player = getPlayer(playedBy)
        val (playerId, playedCard) = player.play(card, currentTrick, heartsHaveBeenPlayed)
        gameEvents.publish(CardPlayed(by = playerId, card = playedCard))
    }
}

fun defaultDealerFn(deck: Deck): (PlayerName) -> List<Card> = { _ ->
    List(13) { deck.draw() }
}

class GameException(override val message: String) : RuntimeException(message)
fun gameRequires(predicate: Boolean, message: () -> String) {
    if (!predicate) throw GameException(message())
}