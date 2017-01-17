package twcore.bots.pokerbot;

import twcore.core.BotAction;
import twcore.core.util.Tools;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PubBuxManager {
    private BotAction botAction;

	public PubBuxManager(BotAction botAction) {
        	this.botAction = botAction;
    	}

    /*
      * Gets the PubBux for a player based on their SubSpace PlayerName
      * @return int
      *		Return's the amount of PubBux player has, any error returns 0.
      */
    public int getPubBuxForPlayerName(String playerName) {
    	int money = 0;
        try {
            //EG VERSION

             ResultSet rs = botAction.SQLQuery(Constants.PUBSTATS_DATABASE_SCHEMA, "SELECT EXISTS(SELECT id FROM egc_accounts WHERE egc_accounts.name = '" + Tools.addSlashesToString(playerName) + "')");
             rs.next();
             if(rs.getBoolean(1) == true) {
            	botAction.SQLClose(rs);
             	ResultSet rs2 = botAction.SQLQuery(Constants.PUBSTATS_DATABASE_SCHEMA, "SELECT balance FROM egc_balances INNER JOIN egc_accounts ON account_id = id WHERE egc_accounts.name = '" + Tools.addSlashesToString(playerName) + "'");
                if(rs2.next())
                    money = rs2.getInt("balance");
             	botAction.SQLClose(rs2);
             } else {
    	    	String query = "CALL Egc_AccountCreate('" + Tools.addSlashesToString(playerName) + "');";
    	    	botAction.sendSmartPrivateMessage(playerName, "This is temporary fix, until if this gets connected to EGC, type !join # again to retry");
            	botAction.SQLBackgroundQuery(Constants.PUBSTATS_DATABASE_SCHEMA, null, query);
		ResultSet rs3 = botAction.SQLQuery(Constants.PUBSTATS_DATABASE_SCHEMA, "SELECT EXISTS(SELECT id FROM egc_accounts WHERE egc_accounts.name = '" + Tools.addSlashesToString(playerName) + "')");
	        rs3.next();
	        if(rs3.getBoolean(1) == true) {
	            botAction.SQLClose(rs);
	            ResultSet rs4 = botAction.SQLQuery(Constants.PUBSTATS_DATABASE_SCHEMA, "SELECT balance FROM egc_balances INNER JOIN egc_accounts ON account_id = id WHERE egc_accounts.name = '" + Tools.addSlashesToString(playerName) + "'");
	            if(rs4.next())
	                money = rs4.getInt("balance");
	            botAction.SQLClose(rs4);
	        }
             }
            //TW VERSION
            /*
            ResultSet rs = botAction.SQLQuery(Constants.PUBSTATS_DATABASE_SCHEMA, "SELECT fnMoney FROM tblPlayerStats WHERE fcName = '" + Tools.addSlashesToString(playerName) + "'");
            if (rs.next())
            	money = rs.getInt("fnMoney");
            botAction.SQLClose(rs);             		  
            */
        } catch (SQLException e) {
            Tools.printStackTrace(e);
        }
        return money;
    }
  
    /*
      * How much PubBux the player name gets changed with could be +/-
      * @param String
      *		SubSpace Player's Name.
      * @param int
      *		Money change to the player could be positive or negitive.
      */
    public void modifyPubBuxForPlayerName(String playerName, int money) {
            //Get freshest money, have to do this because player may have left arena donated to himself and came back.
            int playerMoney = getPubBuxForPlayerName(playerName);
    	    //Check if subtracting negative money will result in no more money left.
   
   	System.out.println("MONEY for " + playerName + " is " + money);
    	    //EG VERSION
    	    if(money < 0) { //subtract egc code 
    	        if((playerMoney + money) <= 0) money = playerMoney;
    	    	String query = "CALL Egc_Remove('" + Tools.addSlashesToString(playerName) + "', '" + Math.abs(money) + "', 'PokerBot Lost Money!');";
            	botAction.SQLBackgroundQuery(Constants.PUBSTATS_DATABASE_SCHEMA, null, query);
    	    } else if (money > 0) { //add egc code
    	    	String query = "CALL Egc_Add('" + Tools.addSlashesToString(playerName) + "', '" + money + "', 'PokerBot Won Money!');";
            	botAction.SQLBackgroundQuery(Constants.PUBSTATS_DATABASE_SCHEMA, null, query);
    	    }
    	    //TW VERSION
    	    /*
    	    String moneyColumn = (money == 0) ? "0" :  "(fnMoney + " + money + ")";

    	    //Change player's money +/-
            String query = "UPDATE tblPlayerStats SET fnMoney = " + moneyColumn + " WHERE fcName = '" + Tools.addSlashesToString(playerName) + "'";
            botAction.SQLBackgroundQuery(Constants.PUBSTATS_DATABASE_SCHEMA, null, query);
            //Guess this is for the logs.
            query = "INSERT INTO tblPlayerDonations (fcName, fcNameTo, fnMoney, fdDate) VALUES('" + Tools.addSlashesToString(botAction.getBotName()) + "', '" + Tools.addSlashesToString(playerName) + "', "
                    + money + ", NOW())";
            botAction.SQLBackgroundQuery(Constants.PUBSTATS_DATABASE_SCHEMA, null, query);
            */
    }
        
    /*
      * How much PubBux gets added or subtracted from Royal Flush Jackpot (Rake)
      * @param int
      *		Money change to the player could be positive or negitive.
      */
    public void addPubBuxToRake(int amount) {
    	modifyPubBuxForPlayerName(botAction.getBotName(), amount);
    }
    
}