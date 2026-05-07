package data.scripts.shipsystems;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
// import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
// import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
//vimport com.fs.starfarer.api.util.Misc;

public class espc_PlasmaJoltStats extends BaseShipSystemScript {

	private static final Color ENGINE_COLOR = new Color(100,255,100,255);
	
	private boolean pluginInit = false;
	private boolean used = false;
	
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		if (stats.getEntity() == null)
			return;
		
		if (!pluginInit) {
			pluginInit = true;
			Global.getCombatEngine().addPlugin(new espc_PlasmaJoltPlugin((ShipAPI) stats.getEntity()));
		}

		// todo: this shouldn't work going backwards!  force the ship's velocity to conform to its vector
		ShipAPI ship = (ShipAPI) stats.getEntity();
		ship.getEngineController().fadeToOtherColor(this, ENGINE_COLOR, new Color(0,0,0,0), 1f, 0.67f);
		ship.getEngineController().extendFlame(this, 2f * 1f, 0f * 1f, 0f * 1f);
		stats.getMaxSpeed().modifyMult(id, 10f * effectLevel);
		ship.getVelocity().scale(ship.getMaxSpeed() / ship.getVelocity().length());
		if (!used) {
			used = true;
		}

		
	}
	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getMaxSpeed().unmodify(id);
		if (stats.getEntity() == null)
			return;
	}
	
	public StatusData getStatusData(int index, State state, float effectLevel) {
		return null;
	}
}
