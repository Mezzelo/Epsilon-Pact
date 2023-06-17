package data.scripts.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

public class espc_ChoraleLGModBehaviour extends BaseHullMod {
	
    private static final float SPEED_PENALTY = 15f;
    private static final float DISSIPATION_PENALTY = 100f;
	
	@Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getMaxSpeed().modifyFlat(id, -SPEED_PENALTY);
		stats.getFluxDissipation().modifyFlat(id, -DISSIPATION_PENALTY);
    }
    
    /*
    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
    }*/
    
    @Override
    public String getDescriptionParam(int index, HullSize hullSize) {
        if (index == 0)
            return ("" + ((int) SPEED_PENALTY));
        if (index == 1)
            return ("" + ((int) DISSIPATION_PENALTY));

        return null;
    }
	
}