package twcore.bots.pokerbot;

import java.util.Random;

 import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import twcore.core.BotAction;
import twcore.core.game.Player;
import twcore.core.util.Tools;

   /**
 * Text to speech uses bong sounds (%) in team chat only.
 */
public class TextToSpeech {
    public enum SOUNDS {
    	GREETING(105),
	AH1(106), 
	AH2(107), 
	AH3(108),
	ALRIGHT_LETS_GET_IT_ON(109),
	ALRIGHT1(110), 
	ALRIGHT2(111), 
	ATLEAST_IVE_TRIED(112),
	BETTER_HURRY(113), 
	CALM_DOWN(114), 
	CHILL_OUT_MAN(115),
	CLOSE_ENOUGH_HUH(116), 
	COIN_DING(117), 
	COIN_JACKPOT(118),
	COME_ON(119),
	COME_ON_GET_IN(120),
	COME_OVER_HERE(121),
	COUNT_ON_THE_NEXT_ONE(122),
	DAMN_O_WELL(123),
	DID_I_WIN(124),
	DO_YOU_THINK_IM_SUPERMAN(125),
	DONT_GET_SO_ANGRY(126),
	DONT_PANIC(127),
	DONT_TELL_ME_I_KNOW(128),
	DONT_WORRY_ABOUT_IT1(129),
	DONT_WORRY_ABOUT_IT2(130),
	DONT_WORRY_RELAX1(131),
	DONT_WORRY_RELAX2(132),
	DRUM_ZOMBIE_LOOP_MUSIC(133),
	GAUGE_MOVEMENT(134),
	GET_SOME_QUICK_OR_MAKE_IT_QUICK(135),
	GIRLY_HAHA_YES(136),
	GIVE_ME_A_BREAK(137),
	GOT_IT_GOT_IT_SHUT_UP(138),
	HEY_COME_HERE(139),
	HEY_HURRY_UP(140),
	HEY_ID_DO_BETTER(141),
	HEY_YOU_SUCK(142),
	HMM_YES_THE_BEST(143),
	HOP_IN(144),
	HOP_IN_AGAIN(145),
	HOP_IN_AGAIN_SOMETIME(146),
	I_DID_MY_BEST(147),
	I_GOT_IT(148),
	I_KNOW_SHUT_UP(149),
	I_SAVED_YOU_RIGHT_TAKE_IT_EASY(150),
	I_TOLD_YOU_SO(151),
	ILL_CATCH_YOU_LATER(152),
	ILL_GET_THEM_NEXT_TIME(153),
	IM_AT_YOUR_SERVICE(154),
	IM_JUST_TOO_GOOD(155),
	IM_THE_BEST(156),
	IM_TOO_GOOD(157),
	IT_WAS_A_PEICE_OF_CAKE(158),
	IT_WAS_NOTHING_BYE(159),
	IT_WASNT_BAD(160),
	ITS_NOT_A_PROBLEM(161),
	ITS_UNDER_CONTROL1(162),
	ITS_UNDER_CONTROL2(163),
	ITS_YOUR_LUCKY_DAY(164),
	IVE_DONE_BETTER(165),
	JUST_DOING_MY_JOB_TAKE_IT_EASY(166),
	JUST_SHUT_UP_AND_LETS_ROLL(167),
	JUST_SHUT_UP_AND_RELAX(168),
	JUST_SIT_BACK_AND_RELAX(169),
	LETS_ROLL(170),
	MAYBE_NEXT_TIME(171),
	NO_PROBLEM_ILL_GET_YOU_THERE(172),
	NO_WORRIES_REALLY(173),
	NOBODY_COMPARES_TO_ME_LATER(174),
	NOBODYS_PERFECT(175),
	NOT_BAD(176),
	NOTHING_I_CANT_HANDLE_LATER(177),
	NOTHING_I_CANT_HANDLE1(178),
	NOTHING_I_CANT_HANDLE2(179),
	OH_DAM(180),
	OH_GIVE_ME_A_BREAK(181),
	OH_MAN_COME_ON(182),
	OH1(183),
	OH2(184),
	OH3(185),
	OH4(186),
	OH5(187),    
	OKAY_BUT_DONT_FREAK_OUT_ON_ME(188),
	OKAY_HERE_WE_GO1(189),
	OKAY_HERE_WE_GO2(190),
	OKAY_LETS_HAVE_SOME_FUN(191),
	OKAY_LETS_PLAY_IT_COOL_GIRL(192),
	OKAY_THANK_ME_LATER(193),
	PLACE_CHIPS_ON_TABLE(194),
	RELAX(195),
	RELAX_RELAX(196),
	REMEMBER_ME_WHEN_YOU_COME_INTO_YOUR_KINGDOM(197),
	SEE_YA_HOP_IN_AGAIN_SOMETIME(198),
	SEE_YA_LATER_ALLIGATOR(199),
	SHOTGUN(200),
	SHUT_UP_MOVE_YOUR_BUTT(201),
	THANKS_SEE_YOU_SOON(202),
	THAT_WAY_HUH(203),
	THATS_ABOUT_RIGHT(204),
	THATS_THE_WAY_TO_DO_IT(205),
	THIS_JUST_ISNT_MY_DAY(206),
	THIS_SUCKS(207),
	TO_THE_RESCUE(208),
	WE_ALL_MAKE_MISTAKES(209),
	WELL_JUST_WASNT_MY_DAY(210),
	WELL_YOU_WIN_SOME_AND_YOU_LOSE_SOME(211),
	WHAT_AM_I_SUPERMAN(212),
	YA_I_GOT_YOU(213),
	YA_I_GUESS_ILL_SEE_YOU_AROUND_THEN(214),
	YA_IM_THE_KING(215),
	YA_JUST_A_SECOND(216),
	YA_PARTY_TIME(217),
	YA_YA_I_GOT_IT(218),
	YA_YA_I_KNOW(219),
	YA_YA_WELL_BE_RIGHT_THERE(220),
	YEAH_WE_GONNA_HAVE_SOME_FUN(221),
	YO_COME_OVER_HERE(222),
	YO_HURRY_UP_GET_OVER_HERE(223),
	YO_I_JUST_HAD_SOME_BAD_LUCK(224),
	YO_OVER_HERE(225),
	YO_RIGHT_HERE(226),
	YO_WHATS_UP_WITH_YOU(227),
	YO_YO_HOP_IN(228),
	YO_YO_STAY_IN_YOUR_SEAT(229),
	YO_YO_YO_WE_GOTTA_MOVE(230),
	YOUR_OKAY_GET_IN(231),
	YOURE_GONNA_LOVE_THIS(232),
	YOURE_NOT_HURT_GET_IN(233);
        
        public final int id;

        private SOUNDS(int id) {
                this.id = id;
        }
    };
    
    //Lazy way no getters/setters
    public static class SentBong {
    	private int totalBongsSent;
    	private long lastBongSentTime; //System.currentTimeMillis()
    	
    	public SentBong() {}
    	
    	public int getTotalBongsSent() {
    		return totalBongsSent;
    	}
    	
    	public void setTotalBongsSent(int totalBongsSent) {
    		this.totalBongsSent = totalBongsSent;
    	}
    	
    	public long getLastBongSentTime() {
    		return lastBongSentTime;
    	}
    	
    	public void setLastBongSentTime(long lastBongSentTime) {
    		this.lastBongSentTime = lastBongSentTime;
    	}
    }
    /** Players that either lagged out or left the arena while being seated. */
    public static Map<String, SentBong> totalBongsSent = new ConcurrentHashMap<String, SentBong>();


  public static void textToSpeech(String name, String text, BotAction m_botAction)
  {
        Player player = m_botAction.getPlayer(name);
        int frequency = 0;
        if(player != null)
        	frequency = player.getFrequency();
	else
		return;
	
  	//Check if the person sent too much tts commands first.
  	if(!totalBongsSent.containsKey(name)) {
  		SentBong sentBong = new SentBong();
  		sentBong.setLastBongSentTime(1);
  		sentBong.setLastBongSentTime(System.currentTimeMillis());
  		totalBongsSent.put(name, sentBong);
  	} else {
  	        SentBong sentBong = totalBongsSent.get(name);
  	        //Every 30 seconds you can send additional bongs
  	        if( (System.currentTimeMillis() - sentBong.getLastBongSentTime()) >= Tools.TimeInMillis.SECOND * 30)
  	        	 sentBong.setTotalBongsSent((sentBong.getTotalBongsSent() > 0) ? sentBong.getTotalBongsSent() - 1 : 0);
  	        if(sentBong.getTotalBongsSent() > 5) return;
  	        sentBong.setTotalBongsSent(sentBong.getTotalBongsSent() + 1);
  	        sentBong.lastBongSentTime = System.currentTimeMillis();
  		totalBongsSent.put(name, sentBong);
  	}
  
  	String textToSpeech = text.replaceAll("[^a-zA-Z]", "").toLowerCase();
  	int bongSoundId = 0;
  	if(textToSpeech.contains("ahh"))
  		bongSoundId = getRandomBong(SOUNDS.AH1.id, SOUNDS.AH2.id, SOUNDS.AH3.id);
  	else if(textToSpeech.contains("alrightletsgetiton"))
  		bongSoundId = TextToSpeech.SOUNDS.ALRIGHT_LETS_GET_IT_ON.id;
  	else if(textToSpeech.contains("alright"))
  		bongSoundId = getRandomBong(SOUNDS.ALRIGHT1.id, SOUNDS.ALRIGHT2.id);
  	else if(textToSpeech.contains("atleastivetried"))
  		bongSoundId = TextToSpeech.SOUNDS.ATLEAST_IVE_TRIED.id;
  	else if(textToSpeech.contains("betterhurry"))
  		bongSoundId = TextToSpeech.SOUNDS.BETTER_HURRY.id;
  	else if(textToSpeech.contains("calmdown"))
  		bongSoundId = TextToSpeech.SOUNDS.CALM_DOWN.id;		
  	else if(textToSpeech.contains("chilloutman"))
  		bongSoundId = TextToSpeech.SOUNDS.CHILL_OUT_MAN.id;	
  	else if(textToSpeech.contains("closeenoughhuh"))
  		bongSoundId = TextToSpeech.SOUNDS.CLOSE_ENOUGH_HUH.id;
  	else if(textToSpeech.contains("comeon"))
  		bongSoundId = TextToSpeech.SOUNDS.COME_ON.id;
  	else if(textToSpeech.contains("comeongetin"))
  		bongSoundId = TextToSpeech.SOUNDS.COME_ON_GET_IN.id;
  	else if(textToSpeech.contains("comeoverhere"))
  		bongSoundId = TextToSpeech.SOUNDS.COME_OVER_HERE.id;
  	else if(textToSpeech.contains("countonthenextone"))
  		bongSoundId = TextToSpeech.SOUNDS.COUNT_ON_THE_NEXT_ONE.id;
  	else if(textToSpeech.contains("damnowell") || textToSpeech.contains("damnohwell") || textToSpeech.contains("damowell") || textToSpeech.contains("damohwell"))
  		bongSoundId = TextToSpeech.SOUNDS.DAMN_O_WELL.id;
  	else if(textToSpeech.contains("didiwin"))
  		bongSoundId = TextToSpeech.SOUNDS.DID_I_WIN.id;
  	else if(textToSpeech.contains("doyouthinkimsuperman"))
  		bongSoundId = TextToSpeech.SOUNDS.DO_YOU_THINK_IM_SUPERMAN.id;
  	else if(textToSpeech.contains("dontgetsoangry"))
  		bongSoundId = TextToSpeech.SOUNDS.DONT_GET_SO_ANGRY.id;
  	else if(textToSpeech.contains("dontpanic"))
  		bongSoundId = TextToSpeech.SOUNDS.DONT_PANIC.id;
  	else if(textToSpeech.contains("donttellmeiknow"))
  		bongSoundId = TextToSpeech.SOUNDS.DONT_TELL_ME_I_KNOW.id;
  	else if(textToSpeech.contains("dontworryaboutit"))
  		bongSoundId = getRandomBong(SOUNDS.DONT_WORRY_ABOUT_IT1.id, SOUNDS.DONT_WORRY_ABOUT_IT2.id);
  	else if(textToSpeech.contains("dontworryrelax"))
  		bongSoundId = getRandomBong(SOUNDS.DONT_WORRY_RELAX1.id, SOUNDS.DONT_WORRY_RELAX2.id);
  	else if(textToSpeech.contains("zombie"))
  		bongSoundId = SOUNDS.DRUM_ZOMBIE_LOOP_MUSIC.id;
  	else if(textToSpeech.contains("getsomequickormakeitquick"))
  		bongSoundId = SOUNDS.GET_SOME_QUICK_OR_MAKE_IT_QUICK.id;
  	else if(textToSpeech.contains("givemeabreak"))
  		bongSoundId = SOUNDS.GIVE_ME_A_BREAK.id;
  	else if(textToSpeech.contains("gotitgotitshutup"))
  		bongSoundId = SOUNDS.GOT_IT_GOT_IT_SHUT_UP.id;
  	else if(textToSpeech.contains("hahayesgirl") || textToSpeech.contains("girlyhahayes"))
  		bongSoundId = SOUNDS.GIRLY_HAHA_YES.id;
  	else if(textToSpeech.contains("heycomehere"))
  		bongSoundId = SOUNDS.HEY_COME_HERE.id;
  	else if(textToSpeech.contains("heyhurryup"))
  		bongSoundId = SOUNDS.HEY_HURRY_UP.id;
  	else if(textToSpeech.contains("heyiddobetter"))
  		bongSoundId = SOUNDS.HEY_ID_DO_BETTER.id;
  	else if(textToSpeech.contains("heyyousuck") || textToSpeech.contains("heyusuck") || textToSpeech.contains("heyyousuk") || textToSpeech.contains("heyusuk") || textToSpeech.contains("heyyousux")  || textToSpeech.contains("heyusux"))
  		bongSoundId = SOUNDS.HEY_YOU_SUCK.id;	
  	else if(textToSpeech.contains("hmmyesthebest") || textToSpeech.contains("hmyesthebest"))
  		bongSoundId = SOUNDS.HMM_YES_THE_BEST.id;
  	else if(textToSpeech.contains("hopin"))
  		bongSoundId = SOUNDS.HOP_IN.id;	
  	else if(textToSpeech.contains("hopinagain"))
  		bongSoundId = SOUNDS.HOP_IN_AGAIN.id;	
  	else if(textToSpeech.contains("hopinagainsometime"))
  		bongSoundId = SOUNDS.HOP_IN_AGAIN_SOMETIME.id;
  	else if(textToSpeech.contains("ididmybest"))
  		bongSoundId = SOUNDS.I_DID_MY_BEST.id;
  	else if(textToSpeech.contains("igotit"))
  		bongSoundId = SOUNDS.I_GOT_IT.id;
  	else if(textToSpeech.contains("iknowshutup"))
  		bongSoundId = SOUNDS.I_KNOW_SHUT_UP.id;	
  	else if(textToSpeech.contains("isavedyourighttakeiteasy"))
  		bongSoundId = SOUNDS.I_SAVED_YOU_RIGHT_TAKE_IT_EASY.id;
  	else if(textToSpeech.contains("itoldyouso"))
  		bongSoundId = SOUNDS.I_TOLD_YOU_SO.id;
  	else if(textToSpeech.contains("illcatchyoulater") || textToSpeech.contains("catchyoulater"))
  		bongSoundId = TextToSpeech.SOUNDS.ILL_CATCH_YOU_LATER.id;
  	else if(textToSpeech.contains("illgetthemnexttime"))
  		bongSoundId = SOUNDS.ILL_GET_THEM_NEXT_TIME.id;
  	else if(textToSpeech.contains("imatyourservice"))
  		bongSoundId = SOUNDS.IM_AT_YOUR_SERVICE.id;
  	else if(textToSpeech.contains("imjusttoogood") || textToSpeech.contains("imjusttogood"))
  		bongSoundId = SOUNDS.IM_JUST_TOO_GOOD.id;
  	else if(textToSpeech.contains("imthebest"))
  		bongSoundId = SOUNDS.IM_THE_BEST.id;
  	else if(textToSpeech.contains("imtoogood") || textToSpeech.contains("imtogood"))
  		bongSoundId = SOUNDS.IM_TOO_GOOD.id;
  	else if(textToSpeech.contains("itwasapeiceofcake"))
  		bongSoundId = SOUNDS.IT_WAS_A_PEICE_OF_CAKE.id;
  	else if(textToSpeech.contains("itwasnothingbye"))
  		bongSoundId = SOUNDS.IT_WAS_NOTHING_BYE.id;
  	else if(textToSpeech.contains("itwasntbad"))
  		bongSoundId = SOUNDS.IT_WASNT_BAD.id;
  	else if(textToSpeech.contains("itsnotaproblem"))
  		bongSoundId = SOUNDS.ITS_NOT_A_PROBLEM.id;
  	else if(textToSpeech.contains("itsundercontrol"))
  		bongSoundId = getRandomBong(SOUNDS.ITS_UNDER_CONTROL1.id, SOUNDS.ITS_UNDER_CONTROL2.id);
  	else if(textToSpeech.contains("itsyourluckyday") || textToSpeech.contains("itsurluckyday"))
  		bongSoundId = SOUNDS.ITS_YOUR_LUCKY_DAY.id;
  	else if(textToSpeech.contains("ivedonebetter"))
  		bongSoundId = SOUNDS.IVE_DONE_BETTER.id;
  	else if(textToSpeech.contains("justdoingmyjobtakeiteasy"))
  		bongSoundId = SOUNDS.JUST_DOING_MY_JOB_TAKE_IT_EASY.id;
  	else if(textToSpeech.contains("justshutupandletsroll"))
  		bongSoundId = SOUNDS.JUST_SHUT_UP_AND_LETS_ROLL.id;
  	else if(textToSpeech.contains("justshutupandrelax"))
  		bongSoundId = SOUNDS.JUST_SHUT_UP_AND_RELAX.id;
  	else if(textToSpeech.contains("justsitbackandrelax") || textToSpeech.contains("justsitbackanrelax") || textToSpeech.contains("justsitbacknrelax") || textToSpeech.contains("justsitbaknrelax"))
  		bongSoundId = SOUNDS.JUST_SIT_BACK_AND_RELAX.id;
  	else if(textToSpeech.contains("letsroll"))
  		bongSoundId = SOUNDS.LETS_ROLL.id;
  	else if(textToSpeech.contains("maybenexttime"))
  		bongSoundId = SOUNDS.MAYBE_NEXT_TIME.id;
  	else if(textToSpeech.contains("noproblemillgetyouthere"))
  		bongSoundId = SOUNDS.NO_PROBLEM_ILL_GET_YOU_THERE.id;
  	else if(textToSpeech.contains("noworriesreally") || textToSpeech.contains("noworrysreally"))
  		bongSoundId = SOUNDS.NO_WORRIES_REALLY.id;	
  	else if(textToSpeech.contains("nobodycomparestomelater") || textToSpeech.contains("noonecomparestomelater"))
  		bongSoundId = SOUNDS.NOBODY_COMPARES_TO_ME_LATER.id;
  	else if(textToSpeech.contains("nobodysperfect") || textToSpeech.contains("nobodyisperfect") || textToSpeech.contains("nobodyperfect"))
  		bongSoundId = SOUNDS.NOBODYS_PERFECT.id;
  	else if(textToSpeech.contains("notbad"))
  		bongSoundId = SOUNDS.NOT_BAD.id;
  	else if(textToSpeech.contains("nothingicanthandlelater"))
  		bongSoundId = SOUNDS.NOTHING_I_CANT_HANDLE_LATER.id;
  	else if(textToSpeech.contains("nothingicanthandle"))
  		bongSoundId = getRandomBong(SOUNDS.NOTHING_I_CANT_HANDLE1.id, SOUNDS.NOTHING_I_CANT_HANDLE2.id);
  	else if(textToSpeech.contains("ohdam") || textToSpeech.contains("ohhdam"))
  		bongSoundId = SOUNDS.OH_DAM.id;
  	else if(textToSpeech.contains("ohgivemeabreak"))
  		bongSoundId = SOUNDS.OH_GIVE_ME_A_BREAK.id;
  	else if(textToSpeech.contains("ohmancomeon"))
  		bongSoundId = SOUNDS.OH_MAN_COME_ON.id;
  	else if(textToSpeech.contains("ohh"))
  		bongSoundId = getRandomBong(SOUNDS.OH1.id, SOUNDS.OH2.id,  SOUNDS.OH3.id, SOUNDS.OH4.id, SOUNDS.OH5.id);
  	else if(textToSpeech.contains("okaybutdontfreakoutonme"))
  		bongSoundId = SOUNDS.OKAY_BUT_DONT_FREAK_OUT_ON_ME.id;
  	else if(textToSpeech.contains("okayherewego"))
  		bongSoundId =  getRandomBong(SOUNDS.OKAY_HERE_WE_GO1.id, SOUNDS.OKAY_HERE_WE_GO2.id);
  	else if(textToSpeech.contains("okayletshavesomefun"))
  		bongSoundId =  SOUNDS.OKAY_LETS_HAVE_SOME_FUN.id;
  	else if(textToSpeech.contains("okayletsplayitcool") || textToSpeech.contains("girlyokayletsplayitcool") )
  		bongSoundId =  SOUNDS.OKAY_LETS_PLAY_IT_COOL_GIRL.id;
  	else if(textToSpeech.contains("okaythankmelater"))
  		bongSoundId =  SOUNDS.OKAY_THANK_ME_LATER.id;
  	else if(textToSpeech.contains("relax"))
  		bongSoundId =  SOUNDS.RELAX.id;
  	else if(textToSpeech.contains("relaxrelax"))
  		bongSoundId =  SOUNDS.RELAX_RELAX.id;
  	else if(textToSpeech.contains("remembermewhenyoucomeintoyourkingdom"))
  		bongSoundId =  SOUNDS.REMEMBER_ME_WHEN_YOU_COME_INTO_YOUR_KINGDOM.id;
  	else if(textToSpeech.contains("seeyahopinagainsometime"))
  		bongSoundId =  SOUNDS.SEE_YA_HOP_IN_AGAIN_SOMETIME.id;	
  	else if(textToSpeech.contains("seeyalateralligator"))
  		bongSoundId =  SOUNDS.SEE_YA_LATER_ALLIGATOR.id;
  	else if(textToSpeech.contains("shutupmoveyourbutt"))
  		bongSoundId =  SOUNDS.SHUT_UP_MOVE_YOUR_BUTT.id;
  	else if(textToSpeech.contains("thanksseeyousoon"))
  		bongSoundId =  SOUNDS.THANKS_SEE_YOU_SOON.id;
  	else if(textToSpeech.contains("thatwayhuh") || textToSpeech.contains("thatway"))
  		bongSoundId =  SOUNDS.THAT_WAY_HUH.id;
  	else if(textToSpeech.contains("thatsaboutright"))
  		bongSoundId =  SOUNDS.THATS_ABOUT_RIGHT.id;
  	else if(textToSpeech.contains("thatsthewaytodoit"))
  		bongSoundId =  SOUNDS.THATS_THE_WAY_TO_DO_IT.id;
  	else if(textToSpeech.contains("thisjustisntmyday"))
  		bongSoundId =  SOUNDS.THIS_JUST_ISNT_MY_DAY.id;
  	else if(textToSpeech.contains("thissucks"))
  		bongSoundId =  SOUNDS.THIS_SUCKS.id;
  	else if(textToSpeech.contains("totherescue"))
  		bongSoundId =  SOUNDS.TO_THE_RESCUE.id;
  	else if(textToSpeech.contains("weallmakemistakes"))
  		bongSoundId =  SOUNDS.WE_ALL_MAKE_MISTAKES.id;
  	else if(textToSpeech.contains("welljustwasntmyday"))
  		bongSoundId =  SOUNDS.WELL_JUST_WASNT_MY_DAY.id;
  	else if(textToSpeech.contains("wellyouwinsomeandyoulosesome"))
  		bongSoundId =  SOUNDS.WELL_YOU_WIN_SOME_AND_YOU_LOSE_SOME.id;
  	else if(textToSpeech.contains("whatamisuperman"))
  		bongSoundId =  SOUNDS.WHAT_AM_I_SUPERMAN.id;
  	else if(textToSpeech.contains("yaigotyou"))
  		bongSoundId =  SOUNDS.YA_I_GOT_YOU.id;
  	else if(textToSpeech.contains("yaiguessillseeyouaroundthen"))
  		bongSoundId =  SOUNDS.YA_I_GUESS_ILL_SEE_YOU_AROUND_THEN.id;
  	else if(textToSpeech.contains("yaimtheking"))
  		bongSoundId =  SOUNDS.YA_IM_THE_KING.id;
  	else if(textToSpeech.contains("yajustasecond"))
  		bongSoundId =  SOUNDS.YA_JUST_A_SECOND.id;
  	else if(textToSpeech.contains("yapartytime"))
  		bongSoundId =  SOUNDS.YA_PARTY_TIME.id;
  	else if(textToSpeech.contains("yayaigotit"))
  		bongSoundId =  SOUNDS.YA_YA_I_GOT_IT.id;
  	else if(textToSpeech.contains("yayaiknow"))
  		bongSoundId =  SOUNDS.YA_YA_I_KNOW.id;
  	else if(textToSpeech.contains("yayawellberightthere"))
  		bongSoundId =  SOUNDS.YA_YA_WELL_BE_RIGHT_THERE.id;
  	else if(textToSpeech.contains("yeahwegonnahavesomefun"))
  		bongSoundId =  SOUNDS.YEAH_WE_GONNA_HAVE_SOME_FUN.id;
  	else if(textToSpeech.contains("yocomeoverhere"))
  		bongSoundId =  SOUNDS.YO_COME_OVER_HERE.id;
  	else if(textToSpeech.contains("yohurryupgetoverhere"))
  		bongSoundId =  SOUNDS.YO_HURRY_UP_GET_OVER_HERE.id;
  	else if(textToSpeech.contains("yoijusthadsomebadluck"))
  		bongSoundId =  SOUNDS.YO_I_JUST_HAD_SOME_BAD_LUCK.id;
  	else if(textToSpeech.contains("yooverhere"))
  		bongSoundId =  SOUNDS.YO_OVER_HERE.id;		
  	else if(textToSpeech.contains("yorighthere"))
  		bongSoundId =  SOUNDS.YO_RIGHT_HERE.id;		
  	else if(textToSpeech.contains("yowhatsupwithyou"))
  		bongSoundId =  SOUNDS.YO_WHATS_UP_WITH_YOU.id;
  	else if(textToSpeech.contains("yoyohopin"))
  		bongSoundId =  SOUNDS.YO_YO_HOP_IN.id;
  	else if(textToSpeech.contains("yoyostayinyourseat"))
  		bongSoundId =  SOUNDS.YO_YO_STAY_IN_YOUR_SEAT.id;
  	else if(textToSpeech.contains("yoyoyowegottamove"))
  		bongSoundId =  SOUNDS.YO_YO_YO_WE_GOTTA_MOVE.id;
  	else if(textToSpeech.contains("yourokaygetin"))
  		bongSoundId =  SOUNDS.YOUR_OKAY_GET_IN.id;	
  	else if(textToSpeech.contains("youregonnalovethis"))
  		bongSoundId =  SOUNDS.YOURE_GONNA_LOVE_THIS.id;	
  	else if(textToSpeech.contains("yourenothurtgetin"))
  		bongSoundId =  SOUNDS.YOURE_NOT_HURT_GET_IN.id;	
  	m_botAction.sendUnfilteredTargetTeamMessage(frequency, name + " said!", bongSoundId);
  }
  
  public static int getRandomBong(int... ints) {
	return ints[new Random().nextInt(ints.length)];
  }
}
