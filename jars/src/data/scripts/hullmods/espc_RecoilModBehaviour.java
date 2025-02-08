package data.scripts.hullmods;

import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;

// import java.util.List;
// import java.util.ArrayList;
// import java.util.Iterator;

public class espc_RecoilModBehaviour extends BaseHullMod {
	
	private static final float ROF_BONUS = 0.5f;
	
    @Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        Global.getCombatEngine().addPlugin(new espc_RecoilModPlugin(ship));
		List<WeaponAPI> shipWeps = ship.getAllWeapons();
        for (int i = 0; i < shipWeps.size(); i++){
			if (((WeaponAPI) shipWeps.get(i)).getSize() == WeaponSize.LARGE)
				return;
        }
		ship.getMutableStats().getBallisticRoFMult().modifyMult(id, 1f + ROF_BONUS);
		ship.getMutableStats().getBallisticAmmoRegenMult().modifyMult(id, 1f + ROF_BONUS);
	}

	@Override
	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0)
			return ("large and medium");
		else if (index == 1)
			return ("medium");
		else if (index == 2)
			return ((int) (ROF_BONUS * 100f) + "%");
		else if (index == 3)
			return ("Heavy Armor");
		else if (index == 4)
			return ("20%");
		return null;
	}
}