package twcore.bots.pokerbot;

import java.util.List;
import java.util.ArrayList;

public class TableEvent {
	/** Whats currently happening on this table */
	private PokerTableGameState currentPokerTableGameState;
	/** What will happen on this table next */
	private PokerTableGameState nextPokerTableGameState;
	private List<Object> pokerTableGameStateParameters;

	public TableEvent(PokerTableGameState initialGameState) {
		this.nextPokerTableGameState = initialGameState;
		this.currentPokerTableGameState = initialGameState;
		
		pokerTableGameStateParameters = new ArrayList<Object>();
	}

	public PokerTableGameState getCurrentGameState() {
		return currentPokerTableGameState;
	}
	
	public PokerTableGameState getNextGameState() {
		//Next game state becomes the Current game state.
		if(currentPokerTableGameState != nextPokerTableGameState) {
        		currentPokerTableGameState = nextPokerTableGameState;
        		pokerTableGameStateParameters.clear();
        	}
        	return currentPokerTableGameState;
	}
	
	public void setNextGameState(PokerTableGameState nextPokerTableGameState) {
		this.nextPokerTableGameState = nextPokerTableGameState;
	}

	public int totalParameters() {
		return pokerTableGameStateParameters.size();
	}

	public List<Object> getParameters()  {
		return pokerTableGameStateParameters;
	}
	
	public void addParameter(Object param) {
		pokerTableGameStateParameters.add(param);
	}

  	public Object getParameterAt(int index) {
    		return ((Object)pokerTableGameStateParameters.get(index)); 
	}

}