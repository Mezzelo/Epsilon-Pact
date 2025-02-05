package data.campaign.listeners;
// doing this through here rather than BaseGenerateFleetOfficersPlugin, as the generic officer generation is fine for my purposes
// there's just a few exceptions i'd like to impose.

// import java.util.Random;

// import org.lwjgl.util.vector.Vector2f;

// you can do this through the fleet inflater apparently?  i still don't know what the fuck that is need to glaze my eyes over
// on the api for longer.  this works for the moment.

import com.fs.starfarer.api.Global;
// import com.fs.starfarer.api.Script;
// import com.fs.starfarer.api.Global;
// import com.fs.starfarer.api.Global;
// import com.fs.starfarer.api.EveryFrameScript;
// import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;

// import data.scripts.world.espc_NexusConstruction;

// import com.fs.starfarer.api.campaign.econ.MarketAPI;
// import com.fs.starfarer.api.campaign.listeners.FleetSpawnListener;
// import com.fs.starfarer.api.characters.OfficerDataAPI;
//import com.fs.starfarer.api.impl.campaign.ids.Abilities;
//import com.fs.starfarer.api.impl.campaign.ids.Factions;
// import java.util.Iterator;
// import java.util.List;

// import org.apache.log4j.Logger;

public class espc_EconomyListener extends BaseCampaignEventListener {
	
	private boolean nexRandom = false;

	public espc_EconomyListener(boolean permaRegister, boolean nexRandom) {
		super(permaRegister);
		this.nexRandom = nexRandom;
	}
	
	@Override
	public void reportEconomyMonthEnd() {
		
		/*
		if (Global.getSector().getClock().getMonth() % 6 == 0)
			espc_NexusConstruction.monthlyConstruction(); */
		
		if (nexRandom)
			return;
		
		if (!Global.getSector().getMemoryWithoutUpdate().getBoolean("$espcAnyiwoCores") &&
			Global.getSector().getMemoryWithoutUpdate().getInt("$espcBetaCores") > 1)
			Global.getSector().getMemoryWithoutUpdate().set("$espcAnyiwoCores", true);
		
		PersonAPI isabelle = Global.getSector().getImportantPeople().getPerson("espc_isabelle");
		if (isabelle == null)
			return;
		
		// isabelle.getMarket().getCommDirectory().removePerson(isabelle);
		// isabelle.getMarket().removePerson(isabelle);
		
		WeightedRandomPicker<MarketAPI> marketPicker = new WeightedRandomPicker<MarketAPI>();
		for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
			if (market.getId().contains("espc")) {
				if (market.hasCondition("espc_ai_population") && !market.getFactionId().equals("epsilpac")) {
					market.removeCondition("espc_ai_population");
					market.addCondition("espc_rogue_ai_population");
				} else if (market.hasCondition("espc_rogue_ai_population") && market.getFactionId().equals("epsilpac")) {
					market.removeCondition("espc_rogue_ai_population");
					market.addCondition("espc_ai_population");
				}
				if (market.getId().equals("espc_tocquiera_market")) {
					if (!market.getFactionId().equals("epsilpac")) {
						if (market.getAdmin().getId().equals("espc_anlo"))
							market.setAdmin(null);
					} else if (!market.getAdmin().getId().equals("espc_anlo") && 
						Global.getSector().getImportantPeople().getPerson("espc_anlo") != null)
						market.setAdmin(Global.getSector().getImportantPeople().getPerson("espc_anlo"));
				} 
				if (market.getFactionId().equals("epsilpac") && market != isabelle.getMarket()) {
					marketPicker.add(market);
				}
			} 
			/* else if (market.getFactionId().equals("epsilpac")) {
				if (market.hasSubmarket(Submarkets.SUBMARKET_OPEN)) {
					market.addSubmarket("espc_open_market");
					market.removeSubmarket(Submarkets.SUBMARKET_OPEN);
				}if (market.hasSubmarket(Submarkets.SUBMARKET_BLACK)) {
					market.addSubmarket("espc_black_market");
					market.getSubmarket("espc_black_market").setFaction(Global.getSector().getFaction(Factions.PIRATES));
					market.removeSubmarket(Submarkets.SUBMARKET_BLACK);
				}
			} */
		}
		
		if (marketPicker.isEmpty()) {
			if (!isabelle.getMarket().getCommDirectory().getEntryForPerson(isabelle).isHidden())
				isabelle.getMarket().getCommDirectory().getEntryForPerson(isabelle).setHidden(true);
			return;
		}

		MarketAPI pick = marketPicker.pick();
		if (pick == null)
			return;
		if (pick.getPeopleCopy() == null)
			return;
			
		if (isabelle.getMarket().getCommDirectory().getEntryForPerson(isabelle) != null &&
			isabelle.getMarket().getCommDirectory().getEntryForPerson(isabelle).isHidden() &&
			Global.getSector().getMemoryWithoutUpdate().getBoolean("$espcCoresRedirected")) {
			isabelle.getMarket().getCommDirectory().getEntryForPerson(isabelle).setHidden(false);
		}
		
		MarketAPI oldMarket = isabelle.getMarket();
		Misc.moveToMarket(isabelle, pick, true);
		if (oldMarket.getCommDirectory().getEntryForPerson(isabelle) != null)
			isabelle.getMarket().getCommDirectory().getEntryForPerson(isabelle).setHidden(
				oldMarket.getCommDirectory().getEntryForPerson(isabelle).isHidden()
			);
			oldMarket.getCommDirectory().removePerson(isabelle);
		isabelle.setImportance(PersonImportance.VERY_HIGH);
	}

}
