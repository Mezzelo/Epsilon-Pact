package data.scripts.shipsystems.ai;

import com.fs.starfarer.api.combat.*;
// import com.fs.starfarer.api.combat.ShipSystemAPI.SystemState;
// import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;
// import com.fs.starfarer.api.plugins.ShipSystemStatsScript.State;
// import com.fs.starfarer.api.util.IntervalUtil;
// import org.lazywizard.lazylib.combat.AIUtils;
// import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

public class espc_FeintAI implements ShipSystemAIScript {
    
    // private static final float THINK_INTERVAL_NORMAL = 0.1f;
	private static final float FLUX_THRESHOLD = 0.8f;

    // private IntervalUtil aiInterval = new IntervalUtil(THINK_INTERVAL_NORMAL, THINK_INTERVAL_NORMAL);

    private ShipSystemAPI system;
    private ShipAPI ship;
    private FluxTrackerAPI flux;

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.flux = ship.getFluxTracker();
        this.system = system;
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {

        // aiInterval.advance(amount);

        // if (aiInterval.intervalElapsed()) {
        	if (amount > 0f && !system.isCoolingDown() && system.getAmmo() > 0 && !flux.isOverloadedOrVenting() &&
        		flux.getFluxLevel() > FLUX_THRESHOLD
            )
            	ship.useSystem();
        // }
    }
}
