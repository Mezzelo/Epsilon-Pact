package data.scripts.campaign.listeners;

import java.util.HashMap;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.PlayerMarketTransaction;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.ColonyInteractionListener;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI.SkillLevelAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.MutableStat.StatMod;
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.util.Misc;

import data.scripts.espc_ModPlugin;
import data.scripts.campaign.skills.espc_RealHumbleBeingBg_Skill;
import exerelin.campaign.backgrounds.CharacterBackgroundUtils;

public class espc_ColonyInteractionListener implements ColonyInteractionListener {

	public espc_ColonyInteractionListener() {
		super();
	}
	
	public static String[] skillsList = {
		"espc_running_hot",
		"espc_second_wind",
		"espc_dancing_steps",
		"espc_underdog",
		"espc_unburdened",
	};

	@Override
	public void reportPlayerOpenedMarket(MarketAPI market) {
		boolean isMeCore = false;
		if (espc_ModPlugin.hasNex() && Global.getSector().getPlayerPerson() != null &&
			Global.getSector().getPlayerPerson().getStats() != null)
			isMeCore = Global.getSector().getPlayerPerson().getStats().getSkillLevel("espc_realHumbleBeingBg_skill") > 0f;
		
		if (market.getFactionId().equals("epsilpac") || isMeCore) {
			int pactOfficerCount = 0;
			
			if (market.getFactionId().equals("epsilpac")) {
				
				if (market.hasSubmarket(Submarkets.SUBMARKET_OPEN)) {
		        	market.removeSubmarket(Submarkets.SUBMARKET_OPEN);
					market.addSubmarket("espc_open_market");
				}/*
				if (market.hasSubmarket(Submarkets.SUBMARKET_BLACK)) {
		        	market.removeSubmarket(Submarkets.SUBMARKET_BLACK);
					market.addSubmarket("espc_black_market");
					market.getSubmarket("espc_black_market").setFaction(Global.getSector().getFaction(Factions.PIRATES));
				}*/
				
				/* can't get this to proc soon enough without a more regular listener, which i don't really care to do.
				it's your story point to waste.
				if (espc_ModPlugin.hasNex() && CharacterBackgroundUtils.isBackgroundActive("espc_realHumanBeing")) {
					PortsideBarData data = PortsideBarData.getInstance();
					for (PortsideBarEvent event : data.getEvents()) {
						if (event instanceof LuddicPathBaseBarEvent) {
							LuddicPathBaseBarEvent patherEvent = (LuddicPathBaseBarEvent) event;
							if (patherEvent.getMarket() != null &&
								patherEvent.getMarket().getId().equals(market.getId())) {
								List<SkillLevelAPI> skills = patherEvent.getPerson().getStats().getSkillsCopy();
					        	for (SkillLevelAPI skill : skills) {
					        		if (!skill.getSkill().getId().contains("espc") && skill.getLevel() > 0 &&
					        			skill.getSkill().isCombatOfficerSkill()) {
										patherEvent.getPerson().getStats().setSkillLevel(
											skillsList[Misc.random.nextInt(skillsList.length)], skill.getLevel());
										patherEvent.getPerson().getStats().setSkillLevel(skill.getSkill().getId(), 0);
							       		break;
					        		}
					        	}
					        	pactOfficerCount++;
					        	break;
							}
						}
					}
				} */
			}
			
	        List<OfficerManagerEvent> managers = Global.getSector().getListenerManager().getListeners(OfficerManagerEvent.class);
	        for (OfficerManagerEvent manager : managers) {
	        	manager.reportPlayerOpenedMarket(market);
	        	for (PersonAPI person : market.getPeopleCopy()) {
		        	if (manager.getOfficer(person.getId()) != null) {
		        		if (person.getMemoryWithoutUpdate().getBoolean(Misc.IS_MERCENARY) ||
		        			isMeCore &&
		        			!market.getFactionId().equals("epsilpac")) {
		        			manager.removeAvailable(manager.getOfficer(person.getId()));
		        		} else {
		        			pactOfficerCount++;
			        		// PersonAPI officer = manager.getOfficer(person.getId()).person;
			        		String[] skillsListCopy = skillsList.clone();
			        		int max = skillsListCopy.length;
			        		List<SkillLevelAPI> skills = person.getStats().getSkillsCopy();
			        		for (SkillLevelAPI skill : skills) {
			        			if (!skill.getSkill().getId().contains("espc") && skill.getLevel() > 0 &&
			        				skill.getSkill().isCombatOfficerSkill()) {
									int rand = Misc.random.nextInt(max);
			        				person.getStats().setSkillLevel(skillsListCopy[rand], skill.getLevel());
			        				person.getStats().setSkillLevel(skill.getSkill().getId(), 0);
			        				max--;
			        				if (max < 1)
			        					break;
			        				for (int i = rand; i < max; i++)
			        					skillsListCopy[i] = skillsListCopy[i + 1];
			        				rand = Misc.random.nextInt(max);
			        			}
			        		}
		        		}
		        	}
	        	}
	        }
	        if (isMeCore) {
	        	if (pactOfficerCount > 0) {
	        		HashMap<String, StatMod> penalty = Global.getSector().getPlayerPerson().getStats().getOfficerNumber().getFlatMods();
	        		for (String key : penalty.keySet()) {
	        			if (key.contains("espc_realHumbleBeingBg") && penalty.get(key).getValue() < 0f) {
	    	        		Global.getSector().getPlayerPerson().getStats().getOfficerNumber().modifyFlat("espc_aibgmaxmod", 
	    	        			Math.min(pactOfficerCount, -penalty.get(key).getValue()));
	        				Global.getSector().getPlayerPerson().getStats().getOfficerNumber().modifyFlat(
    	        				key,
    	        				Math.min(espc_RealHumbleBeingBg_Skill.OFFICER_CAP_PENALTY + 
    	        				espc_RealHumbleBeingBg_Skill.RecomputeOfficerCap(Global.getSector().getPlayerPerson().getStats()), 0)
    	        			);
    	    	        	break;
	        			}
	        		}
	        	}

				boolean found = false;
				for (CargoStackAPI stack : Global.getSector().getPlayerFleet().getCargo().getStacksCopy()) {
					CommoditySpecAPI spec = stack.getResourceIfResource();
					if (spec != null && spec.getId().equals("espc_meCore")) {
						found = true;
						stack.setSize(1f);
						break;
					}
				}
				if (!found) {
			        Global.getSector().getPlayerFleet().getCargo().addItems(
			        	CargoAPI.CargoItemType.RESOURCES, "espc_meCore", 1
				    );
				}
			}
		} else if (!market.getFactionId().equals("epsilpac")) {
			if (market.hasSubmarket("espc_open_market")) {
				market.removeSubmarket("espc_open_market");
	        	market.addSubmarket(Submarkets.SUBMARKET_OPEN);
			}/*
			if (market.hasSubmarket("espc_black_market")) {
				market.removeSubmarket("espc_black_market");
	        	market.addSubmarket(Submarkets.SUBMARKET_BLACK);
				market.getSubmarket(Submarkets.SUBMARKET_BLACK).setFaction(Global.getSector().getFaction(Factions.PIRATES));
			}*/
		}
	}

	@Override
	public void reportPlayerClosedMarket(MarketAPI market) {
        if (espc_ModPlugin.hasNex() && 
        	CharacterBackgroundUtils.isBackgroundActive("espc_realHumbleBeing")) {
        	Global.getSector().getPlayerPerson().getStats().getOfficerNumber().unmodify("espc_aibgmaxmod");
        	// could call refreshCharacterStatsEffects but i'm just not confident enough to do that lol
    		HashMap<String, StatMod> penalty = Global.getSector().getPlayerPerson().getStats().getOfficerNumber().getFlatMods();
    		for (String key : penalty.keySet()) {
    			if (key.contains("espc_realHumbleBeing")) {
    				Global.getSector().getPlayerPerson().getStats().getOfficerNumber().modifyFlat(
    					key,
    					Math.min(espc_RealHumbleBeingBg_Skill.OFFICER_CAP_PENALTY + 
    						espc_RealHumbleBeingBg_Skill.RecomputeOfficerCap(Global.getSector().getPlayerPerson().getStats()), 0)
    				);
	        		break;
    			}
    		}
        }
	}

	@Override
	public void reportPlayerOpenedMarketAndCargoUpdated(MarketAPI market) {
	}

	@Override
	public void reportPlayerMarketTransaction(PlayerMarketTransaction transaction) {
		if (espc_ModPlugin.hasNex() && (CharacterBackgroundUtils.isBackgroundActive("espc_realHumanBeing") ||
    		CharacterBackgroundUtils.isBackgroundActive("espc_realHumbleBeing")) &&
			transaction.getCreditValue() != 0f) {
			for (CargoStackAPI stack : transaction.getSold().getStacksCopy()) {
				CommoditySpecAPI spec = stack.getResourceIfResource();
				if (spec != null && spec.getId().equals("espc_meCore")) {
			        Global.getSector().getPlayerFleet().getCargo().getCredits().subtract(1000f * stack.getSize());
					break;
				}
			}
		}
	}
	

}
