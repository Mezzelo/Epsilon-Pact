package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
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
	private ShipAPI targetOverride = null;
	private boolean used = false;
	private espc_InverseSkimmerPlugin plugin = null;
	
	public void setAlliedTarget(ShipAPI ally) {
		alliedTargets.add(ally);
	}
	
	public void setTarget(ShipAPI target) {
		targetOverride = target;
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
		ShipAPI target = targetOverride != null ? targetOverride : findTarget(ship);
		
		if (plugin == null) {
			plugin = new espc_InverseSkimmerPlugin(ship);
			combatEngine.addPlugin(plugin);
		}
		
		if (ship.getShipAI() == null && ship.getHullSize() != HullSize.CAPITAL_SHIP)
			alliedTargets.add(target);
		else if (ship.getHullSize().equals(HullSize.CAPITAL_SHIP)) {
			List<ShipAPI> targetAllies = CombatUtils.getShipsWithinRange(ship.getLocation(), 
				getMaxRange(ship));
			for (ShipAPI hoverShip : targetAllies) {
				if (hoverShip.getOwner() == ship.getOwner() && !hoverShip.isHulk() &&
					hoverShip != ship &&
					!hoverShip.isFighter() &&
					!hoverShip.getCustomData().containsKey("espc_InverseSkimmer_Ally")) {
					alliedTargets.add(hoverShip);
				}
			}
		}
		
		for (ShipAPI allyShip : alliedTargets) {
			plugin.addAlly(allyShip);
		    
		}
		plugin.calculatePortals(combatEngine.getTotalElapsedTime(false));
	    ship.setCustomData("espc_InverseSkimmer_Ally", true);
		alliedTargets.clear();
		targetOverride = null;
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
		if (target == null)
			return target;
		if (target.isFighter() ||
			!target.isAlive() || target.isHulk() || target.getOwner() != ship.getOwner()
			|| target.getCustomData().containsKey("espc_InverseSkimmer_Ally")) target = null;
		
		return target;
	}

	@Override
	public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
		if (system.getState() != SystemState.IDLE)
			return null;
		
		if (ship.getShipAI() != null)
			return "READY";
		
		if (ship.getHullSize().equals(HullSize.CAPITAL_SHIP)) {
			int numShips = 0;
    		Iterator<Object> shipGridIterator = (Iterator<Object>) (Global.getCombatEngine().getAiGridShips().getCheckIterator(
        		ship.getLocation(),
        		ABILITY_RANGE_CAPITAL * 2f,
        		ABILITY_RANGE_CAPITAL * 2f)
        	);
    		while (shipGridIterator.hasNext()) {
    			ShipAPI currShip = (ShipAPI) shipGridIterator.next();
    			if (!currShip.isShuttlePod() && currShip != ship && !currShip.hasTag(Tags.VARIANT_FX_DRONE)
    				&& !currShip.isFighter()
    				&& !currShip.isHulk() && currShip.isAlive()
    				&& !currShip.isPhased() && currShip.getOwner() == ship.getOwner() &&
    				currShip.getFluxTracker() != null &&
    				!currShip.getFluxTracker().isOverloadedOrVenting())
    				numShips++;
    		}
			if (numShips > 0)
				return ("READY: " + numShips + " TARGETS");
			else
				return "NO VALID TARGETS";
		}
		
		ShipAPI hasTarget = findTarget(ship);

		if (hasTarget != null) {
			float range = getMaxRange(ship);
			if (MathUtils.getDistanceSquared(ship, hasTarget) > range * range)
				return "OUT OF RANGE";
			return "READY";
		}
		else if (ship.getShipTarget() != null)
			return "ALLY TARGET REQUIRED";
		return "NO TARGET";
	}
	
	
	@Override
	public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
		if (ship.getShipAI() != null)
			return true;
		if (ship.getHullSize().equals(HullSize.CAPITAL_SHIP)) {
    		Iterator<Object> shipGridIterator = (Iterator<Object>) (Global.getCombatEngine().getAiGridShips().getCheckIterator(
        		ship.getLocation(),
        		ABILITY_RANGE_CAPITAL * 2f,
        		ABILITY_RANGE_CAPITAL * 2f)
        	);
    		while (shipGridIterator.hasNext()) {
    			ShipAPI currShip = (ShipAPI) shipGridIterator.next();
    			if (!currShip.isShuttlePod() && currShip != ship && !currShip.hasTag(Tags.VARIANT_FX_DRONE)
    				&& !currShip.isFighter()
    				&& !currShip.isHulk() && currShip.isAlive()
    				&& !currShip.isPhased() && currShip.getOwner() == ship.getOwner() &&
    				currShip.getFluxTracker() != null &&
    				!currShip.getFluxTracker().isOverloadedOrVenting())
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