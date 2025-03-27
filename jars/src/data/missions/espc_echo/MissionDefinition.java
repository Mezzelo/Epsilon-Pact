package data.missions.espc_echo;

import java.util.List;

import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;

import com.fs.starfarer.api.impl.campaign.ids.StarTypes;

import org.lwjgl.input.Keyboard;

public class MissionDefinition implements MissionDefinitionPlugin {

	public void defineMission(MissionDefinitionAPI api) {

		
		// Set up the fleets
		api.initFleet(FleetSide.PLAYER, "ISS", FleetGoal.ATTACK, false, 0);
		api.initFleet(FleetSide.ENEMY, "EPS", FleetGoal.ATTACK, true, 3);

		if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) && Keyboard.isKeyDown(Keyboard.KEY_Q)) {
			api.setFleetTagline(FleetSide.PLAYER, "Most of what's in this mod LMAO");
			api.setFleetTagline(FleetSide.ENEMY, "my poor unfortunate babies...");
			
			api.addBriefingItem("Hello, intrepid source-code diver (or accidental key-presser), welcome to my ship tester.");
			api.addBriefingItem("Spoilers abound, but you knew that.");
		} else {
			api.setFleetTagline(FleetSide.PLAYER, "The remains of your modest trade fleet");
			api.setFleetTagline(FleetSide.ENEMY, "Whatever THOSE are");
			
			api.addBriefingItem("Defeat all enemy forces");
			api.addBriefingItem("Utilize your range and missile firepower to deter their powerful offense.");
			api.addBriefingItem("Stay close to your allies for support. You'll be quickly overwhelmed otherwise.");
		}

		api.addToFleet(FleetSide.PLAYER, "manticore_Support", FleetMemberType.SHIP, "ISS Blessed Be", true);
		api.addToFleet(FleetSide.PLAYER, "espc_militia_Standard", FleetMemberType.SHIP, "EPS Homeward Bound", false);
		// api.addToFleet(FleetSide.PLAYER, "condor_Support", FleetMemberType.SHIP, false);
		
		// api.addToFleet(FleetSide.PLAYER, "mule_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "vanguard_Attack", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "espc_jackalope_Standard", FleetMemberType.SHIP, "EPS Wayward Gaze", false);
		// api.addToFleet(FleetSide.PLAYER, "espc_songbird_Standard", FleetMemberType.SHIP, "EPS Road Less Traveled", false);
		api.addToFleet(FleetSide.PLAYER, "kite_pirates_Raider", FleetMemberType.SHIP, "Half-A-Prayer", false);
		api.addToFleet(FleetSide.PLAYER, "buffalo_Standard", FleetMemberType.SHIP, false);
		
		// api.addToFleet(FleetSide.ENEMY, "espc_rampart_Support", FleetMemberType.SHIP, "EPS Anywhere Else", false);
		api.addToFleet(FleetSide.ENEMY, "espc_berserker_Assault", FleetMemberType.SHIP, "EPS Anywhere Else", false);
		api.addToFleet(FleetSide.ENEMY, "espc_bastillon_Assault", FleetMemberType.SHIP, "EPS I'm Fading", false);
		api.addToFleet(FleetSide.ENEMY, "espc_picket_Strike", FleetMemberType.SHIP, "EPS Ruthless", false);
		api.addToFleet(FleetSide.ENEMY, "espc_sentry_Support", FleetMemberType.SHIP, "EPS Ruin of House Isner", false);
		api.addToFleet(FleetSide.ENEMY, "espc_sentry_Escort", FleetMemberType.SHIP, "EPS Snikaree", false);
		
		if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) && Keyboard.isKeyDown(Keyboard.KEY_Q)) {
			// api.addToFleet(FleetSide.PLAYER, "espc_gallant_Standard", FleetMemberType.SHIP, false);
			api.addToFleet(FleetSide.PLAYER, "espc_amanuensis_Assault", FleetMemberType.SHIP, false);
			api.addToFleet(FleetSide.PLAYER, "espc_chorale_Support", FleetMemberType.SHIP, false);
			// api.addToFleet(FleetSide.PLAYER, "espc_chorale_lg_Elite", FleetMemberType.SHIP, "TTS Invisible Hand", false);
			api.addToFleet(FleetSide.PLAYER, "espc_pilgrim_Support", FleetMemberType.SHIP, "EPS Tocquiera", false);
			api.addToFleet(FleetSide.PLAYER, "espc_observer_Strike", FleetMemberType.SHIP, "EPS Name of God", false);
			api.addToFleet(FleetSide.PLAYER, "espc_observer_tt_Assault", FleetMemberType.SHIP, "TTS Invisible Hand", false);
			api.addToFleet(FleetSide.PLAYER, "espc_ember_Standard", FleetMemberType.SHIP, false);
			
			api.addToFleet(FleetSide.PLAYER, "espc_venture_Custom", FleetMemberType.SHIP, false);
			api.addToFleet(FleetSide.PLAYER, "espc_militia_o_Standard", FleetMemberType.SHIP, "EPS Something Strange", false);

			api.addToFleet(FleetSide.PLAYER, "espc_jackalope_lp_Strike", FleetMemberType.SHIP, false);
			api.addToFleet(FleetSide.PLAYER, "espc_songbird_tt_Strike", FleetMemberType.SHIP, false);
			api.addToFleet(FleetSide.PLAYER, "espc_rondel_Standard", FleetMemberType.SHIP, false);
			api.addToFleet(FleetSide.PLAYER, "espc_rondel_h_Attack", FleetMemberType.SHIP, false);
			
			api.addToFleet(FleetSide.PLAYER, "espc_pilgrim_h_Assault", FleetMemberType.SHIP, false);
			api.addToFleet(FleetSide.PLAYER, "espc_pilgrim_h_Strike", FleetMemberType.SHIP, false);
			api.addToFleet(FleetSide.PLAYER, "espc_pilgrim_h_Support", FleetMemberType.SHIP, false);
			api.addToFleet(FleetSide.PLAYER, "espc_ember_c_Standard", FleetMemberType.SHIP, false);
			api.addToFleet(FleetSide.PLAYER, "espc_ember_p_Strike", FleetMemberType.SHIP, false);
			
			api.addToFleet(FleetSide.PLAYER, "espc_flagbearer_Standard", FleetMemberType.SHIP, "EPS Then Comes Light", false);
			api.addToFleet(FleetSide.PLAYER, "espc_opossum_Strike", FleetMemberType.SHIP, "EPS Then Comes Light", false);

			api.addToFleet(FleetSide.PLAYER, "espc_picket_Anti_Armor", FleetMemberType.SHIP, "EPS Then Comes Light", false);
			api.addToFleet(FleetSide.PLAYER, "espc_warden_Strike", FleetMemberType.SHIP, "EPS Then Comes Light", false);
			api.addToFleet(FleetSide.PLAYER, "espc_defender_Standard", FleetMemberType.SHIP, "EPS Then Comes Light", false);
			api.addToFleet(FleetSide.PLAYER, "espc_sentry_Support", FleetMemberType.SHIP, "EPS Then Comes Light", false);
			api.addToFleet(FleetSide.PLAYER, "espc_bastillon_Support", FleetMemberType.SHIP, "EPS Then Comes Light", false);
			api.addToFleet(FleetSide.PLAYER, "espc_berserker_Assault", FleetMemberType.SHIP, "EPS Then Comes Light", false);
			api.addToFleet(FleetSide.PLAYER, "espc_rampart_Strike", FleetMemberType.SHIP, "EPS Then Comes Light", false);
			
			api.addToFleet(FleetSide.PLAYER, "glimmer_espc_Escort", FleetMemberType.SHIP, "EPS Then Comes Light", false);
			api.addToFleet(FleetSide.PLAYER, "lumen_Standard", FleetMemberType.SHIP, "EPS Then Comes Light", false);
			api.addToFleet(FleetSide.PLAYER, "fulgent_espc_Assault", FleetMemberType.SHIP, "EPS Then Comes Light", false);
			api.addToFleet(FleetSide.PLAYER, "fulgent_espc_Strike", FleetMemberType.SHIP, "EPS Then Comes Light", false);
			api.addToFleet(FleetSide.PLAYER, "brilliant_espc_Assault", FleetMemberType.SHIP, "EPS Then Comes Light", false);
			api.addToFleet(FleetSide.PLAYER, "brilliant_espc_Support", FleetMemberType.SHIP, "EPS Then Comes Light", false);
			api.addToFleet(FleetSide.PLAYER, "apex_Standard", FleetMemberType.SHIP, "EPS Then Comes Light", false);
			api.addToFleet(FleetSide.PLAYER, "nova_Standard", FleetMemberType.SHIP, "EPS Then Comes Light", false);
			api.addToFleet(FleetSide.PLAYER, "radiant_espc_Strike", FleetMemberType.SHIP, "EPS Then Comes Light", false);
		}
		
		
		// Set up the map.
		float width = 12000f;
		float height = 12000f;
		
		api.initMap((float)-width/2f, (float)width/2f, (float)-height/2f, (float)height/2f);
		
		float minX = -width/2;
		float minY = -height/2;
		
		// Add an asteroid field
		api.addAsteroidField(minX, minY + height / 2, 0, 8000f,
							 20f, 70f, 100);
		
		api.addPlanet(0, 0, 50f, StarTypes.WHITE_DWARF, 250f, true);
		
		
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






