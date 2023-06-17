package data.scripts.shipsystems;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

public class espc_AMCompressorStats extends BaseShipSystemScript {

	// private static final float RANGE_BONUS = 0.5f;
	private static final float DAMAGE_BONUS = 0.5f;
	
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		// stats.getEnergyWeaponRangeBonus().modifyMult(id, 1f + RANGE_BONUS * effectLevel);
		stats.getEnergyWeaponDamageMult().modifyMult(id, 1f + DAMAGE_BONUS * effectLevel);
	}
	public void unapply(MutableShipStatsAPI stats, String id) {
		// stats.getEnergyWeaponRangeBonus().unmodify(id);
		stats.getEnergyWeaponDamageMult().unmodify(id);
	}
	
	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0) {
		// 	return new StatusData("AM Flamer range +" + (int) (RANGE_BONUS * effectLevel * 100f) + "%", false);
		// }
		// if (index == 1) {
			return new StatusData("AM Flamer damage +" + (int) (DAMAGE_BONUS * effectLevel * 100f) + "%", false);
		}
		return null;
	}
}
