package data.campaign.listeners;

import java.util.List;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PlayerMarketTransaction;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.ColonyInteractionListener;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI.SkillLevelAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.events.BaseEventPlugin;
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent;
import com.fs.starfarer.api.util.Misc;

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
		if (market.getFactionId().equals("epsilpac")) {
	        List<OfficerManagerEvent> managers = Global.getSector().getListenerManager().getListeners(OfficerManagerEvent.class);
	        for (OfficerManagerEvent manager : managers) {
	        	manager.reportPlayerOpenedMarket(market);
	        	for (PersonAPI person : market.getPeopleCopy()) {
		        	if (manager.getOfficer(person.getId()) != null) {
		        		// PersonAPI officer = manager.getOfficer(person.getId()).person;
						int rand2 = Misc.random.nextInt(skillsList.length - 1);
		        		List<SkillLevelAPI> skills = person.getStats().getSkillsCopy();
		        		for (SkillLevelAPI skill : skills) {
		        			if (!skill.getSkill().getId().contains("espc") && skill.getLevel() > 0) {
		        				person.getStats().setSkillLevel(skill.getSkill().getId(), 0);
		        				person.getStats().setSkillLevel(skillsList[rand2], 1);
		        				do {
		        					rand2 = Misc.random.nextInt(skillsList.length - 1);
		        				} while (person.getStats().getSkillLevel(skillsList[rand2]) > 0);
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
	}
	

}
