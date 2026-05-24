package data.missions.espc_remains;

import java.util.List;

import org.lwjgl.input.Keyboard;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;

import data.scripts.util.MezzUtils;

public class MissionDefinition implements MissionDefinitionPlugin {

	public void defineMission(MissionDefinitionAPI api) {

		String playerPrefix = Global.getSector().getFaction(Factions.TRITACHYON).getShipNamePrefix();
		String remnantPrefix = Global.getSector().getFaction(Factions.REMNANTS).getShipNamePrefix();
		String pactPrefix = Global.getSector().getFaction(MezzUtils.factionIdPact).getShipNamePrefix() + " ";
		
		// Set up the fleets
		api.initFleet(FleetSide.PLAYER, playerPrefix, FleetGoal.ATTACK, false, 3);
		api.initFleet(FleetSide.ENEMY, remnantPrefix, FleetGoal.ATTACK, true, 10);
		
		String flagshipName = Global.getSector().getFaction(Factions.TRITACHYON).getShipNamePrefix() + " " +
			MezzUtils.getString("espc_missionstrings", "remains_flagship");

		// Set a blurb for each fleet
		api.setFleetTagline(FleetSide.PLAYER, String.format(
			MezzUtils.getString("espc_missionstrings", "remains_playerTagline"), 
			flagshipName));
		api.setFleetTagline(FleetSide.ENEMY, MezzUtils.getString("espc_missionstrings", "remains_enemyTagline"));
		
		api.addBriefingItem(MezzUtils.getString("espc_missionstrings", "remains_brief1"));
		api.addBriefingItem(MezzUtils.getString("espc_missionstrings", "remains_brief2"));
		api.addBriefingItem(String.format(MezzUtils.getString("espc_missionstrings", "remains_brief3"), flagshipName));
		
		// Set up the player's fleet
		api.addToFleet(FleetSide.PLAYER, "odyssey_Balanced", FleetMemberType.SHIP, flagshipName, true);
		
		// Mark player flagship as essential
		if (!Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) && !Keyboard.isKeyDown(Keyboard.KEY_Q))
			api.defeatOnShipLoss(flagshipName);
		
		// api.addToFleet(FleetSide.PLAYER, "doom_Strike", FleetMemberType.SHIP, false);
		// api.addToFleet(FleetSide.PLAYER, "aurora_Balanced", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "fury_Attack", FleetMemberType.SHIP, false);
		// api.addToFleet(FleetSide.PLAYER, "fury_Support", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "apogee_Balanced", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "venture_Balanced", FleetMemberType.SHIP, false);
		
		// api.addToFleet(FleetSide.PLAYER, "medusa_Attack", FleetMemberType.SHIP, false);
		
		// api.addToFleet(FleetSide.PLAYER, "afflictor_Strike", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "shrike_Attack", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "shrike_Attack", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "shade_Assault", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "tempest_Attack", FleetMemberType.SHIP, false);
		// api.addToFleet(FleetSide.PLAYER, "wolf_Strike", FleetMemberType.SHIP, false);
		// api.addToFleet(FleetSide.PLAYER, "wolf_Strike", FleetMemberType.SHIP, false);
		// api.addToFleet(FleetSide.PLAYER, "wolf_Strike", FleetMemberType.SHIP, false);
		
		// api.addToFleet(FleetSide.PLAYER, "nebula_Standard", FleetMemberType.SHIP, false);
		// api.addToFleet(FleetSide.PLAYER, "phaeton_Standard", FleetMemberType.SHIP, false);
		
		
		// Set up the enemy fleet
		
		// api.addToFleet(FleetSide.ENEMY, "apex_espc_Strike", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "brilliant_espc_Support", FleetMemberType.SHIP, false);
		
		api.addToFleet(FleetSide.ENEMY, "scintilla_Strike", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "fulgent_espc_Assault", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "fulgent_Support", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "glimmer_espc_Escort", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "glimmer_Assault", FleetMemberType.SHIP, false);
		// api.addToFleet(FleetSide.ENEMY, "lumen_Standard", FleetMemberType.SHIP, false);
		
		// atavist
		// api.addToFleet(FleetSide.ENEMY, "espc_atavist_Strike", FleetMemberType.SHIP, "EPS Senescence", false);
		
		api.addToFleet(FleetSide.ENEMY, "espc_observer_Assault", FleetMemberType.SHIP, pactPrefix + 
			MezzUtils.getString("espc_missionstrings", "remains_observer"), false);
		api.addToFleet(FleetSide.ENEMY, "espc_pilgrim_Assault", FleetMemberType.SHIP, pactPrefix + 
			MezzUtils.getString("espc_missionstrings", "remains_pilgrim"), false);
		
		api.addToFleet(FleetSide.ENEMY, "espc_ember_Standard", FleetMemberType.SHIP, pactPrefix + 
			MezzUtils.getString("espc_missionstrings", "remains_ember"), false);
		
		api.addToFleet(FleetSide.ENEMY, "espc_songbird_Standard", FleetMemberType.SHIP, pactPrefix + 
			MezzUtils.getString("espc_missionstrings", "remains_songbird"), false);
		api.addToFleet(FleetSide.ENEMY, "omen_PD", FleetMemberType.SHIP, pactPrefix + 
			MezzUtils.getString("espc_missionstrings", "remains_omen"), false);
		api.addToFleet(FleetSide.ENEMY, "espc_jackalope_Strike", FleetMemberType.SHIP, pactPrefix + 
			MezzUtils.getString("espc_names", "newmoon_jackalope"), false);
		api.addToFleet(FleetSide.ENEMY, "espc_rondel_Standard", FleetMemberType.SHIP, pactPrefix + 
			MezzUtils.getString("espc_missionstrings", "remains_rondel"), false);
		api.addToFleet(FleetSide.ENEMY, "espc_opossum_Strike", FleetMemberType.SHIP, pactPrefix + 
			MezzUtils.getString("espc_missionstrings", "remains_opossum"), false);
		api.addToFleet(FleetSide.ENEMY, "lasher_espc_Strike", FleetMemberType.SHIP, pactPrefix + 
			MezzUtils.getString("espc_missionstrings", "remains_lasher"), false);
		
		
		
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
		api.addObjective(0f, 0f, "sensor_array");
		
		api.setBackgroundSpriteName("graphics/backgrounds/hyperspace1.jpg");
		//api.setBackgroundSpriteName("graphics/backgrounds/background2.jpg");
		
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
				engine.getContext().aiRetreatAllowed = false;
				engine.getFleetManager(FleetSide.ENEMY).getTaskManager(false).setPreventFullRetreat(true);
				engine.removePlugin(this);
			}
		});
	}

}