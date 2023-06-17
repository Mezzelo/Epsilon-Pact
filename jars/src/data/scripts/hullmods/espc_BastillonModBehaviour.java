package data.scripts.hullmods;

import java.util.Iterator;
import java.util.List;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.AIHints;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.combat.listeners.WeaponBaseRangeModifier;
import com.fs.starfarer.api.impl.campaign.ids.Stats;

public class espc_BastillonModBehaviour extends BaseHullMod {
	
    // private static final float RANGE_BONUS_MAX = 700f;
	// private static final float RANGE_BONUS = 200f;
	private static final float DAMAGE_BONUS = 50f;
	
	@Override
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getDynamic().getMod(Stats.PD_IGNORES_FLARES).modifyFlat(id, 1f);
		stats.getDynamic().getMod(Stats.PD_BEST_TARGET_LEADING).modifyFlat(id, 1f);
		stats.getDamageToMissiles().modifyPercent(id, DAMAGE_BONUS);
		
	}

	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		List<WeaponAPI> weapons = ship.getAllWeapons();
		Iterator<WeaponAPI> iter = weapons.iterator();
		while (iter.hasNext()) {
			WeaponAPI weapon = (WeaponAPI)iter.next();
			boolean sizeMatches = weapon.getSize() == WeaponSize.SMALL;
			
			if (sizeMatches && weapon.getType() != WeaponType.MISSILE && !weapon.hasAIHint(AIHints.STRIKE)) {
				weapon.setPD(true);
			}
		}
        // ship.addListener(new espc_BastillonRangeMod());  
	}
    
    @Override
    public String getDescriptionParam(int index, HullSize hullSize) {
        if (index == 0)
            return ("50%");
        else if (index == 1)
            return ("upgraded by default");

        return null;
    }
	
    /*
	public static class espc_BastillonRangeMod implements WeaponBaseRangeModifier {
		
        public float getWeaponBaseRangeFlatMod(ShipAPI ship, WeaponAPI weapon) {
			if (weapon.getSize() != WeaponSize.SMALL ||
				weapon.getType() != WeaponType.ENERGY ||
				weapon.getSpec().getMaxRange() <= 0f)
				return 0f;
            return Math.max(
				Math.min(RANGE_BONUS, RANGE_BONUS_MAX - weapon.getSpec().getMaxRange()),
				0f);
        }
		
        public float getWeaponBaseRangePercentMod(ShipAPI shipAPI, WeaponAPI weaponAPI) {
            return 0;
        }

        public float getWeaponBaseRangeMultMod(ShipAPI shipAPI, WeaponAPI weaponAPI) {
            return 1;
        }
    }*/
	
}