package twcore.bots.pokerbot;

import twcore.core.util.Point;
import static twcore.bots.pokerbot.util.PokerUtils.formatMoney;

/**
 * ChipFactory is what draw's the Chips on the tables.
 */
public class ChipFactory {
  private static final int[] chipValues = { 5000000, 1000000, 500000, 100000, 25000, 5000, 1000, 500, 100, 25, 5, 1 };
  private static final int HEIGHT = 16;
  private static final int WIDTH = 20;
  private static final int YSTEP = 4;


  public static void drawChips(int tableIndex, PokerPlayer player)
  {
       //If player is null, then it draw's in the middle of the table for the tableIndex.
       int chipAmount = 0;
       int playerSeat = 10; //10 = Middle of table.
       if(tableIndex < 0 || tableIndex >= pokerbot.pokerTables.length) return;
       Table pokerTable = pokerbot.pokerTables[tableIndex];
       if(pokerTable == null) return;
       
       if(player != null) {
            playerSeat = pokerTable.getPlayerIndex(player.getName());
            chipAmount = player.getBet();
            if(playerSeat == -1) return;
        } else {
        	chipAmount = pokerTable.getTotalPot();
        }
        
        if(chipAmount == 0) return;
        
        Point tableTopLeftInPixels = Constants.tableTopLeftPixelOffsets[tableIndex];
        Point tableChipValuePixels = Constants.ChipValuePixelOffsets[playerSeat];

        int origx = tableTopLeftInPixels.x + tableChipValuePixels.x;
        int origy = tableTopLeftInPixels.y + tableChipValuePixels.y;
        
        int id = (playerSeat + (tableIndex * 10)) + Constants.ID_CHIP_VALUES;
        String chipAmountString = (formatMoney(chipAmount).length() > Constants.TABLE_CHIPVALUE_AMOUNT_LENGTH_MAX) ? formatMoney(chipAmount).substring(0, Constants.TABLE_CHIPVALUE_AMOUNT_LENGTH_MAX) : formatMoney(chipAmount);
        //Clears old chip values and amount first
        if(pokerbot.lvzManager.isLvzByUniqueIdShown(id, Constants.LVZ_SEND_TO_ALL))
	            pokerbot.lvzManager.clearLvzByUniqueId(id);
	
        int count = 0;
        int y = origy;
        int x = origx;
        int chip[] = new int[chipValues.length];
        for(int i = 0; i < chipValues.length; i++)
        {
            chip[i] = chipAmount / chipValues[i];
            chipAmount %= chipValues[i];
            for(int j = 0; j < chip[i]; j++)
            {
	        pokerbot.lvzManager.drawMapImageToAll(id, x, y, Constants.firstChipImageIndex + i);
                y -= YSTEP;
            }

            if(chip[i] != 0 && (i+1) != chipValues.length)
            {
                x += WIDTH;
                if(++count % YSTEP == 0)
                {
                    origy += HEIGHT;
                    x = origx;
                }
            }
            y = origy;
        }
	pokerbot.lvzManager.drawMapStringToAll(id, chipAmountString, origx, origy + 20);
    }
}
