package data.scripts.campaign.listeners;

import java.util.ArrayList;
import java.util.Random;

import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.GenericPluginManagerAPI;
import com.fs.starfarer.api.campaign.GenericPluginManagerAPI.GenericPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.PlayerMarketTransaction;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.campaign.listeners.CargoScreenListener;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.Items;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.impl.campaign.intel.MessageIntel;
import com.fs.starfarer.api.impl.campaign.missions.DelayedFleetEncounter;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.FleetQuality;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.FleetSize;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.OfficerNum;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.OfficerQuality;
import com.fs.starfarer.api.impl.campaign.tutorial.TutorialMissionIntel;
import com.fs.starfarer.api.util.Misc;

public class espc_CargoListener implements GenericPlugin, CargoScreenListener {
	
	
	public static final String KEY = "$espc_CargoListener";
	
	public static espc_CargoListener getInstance() {
		Object test = Global.getSector().getMemoryWithoutUpdate().get(KEY);
		if (test == null) {
			test = new espc_CargoListener();
			Global.getSector().getMemoryWithoutUpdate().set(KEY, test);
		}
		return (espc_CargoListener) test; 
	}
	
	public espc_CargoListener() {
		super();
		
		GenericPluginManagerAPI plugins = Global.getSector().getGenericPlugins();
		plugins.addPlugin(this, false);
		
		Global.getSector().getListenerManager().addListener(this);
	}

	public void reportCargoScreenOpened() {
	}
	
	public void reportSubmarketOpened(SubmarketAPI submarket) {
	}

	public void reportPlayerLeftCargoPods(SectorEntityToken entity) {
		
	}
	
	public void reportPlayerNonMarketTransaction(PlayerMarketTransaction transaction, InteractionDialogAPI dialog) {

		if (transaction.getBought().isEmpty() ||
			TutorialMissionIntel.isTutorialInProgress() ||
			Global.getSector().getFaction("epsilpac").getRelToPlayer().isAtWorst(RepLevel.WELCOMING) ||
			Global.getSector().getPlayerFleet().isInHyperspace() ||
			// ensure we're far enough from the core - or just the absolute center of the map, because the actual core's location
			// under misc is just busted by mods lol
			Global.getSector().getPlayerFleet().getLocationInHyperspace().lengthSquared() < 25000 * 25000 ||
			Misc.isInAbyss(Global.getSector().getPlayerFleet().getLocationInHyperspace()) ||
			Misc.getFactionMarkets("epsilpac").size() <= 2 ||
			Global.getSector().getClock().getElapsedDaysSince(
				Global.getSector().getMemoryWithoutUpdate().getLong("$espcSalvageInterceptTime")) < 100
			)
			return;

		for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
			if (Vector2f.sub(market.getLocationInHyperspace(),
				Global.getSector().getPlayerFleet().getLocationInHyperspace(), new Vector2f()).length() < 7500f &&
				!market.getFactionId().equals(Factions.PIRATES) && !market.getFactionId().equals(Factions.LUDDIC_PATH) &&
				!market.isHidden())
				return;
		}
		
		int AIInterceptCount = Global.getSector().getMemoryWithoutUpdate().getInt("$espcAIInterceptCount");
		boolean hasAICore = AIInterceptCount >= 0 && 
			(
			// transaction.getBought().getCommodityQuantity(Commodities.GAMMA_CORE) > 0 ||
			transaction.getBought().getCommodityQuantity(Commodities.BETA_CORE) > 0 ||
			transaction.getBought().getCommodityQuantity(Commodities.ALPHA_CORE) > 0);

		// int hasCapBP = 0;
		boolean hasColonyItem = false;
		
		ArrayList<String> items = new ArrayList<String>();

		if (!hasAICore || AIInterceptCount < 0) {
			for (CargoStackAPI stack : transaction.getBought().getStacksCopy()) {
				if (stack.isSpecialStack()) {
					/*
						String bpOf = stack.getSpecialDataIfSpecial().getData();
						if (Global.getSettings().getHullSpec(bpOf) != null &&
							!bpOf.contains("espc") &&
							!Global.getSettings().getHullSpec(bpOf).isCivilianNonCarrier() &&
							// for reference, atlas mk2 is 150k, prom mk2 & retribution are 200k, doom is 250k
							Global.getSettings().getHullSpec(bpOf).getBaseValue() >= 245000f
							) {
							items.add(bpOf);
						}
						
					} else */
					if (stack.getSpecialItemSpecIfSpecial().getId().equals(Items.PRISTINE_NANOFORGE) ||
						stack.getSpecialItemSpecIfSpecial().getId().equals(Items.DRONE_REPLICATOR) ||
						stack.getSpecialItemSpecIfSpecial().getId().equals(Items.CRYOARITHMETIC_ENGINE)) {
						hasColonyItem = true;
					}
				}
			}
		}	
		/*
		if (!hasAICore && !hasColonyItem)
			return;
		
		if (hasAICore && AIInterceptCount >= 0) {
			AIInterceptCount++;
			
			float chance = AIInterceptCount == 3 ? 1f : 
				Misc.random.nextFloat();
			if (chance > 0.5f) {
				Global.getSector().getMemoryWithoutUpdate().set("$espcAIInterceptCount", 
					-1);
				Global.getSector().getMemoryWithoutUpdate().set("$espcSalvageInterceptTime", Global.getSector().getClock().getTimestamp());
				
				MessageIntel intel = new MessageIntel();
				intel.addLine("dog they're comin!!!");
				//intel.setIcon(Global.getSettings().getSpriteName("intel_categories", "reputation"));
				Global.getSector().getCampaignUI().addMessage(intel);

				DelayedFleetEncounter e = new DelayedFleetEncounter(new Random(), "espcAIInterceptMission");
				e.setDelayNone();
				e.setLocationAnywhere(true, "epsilpac");
				e.setEncounterInHyper();
				e.setDoNotAbortWhenPlayerFleetTooStrong();
				e.beginCreate();
				e.triggerCreateFleet(FleetSize.LARGER, FleetQuality.VERY_HIGH, "epsilpac", FleetTypes.PATROL_LARGE, new Vector2f());
				e.triggerSetFleetOfficers(OfficerNum.MORE, OfficerQuality.DEFAULT);
				e.triggerFleetSetFlagship("espc_flagbearer_Standard");
				e.triggerFleetAddCommanderSkill(Skills.WOLFPACK_TACTICS, 1);
				e.triggerFleetSetFaction("epsilpac");
				e.triggerSetFleetFlag("$espcAIInterceptFleet");
				e.triggerMakeLowRepImpact();
				e.triggerMakeNonHostile();
				e.triggerOrderFleetInterceptPlayer(false, true);
				e.endCreate();
				return;
			} else
				Global.getSector().getMemoryWithoutUpdate().set("$espcAIInterceptCount", 
					AIInterceptCount);
		}
		if (items.size() > 0 || hasColonyItem) {
			float chance = Misc.random.nextFloat();
			if (chance > 0.6 ||
				Global.getSector().getMemoryWithoutUpdate().getLong("$espcSalvageInterceptTime") <= 0 && chance > 0.3) {
				Global.getSector().getMemoryWithoutUpdate().set("$espcSalvageInterceptTime", Global.getSector().getClock().getTimestamp());
				Global.getSector().getMemoryWithoutUpdate().set("$espcSalvageInterceptCount", items.size());
				for (int i = items.size() - 1; i >= 0; i--)
					Global.getSector().getMemoryWithoutUpdate().set("$espcSalvageInterceptItem" + i, items.get(i));
				Global.getLogger(getClass()).info("ring ring ring!!!  fleet otw");
				
				DelayedFleetEncounter e = new DelayedFleetEncounter(new Random(), "espcBPInterceptMission");
				e.setDelayNone();
				e.setLocationAnywhere(true, "epsilpac");
				e.setEncounterInHyper();
				e.setDoNotAbortWhenPlayerFleetTooStrong();
				e.beginCreate();
				e.triggerCreateFleet(FleetSize.LARGER, FleetQuality.SMOD_1, "epsilpac", FleetTypes.TASK_FORCE, new Vector2f());
				e.triggerFleetSetFlagship("espc_amanuensis_Assault");
				e.triggerSetFleetOfficers(OfficerNum.MORE, OfficerQuality.HIGHER);
				e.triggerFleetSetFaction("epsilpac");
				e.triggerSetFleetFlag("$espcBPInterceptFleet");
				e.triggerMakeLowRepImpact();
				e.triggerSetStandardAggroInterceptFlags();
				e.endCreate();
				return;
			}
		}
		*/
	}

	@Override
	public int getHandlingPriority(Object params) {
		return -1;
	}
}







