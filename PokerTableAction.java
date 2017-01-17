package twcore.bots.pokerbot;

public enum PokerTableAction {
    PRE_FLOP_CARDS,
    PRE_FLOP_BETTING_ROUND,
    FLOP_CARDS,
    FLOP_BETTING_ROUND,
    TURN_CARD,
    TURN_BETTING_ROUND,
    RIVER_CARD,
    RIVER_BETTING_ROUND,
    SHOWDOWN,
    NOTHING,
}