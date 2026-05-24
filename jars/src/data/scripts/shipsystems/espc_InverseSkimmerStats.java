package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.Misc;

import data.scripts.util.MezzUtils;

import com.fs.starfarer.api.combat.ShipSystemAPI.SystemState;


import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.List;

import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;

public class espc_InverseSkimmerStats extends BaseShipSystemScript {
	
	private static final float ABILITY_RANGE = 1750f;
	private static final float ABILITY_RANGE_CAPITAL = 2250f;
	// additional radius around the given ship's shield bounds (or collision bounds, whichever is more generous as to mod-proof) 
	
	private ArrayDeque<ShipAPI> alliedTargets = new ArrayDeque<ShipAPI>();
	private boolean used = false;
	private espc_InverseSkimmerPlugin plugin = null;
	
	public void setAlliedTarget(ShipAPI ally) {
		// it's spaghetti.
		alliedTargets.clear();
		alliedTargets.add(ally);
	}
	
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		if (stats.getEntity() == null)
			return;

		CombatEngineAPI combatEngine = Global.getCombatEngine();
		if (combatEngine.getElapsedInLastFrame() <= 0f || combatEngine.isPaused())
			return;
		
		if (used)
			return;
		
		used = true;
		
		ShipAPI ship = (ShipAPI) stats.getEntity();
		
		if (plugin == null) {
			plugin = new espc_InverseSkimmerPlugin(ship);
			combatEngine.addPlugin(plugin);
		}
		
		if (ship.getShipAI() == null && !ship.getHullSize().equals(HullSize.CAPITAL_SHIP))
			alliedTargets.add(findTarget(ship));
		else if (ship.getHullSize().equals(HullSize.CAPITAL_SHIP)) {
			List<ShipAPI> targetAllies = CombatUtils.getShipsWithinRange(ship.getLocation(), 
				getMaxRange(ship));
			for (ShipAPI hoverShip : targetAllies) {
				if (hoverShip.getOwner() != ship.getOwner() || hoverShip.isHulk() ||
					hoverShip.equals(ship) || hoverShip.isShuttlePod() ||
					hoverShip.isFighter() || hoverShip.hasTag(Tags.VARIANT_FX_DRONE) ||
					hoverShip.getCustomData().containsKey("espc_InverseSkimmer_Ally"))
					continue;
				alliedTargets.add(hoverShip);
			}
		}
		
		for (ShipAPI allyShip : alliedTargets) {
			plugin.addAlly(allyShip);
		}
		
		plugin.calculatePortals(combatEngine.getTotalElapsedTime(false));
	    ship.setCustomData("espc_InverseSkimmer_Ally", true);
		alliedTargets.clear();
		/*
		if (target != null) {
			Global.getSoundPlayer().playLoop(
				"espc_temporal_loop",
				ship,
				1f,
				// state == State.OUT ? 1.2f - effectLevel * 0.6f : effectLevel * 0.6f,
				effectLevel * 0.75f,
				target.getLocation(),
				target.getVelocity()
			);
		} */
	}
	
	public void unapply(MutableShipStatsAPI stats, String id) {
		used = false;
		if (plugin != null) {
			plugin.cancelSystem();
		}
	}
	
	public static float getMaxRange(ShipAPI ship) {
		return ship.getMutableStats().getSystemRangeBonus().computeEffective(
			ship.getHullSize().equals(HullSize.CAPITAL_SHIP) ? ABILITY_RANGE_CAPITAL : ABILITY_RANGE);
	}
	
	protected ShipAPI findTarget(ShipAPI ship) {
		ShipAPI target = ship.getShipTarget();
		if (target == null || !target.isAlive() || target.isHulk() || target.getOwner() != ship.getOwner() ||
			target.isPhased() || target.isShuttlePod() || target.isFighter() ||
			target.getCustomData().containsKey("espc_InverseSkimmer_Ally"))
			target = null;
		else
			return target;
		
		if (ship.getShipAI() == null) {
			target = Misc.findClosestShipTo(ship, ship.getMouseTarget(), HullSize.FRIGATE, getMaxRange(ship), true, false, null);
			if (target == null || !target.isAlive() || target.isHulk() || target.getOwner() != ship.getOwner() ||
				target.isPhased() || target.isShuttlePod() || target.isFighter() ||
				target.getCustomData().containsKey("espc_InverseSkimmer_Ally"))
				return null;
			else
				return target;
		}
		return target;
	}

	@Override
	public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
		if (system.getState() != SystemState.IDLE)
			return null;
		
		if (ship.getHullSize().equals(HullSize.CAPITAL_SHIP)) {
			int numShips = 0;
    		Iterator<Object> shipGridIterator = (Iterator<Object>) (Global.getCombatEngine().getAiGridShips().getCheckIterator(
        		ship.getLocation(),
        		getMaxRange(ship) * 2f,
        		getMaxRange(ship) * 2f)
        	);
    		while (shipGridIterator.hasNext()) {
    			ShipAPI currShip = (ShipAPI) shipGridIterator.next();
    			if (currShip.isShuttlePod() || currShip.equals(ship) || currShip.hasTag(Tags.VARIANT_FX_DRONE)
    				|| currShip.isFighter() || currShip.isHulk() || !currShip.isAlive()
    				|| currShip.isPhased() || currShip.getOwner() != ship.getOwner() ||
    				currShip.getCustomData().containsKey("espc_InverseSkimmer_Ally"))
    				continue;
    			numShips++;
    		}
			if (numShips > 0)
				return String.format(MezzUtils.getString("espc_shipsystem", 
					numShips > 1 ? "ready_target_count" : "ready_target_count_single"),
					numShips + "");
			else
				return MezzUtils.getString("espc_shipsystem", "no_valid_targets");
		}
		
		ShipAPI hasTarget = findTarget(ship);

		if (hasTarget != null) {
			float range = getMaxRange(ship);
			if (MathUtils.getDistanceSquared(ship, hasTarget) > range * range)
				return MezzUtils.getString("espc_shipsystem", "out_of_range");
			return MezzUtils.getString("espc_shipsystem", "ready");
		}
		else if (ship.getShipTarget() != null)
			return MezzUtils.getString("espc_shipsystem", "ally_required");
		return MezzUtils.getString("espc_shipsystem", "no_target");
	}
	
	
	@Override
	public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
		if (ship.getShipAI() != null)
			return true;
		if (ship.getHullSize().equals(HullSize.CAPITAL_SHIP)) {
    		Iterator<Object> shipGridIterator = (Iterator<Object>) (Global.getCombatEngine().getAiGridShips().getCheckIterator(
        		ship.getLocation(),
        		getMaxRange(ship) * 2f,
        		getMaxRange(ship) * 2f)
        	);
    		while (shipGridIterator.hasNext()) {
    			ShipAPI currShip = (ShipAPI) shipGridIterator.next();
    			if (currShip.isShuttlePod() || currShip.equals(ship) || currShip.hasTag(Tags.VARIANT_FX_DRONE)
    				|| currShip.isFighter()
    				|| currShip.isHulk() || !currShip.isAlive()
    				|| currShip.getOwner() != ship.getOwner() ||
    				currShip.getCustomData().containsKey("espc_InverseSkimmer_Ally"))
    				continue;
    			return true;
    		}
			return false;
		}
		ShipAPI hasTarget = findTarget(ship);
		if (hasTarget == null)
			return false;
		if (hasTarget == ship)
			return false;
		float range = getMaxRange(ship);
		if (MathUtils.getDistanceSquared(ship, hasTarget) > range * range)
			return false;
		
		return true;
	}
}