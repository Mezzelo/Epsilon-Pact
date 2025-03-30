package data.scripts.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponGroupAPI;

// force staggers alternating remise mortars, as the game should but doesn't smile
// will technically cause undesired behaviour with linked mortars, but wyd
public class espc_RemMortarEffect implements OnFireEffectPlugin {
	
	// could theoretically run into issues with a fast enough fire rate, but that needs to be like 10x to take effect
	private static final float CD_SPACING = 0.25f;
	
	int count = 0;
	WeaponGroupAPI group;
    @Override
    public void onFire(DamagingProjectileAPI proj, WeaponAPI weapon, CombatEngineAPI engine) {
    	if (count == -1 || weapon.getShip() == null)
    		return;
    	else if (group != null && group.equals(weapon.getShip().getSelectedGroupAPI()))
    		return;
    	else if (count == 0) {
        	ShipAPI ship = weapon.getShip();
        	for (WeaponGroupAPI currGroup : ship.getWeaponGroupsCopy()) {
        		boolean foundSelf = false;
        		boolean allMortars = true;
        		count = 0;
        		for (WeaponAPI wep : currGroup.getWeaponsCopy()) {
        			if (!wep.getId().equals("espc_remmortar"))
        				allMortars = false;
        			else
        				count++;
        			if (weapon.equals(wep))
        				foundSelf = true;
        		}
        		if (count > 1 && allMortars && foundSelf) {
        			group = currGroup;
        			break;
        		}
        	}
        	if (count <= 1) {	
        		count = -1;
        		return;
        	} else if (group.equals(weapon.getShip().getSelectedGroupAPI()))
        		return;
    	}
    	
    	for (WeaponAPI wep : group.getWeaponsCopy()) {
    		if (weapon.equals(wep))
    			continue;
    		if ((weapon.getCooldownRemaining() - wep.getCooldownRemaining() + CD_SPACING * 0.65f) % weapon.getCooldown() < CD_SPACING) {
    			wep.setRemainingCooldownTo(wep.getCooldownRemaining() + CD_SPACING);
    		}
    	}
    }
}