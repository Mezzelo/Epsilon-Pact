package data.missions.espc_looseends;

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

public class MissionDefinition implements MissionDefinitionPlugin {

	public void defineMission(MissionDefinitionAPI api) {

		
		// Set up the fleets
		api.initFleet(FleetSide.PLAYER, "TTDS", FleetGoal.ATTACK, false, 0);
		api.initFleet(FleetSide.ENEMY, "HSS", FleetGoal.ATTACK, true, 3);

		// Set a blurb for each fleet
		api.setFleetTagline(FleetSide.PLAYER, "A wandering spacer and her unlikely allies");
		api.setFleetTagline(FleetSide.ENEMY, "The HSS Burgundy, with heavy escort");
		
		api.addBriefingItem("Your cheap, low-tech frigate is well-equipped and helmed by an exceptional captain.");
		api.addBriefingItem("Your allies will not stand a chance without your tactical command and field presence.");
		api.addBriefingItem("Keep the enemy flagship away from your allies as you hunt down her escort.");
		api.addBriefingItem("The ISS Moonrise must survive");
		
		FleetMemberAPI member = api.addToFleet(FleetSide.PLAYER, "lasher_espc_Nola", FleetMemberType.SHIP, "ISS Moonrise", true);
		PersonAPI pilot = EspcOfficerFactory.MakePilot("Nola", "Ganymede", FullName.Gender.FEMALE, "aggressive", 
			"graphics/portraits/espc_nola_young.png", "independent", 11);
		api.defeatOnShipLoss("ISS Moonrise");
		
		EspcOfficerFactory.PopulateSkills(pilot, new String[]{
			Skills.HELMSMANSHIP, Skills.FIELD_MODULATION, 
			Skills.BALLISTIC_MASTERY, Skills.TARGET_ANALYSIS, 
			Skills.SYSTEMS_EXPERTISE, Skills.GUNNERY_IMPLANTS, 
			Skills.TACTICAL_DRILLS, Skills.COORDINATED_MANEUVERS, 
			Skills.WOLFPACK_TACTICS, Skills.ORDNANCE_EXPERTISE,
			Skills.FLUX_REGULATION, 
			"espc_dancing_steps",
			"espc_running_hot", "espc_underdog"}, new int[]{
			2, 2, 2, 
			2, 1, 2, 
			1, 1, 1, 2, 1,
			2, 1, 2});
        member.setCaptain(pilot);
		api.getDefaultCommander(FleetSide.PLAYER).setStats(pilot.getStats());
		
		api.addToFleet(FleetSide.PLAYER, "brilliant_Standard", FleetMemberType.SHIP, false);
		
		api.addToFleet(FleetSide.PLAYER, "scintilla_Strike", FleetMemberType.SHIP, false);
		
		api.addToFleet(FleetSide.PLAYER, "fulgent_Assault", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "fulgent_Support", FleetMemberType.SHIP, false);
		
		api.addToFleet(FleetSide.PLAYER, "glimmer_Assault", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "glimmer_Assault", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "glimmer_Support", FleetMemberType.SHIP, false);
		
		api.addToFleet(FleetSide.PLAYER, "lumen_Standard", FleetMemberType.SHIP, false);
		
		//FleetMemberType.SHIP, "name", true, CrewXPLevel.ELITE); ?
		
		// Mark player flagship as essential
		
		// Set up the enemy fleet

		member = api.addToFleet(FleetSide.ENEMY, "onslaught_xiv_Elite", FleetMemberType.SHIP, "HSS Burgundy", false);
		pilot = EspcOfficerFactory.MakePilot("Lanya", "Neil", FullName.Gender.FEMALE, "steady", 
			"graphics/portraits/portrait_hegemony04.png", "hegemony", 5);
		
		EspcOfficerFactory.PopulateSkills(pilot, new String[]{
			Skills.FIELD_MODULATION, Skills.COMBAT_ENDURANCE,
			Skills.POLARIZED_ARMOR, Skills.ORDNANCE_EXPERTISE,
			Skills.IMPACT_MITIGATION
			}, new int[]{
			2, 2, 1, 2, 2});
        member.setCaptain(pilot);
		
		api.addToFleet(FleetSide.ENEMY, "eagle_xiv_Elite", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "gryphon_FS", FleetMemberType.SHIP, false);
		// api.addToFleet(FleetSide.ENEMY, "mora_Support", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "drover_Strike", FleetMemberType.SHIP, false);
		
		api.addToFleet(FleetSide.ENEMY, "enforcer_Assault", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "enforcer_XIV_Elite", FleetMemberType.SHIP, false);

		api.addToFleet(FleetSide.ENEMY, "wolf_hegemony_CS", FleetMemberType.SHIP, false);	
		api.addToFleet(FleetSide.ENEMY, "lasher_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "lasher_Assault", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "hound_hegemony_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "hound_hegemony_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "kite_hegemony_Interceptor", FleetMemberType.SHIP, false);
		
		
		// Set up the map.
		float width = 24000f;
		float height = 18000f;
		api.initMap((float)-width/2f, (float)width/2f, (float)-height/2f, (float)height/2f);
		
		
		for (int i = 0; i < 15; i++) {
			float x = (float) Math.random() * width - width/2;
			float y = (float) Math.random() * height - height/2;
			float radius = 100f + (float) Math.random() * 900f; 
			api.addNebula(x, y, radius);
		}
		
		api.setHyperspaceMode(true);
		api.setBackgroundSpriteName("graphics/backgrounds/background5.jpg");
		
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






