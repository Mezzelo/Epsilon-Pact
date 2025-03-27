package data.scripts.campaign.enc;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.enc.BaseEPEncounterCreator;
import com.fs.starfarer.api.impl.campaign.enc.EncounterManager;
import com.fs.starfarer.api.impl.campaign.enc.EncounterPoint;
import com.fs.starfarer.api.impl.campaign.fleets.AutoDespawnScript;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflaterParams;
import com.fs.starfarer.api.impl.campaign.ids.Abilities;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.missions.FleetCreatorMission;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.FleetQuality;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.FleetSize;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.OfficerNum;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.OfficerQuality;
import com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantStationFleetManager;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;

import data.scripts.campaign.fleets.espc_PactFleetInflater;

public class espc_OutsideSystemEpsilpacEPEC extends BaseEPEncounterCreator {
	
	@Override
	public void createEncounter(EncounterManager manager, EncounterPoint point) {
		if (!(point.custom instanceof RemnantStationFleetManager)) return;

		WeightedRandomPicker<SectorEntityToken> marketPicker = new WeightedRandomPicker<SectorEntityToken>();
		for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
			if (market.getFactionId().equals("epsilpac") &&
				market.getStabilityValue() > 4) {
				marketPicker.add(market.getPrimaryEntity());
			}
		}
		if (marketPicker.isEmpty())
			return;
		SectorEntityToken home = marketPicker.pick();
		
		Random random = manager.getRandom();
		RemnantStationFleetManager fm = (RemnantStationFleetManager) point.custom;
		
		int difficulty = 0;
		int max = 10;
		float mult = 1f;
		if (fm.getSource() != null && fm.getSource().getStarSystem() != null && 
			fm.getSource().getStarSystem().hasTag(Tags.THEME_REMNANT_SUPPRESSED)) {
			max = 7;
			mult = 0.5f;
		}
		
		difficulty += (int) Math.min(fm.getTotalLost() * mult, max);
		difficulty += random.nextInt(4);
		if (difficulty > 10) difficulty = 10;
		
		FleetCreatorMission m = new FleetCreatorMission(random);
		m.beginFleet();
		
		Vector2f loc = point.getLocInHyper();
		
		FleetSize size = FleetSize.MEDIUM;
		FleetQuality quality = FleetQuality.HIGHER;
		OfficerQuality oQuality = OfficerQuality.HIGHER;
		OfficerNum oNum = OfficerNum.DEFAULT;
		String type = FleetTypes.PATROL_SMALL;
		
		if (difficulty <= 5) {
			size = FleetSize.MEDIUM;
			type = FleetTypes.PATROL_SMALL;
		} else if (difficulty < 8) {
			size = FleetSize.LARGE;
			type = FleetTypes.PATROL_MEDIUM;
		} else { // (difficulty >= 8) 
			size = FleetSize.LARGE;
			type = FleetTypes.PATROL_LARGE;
		}
		
		m.triggerCreateFleet(size, quality, "epsilpac", type, loc);
		m.triggerSetFleetOfficers(oNum, oQuality);
		m.triggerMakeLowRepImpact();
		m.triggerAddAbilities(Abilities.EMERGENCY_BURN);
		m.triggerAddAbilities(Abilities.SENSOR_BURST);
		m.triggerAddAbilities(Abilities.GO_DARK);
		m.triggerFleetAllowJump();
		m.triggerFleetUnsetAllowLongPursuit();
		
		CampaignFleetAPI fleet = m.createFleet();
		if (fleet != null) {
			point.where.addEntity(fleet);
			if (fleet.getInflater() != null && 
				!(fleet.getInflater() instanceof espc_PactFleetInflater) &&
				fleet.getInflater().getParams() instanceof DefaultFleetInflaterParams)
				fleet.setInflater(new espc_PactFleetInflater((DefaultFleetInflaterParams) fleet.getInflater().getParams()));
			fleet.setLocation(point.loc.x, point.loc.y);
			Vector2f spawnLoc = Misc.getPointWithinRadius(point.loc, 1000f);
			SectorEntityToken e = point.where.createToken(spawnLoc);
			String actionText = "patrolling";	
			fleet.addAssignment(FleetAssignment.PATROL_SYSTEM, e, 5f * random.nextFloat() + 10f, actionText);
			fleet.addAssignment(FleetAssignment.GO_TO_LOCATION, fm.getSource(), 1000f, "moving to " + fm.getSource().getName());
			fleet.addAssignment(FleetAssignment.ORBIT_PASSIVE, fm.getSource(), 15f * random.nextFloat() * 10f, "performing maintenance");
			fleet.addAssignment(FleetAssignment.PATROL_SYSTEM, fm.getSource(), 15f * random.nextFloat() + 10f, actionText);
			fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, home, 300f, "returning to " + home.getName());
			fleet.addScript(new AutoDespawnScript(fleet));
		}
	}

	public float getFrequencyForPoint(EncounterManager manager, EncounterPoint point) {
		
		if (!EncounterManager.EP_TYPE_OUTSIDE_SYSTEM.equals(point.type)) return 0f;
		if (!(point.custom instanceof RemnantStationFleetManager)) return 0f;
		RemnantStationFleetManager fm = (RemnantStationFleetManager) point.custom;
		// remnant is 10f;
		float baseFreq = 7f;
		int marketCount = 0;
		for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
			if (market.getFactionId().equals("epsilpac") &&
				market.getStabilityValue() > 4) {
				marketCount++;
			}
		}
		if (marketCount == 3)
			baseFreq = 4f;
		else if (marketCount < 3 && marketCount > 0)
			 baseFreq = 2f;
		else
			return 0f;
		
		float mult = 0.5f;
		
		return baseFreq * (float) Math.min(10f, fm.getTotalLost()) * mult;
	}
	
	
	
}