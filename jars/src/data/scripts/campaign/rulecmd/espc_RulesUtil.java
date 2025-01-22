package data.scripts.campaign.rulecmd;

import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
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
		
		return false;
	}
	
	
}















