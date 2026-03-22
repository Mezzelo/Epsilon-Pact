package data.scripts.sic.espc;


import java.awt.Color;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import second_in_command.SCData;
import second_in_command.specs.SCBaseSkillPlugin;

public class espc_AllWalks extends SCBaseSkillPlugin {

	private static float DAMAGE_BONUS_PER = 1.5f;
	private static float DAMAGE_BONUS_MAX = 15f;
	private static float SPEED_BONUS_PER = 1f;
	private static float SPEED_BONUS_MAX = 10f;
	private static float DP_MAX = 240f;
	private static float DP_MIN = 280f;
	
    @Override
    public String getAffectsString() {
        return "all ships in the fleet";
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
		tooltip.addPara("+%s damage dealt for each unique design type deployed in combat, up to %s", 
			0f, Misc.getHighlightColor(), Misc.getHighlightColor(), 
			DAMAGE_BONUS_PER + "%", (int) DAMAGE_BONUS_MAX + "%");
		tooltip.addPara("+%s top speed for each unique design type deployed in combat, up to %s", 
			0f, Misc.getHighlightColor(), Misc.getHighlightColor(), 
			(int) SPEED_BONUS_PER + "%", (int) SPEED_BONUS_MAX + "%");
		tooltip.addPara(BaseIntelPlugin.BULLET + 
			"%s hulls are split into three design types, and can count for up to three total", 
			0f, Misc.getTextColor(), new Color(20, 170, 190, 255), 
			"Epsilon Pact");
		tooltip.addPara(BaseIntelPlugin.BULLET + 
			"Max effectiveness under %s fleet combat DP, diminishing fully at %s DP", 
			0f, Misc.getTextColor(), Misc.getHighlightColor(), 
			(int) DP_MAX + "", (int) DP_MIN + "");
        
		
	}
}