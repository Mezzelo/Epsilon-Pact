package data.scripts.campaign.listeners;

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
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.util.Misc;

import data.scripts.espc_ModPlugin;
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
		if (market.getFactionId().equals("epsilpac") || (espc_ModPlugin.hasNex() && CharacterBackgroundUtils.isBackgroundActive("espc_realHumanBeing"))) {

			if (espc_ModPlugin.hasNex() && CharacterBackgroundUtils.isBackgroundActive("espc_realHumanBeing")) {
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
			if (market.getFactionId().equals("epsilpac")) {
				if (market.hasSubmarket(Submarkets.SUBMARKET_OPEN)) {
		        	market.removeSubmarket(Submarkets.SUBMARKET_OPEN);
					market.addSubmarket("espc_open_market");
				}
				if (market.hasSubmarket(Submarkets.SUBMARKET_BLACK)) {
		        	market.removeSubmarket(Submarkets.SUBMARKET_BLACK);
					market.addSubmarket("espc_black_market");
					market.getSubmarket("espc_black_market").setFaction(Global.getSector().getFaction(Factions.PIRATES));
				}
			}
			
	        List<OfficerManagerEvent> managers = Global.getSector().getListenerManager().getListeners(OfficerManagerEvent.class);
	        for (OfficerManagerEvent manager : managers) {
	        	manager.reportPlayerOpenedMarket(market);
	        	for (PersonAPI person : market.getPeopleCopy()) {
		        	if (manager.getOfficer(person.getId()) != null) {
		        		if (espc_ModPlugin.hasNex() && CharacterBackgroundUtils.isBackgroundActive("espc_realHumanBeing") &&
		        			!market.getFactionId().equals("epsilpac")) {
		        			manager.removeAvailable(manager.getOfficer(person.getId()));
		        		} else if (!person.getMemoryWithoutUpdate().getBoolean(Misc.IS_MERCENARY)) {
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
		}
	}

	@Override
	public void reportPlayerClosedMarket(MarketAPI market) {
	}

	@Override
	public void reportPlayerOpenedMarketAndCargoUpdated(MarketAPI market) {
	}

	@Override
	public void reportPlayerMarketTransaction(PlayerMarketTransaction transaction) {
		if (espc_ModPlugin.hasNex() && CharacterBackgroundUtils.isBackgroundActive("espc_realHumanBeing") &&
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
