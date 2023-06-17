package data.scripts.shipsystems;

// import java.awt.Color;
// import java.util.Iterator;
// import java.util.LinkedList;
//vimport java.util.List;

// import org.lazywizard.lazylib.MathUtils;
// import org.lazywizard.lazylib.VectorUtils;
// import org.lwjgl.opengl.GL11;
// import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
// import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
// import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
//vimport com.fs.starfarer.api.util.Misc;

//vimport data.scripts.plugins.MagicAutoTrails.trailData;
// import data.scripts.plugins.MagicTrailPlugin;

public class espc_OverpressureStats extends BaseShipSystemScript {

	public static final float ROF_BONUS = -0.33f;
	public static final float DAMAGE_BONUS = 2.0f;
	public static final float VELOCITY_BONUS = 0.33f;
	// public static final float RANGE_BONUS = 0.05f;
	private boolean debounce = false;
	
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		if (stats.getEntity() == null)
			return;
		
		if (!debounce && state != State.IDLE) {
			debounce = true;
			Global.getCombatEngine().addPlugin(new espc_OverpressureVFX((ShipAPI) stats.getEntity()));	
		}

		stats.getBallisticRoFMult().modifyMult(id, 1f + ROF_BONUS * effectLevel);
		stats.getBallisticWeaponDamageMult().modifyMult(id, 1f + DAMAGE_BONUS * effectLevel);
		stats.getBallisticProjectileSpeedMult().modifyMult(id, 1f + VELOCITY_BONUS * effectLevel);
		
		// as beams are contiguous, we can emulate the systems' effects on them with the ROF mult
		stats.getBeamWeaponDamageMult().modifyMult(id, 1f + ROF_BONUS * effectLevel);
		stats.getBeamWeaponFluxCostMult().modifyMult(id, 1f + ROF_BONUS * effectLevel);
		
	}
	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getBallisticRoFMult().unmodify(id);
		stats.getBallisticWeaponDamageMult().unmodify(id);
		stats.getBallisticProjectileSpeedMult().unmodify(id);
		stats.getBeamWeaponDamageMult().unmodify(id);
		stats.getBeamWeaponFluxCostMult().unmodify(id);
		// stats.getBallisticWeaponRangeBonus().unmodify(id);
		debounce = false;
	}
	
	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 2) {
			return new StatusData("ballistic damage +" + ((int) (effectLevel * DAMAGE_BONUS * 100)) + "%", false);
		}
		if (index == 1) {
			return new StatusData("ballistic projectile speed +" + ((int) (effectLevel * VELOCITY_BONUS * 100)) + "%", false);
		}
		if (index == 0) {
			return new StatusData("ballistic rate of fire -" + ((int) (effectLevel * ROF_BONUS * -100)) + "%", false);
		}
		// if (index == 3) {
		// 	return new StatusData("ballistic range +" + ((int) (effectLevel * RANGE_BONUS * 100)) + "%", false);
		// }
		return null;
	}
}
