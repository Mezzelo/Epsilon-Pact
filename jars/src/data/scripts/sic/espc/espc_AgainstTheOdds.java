package data.scripts.sic.espc;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import second_in_command.SCData;
import second_in_command.specs.SCBaseSkillPlugin;

public class espc_AgainstTheOdds extends SCBaseSkillPlugin {
    @Override
    public String getAffectsString() {
        return "all ships in the fleet";
    }

    @Override
    public void applyEffectsBeforeShipCreation(SCData data, MutableShipStatsAPI stats, ShipVariantAPI variant, ShipAPI.HullSize hullSize, String id) {
        stats.getMaxCombatReadiness().modifyFlat(id, 0.15f);
    }

	@Override
	public void addTooltip(SCData data, TooltipMakerAPI tooltip) {
		// TODO Auto-generated method stub
		
	}
}