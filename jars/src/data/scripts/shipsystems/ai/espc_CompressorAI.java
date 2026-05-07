package data.scripts.shipsystems.ai;

// import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
// import com.fs.starfarer.api.combat.ShipSystemAPI.SystemState;
// import com.fs.starfarer.api.plugins.ShipSystemStatsScript.State;
import com.fs.starfarer.api.util.IntervalUtil;


// import org.lazywizard.lazylib.MathUtils;
// import org.lazywizard.lazylib.combat.AIUtils;
// import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

public class espc_CompressorAI implements ShipSystemAIScript {
    
    private static final float THINK_INTERVAL_NORMAL = 0.35f;
    private static final float THINK_INTERVAL_TOGGLE = 1.5f;

    private IntervalUtil aiInterval = new IntervalUtil(THINK_INTERVAL_NORMAL, THINK_INTERVAL_NORMAL + 0.05f);
    private ShipAPI ship;
    private FluxTrackerAPI flux;

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.flux = ship.getFluxTracker();
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
    	if (amount <= 0f)
    		return;
    	
    	if (flux.isOverloadedOrVenting())
    		return;
    	
        aiInterval.advance(amount);

        if (aiInterval.intervalElapsed()) {

    		aiInterval.setInterval(THINK_INTERVAL_TOGGLE, THINK_INTERVAL_TOGGLE + 0.05f);
    		ship.useSystem();
        }
    }
}
