package data.missions.espc_principle;

import java.util.List;

import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
// import com.fs.starfarer.api.combat.CombatAssignmentType;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;
// import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
// import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import java.util.ArrayList;
// import java.util.Iterator;

import data.scripts.util.EspcOfficerFactory;

public class MissionDefinition implements MissionDefinitionPlugin {

	public void defineMission(MissionDefinitionAPI api) {

		
		// Set up the fleets
		api.initFleet(FleetSide.PLAYER, "EPS", FleetGoal.ATTACK, false, -5);
		api.initFleet(FleetSide.ENEMY, "TTS", FleetGoal.ATTACK, true, 3);

		// Set a blurb for each fleet
		api.setFleetTagline(FleetSide.PLAYER, "EPS Taste for Blood");
		api.setFleetTagline(FleetSide.ENEMY, "Tri-Tachyon Heavy Pursuit Fleet");
		
		// These show up as items in the bulleted list under 
		// "Tactical Objectives" on the mission detail screen
		api.addBriefingItem("Your cruiser is vulnerable when flanked, but exceptionally powerful in a close-quarters duel.");
		api.addBriefingItem("You have about a minute until the rear of their fleet catches up. Work quickly.");
		api.addBriefingItem("Time your strikes alongside theirs. Let their confidence be their undoing.");
		// api.addBriefingItem("Do not allow your attackers to disengage freely, lest you end up surrounded.");
		api.addBriefingItem("You've been through worse.");
		
		
		FleetMemberAPI member = api.addToFleet(FleetSide.PLAYER, "espc_chorale_Elite", FleetMemberType.SHIP, "EPS Taste for Blood", true);
		PersonAPI pilot = EspcOfficerFactory.MakePilot("Isabelle", "de' Medici", FullName.Gender.FEMALE, "aggressive", 
			"graphics/portraits/espc_isabelle.png", "epsilpac", 5);
		EspcOfficerFactory.PopulateSkills(pilot, new String[]{
			Skills.HELMSMANSHIP, Skills.TARGET_ANALYSIS, 
			Skills.SYSTEMS_EXPERTISE, Skills.ENERGY_WEAPON_MASTERY, 
			Skills.ORDNANCE_EXPERTISE}, new int[]{
			2, 2,
			1, 2, 2});
        member.setCaptain(pilot);
		
		api.addToFleet(FleetSide.ENEMY, "odyssey_Balanced", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "doom_Strike", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "aurora_Strike", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "fury_Attack", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "fury_Attack", FleetMemberType.SHIP, false);
		
		
		// Set up the map.
		float width = 12000f;
		float height = 12000f;
		api.initMap((float)-width/2f, (float)width/2f, (float)-height/2f, (float)height/2f);
		
		api.setHyperspaceMode(true);
		api.setBackgroundSpriteName("graphics/backgrounds/background_galatia.jpg");
		
		api.addPlugin(new BaseEveryFrameCombatPlugin() {
			FleetMemberAPI doom, odyssey = null;
			int removed1, removed2 = 0;
			int spawnState = 0;
			CombatFleetManagerAPI fleetManager;
			CombatEngineAPI combatEngine;
			ArrayList<ShipAPI> startingShips;
			
			@SuppressWarnings("rawtypes")
			public void advance(float amount, List events) {
				if (spawnState < 2) {
					CombatFleetManagerAPI fleetManager = combatEngine.getFleetManager(FleetSide.ENEMY);
					
					// ensures the initial wave starts in the same positions
					// arguably this is the toughest configuration to fight lol
					for (FleetMemberAPI thisShip : fleetManager.getReservesCopy()) {
						String hullName = thisShip.getHullSpec().getHullName();
						if (hullName.equals("Fury")) {
							startingShips.add(fleetManager.spawnFleetMember(thisShip, new Vector2f(-1000f + 2000f * spawnState, 3000f), 270f, 5f));
							spawnState++;
						} else if (hullName.equals("Aurora")) {
							startingShips.add(fleetManager.spawnFleetMember(thisShip, new Vector2f(0f, 3000f), 270f, 5f));
						}
					}
				}
				
				if (combatEngine.isPaused()) {
					if (removed1 == 0) {
						fleetManager.addToReserves(doom);
						removed1 = 1;
					}
					if (removed2 == 0) {
						fleetManager.addToReserves(odyssey);
						removed2 = 1;
					}
				} else {
					for (int i = startingShips.size() - 1; i > 0; i--) {
						if (!(((ShipAPI) startingShips.get(i)).isAlive()))
							startingShips.remove(i);
					}
					
					if (removed1 == 1) {
						fleetManager.removeFromReserves(doom);
						removed1 = 0;
					}
					if (removed2 == 1) {
						fleetManager.removeFromReserves(odyssey);
						removed2 = 0;
					}

					if (removed2 == 0) {
						if (startingShips.size() < 2 || combatEngine.getTotalElapsedTime(false) > 50f) {
							removed2 = 2;
							removed1 = 2;
							fleetManager.addToReserves(odyssey);
							fleetManager.addToReserves(doom);
						}
					} /* else if (removed1 == 0) {
						if ((combatEngine.getTotalElapsedTime(false) > 60f && (combatEngine.getTotalElapsedTime(false) > 120f || 
							startingShips.size() < 1))) {
						}
					} */
				}
			}
			public void init(CombatEngineAPI engine) {
				engine.getContext().setStandoffRange(5000f);
				fleetManager = engine.getFleetManager(FleetSide.ENEMY);
				combatEngine = engine;
				startingShips = new ArrayList<ShipAPI>();
				
				for (FleetMemberAPI thisShip : fleetManager.getReservesCopy()) {
					String hullName = thisShip.getHullSpec().getHullName();
					if (hullName.equals("Doom") || hullName.equals("Odyssey")) {
						if (hullName.equals("Doom"))
							doom = thisShip;
						else
							odyssey = thisShip;
						fleetManager.removeFromReserves(thisShip);
					}
				}		
				
			}
		});
	}

}






