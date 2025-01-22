package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;

import java.util.List;
				
public class CHM_epsilpacPlugin extends BaseEveryFrameCombatPlugin {
	
	private static final float MIN_FLUX_THRESHOLD = 0.7f;
	private static final float MAX_FLUX_DIFF = 0.2f;
	
	private static final float MAX_HULL_THRESHOLD = 0.3f;
	private static final float MIN_HULL_DIFF = 0.2f;
	
	
	private ShipAPI ship;
	private float thisBonus = 5f;
	
	
	public CHM_epsilpacPlugin(ShipAPI ship) {
		this.ship = ship;
		if (ship.getHullSize() == HullSize.FRIGATE)
			thisBonus = 20f;
		else if (ship.getHullSize() == HullSize.DESTROYER)
			thisBonus = 15f;
		else if (ship.getHullSize() == HullSize.CRUISER)
			thisBonus = 10f;
	}
	
    @Override
	public void advance(float amount, List<InputEventAPI> events) {
    	if (ship == null || ship.isHulk()) {
    		Global.getCombatEngine().removePlugin(this);
    		return;
    	}
    	
    	float bonus = thisBonus * Math.max(Math.min((Float.isNaN(ship.getFluxLevel()) ? 0f : ship.getFluxLevel() - MIN_FLUX_THRESHOLD) / MAX_FLUX_DIFF, 1f), 0f)
    	+ thisBonus * Math.max(Math.min((MAX_HULL_THRESHOLD - ship.getHullLevel()) / MIN_HULL_DIFF, 1f), 0f);
		ship.getMutableStats().getBallisticWeaponDamageMult().modifyPercent("CHM_epsilpacmod", 
			bonus);
		ship.getMutableStats().getEnergyWeaponDamageMult().modifyPercent("CHM_epsilpacmod", 
			bonus);
	}
}