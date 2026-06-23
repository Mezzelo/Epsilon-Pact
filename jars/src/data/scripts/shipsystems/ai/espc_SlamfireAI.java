package data.scripts.shipsystems.ai;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.WeaponAPI.AIHints;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.util.Misc;

import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

public class espc_SlamfireAI implements ShipSystemAIScript {
    
    private static final float THINK_INTERVAL_NORMAL = 0.15f;
    private static final float THINK_INTERVAL_USED = 2f;
    
    private static final float ACTIVATION_RANGE_MULT = 1.1f;
    private static final float DEACTIVATION_RANGE_MULT = 1.4f;
    
    private static final float ACTIVATION_FLUX_MAX = 0.4f;
    private static final float DEACTIVATE_FLUX = 0.6f;
    private static final float PD_RANGE_ASSUMPTION = 500f;
    private static final float AI_RANGE_MIN_CUTOFF = 400f;
    private static final float AI_RANGE_MAX_CUTOFF = 800f;

    private ShipSystemAPI system;
    private ShipAPI ship;
    private FluxTrackerAPI flux;
    
    private float useDistance = AI_RANGE_MIN_CUTOFF;
    private float deactivateDistance;
    private float aiThinkTime = THINK_INTERVAL_NORMAL;

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.system = system;
        this.flux = ship.getFluxTracker();
        
        // attempt to detect attack weapon group, based on longest range.
    	boolean maxIsNotPD = false;
        for (WeaponGroupAPI weaponGroup : ship.getWeaponGroupsCopy()) {
        	
        	float rangeTotal = 0f;
        	boolean isNotPD = false;
        	
        	for (WeaponAPI weapon : weaponGroup.getWeaponsCopy()) {
        		if (weapon.getType() == WeaponType.BALLISTIC) {
        			// Global.getLogger(espc_SlamfireAI.class).info("range for " + weapon.getDisplayName() + ": " + weapon.getRange());
            		rangeTotal += weapon.getRange();
            		// we weigh the group higher if it's not PD.
            		if (!weapon.hasAIHint(AIHints.PD)&& weapon.getRange() >= PD_RANGE_ASSUMPTION)
            			isNotPD = true;
        		}
        	}
        	rangeTotal = rangeTotal / weaponGroup.getWeaponsCopy().size();
        	
        	if ((rangeTotal > useDistance && maxIsNotPD == isNotPD) || (!maxIsNotPD && isNotPD)) {
        		useDistance = rangeTotal;
        		maxIsNotPD = isNotPD;
        	}
        }
        
        useDistance = Math.max(useDistance * ACTIVATION_RANGE_MULT, AI_RANGE_MIN_CUTOFF);
        deactivateDistance = Math.max(useDistance * DEACTIVATION_RANGE_MULT, AI_RANGE_MAX_CUTOFF);
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
    	
    	if (amount <= 0f || target == null ||
    		flux.isOverloadedOrVenting() || system.isCoolingDown())
    		return;
    	
    	aiThinkTime -= amount;

        if (aiThinkTime <= 0f) {
        	if (system.isActive() &&
        		(flux.getFluxLevel() > DEACTIVATE_FLUX || target.isFighter() || target.isPhased() ||
        		(MathUtils.getDistanceSquared(ship, target) > Math.pow(deactivateDistance, 2f) && 
        		!ship.getCustomData().containsKey("espc_InverseSkimmer_Ally"))
        		)) {
            	aiThinkTime = THINK_INTERVAL_USED;
        		ship.useSystem();
        	}
        	else if (!system.isActive() && flux.getFluxLevel() < ACTIVATION_FLUX_MAX
        		&& MathUtils.isWithinRange(ship, target, useDistance) && !target.isFighter() && !target.isPhased()) {
            	aiThinkTime = THINK_INTERVAL_USED;
        		ship.useSystem();
        	} else
            	aiThinkTime = THINK_INTERVAL_NORMAL + 0.1f * Misc.random.nextFloat();
        }
    }
}
