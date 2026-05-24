package data.missions.espc_hydra;

import java.util.List;


import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;

import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.impl.campaign.ids.StarTypes;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import org.lwjgl.util.vector.Vector2f;
import java.util.LinkedList;
import java.util.Iterator;

import data.scripts.util.EspcOfficerFactory;
import data.scripts.util.MezzUtils;

public class MissionDefinition implements MissionDefinitionPlugin {

	public void defineMission(MissionDefinitionAPI api) {


		String enemyPrefix = Global.getSector().getFaction(Factions.INDEPENDENT).getShipNamePrefix();
		String hegPrefix = Global.getSector().getFaction(Factions.HEGEMONY).getShipNamePrefix();
		String tritachPrefix = Global.getSector().getFaction(Factions.TRITACHYON).getShipNamePrefix() + " ";
		String leaguePrefix = Global.getSector().getFaction(Factions.PERSEAN).getShipNamePrefix() + " ";
		
		api.initFleet(FleetSide.PLAYER, hegPrefix, FleetGoal.ATTACK, false, 3);
		api.initFleet(FleetSide.ENEMY, enemyPrefix, FleetGoal.ATTACK, true, 1);
		enemyPrefix += " ";
		hegPrefix += " ";

		api.setFleetTagline(FleetSide.PLAYER, MezzUtils.getString("espc_missionstrings", "hydra_playerTagline"));
		api.setFleetTagline(FleetSide.ENEMY, MezzUtils.getString("espc_missionstrings", "hydra_enemyTagline"));
		
		api.addBriefingItem(MezzUtils.getString("espc_missionstrings", "hydra_brief1"));

		api.addToFleet(FleetSide.PLAYER, "onslaught_Standard", FleetMemberType.SHIP, 
			hegPrefix + MezzUtils.getString("espc_names", "hydra_onslaught"), true);
		
		api.addToFleet(FleetSide.PLAYER, "eradicator_Assault", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "condor_Attack", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "condor_Strike", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "enforcer_Assault", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "enforcer_CS", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "vanguard_Attack", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "lasher_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "lasher_Standard", FleetMemberType.SHIP, false);
		
		api.addToFleet(FleetSide.PLAYER, "astral_Strike", FleetMemberType.SHIP, 
			tritachPrefix + MezzUtils.getString("espc_missionstrings", "hydra_astral"), false);
		api.addToFleet(FleetSide.PLAYER, "fury_Attack", FleetMemberType.SHIP, 
			tritachPrefix + MezzUtils.getString("espc_missionstrings", "hydra_fury"), false);
		api.addToFleet(FleetSide.PLAYER, "medusa_Attack", FleetMemberType.SHIP, 
			tritachPrefix + MezzUtils.getString("espc_missionstrings", "hydra_medusa"), false);
		api.addToFleet(FleetSide.PLAYER, "afflictor_Strike", FleetMemberType.SHIP, 
			tritachPrefix + MezzUtils.getString("espc_missionstrings", "hydra_afflictor"), false);
		api.addToFleet(FleetSide.PLAYER, "tempest_Attack", FleetMemberType.SHIP, 
			tritachPrefix + MezzUtils.getString("espc_missionstrings", "hydra_tempest"), false);
		api.addToFleet(FleetSide.PLAYER, "wolf_Strike", FleetMemberType.SHIP, 
			tritachPrefix + MezzUtils.getString("espc_missionstrings", "hydra_wolf1"), false);
		api.addToFleet(FleetSide.PLAYER, "wolf_Strike", FleetMemberType.SHIP, 
			tritachPrefix + MezzUtils.getString("espc_missionstrings", "hydra_wolf2"), false);
		
		api.addToFleet(FleetSide.PLAYER, "eagle_Assault", FleetMemberType.SHIP, 
			leaguePrefix + MezzUtils.getString("espc_missionstrings", "hydra_eagle"), false);
		api.addToFleet(FleetSide.PLAYER, "falcon_Attack", FleetMemberType.SHIP, 
			leaguePrefix + MezzUtils.getString("espc_missionstrings", "hydra_falcon"), false);
		api.addToFleet(FleetSide.PLAYER, "drover_Strike", FleetMemberType.SHIP, 
			leaguePrefix + MezzUtils.getString("espc_missionstrings", "hydra_drover"), false);
		api.addToFleet(FleetSide.PLAYER, "hammerhead_Elite", FleetMemberType.SHIP, 
			leaguePrefix + MezzUtils.getString("espc_missionstrings", "hydra_hammerhead"), false);
		api.addToFleet(FleetSide.PLAYER, "centurion_Assault", FleetMemberType.SHIP, 
			leaguePrefix + MezzUtils.getString("espc_missionstrings", "hydra_centurion"), false);
		api.addToFleet(FleetSide.PLAYER, "vigilance_Standard", FleetMemberType.SHIP, 
			leaguePrefix + MezzUtils.getString("espc_missionstrings", "hydra_vigilance1"), false);
		api.addToFleet(FleetSide.PLAYER, "vigilance_Strike", FleetMemberType.SHIP, 
			leaguePrefix + MezzUtils.getString("espc_missionstrings", "hydra_vigilance2"), false);

		FleetMemberAPI member;
		PersonAPI pilot;
		/* funny, but a little too funny.  also not good for slower systems.
		FleetMemberAPI member = api.addToFleet(FleetSide.ENEMY, "espc_ashes_radiant_Assault", FleetMemberType.SHIP, "TTDS Nova", false);
		PersonAPI pilot = EspcOfficerFactory.MakePilot("Alpha AI Core", "", FullName.Gender.FEMALE, "reckless", 
			"graphics/portraits/portrait_ai2b.png", "independent", 7);
		pilot.setAICoreId(Commodities.ALPHA_CORE);
		
		EspcOfficerFactory.PopulateSkills(pilot, new String[]{
			Skills.TARGET_ANALYSIS, Skills.ENERGY_WEAPON_MASTERY,
			Skills.SYSTEMS_EXPERTISE, Skills.HELMSMANSHIP,
			Skills.ORDNANCE_EXPERTISE, Skills.MISSILE_SPECIALIZATION, Skills.GUNNERY_IMPLANTS}, new int[]{
			2, 2, 2, 2, 2, 2, 2});
        member.setCaptain(pilot);
		
		
		// api.addToFleet(FleetSide.ENEMY, "onslaught_xiv_Elite", FleetMemberType.SHIP, "HSS Thunder Child", false);
		member = api.addToFleet(FleetSide.ENEMY, "espc_ashes_brilliant_Attack", FleetMemberType.SHIP, "TTDS Visions of Europa", false);
		pilot = EspcOfficerFactory.MakePilot("Alpha AI Core", "", FullName.Gender.FEMALE, "reckless", 
			"graphics/portraits/portrait_ai2b.png", "independent", 7);
		pilot.setAICoreId(Commodities.ALPHA_CORE);
		
		EspcOfficerFactory.PopulateSkills(pilot, new String[]{
			Skills.TARGET_ANALYSIS, Skills.ENERGY_WEAPON_MASTERY,
			Skills.SYSTEMS_EXPERTISE, Skills.HELMSMANSHIP,
			Skills.ORDNANCE_EXPERTISE, Skills.GUNNERY_IMPLANTS, Skills.FIELD_MODULATION}, new int[]{
			2, 2, 2, 2, 2, 2, 2});
        member.setCaptain(pilot);
		*/
		
		member = api.addToFleet(FleetSide.ENEMY, "espc_ashes_champion_Assault", FleetMemberType.SHIP, 
			leaguePrefix + MezzUtils.getString("espc_missionstrings", "hydra_champion_nola"), false);
		pilot = EspcOfficerFactory.MakePilot("Craig", "Hope", FullName.Gender.MALE, Personalities.AGGRESSIVE, 
			"graphics/portraits/portrait_mercenary03.png", "independent", 5);
		
		EspcOfficerFactory.PopulateSkills(pilot, new String[]{
			Skills.TARGET_ANALYSIS, Skills.MISSILE_SPECIALIZATION,
			Skills.GUNNERY_IMPLANTS, Skills.BALLISTIC_MASTERY,
			Skills.ORDNANCE_EXPERTISE}, new int[]{
			1, 1, 1, 1, 2});
        member.setCaptain(pilot);
		member = api.addToFleet(FleetSide.ENEMY, "espc_ashes_apogee_Strike", FleetMemberType.SHIP, false);
		pilot = EspcOfficerFactory.MakePilot("Raymond", "Chen", FullName.Gender.MALE, Personalities.STEADY, 
			"graphics/portraits/portrait_corporate01.png", "independent", 5);
		
		EspcOfficerFactory.PopulateSkills(pilot, new String[]{
			Skills.TARGET_ANALYSIS, Skills.ENERGY_WEAPON_MASTERY,
			Skills.GUNNERY_IMPLANTS, Skills.FIELD_MODULATION,
			Skills.MISSILE_SPECIALIZATION}, new int[]{
			1, 1, 1, 2, 1});
        member.setCaptain(pilot);
		
		api.addToFleet(FleetSide.ENEMY, "espc_ashes_heron_Attack", FleetMemberType.SHIP, false);
		
		member = api.addToFleet(FleetSide.ENEMY, "espc_ashes_gryphon_Strike", FleetMemberType.SHIP, false);
		pilot = EspcOfficerFactory.MakePilot("Lynn Anno", "Nuevo", FullName.Gender.FEMALE, Personalities.STEADY, 
			"graphics/portraits/portrait_mercenary08.png", "independent", 5);
		
		EspcOfficerFactory.PopulateSkills(pilot, new String[]{
			Skills.TARGET_ANALYSIS, Skills.HELMSMANSHIP,
			Skills.GUNNERY_IMPLANTS, Skills.FIELD_MODULATION,
			Skills.MISSILE_SPECIALIZATION}, new int[]{
			1, 1, 1, 1, 2});
        member.setCaptain(pilot);
		
		member = api.addToFleet(FleetSide.ENEMY, "espc_ashes_medusa_Attack", FleetMemberType.SHIP, false);
		pilot = EspcOfficerFactory.MakePilot("Anurak", "Kerr", FullName.Gender.MALE, Personalities.STEADY, 
			"graphics/portraits/portrait_league07.png", "independent", 5);
		
		EspcOfficerFactory.PopulateSkills(pilot, new String[]{
			Skills.ORDNANCE_EXPERTISE, Skills.HELMSMANSHIP,
			Skills.SYSTEMS_EXPERTISE, Skills.FIELD_MODULATION,
			Skills.ENERGY_WEAPON_MASTERY}, new int[]{
			1, 2, 1, 1, 1});
        member.setCaptain(pilot);
		
		
		member = api.addToFleet(FleetSide.ENEMY, "espc_ashes_hammerhead_Elite", FleetMemberType.SHIP, 
			enemyPrefix + MezzUtils.getString("espc_missionstrings", "hydra_hammerhead_nola"), false);
		pilot = EspcOfficerFactory.MakePilot("Vacha", "Temujax", FullName.Gender.FEMALE, Personalities.STEADY, 
			"graphics/portraits/portrait27.png", "independent", 5);
		
		EspcOfficerFactory.PopulateSkills(pilot, new String[]{
			Skills.TARGET_ANALYSIS, Skills.HELMSMANSHIP,
			Skills.GUNNERY_IMPLANTS, Skills.BALLISTIC_MASTERY,
			Skills.ORDNANCE_EXPERTISE}, new int[]{
			1, 1, 1, 2, 1});
        member.setCaptain(pilot);
		
		
		member = api.addToFleet(FleetSide.ENEMY, "espc_ashes_scarab_Escort", FleetMemberType.SHIP, false);
		pilot = EspcOfficerFactory.MakePilot("Lynn", "Fares", FullName.Gender.FEMALE, Personalities.AGGRESSIVE, 
			"graphics/portraits/portrait_league08.png", "independent", 5);
		
		EspcOfficerFactory.PopulateSkills(pilot, new String[]{
			Skills.COMBAT_ENDURANCE, Skills.HELMSMANSHIP,
			Skills.SYSTEMS_EXPERTISE, Skills.ORDNANCE_EXPERTISE,
			Skills.ENERGY_WEAPON_MASTERY}, new int[]{
			1, 2, 1, 1, 1});
        member.setCaptain(pilot);
		
		member = api.addToFleet(FleetSide.ENEMY, "espc_ashes_afflictor_Strike", FleetMemberType.SHIP, 
			MezzUtils.getString("espc_missionstrings", "hydra_afflictor_nola"), false);
		pilot = EspcOfficerFactory.MakePilot("Halcyon", "Oni", FullName.Gender.FEMALE, Personalities.AGGRESSIVE, 
			"graphics/portraits/portrait_luddic11.png", "independent", 5);
		
		EspcOfficerFactory.PopulateSkills(pilot, new String[]{
			Skills.TARGET_ANALYSIS, Skills.HELMSMANSHIP,
			Skills.SYSTEMS_EXPERTISE, Skills.FIELD_MODULATION,
			Skills.COMBAT_ENDURANCE}, new int[]{
			1, 2, 1, 1, 1});
        member.setCaptain(pilot);
		
		member = api.addToFleet(FleetSide.ENEMY, "espc_ashes_shade_Assault", FleetMemberType.SHIP, false);
		pilot = EspcOfficerFactory.MakePilot("Leita", "Navarez", FullName.Gender.FEMALE, Personalities.RECKLESS, 
			"graphics/portraits/portrait44.png", "independent", 5);
		
		EspcOfficerFactory.PopulateSkills(pilot, new String[]{
			Skills.MISSILE_SPECIALIZATION, Skills.HELMSMANSHIP,
			Skills.SYSTEMS_EXPERTISE, Skills.FIELD_MODULATION,
			Skills.COMBAT_ENDURANCE}, new int[]{
			2, 1, 1, 1, 1});
        member.setCaptain(pilot);
		
		api.addToFleet(FleetSide.ENEMY, "espc_ashes_tempest_Attack", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "espc_ashes_omen_Escort", FleetMemberType.SHIP, false);
		
		api.addToFleet(FleetSide.ENEMY, "espc_ashes_wolf_Escort", FleetMemberType.SHIP, false);
		
		
		// Set up the map.
		float width = 20000f;
		float height = 18000f;
		api.initMap((float)-width/2f, (float)width/2f, (float)-height/2f, (float)height/2f);
		

		api.addPlanet(400f, -600f, 70f, StarTypes.WHITE_DWARF, 250f, true);
		api.addPlanet(-100f, -100f, 200f, "ice_giant", 250f, true);
		api.addPlanet(300f, -200f, 120f, "barren", 250f, true);
		
		/*
		for (int i = 0; i < 15; i++) {
			float x = (float) Math.random() * width - width/2;
			float y = (float) Math.random() * height - height/2;
			float radius = 100f + (float) Math.random() * 900f;
		} */
		api.setBackgroundSpriteName("graphics/backgrounds/background3.jpg");
		
		api.addAsteroidField(0f, 0f, (float) Math.random() * 360f, width,
									20f, 70f, 300);
		
		
		api.addPlugin(new BaseEveryFrameCombatPlugin() {
			FleetMemberAPI playerShip;
			CombatEngineAPI combatEngine;
			boolean didSpawns = false;
			LinkedList<FleetMemberAPI> hgMembers, ttMembers, plMembers;
			
			@SuppressWarnings("rawtypes")
			public void advance(float amount, List events) {
				if (!didSpawns) {
					CombatFleetManagerAPI fleetManager = combatEngine.getFleetManager(FleetSide.PLAYER);
					fleetManager.setSuppressDeploymentMessages(true);
					fleetManager.spawnFleetMember(playerShip, new Vector2f(0f, -7500f), 90f, 5f);
					
					Iterator<FleetMemberAPI> fleetIterator = hgMembers.iterator();
					int index = 0;
					int fleetSize = hgMembers.size();
					while (fleetIterator.hasNext()) {
						// 
						fleetManager.spawnFleetMember((FleetMemberAPI)fleetIterator.next(), 
							new Vector2f(
								fleetSize / 2 * -375f + (index % (fleetSize / 2)) * 750f + (index == 0 ? -750f : 0f) +
								(index <= fleetSize / 2 ? 375f : 0f),
								(index > fleetSize / 2 ? -8000f : -8750f)
							), 90f, 5f
						);
						index++;
					}
					
					fleetIterator = ttMembers.iterator();
					index = 0;
					fleetSize = ttMembers.size();
					while (fleetIterator.hasNext()) {
						// 
						((ShipAPI) fleetManager.spawnFleetMember((FleetMemberAPI)fleetIterator.next(), 
							new Vector2f(
								(index > fleetSize / 2 ? 9000f : 9750f),
								1000f + fleetSize / 2 * -375f + (index % (fleetSize / 2)) * 750f + (index == 0 ? -750f : 0f) +
								(index <= fleetSize / 2 ? 375f : 0f)
								
							), 180f, 5f
						)).setAlly(true);
						index++;
					}
					
					fleetIterator = plMembers.iterator();
					index = 0;
					fleetSize = plMembers.size();
					while (fleetIterator.hasNext()) {
						// 
						((ShipAPI) fleetManager.spawnFleetMember((FleetMemberAPI)fleetIterator.next(), 
							new Vector2f(
								(index > fleetSize / 2 ? -9000f : -9750f),
								1000f + fleetSize / 2 * -375f + (index % (fleetSize / 2)) * 750f + (index == 0 ? -750f : 0f) +
								(index <= fleetSize / 2 ? 375f : 0f)
								
							), 0f, 5f
						)).setAlly(true);
						index++;
					}
					combatEngine.getFleetManager(FleetSide.ENEMY).getTaskManager(false).setPreventFullRetreat(true);
					
					didSpawns = true;
					combatEngine.removePlugin(this);
				}
			}
			
			public void init(CombatEngineAPI engine) {
				engine.getContext().aiRetreatAllowed = false;
				engine.getContext().setStandoffRange(10000f);
				combatEngine = engine;
				CombatFleetManagerAPI fleetManager = engine.getFleetManager(FleetSide.PLAYER);
				
				hgMembers = new LinkedList<FleetMemberAPI>();
				ttMembers = new LinkedList<FleetMemberAPI>();
				plMembers = new LinkedList<FleetMemberAPI>();
				
				String midline = Global.getSettings().getHullSpec("eagle").getManufacturer();
				String xiv = Global.getSettings().getHullSpec("onslaught_xiv").getManufacturer();
				// String hightech = Global.getSettings().getHullSpec("aurora").getManufacturer();
				
				for (FleetMemberAPI thisShip : fleetManager.getReservesCopy()) {
					String designType = thisShip.getHullSpec().getManufacturer();
					if (designType.equals(MezzUtils.lowtechString)) {
						if (thisShip.getHullSpec().getHullName().equals("Onslaught")) {
							playerShip = thisShip;
						} else
							hgMembers.addLast(thisShip);
					} else if (designType.equals(midline))
						plMembers.addLast(thisShip);
					else if (designType.equals(xiv))
						hgMembers.addLast(thisShip);
					// high tech
					else
						ttMembers.addLast(thisShip);
					
					fleetManager.removeFromReserves(thisShip);
				}		
				
			}
		});
	}

}






