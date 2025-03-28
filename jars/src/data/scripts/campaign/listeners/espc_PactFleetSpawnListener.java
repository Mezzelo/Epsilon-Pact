package data.scripts.campaign.listeners;
// doing this through here rather than BaseGenerateFleetOfficersPlugin, as the generic officer generation is fine for my purposes
// there's just a few exceptions i'd like to impose.

import java.util.LinkedList;
import java.util.List;

// you can do this through the fleet inflater apparently?  i still don't know what the fuck that is need to glaze my eyes over
// on the api for longer.  this works for the moment.

import com.fs.starfarer.api.Global;
// import com.fs.starfarer.api.Global;
// import com.fs.starfarer.api.Global;
// import com.fs.starfarer.api.EveryFrameScript;
// import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
// import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI.SkillLevelAPI;
// import com.fs.starfarer.api.characters.MutableCharacterStatsAPI.SkillLevelAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
// 	import com.fs.starfarer.api.combat.ShipHullSpecAPI.ShipTypeHints;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflaterParams;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.impl.campaign.intel.PersonBountyIntel;
import com.fs.starfarer.api.util.Misc;

import data.scripts.campaign.fleets.espc_PactFleetInflater;

// import com.fs.starfarer.api.campaign.econ.MarketAPI;
// import com.fs.starfarer.api.campaign.listeners.FleetSpawnListener;
// import com.fs.starfarer.api.characters.OfficerDataAPI;
//import com.fs.starfarer.api.impl.campaign.ids.Abilities;
//import com.fs.starfarer.api.impl.campaign.ids.Factions;
// import java.util.Iterator;
// import java.util.List;

// import org.apache.log4j.Logger;

public class espc_PactFleetSpawnListener extends BaseCampaignEventListener {

	public espc_PactFleetSpawnListener(boolean permaRegister) {
		super(permaRegister);
	}
	
	public static String[] portraitList = {
		"alma",
		"lindsay",
		"nadia",
		"tam",
		"carl",
		"lin",
		"ross",
		"sien",
		"ken",
	};
	
	@Override
	public void reportFleetSpawned(CampaignFleetAPI fleet) {
		if (!(
			// pact fleets (duh)
			fleet.getFaction().getId().equals("epsilpac") || 
			// pact bounty fleets
			(fleet.getFaction().getId().equals(Factions.NEUTRAL) && fleet.getNameWithFactionKeepCase().contains("'s Fleet")) ||
			// nex pact allied fleets, under a different faction
			fleet.getNameWithFactionKeepCase().contains("Epsilon Pact")))
			return;
		
		boolean isBounty = fleet.getFaction().getId().equals(Factions.NEUTRAL);
		
		if (isBounty) {
			boolean found = false;
	        for (FleetEventListener listener : fleet.getEventListeners()) {
	        	if (listener instanceof PersonBountyIntel) {
	        		PersonBountyIntel intel = (PersonBountyIntel) listener;
	        		if (intel.getFactionForUIColors().getId().equals("epsilpac")) {
	        			found = true;
	        			break;
	        		}
	        	}
	        }
	        if (!found)
	        	return;
		}
		
		if (isBounty) {
			boolean found = false;
	        for (FleetMemberAPI fleetMember : fleet.getFleetData().getMembersListCopy()) {
	        	found = (fleetMember.getHullSpec().getManufacturer().equals("Pact-Explorarium") ||
	        		fleetMember.getHullSpec().getManufacturer().equals("Epsilon Pact"));
	        	if (found)
	        		break;
	        }
			if (!found)
				return;
		}
		if (fleet.getInflater() != null && 
			!(fleet.getInflater() instanceof espc_PactFleetInflater) &&
			fleet.getInflater().getParams() instanceof DefaultFleetInflaterParams &&
			!fleet.isStationMode())
			fleet.setInflater(new espc_PactFleetInflater((DefaultFleetInflaterParams) fleet.getInflater().getParams()));
        for (FleetMemberAPI fleetMember : fleet.getFleetData().getMembersListCopy()) {
			PersonAPI person = fleetMember.getCaptain();
			if (person != null) {
				// ignore nex allied faction fleets by checking for portrait.  how convenient it is that this mod uses its own portraits lol
				if (!person.getPortraitSprite().contains("espc"))
					continue;
				
				// can't just check for size > 4, in case of added skill categories, i'm guessing.
				boolean hasNonZeroSkill = false;
				for (SkillLevelAPI skill : person.getStats().getSkillsCopy()) {
					hasNonZeroSkill = skill.getLevel() > 0f;
					if (hasNonZeroSkill)
						break;
				}
				if (hasNonZeroSkill) {
					// if (!isBounty)
					adjustSkills(person, fleetMember);
				} else
					continue;
				// if (fleetMember.getHullSpec().getHints().contains(ShipTypeHints.UNBOARDABLE)) {
				if (Misc.isAutomated(fleetMember)) {
					// hasCores = true;
					
					int coreType = Misc.random.nextInt(11);
					if (coreType > 8)
						coreType = 2;
					else if (coreType > 4)
						coreType = 3;
					else
						coreType = 1;
					person.setPortraitSprite("graphics/portraits/portrait_ai" + coreType + "b.png");
					// if (Misc.random.nextFloat() > 0.5f)
					// else
					// 	person.setPortraitSprite("graphics/portraits/portrait_ai" + coreType + ".png");
					
					FullName name = person.getName();
					if (Misc.random.nextFloat() > 0.35f || isBounty) {
						if (Misc.random.nextFloat() > 0.7f)
							name.setFirst(name.getLast());
						name.setLast("");
						if (Misc.random.nextFloat() > 0.85) {
							person.setPortraitSprite(Global.getSettings().getSpriteName(
								"characters", "espc_" + portraitList[Misc.random.nextInt(portraitList.length)]));
						}
					} else {
						name.setLast("Core");
						if (coreType == 1)
							name.setFirst("Gamma");
						else if (coreType == 2)
							name.setFirst("Alpha");
						else
							name.setFirst("Beta");
					}
					person.setName(name);
					// fleetMember.setCaptain(person);
				}
	        }
        }
        /*
        if (hasCores && fleet.getFlagship() != null && !Misc.isAutomated(fleet.getFlagship())) {
            PersonAPI commander = fleet.getCommander();
            // commander.getStats().setSkillLevel(
            // 	((SkillLevelAPI) commander.getStats().getSkillsCopy().get(0)).getSkill().getId()
            // , 0);
            commander.getStats().setSkillLevel("espc_core_communications", 1);
        } */
        
	}
	
	private String[] skillsFrigate = {
		Skills.COMBAT_ENDURANCE,
		Skills.FIELD_MODULATION,
		"espc_underdog",
		"espc_dancing_steps"
	};
	
	private String[] skillsDestroyer = {
		Skills.FIELD_MODULATION,
		Skills.POINT_DEFENSE,
		Skills.SYSTEMS_EXPERTISE,
		"espc_running_hot",
		"espc_second_wind",
		"espc_unburdened",
		"espc_underdog",
	};
	
	private String[] skillsCruiser = {
		Skills.FIELD_MODULATION,
		Skills.ORDNANCE_EXPERTISE,
		Skills.POINT_DEFENSE,
		Skills.SYSTEMS_EXPERTISE,
		"espc_running_hot",
		"espc_unburdened",
		"espc_second_wind"
	};
	
	private String[] grabBag = {
		Skills.FIELD_MODULATION,
		Skills.ORDNANCE_EXPERTISE,
		Skills.POINT_DEFENSE,
		"espc_dancing_steps",
		"espc_running_hot",
	};

	private String[] grabBag2 = {
		Skills.COMBAT_ENDURANCE,
		Skills.DAMAGE_CONTROL,
		Skills.IMPACT_MITIGATION,
		Skills.SYSTEMS_EXPERTISE,
		"espc_underdog",
	};
	
	private void adjustSkills(PersonAPI person, FleetMemberAPI fleetMember) {
		String hullId = fleetMember.getHullId();
		LinkedList<String> skillPool = new LinkedList<String>();
		skillPool.add(Skills.HELMSMANSHIP);
		skillPool.add(Skills.TARGET_ANALYSIS);
		
		if (fleetMember.getHullSpec().getManufacturer().equals("Low Tech") ||
			hullId.equals("hammerhead") || hullId.equals("lasher") ||
			fleetMember.getHullSpec().getTags().contains("espc_ballistic") ||
			hullId.equals("espc_warden") && 
				fleetMember.getVariant().getSlot("WS 001").getWeaponType().equals(WeaponType.BALLISTIC)
		)
			skillPool.add(Skills.BALLISTIC_MASTERY);
		
		if (fleetMember.getHullSpec().getHullSize().equals(HullSize.FRIGATE)) {
			for (String skill : skillsFrigate)
				skillPool.add(skill);
			if (fleetMember.getHullSpec().getManufacturer().equals("Pact-Derelict")) {
				skillPool.remove(Skills.COMBAT_ENDURANCE);
				skillPool.remove(Skills.FIELD_MODULATION);
				if (hullId.equals("espc_warden"))
					skillPool.add(Skills.GUNNERY_IMPLANTS);
				else if (hullId.equals("espc_sentry")) {
					skillPool.add(Skills.POINT_DEFENSE);
					skillPool.add(Skills.SYSTEMS_EXPERTISE);
					skillPool.add("espc_second_wind");
				}
			} else if (hullId.equals("espc_flagbearer") || hullId.equals("espc_songbird")) {
				skillPool.add(Skills.ORDNANCE_EXPERTISE);
				skillPool.add(Skills.SYSTEMS_EXPERTISE);
				skillPool.add("espc_running_hot");
			} else if (hullId.equals("espc_opossum") || hullId.equals("espc_jackalope") || hullId.equals("omen")) {
				skillPool.add(Skills.SYSTEMS_EXPERTISE);
				skillPool.add("espc_second_wind");
				if (!hullId.equals("omen"))
					skillPool.add("espc_running_hot");
			}
			if (hullId.equals("lasher") || hullId.equals("espc_rondel") || hullId.equals("espc_picket"))
				skillPool.add("espc_running_hot");
			if (fleetMember.getHullSpec().getTags().contains("espc_ballistic"))
				skillPool.remove(Skills.FIELD_MODULATION);
		}
		else if (fleetMember.getHullSpec().getHullSize().equals(HullSize.DESTROYER)) {
			for (String skill : skillsDestroyer)
				skillPool.add(skill);
			
			if (fleetMember.getHullSpec().getManufacturer().equals("Pact-Derelict") ||
				fleetMember.getHullSpec().getManufacturer().equals("Remnant")) {
				skillPool.add(Skills.GUNNERY_IMPLANTS);
				skillPool.remove(Skills.COMBAT_ENDURANCE);
				if (hullId.equals("espc_berserker"))
					skillPool.remove(Skills.POINT_DEFENSE);
			}
		}
		else if (fleetMember.getHullSpec().getHullSize().equals(HullSize.CRUISER) ||
				fleetMember.getHullSpec().getHullSize().equals(HullSize.CAPITAL_SHIP)) {
			for (String skill : skillsCruiser)
				skillPool.add(skill);
			
			if (hullId.equals("espc_observer") || hullId.equals("espc_amanuensis") || hullId.equals("apex")) {
				skillPool.remove("espc_second_wind");
				skillPool.remove(Skills.SYSTEMS_EXPERTISE);
			} else if (hullId.equals("espc_rampart")) {
				skillPool.add(Skills.COMBAT_ENDURANCE);
				skillPool.add(Skills.DAMAGE_CONTROL);
				skillPool.add(Skills.IMPACT_MITIGATION);
			} else if (Misc.isAutomated(fleetMember))
				skillPool.add(Skills.GUNNERY_IMPLANTS);
			
			if (hullId.equals("espc_observer") || hullId.equals("brilliant") || hullId.equals("espc_chorale")) {
				skillPool.add(Skills.ENERGY_WEAPON_MASTERY);
				if (hullId.equals("brilliant"))
					skillPool.remove(Skills.POINT_DEFENSE);
			}
		}
		
		List<SkillLevelAPI> skills = person.getStats().getSkillsCopy();
		int skillPoints = 0;
		int elitePoints = 0;
		for (SkillLevelAPI skill : skills) {
			if (skill.getLevel() > 0 &&
				skill.getSkill().isCombatOfficerSkill()) {
				skillPoints++;
				if (skill.getLevel() > 1)
					elitePoints++;
				person.getStats().setSkillLevel(skill.getSkill().getId(), 0);
			}
		}
		boolean usedFirst = false;
		for (int i = 0; i < skillPoints; i++) {
			if (skillPool.size() == 0) {
				if (usedFirst) {
					person.getStats().setLevel(i);
					return;
				}
				for (String skill : usedFirst ? grabBag2 : grabBag)
					if (person.getStats().getSkillLevel(skill) <= 0f)
						skillPool.add(skill);
				usedFirst = true;
			}
			
			int rand = Misc.random.nextInt(skillPool.size());
			// more priority!  i really gotta pull out every stop to make the ship ai not BEANS here
			if (skillPool.contains(Skills.HELMSMANSHIP) && (hullId.equals("espc_chorale") || hullId.equals("espc_amanuensis")))
				rand = skillPool.indexOf(Skills.HELMSMANSHIP);
			person.getStats().setSkillLevel(skillPool.get(rand), elitePoints > 0 ? 2 : 1);
			if (skillPool.get(rand).equals(Skills.HELMSMANSHIP) && 
				!skillPool.contains("espc_dancing_steps") &&
				person.getStats().getSkillLevel("espc_dancing_steps") <= 0f) {
				skillPool.add("espc_dancing_steps");
			}
			elitePoints--;
			skillPool.remove(rand);
		}
	}

}

/*
private String[] blacklistedSkills = {
	Skills.MISSILE_SPECIALIZATION,
	Skills.COMBAT_ENDURANCE,
	Skills.IMPACT_MITIGATION,
	Skills.POLARIZED_ARMOR,
	Skills.DAMAGE_CONTROL,
	Skills.POINT_DEFENSE,
};

private String[] whitelistedSkills = {
	Skills.HELMSMANSHIP,
	Skills.FIELD_MODULATION,
	Skills.ORDNANCE_EXPERTISE,
	Skills.TARGET_ANALYSIS
};
private String[] whitelistedSkillsLow = {
	Skills.COMBAT_ENDURANCE,
	Skills.GUNNERY_IMPLANTS,
	Skills.SYSTEMS_EXPERTISE,
	Skills.POINT_DEFENSE,
};

int rand = Misc.random.nextInt(blacklistedSkills.length);
for (int i = 0; i < blacklistedSkills.length; i++) {
	if (person.getStats().getSkillLevel(blacklistedSkills[(rand + i) % blacklistedSkills.length]) > 0) {
		boolean newAssigned = false;
		int rand2 = Misc.random.nextInt(whitelistedSkills.length);
		
		if (person.getStats().getSkillLevel(blacklistedSkills[(rand + i) % blacklistedSkills.length]) > 1)
			sparePoints++;
		
		for (int g = 0; g < whitelistedSkills.length; g++) {
			if (whitelistedSkills[(rand2 + g) % whitelistedSkills.length].equals(Skills.FIELD_MODULATION) &&
				fleetMember.getHullSpec().getShieldType() == ShieldType.NONE)
				continue;
			if (person.getStats().getSkillLevel(whitelistedSkills[(rand2 + g) % whitelistedSkills.length]) <= 0) {
				person.getStats().setSkillLevel(
					whitelistedSkills[(rand2 + g) % whitelistedSkills.length],
					sparePoints > 0 ? 2 : 1
				);
				if (sparePoints > 0)
					sparePoints--;
				newAssigned = true;
				person.getStats().setSkillLevel(blacklistedSkills[(rand + i) % blacklistedSkills.length], 0);
				break;
			}
		}
		if (!newAssigned) {
			rand2 = Misc.random.nextInt(whitelistedSkills.length);
			for (int g = 0; g < whitelistedSkillsLow.length; g++) {
				if (person.getStats().getSkillLevel(whitelistedSkillsLow[(rand2 + g) % whitelistedSkillsLow.length]) <= 0) {
					person.getStats().setSkillLevel(
						whitelistedSkillsLow[(rand2 + g) % whitelistedSkillsLow.length],
						sparePoints > 0 ? 2 : 1
					);
					if (sparePoints > 0)
						sparePoints--;
					person.getStats().setSkillLevel(blacklistedSkills[(rand + i) % blacklistedSkills.length], 0);
					break;
				}
			}
		}
	}*/
