package data.scripts.shipsystems.ai;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.ShieldAPI.ShieldType;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAIScript;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;
import com.fs.starfarer.api.combat.WeaponAPI.AIHints;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.util.IntervalUtil;

import data.scripts.shipsystems.espc_AlternatorStats;

public class espc_AlternatorAI implements ShipSystemAIScript {
    
    private static final float THINK_INTERVAL_NORMAL = 0.35f;

    private IntervalUtil aiInterval = new IntervalUtil(THINK_INTERVAL_NORMAL, THINK_INTERVAL_NORMAL + 0.05f);

    private espc_AlternatorStats systemScript = null;
    private ShipSystemAPI system;
    private ShipAPI ship;
    private FluxTrackerAPI flux;
    private ShipwideAIFlags flags;
    
    private boolean hitStrengthIsBallistic = false;
    private boolean pdGroupBallistic = true;
    private boolean ballisticIsBurst = false;
    private boolean energyIsBurst = false;
    
    private float maxBallisticShieldRange = 0f;
    private float maxEnergyShieldRange = 0f;
    
    private float ballisticHitStrBurstRange = 0f;
    private float energyHitStrBurstRange = 0f;
    
    private List<WeaponAPI> largestBallistic = new ArrayList<WeaponAPI>();
    private List<WeaponAPI> largestEnergy = new ArrayList<WeaponAPI>();

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.flags = flags;
        this.system = system;
        this.flux = ship.getFluxTracker();
        
        float totalBallisticDPS = 0f;
        float totalEnergyDPS = 0f;
        float totalBallisticShieldDPS = 0f;
        float totalEnergyShieldDPS = 0f;
        float totalBallisticPDDPS = 0f;
        float totalEnergyPDDPS = 0f;
        
        float ballisticBurstDamage = 0f;
        float energyBurstDamage = 0f;
        float ballisticHitStr = 0f;
        float energyHitStr = 0f;
        float ballisticHitStrArmor = 0f;
        float energyHitStrArmor = 0f;
        for (WeaponAPI weapon : ship.getAllWeapons()) {
        	if (!weapon.getType().equals(WeaponType.BALLISTIC) || !weapon.getType().equals(WeaponType.ENERGY))
        		continue;
			float effectiveDamage = weapon.getDamage().getBaseDamage();
			float effectiveDamageShield = effectiveDamage;
			if (weapon.getDamage().getType().equals(DamageType.FRAGMENTATION)) {
				effectiveDamage *= 0.25f;
				effectiveDamageShield *= 0.25f;
			} else if (weapon.getDamage().getType().equals(DamageType.KINETIC)) {
				effectiveDamage *= 0.5f;
				effectiveDamageShield *= 2f;
			} else if (weapon.getDamage().getType().equals(DamageType.HIGH_EXPLOSIVE)) {
				effectiveDamage *= 2f;
				effectiveDamageShield *= 0.5f;
			}
			if (weapon.getType() == WeaponType.BALLISTIC) {
				if ((weapon.getSize().equals(WeaponSize.LARGE) || !ship.getHullSize().equals(HullSize.CAPITAL_SHIP)) &&
					!weapon.isPermanentlyDisabled()) {
					largestBallistic.add(weapon);
				}
				totalBallisticDPS += weapon.getDerivedStats().getSustainedDps() *
					(weapon.getDamageType().equals(DamageType.FRAGMENTATION) ? 0.25f : 1f);
				if (weapon.getRange() > ballisticHitStrBurstRange) {
					ballisticHitStrBurstRange = weapon.getRange();
				}
				
				
			}
			else {
				if ((weapon.getSize().equals(WeaponSize.LARGE) || !ship.getHullSize().equals(HullSize.CAPITAL_SHIP)) &&
					!weapon.isPermanentlyDisabled()) {
					largestEnergy.add(weapon);
				}
				totalEnergyDPS += weapon.getDerivedStats().getSustainedDps() *
					(weapon.getDamageType().equals(DamageType.FRAGMENTATION) ? 0.25f : 1f);
				if (weapon.getRange() > energyHitStrBurstRange) {
					energyHitStrBurstRange = weapon.getRange();
				}
			}
        	if (weapon.hasAIHint(AIHints.PD) || weapon.hasAIHint(AIHints.PD_ALSO) || weapon.hasAIHint(AIHints.PD_ONLY)) {
				totalBallisticDPS += weapon.getDerivedStats().getSustainedDps() *
					(weapon.getDamageType().equals(DamageType.FRAGMENTATION) ? 0.25f : 1f);
        	}
			else {
				totalEnergyDPS += weapon.getDerivedStats().getSustainedDps() *
					(weapon.getDamageType().equals(DamageType.FRAGMENTATION) ? 0.25f : 1f);
			}
        	continue;
			// if (weapon.getType() == WeaponType.BALLISTIC)
			// 	ballisticDPS += weapon.getDerivedStats().getSustainedDps();
			// else if (weapon.getType() == WeaponType.ENERGY)
			//	energyDPS += weapon.getDerivedStats().getSustainedDps();
			
		}
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
    	if (amount <= 0f)
    		return;

    	if (systemScript == null) {
    		if (ship.getCustomData().containsKey("espc_alternatorRef")) {
    			systemScript = (espc_AlternatorStats) ship.getCustomData().get("espc_alternatorRef");
    		} else
            	return;
    	}
    	if (flux.isOverloadedOrVenting() || system.isCoolingDown())
    		return;
    	
        aiInterval.advance(amount);

        if (aiInterval.intervalElapsed()) {
        	boolean isEnergy = systemScript.getMode();
        	if (target == null || target.isFighter()) {
        		if (isEnergy && pdGroupBallistic || !isEnergy && !pdGroupBallistic) {
        			ship.useSystem();
        		}
        	} else {
        		boolean targetShielded = target.getShield() != null && !target.getShield().getType().equals(ShieldType.PHASE);
        		if (target.isShipWithModules()) {
        			target.getChildModulesCopy();
        		}
        		if (!targetShielded || targetShielded && target.getShield().isOff()) {
        			if (!isEnergy) {
            			ship.useSystem();
            		}
        		} else {
        			if (isEnergy) {
            			ship.useSystem();
            		}
        		}
        	}
    		aiInterval.setInterval(THINK_INTERVAL_NORMAL, THINK_INTERVAL_NORMAL + 0.05f);
        }
    }
}
