package data.scripts.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.impl.campaign.ids.Stats;

import java.util.List;

public class espc_SerenadeModBehaviour extends BaseHullMod {
	
    private static final int OP_DISCOUNT_LARGE = 10;
    private static final int OP_DISCOUNT_MEDIUM = 5;
	
	private static final int ASYM_BONUS_LARGE = 100;
	private static final int ASYM_BONUS_MEDIUM = 50;
    
    @Override
    public boolean affectsOPCosts() {
        return true;
    }
	
	@Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getDynamic().getMod(Stats.LARGE_BALLISTIC_MOD).modifyFlat(id, -OP_DISCOUNT_LARGE);
		stats.getDynamic().getMod(Stats.MEDIUM_BALLISTIC_MOD).modifyFlat(id, -OP_DISCOUNT_MEDIUM);
    }
    
    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        
		int asymmetryBonus = 0;
		
		List<WeaponAPI> shipWeps = ship.getAllWeapons();
		boolean[] foundMirror = new boolean[shipWeps.size()];
        for (int i = 0; i < shipWeps.size(); i++){
			if (foundMirror[i])
				continue;
			for (int g = i + 1; g < shipWeps.size() && !foundMirror[i]; g++) {
				if (foundMirror[g])
					continue;
				if (((WeaponAPI) shipWeps.get(i)).getSize() != ((WeaponAPI) shipWeps.get(g)).getSize())
					continue;
				// float slotX, slotY;
				// check to make sure mount positions are mirrored, assuming symmetry over the X axis
				if (((WeaponAPI) shipWeps.get(i)).getSlot().getLocation().x - ((WeaponAPI) shipWeps.get(g)).getSlot().getLocation().x != 0f ||
					((WeaponAPI) shipWeps.get(i)).getSlot().getLocation().y + ((WeaponAPI) shipWeps.get(g)).getSlot().getLocation().y!= 0f)
					continue;
				// Global.getLogger(espc_serenademodbehaviour.class).info("slotx " + slotX + " spec " + ((WeaponAPI) shipWeps.get(i)).getSpec().getWeaponId() + ", sloty " + slotY + " spec " + ((WeaponAPI) shipWeps.get(g)).getSpec().getWeaponId());
				foundMirror[i] = (((WeaponAPI) shipWeps.get(i)).getSpec().getWeaponId().equals(((WeaponAPI) shipWeps.get(g)).getSpec().getWeaponId()));
				// found the mirrored slot, no need to check it over again.
				foundMirror[g] = true;
			}
			if (foundMirror[i]) {
				if (((WeaponAPI) shipWeps.get(i)).getSize() == WeaponSize.LARGE) 
					asymmetryBonus += ASYM_BONUS_LARGE;
				else
					asymmetryBonus += ASYM_BONUS_MEDIUM;
			}
        }
		ship.getMutableStats().getFluxDissipation().modifyFlat("id", asymmetryBonus);      
    }
    
    @Override
    public String getDescriptionParam(int index, HullSize hullSize) {
        if (index == 0)
            return ("10");
        else if (index == 1)
            return ("5");
        else if (index == 2)
            return ("100");
        else if (index == 3)
            return ("50");

        return null;
    }
	
}