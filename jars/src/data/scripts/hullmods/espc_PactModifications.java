package data.scripts.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAIConfig;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;

import data.scripts.util.MezzUtils;

public class espc_PactModifications extends BaseHullMod {

    private static final float THINK_INTERVAL = 0.2f;
	private static final float AI_HULL_BACK = 0.15f;
	private static final float AI_FLUX_BACK = 0.4f;
	private static final float AI_FLUX_CRITICAL = 0.7f;

	public static class espc_PactRemnantAIScript implements AdvanceableListener {
		public ShipAPI ship;
		private ShipAIConfig origConfig = null;
		private int aiStateLast = 0;
		private float aiThinkTime = THINK_INTERVAL;
		
		public espc_PactRemnantAIScript(ShipAPI ship) {
			this.ship = ship;
	    	
	    	if (ship.getShipAI() != null && ship.getShipAI().getConfig() != null)
	    		if (Personalities.TIMID.equals(ship.getShipAI().getConfig().personalityOverride) || 
	    			Personalities.CAUTIOUS.equals(ship.getShipAI().getConfig().personalityOverride)) {
	    			ship.removeListener(this);
	    			return;
	    		} else {
					ShipAIConfig config = ship.getShipAI().getConfig();
					origConfig = config.clone();
					aiStateLast = 0;
				}
		}

		public void advance(float amount) {

	    	if (ship == null || ship.isHulk() || !ship.isAlive() || amount <= 0f)
	    		return;
	    	
	    	if (ship.getAI() == null) {
	    		aiStateLast = -1;
	    		return;
	    	} else {
	    		aiThinkTime -= amount;
	    		if (aiThinkTime <= 0f)
	    			aiThinkTime = THINK_INTERVAL;
	    		else
	    			return;
	    	}
	    	
	    	if (origConfig == null && ship.getShipAI().getConfig() != null) {
				ShipAIConfig config = ship.getShipAI().getConfig();
				origConfig = config.clone();
				aiStateLast = 0;
			}
	    	
			if (origConfig != null) {
				ShipAIConfig config = ship.getShipAI().getConfig();
				if (config != null) {
					if (aiStateLast != 1 && (ship.getFluxLevel() < AI_FLUX_BACK &&
						ship.getHullLevel() > AI_HULL_BACK || 
						ship.getFluxLevel() < AI_FLUX_CRITICAL &&
						ship.getSystem().isActive())) {
						config.personalityOverride = Personalities.RECKLESS;
						config.alwaysStrafeOffensively = true;
						/*
						if (ship.getFluxLevel() < AI_FLUX_FORCE_ENGAGE)
							config.backingOffWhileNotVentingAllowed = false;
						else
							config.backingOffWhileNotVentingAllowed = true; */
						config.turnToFaceWithUndamagedArmor = false;
						config.burnDriveIgnoreEnemies = true;
						config.backingOffWhileNotVentingAllowed = false;
						// if (aiStateLast != -1)
						// 	ship.getShipAI().forceCircumstanceEvaluation();
						if (ship.getSystem().isActive())
							ship.getShipAI().forceCircumstanceEvaluation();
						aiStateLast = 1;
					} else if (aiStateLast != 0) {
						config.copyFrom(origConfig);
						if (aiStateLast != -1)
							ship.getShipAI().forceCircumstanceEvaluation();
						aiStateLast = 0;
					}
				}
			}
		}
	}
    
	@Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		if (ship.getHullSpec().getBaseHullId().equals("radiant"))
			ship.addListener(new espc_PactRemnantAIScript(ship));
    }

	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0)
			return MezzUtils.getString("espc_hullmod", "pactmods1");
		return null;
	}
	
}