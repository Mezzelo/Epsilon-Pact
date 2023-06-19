package data.scripts.world;

import java.util.Random;

import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.Script;
import com.fs.starfarer.api.campaign.AICoreOfficerPlugin;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.Abilities;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.missions.hub.BaseHubMission.EntityLocationType;
import com.fs.starfarer.api.impl.campaign.missions.hub.BaseHubMission.LocData;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator.EntityLocation;
import com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantOfficerGeneratorPlugin;
import com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantStationFleetManager;
import com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantThemeGenerator.RemnantStationInteractionConfigGen;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;

public class espc_NexusConstruction {
	
	public static void monthlyConstruction() {
		WeightedRandomPicker<MarketAPI> sourcePicker = new WeightedRandomPicker<MarketAPI>();
		for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
			if (market.getFactionId().equals("epsilpac")) {
				sourcePicker.add(market);
			}
		}
		
		if (!sourcePicker.isEmpty()) {
			MarketAPI chooseMarket = sourcePicker.pick();
			// Global.getLogger(espc_NexusConstruction.class).info("spawn at market: " + chooseMarket.getName());
			
			EntityLocation targetLoc = pickNexusLocation(
				chooseMarket.getPrimaryEntity().getContainingLocation().getLocation());
			
			
			if (targetLoc == null)
				return;
				// Global.getLogger(espc_NexusConstruction.class).info("no suitable loc");
			
			// Global.getLogger(espc_NexusConstruction.class).info("target system: " + targetLoc.orbit.getFocus().getStarSystem());
			
			/*
			Global.getLogger(espc_NexusConstruction.class).info("distance: " + (new Vector2f(
				Vector2f.sub(chooseMarket.getContainingLocation().getLocation(), 
				targetLoc.orbit.getFocus().getContainingLocation().getLocation(), new Vector2f())
				)
			).length()); */
			
			CampaignFleetAPI fleet = FleetFactoryV3.createFleet(
				new FleetParamsV3(chooseMarket,
					FleetTypes.TASK_FORCE,
					110f + 20f * Misc.random.nextFloat(), // combatPts
					25f,
					25f,
					0f,
					0f,
					0f,
					1.0f
				)
			);
			
			chooseMarket.getContainingLocation().addEntity(fleet);
			fleet.setLocation(
				chooseMarket.getPrimaryEntity().getLocation().x, 
				chooseMarket.getPrimaryEntity().getLocation().y
			);
			
			// Global.getLogger(espc_NexusConstruction.class).info("fleet spawned: " + fleet.getName());

			int constructionIndex = 0;
			if (!Global.getSector().getMemoryWithoutUpdate().contains("$espcConstructionIndex")) {
				Global.getSector().getMemoryWithoutUpdate().set("$espcConstructionIndex", constructionIndex);
			}
			else {
				constructionIndex = Global.getSector().getMemoryWithoutUpdate().getInt("$espcConstructionIndex") + 1;
				Global.getSector().getMemoryWithoutUpdate().set("$espcConstructionIndex", 
					constructionIndex);
			}
			Global.getSector().getMemoryWithoutUpdate().set("$espcConstructionFleetId" + constructionIndex,
				fleet.getId());
			fleet.getMemoryWithoutUpdate().set("$espcConstructionStage", 0);
			fleet.getMemoryWithoutUpdate().set("$espcStationTarget", targetLoc.orbit.getFocus().getId());
			
			fleet.addAssignment(FleetAssignment.GO_TO_LOCATION, targetLoc.orbit.getFocus(), 1200f, 
					"traveling to an unknown location",
					new Script() {
					public void run() {
						// on the very rare chance of multiple fleets constructing at once
						// one of them will simply finish early.
						// shouldn't be a big deal.
						manageConstructionFleets(1);
					}
				});
			fleet.addAssignment(FleetAssignment.ORBIT_PASSIVE, targetLoc.orbit.getFocus(), 10f, "completing construction project",
				new Script() {
				public void run() {
					manageConstructionFleets(2);
				}
			});
			Misc.giveStandardReturnToSourceAssignments(fleet, false);
			
		} // else {
		// 	Global.getLogger(espc_NexusConstruction.class).info("no suitable market found");
		// }
	}
	
	
	private static EntityLocation pickNexusLocation(Vector2f origin) {
		Random random = new Random();
		WeightedRandomPicker<EntityLocation> far = new WeightedRandomPicker<EntityLocation>(random);
		WeightedRandomPicker<EntityLocation> picker = new WeightedRandomPicker<EntityLocation>(random);
		
		for (StarSystemAPI system : Global.getSector().getStarSystems()) {
			if (system.hasPulsar()) continue;
			if (Misc.getMarketsInLocation(system).size() > 0) continue;
			
			float days = Global.getSector().getClock().getElapsedDaysSince(system.getLastPlayerVisitTimestamp());
			if (days < 120f) continue;
			
			float weight = 0f;
			if (system.hasTag(Tags.THEME_REMNANT_NO_FLEETS))
				weight = 5f;
			else if (system.hasTag(Tags.THEME_REMNANT_DESTROYED))
				weight = 5f;
			else if (system.hasTag(Tags.THEME_REMNANT_SECONDARY))
				weight = 3f;
			else if (system.hasTag(Tags.THEME_REMNANT_MAIN))
				weight = 2f;
			else if (system.hasTag(Tags.THEME_REMNANT_RESURGENT))
				weight = 2f;
			else if (system.hasTag(Tags.THEME_REMNANT_SUPPRESSED))
				weight = 3f;
			else if (system.hasTag(Tags.THEME_REMNANT))
				weight = 1f;
			else if (system.hasTag(Tags.THEME_DERELICT_CRYOSLEEPER))
				weight = 5f;
			else if (system.hasTag(Tags.THEME_DERELICT_MOTHERSHIP))
				weight = 5f;
			else if (system.hasTag(Tags.THEME_DERELICT_SURVEY_SHIP))
				weight = 1f;
			else if (system.hasTag(Tags.THEME_DERELICT_PROBES))
				weight = 1f;
			else if (system.hasTag(Tags.THEME_DERELICT))
				weight = 1f;
			else
				continue;
			
			float usefulStuff = system.getCustomEntitiesWithTag(Tags.OBJECTIVE).size();
			
			if (usefulStuff <= 0)
				continue;
			
			if (!Misc.getMarketsInLocation(system).isEmpty())
				continue;
			
			for (CampaignFleetAPI fleet : system.getFleets()) {
				if (fleet.isStationMode())
					continue;
			}
			
			float dist = (new Vector2f(
				Vector2f.sub(origin, 
				system.getLocation(), new Vector2f())
				)
			).length();
			
			EntityLocation targetLoc = BaseThemeGenerator.pickCommonLocation(random, system, 200f, false, null);
			if (targetLoc == null)
				continue;
			
			if (dist > 50000f) {
				far.add(targetLoc, weight * usefulStuff);
			} else {
				picker.add(
					targetLoc, 
					weight * usefulStuff * ((50000f - dist)/50000f * 0.7f + 0.3f)
				);
			}
		}
		
		if (picker.isEmpty()) {
			if (far.isEmpty())
				return null;
			picker.addAll(far);
		}
		
		return picker.pick();
	}
	
	private static void createRemnantStation(SectorEntityToken target) {
		Random random = new Random();
		EntityLocation loc = BaseThemeGenerator.createLocationAtRandomGap(
			random, 
			target,
			200f
		);
		
		CampaignFleetAPI fleet = FleetFactoryV3.createEmptyFleet(Factions.REMNANTS, FleetTypes.BATTLESTATION, null);
		
		FleetMemberAPI member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, "remnant_station2_Standard");
		fleet.getFleetData().addFleetMember(member);
		
		//fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_PIRATE, true);
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_JUMP, true);
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_ALLOW_DISENGAGE, true);
		fleet.addTag(Tags.NEUTRINO_HIGH);
		
		fleet.setStationMode(true);
		
		fleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_INTERACTION_DIALOG_CONFIG_OVERRIDE_GEN, 
			new RemnantStationInteractionConfigGen());		
		
		target.getContainingLocation().addEntity(fleet);
		
		//fleet.setTransponderOn(true);
		fleet.clearAbilities();
		fleet.addAbility(Abilities.TRANSPONDER);
		fleet.getAbility(Abilities.TRANSPONDER).activate();
		fleet.getDetectedRangeMod().modifyFlat("gen", 1000f);
		
		fleet.setAI(null);
		
		BaseThemeGenerator.setEntityLocation(fleet, loc, null);
		BaseThemeGenerator.convertOrbitWithSpin(fleet, 5f);
		
		String coreId = Commodities.ALPHA_CORE;
			
		AICoreOfficerPlugin plugin = Misc.getAICoreOfficerPlugin(coreId);
		PersonAPI commander = plugin.createPerson(coreId, fleet.getFaction().getId(), random);
		
		fleet.setCommander(commander);
		fleet.getFlagship().setCaptain(commander);
		RemnantOfficerGeneratorPlugin.integrateAndAdaptCoreForAIFleet(fleet.getFlagship());
		RemnantOfficerGeneratorPlugin.addCommanderSkills(commander, fleet, null, 3, random);
		
		member.getRepairTracker().setCR(member.getRepairTracker().getMaxCR());
		target.getStarSystem().addScript(
			new RemnantStationFleetManager(
				fleet, 1f, 0, 10, 20f, 6, 20)
		);
	}
	
	private static void manageConstructionFleets(int stage) {
		CampaignFleetAPI constructionFleet = null;
		boolean foundFleet = false;
		int constructorIndex = Global.getSector().getMemoryWithoutUpdate().getInt("$espcConstructionIndex");
		for (int i = constructorIndex; i > -1; i--) {
			constructionFleet = (CampaignFleetAPI) Global.getSector().getEntityById(
				Global.getSector().getMemoryWithoutUpdate().getString(
					"$espcConstructionFleetId" + i
				)
			);
			boolean markForRemove = false;
			if (constructionFleet != null && !foundFleet) {
				int thisFleetStage = constructionFleet.getMemoryWithoutUpdate().getInt("$espcConstructionStage");
				if (thisFleetStage == stage - 1) {
					if (stage == 2) {
						markForRemove = true;
						SectorEntityToken target = constructionFleet.getContainingLocation().getEntityById(
							constructionFleet.getMemoryWithoutUpdate().getString(
								"$espcStationTarget"
							));

						createRemnantStation(target);
					}//  else
						// Global.getLogger(espc_NexusConstruction.class).info("begin construction");
					constructionFleet.getMemoryWithoutUpdate().set("$espcConstructionStage", stage);
					foundFleet = true;
				}
			}
			if (constructionFleet == null || markForRemove) {
				for (int g = i; g < constructorIndex; g++) {
					Global.getSector().getMemoryWithoutUpdate().set(
						"$espcConstructionFleetId" + g,
						Global.getSector().getMemoryWithoutUpdate().getString(
							"$espcConstructionFleetId" + (g + 1)
						));
				}
				Global.getSector().getMemoryWithoutUpdate().unset(
					"$espcConstructionFleetId" + constructorIndex);
				constructorIndex--;
			}
		}
		Global.getSector().getMemoryWithoutUpdate().set("$espcConstructionIndex", constructorIndex);
	}
}
