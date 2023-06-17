package data.scripts.shipsystems;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

public class espc_MissileBoostStats extends BaseShipSystemScript {

	private static final float ROF_BONUS = 0.5f;
	private static final float DAMAGE_BONUS = 0.5f;
	private static final float SPEED_BONUS = 0.5f;
	
	private boolean didRepair = false;
	
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		
		if (!didRepair) {
			didRepair = true;
			ShipAPI ship = (ShipAPI) stats.getEntity();
			if (ship == null)
				return;
			for (WeaponAPI weapon : ship.getAllWeapons()) {
				if (weapon.getSize() == WeaponSize.LARGE && weapon.isDisabled() && !weapon.isPermanentlyDisabled()) {
					weapon.repair();
					break;
				}
			}
		}
		
		stats.getMissileRoFMult().modifyMult(id, 1f + ROF_BONUS * effectLevel);
		stats.getMissileWeaponDamageMult().modifyMult(id, 1f + DAMAGE_BONUS * effectLevel);
		stats.getMissileMaxSpeedBonus().modifyMult(id, 1f + SPEED_BONUS * effectLevel);
		
	}
	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getMissileRoFMult().unmodify(id);
		stats.getMissileWeaponDamageMult().unmodify(id);
		stats.getMissileMaxSpeedBonus().unmodify(id);
		didRepair = false;
	}
	
	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 2)
			return new StatusData("missile rate of fire +" + (int) (ROF_BONUS * effectLevel * 100f)  + "%", false);
		if (index == 1)
			return new StatusData("missile damage +" + (int) (DAMAGE_BONUS * effectLevel * 100f)  + "%", false);
		if (index == 0)
			return new StatusData("missile speed +" + (int) (SPEED_BONUS * effectLevel * 100f)  + "%", false);
		return null;
	}
}