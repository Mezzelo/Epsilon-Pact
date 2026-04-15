package data.scripts.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

public class espc_PactModifications extends BaseHullMod {

	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0)
			return ("altered its shipsystem");
		else if (index == 0)
			return ("permanently lost on restoration");
		return null;
	}
	
}