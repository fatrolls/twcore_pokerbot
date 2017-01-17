package twcore.bots.pokerbot;

import java.util.List;
import java.util.ArrayList;


/**
 * Represents the value of a poker hand. <br />
 * <br />
 *
 * Implements the <code>Comparable</code> interface with <b>reversed</b>
 * (descending sort) order.
 *
 * @author Oscar Stigter
 */
public class HandValue implements Comparable<HandValue> {

    /** The hand. */
    private final Hand hand;

    /** The hand value type. */
    private final HandValueType type;

    /** The exact, numeric hand value. */
    private final int value;
    
    /**
     * Constructor.
     *
     * @param hand
     *            The hand.
     */
    public HandValue(Hand hand) {
        this.hand = hand;
        HandEvaluator evaluator = new HandEvaluator(hand);
        type = evaluator.getType();
        value = evaluator.getValue();
    }

    /**
     * Returns the hand.
     *
     * @return The hand.
     */
    public Hand getHand() {
        return hand;
    }

    /**
     * Returns the hand value type.
     *
     * @return The hand value type.
     */
    public HandValueType getType() {
        return type;
    }
    
    /**
     * Removes all kickers from the rankings array.
     *
     * @param int array
     *		Input a rankings array which may have kickers.
     * @return int array
     *		Returns a rankings array which will not have any kickers.
     */
    public int[] removeKickers(int[] rankings) {
    	int[] retRankings = new int[6];
    	for(int i = 0; i < retRankings.length; i++) {
    		//i == 0 is the HandValueType
    		//i == 1 is the Winning Card
    		//i == 2 is special case for TWO_PAIRS which has the second Winning Card.
    
    		if(i == 0 || i == 1 || (i == 2 && rankings[0] == HandValueType.TWO_PAIRS.getValue())) {
    			retRankings[i] = rankings[i];
    			continue;
    		}
    		//Removes any kicker.
		retRankings[i] = -1;
    	}
    	return retRankings;
    }
    
    public int[] getRequiredKickerRankings(List<Integer> otherHandValues) {
    	int[] myRankings = new int[6];
	int v = value; //copy
	for (int i=0; i < HandEvaluator.NO_OF_RANKINGS; i++) {
		myRankings[i] = (int)(v / HandEvaluator.RANKING_FACTORS[i]);
		v %= HandEvaluator.RANKING_FACTORS[i];
	}
	
	//No other Hand Values, so only I win, no kickers required to be shown.
	if(otherHandValues == null || otherHandValues.size() == 0)
		return removeKickers(myRankings); //no kickers.
	
	int[] checkRankings = new int[6];
	
	//Stores the information, if my kicker is better then anyone elses, show it then.
	int myHighestKickerIndex = -1;
	
	//Check all hand values from all opponents.
	for(Integer otherHandValue : otherHandValues) {
		v = otherHandValue;
		
		//Check all my opponent's kickers.
		for(int i=0; i < HandEvaluator.NO_OF_RANKINGS; i++) {
			checkRankings[i] = (int)(v / HandEvaluator.RANKING_FACTORS[i]);
			v %= HandEvaluator.RANKING_FACTORS[i];
			
			//Different HandValueType Or Different Winning Card Rank.
			//TWO_PAIRS is a special case because it uses the kicker space to hold the second pair.
			if((i == 0 && myRankings[0] != checkRankings[0]) ||
			    (i == 1 && myRankings[1] != checkRankings[1]) ||
			    (i == 2 && myRankings[0] == HandValueType.TWO_PAIRS.getValue() && 
			    myRankings[2] != checkRankings[2])) {
				break;
			} else if(i >= 2) {
				//Check which hand value type starts using it's kickers.
				if(myRankings[0] == HandValueType.ROYAL_FLUSH.getValue())
					break; //no kickers
				else if(myRankings[0] == HandValueType.STRAIGHT_FLUSH.getValue())
					break; //no kickers
				else if(myRankings[0] == HandValueType.FOUR_OF_A_KIND.getValue() && i > 2)
					break; //1 kicker
				else if(myRankings[0] == HandValueType.FULL_HOUSE.getValue())
					break; //no kickers
				else if(myRankings[0] == HandValueType.FLUSH.getValue() && i > 2)
					break; //1 kicker
				else if(myRankings[0] == HandValueType.STRAIGHT.getValue())
					break; //no kickers
				else if(myRankings[0] == HandValueType.THREE_OF_A_KIND.getValue() && i > 3)
					break; //2 kickers
				else if(myRankings[0] == HandValueType.TWO_PAIRS.getValue() && i == 2)
					continue; //This isn't kicker [this is second pair]			
				else if(myRankings[0] == HandValueType.TWO_PAIRS.getValue() && i > 3)
					break; //1 kicker
				else if(myRankings[0] == HandValueType.ONE_PAIR.getValue() && i > 4)
					break; //3 kickers
				else if(myRankings[0] == HandValueType.HIGH_CARD.getValue() && i > 5)
					break; //4 kickers

				//If any of my opponents have better kicker, 
				//then my kicker is useless to show.
				if(myRankings[i] < checkRankings[i] &&
				   (myHighestKickerIndex == -1 || myHighestKickerIndex == i)) {
					//My kicker is worse, can't use any kickers.
					//Or my kicker was beat by the upcoming otherHandValue.
					myRankings[i] = -1;
					return removeKickers(myRankings);
				} else if(myRankings[i] > checkRankings[i]) {
					//My kicker is better, but better one may still be found
					//From a different player's hand.
					myHighestKickerIndex = i;
					break;
				} else if(myRankings[i] == checkRankings[i]) { 
					//Both players have use same kicker, can't use this kicker.
					myRankings[i] = -1;
					continue;
				}
			}
		}
	}
	
	/*
	  * Checks if kickers required to show.
	  * Since my opponents with similar hands 
	  * Either have or don't have a better kicker then me.
	  */
	return (myHighestKickerIndex == -1) ? removeKickers(myRankings) : myRankings;
    }
	
    /**
     * Returns a description of the hand value type.
     *
     * @param optional int array
     *            These are all the numeric hand values to compare against.
     * In other to figure out whether or not to show kickers and how deep.
     * @return The description of the hand value type.
     */
    public String getDescription(List<Integer> values) {    
	    int[] myRankings = getRequiredKickerRankings(values);

            switch(type) {
    		case ROYAL_FLUSH:
    			return type.getDescription();
    		case STRAIGHT_FLUSH:
    	    		if (myRankings[1] == Card.KING)
    				return type.getDescription() + ", " + Card.CARD_NAME[myRankings[1]] + " high";
    			else if(myRankings[1] == Card.FIVE)
					return type.getDescription() + ", " + Card.CARD_NAME[myRankings[1]] + " high (Steel Wheel)";
    			else if((myRankings[1]-4) >= 0)
    				return type.getDescription() + ", " + Card.CARD_NAME[myRankings[1]-4] + " to " + Card.CARD_NAME[myRankings[1]];
    		case FOUR_OF_A_KIND:
    			if(myRankings[1] != -1 && myRankings[2] != -1) //kicker 1
    				return type.getDescription() + ", " + Card.CARD_NAMES[myRankings[1]] + " with " + Card.CARD_NAME[myRankings[2]] + " kicker";
    			else if(myRankings[1] != -1) //no kicker
    				return type.getDescription() + ", " + Card.CARD_NAMES[myRankings[1]];
    		case FULL_HOUSE:
    			if(myRankings[1] != -1 && myRankings[2] != -1)
    				return type.getDescription() + ", " + Card.CARD_NAMES[myRankings[1]] + " full of " + Card.CARD_NAMES[myRankings[2]];
   		case FLUSH: 
   			if(myRankings[1] != -1 && myRankings[2] != -1) //kicker 1
    				return type.getDescription() + ", " + Card.CARD_NAME[myRankings[1]] + " high with " + Card.CARD_NAME[myRankings[2]] + " kicker";
   			else if(myRankings[1] != -1) //no kicker
    				return type.getDescription() + ", " + Card.CARD_NAME[myRankings[1]] + " high";
    		case STRAIGHT:
    			if (myRankings[1] == Card.ACE)
    				return type.getDescription() + ", " + Card.CARD_NAME[myRankings[1]] + " high, (Broadway straight)";
    			else if((myRankings[1]-3) >= 0)
    				return type.getDescription() + ", " + Card.CARD_NAME[myRankings[1]-3] + " to " + Card.CARD_NAME[myRankings[1]];
    		case THREE_OF_A_KIND:
    			if(myRankings[1] != -1 && myRankings[2] != -1) //kicker 1
    				return type.getDescription() + ", " + Card.CARD_NAMES[myRankings[1]] + " with " + Card.CARD_NAME[myRankings[2]] + " kicker";
     			else if(myRankings[1] != -1 && myRankings[3] != -1) //kicker 2
    				return type.getDescription() + ", " + Card.CARD_NAMES[myRankings[1]] + " with " + Card.CARD_NAME[myRankings[3]] + " kicker";
    			else if (myRankings[1] != -1) //no kicker
    				return type.getDescription() + ", " + Card.CARD_NAMES[myRankings[1]];
    		case TWO_PAIRS:
    			if(myRankings[1] != -1 && myRankings[2] != -1 && myRankings[3] != -1) //kicker 1
    				return type.getDescription() + ", " + Card.CARD_NAMES[myRankings[1]] + " and " + Card.CARD_NAMES[myRankings[2]] + " with " + Card.CARD_NAME[myRankings[3]] + " kicker";
    			else if (myRankings[1] != -1 && myRankings[2] != -1) //no kicker
    				return type.getDescription() + ", " + Card.CARD_NAMES[myRankings[1]] + " and " + Card.CARD_NAMES[myRankings[2]];
    		case ONE_PAIR:
    			if(myRankings[1] != -1 && myRankings[2] != -1) //kicker 1
    				return type.getDescription() + " of " + Card.CARD_NAMES[myRankings[1]] + " with " + Card.CARD_NAME[myRankings[2]] + " kicker";
			else if(myRankings[1] != -1 && myRankings[3] != -1) //kicker 2
    				return type.getDescription() + " of " + Card.CARD_NAMES[myRankings[1]] + " with " + Card.CARD_NAME[myRankings[3]] + " kicker";
			else if(myRankings[1] != -1 && myRankings[4] != -1) //kicker 3
    				return type.getDescription() + " of " + Card.CARD_NAMES[myRankings[1]] + " with " + Card.CARD_NAME[myRankings[4]] + " kicker";
			else if (myRankings[1] != -1) //no kicker
    				return type.getDescription() + " of " + Card.CARD_NAMES[myRankings[1]];
    		
		case HIGH_CARD:
 			if(myRankings[1] != -1 && myRankings[2] != -1) //kicker 1
				return type.getDescription() + " " + Card.CARD_NAME[myRankings[1]] + " with " + Card.CARD_NAME[myRankings[2]] + " kicker";
 			else if(myRankings[1] != -1 && myRankings[3] != -1) //kicker 2
				return type.getDescription() + " " + Card.CARD_NAME[myRankings[1]] + " with " + Card.CARD_NAME[myRankings[3]] + " kicker";
 			else if(myRankings[1] != -1 && myRankings[4] != -1) //kicker 3
				return type.getDescription() + " " + Card.CARD_NAME[myRankings[1]] + " with " + Card.CARD_NAME[myRankings[4]] + " kicker";
 			else if(myRankings[1] != -1 && myRankings[5] != -1) //kicker 4
				return type.getDescription() + " " + Card.CARD_NAME[myRankings[1]] + " with " + Card.CARD_NAME[myRankings[5]] + " kicker";
		        else if(myRankings[1] != -1) //no kicker
				return type.getDescription() + " " + Card.CARD_NAME[myRankings[1]];
    		default:
    			return type.getDescription();
    	}
    }

    /**
     * Returns the exact, numeric hand value.
     *
     * @return The exact, numeric hand value.
     */
    public int getValue() {
        return value;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return value;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof HandValue) {
            return ((HandValue) obj).getValue() == value;
        } else {
            return false;
        }
    }

    /** {@inheritDoc} */
    @Override
    public int compareTo(HandValue handValue) {
        if (value > handValue.getValue()) {
            return -1;
        } else if (value < handValue.getValue()) {
            return 1;
        } else {
            return 0;
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return String.format("%s (%d)", type.getDescription(), value);
    }

}
