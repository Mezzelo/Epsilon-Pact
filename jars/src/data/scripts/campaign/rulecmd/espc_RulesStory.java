package data.scripts.campaign.rulecmd;

import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.characters.ImportantPeopleAPI.PersonDataAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc.Token;

// import data.scripts.espc_ModPlugin;
// import exerelin.campaign.SectorManager;

/**
 * NotifyEvent $eventHandle <params> 
 * 
 */
public class espc_RulesStory extends BaseCommandPlugin {
	
    @Override
	public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Token> params, Map<String, MemoryAPI> memoryMap) {
		
		String command = params.get(0).getString(memoryMap);
		if (dialog == null || command == null) return false;
		if (command.equals("spawnAnyiwo")) {
			MarketAPI market = Global.getSector().getEconomy().getMarket("espc_tocquiera_market");
			if (market != null) {
				PersonDataAPI data = Global.getSector().getImportantPeople().getData("espc_anyiwo");
				PersonAPI person = data.getPerson();
				if (person.getMarket() != null)
					market.getCommDirectory().getEntryForPerson(person).setHidden(false);
			}
		} else if (command.equals("revealCoreContacts")) {
			MarketAPI market = Global.getSector().getEconomy().getMarket("espc_tocquiera_market");
			if (market != null) {
				PersonDataAPI data = Global.getSector().getImportantPeople().getData("espc_gauss");
				PersonAPI person = data.getPerson();
				if (person.getMarket() != null)
					market.getCommDirectory().getEntryForPerson(person).setHidden(false);
			}
			PersonDataAPI data = Global.getSector().getImportantPeople().getData("espc_isabelle");
			PersonAPI person = data.getPerson();
			market = person.getMarket();
			if (market != null) {
				market.getCommDirectory().getEntryForPerson(person).setHidden(false);
			}
		}
		return true;
	}
	
	
}















