package twcore.bots.pokerbot;

import twcore.core.util.Point;

/**
  * Class containing all the constant values that maybe tweaked at any time.
  */
public final class Constants {
	public static String VERSION = "1.0";
   //All of charset (yellow) All these are 8x8 [shrtfont], tallfont is 8x12
   //All PURE BLACK COLORS ARE BASED ON 8/16/32 BIT CONTINUUM MODE STARTING AT RGB[10,10,10]
   //Any colors less then RGB[10,10,10] are transparent in certain continuum color modes.
   
    //****************** These are non-final constants. ******************
    /** Whether players will always call the showdown, or fold when no chance. */
    public static boolean ALWAYS_CALL_SHOWDOWN = false;
    public static boolean IsZombieGameEnabled = true;
    /** How much seconds to wait before attempting to start a new game */
    public static int PAUSE_SECONDS_UNTIL_NEW_GAME = 10;
    public static float RAKE_DEDUCTION_PERCENTAGE = 0.5f;
    public static int MINIGAME_HUMAN_WINNINGS_PERCENTAGE = 80;
    public static int MINIGAME_ZOMBIE_WINNINGS_PERCENTAGE = 20; 
    //*************************************************************
    public static String PUBSTATS_DATABASE_SCHEMA = "eg";

    public static final int MAX_LVZ_OBJECTS = 5000;
    public static final int START_MAP_LVZ_OBJECTS = 1; //1 ~ 5000 [5000 objs]
    public static final int START_SCREEN_LVZ_OBJECTS = 10000; //10000 ~ 14999 [5000 objs]

    public static final int MAX_POKER_TABLES = 8;
    public static final int TABLE_PLAYERNAME_LENGTH_MAX = 14; //23 is TW max.
    public static final int TABLE_PLAYERMONEY_LENGTH_MAX = 14;
    public static final int TABLE_CHIPVALUE_AMOUNT_LENGTH_MAX = 12; 

    // ----------------------------------- Unique ID's -----------------------------------
    //LVZ ID's for drawString/drawImage/drawScreenImage.
    public static final int ID_RULES = 0; //[0] shared among any player at any time.
    //* Below is based on [1-10 per table] x 8 tables. *
    public static final int ID_PLAYER_NAME = 1; //([0-9] + (7*10))+1  Highest:[10+70=80][1-80], [[0-79]+1]
    public static final int ID_PLAYER_CARDS_DOWN = 81; //([0-9] + (7*10))+81 Highest: [81-160], [[0-79]+1]
    public static final int ID_PLAYER_CARDS_UP = 161; //([0-9] + (7*10))+161 Highest: [161-240], [[0-79]+1]
    //* Below is based on [1 per table] x 8 tables. *
    public static final int ID_DEALER_CHIP = 241; //(7)+241  Highest: [241+7=248][241-248]
    //* Below is based on [5 per table] x 8 tables. */
    public static final int ID_COMMUNITY_CARDS = 249; //[0-4] + (7*5)+249 Highest: [249-288], [[0-39]+1]
    //* Below is based on [1-10 per table] x 8 tables. TODO: Move it under ID_PLAYER_NAME *
    public static final int ID_PLAYER_MONEY = 289; //([0-9] + (7*10))+289  Highest:[10+70=80][289-368], [[0-79]+1]
    //* Below is based on [1-11 per table] x 8 tables. [10 players + 1 middle of table]
    public static final int ID_CHIP_VALUES = 369; //([0-10] + (7*11))+369  Highest:[11+77=88][369-456], [[0-87]+1]
    /* Below is based on [1-10 per table] x 8 tables. */
    public static final int ID_TABLE_BLINKER = 457; //([0-9] + (7*10))+457  Highest:[10+70=80][457-536], [[0-79]+1]
     
    public static final int ID_PLAYER_HELP = 537; //this is just 1, shared among all actors on all tables.
    public static final int ID_PLAYER_GAUGE = 538; //this is just 1, shared among all actors on all tables.
    public static final int ID_ROYAL_FLUSH_JACKPOT_LABEL = 539; //this is just 1, shared among all players in arena.
    public static final int ID_ROYAL_FLUSH_JACKPOT_VALUE = 540; //this is just 1, shared among all players in arena.
    
    // ----------------------------------- End of Unique ID's -----------------------------------
    
    //These are the starting offsets in the poker.lvz ini file.
    public static final int firstFontLetterImageIndex = 2;
    public static final int firstTableCardImageIndex = 97;
    public static final int firstPlayerCardImageIndex = 150;
    public static final int dealerImageIndex = 203;
    public static final int checkBetHelpImageIndex = 204;
    public static final int checkRaiseHelpImageIndex = 205;
    public static final int callBetHelpImageIndex = 206;
    public static final int callRaiseHelpImageIndex = 207;
    public static final int rulesImageIndex = 208;
    public static final int firstGuageImageIndex = 209; //209~218 [10 guages]
    public static final int firstChipImageIndex = 219; //219~230 [12 chip values]
    public static final int tableBlinkerImageIndex = 231;
    
    public static final int checkBetHelpWidthX = -170; //340 width / 2.
    public static final int checkRaiseHelpWidthX = -170; //340 width / 2.
    public static final int callBetHelpWidthX = -161; //322 width / 2.
    public static final int callRaiseHelpWidthX = -161; //322 width / 2.

    //This follows the same format as [shrtfont.bm2] in continuum graphics minus the first blank tile, it's tranparent anyways.
    public static final String FONT = "!\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~ß";

    //Each of the 8 tables top/left corner in pixels. [This info also in poker.lvz.ini]
    public static final Point[] tableTopLeftPixelOffsets = {
        new Point(6848,7728), new Point(7584,7728), new Point(8320,7728), new Point(9056,7728),
        new Point(6848,8272), new Point(7584,8272), new Point(8320,8272), new Point(9056,8272)
    };

    //This is based on 10 seats for one table, where first seat map coordinates starts at 431, 487
    public static final Point[] tableSeatWarpPoints = {
        new Point(431, 487), new Point(439, 487), new Point(447, 487), new Point(455, 487), new Point(455, 495),
        new Point(455, 503), new Point(447, 503), new Point(439, 503), new Point(431, 503), new Point(431, 495)
    };
    
    public static final Point[] miniGameZombieWarpPoints = {
    	new Point(487, 426), new Point(487, 429), new Point(487, 432), new Point(487, 435), 
    	new Point(487, 438), new Point(487, 441), new Point(487, 444), new Point(487, 447), 
    	new Point(487, 450), new Point(487, 453), new Point(487, 456), new Point(487, 459), 
    	new Point(487, 462), new Point(487, 465), new Point(487, 468), new Point(487, 471), 
    	new Point(487, 474), new Point(490, 474), new Point(493, 474), new Point(496, 474),
    	new Point(499, 474), new Point(502, 474), new Point(505, 474), new Point(508, 474), 
    	new Point(511, 474), new Point(514, 474), new Point(517, 474), new Point(520, 474), 
    	new Point(523, 474), new Point(526, 474), new Point(529, 474), new Point(532, 474), 
    	new Point(535, 474), new Point(535, 471), new Point(535, 468), new Point(535, 465), 
    	new Point(535, 462), new Point(535, 459), new Point(535, 456), new Point(535, 453), 
    	new Point(535, 450), new Point(535, 447), new Point(535, 444), new Point(535, 441), 
    	new Point(535, 438), new Point(535, 435), new Point(535, 432), new Point(535, 429), 
        new Point(535, 426), new Point(532, 426), new Point(529, 426), new Point(526, 426), 
        new Point(523, 426), new Point(520, 426), new Point(517, 426), new Point(514, 426), 
        new Point(511, 426), new Point(508, 426), new Point(505, 426), new Point(502, 426),
        new Point(499, 426), new Point(496, 426), new Point(493, 426), new Point(490, 426)
    };
    
    public static final Point miniGameHumanWarpPoint = new Point(511, 450);
    public static final int HumanRoamRadius = 21;
     
    //From one table to the next table offset, this is based on first seat map coordinates in both x,y directions.
    //How to get seat 1 on table 8?
    //(3*46)+431, (1*34)+487 = [569,521]
    //How to get seat 8 on table 8?
    //(3*46)+439, (1*34)+503 = [577,537]
    public static final Point tableSeatWarpPointOffset = new Point(46, 34);
    
    //Offsetted by tableTopLeftPixelOffsets 
    //2 cards x 10 players = 20 elements.
    public static final Point[][] playerCardsPixelOffsets = {
        { new Point(16, 13), new Point(46, 13) }, //top left
        { new Point(144, 13), new Point(174, 13) },
        { new Point(272, 13), new Point(302, 13) },
        { new Point(400, 13), new Point(430, 13) }, //top right
        { new Point(400, 141),  new Point(430, 141) },
        { new Point(400, 269),  new Point(430, 269) }, //bottom left
        { new Point(272, 269),  new Point(302, 269) },
        { new Point(144, 269),  new Point(174, 269) },
        { new Point(16, 269),  new Point(46, 269) },  //bottom right
        { new Point(16, 141),  new Point(46, 141) }
    };
    
    //Offsetted by tableTopLeftPixelOffsets 
    //Middle 5 table cards. [Since Y coordinate never changes maybe use math for this?]
    public static final Point[] tableCardsPixelOffsets = {
        new Point(121, 165), new Point(172, 165), new Point(223, 165), new Point(274, 165), new Point(325, 165)
    };
    
    //Offsetted by tableTopLeftPixelOffsets 
    //Dealer chip for all 10 players.
    public static final Point[] dealerChipPixelOffsets = {
        new Point(74, 92), new Point(202, 92), new Point(330, 92), new Point(458, 92), new Point(458, 220),
        new Point(458, 348), new Point(330, 348), new Point(202, 348), new Point(74, 348), new Point(74, 220)
    };
    
    //Offsetted by tableTopLeftPixelOffsets 
    //Table blinker for all 10 players.
    public static final Point[] tableBlinkerPixelOffsets = {
        new Point(16, 48), new Point(144, 48), new Point(272, 48), new Point(400, 48), new Point(400, 176),
        new Point(400, 304), new Point(272, 304), new Point(144, 304), new Point(16, 304), new Point(16, 176)
    };
    
    //Offsetted by tableTopLeftPixelOffsets 
    //Since not enough room playernames can have maximum of 14 characters. [14*8=112 pixels].
    public static final Point[] tablePlayerNamesPixelOffsets = {
        new Point(0, 2), new Point(128, 2), new Point(256, 2), new Point(384, 2), new Point(384, 130),
        new Point(384, 258), new Point(256, 258),  new Point(128, 258), new Point(0, 258),  new Point(0, 130)
    };
    
    //Offsetted by tableTopLeftPixelOffsets 
    //tablePlayerMoneyPixelOffsets has a maximum of 14 characters [14*8=112 pixels]
    public static final Point[] tablePlayerMoneyPixelOffsets = {
	new Point(0, 114), new Point(128, 114), new Point(256, 114), new Point(384, 114), new Point(384, 242),
	new Point(384, 370), new Point(256, 370), new Point(128, 370), new Point(0, 370),  new Point(0, 242)
    };
    
    //Offsetted by tableTopLeftPixelOffsets 
    //Where to start drawing Chips for all 10 players + middle table.
    public static final Point[] ChipValuePixelOffsets = {
	new Point(0, -61), new Point(128, -61), new Point(256, -61), new Point(384, -61), new Point(512, 195),
	new Point(384, 447), new Point(256, 447), new Point(128, 447), new Point(0, 447), new Point(-116, 195),
	new Point(201, 121)
    };

    //Messages that you could use to write stuff on the table.
    public static final Point tableMessageOnePixelOffsets = new Point(120, 136);
    public static final Point tableMessageTwoPixelOffsets = new Point(120, 147);
    public static final Point tableMessageThreePixelOffsets = new Point(120, 185);
    public static final Point tableMessageFourPixelOffsets = new Point(120, 196);
    public static final Point tableMessageFivePixelOffsets = new Point(120, 207);  
    
    public static final String LVZ_SEND_TO_ALL = "-1";
    public static final String LVZ_SEND_TO_ALL_BUT_ONE = "-2";
}