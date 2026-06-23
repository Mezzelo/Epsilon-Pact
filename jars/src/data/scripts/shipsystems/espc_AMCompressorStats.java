package data.scripts.shipsystems;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

import data.scripts.util.MezzUtils;

public class espc_AMCompressorStats extends BaseShipSystemScript {

	// private static final float RANGE_BONUS = 0.5f;
	private static final float DAMAGE_BONUS = 50f;
	private static final float RANGE_BONUS = 30f;
	private static final float FLUX_USE = 50f;
	private static final float SPEED_BONUS = 100f;
	private static final float HIT_STRENGTH_BONUS = 200f;
	private static final float TURN_MALUS = 50f;
	
	
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		// stats.getEnergyWeaponRangeBonus().modifyMult(id, 1f + RANGE_BONUS * effectLevel);
		stats.getEnergyWeaponDamageMult().modifyPercent(id, DAMAGE_BONUS * effectLevel);
		stats.getEnergyWeaponRangeBonus().modifyPercent(id, RANGE_BONUS);
		stats.getEnergyWeaponFluxCostMod().modifyPercent(id, FLUX_USE * effectLevel);
		stats.getEnergyProjectileSpeedMult().modifyPercent(id, SPEED_BONUS * effectLevel);
		stats.getWeaponTurnRateBonus().modifyPercent(id, -TURN_MALUS * effectLevel);
		stats.getHitStrengthBonus().modifyPercent(id, HIT_STRENGTH_BONUS * effectLevel);
	}
	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getEnergyWeaponDamageMult().unmodify(id);
		stats.getEnergyWeaponRangeBonus().unmodify(id);
		stats.getEnergyWeaponFluxCostMod().unmodify(id);
		stats.getEnergyProjectileSpeedMult().unmodify(id);
		stats.getWeaponTurnRateBonus().unmodify(id);
		stats.getHitStrengthBonus().unmodify(id);
	}
	
	public StatusData getStatusData(int index, State state, float effectLevel) {
		
		if (index == 0)
			return new StatusData(MezzUtils.getString("espc_shipsystem", "amcompressor_active"), false);
		
		return null;
	}
}
