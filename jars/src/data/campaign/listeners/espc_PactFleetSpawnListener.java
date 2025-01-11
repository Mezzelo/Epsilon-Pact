package data.campaign.listeners;
// doing this through here rather than BaseGenerateFleetOfficersPlugin, as the generic officer generation is fine for my purposes
// there's just a few exceptions i'd like to impose.

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
import com.fs.starfarer.api.combat.ShieldAPI.ShieldType;
import com.fs.starfarer.api.combat.ShipVariantAPI;
// 	import com.fs.starfarer.api.combat.ShipHullSpecAPI.ShipTypeHints;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.impl.campaign.intel.PersonBountyIntel;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.util.Misc;

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
	
	private String[] variantList = {
		"espc_observer_Strike",
		"espc_observer_Suppressive",
		"fulgent_espc_Assault",
		"apex_espc_Assault",
		"radiant_espc_Assault",
		"radiant_espc_Attack",
		// "radiant_espc_Hunter", as funny as this one is, more interesting to let the game autofit this one.
		"radiant_espc_Strike",
		"espc_observer_Assault",
		"sunder_espc_Strike",
		"sunder_espc_Support",
		"hammerhead_espc_Support",
		"espc_ember_Standard",
		"espc_pilgrim_Assault",
		"espc_pilgrim_Strike",
		"espc_pilgrim_Support",
		"espc_flagbearer_Standard",
		"espc_flagbearer_Support",
		"espc_jackalope_Strike",
		"espc_songbird_Standard",
		"espc_militia_Support",
		"espc_berserker_Assault",
		"espc_berserker_Support",
		"espc_rampart_Strike",
		"espc_rampart_Support",
		"espc_rampart_Assault",
	};
	
	@Override
	public void reportFleetSpawned(CampaignFleetAPI fleet) {
		if (!(
			fleet.getFaction().getId().equals("epsilpac") || 
			(fleet.getFaction().getId().equals(Factions.NEUTRAL) && fleet.getNameWithFactionKeepCase().contains("'s Fleet"))
			))
			return;
		
		// boolean hasCores = false;
		
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
		
		// getofficerscopy doesn't appear to work urghh
        for (FleetMemberAPI fleetMember : fleet.getFleetData().getMembersListCopy()) {
			// 	
        	if (fleet.getFaction().getId().equals("epsilpac")) {
            	for (int i = 0; i < variantList.length; i++) {
            		if (variantList[i].equals(fleetMember.getVariant().getHullVariantId()) ) {
            			ShipVariantAPI refit = Global.getSettings().getVariant(variantList[i]).clone();
        	            refit.addTag("no_autofit");
            			refit.setSource(VariantSource.REFIT);
                        fleetMember.setVariant(refit, false, true);
            			break;
            		}
            	}
        	}
			// OfficerDataAPI officer = (OfficerDataAPI) officerIterator.next();
			PersonAPI person = fleetMember.getCaptain();
			
			if (person != null) {
				
				// can't just check for size > 4, in case of added skill categories, i'm guessing.
				boolean hasNonZeroSkill = false;
				for (SkillLevelAPI skill : person.getStats().getSkillsCopy()) {
					hasNonZeroSkill = skill.getLevel() > 0f;
					if (hasNonZeroSkill)
						break;
				}
				if (hasNonZeroSkill) {
					if (!isBounty) {
						if (!fleetMember.getHullId().equals("espc_rampart"))
							adjustSkills(person, fleetMember);
					}
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
					
					// if (Misc.random.nextFloat() > 0.5f)
						person.setPortraitSprite("graphics/portraits/portrait_ai" + coreType + "b.png");
					// else
					// 	person.setPortraitSprite("graphics/portraits/portrait_ai" + coreType + ".png");
					
					FullName name = person.getName();
					if (Misc.random.nextFloat() > 0.35f || isBounty) {
						if (Misc.random.nextFloat() > 0.7f)
							name.setFirst(name.getLast());
						name.setLast("");
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
	
	private void adjustSkills(PersonAPI person, FleetMemberAPI fleetMember) {
		int sparePoints = 0;
		int rand = Misc.random.nextInt(blacklistedSkills.length - 1);
		for (int i = 0; i < blacklistedSkills.length; i++) {
			if (person.getStats().getSkillLevel(blacklistedSkills[(rand + i) % blacklistedSkills.length]) > 0) {
				boolean newAssigned = false;
				int rand2 = Misc.random.nextInt(whitelistedSkills.length - 1);
				
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
					rand2 = Misc.random.nextInt(whitelistedSkills.length - 1);
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
			}
		}
	}

}
