package data.scripts.weapons;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.WeaponEffectPluginWithInit;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.OnHitEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.util.Misc;

import data.scripts.weapons.proj.espc_IonAccumulatorRenderPlugin;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.Iterator;

import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

public class espc_IonAccumulatorEffect implements OnFireEffectPlugin, EveryFrameWeaponEffectPlugin, WeaponEffectPluginWithInit,
	OnHitEffectPlugin{
	
	private ShipAPI ship;
	private final static int ACCUMULATION_MAX_MEDIUM = 20;
	private final static int ACCUMULATION_MAX_SMALL = 10;
	private final static float FIRE_DELAY_SMALL = 0.4f;
	private final static float FIRE_DELAY_MED = 0.4f;
	private final static float PD_MULT = 0.5f;
	private final static float MED_PD_RANGE = 1f;
	private final static float SMALL_PD_RANGE = 1f;
	private final static float ARC_PORTION = 0.25f;

	private final static float EMP_MED = 400f;
	private final static float EMP_SMALL = 300f;
	
	
	private int wepSize = 0;
	
	float chargeLast = 0f;
	float chargeTime = 0f;
	boolean firedThisFrame = false;
	
	float storedDamage = 0f;
	float storedEMP = 0f;
	
	float fireDelay = 0f;
	
	int accumulationCount = 0;
	
	DamagingProjectileAPI lastProj = null;

	ArrayDeque<AccumulatedProj> projs;
	espc_IonAccumulatorRenderPlugin plugin;
	
	public class AccumulatedProj {
		private ShipAPI target;
		private float damage;
		private float empDamage;
		private Vector2f hitLoc;
		public AccumulatedProj(ShipAPI target, float damage, float empDamage, Vector2f hitLoc) {
			this.target = target;
			this.damage = damage;
			this.empDamage = empDamage;
			this.hitLoc = hitLoc;
		}
	}
    
	public CombatEntityAPI findTarget(WeaponAPI weapon, CombatEngineAPI engine) {

		// TODO: go over this code, specifically score calculation
		// it should prioritize PD threats, but still target enemy ships
		float range = weapon.getRange() * (wepSize == 1 ? MED_PD_RANGE : SMALL_PD_RANGE);
		Vector2f from = weapon.getFirePoint(0);
		
		Iterator<Object> iter = Global.getCombatEngine().getAllObjectGrid().getCheckIterator(
			from, range * 2f, range * 2f);
		int owner = weapon.getShip().getOwner();
		CombatEntityAPI best = null;
		float minScore = Float.MAX_VALUE;
		
		ShipAPI ship = weapon.getShip();
		
		while (iter.hasNext()) {
			Object o = iter.next();
			if (!(o instanceof MissileAPI || o instanceof ShipAPI)) continue;
			CombatEntityAPI other = (CombatEntityAPI) o;
			if (other.getOwner() == owner) continue;
			
			if (other instanceof ShipAPI) {
				ShipAPI otherShip = (ShipAPI) other;
				if (otherShip.isHulk()) continue;
				if (!otherShip.isFighter()) continue;
				// if (!otherShip.isAlive()) continue;
				if (otherShip.isPhased()) continue;
				if (!otherShip.isTargetable()) continue;
			}
			
			if (other.getCollisionClass() == CollisionClass.NONE) continue;
			
			if (other instanceof MissileAPI && ((MissileAPI) other).isFlare())
				continue;

			float radius = Misc.getTargetingRadius(from, other, false);
			float dist = Misc.getDistance(from, other.getLocation()) - radius;
			if (dist > range) continue;
			
			if (!Misc.isInArc(ship.getFacing() + weapon.getSlot().getAngle(), 
				weapon.getArc(), from, other.getLocation())) continue;
			
			//float angleTo = Misc.getAngleInDegrees(from, other.getLocation());
			//float score = Misc.getAngleDiff(weapon.getCurrAngle(), angleTo);
			float score = dist;
			// if (other instanceof ShipAPI && !((ShipAPI) other).isFighter())
			// 	score *= 2f;
			
			if (score < minScore) {
				minScore = score;
				best = other;
			}
		}
		return best;
	}
	
	@Override
	public void init(WeaponAPI weapon) {
		ship = weapon.getShip();
		if (ship == null)
			return;
		
		plugin = new espc_IonAccumulatorRenderPlugin(this);
		Global.getCombatEngine().addPlugin(plugin);
		
		projs = new ArrayDeque<AccumulatedProj>();
		if (weapon.getSize().equals(WeaponSize.MEDIUM))
			wepSize = 1;
		
	}

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (Global.getCurrentState() != GameState.COMBAT ||
        	weapon == null || engine.isPaused() || amount <= 0f) return;
        
        for (AccumulatedProj proj : projs)
        	if (proj.target != null && engine.isInPlay(proj.target) &&
        		proj.target.getFluxTracker() != null && proj.target.getFluxTracker().isOverloaded()) {
        		while (proj.empDamage > 0f) {
        			engine.spawnEmpArcPierceShields(ship, 
            			proj.hitLoc, 
            			proj.target, 
            			proj.target,
            			DamageType.ENERGY, 
            			proj.damage,
            			Math.min(proj.empDamage, wepSize == 1 ? EMP_MED : EMP_SMALL),
            			100000f, // max range 
            			"shock_repeater_emp_impact",
            			20f, // thickness
            			new Color(25,100,155,255),
            			new Color(255,255,255,255)
            		);
        			proj.damage = 0f;
        			proj.empDamage -= (wepSize == 1 ? EMP_MED : EMP_SMALL);
        		}
        	}
        projs.clear();
        
        if (fireDelay > 0f)
        	fireDelay -= amount;
        else
        	return;
        if (fireDelay <= 0f) {
    		// TODO: fire vfx/sfx
			DamagingProjectileAPI proj = (DamagingProjectileAPI) engine.spawnProjectile(
				ship,
				weapon,
				weapon.getId(),
				weapon.getFirePoint(0),
				//new Vector2f(proj.getLocation().x + (Misc.random.nextFloat() * 10f - 5f), proj.getLocation().y + (Misc.random.nextFloat() * 10f - 3f)),
				// 2x regular max weapon spread, plus some more on top.
				weapon.getCurrAngle(),
				ship.getVelocity()
			);
    		proj.setHitpoints(Math.min(Math.max(proj.getHitpoints(), storedDamage), 1000f));
    		proj.setDamageAmount(storedDamage);
    		storedEMP -= proj.getEmpAmount();
    		proj.setCustomData("espc_ion_accumulator_emp", storedEMP);
    		storedDamage = 0f;
    		storedEMP = 0f;
    		accumulationCount = 0;
        }
    }

    
    @Override
    public void onFire(DamagingProjectileAPI proj, WeaponAPI weapon, CombatEngineAPI engine) {
    	if (MathUtils.getDistanceSquared(proj, weapon.getFirePoint(0)) > 100f) {
    		plugin.addProj(proj);
    		return;
    	}
    	if (ship.getMutableStats().getEnergyRoFMult().getModifiedValue() *
    		ship.getMutableStats().getTimeMult().getModifiedValue() > 1f &&
    		accumulationCount < (wepSize == 1 ? ACCUMULATION_MAX_MEDIUM : ACCUMULATION_MAX_SMALL) - 1
    		) {
    		CombatEntityAPI target = findTarget(weapon, engine);
    		if (target != null)
    			engine.spawnEmpArc(ship, 
    				weapon.getFirePoint(0), 
    				ship, 
    				target,
    				DamageType.ENERGY, 
    				proj.getDamageAmount() * PD_MULT,
    				proj.getEmpAmount() * PD_MULT,
    				100000f, // max range 
    				"shock_repeater_emp_impact",
    				5f, // thickness
    				new Color(25,100,155,255),
    				new Color(255,255,255,255)
    			);
    		storedDamage += proj.getDamageAmount() * (target != null ? (1f - PD_MULT) : 1f);
    		storedEMP += proj.getEmpAmount() * (target != null ? (1f - PD_MULT) : 1f);
    		accumulationCount++;
    		engine.removeEntity(proj);
    		fireDelay = (wepSize == 1 ? FIRE_DELAY_MED : FIRE_DELAY_SMALL);
    	} else {
    		plugin.addProj(proj);
    		proj.setDamageAmount(proj.getBaseDamageAmount() + storedDamage);
    		proj.setCustomData("espc_ion_accumulator_emp", storedEMP);
    		proj.setHitpoints(Math.min(Math.max(proj.getHitpoints(), storedDamage), 1000f));
    		storedDamage = 0f;
    		storedEMP = 0f;
    		accumulationCount = 0;
    		fireDelay = 0f;
    	}
    }

	@Override
	public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit,
		ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
		if (!(target instanceof ShipAPI))
			return;
		if (!shieldHit) {
			engine.spawnEmpArc(projectile.getSource(), point, target, target,
				DamageType.ENERGY, 
				projectile.getDamageAmount(),
				projectile.getEmpAmount(), // emp 
				100000f, // max range 
				"tachyon_lance_emp_impact",
				20f, // thickness
				new Color(25,100,155,255),
				new Color(255,255,255,255)
			);
			if (projectile.getCustomData().containsKey("espc_ion_accumulator_emp") && !shieldHit) {
				float empDamage = (Float) projectile.getCustomData().get("espc_ion_accumulator_emp");
				empDamage *= ARC_PORTION;
				boolean first = true;
				while (empDamage > 0f && projectile.getEmpAmount() > 0f) {
					engine.spawnEmpArc(projectile.getSource(), point, target, target,
						DamageType.ENERGY, 
						first ? (projectile.getDamageAmount() * ARC_PORTION) : 0f,
						Math.min(projectile.getEmpAmount() * ARC_PORTION, empDamage), // emp 
						100000f, // max range 
						"tachyon_lance_emp_impact",
						20f, // thickness
						new Color(25,100,155,255),
						new Color(255,255,255,255)
					);
					empDamage -= projectile.getEmpAmount();
					first = false;
				}
			}
		} else if (shieldHit && projectile.getWeapon() != null) {
			float empDamage = 0f;
			if (projectile.getCustomData().containsKey("espc_ion_accumulator_emp"))
				empDamage = (Float) projectile.getCustomData().get("espc_ion_accumulator_emp");
			((espc_IonAccumulatorEffect) projectile.getWeapon().getEffectPlugin()).projs.add(
				new AccumulatedProj((ShipAPI) target, projectile.getDamageAmount() * ARC_PORTION, empDamage * ARC_PORTION, point));
		}
	}
}