package data.scripts.shipsystems;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

public class espc_PlasmaJoltStats extends BaseShipSystemScript {

	// private static final float RANGE_BONUS = 0.5f;
	private static final float DAMAGE_BONUS = 50f;
	private static final float RANGE_BONUS = 30f;
	private static final float FLUX_USE = 50f;
	private static final float SPEED_MALUS = 50f;
	private static final float TURN_MALUS = 30f;
	
	
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		// stats.getEnergyWeaponRangeBonus().modifyMult(id, 1f + RANGE_BONUS * effectLevel);
		stats.getEnergyWeaponDamageMult().modifyPercent(id, DAMAGE_BONUS * effectLevel);
		stats.getEnergyWeaponRangeBonus().modifyPercent(id, RANGE_BONUS);
		stats.getEnergyWeaponFluxCostMod().modifyPercent(id, FLUX_USE * effectLevel);
		stats.getEnergyProjectileSpeedMult().modifyPercent(id, -SPEED_MALUS * effectLevel);
		stats.getWeaponTurnRateBonus().modifyPercent(id, -TURN_MALUS * effectLevel);
	}
	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getEnergyWeaponDamageMult().unmodify(id);
		stats.getEnergyWeaponRangeBonus().unmodify(id);
		stats.getEnergyWeaponFluxCostMod().unmodify(id);
		stats.getEnergyProjectileSpeedMult().unmodify(id);
		stats.getWeaponTurnRateBonus().unmodify(id);
	}
	
	public StatusData getStatusData(int index, State state, float effectLevel) {
		
		if (index == 4)
			return new StatusData("AM Flamer projectile speed " + (int) (-SPEED_MALUS * effectLevel) + "%", false);
		else if (index == 3)
			return new StatusData("AM Flamer turn rate " + (int) (-TURN_MALUS * effectLevel) + "%", false);
		else if (index == 2)
			return new StatusData("AM Flamer flux generation +" + (int) (FLUX_USE * effectLevel) + "%", false);
		else if (index == 1)
			return new StatusData("AM Flamer damage +" + (int) (DAMAGE_BONUS * effectLevel) + "%", false);
		else if (index == 0)
			return new StatusData("AM Flamer range +" + (int) RANGE_BONUS + "%", false);
		
		return null;
	}
}
