package twcore.bots.pokerbot;

import twcore.core.lvz.LvzObject;

public class LvzObjectPackage implements Comparable<LvzObjectPackage> {
	private LvzObject lvzObject;
	private String playerName;
	private String ignoredPlayerName;

	public LvzObjectPackage(LvzObject lvzObject, String playerName, String ignoredPlayerName) {
		this.lvzObject = lvzObject;
		this.playerName = playerName;
		this.ignoredPlayerName = ignoredPlayerName;
	}

	public LvzObject getLvzObject() {
		return lvzObject;
	}

	public String getPlayerName() {
		return playerName;
	}

	public String getIgnoredPlayerName() {
		return (ignoredPlayerName != null) ? ignoredPlayerName : "";
	}

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return (lvzObject != null) ? lvzObject.getObjectID() : -1;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
       if(obj == null || lvzObject == null)
           return false;
        if (obj instanceof LvzObjectPackage)
            return ((LvzObjectPackage) obj).lvzObject.getObjectID() == lvzObject.getObjectID();
        else
            return false;
    }

    /** {@inheritDoc} */
    @Override
    public int compareTo(LvzObjectPackage lvzObjectPackage) {
        int thisValue = hashCode();
        int otherValue = lvzObjectPackage.hashCode();
        if (thisValue < otherValue) {
            return -1;
        } else if (thisValue > otherValue) {
            return 1;
        } else {
            return 0;
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "[PlayerName: " + playerName + "] [Lvz Object Id: " + hashCode() + "]" + (ignoredPlayerName != null ? "[IgnoredPlayerName: " + ignoredPlayerName + "]" : "");
    }
}