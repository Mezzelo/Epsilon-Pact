package data.scripts.weapons;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.OnHitEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponEffectPluginWithInit;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI.ShipEngineAPI;

import java.util.ArrayDeque;
import java.util.Iterator;

import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

public class espc_ArcFlakEffect implements OnFireEffectPlugin, WeaponEffectPluginWithInit, EveryFrameWeaponEffectPlugin,
	OnHitEffectPlugin{
	
	private static final float EXPLOSION_RADIUS = 50f;
	private static final float EXPLOSION_DAMAGE = 20f;
	private static final float EMP_DAMAGE = 50f;
	private static final float DAMAGE_DIRECT = 30f;
	private static final float EMP_DAMAGE_DIRECT = 50f;
	
	public ArrayDeque<ArcFlakProj> shots;
	
	private class ArcFlakProj {
		DamagingProjectileAPI projectile;
		float ang;
		float distSquared;
		float lifetime = 0f;
		Vector2f lastPos;
		public ArcFlakProj(DamagingProjectileAPI proj, float range) {
			this.projectile = proj;
			this.ang = Misc.random.nextFloat() * 100f - 50f;
			this.distSquared = (float) Math.pow(range * (1f - Misc.random.nextFloat() * 0.6f - 0.1f), 2f);
			this.lastPos = proj.getLocation();
		}
	}

	@Override
	public void init(WeaponAPI weapon) {
		shots = new ArrayDeque<ArcFlakProj>();
	}

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (Global.getCurrentState() != GameState.COMBAT ||
        	weapon == null || engine.isPaused() || amount <= 0f) return;
        Iterator<ArcFlakProj> iter = shots.iterator();
        while (iter.hasNext()) {
        	ArcFlakProj proj = iter.next();
        	if (!engine.isInPlay(proj.projectile) && proj.lifetime > 0.05f) {
        		Iterator<Object> empIter = Global.getCombatEngine().getAllObjectGrid().getCheckIterator(
        			proj.lastPos, EXPLOSION_RADIUS * 2f, EXPLOSION_RADIUS * 2f);
        		while (empIter.hasNext()) {
        			Object o = empIter.next();
	        		if (o instanceof MissileAPI) {
	        			if (((MissileAPI) o).getOwner() != weapon.getShip().getOwner() &&
	        				MathUtils.getDistanceSquared(proj.lastPos, ((MissileAPI) o).getLocation()) <
		        			EXPLOSION_RADIUS * EXPLOSION_RADIUS * 1.5f * 1.5f)
		        			engine.applyDamage(
		        				(CombatEntityAPI) o, 
		        				((CombatEntityAPI) o).getLocation(), EXPLOSION_DAMAGE * 2f, DamageType.KINETIC, 
		        				EMP_DAMAGE * 2f, false, false, weapon);
	        		} else if (o instanceof ShipAPI) {
        				ShipAPI targ = (ShipAPI) o;
        				if (targ.isPhased())
        					continue;
        				if (MathUtils.getDistance(proj.lastPos, targ.getLocation()) >
	        				EXPLOSION_RADIUS + targ.getCollisionRadius())
        					continue;
        				boolean wasHit = false;
        				for (WeaponAPI wep : targ.getAllWeapons())
        					if (MathUtils.getDistanceSquared(wep.getLocation(), proj.lastPos) <
        						EXPLOSION_RADIUS * EXPLOSION_RADIUS && (!wasHit || !wep.isDisabled())) {
        	        			engine.applyDamage(
            		       			targ, 
            		       			wep.getLocation(), 
            		       			EXPLOSION_DAMAGE * (wasHit ? 0f : 1f)
            		       			, DamageType.KINETIC, 
            		       			EMP_DAMAGE, false, false, weapon);
        	        			wasHit = true;
        					}
        				for (ShipEngineAPI shipEngine : targ.getEngineController().getShipEngines()) {
        					if (MathUtils.getDistanceSquared(shipEngine.getLocation(), proj.lastPos) <
            					EXPLOSION_RADIUS * EXPLOSION_RADIUS && (!wasHit || !shipEngine.isDisabled())) {
        						
            	       			engine.applyDamage(
            		       			targ, 
            		       			shipEngine.getLocation(), 
            		       			EXPLOSION_DAMAGE * (wasHit ? 0f : 1f), 
            		       			DamageType.KINETIC, 
            		       			EMP_DAMAGE, false, false, weapon);
            	       			wasHit = true;
        					}
        				}
        				if (!wasHit) {
        					Vector2f adj = Vector2f.sub(targ.getLocation(), proj.lastPos, new Vector2f());
        					adj.normalise();
        					adj.scale(targ.getShieldRadiusEvenIfNoShield() * 0.85f);
        	        		engine.applyDamage(
        	        			targ, 
        		       			Vector2f.sub(targ.getLocation(), adj, new Vector2f()), 
        		       			EXPLOSION_DAMAGE, 
        	        			DamageType.KINETIC, 
       		        			EMP_DAMAGE, false, false, weapon);
       					}
       					
       				}
	       		}
	       		iter.remove();
	       	}
        	proj.lastPos = proj.projectile.getLocation();
        	proj.lifetime += amount;
        	
        	if (proj.distSquared <= 0f || 
        		MathUtils.getDistanceSquared(proj.projectile.getLocation(), proj.projectile.getSpawnLocation()) > 
        		proj.distSquared) {
        		if (proj.distSquared > 0f)
        			proj.distSquared = -1f;
        		float magnitude = Math.max(proj.projectile.getVelocity().length() - 1000f * amount * 2f, 0f);
        		if (magnitude <= 250f) {
        			continue;
        		}
        		proj.projectile.setFacing(proj.projectile.getFacing() + proj.ang * amount * 3.5f);
        		Vector2f.add(proj.projectile.getVelocity(), new Vector2f(
        			-proj.projectile.getVelocity().x +
        				magnitude * (float) FastTrig.cos(Math.toRadians(proj.projectile.getFacing())),
        			-proj.projectile.getVelocity().y +
    					magnitude * (float) FastTrig.sin(Math.toRadians(proj.projectile.getFacing()))
        			), proj.projectile.getVelocity());
        	}
        }
		
    }
    
    @Override
    public void onFire(DamagingProjectileAPI proj, WeaponAPI weapon, CombatEngineAPI engine) {
    	proj.getVelocity().scale(Misc.random.nextFloat() * 0.2f + 0.9f);
    	shots.add(new ArcFlakProj(proj, weapon.getRange()));
    }

	@Override
	public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit,
		ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
		if (projectile.getWeapon() == null)
			return;
		if (target instanceof MissileAPI || (target instanceof ShipAPI) && ((ShipAPI) target).isFighter())
			engine.applyDamage(
    		(CombatEntityAPI) target, 
    		point, target instanceof MissileAPI ? DAMAGE_DIRECT : 0f, DamageType.KINETIC, 
    		EMP_DAMAGE_DIRECT + (target instanceof MissileAPI ? 20f + EMP_DAMAGE_DIRECT : 0f), 
    		false, false, projectile.getWeapon());
		espc_ArcFlakEffect plugin = (espc_ArcFlakEffect) projectile.getWeapon().getEffectPlugin();
        Iterator<ArcFlakProj> iter = plugin.shots.iterator();
        while (iter.hasNext()) {
        	ArcFlakProj proj = iter.next();
        	if (proj.projectile.equals(projectile)) {
        		iter.remove();
        		return;
        	}
        }
	}
}