package data.scripts.campaign.submarkets;

import java.util.Random;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionDoctrineAPI;
import com.fs.starfarer.api.campaign.FactionAPI.ShipPickMode;
import com.fs.starfarer.api.combat.ShipHullSpecAPI.ShipTypeHints;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.submarkets.OpenMarketPlugin;
import com.fs.starfarer.api.util.Misc;

/*
import data.scripts.EpsilfacInitialization;
import exerelin.utilities.NexConfig;*/

public class espc_PactOpenMarketPlugin extends OpenMarketPlugin {

	@Override
	public void addShips(String factionId, 
		float combat,
		float freighter,
		float tanker,
		float transport, 
		float liner, 
		float utility,
		Float qualityOverride,
		float qualityMod,
		ShipPickMode modeOverride,
		FactionDoctrineAPI doctrineOverride,
		int maxShipSize) {
		
		FleetParamsV3 params = new FleetParamsV3(
			market,
			Global.getSector().getPlayerFleet().getLocationInHyperspace(),
			factionId,
			null, // qualityOverride
			FleetTypes.PATROL_LARGE,
			combat, // combatPts
			freighter, // freighterPts 
			tanker, // tankerPts
			transport, // transportPts
			liner, // linerPts
			utility, // utilityPts
			0f // qualityMod
			);
		params.maxShipSize = maxShipSize;
		params.random = new Random(itemGenRandom.nextLong());
		params.qualityOverride = Misc.getShipQuality(market, factionId) + qualityMod;
		if (qualityOverride != null) {
		params.qualityOverride = qualityOverride + qualityMod;
		}
		//params.qualityMod = qualityMod;
		
		params.withOfficers = false;
		
		params.forceAllowPhaseShipsEtc = true;
		params.treatCombatFreighterSettingAsFraction = true;
		
		params.modeOverride = Misc.getShipPickMode(market, factionId);
		if (modeOverride != null) {
		params.modeOverride = modeOverride;
		}
		
		params.doctrineOverride = doctrineOverride;
		
		CampaignFleetAPI fleet = FleetFactoryV3.createFleet(params);
		if (fleet != null) {

			fleet.getFleetData().setOnlySyncMemberLists(false);
			if (market.getFactionId().equals("epsilpac"))
				for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
					if (espc_MarketOverrideList.isOverride(member.getHullId()) && Misc.random.nextFloat() > 0.2f) {
						fleet.getFleetData().removeFleetMember(member);
						fleet.getFleetData().addFleetMember(espc_MarketOverrideList.getHull() + "_Hull");
					}
				}
			
			fleet.getFleetData().sort();
	        fleet.forceSync();
	        fleet.getFleetData().setSyncNeeded();
	        fleet.getFleetData().syncIfNeeded();
	        
			float p = 0.5f;
			//p = 1f;
			for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
				if (itemGenRandom.nextFloat() > p) continue;
				if (member.getHullSpec().hasTag(Tags.NO_SELL)) continue;
				if (!isMilitaryMarket() && member.getHullSpec().hasTag(Tags.MILITARY_MARKET_ONLY)) continue;
				if (market.getFactionId().equals("epsilpac")
					&& member.getHullSpec().getHints().contains(ShipTypeHints.UNBOARDABLE)) continue;
				String emptyVariantId = member.getHullId() + "_Hull";
				addShip(emptyVariantId, true, params.qualityOverride);
			}
		}
	}
	
}