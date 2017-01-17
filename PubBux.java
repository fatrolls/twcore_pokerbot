package twcore.bots.pokerbot;

public class PubBux {
	private String playerName; //name of player who PubBux belong to.
	private int amount; //amount PubBux, could be [+/-]

	public PubBux(String playerName, int amount) {
		this.playerName = playerName;
		this.amount = amount;
	}

	public String getPlayerName() {
		return playerName;
	}

	public int getAmount() {
		return amount;
	}
}