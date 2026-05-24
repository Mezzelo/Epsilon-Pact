package data.scripts.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

import data.scripts.util.MezzUtils;

public class espc_PactModifications extends BaseHullMod {

	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0)
			return MezzUtils.getString("espc_hullmod", "pactmods1");
		else if (index == 1)
			return MezzUtils.getString("espc_hullmod", "pactmods2");
		return null;
	}
	
}