package data.scripts.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

// import java.util.List;
// import java.util.ArrayList;
// import java.util.Iterator;

public class espc_HalfslaughtModBehaviour extends BaseHullMod {

	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getMaxSpeed().modifyFlat(id, 15f);
		stats.getHullBonus().modifyFlat(id, -5000);
		stats.getFluxCapacity().modifyFlat(id, -3000f);
		stats.getFluxDissipation().modifyFlat(id, -100f);
		stats.getMinCrewMod().modifyFlat(id, -150f);
		stats.getMaxCrewMod().modifyFlat(id, -500f);
		stats.getCargoMod().modifyFlat(id, -150f);
		stats.getFuelMod().modifyFlat(id, -150f);
		stats.getMaxBurnLevel().modifyFlat(id, 1f);
	}
	
	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {
		super.advanceInCombat(ship, amount);
    	if (ship == null || ship.isHulk())
    		return;
    	if (ship.getSystem() != null && ship.getSystem().getId().equals("burndrive") &&
    		ship.getSystem().isActive()) {
    		ship.setAngularVelocity(ship.getAngularVelocity() + 0.13f * 
    			Math.min(ship.getSystem().getEffectLevel() * 3f, 1f));
    	}
	}

	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0)
			return ("significant performance alterations");
		return null;
	}
}