package data.scripts.shipsystems;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.combat.ShipSystemAPI.SystemState;

// import com.fs.starfarer.api.SoundPlayerAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import java.awt.Color;
import org.lazywizard.lazylib.MathUtils;

public class espc_MomentumStats extends BaseShipSystemScript {
	
	public static final float ABILITY_RANGE = 1750f;
	public static final float LINEAR_MULT = 3f;
	public static final float ANGULAR_MULT = 12f;
	public static final float FLAMEOUT_MULT = 5f;
	
	public static final float ACCEL_MOD = 0.1f;
	public static final float TURN_ACCEL_MOD = 0.1f;
	
	private boolean used = false;
	private ShipAPI target;
	
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		if (stats.getEntity() == null)
			return;
		
		ShipAPI ship = (ShipAPI) stats.getEntity();
		
		if (state == State.ACTIVE) {
			if (!used) {
				target.getMutableStats().getAcceleration().modifyMult(id, ACCEL_MOD);
				target.getMutableStats().getDeceleration().modifyMult(id, ACCEL_MOD);
				target.getMutableStats().getTurnAcceleration().modifyMult(id, TURN_ACCEL_MOD);
				
				target.getVelocity().scale(LINEAR_MULT);
				target.setAngularVelocity(target.getAngularVelocity() * (ANGULAR_MULT));
				
				/* extremely funny, but annoying.
				if (Math.hypot(target.getVelocity().x, target.getVelocity().y) > target.getMaxSpeed() * FLAMEOUT_MULT) {
					for (ShipEngineAPI thisEngine : target.getEngineController().getShipEngines())
						thisEngine.disable();
				} */
				used = true;
			}
			else {
				target.setJitter(this, new Color(50, 100, 255, 120), effectLevel, 2, effectLevel * 30f);
			}
		} else if (state == State.IN) {
			if (target == null) {
				target = ship.getShipTarget();
			}
			target.setJitter(this, new Color(50, 100, 255, 140), effectLevel, 2, effectLevel * 60f);
			ship.setJitterUnder(this, new Color(50, 100, 255, 170), effectLevel, 2, effectLevel * 60f);
			//ship.setJitter
		}
	}
	
	public void unapply(MutableShipStatsAPI stats, String id) {
		if (target != null && used) {
			target.getMutableStats().getAcceleration().unmodify(id);
			target.getMutableStats().getDeceleration().unmodify(id);
			target.getMutableStats().getTurnAcceleration().unmodify(id);
			target = null;
			used = false;
		}
	}

	@Override
	public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
		// if (system.isOutOfAmmo()) 
			// return null;
		if (system.getState() != SystemState.IDLE)
			return null;
		
		ShipAPI hasTarget = ship.getShipTarget();

		if (hasTarget == null)
			return "NO TARGET";

		return MathUtils.isWithinRange(hasTarget, ship, ABILITY_RANGE) ? "READY" : "OUT OF RANGE";
		//return super.getInfoText(system, ship);
	}

	
	@Override
	public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
		ShipAPI hasTarget = ship.getShipTarget();
		if (hasTarget == null)
			return false;
		else
			return MathUtils.isWithinRange(hasTarget, ship, ABILITY_RANGE);
	}
	
}