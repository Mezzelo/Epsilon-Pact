package data.scripts.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAIConfig;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.WeaponBaseRangeModifier;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;

import data.scripts.util.MezzUtils;

import java.util.List;


public class espc_ChoraleModBehaviour extends BaseHullMod {
	
    private static final float RANGE_BONUS_MAX = 800f;
    private static final float SPEED_PENALTY_RANGE_MIN = 1000f;
	
	private static final float RANGE_BONUS = 200f;
	private static final float RANGE_BONUS_LARGE = 100f;
	private static final float SPEED_PENALTY = 2f;
	private static final float RECOIL_MULT = 0.5f;
	
	private static final float AI_HULL_BACK = 0.3f;
	private static final float AI_FLUX_BACK = 0.25f;
	// private static final float AI_FLUX_FORCE_ENGAGE = 0.05f;
	
    private static final float THINK_INTERVAL = 0.1f;
	
	
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		// the AI REALLY wants the larges to be turrets in order to orient the ship properly
		// ends up being a pretty massive downside to large ballistics
		// the recoil modifications here effectively give them hardpoint recoil
		stats.getRecoilPerShotMultSmallWeaponsOnly().modifyMult(id, 1f / RECOIL_MULT);
		stats.getRecoilPerShotMult().modifyMult(id, RECOIL_MULT);
		stats.getMaxRecoilMult().modifyMult(id, RECOIL_MULT);
	}
	
	public static class espc_ChoraleAIScript implements AdvanceableListener {
		public ShipAPI ship;
		private ShipAIConfig origConfig = null;
		private int aiStateLast = 0;
		private float aiThinkTime = THINK_INTERVAL;
		
		public espc_ChoraleAIScript(ShipAPI ship) {
			this.ship = ship;
	    	
	    	if (ship.getShipAI() != null && ship.getShipAI().getConfig() != null)
	    		if ((!MezzUtils.getSetting("CustomShipAI") && ship.getOwner() == 0 && !ship.isAlly()) ||
	    			ship.getShipAI().getConfig().personalityOverride.equals(Personalities.TIMID) || 
					ship.getShipAI().getConfig().personalityOverride.equals(Personalities.CAUTIOUS)) {
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
					if (aiStateLast != 1 && ship.getFluxLevel() < AI_FLUX_BACK &&
						ship.getHullLevel() > AI_HULL_BACK &&
						ship.getMutableStats().getShieldDamageTakenMult().computeMultMod() < 1.25f) {
						config.personalityOverride = Personalities.RECKLESS;
						config.alwaysStrafeOffensively = true;
						/*
						if (ship.getFluxLevel() < AI_FLUX_FORCE_ENGAGE)
							config.backingOffWhileNotVentingAllowed = false;
						else
							config.backingOffWhileNotVentingAllowed = true; */
						config.turnToFaceWithUndamagedArmor = false;
						config.burnDriveIgnoreEnemies = true;
						// if (aiStateLast != -1)
						// 	ship.getShipAI().forceCircumstanceEvaluation();
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
		List<WeaponAPI> shipWeps = ship.getAllWeapons();
		
		int speedPenaltyWeps = 0;
        for (int i = 0; i < shipWeps.size(); i++){
			if (((WeaponAPI) shipWeps.get(i)).getSize() == WeaponSize.SMALL)
				continue;
			if (((WeaponAPI) shipWeps.get(i)).getSpec().getMaxRange() >= SPEED_PENALTY_RANGE_MIN)
				speedPenaltyWeps++;
        }
		ship.getMutableStats().getMaxSpeed().modifyFlat(id, speedPenaltyWeps * -SPEED_PENALTY);
        ship.addListener(new espc_ChoraleRangeMod());  
        ship.addListener(new espc_ChoraleAIScript(ship));  
    }
    
    public String getDescriptionParam(int index, HullSize hullSize) {
        if (index == 0)
            return ("" + (int) RANGE_BONUS_MAX);
        else if (index == 1)
            return ("" + (int) RANGE_BONUS);
        else if (index == 2)
            return ("" + (int) RANGE_BONUS_LARGE);
        else if (index == 3)
            return ("" + (int) RANGE_BONUS_MAX);
        else if (index == 4)
            return ("" + (int) SPEED_PENALTY);
        else if (index == 5)
            return ("" + (int) SPEED_PENALTY_RANGE_MIN);

        return null;
    }
	
	public static class espc_ChoraleRangeMod implements WeaponBaseRangeModifier {
        public float getWeaponBaseRangeFlatMod(ShipAPI ship, WeaponAPI weapon) {
			if (weapon.getSize() == WeaponSize.SMALL ||
				weapon.getSpec().getAIHints().contains(WeaponAPI.AIHints.PD) ||
				// guess this is an edge case lol
				weapon.getSpec().getMaxRange() <= 0f)
				return 0f;
            return Math.max(
				Math.min(weapon.getSize() == WeaponSize.MEDIUM ? RANGE_BONUS : RANGE_BONUS_LARGE, 
					RANGE_BONUS_MAX - weapon.getSpec().getMaxRange()),
				0f);
        }
		
        public float getWeaponBaseRangePercentMod(ShipAPI shipAPI, WeaponAPI weaponAPI) {
            return 0;
        }

        public float getWeaponBaseRangeMultMod(ShipAPI shipAPI, WeaponAPI weaponAPI) {
            return 1;
        }
    }
	
}