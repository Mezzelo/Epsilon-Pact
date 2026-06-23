package data.scripts.shipsystems.ai;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;

import data.scripts.shipsystems.espc_ResonatorShieldStats;
import data.scripts.util.MezzUtils;

import org.lwjgl.util.vector.Vector2f;

public class espc_ResonatorShieldAI implements ShipSystemAIScript {

    private static final float HF_THRESHOLD = 0.6f;
    private static final float HF_DISSIPATE_OFF_THRESHOLD = 0.15f;
    
    private static final float HF_BURST_PADDING = 1.15f;
    
	private static final float AI_HULL_BACK = 0.3f;
	private static final float AI_FLUX_BACK = 0.35f;
	// private static final float AI_FLUX_FORCE_ENGAGE = 0.2f;
    
    private static final float FLUX_DISENGAGE_THRESHOLD = 0.1f;
    private static final float FLUX_EMERGENCY_THRESHOLD = 0.9f;
    
    private static final float THINK_INTERVAL_NORMAL = 0.2f;
    // private static final float THINK_INTERVAL_USED = 1f;

	private ShipAIConfig origConfig = null;
	
	private int aiStateLast = 0;
    private float aiThinkTime = THINK_INTERVAL_NORMAL;

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
    	
        aiThinkTime -= amount;

        if (aiThinkTime <= 0f) {
            aiThinkTime = THINK_INTERVAL_NORMAL;

    		if (aiStateLast == -1) {
    			
    		} else if (origConfig == null) {
        		if (ship.getShipAI().getConfig() != null) {
    				if ((!MezzUtils.getSetting("CustomShipAI") && ship.getOwner() == 0 && !ship.isAlly()) 
    					|| Personalities.TIMID.equals(ship.getShipAI().getConfig().personalityOverride) || 
    					Personalities.CAUTIOUS.equals(ship.getShipAI().getConfig().personalityOverride)) {
    					aiStateLast = -1;
    				} else {
    					ShipAIConfig config = ship.getShipAI().getConfig();
    					origConfig = config.clone();
    				}
    			}
        	} else {
        		ShipAIConfig config = ship.getShipAI().getConfig();
        		if (config != null) {
        			if (aiStateLast != 1 && ship.getFluxLevel() < AI_FLUX_BACK &&
        				ship.getHullLevel() > AI_HULL_BACK) {
        				config.personalityOverride = Personalities.RECKLESS;
        				config.alwaysStrafeOffensively = true;
        					/*
        				if (ship.getFluxLevel() < AI_FLUX_FORCE_ENGAGE)
        					config.backingOffWhileNotVentingAllowed = false;
        				else
        					config.backingOffWhileNotVentingAllowed = true; */
        				config.turnToFaceWithUndamagedArmor = false;
        				config.burnDriveIgnoreEnemies = true;
        				// ship.getShipAI().forceCircumstanceEvaluation();
        				aiStateLast = 1;
        			} else if (aiStateLast != 0 && (ship.getFluxLevel() >= AI_FLUX_BACK ||
    					ship.getHullLevel() <= AI_HULL_BACK)){
        				config.copyFrom(origConfig);
        				ship.getShipAI().forceCircumstanceEvaluation();
        				aiStateLast = 0;
        			}
        		}
        	}
        	
        	if (toggled) {
        		toggled = false;
        		aiThinkTime = THINK_INTERVAL_NORMAL;
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
        }
    }
}
