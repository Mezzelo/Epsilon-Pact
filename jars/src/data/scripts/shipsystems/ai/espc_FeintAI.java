package data.scripts.shipsystems.ai;

import com.fs.starfarer.api.combat.*;
import org.lwjgl.util.vector.Vector2f;

public class espc_FeintAI implements ShipSystemAIScript {
    
	private static final float FLUX_THRESHOLD = 0.7f;

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
        	if (amount > 0f && !system.isCoolingDown() && system.getAmmo() > 0 && !flux.isOverloadedOrVenting() &&
        		flux.getFluxLevel() > FLUX_THRESHOLD && (ship.getHullLevel() > 0.2f || ship.getShield() == null || (ship.getShield() != null &&
        		ship.getShield().isOff()))
            )
            	ship.useSystem();
    }
}
