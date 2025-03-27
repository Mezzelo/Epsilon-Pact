package data.scripts.campaign.listeners;

import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.GenericPluginManagerAPI;
import com.fs.starfarer.api.campaign.GenericPluginManagerAPI.GenericPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.PlayerMarketTransaction;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.campaign.listeners.CargoScreenListener;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Items;
import com.fs.starfarer.api.impl.campaign.tutorial.TutorialMissionIntel;
import com.fs.starfarer.api.plugins.AutofitPlugin.AvailableWeapon;
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
		// tag for any transfer of the targeted items
		
		// this would be so funny, actually, but no.
		if (TutorialMissionIntel.isTutorialInProgress())
			return;
		
		if (Global.getSector().getFaction("epsilpac").getRelToPlayer().isAtWorst(RepLevel.WELCOMING))
			return;
		
		if (Misc.getFactionMarkets("epsilpac").size() <= 2)
			return;
		
		if (Global.getSector().getMemoryWithoutUpdate().getBoolean("$espcAIInterceptFleetSent") &&
			Global.getSector().getMemoryWithoutUpdate().getBoolean("$espcBlueprintInterceptFleetSent"))
			return;

		boolean hasAICore = false;
		boolean hasCapBP = false;
		boolean hasColonyItem = false;
		for (CargoStackAPI stack : transaction.getSold().getStacksCopy()) {
			CommoditySpecAPI spec = stack.getResourceIfResource();
			if (spec != null && 
				(spec.getId().equals(Commodities.GAMMA_CORE)) ||
				(spec.getId().equals(Commodities.BETA_CORE)) ||
				(spec.getId().equals(Commodities.ALPHA_CORE))
				) {
				hasAICore = true;
			} else if (stack.getSpecialDataIfSpecial() != null) {
				if (stack.getSpecialDataIfSpecial().getData().equals(Items.SHIP_BP)) {
					String bpOf = stack.getSpecialDataIfSpecial().getId();
					if (Global.getSettings().getHullSpec(bpOf) != null &&
						!bpOf.contains("espc") &&
						Global.getSettings().getHullSpec(bpOf).isCivilianNonCarrier() &&
						// for reference, atlas mk2 is 150k, prom mk2 & retribution are 200k, doom is 250k
						Global.getSettings().getHullSpec(bpOf).getBaseValue() >= 245000f
						) {
						hasCapBP = true;
					}
				} else if (stack.getSpecialItemSpecIfSpecial().getId().equals(Items.PRISTINE_NANOFORGE) ||
					stack.getSpecialItemSpecIfSpecial().getId().equals(Items.DRONE_REPLICATOR) ||
					stack.getSpecialItemSpecIfSpecial().getId().equals(Items.CRYOARITHMETIC_ENGINE)) {
				}
			}
			if (hasAICore && !Global.getSector().getMemoryWithoutUpdate().getBoolean("$espcAIInterceptFleetSent") ||
				(hasCapBP || hasColonyItem) && !Global.getSector().getMemoryWithoutUpdate().getBoolean("$espcBPInterceptFleetSent"))
				break;
			
		}
		if (hasAICore && !Global.getSector().getMemoryWithoutUpdate().getBoolean("$espcAIInterceptFleetSent")) {
			Global.getLogger(getClass()).info("intercept time: " + Global.getSector().getClock().getElapsedDaysSince(0));
			Global.getLogger(getClass()).info("getfloat default: " + 
			Global.getSector().getMemoryWithoutUpdate().getFloat("$espcAIInterceptTime"));
			Global.getSector().getMemoryWithoutUpdate().set("$espcAIInterceptFleetSent", true);
			
		}

	}

	@Override
	public int getHandlingPriority(Object params) {
		return -1;
	}
}







