package data.missions.espc_gauss;

import java.util.List;

import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.CombatAssignmentType;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;

import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import org.lwjgl.util.vector.Vector2f;
import com.fs.starfarer.api.util.Misc;
import java.util.LinkedList;
import java.util.Iterator;

import data.scripts.util.EspcOfficerFactory;

// import org.lwjgl.input.Keyboard;

public class MissionDefinition implements MissionDefinitionPlugin {

	public void defineMission(MissionDefinitionAPI api) {

		
		// Set up the fleets
		api.initFleet(FleetSide.PLAYER, "EPS", FleetGoal.ATTACK, false, -4);
		api.initFleet(FleetSide.ENEMY, "", FleetGoal.ATTACK, true, 3);

		// Set a blurb for each fleet
		api.setFleetTagline(FleetSide.PLAYER, "Your stolen ship, some unknown rescuers, and a dream");
		api.setFleetTagline(FleetSide.ENEMY, "The station warlord and his subordinates");
		
		// These show up as items in the bulleted list under 
		// "Tactical Objectives" on the mission detail screen
		api.addBriefingItem("You are not a captain, let alone much of a pilot. Command points will be extremely limited.");
		api.addBriefingItem("Your allies are heavily outnumbered. Be ready to interdict, but watch your hull.");
		api.addBriefingItem("Leverage your unique ship system to prevail against an overwhelming enemy force.");
		api.addBriefingItem("Don't lose your ship. You're borrowing it, after all.");
		
		// Set up the player's fleet
		FleetMemberAPI member = api.addToFleet(FleetSide.PLAYER, "wolf_espc_Gauss_Original", FleetMemberType.SHIP, "That's my ship, you runt!", true);
		PersonAPI pilot = EspcOfficerFactory.MakePilot("Gauss", "", FullName.Gender.FEMALE, Personalities.RECKLESS, 
			"graphics/portraits/espc_gauss.png", "pirates", 1);
		
		EspcOfficerFactory.PopulateSkills(pilot, new String[]{
			Skills.SYSTEMS_EXPERTISE}, new int[]{
			2});
        member.setCaptain(pilot);
		
		// Mark player flagship as essential
		api.defeatOnShipLoss("That's my ship, you runt!");
		
		
		// chorale
		member = api.addToFleet(FleetSide.PLAYER, "espc_chorale_Common", FleetMemberType.SHIP, "EPS Taste for Blood", false);
		pilot = EspcOfficerFactory.MakePilot("Isabelle", "de' Medici", FullName.Gender.FEMALE, Personalities.AGGRESSIVE, 
			"graphics/portraits/espc_isabelle.png", "epsilpac", 4);
		EspcOfficerFactory.PopulateSkills(pilot, new String[]{
			Skills.HELMSMANSHIP, Skills.TARGET_ANALYSIS, 
			Skills.ENERGY_WEAPON_MASTERY, Skills.ORDNANCE_EXPERTISE}, new int[]{
			2, 2, 2, 2});
        member.setCaptain(pilot);
		
		// api.addToFleet(FleetSide.PLAYER, "fury_Attack", FleetMemberType.SHIP, "EPS Kingfisher", false);
		
		// api.addToFleet(FleetSide.PLAYER, "medusa_Attack", FleetMemberType.SHIP, false);
		// api.addToFleet(FleetSide.PLAYER, "espc_militia_Standard", FleetMemberType.SHIP, false);
		
		api.addToFleet(FleetSide.PLAYER, "cerberus_Shielded", FleetMemberType.SHIP, "EPS Indefatigable", false);
		api.addToFleet(FleetSide.PLAYER, "wolf_Assault", FleetMemberType.SHIP, "EPS Cinder", false);
		
		// rondel
		api.addToFleet(FleetSide.PLAYER, "espc_rondel_Standard", FleetMemberType.SHIP, "EPS Sucker Punch", false);
		
		// jackalope
		api.addToFleet(FleetSide.PLAYER, "espc_jackalope_Standard", FleetMemberType.SHIP, "EPS Cottontail", false);
		
		member = api.addToFleet(FleetSide.PLAYER, "lasher_espc_Strike_Dated", FleetMemberType.SHIP, "EPS Retour Près De Toi", false);
		pilot = EspcOfficerFactory.MakePilot("Nola", "Ganymede", FullName.Gender.FEMALE, Personalities.AGGRESSIVE, 
			"graphics/portraits/espc_nola.png", "epsilpac", 11);
		
		EspcOfficerFactory.PopulateSkills(pilot, new String[]{
			Skills.HELMSMANSHIP, "espc_dancing_steps", 
			Skills.BALLISTIC_MASTERY, Skills.TARGET_ANALYSIS, 
			"espc_running_hot", "espc_underdog", 
			Skills.TACTICAL_DRILLS, Skills.COORDINATED_MANEUVERS, 
			Skills.WOLFPACK_TACTICS,
			Skills.ORDNANCE_EXPERTISE, Skills.FLUX_REGULATION}, new int[]{
			2, 2, 2, 
			2, 2, 2, 
			1, 1, 1, 2, 1});
        member.setCaptain(pilot);
		api.getDefaultCommander(FleetSide.PLAYER).setStats(pilot.getStats());
		
		api.addToFleet(FleetSide.PLAYER, "condor_Support", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "tarsus_espc_conv_G", FleetMemberType.SHIP, false);
		
		
		member = api.addToFleet(FleetSide.ENEMY, "dominator_Outdated", FleetMemberType.SHIP, "His Eminence", false);
		pilot = EspcOfficerFactory.MakePilot("Marco", "Reyes", FullName.Gender.MALE, "aggressive", 
			"graphics/portraits/portrait_pirate03.png", "pirates", 5);
		
		EspcOfficerFactory.PopulateSkills(pilot, new String[]{
			Skills.DAMAGE_CONTROL, Skills.BALLISTIC_MASTERY, //
			Skills.TARGET_ANALYSIS, Skills.MISSILE_SPECIALIZATION, 
			Skills.POLARIZED_ARMOR,
			}, new int[]{
			2, 1, 2, 
			2, 2});
        member.setCaptain(pilot);
		
		api.addToFleet(FleetSide.ENEMY, "atlas2_Standard", FleetMemberType.SHIP, false);
		
		api.addToFleet(FleetSide.ENEMY, "eradicator_pirates_Attack", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "eradicator_pirates_Overdriven", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "enforcer_Balanced", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "enforcer_d_Strike", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "enforcer_d_pirates_Strike", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "manticore_pirates_Assault", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "manticore_pirates_Support", FleetMemberType.SHIP, false);
		
		api.addToFleet(FleetSide.ENEMY, "hammerhead_d_CS", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "buffalo2_FS", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "buffalo2_FS", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "gremlin_d_pirates_Strike", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "gremlin_d_pirates_Strike", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "hound_d_pirates_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "hound_d_pirates_Overdriven", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "hound_d_pirates_Overdriven", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "hound_d_pirates_Overdriven", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "lasher_Assault", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "lasher_Assault", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "lasher_Overdriven", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "lasher_d_CS", FleetMemberType.SHIP, false);
		
		
		// Set up the map.
		float width = 24000f;
		float height = 18000f;
		api.initMap((float)-width/2f, (float)width/2f, (float)-height/2f, (float)height/2f);
		
		float minX = -width/2;
		float minY = -height/2;
		
		for (int i = 0; i < 15; i++) {
			float x = (float) Math.random() * width - width/2;
			float y = (float) Math.random() * height - height/2;
			float radius = 100f + (float) Math.random() * 900f; 
			api.addNebula(x, y, radius);
		}
		api.setBackgroundSpriteName("graphics/backgrounds/background2.jpg");
		
		// Add two big nebula clouds
		api.addNebula(minX + width * 0.75f, minY + height * 0.5f, 2000);
		api.addNebula(minX + width * 0.25f, minY + height * 0.5f, 1000);
		

		for (int i = 0; i < 25; i++) {
			float x = (float) Math.random() * width - width/2;
			float y = (float) Math.random() * height - height/2;
			float radius = 1000f + (float) Math.random() * 1000f; 
			api.addNebula(x, y, radius);
		}
		
		api.addObjective(minX + width * 0.4f, minY + height * 0.3f, "comm_relay");
		api.addObjective(minX + width * 0.6f, minY + height * 0.3f, "comm_relay");
		
		// Add an asteroid field going diagonally across the
		// battlefield, 2000 pixels wide, with a maximum of 
		// 100 asteroids in it.
		// 20-70 is the range of asteroid speeds.
		api.addAsteroidField(0f, 0f, (float) Math.random() * 360f, width,
									20f, 70f, 100);
		
		api.addPlugin(new BaseEveryFrameCombatPlugin() {
			FleetMemberAPI playerShip;
			CombatEngineAPI combatEngine;
			int thisHound, thisLasher, thisEradicator = 0;
			boolean didSpawns = false;
			LinkedList<FleetMemberAPI> friendlyFleetMembers;
			
			@SuppressWarnings("rawtypes")
			public void advance(float amount, List events) {
				if (!didSpawns) {
					CombatFleetManagerAPI fleetManager = combatEngine.getFleetManager(FleetSide.ENEMY);
					
					for (FleetMemberAPI thisShip : fleetManager.getReservesCopy()) {
						String hullName = thisShip.getHullId();
						if (hullName.equals("hound_d_pirates")) {
							fleetManager.spawnFleetMember(thisShip, new Vector2f(-350f + Misc.random.nextFloat() * 100f + (600f * (float)(thisHound % 2)),
								2700f + (thisHound < 2 ? -2900f : 0f) + Misc.random.nextFloat() * 200f), (thisHound < 2 ? 70f : 250f) + Misc.random.nextFloat() * 40f, 1f);
							thisHound++;
						} else if (hullName.equals("lasher") 
							// || hullName.equals("lasher_d")
							) {
							fleetManager.spawnFleetMember(thisShip, new Vector2f(-500f + (450f * (float)thisLasher), 3400f + Misc.random.nextFloat() * 200f),
								250f + Misc.random.nextFloat() * 40f, 1f);
							thisLasher++;
						} else if (hullName.equals("dominator")) {
							fleetManager.spawnFleetMember(thisShip, new Vector2f(-1500f, 6000f), 270f, 4f);
						} else if (hullName.equals("atlas2")) {
							fleetManager.spawnFleetMember(thisShip, new Vector2f(1500f, 7000f), 270f, 4f);
						} else if (hullName.equals("eradicator_pirates")) {
							fleetManager.spawnFleetMember(thisShip, new Vector2f((thisEradicator == 0 ? -500f : 500f), 4750f), 270f, 4f);
						}
					}
					
					fleetManager = combatEngine.getFleetManager(FleetSide.PLAYER);
					fleetManager.setSuppressDeploymentMessages(true);
					
					combatEngine.getFleetManager(FleetSide.ENEMY).getTaskManager(false).createAssignment(CombatAssignmentType.ENGAGE,
						fleetManager.getDeployedFleetMember(
							fleetManager.spawnFleetMember(playerShip, new Vector2f(0f, 1500f), 270f, 2f)
						), true
					);
					
					
					Iterator<FleetMemberAPI> friendlyIterator = friendlyFleetMembers.iterator();
					int index = 0;
					int friendlySize = friendlyFleetMembers.size();
					while (friendlyIterator.hasNext()) {
						// 
						((ShipAPI) fleetManager.spawnFleetMember((FleetMemberAPI)friendlyIterator.next(), 
							new Vector2f(
								friendlySize / 2 * -375f + (index % (friendlySize / 2)) * 750f + (index == 0 ? -750f : 0f) +
								(index <= friendlySize / 2 ? 375f : 0f),
								(index > friendlySize / 2 ? -8000f : -8750f)
							), 90f, 5f
						)).setAlly(true);
						index++;
					}
					
					didSpawns = true;
					combatEngine.removePlugin(this);
				}
			}
			
			public void init(CombatEngineAPI engine) {
				engine.getContext().setStandoffRange(10000f);
				combatEngine = engine;
					
				CombatFleetManagerAPI fleetManager = engine.getFleetManager(FleetSide.PLAYER);
				fleetManager.getTaskManager(false).getCPRateModifier().modifyMult("espc_gauss", 0.2f);
				friendlyFleetMembers = new LinkedList<FleetMemberAPI>();
				
				for (FleetMemberAPI thisShip : fleetManager.getReservesCopy()) {
					String hullName = thisShip.getHullId();
					if (hullName.equals("espc_wolf_d")) {
						playerShip = thisShip;
						fleetManager.removeFromReserves(playerShip);
					} else {
						friendlyFleetMembers.addLast(thisShip);
						fleetManager.removeFromReserves(thisShip);
					}
				}		
				
			}
		});
	}

}






