package data.scripts.campaign.enc;

import java.util.Random;

import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.enc.BaseEPEncounterCreator;
import com.fs.starfarer.api.impl.campaign.enc.EncounterManager;
import com.fs.starfarer.api.impl.campaign.enc.EncounterPoint;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflaterParams;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.intel.bases.LuddicPathBaseIntel;
import com.fs.starfarer.api.impl.campaign.intel.bases.PirateBaseIntel;
import com.fs.starfarer.api.impl.campaign.missions.FleetCreatorMission;
import com.fs.starfarer.api.impl.campaign.missions.hub.MissionFleetAutoDespawn;
import com.fs.starfarer.api.util.Misc;

import data.scripts.campaign.fleets.espc_PactFleetInflater;

public class espc_SlipstreamEpsilpacEPEC extends BaseEPEncounterCreator {
		
		@Override
		public void createEncounter(EncounterManager manager, EncounterPoint point) {
			Random random = manager.getRandom();
			
			float f = getProximityFactor(point.getLocInHyper());
			
			int difficulty = 0;
			difficulty += (int) Math.round(f * 5f);
			difficulty += random.nextInt(6);
			
			FleetCreatorMission m = new FleetCreatorMission(random);
			m.beginFleet();
			
			Vector2f loc = point.getLocInHyper();
			m.createQualityFleet(difficulty, "epsilpac", loc);
			m.triggerFleetAllowLongPursuit();
			m.triggerMakeLowRepImpact();
			m.triggerSetFleetFaction("epsilpac");
			m.triggerFleetSetAllWeapons();
			
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
				fleet.addAssignment(FleetAssignment.ORBIT_AGGRESSIVE, e, 20f * random.nextFloat() + 10f, "guarding area");
				fleet.removeScriptsOfClass(MissionFleetAutoDespawn.class);
				fleet.addScript(new MissionFleetAutoDespawn(null, fleet));
			}
		}

		public float getFrequencyForPoint(EncounterManager manager, EncounterPoint point) {
			if (!point.type.equals(EncounterManager.EP_TYPE_SLIPSTREAM)) return 0f;
			
			float f = getProximityFactor(point.getLocInHyper());
			if (f > 0) {
				f = 0.25f + 0.75f * f;
			}
			return 2f * f;
		}
		
		public static float getProximityFactor(Vector2f locInHyper) {
			LuddicPathBaseIntel intel = getClosestLuddicPathBase(locInHyper);
			float f1 = getLuddicPathBaseProximityFactor(intel, locInHyper);
			
			PirateBaseIntel intel2 = getClosestPirateBase(locInHyper);
			float f2 = getPirateBaseProximityFactor(intel2, locInHyper);
			
			float result = Math.max(f1, f2);
			
			StarSystemAPI ruins = getClosestSystemWithRuins(locInHyper);
			float f3 = getRuinsProximityFactor(ruins, locInHyper);
			result = Math.max(result, f3);
			result *= 0.35f;
			
			if (result <= 0f)
				return 0f;
			int marketCount = 0;
			for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
				if (market.getFactionId().equals("epsilpac") &&
					market.getStabilityValue() > 4) {
					marketCount++;
				}
			}
			if (marketCount == 3)
				result *= 0.7f;
			else if (marketCount < 3 && marketCount > 0)
				result *= 0.4f;
			else
				return 0f;
			
			return result;
		}
		
	}