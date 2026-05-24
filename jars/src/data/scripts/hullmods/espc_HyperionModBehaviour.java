package data.scripts.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;

import data.scripts.util.MezzUtils;


public class espc_HyperionModBehaviour extends BaseHullMod {
	
	private static final float PPT_MULT = 0f;
	private static final float CR_LOSS_PERCENT = 100f;
    
    @Override
    public boolean affectsOPCosts() {
        return true;
    }
	
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
    	
    }
    
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        
		int mountCalc = 0;
		for (WeaponAPI wep : ship.getAllWeapons())
			if (wep.getSlot().getSlotSize().equals(WeaponSize.MEDIUM)) {
				if (mountCalc == 0)
					mountCalc = 1;
				else if (mountCalc == -1) {
					mountCalc = -2;
					break;
				}
			} else if (wep.getSlot().getSlotSize().equals(WeaponSize.LARGE))
				if (mountCalc == 0)
					mountCalc = -1;
				else if (mountCalc == 1) {
					mountCalc = -2;
					break;
				}
		
		if (mountCalc == -2) {
			ship.getMutableStats().getPeakCRDuration().modifyMult(id, PPT_MULT);   
			ship.getMutableStats().getCRLossPerSecondPercent().modifyPercent(id, CR_LOSS_PERCENT);   
		}   
    }
    
    public String getDescriptionParam(int index, HullSize hullSize) {
        if (index == 0)
            return MezzUtils.getString("espc_general", "mutually_exclusive");
        else if (index == 1)
            return MezzUtils.getString("espc_general", "both");
        else if (index == 2)
            return MezzUtils.getString("espc_general", "severely_hampered");

        return null;
    }
	
}