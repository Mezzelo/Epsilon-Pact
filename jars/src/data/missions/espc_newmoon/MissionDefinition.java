package data.missions.espc_newmoon;

import java.util.List;

import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;

import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;

import data.scripts.util.EspcOfficerFactory;

// import org.lwjgl.input.Keyboard;

public class MissionDefinition implements MissionDefinitionPlugin {

	public void defineMission(MissionDefinitionAPI api) {

		
		// Set up the fleets
		api.initFleet(FleetSide.PLAYER, "EPS", FleetGoal.ATTACK, false, 0);
		api.initFleet(FleetSide.ENEMY, "PLS", FleetGoal.ATTACK, true, 10);

		// Set a blurb for each fleet
		api.setFleetTagline(FleetSide.PLAYER, "Epsilon strike fleet");
		api.setFleetTagline(FleetSide.ENEMY, "Persean League response fleet, from Kazeron and Suddene");
		
		// These show up as items in the bulleted list under 
		// "Tactical Objectives" on the mission detail screen
		api.addBriefingItem("Your fleet's designs are slower, but considerably harder-hitting than their midline competition.");
		// api.addBriefingItem("Your fleet's designs lack mobility, but punch well above their weight when working together.");
		api.addBriefingItem("Command aggressively. The opposing fleet is twice as large and will outlast yours otherwise.");
		api.addBriefingItem("The EPS Moonrise must survive");
		
		// Set up the player's fleet
		
		
		FleetMemberAPI member = api.addToFleet(FleetSide.PLAYER, "lasher_espc_Nola", FleetMemberType.SHIP, "EPS Moonrise", true);
		PersonAPI pilot = EspcOfficerFactory.MakePilot("Nola", "Ganymede", FullName.Gender.FEMALE, "steady", 
			"graphics/portraits/espc_nola.png", "epsilpac", 12);
		EspcOfficerFactory.PopulateSkills(pilot, new String[]{
			Skills.HELMSMANSHIP, "espc_dancing_steps", 
			Skills.BALLISTIC_MASTERY, Skills.TARGET_ANALYSIS, 
			"espc_running_hot", "espc_underdog", 
			Skills.TACTICAL_DRILLS, Skills.COORDINATED_MANEUVERS, 
			Skills.WOLFPACK_TACTICS, Skills.CREW_TRAINING,
			Skills.ORDNANCE_EXPERTISE, Skills.FLUX_REGULATION}, new int[]{
			2, 2, 2, 
			2, 2, 2, 
			1, 1, 1, 1, 2, 1});
        member.setCaptain(pilot);
		api.getDefaultCommander(FleetSide.PLAYER).setStats(pilot.getStats());
		
		api.defeatOnShipLoss("EPS Moonrise");
		
		member = api.addToFleet(FleetSide.PLAYER, "espc_chorale_Elite", FleetMemberType.SHIP, "EPS Taste for Blood", false);
		pilot = EspcOfficerFactory.MakePilot("Isabelle", "de' Medici", FullName.Gender.FEMALE, "aggressive", 
			"graphics/portraits/espc_isabelle.png", "epsilpac", 5);
		EspcOfficerFactory.PopulateSkills(pilot, new String[]{
			Skills.HELMSMANSHIP, Skills.TARGET_ANALYSIS, 
			"espc_second_wind", "espc_running_hot", 
			Skills.ORDNANCE_EXPERTISE}, new int[]{
			2, 2,
			1, 2, 2});
        member.setCaptain(pilot);

		api.addToFleet(FleetSide.PLAYER, "espc_observer_Strike", FleetMemberType.SHIP, "EPS Name of God", false);
		api.addToFleet(FleetSide.PLAYER, "espc_pilgrim_Support", FleetMemberType.SHIP, "EPS Tocquiera", false);

		member = api.addToFleet(FleetSide.PLAYER, "espc_flagbearer_Standard", FleetMemberType.SHIP, "EPS Then Comes Light", false);
		pilot = EspcOfficerFactory.MakePilot("Gauss", "", FullName.Gender.FEMALE, "steady", 
			"graphics/portraits/espc_gauss.png", "epsilpac", 4);
		EspcOfficerFactory.PopulateSkills(pilot, new String[]{
			Skills.TARGET_ANALYSIS, Skills.COMBAT_ENDURANCE,
			Skills.SYSTEMS_EXPERTISE, Skills.ORDNANCE_EXPERTISE}, new int[]{
			1, 2, 2, 2});
        member.setCaptain(pilot);
        
		api.addToFleet(FleetSide.PLAYER, "espc_ember_Standard", FleetMemberType.SHIP, false);

		api.addToFleet(FleetSide.PLAYER, "espc_militia_Standard", FleetMemberType.SHIP, false);

		api.addToFleet(FleetSide.PLAYER, "espc_songbird_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "espc_rondel_Anti_Shield", FleetMemberType.SHIP, "EPS Sucker Punch", false);
		api.addToFleet(FleetSide.PLAYER, "espc_jackalope_Strike", FleetMemberType.SHIP, "EPS Cottontail", false);
		
        // member.getRepairTracker().setCR(member.getRepairTracker().getMaxCR());

		
		
		api.addToFleet(FleetSide.ENEMY, "odyssey_Balanced", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "conquest_Elite", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "aurora_Balanced", FleetMemberType.SHIP, false);
		
		api.addToFleet(FleetSide.ENEMY, "champion_Elite", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "eagle_Assault", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "heron_Strike", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "gryphon_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "falcon_Attack", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "falcon_Attack", FleetMemberType.SHIP, false);
		
		api.addToFleet(FleetSide.ENEMY, "hammerhead_Elite", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "sunder_Assault", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "sunder_Assault", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "medusa_PD", FleetMemberType.SHIP, false);
		
		api.addToFleet(FleetSide.ENEMY, "wolf_Assault", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "shrike_Attack", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "shrike_Attack", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "vigilance_Strike", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "vigilance_FS", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "vigilance_Standard", FleetMemberType.SHIP, false);
		
		
		// Set up the map.
		float width = 18000f;
		float height = 24000f;
		api.initMap((float)-width/2f, (float)width/2f, (float)-height/2f, (float)height/2f);
		
		
		for (int i = 0; i < 15; i++) {
			float x = (float) Math.random() * width - width/2;
			float y = (float) Math.random() * height - height/2;
			float radius = 100f + (float) Math.random() * 900f; 
			api.addNebula(x, y, radius);
		}
		
		api.setHyperspaceMode(true);
		api.setBackgroundSpriteName("graphics/backgrounds/hyperspace1.jpg");
		
		//system.setBackgroundTextureFilename("graphics/backgrounds/background2.jpg");
		//api.setBackgroundSpriteName();
		
		// Add an asteroid field going diagonally across the
		// battlefield, 2000 pixels wide, with a maximum of 
		// 100 asteroids in it.
		// 20-70 is the range of asteroid speeds.
		api.addAsteroidField(0f, 0f, (float) Math.random() * 360f, width,
									20f, 70f, 100);
		
		
		api.addPlugin(new BaseEveryFrameCombatPlugin() {
			@SuppressWarnings("rawtypes")
			public void advance(float amount, List events) {
			}
			public void init(CombatEngineAPI engine) {
				engine.getContext().setStandoffRange(10000f);
				engine.removePlugin(this);
			}
		});
	}

}






