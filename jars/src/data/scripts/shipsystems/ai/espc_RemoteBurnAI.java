package data.scripts.shipsystems.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShieldAPI.ShieldType;
import com.fs.starfarer.api.combat.ShipHullSpecAPI.ShipTypeHints;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.util.Misc;

import data.scripts.shipsystems.espc_RemoteBurnStats;
import java.util.Iterator;

import org.json.JSONException;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

public class espc_RemoteBurnAI implements ShipSystemAIScript {
    
    private static final float THINK_INTERVAL_NORMAL = 0.5f;
    private static final float THINK_INTERVAL_USED = 0.1f;


    private static final float ALLY_LOW_HARDFLUX = 0.15f;
    private static final float ALLY_HIGH_FLUX = 0.6f;
    private static final float ALLY_LOW_HULL = 0.25f;
    private static final float ALLY_OVERLOAD_REMAINING_MIN = 1f;
    private static final float ALLY_ANGULAR_VELOCITY_THRESHOLD = 10f;
    private static final float ALLY_FORWARDS_VEL_ANGLE_THRESHOLD = 7f;
    private static final float ALLY_MAX_SPEED_THRESHOLD = 0.5f;
    private static final float ALLY_ENEMY_DEST_RANGE = 1800f;
    
    private static final float TARGET_ANGLE_OPPOSITE_THRESHOLD = 30f;
    private static final float TARGET_ANGLE_USER_THRESHOLD = 50f;
    private static final float TARGET_ANGLE_ENGAGE_THRESHOLD = 70f;
    private static final float TARGET_RETREAT_MAX_SPEED_THRESHOLD = 0.7f;
    private static final float TARGET_RETREAT_ANGLE_THRESHOLD = 45f;
    private static final float TARGET_LOW_FLUX = 0.2f;
    private static final float TARGET_OVERLOAD_FLUX_MIN = 0.6f;
    private static final float TARGET_HIGH_FLUX = 0.9f;
    private static final float TARGET_HIGH_HARDFLUX = 0.7f;
    private static final float TARGET_OVERLOAD_REMAINING_MIN = 2.5f;

    private static final int TARGET_FLUX_SCORE = 2;
    private static final int TARGET_OVERLOAD_SCORE = 3;
    private static final int TARGET_RETREAT_SCORE = 2;
    private static final int TARGET_SYS_SCORE = 2;
    private static final int TARGET_SELF_SCORE = 3;
    
    private static final float SYSTEM_DISTANCE = 500f;
    private static final float SEARCH_RANGE_DEST = 3000f;

    private float aiThinkTime = THINK_INTERVAL_NORMAL;
    private int isReckless = 0;
    // reckless shipsystem AI behaviour currently disabled.  normally, ships using it disregard friendly collision & flux checks
    // this was funny but it mostly just made it look like i didn't have proper shipsystem AI at all lol
    // may make a return w/ colony crisis or SiC tree

    private ShipSystemAPI system;
    private ShipAPI ship;
    private FluxTrackerAPI flux;
    
    // a few checks happen here:
    // - collision checks, try not to bump anything into friendlies
    // - search around destination, ensure we don't burn friendlies with any flux or low hull into hostiles at all
    private boolean safeTargetCheck(ShipAPI target) {
    	if (isReckless > 0)
    		return true;
    	float timeMult = target.getMutableStats().getTimeMult().computeMultMod();
    	Vector2f dest = new Vector2f(
			target.getLocation().x + 
			(SYSTEM_DISTANCE * timeMult) * 
			(float) FastTrig.cos(Math.toRadians(target.getFacing())),
			target.getLocation().y + 
			(SYSTEM_DISTANCE * timeMult) * 
			(float) FastTrig.sin(Math.toRadians(target.getFacing()))
		);
    	/*
    	Vector2f halfDest = new Vector2f(
			target.getLocation().x + 
			(target.getCollisionRadius() + SYSTEM_DISTANCE * 0.5f * timeMult) * 
			(float) FastTrig.cos(Math.toRadians(target.getFacing())),
			target.getLocation().y + 
			(target.getCollisionRadius() + SYSTEM_DISTANCE * 0.5f * timeMult) * 
			(float) FastTrig.sin(Math.toRadians(target.getFacing()))
		); */
    
    	// TODO: holy shit you really just need to make some readouts because this is just not working rn.
    	boolean hostileTargetFluxVuln = target.getOwner() != ship.getOwner() &&
    		(target.getFluxTracker() != null && (target.getFluxTracker().isOverloaded() &&
    		(target.getFluxTracker().getOverloadTimeRemaining() > TARGET_OVERLOAD_REMAINING_MIN ||
    		target.getFluxTracker().isVenting() && target.getFluxTracker().getTimeToVent() > TARGET_OVERLOAD_REMAINING_MIN)
    		|| target.getHardFluxLevel() > TARGET_HIGH_HARDFLUX && !target.isCapital() && !target.isCruiser() ||
    		target.getFluxLevel() > TARGET_HIGH_FLUX && !target.isCapital() && !target.isCruiser()) ||
    		target.getHullSpec().getHints().contains(ShipTypeHints.CARRIER) &&
			!target.getHullSpec().getHints().contains(ShipTypeHints.COMBAT));
    	boolean anyAlliesAtDest = false;

    	Iterator<Object> shipIter = Global.getCombatEngine().getShipGrid().
    		getCheckIterator(
    			dest, 
    			SEARCH_RANGE_DEST,
    			SEARCH_RANGE_DEST);
    	while (shipIter.hasNext()) {
    		ShipAPI check = (ShipAPI) shipIter.next();
    		if (check.isFighter() || check.hasTag(Tags.VARIANT_FX_DRONE) || check.equals(target))
    			continue;
    		if (ship.getOwner() == check.getOwner() && ship.getOwner() != target.getOwner() && 
    			check.isAlive() && !check.isHulk()) {
    			boolean vulnAlly =
        			check.getHullLevel() < ALLY_LOW_HULL ||
        			check.getEngineController() != null && check.getEngineController().computeDisabledFraction() > 0f ||
            		check.getFluxLevel() > ALLY_HIGH_FLUX && check.getShield() != null && 
            			!check.getShield().getType().equals(ShieldType.NONE) || 
            		check.getFluxTracker() != null && (check.getFluxTracker().isOverloaded() &&
            			check.getFluxTracker().getOverloadTimeRemaining() > ALLY_OVERLOAD_REMAINING_MIN ||
            			check.getFluxTracker().isVenting() &&
            			check.getFluxTracker().getTimeToVent() > ALLY_OVERLOAD_REMAINING_MIN);
    			if (!hostileTargetFluxVuln && vulnAlly ||
    				// MathUtils.getDistanceSquared(halfDest, check.getLocation()) < 
            		// Math.pow(SYSTEM_DISTANCE / 2f + 
            		// 	target.getShieldRadiusEvenIfNoShield() + check.getShieldRadiusEvenIfNoShield(), 2f)
    				// &&
            		CollisionUtils.getCollides(target.getLocation(), dest, check.getLocation(), 
            			check.getCollisionRadius() + target.getCollisionRadius())
    			)
    			return false;
    			anyAlliesAtDest |= !vulnAlly;
    		}
    		else if (ship.getOwner() == target.getOwner() &&
    			(check.getOwner() != target.getOwner() && check.isAlive() && !check.isHulk() && 
    				MathUtils.getDistanceSquared(dest, check.getLocation()) < ALLY_ENEMY_DEST_RANGE * ALLY_ENEMY_DEST_RANGE &&
    				Misc.getAngleDiff(check.getFacing(), VectorUtils.getAngle(check.getLocation(), dest)) 
    					< TARGET_ANGLE_ENGAGE_THRESHOLD &&
    				(check.getFluxTracker() != null && 
    					!(check.getFluxTracker().isOverloaded() && 
    					check.getFluxTracker().getOverloadTimeRemaining() > TARGET_OVERLOAD_REMAINING_MIN &&
    					check.getFluxLevel() > TARGET_OVERLOAD_FLUX_MIN ||
	    				check.getFluxTracker().isVenting() && 
	    				check.getFluxTracker().getTimeToVent() > TARGET_OVERLOAD_REMAINING_MIN)
    				)
    			) || 
    			// MathUtils.getDistanceSquared(halfDest, check.getLocation()) < 
            	// Math.pow(SYSTEM_DISTANCE / 2f + 
            	// 	target.getShieldRadiusEvenIfNoShield() + check.getShieldRadiusEvenIfNoShield(), 2f)
    			// &&
            	CollisionUtils.getCollides(target.getLocation(), dest, check.getLocation(), 
            		check.getCollisionRadius() + target.getCollisionRadius())
    			)
            	return false;
    		
    	}
    	
    	if (target.getOwner() != ship.getOwner() && !anyAlliesAtDest &&
    		(!target.getHullSpec().getHints().contains(ShipTypeHints.CARRIER) ||
			target.getHullSpec().getHints().contains(ShipTypeHints.COMBAT)))
    		return false;
    	
    	return true;
    	/*
    	Iterator<Object> shipIter = Global.getCombatEngine().getShipGrid().
    		getCheckIterator(
    			new Vector2f(
    				target.getLocation().x + 
    				(target.getCollisionRadius() + SYSTEM_DISTANCE * timeMult / 2f) * 
    				(float) FastTrig.cos(Math.toRadians(target.getFacing())),
    				target.getLocation().y + 
    				(target.getCollisionRadius() + SYSTEM_DISTANCE * timeMult / 2f) * 
    				(float) FastTrig.sin(Math.toRadians(target.getFacing()))
    			), 
    			SYSTEM_DISTANCE * timeMult * (float) FastTrig.cos(Math.toRadians(target.getFacing()) + 
    				target.getShieldRadiusEvenIfNoShield()),
    			SYSTEM_DISTANCE * timeMult * (float) FastTrig.sin(Math.toRadians(target.getFacing())) + 
    				target.getShieldRadiusEvenIfNoShield());
    	while (shipIter.hasNext()) {
    		ShipAPI check = (ShipAPI) shipIter.next();
    		if (check.isFighter() || check.hasTag(Tags.VARIANT_FX_DRONE) || check.equals(target))
    			continue;
    		if (ship.getOwner() == check.getOwner() && check.isAlive() && !check.isHulk() || ship.getOwner() == target.getOwner())
    			return true;
    	}
    	return false;
    	*/
    }

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.system = system;
        this.flux = ship.getFluxTracker();
        
        if (isReckless == -1 && ship.getShipAI() != null && ship.getShipAI().getConfig() != null &&
        	Personalities.RECKLESS.equals(ship.getShipAI().getConfig().personalityOverride))
        	isReckless = 1;
        
        /* this.useDistance = espc_RemoteBurnStats.getMaxRange(ship);
        
        LinkedList<Float> dps = new LinkedList<Float>();
        LinkedList<Float> ranges = new LinkedList<Float>();
        float totalDPS = 0f;
        for (WeaponAPI wep : ship.getAllWeapons()) {
        	if (wep.getType().equals(WeaponType.DECORATIVE) ||
        		wep.getType().equals(WeaponType.SYSTEM) ||	
        		(wep.getType().equals(WeaponType.MISSILE) && wep.getAmmoPerSecond() <= 0f) ||
        		wep.getType().equals(WeaponType.BUILT_IN) ||
        		wep.hasAIHint(AIHints.PD) && !wep.hasAIHint(AIHints.PD_ALSO) ||
        		wep.hasAIHint(AIHints.PD_ONLY))
        		continue;
        	if (!ranges.contains(wep.getRange())) {
        		boolean sorted = false;
        		for (int i = 0; i < ranges.size() && !sorted; i++)
        			if (wep.getRange() < ranges.get(i)) {
        				sorted = true;
        				ranges.add(i, wep.getRange());
        				dps.add(i, wep.getDerivedStats().getSustainedDps());
        				totalDPS += wep.getDerivedStats().getSustainedDps();
        			}
        	} else {
        		dps.set(ranges.indexOf(wep.getRange()), 
        			dps.get(ranges.indexOf(wep.getRange())) + wep.getDerivedStats().getSustainedDps());
        	}
    		totalDPS += wep.getDerivedStats().getSustainedDps();
        }
        float dpsCount = 0f;
        for (int i = 0; i < ranges.size(); i++) {
        	if (ranges.get(i) > RANGE_TARGET_CAP) {
        		targetRange = RANGE_TARGET_CAP;
        		break;
        	}
        	dpsCount += dps.get(i);
        	if (dpsCount / totalDPS > 0.6f) {
        		targetRange = ranges.get(i) * 0.9f;
        		break;
        	}
        }
        */
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
    	
    	if (amount <= 0f || flux.isOverloadedOrVenting() || system.isActive() || system.getAmmo() <= 0)
    		return;
    	
    	if (isReckless == -1 && ship.getShipAI() != null) {
            if (ship.getShipAI().getConfig() != null &&
            	Personalities.RECKLESS.equals(ship.getShipAI().getConfig().personalityOverride))
            	isReckless = 1;
            else
            	isReckless = 0;
    	}
    	
        aiThinkTime -= amount;

        if (aiThinkTime <= 0f) {
        	aiThinkTime = THINK_INTERVAL_NORMAL + Misc.random.nextFloat() * 0.2f;
        	Iterator<Object> targetIter = Global.getCombatEngine().getShipGrid().getCheckIterator(
        		ship.getLocation(), 
        		espc_RemoteBurnStats.getMaxRange(ship) * 1.8f, espc_RemoteBurnStats.getMaxRange(ship) * 1.8f);
        	
        	ShipAPI bestTarget = null;
        	int bestScore = 0;
        	while (targetIter.hasNext()) {
        		int thisScore = 0;
        		boolean vuln = false;
            	
        		ShipAPI targ = (ShipAPI) targetIter.next();
        		if (!targ.isTargetable() ||
        			targ.isFighter() || !targ.isAlive() || targ.isHulk() || targ.hasTag(Tags.VARIANT_FX_DRONE)
        			|| targ.getEngineController() != null && targ.getEngineController().isFlamedOut())
        			continue;
        		
        		if (targ.getOwner() == ship.getOwner() 
        			// reserve last charge for offensive use
        			&& system.getAmmo() > 1) {
        			if (isReckless < 1 && targ.isAlive() && !targ.isHulk() &&
        				(targ.getHullLevel() < ALLY_LOW_HULL ||
        	        	targ.getEngineController() != null && targ.getEngineController().computeDisabledFraction() > 0f ||
        				targ.getHullSpec().getHints().contains(ShipTypeHints.CARRIER) &&
                			!targ.getHullSpec().getHints().contains(ShipTypeHints.COMBAT) ||
        				(targ.getFluxLevel() > ALLY_HIGH_FLUX || targ.getHardFluxLevel() > ALLY_LOW_HARDFLUX)
        				&& targ.getShield() != null && 
        				!targ.getShield().getType().equals(ShieldType.NONE) || 
        				targ.getFluxTracker() != null && (targ.getFluxTracker().isOverloaded() &&
        				targ.getFluxTracker().getOverloadTimeRemaining() > ALLY_OVERLOAD_REMAINING_MIN ||
        				targ.getFluxTracker().isVenting() &&
        				targ.getFluxTracker().getTimeToVent() > ALLY_OVERLOAD_REMAINING_MIN)))
        				continue;
        			if (targ.equals(ship))
        				continue;
        			
        			if (targ.getVelocity().lengthSquared() > 400f * 400f)
						continue;
        			
        			if (targ.isFrigate())
            			thisScore = 1; 
            		else if (targ.isDestroyer())
            			thisScore = 2;
            		else if (targ.isCruiser())
            			thisScore = 3;
            		else if (targ.isCapital())
            			thisScore = 4;
        			
        			if (thisScore > bestScore && targ.getAngularVelocity() <= ALLY_ANGULAR_VELOCITY_THRESHOLD &&
        				targ.getVelocity().lengthSquared() > Math.pow(targ.getMaxSpeed() * ALLY_MAX_SPEED_THRESHOLD, 2f) &&
        				Misc.getAngleDiff(VectorUtils.getAngle(Misc.ZERO, targ.getVelocity()), targ.getFacing()) <
        				ALLY_FORWARDS_VEL_ANGLE_THRESHOLD &&
        				safeTargetCheck(targ)) {
        				bestTarget = targ;
        				bestScore = thisScore;
        			}
    				continue;
        		}
        		
        		thisScore = 1;
        		if (targ.getEngineController() == null || targ.isStation() || targ.isStationModule() || targ.getMaxSpeed() <= 0f
        			|| targ.isPhased())
        			continue;
        		
        		boolean facingOpposite = Misc.getAngleDiff(targ.getFacing() + 180f, ship.getFacing()) < 
        			TARGET_ANGLE_OPPOSITE_THRESHOLD &&
            		Misc.getAngleDiff(targ.getFacing(),
                	VectorUtils.getAngle(Misc.ZERO, 
                	Vector2f.sub(ship.getLocation(), targ.getLocation(), new Vector2f()))) < TARGET_ANGLE_USER_THRESHOLD;
        		
        		if (facingOpposite) {
            		if (targ.getFluxTracker() != null && (targ.getFluxTracker().isOverloaded() &&
            			targ.getFluxTracker().getOverloadTimeRemaining() > TARGET_OVERLOAD_REMAINING_MIN &&
            			targ.getFluxLevel() > TARGET_OVERLOAD_FLUX_MIN
            			|| targ.getFluxTracker().isVenting() &&
                			targ.getFluxTracker().getTimeToVent() > TARGET_OVERLOAD_REMAINING_MIN)
            			|| targ.getHullSpec().getHints().contains(ShipTypeHints.CARRIER) &&
            				!targ.getHullSpec().getHints().contains(ShipTypeHints.COMBAT)
            			) {
            			thisScore += TARGET_OVERLOAD_SCORE;
            			vuln = true;
            		}
            		else if (targ.getShield() != null && !ShieldType.NONE.equals(targ.getShield().getType()) &&
                		targ.getHardFluxLevel() > TARGET_HIGH_HARDFLUX) {
                		thisScore += TARGET_FLUX_SCORE;
            			vuln = true;
            		}
        		}
        		
        		if ((targ.getFluxLevel() > TARGET_LOW_FLUX || targ.isDestroyer() || targ.isFrigate()) &&
        				targ.getVelocity().lengthSquared() > 
        			Math.pow(targ.getMaxSpeed() * TARGET_RETREAT_MAX_SPEED_THRESHOLD, 2f) &&
        			(Misc.getAngleDiff(VectorUtils.getAngle(Misc.ZERO, targ.getVelocity()), targ.getFacing() - 180f) < 
        			TARGET_RETREAT_ANGLE_THRESHOLD ||
        			facingOpposite && VectorUtils.getAngle(Vector2f.sub(targ.getLocation(), ship.getLocation(), new Vector2f()), 
        			targ.getVelocity()) < TARGET_RETREAT_ANGLE_THRESHOLD)) {
    				thisScore += TARGET_RETREAT_SCORE;
        			if (targ.getSystem() != null && targ.getSystem().getEffectLevel() > 0.5f) {
            			String systemAI = "";
            			try {
        					systemAI = targ.getSystem().getSpecAPI().getSpecJson().getString("aiType");
        				} catch (JSONException e) {
        					systemAI = "null";
        				}
            			if (systemAI.equals("MANEUVERING_JETS")) {
            				thisScore += TARGET_SYS_SCORE;
                			vuln = true;
            			}
            		}
        		}
        		
        		if (facingOpposite && targ.equals(ship.getShipTarget()) && thisScore > 1) {
            		thisScore += TARGET_SELF_SCORE;
        		}
        		
        		if (thisScore > 1 && thisScore > bestScore && safeTargetCheck(targ) && (!targ.isCapital() ||
        			vuln)) {
        			bestTarget = targ;
        			bestScore = thisScore;
        		}
        	}
        	if (bestTarget != null) {
        		((espc_RemoteBurnStats) ship.getSystem().getScript()).setTarget(bestTarget);
            	ship.useSystem();
            	aiThinkTime = THINK_INTERVAL_USED;
        	}
        }
    }
}
