package data.scripts.shipsystems.ai;

import com.fs.starfarer.api.Global;
// import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI.ShipTypeHints;
// import com.fs.starfarer.api.combat.ShipSystemAPI.SystemState;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;
import com.fs.starfarer.api.combat.WeaponAPI.AIHints;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
// import com.fs.starfarer.api.plugins.ShipSystemStatsScript.State;
import com.fs.starfarer.api.util.IntervalUtil;

import data.scripts.shipsystems.espc_InverseSkimmerStats;

import java.util.HashMap;
import java.util.Iterator;

import org.json.JSONException;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
// import org.lazywizard.lazylib.MathUtils;
// import org.lazywizard.lazylib.combat.AIUtils;
// import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

public class espc_InverseSkimmerAI implements ShipSystemAIScript {

    private static final float ALLY_FLUX_THRESHOLD = 0.7f;
    private static final float ALLY_HARDFLUX_THRESHOLD = 0.4f;
    
    private static final float FLUX_THRESHOLD_DANGER = 0.9f;
    private static final float HARDFLUX_THRESHOLD_DANGER = 0.8f;
    
    private static final float RANGE_MULT = 0.9f;
    
    private static final float TARGET_DISTANCE_MAX = 1750f;
    private static final float TARGET_DISTANCE_MAX_CAPITAL = 2250f;
    
    private static final float THINK_INTERVAL_NORMAL = 2.5f;
    // private static final float THINK_INTERVAL_ACTIVE = 0.25f;

    private IntervalUtil aiInterval = new IntervalUtil(THINK_INTERVAL_NORMAL, THINK_INTERVAL_NORMAL + 0.05f);

    private espc_InverseSkimmerStats systemScript = null;
    private ShipAPI ship;
    private boolean isCapital;
    // private boolean systemActive = false;
    private ShipAPI ally = null;
    private HashMap<String, Float> ranges;

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.systemScript = (espc_InverseSkimmerStats) ship.getSystem().getScript();
        this.isCapital = ship.getHullSize().equals(HullSize.CAPITAL_SHIP);
        this.ranges = new HashMap<String, Float>();
        // this.flags = flags;
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
    	if (amount <= 0f)
    		return;
    	
    	if (ship.getFluxTracker().isOverloadedOrVenting())
    		return;
    	
    	if (ship.getSystem().isCoolingDown())
    		return;

    	if (ship.getSystem().isActive())
    		return;
    	
    	if (ship.getCustomData().containsKey("espc_InverseSkimmer_Ally"))
    		return;
    	
        aiInterval.advance(amount);

        if (aiInterval.intervalElapsed()) {
    		aiInterval.setInterval(THINK_INTERVAL_NORMAL, THINK_INTERVAL_NORMAL + 0.05f);
    		ShipAPI targ = ship.getShipTarget();
    		if (targ != null)
	    		if (ship.getOwner() == targ.getOwner() || targ.isFighter() ||
	    			targ.isHulk() || !targ.isAlive() ||
	    			MathUtils.getShortestRotation(VectorUtils.getAngle(ship.getLocation(), targ.getLocation()),
	        			ship.getFacing()) > 30f ||
	        		!MathUtils.isWithinRange(
	        			targ, ship.getLocation(), 
	        	  	ship.getMutableStats().getSystemRangeBonus().computeEffective(
	        	  		isCapital ? TARGET_DISTANCE_MAX_CAPITAL : TARGET_DISTANCE_MAX
	        	  	)))
	    			targ = null;
    		
    		if (targ == null) {
				Object test = ship.getAIFlags().getCustom(AIFlags.MANEUVER_TARGET);
				if (test instanceof ShipAPI) {
					targ = (ShipAPI) test;
		    		if (ship.getOwner() == targ.getOwner() && targ.isFighter() ||
		    			targ.isHulk() || !targ.isAlive() ||
		    			MathUtils.getShortestRotation(VectorUtils.getAngle(ship.getLocation(), targ.getLocation()),
		    	        	ship.getFacing()) > 30f ||
		            	!MathUtils.isWithinRange(
		            		targ, ship.getLocation(), 
		             	ship.getMutableStats().getSystemRangeBonus().computeEffective(
		             		isCapital ? TARGET_DISTANCE_MAX_CAPITAL : TARGET_DISTANCE_MAX
		             	)))
		        		targ = null;
				}
    		}
    		
    		// TODO: shorter thinking interval after using system for non-capital, should raycast for autofire
    		// for capital: if not in emergency state, don't use if captured allied hullsize points < 50% of enemy
    		
    		if (targ == null || targ.isPhased())
    			return;
    		
    		boolean danger = ship.getFluxLevel() > FLUX_THRESHOLD_DANGER ||
    			ship.getHardFluxLevel() > HARDFLUX_THRESHOLD_DANGER;
    		
    		float alliedScore = 0f;
    		float scoreTotal = 0f;
    		CombatEngineAPI combatEngine = Global.getCombatEngine();
    		
    		Iterator<Object> shipGridIterator = (Iterator<Object>) (combatEngine.getAiGridShips().getCheckIterator(
    			ship.getLocation(),
    			ship.getMutableStats().getSystemRangeBonus().computeEffective(
    				isCapital ? TARGET_DISTANCE_MAX_CAPITAL : TARGET_DISTANCE_MAX
    			) * 2f,
    			ship.getMutableStats().getSystemRangeBonus().computeEffective(
    				isCapital ? TARGET_DISTANCE_MAX_CAPITAL : TARGET_DISTANCE_MAX
    			) * 2f)
    		);
    		
    		while (shipGridIterator.hasNext()) {
    			ShipAPI currShip = (ShipAPI) shipGridIterator.next();
    			if (currShip.getFleetMember() != null && currShip.getFleetMember().isCivilian())
    				continue;
    			
    			// collision checking: if a ship is in-between there's a good chance it'll be within range,
    			// but this shouldn't be too expensive anyway and we should check for edge cases
    			if (ranges.containsKey(currShip.getId()) &&
    				ranges.get(currShip.getId()) < 0f)
    				continue;
    			
    			if (currShip.isShuttlePod() || currShip == ship || currShip.hasTag(Tags.VARIANT_FX_DRONE)
    				|| currShip.getOwner() != ship.getOwner()
    				|| currShip.isFighter()
    				|| currShip.getMutableStats().getHullDamageTakenMult().getMult() <= 0f
    				|| currShip.isHulk() || !currShip.isAlive() || currShip.getMaxFlux() <= 0f
    				|| !MathUtils.isWithinRange(currShip, ship.getLocation(), 
    					ship.getMutableStats().getSystemRangeBonus().computeEffective(
    					isCapital ? TARGET_DISTANCE_MAX_CAPITAL : TARGET_DISTANCE_MAX
    				))
    				|| currShip.isPhased()
    				
    			)
    				continue;
    			
    			if (!currShip.getHullSize().equals(HullSize.FRIGATE) && CollisionUtils.getCollides(
					ship.getLocation(), 
					targ.getLocation(),
					currShip.getLocation(),
					currShip.getShieldRadiusEvenIfNoShield() * 0.9f
					)) {
    				ally = null;
    				return;
    			}
    			
    			if (currShip.getFluxTracker().isOverloadedOrVenting()
        			|| ((currShip.getFluxLevel() > ALLY_FLUX_THRESHOLD
        			|| currShip.getHardFluxLevel() > ALLY_HARDFLUX_THRESHOLD) &&
        				!danger)
        			|| currShip.getCustomData().containsKey("espc_InverseSkimmer_Ally")) {
    				continue;
    			}
    			
    			if (!ranges.containsKey(currShip.getId())) {
    				float range = -1f;
    				for (WeaponAPI wep : currShip.getAllWeapons()) {
    					if (wep.isDecorative() || wep.isBeam() || wep.getType().equals(WeaponType.MISSILE) || 
    						wep.getSlot().isSystemSlot() ||
    						(wep.hasAIHint(AIHints.PD) && !wep.hasAIHint(AIHints.PD_ALSO)))
    						continue;
    					range = Math.max(range, wep.getRange());
    				}
    				ranges.put(currShip.getId(), range);
    				if (range < 0f)
    					continue;
    			}
    			if (ranges.get(currShip.getId()) * RANGE_MULT > 
    				MathUtils.getDistance(currShip, targ) ||
    				ranges.get(currShip.getId()) * RANGE_MULT < MathUtils.getDistance(ship, targ))
    				continue;
    			float score = 1f;
    			boolean hasTarget = currShip.getShipTarget() != null && !danger;
    			if (hasTarget && currShip.getShipAI() == null)
    				hasTarget = false;
    			if (hasTarget && (currShip.getShipTarget().isFighter() ||
    				currShip.getShipTarget().getOwner() == currShip.getOwner()))
    				hasTarget = false;
    			if (hasTarget && ranges.get(currShip.getId()) * RANGE_MULT > 
    				MathUtils.getDistance(currShip, currShip.getShipTarget()) &&
    				MathUtils.getShortestRotation(VectorUtils.getAngle(currShip.getLocation(), currShip.getShipTarget().getLocation()),
    					currShip.getFacing()) < 30f)
    				continue;
    			if (currShip.getShipAI() != null && !danger) {
        			Object test = currShip.getAIFlags().getCustom(AIFlags.MANEUVER_TARGET);
    				if (test instanceof ShipAPI) {
    					ShipAPI currTarg = (ShipAPI) test;
    					if (!currTarg.isFighter() &&
    						currTarg.getOwner() != currShip.getOwner() &&
    						ranges.get(currShip.getId()) * RANGE_MULT > 
        					MathUtils.getDistance(currShip, currTarg) &&
        					MathUtils.getShortestRotation(VectorUtils.getAngle(
        						currShip.getLocation(), currTarg.getLocation()),
        						currShip.getFacing()) < 30f)
    						continue;
    				}
    			}
    			if (currShip.getHullSize().equals(HullSize.CAPITAL_SHIP))
    				score = 4f;
    			else if (currShip.getHullSize().equals(HullSize.CRUISER))
       				score = 3f;
    			else if (currShip.getHullSize().equals(HullSize.DESTROYER))
       				score = 2f;
    			if (currShip.getSystem() != null) {
    				String systemAI = "";
    				try {
						systemAI = currShip.getSystem().getSpecAPI().getSpecJson().getString("aiType");
					} catch (JSONException e) {
						systemAI = "null";
						}
    			if (systemAI.equals("WEAPON_BOOST") && !currShip.getSystem().getDisplayName().equals("Shieldwall"))
    				score += 1.5f;
    			}
    			if (currShip.getHullSpec().getHints().contains(ShipTypeHints.CARRIER) && !
   					currShip.getHullSpec().getHints().contains(ShipTypeHints.COMBAT))
   					score = Math.max(0.1f, score - 2.1f);
   				scoreTotal += alliedScore;
   				if (score > alliedScore) {
   					alliedScore = score;
   					ally = currShip;
   				}
   			}
   			
   			if (ally != null) {
   				if (!isCapital)
   					systemScript.setAlliedTarget(ally);
   				else if (isCapital && targ.getHullSize().equals(HullSize.CAPITAL_SHIP) &&
   					scoreTotal <= 1f)
   					return;
   				systemScript.setTarget(targ);
   				ship.useSystem();
   				if (!isCapital)
   					aiInterval.setInterval(THINK_INTERVAL_NORMAL * 0.1f, THINK_INTERVAL_NORMAL * 0.1f + 0.05f);
   			}
			ally = null;
   		}
   	}
}
