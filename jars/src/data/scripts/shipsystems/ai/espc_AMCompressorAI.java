package data.scripts.shipsystems.ai;

// import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
// import com.fs.starfarer.api.combat.ShipSystemAPI.SystemState;
// import com.fs.starfarer.api.plugins.ShipSystemStatsScript.State;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;

import org.lazywizard.lazylib.MathUtils;
// import org.lazywizard.lazylib.MathUtils;
// import org.lazywizard.lazylib.combat.AIUtils;
// import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

public class espc_AMCompressorAI implements ShipSystemAIScript {
    
    private static final float THINK_INTERVAL_NORMAL = 0.5f;
    private static final float THINK_INTERVAL_USED = 2f;
    private static final float SYSTEM_RANGE_BONUS = 20f;
    private static final float FLUX_THRESHOLD_ACTIVATE = 0.35f;
    private static final float FLUX_THRESHOLD_DEACTIVATE = 0.8f;

    private float aiThinkTime = THINK_INTERVAL_NORMAL;
    private ShipAPI ship;
    private WeaponAPI wep = null;
    private FluxTrackerAPI flux;

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.flux = ship.getFluxTracker();
        if (ship.getAllWeapons().size() > 0)
        	wep = ship.getAllWeapons().get(0);
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
    	if (amount <= 0f)
    		return;
    	
    	if (flux.isOverloadedOrVenting())
    		return;
    	
    	if (!ship.getSystem().canBeActivated())
    		return;
    	
        aiThinkTime -= amount;

        if (aiThinkTime <= 0f) {
    		ShipAPI targ = ship.getShipTarget();
    		if (targ == null || wep == null || wep.isDisabled() ||
    			targ.isFighter()
    			|| ship.getSystem().isActive() && ship.getFluxLevel() > FLUX_THRESHOLD_DEACTIVATE
    			|| !ship.getSystem().isActive() && ship.getFluxLevel() > FLUX_THRESHOLD_ACTIVATE
    			|| !MathUtils.isWithinRange(ship, targ, wep.getRange() * 
    				(ship.getSystem().isActive() ? 1f : (100f + SYSTEM_RANGE_BONUS)/100f)))
    			targ = null;
    		if (targ == null) {
    			Object test = ship.getAIFlags().getCustom(AIFlags.MANEUVER_TARGET);
    			if (test instanceof ShipAPI)
    				targ = (ShipAPI) test;
    		}
        	if (targ == null || wep == null || wep.isDisabled() ||
           		targ.isFighter()
    			|| ship.getSystem().isActive() && ship.getFluxLevel() > FLUX_THRESHOLD_DEACTIVATE
    			|| !ship.getSystem().isActive() && ship.getFluxLevel() > FLUX_THRESHOLD_ACTIVATE
           		|| !MathUtils.isWithinRange(ship, targ, wep.getRange() * 
           			(ship.getSystem().isActive() ? 1f : (100f + SYSTEM_RANGE_BONUS)/100f)))
           		targ = null;
    		if (ship.getSystem().isActive() && targ == null && !ship.getCustomData().containsKey("espc_InverseSkimmer_Ally")
    			|| !ship.getSystem().isActive() && targ != null) {
        		ship.useSystem();
        		aiThinkTime = THINK_INTERVAL_USED;
    		} else
        		aiThinkTime = THINK_INTERVAL_NORMAL;
        }
    }
}
