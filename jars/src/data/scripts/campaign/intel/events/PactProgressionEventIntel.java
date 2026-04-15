package data.scripts.campaign.intel.events;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.StoryPointActionDelegate;
import com.fs.starfarer.api.campaign.econ.EconomyAPI.EconomyUpdateListener;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.campaign.listeners.ListenerUtil;
import com.fs.starfarer.api.combat.MutableStatWithTempMods;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.command.WarSimScript.LocationDanger;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Sounds;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.bases.LuddicPathBaseIntel;
import com.fs.starfarer.api.impl.campaign.intel.bases.PirateBaseIntel;
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel;
import com.fs.starfarer.api.impl.campaign.intel.events.BaseFactorTooltip;
import com.fs.starfarer.api.impl.campaign.intel.events.EventFactor;
import com.fs.starfarer.api.impl.campaign.intel.events.HABlowbackFactor;
import com.fs.starfarer.api.impl.campaign.intel.events.HAColonyDefensesFactor;
import com.fs.starfarer.api.impl.campaign.intel.events.HAShipsDestroyedFactorHint;
import com.fs.starfarer.api.impl.campaign.intel.events.HegemonyAICoresActivityCause;
import com.fs.starfarer.api.impl.campaign.intel.events.HegemonyHostileActivityFactor;
import com.fs.starfarer.api.impl.campaign.intel.events.HostileActivityEventIntel;
import com.fs.starfarer.api.impl.campaign.intel.events.KantasProtectionPirateActivityCause2;
import com.fs.starfarer.api.impl.campaign.intel.events.KantasWrathPirateActivityCause2;
import com.fs.starfarer.api.impl.campaign.intel.events.LuddicChurchHostileActivityFactor;
import com.fs.starfarer.api.impl.campaign.intel.events.LuddicChurchStandardActivityCause;
import com.fs.starfarer.api.impl.campaign.intel.events.LuddicPathAgreementHostileActivityCause2;
import com.fs.starfarer.api.impl.campaign.intel.events.LuddicPathHostileActivityFactor;
import com.fs.starfarer.api.impl.campaign.intel.events.PerseanLeagueHostileActivityFactor;
import com.fs.starfarer.api.impl.campaign.intel.events.PirateBasePirateActivityCause2;
import com.fs.starfarer.api.impl.campaign.intel.events.PirateHostileActivityFactor;
import com.fs.starfarer.api.impl.campaign.intel.events.RemnantHostileActivityFactor;
import com.fs.starfarer.api.impl.campaign.intel.events.RemnantNexusActivityCause;
import com.fs.starfarer.api.impl.campaign.intel.events.SindrianDiktatHostileActivityFactor;
import com.fs.starfarer.api.impl.campaign.intel.events.SindrianDiktatStandardActivityCause;
import com.fs.starfarer.api.impl.campaign.intel.events.StandardLuddicPathActivityCause2;
import com.fs.starfarer.api.impl.campaign.intel.events.StandardPerseanLeagueActivityCause;
import com.fs.starfarer.api.impl.campaign.intel.events.StandardPirateActivityCause2;
import com.fs.starfarer.api.impl.campaign.intel.events.TriTachyonHostileActivityFactor;
import com.fs.starfarer.api.impl.campaign.intel.events.TriTachyonStandardActivityCause;
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel.EventStageData;
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel.RandomizedStageType;
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel.StageIconSize;
import com.fs.starfarer.api.impl.campaign.intel.events.HostileActivityEventIntel.HAEFactorDangerData;
import com.fs.starfarer.api.impl.campaign.intel.events.HostileActivityEventIntel.HAERandomEventData;
import com.fs.starfarer.api.impl.campaign.intel.events.HostileActivityEventIntel.HAEStarSystemDangerData;
import com.fs.starfarer.api.impl.campaign.intel.events.HostileActivityEventIntel.Stage;
import com.fs.starfarer.api.impl.campaign.rulecmd.HA_CMD;
import com.fs.starfarer.api.impl.campaign.rulecmd.SetStoryOption.BaseOptionStoryPointActionDelegate;
import com.fs.starfarer.api.impl.campaign.rulecmd.SetStoryOption.StoryOptionParams;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.IntelUIAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipCreator;
import com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipLocation;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;

public class PactProgressionEventIntel extends BaseEventIntel implements EconomyUpdateListener, FleetEventListener {

	public static enum Stage {
		START,
		VARIANTS,
		AUTOMATED,
		PHASEOUT,
		CAPITALS,
		FINISHED
	}
	
	public static int PROGRESS_1 = 1 * 600;
	public static int PROGRESS_2 = 3 * 600;
	public static int PROGRESS_3 = 4 * 600;
	public static int PROGRESS_4 = 6 * 600;
	public static int PROGRESS_MAX = 8 * 600;

	public static String KEY = "$espc_doctrineevent_ref";

	public static PactProgressionEventIntel get() {
		return (PactProgressionEventIntel) Global.getSector().getMemoryWithoutUpdate().get(KEY);
	}
	
	protected int blowback;
	protected Map<String, MutableStatWithTempMods> systemSpawnMults = new LinkedHashMap<String, MutableStatWithTempMods>();
	
	public PactProgressionEventIntel() {
		super();
		
		//Global.getSector().getEconomy().addUpdateListener(this);
		
		Global.getSector().getMemoryWithoutUpdate().set(KEY, this);
		
		setup();
		
		// now that the event is fully constructed, add it and send notification
		Global.getSector().getIntelManager().addIntel(this);
	}
	
	protected void setup() {
		factors.clear();
		stages.clear();
		
		setMaxProgress(PROGRESS_MAX);
		
		addStage(Stage.START, 0, StageIconSize.MEDIUM);
		addStage(Stage.VARIANTS, PROGRESS_1, StageIconSize.MEDIUM);
		addStage(Stage.AUTOMATED, PROGRESS_2, StageIconSize.LARGE);
		addStage(Stage.PHASEOUT, PROGRESS_3, StageIconSize.MEDIUM);
		addStage(Stage.CAPITALS, PROGRESS_4, StageIconSize.LARGE);
		addStage(Stage.FINISHED, PROGRESS_MAX, StageIconSize.MEDIUM);
		
		setProgress(PROGRESS_3 + 400);

		
		getDataFor(Stage.START).keepIconBrightWhenLaterStageReached = true;
		getDataFor(Stage.VARIANTS).keepIconBrightWhenLaterStageReached = true;
		getDataFor(Stage.AUTOMATED).keepIconBrightWhenLaterStageReached = true;
		getDataFor(Stage.PHASEOUT).keepIconBrightWhenLaterStageReached = true;
		getDataFor(Stage.CAPITALS).keepIconBrightWhenLaterStageReached = true;
		getDataFor(Stage.FINISHED).keepIconBrightWhenLaterStageReached = true;
		
		/*
		Global.getSector().getListenerManager().removeListenerOfClass(PirateHostileActivityFactor.class);
		
		addFactor(new HAColonyDefensesFactor());
		addFactor(new HAShipsDestroyedFactorHint());
		
		addFactor(new HABlowbackFactor());
		
		PirateHostileActivityFactor pirate = new PirateHostileActivityFactor(this);
		addActivity(pirate, new KantasProtectionPirateActivityCause2(this));
		addActivity(pirate, new StandardPirateActivityCause2(this));
		addActivity(pirate, new PirateBasePirateActivityCause2(this));
		addActivity(pirate, new KantasWrathPirateActivityCause2(this));
		
		LuddicPathHostileActivityFactor path = new LuddicPathHostileActivityFactor(this);
		addActivity(path, new LuddicPathAgreementHostileActivityCause2(this));
		addActivity(path, new StandardLuddicPathActivityCause2(this));
		
		addActivity(new PerseanLeagueHostileActivityFactor(this), new StandardPerseanLeagueActivityCause(this));
		addActivity(new TriTachyonHostileActivityFactor(this), new TriTachyonStandardActivityCause(this));
		addActivity(new LuddicChurchHostileActivityFactor(this), new LuddicChurchStandardActivityCause(this));
		addActivity(new SindrianDiktatHostileActivityFactor(this), new SindrianDiktatStandardActivityCause(this));
		addActivity(new HegemonyHostileActivityFactor(this), new HegemonyAICoresActivityCause(this));
		addActivity(new RemnantHostileActivityFactor(this), new RemnantNexusActivityCause(this));
		
		ListenerUtil.finishedAddingCrisisFactors(this);
		*/
	}
	
	protected Object readResolve() {
		// if (systemSpawnMults == null) {
		//	systemSpawnMults = new LinkedHashMap<String, MutableStatWithTempMods>();
		return this;
	}
	
	
	@Override
	protected void notifyEnding() {
		super.notifyEnding();
	}

	@Override
	protected void notifyEnded() {
		super.notifyEnded();
		Global.getSector().getMemoryWithoutUpdate().unset(KEY);
	}
	
	protected void addBulletPoints(TooltipMakerAPI info, ListInfoMode mode, boolean isUpdate, 
		Color tc, float initPad) {

	if (addEventFactorBulletPoints(info, mode, isUpdate, tc, initPad))
		return;
		
	Color h = Misc.getHighlightColor();
	if (isUpdate && getListInfoParam() instanceof EventStageData) {
		EventStageData esd = (EventStageData) getListInfoParam();
		if (esd.id == Stage.START) 
			info.addPara("bullet points text where does this go lmao", tc, initPad);
		return;
	}
	
	
	//super.addBulletPoints(info, mode, isUpdate, tc, initPad);
	}
	
	public float getImageSizeForStageDesc(Object stageId) {
		return 64f;
	}
	public float getImageIndentForStageDesc(Object stageId) {
		return 16f;
	}

	@Override
	public void addStageDescriptionText(TooltipMakerAPI info, float width, Object stageId) {
		float opad = 10f;
		float small = 0f;
		Color h = Misc.getHighlightColor();

		EventStageData stage = getDataFor(stageId);
		if (stage == null) return;
		if (isStageActiveAndLast(stageId)) {
			if (stageId == Stage.START || stageId == Stage.VARIANTS)
				info.addPara("The Epsilon Pact is still a recently formed polity - beleaguered from most sides, yet unexpectedly resilient. "
					+ "Though currently employing a set of ships largely comparable to independent forces, their proclivity for "
					+ "technological development is no secret: Provided sufficient werewithal, we expect their arsenal of hulls "
					+ "and weapons to gradually begin incorporating deadlier, more exotic original designs.",
					opad);
				else if (stageId == Stage.AUTOMATED || stageId == Stage.PHASEOUT || stageId == Stage.CAPITALS)
				info.addPara("The Epsilon Pact is still a younger polity - beleaguered from most sides, yet unexpectedly resilient. "
					+ "Initially employing a set of ships largely comparable to independent forces, their proclivity for "
					+ "technological development is no secret: Provided sufficient werewithal, we expect their arsenal of hulls "
					+ "and weapons to continue incorporating more exotic and deadly original designs.",
					opad);
				else {
					info.addPara("Once a fresh arrival to the sector, the Pact has since managed to mature their doctrine to meet their "
						+ "eccentric arsenal's requirements. Previously employing a set of ships largely comparable to independent forces, "
						+ "they now possess a full array of deadly and exotic hulls and weapons.",
						opad);
					return;
				}
			info.addSpacer(3f);
			addStageDesc(info, stageId, small, false);
		}
	}
	
	
	public void addStageDesc(TooltipMakerAPI info, Object stageId, float initPad, boolean forTooltip) {
		if (stageId == Stage.START)
			info.addPara("Previously concerned with establishing a foothold in the sector, "
				+ "the Pact currently relies on a majority of imported designs. Once they resume research & development, "
				+ "%s and %s will likely be among their priorities.", initPad,
				Misc.getHighlightColor(),
				"logistics ships", "smaller patterns");
		else if (stageId == Stage.VARIANTS)
			info.addPara("As the Pact solidifies their presence in the sector and introduces more designs, "
				+ "their hulls will proliferate across the core worlds - "
				+ "%s, given the eccentricity of the Pact doctrine.", initPad,
				Misc.getHighlightColor(),
				"likely not in their standard configurations");
		else if (stageId == Stage.AUTOMATED)
			info.addPara("The Pact is already known for their stance of 'AI personhood', "
				+ "and is rumored to move unfettered in the sector's furthest reaches. "
				+ "With sufficient economic and political werewithal, %s "
				+ "- beginning with rudimentary designs, but progressing quickly afterwards.", initPad,
				Misc.getHighlightColor(),
				"we expect them to begin openly fielding automated designs");
		else if (stageId == Stage.PHASEOUT)
			info.addPara("The Pact's doctrine is known to be highly unorthodox, given their economic needs: "
				+ "Their manner of fitting tends to leave numerous redundancies on common patterns, "
				+ "reducing the effectiveness of their fleets. We expect them to eventually %s.", 
				initPad,
				Misc.getHighlightColor(),
				"phase out a large portion of their initial arsenal");
		else if (stageId == Stage.CAPITALS)
			info.addPara("As a smaller polity with many enemies, manpower is among the Pact's larger shortcomings - "
				+ "to this end, they %s if they are given leeway.",
				initPad,
				Misc.getHighlightColor(),
				"exclusively field sub-capital fleets. We should expect this to change");
		else if (stageId == Stage.FINISHED)
			info.addPara("By this point, we estimate the polity will have had sufficient stability - or time - "
				+ "to have concluded %s.",
				initPad,
				Misc.getHighlightColor(),
				"all of their immediate doctrinal development goals");
		
		// if (isStageActiveAndLast(stageId)) {
		
		
	}
	
	
	public TooltipCreator getStageTooltipImpl(Object stageId) {
        final EventStageData esd = getDataFor(stageId);

        if (esd != null) {
            return new BaseFactorTooltip() {
                @Override
                public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
                    float opad = 10f;

                    if (esd.id == Stage.START)
                        tooltip.addTitle("Pact Doctrine");
                    else if (esd.id == Stage.VARIANTS)
                        tooltip.addTitle("Initial Proliferation");
                    else if (esd.id == Stage.AUTOMATED)
                        tooltip.addTitle("Automated Ships");
                    else if (esd.id == Stage.PHASEOUT)
                        tooltip.addTitle("Consolidation");
                    else if (esd.id == Stage.CAPITALS)
                        tooltip.addTitle("Capital Ships");
                    else if (esd.id == Stage.FINISHED)
                        tooltip.addTitle("Conclusion");
                    
                    addStageDesc(tooltip, esd.id, opad, true);
                    esd.addProgressReq(tooltip, opad);
                }
            };
        }

        return null;
	}



	@Override
	public String getIcon() {
		return Global.getSettings().getSpriteName("events", "espc_doctrine_automated");
	}

	protected String getStageIconImpl(Object stageId) {
		EventStageData esd = getDataFor(stageId);
		if (esd == null) return null;
		if (stageId == Stage.START)
			return Global.getSettings().getSpriteName("events", "espc_doctrine_start");
		else if (stageId == Stage.VARIANTS)
			return Global.getSettings().getSpriteName("events", "espc_doctrine_variants");
		else if (stageId == Stage.AUTOMATED)
			return Global.getSettings().getSpriteName("events", "espc_doctrine_automated");
		else if (stageId == Stage.PHASEOUT)
			return Global.getSettings().getSpriteName("events", "espc_doctrine_phaseout");
		else if (stageId == Stage.CAPITALS)
			return Global.getSettings().getSpriteName("events", "espc_doctrine_capitals");
		else if (stageId == Stage.FINISHED)
			return Global.getSettings().getSpriteName("events", "espc_doctrine_finished");

		return Global.getSettings().getSpriteName("events", "stage_unknown_bad");
	}
	
	
	@Override
	public Color getBarColor() {
		Color color = Global.getSector().getFaction("epsilpac").getBaseUIColor();
		color = Misc.interpolateColor(color, Color.black, 0.25f);
		return color;
	}
	
	@Override
	public Color getBarProgressIndicatorColor() {
		return super.getBarProgressIndicatorColor();
	}

	@Override
	protected int getStageImportance(Object stageId) {
		return super.getStageImportance(stageId);
	}


	@Override
	protected String getName() {
		return "Pact Doctrine";
	}


	@Override
	public Set<String> getIntelTags(SectorMapAPI map) {
		Set<String> tags = super.getIntelTags(map);
		tags.add("epsilpac");

		return tags;
	}

	@Override
	protected void advanceImpl(float amount) {
		super.advanceImpl(amount);
	}


	@Override
	protected void notifyStageReached(EventStageData stage) {
		if (stage.id.equals(Stage.START)) {
			
		} else if (stage.id.equals(Stage.VARIANTS)) {
			
		} else if (stage.id.equals(Stage.AUTOMATED)) {
			
		} else if (stage.id.equals(Stage.PHASEOUT)) {
			
		} else if (stage.id.equals(Stage.CAPITALS)) {
			
		} else if (stage.id.equals(Stage.FINISHED)) {
			
		}
	}

	@Override
	public int getMaxMonthlyProgress() {
		return 100;
	}

	public boolean withMonthlyFactors() {
		return false;
	}

	public boolean withOneTimeFactors() {
		return false;
	}

	protected String getSoundForStageReachedUpdate(Object stageId) {
		return super.getSoundForStageReachedUpdate(stageId);
	}

	@Override
	protected String getSoundForOneTimeFactorUpdate(EventFactor factor) {

		return null;
	}
	
	

	@Override
	public void reportFleetDespawnedToListener(CampaignFleetAPI fleet, FleetDespawnReason reason, Object param) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void reportBattleOccurred(CampaignFleetAPI fleet, CampaignFleetAPI primaryWinner, BattleAPI battle) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void commodityUpdated(String commodityId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void economyUpdated() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isEconomyListenerExpired() {
		// TODO Auto-generated method stub
		return false;
	}

	
}








