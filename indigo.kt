package indigo

import kotlin.random.Random

const val RANKS = "A 2 3 4 5 6 7 8 9 10 J Q K"
const val SUITS = "♦ ♥ ♠ ♣"
const val STARTING_HAND = 6
const val STARTING_TABLE = 4

fun main() = IndigoCardGame.start()

class IndigoCardGame {
    companion object {
        private val cardDeck = SUITS.split(" ").flatMap { suit -> RANKS.split(" ").map { Card(it, suit) } }.toList()
        private val dealer = Dealer().also { it.drawCards(cardDeck) }
        private val table = Table().also { it.drawCards(dealer.dealCards(STARTING_TABLE)) }
        private val playerUser = Player().also { it.drawCards(dealer.dealCards(STARTING_HAND)) }
        private val playerAI = PlayerAI().also { it.drawCards(dealer.dealCards(STARTING_HAND)) }
        private var winner: Player? = null

        fun start() = println("Indigo Card Game").also { chooseFirstTurn() }.also { println("Game Over") }

        private fun chooseFirstTurn() {
            println("Play first?")
            when(val choice = readln()) {
                "yes", "no" ->  println("Initial cards on the table: $table") .also { nextTurn(choice == "no") }
                "exit" -> return
                else -> chooseFirstTurn()
            }
        }

        private fun nextTurn(isTurnAI: Boolean) {
            val player = if (isTurnAI) playerAI.also { it.selectCard(table.cards.lastOrNull() ?: Card("", "")) } else playerUser
            println("\n${table.getInfo()}\n${player.getInfo()}")
            chooseCard(isTurnAI, player)
        }

        private fun chooseCard(isTurnAI: Boolean, player: Player) {
            if (!isTurnAI) println("Choose a card to play (1-${player.cards.size}):")
            when(val choice = if (!isTurnAI) readln() else (playerAI.numCard!!).toString()) {
                in player.cards.indices.map { (it + 1).toString() } -> {
                    checkWinner(player, player.dealCards(choice.toInt() - 1)).also { topUpCards(player) }
                    if (playerUser.cards.size + playerAI.cards.size == 0) endGame() else nextTurn(!isTurnAI)
                }
                "exit" -> return
                else -> chooseCard(isTurnAI, player)
            }
        }

        private fun checkWinner(player: Player, selectedCard: List<Card>) {
            with(table) {
                val topCard = cards.lastOrNull() ?: Card("", "")
                drawCards(selectedCard)
                if (topCard.suit == selectedCard.first().suit || topCard.rank == selectedCard.first().rank) {
                    winner = player.also { println("${it.name} wins cards") }
                    passCardsToWinner(player).also { println(stats(false)) }
                }
            }
        }

        private fun endGame() {
            println("\n${table.getInfo()}")
            table.passCardsToWinner(winner!!)
            println(table.stats(true))
        }

        private fun topUpCards(player: Player) {
            if (dealer.cards.size != 0 && player.cards.size == 0) player.drawCards(dealer.dealCards(STARTING_HAND))
        }
    }
}

data class Card(val rank: String, var suit: String) {
    var point = if (rank in listOf("A", "10", "J", "Q", "K")) 1 else 0
    override fun toString() = rank + suit
}

open class Dealer(open var cards: MutableList<Card> = mutableListOf()) {
    open fun drawCards(drawnCards: List<Card>) { cards = drawnCards.shuffled().toMutableList() }
    open fun dealCards(number: Int) = List(number) { cards.removeFirst() }
    override fun toString() = cards.joinToString(" ")
    open fun getInfo() = (RANKS + SUITS).split(" ").size.toString()
}

open class Player(var name: String = "Player") : Dealer() {
    override fun drawCards(drawnCards: List<Card>) {cards += drawnCards.toMutableList() }
    override fun dealCards(number: Int) = listOf(cards.removeAt(number))
    override fun getInfo() = "Cards in hand: ${cards.mapIndexed { i, card ->  "${i + 1})$card"}.joinToString(" ")}"
}

class PlayerAI : Player(name = "Computer") {
    var numCard: Int? = null

    fun selectCard(topCard: Card) {
        when {
            getCandidateCards(topCard).isEmpty() ->
                getSimilarCards().also { numCard = cards.indexOf(it[Random.nextInt(0, it.size)]) + 1 }
            else -> getCandidateCards(topCard).also { numCard = cards.indexOf(it[Random.nextInt(0, it.size)]) + 1 }
        }

    }

    private fun getSimilarCards(keySuit: Boolean = true): List<Card> {
        var cards = cards.groupBy { if (keySuit) it.suit else it.rank }.values.filter { group -> group.size > 1 }.flatten()
        if (cards.isEmpty() && keySuit) cards = getSimilarCards(false) else if (cards.isEmpty()) cards = this.cards
        return cards
    }

    private fun getCandidateCards(topCard: Card): List<Card> {
        return cards.filter { it.suit == topCard.suit }.toMutableList()
            .also { if (it.size < 2) it += cards.filter { card -> card.rank == topCard.rank } }
    }

    override fun getInfo() = "${cards.joinToString(" ")}\nComputer plays ${cards[numCard!! - 1]}"
}

class Table : Player(name = "Table") {
    private val playerCards: MutableList<Card> = mutableListOf()
    private val computerCards: MutableList<Card> = mutableListOf()

    override fun getInfo(): String {
        return if (cards.isNotEmpty()) {
            "${cards.size} cards on the table, and the top card is ${cards.last()}"
        } else {
            "No cards on the table"
        }
    }

    fun passCardsToWinner(winner: Player) {
        (if (winner.name == "Computer") computerCards else playerCards) += cards
        cards.clear()
    }

    fun stats(addBonusPoints: Boolean): String {
        fun scoring(cards: MutableList<Card>, opponentCards: MutableList<Card>): Int {
            return cards.sumOf { it.point } + if (addBonusPoints && cards.size > opponentCards.size) 3 else 0
        }
        return "Score: Player ${scoring(playerCards, computerCards) } - Computer ${scoring(computerCards, playerCards) }" +
                "\nCards: Player ${playerCards.size} - Computer ${computerCards.size}"
    }
}