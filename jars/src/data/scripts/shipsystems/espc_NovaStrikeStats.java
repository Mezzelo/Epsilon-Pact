package data.scripts.shipsystems;

import java.awt.Color;

import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI.SystemState;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript.State;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;

public class espc_NovaStrikeStats extends BaseShipSystemScript {

	private static final float SYSTEM_RANGE = 2000f;
	public static final float MIN_SPAWN_DIST = 75f;
	public static final float MIN_SPAWN_DIST_FRIGATE = 110f;
	
	public static final Color JITTER_COLOR = new Color(255,155,255,75);
	public static final Color JITTER_UNDER_COLOR = new Color(255,155,255,155);
	
	private ShipAPI targ;
	private boolean started = false;
	private espc_NovaStrikePlugin plugin;
	
	public void setTarget(ShipAPI targ) {
		if (this.targ == null)
			this.targ = targ;
	}

	public static float getRange(ShipAPI ship) {
		if (ship == null) return SYSTEM_RANGE;
		return ship.getMutableStats().getSystemRangeBonus().computeEffective(SYSTEM_RANGE);
	}

	public float getFuseTime() {
		return espc_NovaStrikePlugin.FUSE_TIME;
	}
	
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		ShipAPI ship = (ShipAPI) stats.getEntity();
		if (effectLevel > 0f && state != State.OUT) {
			if (!started) {
				started = true;
				if (plugin == null) {
					plugin = new espc_NovaStrikePlugin(ship);
					Global.getCombatEngine().addPlugin(plugin);
				}
			}
			for (WeaponAPI w : ((ShipAPI) stats.getEntity()).getAllWeapons()) {
				if (w.isDecorative() && w.getSpec().hasTag(Tags.NOVA)) {
					w.setForceFireOneFrame(true);
					w.forceShowBeamGlow();
				}
			}
		} else if (effectLevel > 0f && state == State.OUT) {
			started = false;
			// summon code here
		}
		
		ship.setJitterUnder(this, JITTER_UNDER_COLOR, 
			effectLevel * (state == State.OUT ? effectLevel : 1f), 
			11, 0f, effectLevel * (state == State.OUT ? effectLevel : 1f) * 25f + 3f);
		ship.setJitter(this, JITTER_COLOR, 
			effectLevel * (state == State.OUT ? effectLevel : 1f), 4, 0f, 
			effectLevel * (state == State.OUT ? effectLevel : 1f) * 25f);
	}
	public void unapply(MutableShipStatsAPI stats, String id) {
		targ = null;
		started = false;
	}

	
	@Override
	public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
		if (system.isOutOfAmmo()) return null;
		if (system.getState() != SystemState.IDLE) return null;
		
		Vector2f target = ship.getMouseTarget();
		if (target != null) {
			float dist = Misc.getDistance(ship.getLocation(), target);
			float max = getRange(ship) + ship.getCollisionRadius();
			if (dist > max) {
				return "OUT OF RANGE";
			} else {
				return "READY";
			}
		}
		return null;
	}

	
	@Override
	public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
		return ship.getMouseTarget() != null;
	}
	
	
	private Vector2f findClearLocation(ShipAPI ship, Vector2f dest) {
		if (isLocationClear(dest)) return dest;
		
		float incr = 50f;

		WeightedRandomPicker<Vector2f> tested = new WeightedRandomPicker<Vector2f>();
		for (float distIndex = 1; distIndex <= 32f; distIndex *= 2f) {
			float start = (float) Math.random() * 360f;
			for (float angle = start; angle < start + 360; angle += 60f) {
				Vector2f loc = Misc.getUnitVectorAtDegreeAngle(angle);
				loc.scale(incr * distIndex);
				Vector2f.add(dest, loc, loc);
				tested.add(loc);
				if (isLocationClear(loc)) {
					return loc;
				}
			}
		}
		
		if (tested.isEmpty()) return dest; // shouldn't happen
		
		return tested.pick();
	}
	
	private boolean isLocationClear(Vector2f loc) {
		for (ShipAPI other : Global.getCombatEngine().getShips()) {
			if (other.isShuttlePod()) continue;
			if (other.isFighter()) continue;
			
//			Vector2f otherLoc = other.getLocation();
//			float otherR = other.getCollisionRadius();
			
//			if (other.isPiece()) {
//				System.out.println("ewfewfewfwe");
//			}
			Vector2f otherLoc = other.getShieldCenterEvenIfNoShield();
			float otherR = other.getShieldRadiusEvenIfNoShield();
			if (other.isPiece()) {
				otherLoc = other.getLocation();
				otherR = other.getCollisionRadius();
			}
			
			
//			float dist = Misc.getDistance(loc, other.getLocation());
//			float r = other.getCollisionRadius();
			float dist = Misc.getDistance(loc, otherLoc);
			float r = otherR;
			//r = Math.min(r, Misc.getTargetingRadius(loc, other, false) + r * 0.25f);
			float checkDist = MIN_SPAWN_DIST;
			if (other.isFrigate()) checkDist = MIN_SPAWN_DIST_FRIGATE;
			if (dist < r + checkDist) {
				return false;
			}
		}
		for (CombatEntityAPI other : Global.getCombatEngine().getAsteroids()) {
			float dist = Misc.getDistance(loc, other.getLocation());
			if (dist < other.getCollisionRadius() + MIN_SPAWN_DIST) {
				return false;
			}
		}
		
		return true;
	}


}
