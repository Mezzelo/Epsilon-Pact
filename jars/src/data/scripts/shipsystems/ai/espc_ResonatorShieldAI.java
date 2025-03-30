package data.scripts.shipsystems.ai;

// import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
// import com.fs.starfarer.api.combat.ShipSystemAPI.SystemState;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;
// import com.fs.starfarer.api.plugins.ShipSystemStatsScript.State;
import com.fs.starfarer.api.util.IntervalUtil;

import data.scripts.shipsystems.espc_ResonatorShieldStats;

// import org.lazywizard.lazylib.MathUtils;
// import org.lazywizard.lazylib.combat.AIUtils;
// import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

public class espc_ResonatorShieldAI implements ShipSystemAIScript {

    private static final float HF_THRESHOLD = 0.6f;
    private static final float HF_DISSIPATE_OFF_THRESHOLD = 0.15f;
    
    private static final float HF_BURST_PADDING = 1.15f;
    
    private static final float FLUX_DISENGAGE_THRESHOLD = 0.1f;
    private static final float FLUX_EMERGENCY_THRESHOLD = 0.9f;
    
    private static final float THINK_INTERVAL_NORMAL = 0.35f;
    private static final float THINK_INTERVAL_TOGGLE = 1.5f;

    private IntervalUtil aiInterval = new IntervalUtil(THINK_INTERVAL_NORMAL, THINK_INTERVAL_NORMAL + 0.05f);

    private espc_ResonatorShieldStats systemScript = null;
    private ShipSystemAPI system;
    private ShipAPI ship;
    private FluxTrackerAPI flux;
    private ShipwideAIFlags flags;
    
    private float hfBurstMax = 0f;
    private boolean toggled = false;
    private boolean noShield = false;

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.flags = flags;
        this.system = system;
        this.flux = ship.getFluxTracker();
        if (ship.getShield() == null)
        	noShield = true;
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
    	if (amount <= 0f || noShield)
    		return;
    	
    	if (systemScript == null) {
    		if (ship.getCustomData().containsKey("espc_resonatorShieldRef")) {
    			systemScript = (espc_ResonatorShieldStats) ship.getCustomData().get("espc_resonatorShieldRef");
    		} else if (!system.isActive()){
            	ship.useSystem();
            	return;
    		}
    	}
    	if (flux.isOverloadedOrVenting())
    		return;
    	
        aiInterval.advance(amount);

        if (aiInterval.intervalElapsed()) {
        	if (toggled) {
        		toggled = false;
        		aiInterval.setInterval(THINK_INTERVAL_NORMAL, THINK_INTERVAL_NORMAL + 0.05f);
        	}
            float useWeight = 0f;
            
            // keep track of the highest burst of damage we've received since engaging the system.
            // if it ever exceeds our remaining hard flux space we have left, we want to turn off system whenever possible.
            
            if (flux.getFluxLevel() < flux.getMaxFlux() * FLUX_DISENGAGE_THRESHOLD) {
            	hfBurstMax = 0f;
        		flags.unsetFlag(AIFlags.BACK_OFF);
            }
            else
            	hfBurstMax = Math.max(systemScript.getHfToDissipate(), hfBurstMax);
            
            if (ship.getShield().isOn() ||
            		systemScript.getHfToDissipate() > 0f) {
            	useWeight += 1f;
            } else {
            	useWeight = 0f;
        	}
            
            if (useWeight > 0f) {
            	// if we ever see ourselves of being in risk of overloading due to system use, disengage
            	// this is determined by either the highest burst damage received since engaging, or on reaching a soft threshold.
            	// only whenever we've dissipated most of the excess.
            	if ((flux.getMaxFlux() - (flux.getHardFlux() - systemScript.getHfToDissipate()) < hfBurstMax * HF_BURST_PADDING ||
            			flux.getHardFlux() - systemScript.getHfToDissipate() > flux.getMaxFlux() * HF_THRESHOLD)) {
                		flags.setFlag(AIFlags.BACK_OFF);
            		if (systemScript.getHfToDissipate() < flux.getMaxFlux() * HF_DISSIPATE_OFF_THRESHOLD) {
                		useWeight -= 5f;
            		}
            	}
            }
            // Global.getLogger(espc_ResonatorShieldAI.class).info("weight: " + useWeight);
            	
            // at this point, just override the AI flag and throw shields down entirely instead of waiting for it to dissipate.
            // better than overloading, though this is tuned quite aggressively.
            if (flux.getHardFlux() > flux.getMaxFlux() * FLUX_EMERGENCY_THRESHOLD) {
            	useWeight -= 10f;
        		flags.setFlag(AIFlags.BACK_OFF);
            }
            
            if (useWeight > 0f) {
            	if (!system.isOn()) {
                	ship.getShield().toggleOn();
                	ship.useSystem();
            	}
            	flags.setFlag(AIFlags.KEEP_SHIELDS_ON);
            } else if (system.isOn() && useWeight <= 0f) {
            	if (useWeight < 0f) {
                	flags.unsetFlag(AIFlags.KEEP_SHIELDS_ON);
                	// if (useWeight < 0f)
                		ship.useSystem();
                    if (useWeight < -7.5f) {
                    	ship.getShield().toggleOff();
                    }
                }
            }
            
            if (toggled)
        		aiInterval.setInterval(THINK_INTERVAL_TOGGLE, THINK_INTERVAL_TOGGLE + 0.05f);
        }
    }
}
