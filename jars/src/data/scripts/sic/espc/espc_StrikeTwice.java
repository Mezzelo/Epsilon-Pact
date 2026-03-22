package data.scripts.sic.espc;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import second_in_command.SCData;
import second_in_command.specs.SCBaseSkillPlugin;

public class espc_StrikeTwice extends SCBaseSkillPlugin {
	
    @Override
    public String getAffectsString() {
        return "fleet";
    }
    
    
    @Override
    public void applyEffectsAfterShipCreation(SCData data, ShipAPI ship, ShipVariantAPI variant, String id) {
    	if (ship.getFleetMember() == null || ship.getFleetMember().getFleetData() == null)
    		return;
    }

    
    @Override
    public void applyEffectsBeforeShipCreation(SCData data, MutableShipStatsAPI stats, ShipVariantAPI variant, ShipAPI.HullSize hullSize, String id) {
    	if (stats.getFleetMember() == null || stats.getFleetMember().getFleetData() == null)
    		return;
    }
    
    @Override
    public void advance(SCData data, Float amount) {
    	if (data.getFleet() == null || data.getFleet().getFleetData() == null)
    		return;
    }
    
    @Override
    public void onActivation(SCData data) {
    }
    
    @Override
    public void onDeactivation(SCData data) {
    }

	@Override
	public void addTooltip(SCData data, TooltipMakerAPI tooltip) {
    	
    	if (data.getFleet() != null && data.getFleet().getFleetData() != null) {
    	} else {
    	}
		tooltip.addPara("%s maximum burn level if below %s fleet fuel capacity", 
			0f, Misc.getHighlightColor(), Misc.getHighlightColor(), 
			"+2", (int) 5 + "");

        tooltip.addSpacer(10f);
        tooltip.addPara("%s maximum burn level for each capital ship in fleet", 
                0f, Misc.getNegativeHighlightColor(), Misc.getNegativeHighlightColor(), 
                "-1", (int) 5 + "%");
        tooltip.addSpacer(20f);
        tooltip.addPara("Skill has no effect if a Logistical executive officer is in use", 
                0f, Misc.getNegativeHighlightColor(), Misc.getNegativeHighlightColor());
        
		
	}
}