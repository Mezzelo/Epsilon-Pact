package data.scripts.campaign.fleets;

import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.fleets.PersonalFleetScript;
import com.fs.starfarer.api.impl.campaign.ids.Abilities;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.missions.FleetCreatorMission;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.FleetQuality;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.FleetSize;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.OfficerNum;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.OfficerQuality;
import com.fs.starfarer.api.impl.campaign.missions.hub.MissionFleetAutoDespawn;
// import com.fs.starfarer.api.impl.campaign.ids.People;
// import com.fs.starfarer.api.impl.campaign.ids.Factions;




public class espc_PersonalFleetNola extends PersonalFleetScript {
	
	/*
	private class NolaFleetMember {
		String variant;
		String[] wepsToReplace;
		String[] replacements;
		String[] sMods;
		
		public NolaFleetMember(String variant, String[] wepsToReplace, String[] replacements, String[] sMods) {
			this.variant = variant;
			this.wepsToReplace = wepsToReplace;
			this.replacements = replacements;
			this.sMods = sMods;
		}
	}
	*/
	
	private FleetMemberAPI MakeNolaShip (String variant, String[] wepsToReplace, String[] replacements, String[] sMods,
		PersonAPI commander) {
		ShipVariantAPI customVar = Global.getSettings().getVariant(variant).clone();
		for (String weaponSlot : customVar.getFittedWeaponSlots()) {
			boolean found = false;
			for (int i = 0; i < wepsToReplace.length && !found; i++) {
				if (customVar.getWeaponId(weaponSlot).equals(wepsToReplace[i])) {
					found = true;
					customVar.addWeapon(weaponSlot, replacements[i]);
				}
			}
		}
		for (String sMod : sMods) {
			customVar.addMod(sMod);
			customVar.addPermaMod(sMod, true);
		}
		
		FleetMemberAPI ship = Global.getFactory().createFleetMember(FleetMemberType.SHIP, customVar);
		
		ship.getCrewComposition().setCrew(ship.getMaxCrew());
		ship.getRepairTracker().setCR(0.7f);
		ship.setCaptain(commander);
		Global.getSector().getPlayerFleet().getFleetData().addFleetMember(ship);
		return ship;
	}
	
	public espc_PersonalFleetNola() {
		super("espc_nola");
		setMinRespawnDelayDays(10f);
		setMaxRespawnDelayDays(20f);
	}

	@Override
	protected MarketAPI getSourceMarket() {
		return Global.getSector().getEconomy().getMarket("sindria");
	}
	
	@Override
	public void advance(float amount) {
		super.advance (amount);
	}
	
	@Override
	public CampaignFleetAPI spawnFleet() {
		
		MarketAPI lunron = getSourceMarket();
		
		FleetCreatorMission m = new FleetCreatorMission(random);
		m.beginFleet();
		
		Vector2f loc = lunron.getLocationInHyperspace();
		
		m.triggerCreateFleet(FleetSize.TINY, FleetQuality.DEFAULT, "epsilpac", FleetTypes.TASK_FORCE, loc);
		m.triggerSetFleetOfficers( OfficerNum.NONE, OfficerQuality.DEFAULT);
		m.triggerSetFleetCommander(getPerson());
		m.triggerSetFleetFaction("epsilpac");
		m.triggerSetPatrol();
		m.triggerSetFleetMemoryValue(MemFlags.MEMORY_KEY_SOURCE_MARKET, lunron);
		m.triggerFleetSetNoFactionInName();
		m.triggerPatrolAllowTransponderOff();
		m.triggerFleetSetName("Nola's Fleet");
		m.triggerOrderFleetPatrol(lunron.getStarSystem());
		
		CampaignFleetAPI fleet = m.createFleet();
		
		fleet.getFleetData().addFleetMember(MakeNolaShip("espc_Amanuensis_Nola", 
			new String[] {"ionbeam", "ioncannon"}, new String[] {"rift_lightning", "inimical_emanation"}, 
			new String[] {"targetingunit", "hardenedshieldemitter"}, null)
		);
		
		fleet.addAbility(Abilities.GENERATE_SLIPSURGE);
		fleet.addAbility(Abilities.REVERSE_POLARITY);
		fleet.addAbility(Abilities.INTERDICTION_PULSE);
		fleet.addAbility(Abilities.TRANSVERSE_JUMP);
		fleet.addAbility(Abilities.TRANSPONDER);
		fleet.setTransponderOn(false);
		fleet.removeScriptsOfClass(MissionFleetAutoDespawn.class);
		lunron.getContainingLocation().addEntity(fleet);
		fleet.setLocation(lunron.getPlanetEntity().getLocation().x, lunron.getPlanetEntity().getLocation().y);
		fleet.setFacing((float) random.nextFloat() * 360f);
		
		return fleet;
	}

	@Override
	public boolean canSpawnFleetNow() {
		MarketAPI lunron = Global.getSector().getEconomy().getMarket("espc_lunron");
		if (lunron == null || lunron.hasCondition(Conditions.DECIVILIZED)) return false;
		if (!lunron.getFactionId().equals("epsilpac")) return false;
		return true;
	}

	@Override
	public boolean shouldScriptBeRemoved() {
		return false;
	}

}




