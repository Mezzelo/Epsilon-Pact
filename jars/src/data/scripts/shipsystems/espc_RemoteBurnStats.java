package data.scripts.shipsystems;

import java.awt.Color;

import org.lazywizard.lazylib.MathUtils;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.EmpArcEntityAPI.EmpArcParams;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipSystemAPI.SystemState;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
// import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
// import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
//vimport com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Misc;

import data.scripts.util.MezzUtils;

public class espc_RemoteBurnStats extends BaseShipSystemScript {

	private static final Color ENGINE_COLOR = new Color(100,255,100,255);
	private static final Color JITTER_COLOR = new Color(0,255,200,255);
	private static final float ABILITY_RANGE = 1750f;
	private static final float BURN_SPEED = 600f;
	private static final float BURN_ACCEL = 1200f;
	private static final float USE_DELAY = 0.1f;
	private static final float IN_DURATION = 0.5f;
	private int useState = 0;
	private ShipAPI targ = null;
	
	public static float getMaxRange(ShipAPI ship) {
		return ship.getMutableStats().getSystemRangeBonus().computeEffective(ABILITY_RANGE);
	}
	
	public void setTarget(ShipAPI targ) {
		if (this.targ == null)
			this.targ = targ;
	}
	
	protected ShipAPI findTarget(ShipAPI ship) {
		float range = getMaxRange(ship);
		boolean player = ship == Global.getCombatEngine().getPlayerShip();
		ShipAPI target = ship.getShipTarget();
		if (target != null) {
			if (target.isStationModule() || target.getMaxSpeedWithoutBoost() <= 0f || target.isStation() ||
				target.isShuttlePod() || target.hasTag(Tags.VARIANT_FX_DRONE) ||
				MathUtils.getDistanceSquared(ship.getLocation(), target.getLocation())
				> Math.pow(range + ship.getCollisionRadius() + target.getCollisionRadius(), 2f)) target = null;
		} else {
			if (target == null || target.getOwner() == ship.getOwner()) {
				if (player) {
					target = Misc.findClosestShipTo(ship, ship.getMouseTarget(), HullSize.FRIGATE, range, true, false, null);
					if (target == null)
						return target;
					if (target.isStationModule() || target.getMaxSpeedWithoutBoost() <= 0f || target.isStation()
						|| target.isShuttlePod() || target.hasTag(Tags.VARIANT_FX_DRONE))
						target = null;
				} else {
					Object test = ship.getAIFlags().getCustom(AIFlags.MANEUVER_TARGET);
					if (test instanceof ShipAPI) {
						target = (ShipAPI) test;
						if (target.isStationModule() || target.getMaxSpeedWithoutBoost() <= 0f || target.isStation() ||
							MathUtils.getDistanceSquared(ship.getLocation(), target.getLocation())
							> Math.pow(range + ship.getCollisionRadius() + target.getCollisionRadius(), 2f)) target = null;
					}
				}
			}
		}
		
		return target;
	}
	
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		if (stats.getEntity() == null)
			return;

		if (useState == 0) {
			useState = 1;
			if (targ == null)
				targ = findTarget((ShipAPI) stats.getEntity());
			if (targ != null) {
				ShipAPI ship = (ShipAPI) stats.getEntity();
				
				EmpArcParams params = new EmpArcParams();
				params.segmentLengthMult = 8f;
				params.zigZagReductionFactor = 0.15f;
				params.fadeOutDist = 500f;
				params.minFadeOutMult = 2f;
				params.flickerRateMult = 0.7f;
				params.flickerRateMult = 0.3f;
				params.movementDurMax = USE_DELAY / 2f +
					USE_DELAY / 2f * MathUtils.getDistance(targ, ship) / getMaxRange(ship);
				EmpArcEntityAPI arc = (EmpArcEntityAPI) 
					Global.getCombatEngine().spawnEmpArcVisual(ship.getLocation(), ship, targ.getLocation(), targ,
						40f, // thickness
						//new Color(100,165,255,255),
						ENGINE_COLOR,
						new Color(255,255,255,255),
						params
					);
				arc.setTargetToShipCenter(ship.getLocation(), targ);
				arc.setCoreWidthOverride(50f);
				arc.setSingleFlickerMode(true);
			}
		} else if (useState == 1 && targ != null && state.equals(State.ACTIVE)) {
			useState = 2;
			Global.getSoundPlayer().playSound(
				"system_plasma_burn",
				0.9f,
				0.7f,
				targ.getLocation(),
				targ.getVelocity()
			);
		}
		if (targ == null || (state.equals(State.IN) && effectLevel <= USE_DELAY / (USE_DELAY + IN_DURATION)))
			return;
		
		if (targ != null && !targ.isHulk() && targ.isAlive()) {
			targ.blockCommandForOneFrame(ShipCommand.DECELERATE);
			targ.blockCommandForOneFrame(ShipCommand.ACCELERATE_BACKWARDS);
			// targ.blockCommandForOneFrame(ShipCommand.TURN_LEFT);
			// targ.blockCommandForOneFrame(ShipCommand.TURN_RIGHT);
			targ.blockCommandForOneFrame(ShipCommand.STRAFE_LEFT);
			targ.blockCommandForOneFrame(ShipCommand.STRAFE_RIGHT);
			targ.giveCommand(ShipCommand.ACCELERATE, state, 0);
			targ.getFluxTracker().increaseFlux(2f, false);
		}
		
		float level = Math.max(0f, effectLevel - (state.equals(State.IN) ? USE_DELAY : 0f)
			/ (state.equals(State.IN) ? IN_DURATION : 1f));
		if (state.equals(State.OUT))
			targ.getMutableStats().getMaxSpeed().unmodify(id);
		else {
			if (state.equals(State.IN))
				targ.setJitterUnder(this, JITTER_COLOR, 1f - level, 
					5, 0f, 25f
				);
			
			targ.getMutableStats().getMaxSpeed().modifyFlat(id, BURN_SPEED * level);
			targ.getMutableStats().getAcceleration().modifyFlat(id, BURN_ACCEL * level);
		}

		Global.getSoundPlayer().playLoop(
			"system_plasma_burn_loop",
			targ,
			1.0f,
			1.0f * level,
			targ.getLocation(),
			targ.getVelocity()
		);
		targ.getEngineController().fadeToOtherColor(this, ENGINE_COLOR, new Color(0,0,0,0), level, 0.67f);
		targ.getEngineController().extendFlame(this, 3f * level, 1.5f * level, 3f * level);

		
	}
	public void unapply(MutableShipStatsAPI stats, String id) {
		useState = 0;
		if (targ != null) {
			targ.getMutableStats().getMaxSpeed().unmodify(id);
			targ.getMutableStats().getAcceleration().unmodify(id);
			targ = null;
		}
		if (stats.getEntity() == null)
			return;
	}
	
	@Override
	public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
		if (system.getState() != SystemState.IDLE || system.isOutOfAmmo())
			return null;
		
		ShipAPI hasTarget = findTarget(ship);

		if (hasTarget != null && hasTarget != ship) {
			if (hasTarget.isPhased())
				return MezzUtils.getString("espc_shipsystem", "target_phased");
			else if (hasTarget.getEngineController() != null &&
				hasTarget.getEngineController().isFlamedOut())
				return MezzUtils.getString("espc_shipsystem", "target_flamed_out");
			else
				return MezzUtils.getString("espc_shipsystem", "ready");
		}
		
		if ((hasTarget == null) && ship.getShipTarget() != null) {
			if (ship.getShipTarget().getEngineController() != null &&
				ship.getShipTarget().getEngineController().isFlamedOut())
				return MezzUtils.getString("espc_shipsystem", "target_flamed_out");
			else if (ship.getShipTarget().isStationModule() || ship.getShipTarget().isStation() || 
				ship.getShipTarget().getMaxSpeedWithoutBoost() <= 0f)
				return MezzUtils.getString("espc_shipsystem", "invalid_target");
			return MezzUtils.getString("espc_shipsystem", "out_of_range");
		}
		return MezzUtils.getString("espc_shipsystem", "no_target");
	}
	
	
	@Override
	public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
		ShipAPI hasTarget = findTarget(ship);
		if (hasTarget == null)
			return false;
		if (hasTarget == ship)
			return false;
		if (hasTarget.hasTag(Tags.VARIANT_FX_DRONE))
			return false;
		if (hasTarget.isShuttlePod())
			return false;
		if (hasTarget.isPhased())
			return false;
		if (hasTarget.isStationModule() || hasTarget.isStation() || 
				hasTarget.getMaxSpeedWithoutBoost() <= 0f)
			return false;
		if (hasTarget.getEngineController() == null)
			return false;
		if (hasTarget.getEngineController().isFlamedOut())
			return false;
		return true;
	}
}
