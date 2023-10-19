package data.scripts.shipsystems.ai;

import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAIScript;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;
import com.fs.starfarer.api.util.IntervalUtil;

import data.scripts.shipsystems.espc_ResonatorShieldStats;

public class espc_AlternatorAI implements ShipSystemAIScript {
    
    private static final float THINK_INTERVAL_NORMAL = 0.35f;

    private IntervalUtil aiInterval = new IntervalUtil(THINK_INTERVAL_NORMAL, THINK_INTERVAL_NORMAL + 0.05f);

    private espc_ResonatorShieldStats systemScript = null;
    private ShipSystemAPI system;
    private ShipAPI ship;
    private FluxTrackerAPI flux;
    private ShipwideAIFlags flags;

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.flags = flags;
        this.system = system;
        this.flux = ship.getFluxTracker();
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
    	if (amount <= 0f)
    		return;
    	
    	if (systemScript == null) {
    		return;
    	}
    	if (flux.isOverloadedOrVenting())
    		return;
    	
        aiInterval.advance(amount);

        //Wait for interval elapse before doing a check
        if (aiInterval.intervalElapsed()) {
        }
    }
}
