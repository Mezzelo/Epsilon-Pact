package data.scripts.campaign.rulecmd;

import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Items;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Misc.Token;

import data.scripts.espc_ModPlugin;
import exerelin.campaign.SectorManager;

/**
 * NotifyEvent $eventHandle <params> 
 * 
 */
public class espc_RulesUtil extends BaseCommandPlugin {
	
    @Override
	public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Token> params, Map<String, MemoryAPI> memoryMap) {
		
		String command = params.get(0).getString(memoryMap);
		if (dialog == null || command == null) return false;
		if (command.equals("relationGT"))
            return dialog.getInteractionTarget().getActivePerson().getRelToPlayer().getRepInt() >
            	params.get(1).getInt(memoryMap);
        else if (command.equals("relationLT"))
            return dialog.getInteractionTarget().getActivePerson().getRelToPlayer().getRepInt() <
        	params.get(1).getInt(memoryMap);
        else if (command.equals("relationGTOther"))
                return Global.getSector().getImportantPeople().getPerson(params.get(1).getString(memoryMap)).getRelToPlayer().getRepInt() >
                	params.get(2).getInt(memoryMap);
            else if (command.equals("relationLTOther"))
                return Global.getSector().getImportantPeople().getPerson(params.get(1).getString(memoryMap)).getRelToPlayer().getRepInt() <
            	params.get(2).getInt(memoryMap);
        else if (command.equals("isRandomSector"))
            return (espc_ModPlugin.hasNex() && !SectorManager.getManager().isCorvusMode());
        else if (command.equals("hasAICore")) {
        	return Global.getSector().getPlayerFleet().getCargo().getCommodityQuantity(Commodities.GAMMA_CORE) > 0 ||
        		Global.getSector().getPlayerFleet().getCargo().getCommodityQuantity(Commodities.BETA_CORE) > 0 ||
        		Global.getSector().getPlayerFleet().getCargo().getCommodityQuantity(Commodities.ALPHA_CORE) > 0;
        }
        else if (command.equals("hasSalvageContrabrand")) {
        	for (CargoStackAPI stack : Global.getSector().getPlayerFleet().getCargo().getStacksCopy()) {
				if (stack.isSpecialStack()) {
					if (stack.getSpecialItemSpecIfSpecial().getId().equals(Items.SHIP_BP)) {
						String bpOf = stack.getSpecialDataIfSpecial().getData();
						if (Global.getSettings().getHullSpec(bpOf) != null &&
							!bpOf.contains("espc") &&
							!Global.getSettings().getHullSpec(bpOf).isCivilianNonCarrier() &&
							// for reference, atlas mk2 is 150k, prom mk2 & retribution are 200k, doom is 250k
							Global.getSettings().getHullSpec(bpOf).getBaseValue() >= 245000f
							) {
							return true;
						}
						
					} else if (stack.getSpecialItemSpecIfSpecial().getId().equals(Items.PRISTINE_NANOFORGE) ||
						stack.getSpecialItemSpecIfSpecial().getId().equals(Items.DRONE_REPLICATOR) ||
						stack.getSpecialItemSpecIfSpecial().getId().equals(Items.CRYOARITHMETIC_ENGINE)) {
						return true;
					}
				}
			}
        }
        else if (command.equals("hasAutomatedShips")) {
        	for (FleetMemberAPI member : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy())
        		if (Misc.isAutomated(member))
        			return true;
        }
		
		return false;
	}
	
	
}















