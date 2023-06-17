package data.scripts.campaign.submarkets;
// currently unused, as i can't figure out how to hide automatically-added generic military market
// it's fine, but urgh.  don't wanna jump through those loops for release, other stuff to work on rn
// TODO ig?

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
import com.fs.starfarer.api.impl.campaign.submarkets.MilitarySubmarketPlugin;
import com.fs.starfarer.api.util.Misc;
/*
import data.scripts.EpsilfacInitialization;
import exerelin.campaign.AllianceManager;
import exerelin.campaign.PlayerFactionStore;
import exerelin.utilities.NexConfig;
import exerelin.utilities.NexUtilsFaction;*/

public class espc_PactMilitarySubmarketPlugin extends MilitarySubmarketPlugin {
	
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
			float p = 0.5f;
			//p = 1f;
			for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
				if (itemGenRandom.nextFloat() > p) continue;
				if (member.getHullSpec().hasTag(Tags.NO_SELL)) continue;
				if (!isMilitaryMarket() && member.getHullSpec().hasTag(Tags.MILITARY_MARKET_ONLY)) continue;
				if (member.getHullSpec().getHints().contains(ShipTypeHints.UNBOARDABLE)) continue;
				String emptyVariantId = member.getHullId() + "_Hull";
				addShip(emptyVariantId, true, params.qualityOverride);
			}
		}
	}
}
