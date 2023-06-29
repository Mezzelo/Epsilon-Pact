package data.scripts.weapons;

import java.awt.Color;
import java.util.Iterator;
import java.util.LinkedList;

import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
// import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.OnHitEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponEffectPluginWithInit;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
// import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.combat.ShipAPI;

public class espc_AMFlamerEffect implements OnFireEffectPlugin, OnHitEffectPlugin, EveryFrameWeaponEffectPlugin, WeaponEffectPluginWithInit {

	private static final float DECAY_PER_SHOT = 0.0005f;
	private static final float HULLMOD_DECAY_MULT = 0.5f;
	
	private static final float BURST_HALT_FLUX_THRESHOLD = 0.95f;
	private static final float BURST_HALT_TIME_THRESHOLD = 0.1f;
	private static final float BURST_HALT_COOLDOWN_FLUX = 3f;
	private static final float BURST_HALT_COOLDOWN_TIME = 1f;
	
	private static final float SHOT_DELAY_MAX = 0.075f;
	private static final float SHOT_DISTANCE_MAX = 15f;
	
	private static final float PROXIMITY_DAMAGE = 150f;
	private static final float PROXIMITY_RADIUS = 120f;
	private static final float PROXIMITY_EXPLOSION_INTERVAL = 0.5f;
	
	// 0.97^20 = 54% chance of no malfunction.  .975 = 60%, .98 = 67%
	// 0.97^30 = 40% chance of no malfunction  .975 = 47%, .98 = 55%
	private static final float AMMO_MALFUNCTION_THRESHOLD = 0.5f;
	private float MALFUNCTION_CHANCE_BASE = 0.02f;
	private static final float MALFUNCTION_EXPLOSION_CHANCE = 0.25f;
	
	private ShipAPI ship;
	private float finalDecay = DECAY_PER_SHOT;
	private float delayTime = -10f;
	private float explosionLast = 0f;
	private float lastFiredTime = 0f;
	
	private boolean isSolo = false;
	// private float SHOT_DISTANCE_MAX;
	
	private LinkedList<DamagingProjectileAPI> projs;

	@Override
	public void init(WeaponAPI weapon) {
		ship = weapon.getShip();
		projs = new LinkedList<DamagingProjectileAPI>();
		
		isSolo = weapon.getSpec().getWeaponId().equals("espc_amflamersolo");
		if (!isSolo)
			isSolo = ship.getSystem() == null;
		if (ship.getVariant().hasHullMod("auxiliary_fuel_tanks")) {
			finalDecay *= HULLMOD_DECAY_MULT;
			MALFUNCTION_CHANCE_BASE *= 0.5f;
		}
	}
	
    @Override
    public void onFire(DamagingProjectileAPI proj, WeaponAPI weapon, CombatEngineAPI engine) {

    	float systemLevel = !isSolo ? ship.getSystem().getEffectLevel() : 0f;
		if (systemLevel > 0f) {
			spurt(engine, weapon.getFirePoint(0), weapon.getCurrAngle(), 400f, 5, 95f, 2.3f);
			/* no way to clone projectile spec as far as i'm aware of u_u
			proj.getProjectileSpec().setFringeColor(
				new Color(255 - (int)(160f * systemLevel),
					25 + (int)(60f * systemLevel),
					10 + (int)(240f * systemLevel),
					100)
			);
			proj.getProjectileSpec().setCoreColor(
					new Color(255,
						175 + (int)(75f * systemLevel),
						50 + (int)(190f * systemLevel),
						100)
				);
			*/
		} else {
			spurt(engine, weapon.getFirePoint(0), weapon.getCurrAngle(), 250f, 3, 75f, 1.5f);
		}
		Global.getSoundPlayer().playLoop(
				"espc_amflamer_loop",
				weapon,
				1.0f,
				0.7f,
				weapon.getLocation(),
				weapon.getShip().getVelocity()
		);
    	
    	if (!isSolo) {
    		ship.setCurrentCR(ship.getCurrentCR() - finalDecay);
    		if (ship.getCurrentCR() <= 0f)
    			weapon.disable(true);
    	} else if (weapon.getAmmo() < weapon.getMaxAmmo() * AMMO_MALFUNCTION_THRESHOLD) {
			if (Misc.random.nextFloat() < MALFUNCTION_CHANCE_BASE) {
				weapon.disable(false);
				spurt(engine, weapon.getFirePoint(0), weapon.getCurrAngle(), 300f, 35, 30f, 2f);
				/*
				Global.getSoundPlayer().playSound(
					"espc_amflamer_burst", 
					1.1f,
					0.7f,
					weapon.getLocation(),
					ship.getVelocity()
				);*/
				if (Misc.random.nextFloat() < MALFUNCTION_EXPLOSION_CHANCE) {
					DamagingExplosionSpec explosion = new DamagingExplosionSpec(
							0.5f, // duration
							150f,
							50f, // core radius, max -> min falloff
							350f,
							150f,
							CollisionClass.HITS_SHIPS_AND_ASTEROIDS,
							CollisionClass.HITS_SHIPS_AND_ASTEROIDS,
							7f, // min particle size
							4f, // particle size range
							2.0f, // particle duration
							12, // particleCount
							new Color(255,130,25,100), // particleColor
							new Color(255,100,15,100)  // explosionColor
					);
					
					explosion.setDamageType(DamageType.HIGH_EXPLOSIVE);
					explosion.setUseDetailedExplosion(true);
					explosion.setSoundSetId("explosion_from_damage");
					engine.spawnDamagingExplosion(
						explosion, 
						ship, weapon.getFirePoint(0),
						true
					);
				}
			}
		}
		if (ship.getFluxTracker().getCurrFlux()/ship.getFluxTracker().getMaxFlux() > BURST_HALT_FLUX_THRESHOLD &&
				delayTime <= 0f) {
			delayTime = BURST_HALT_COOLDOWN_FLUX;
			/*
			Global.getSoundPlayer().playSound(
				"espc_amflamer_burst", 
				1.1f,
				0.7f,
				weapon.getLocation(),
				ship.getVelocity()
			);*/
			spurt(engine, weapon.getFirePoint(0), weapon.getCurrAngle(), 300f, 30, 30f, 2f);
			weapon.setRemainingCooldownTo(weapon.getCooldown());
		}
		
		if (projs.size() > 0) {
			DamagingProjectileAPI lastProj = (DamagingProjectileAPI) projs.getLast();
			if (lastProj.getElapsed() < SHOT_DELAY_MAX &&
				MathUtils.getDistanceSquared(proj.getLocation(), lastProj.getLocation()) > SHOT_DISTANCE_MAX * SHOT_DISTANCE_MAX) {
				Vector2f diff = new Vector2f();
				Vector2f.sub(lastProj.getLocation(), proj.getLocation(), diff);
				diff.normalise();
				float diffAng = Vector2f.angle(
						new Vector2f(
							(float) FastTrig.cos(Math.toRadians(lastProj.getFacing())), 
							(float) FastTrig.sin(Math.toRadians(lastProj.getFacing()))
						), 
						diff
					);
				if (diffAng > 1.3f)
					diffAng = MathUtils.FPI - diffAng;
    			// Global.getLogger(espc_AMFlamerEffect.class).info(diffAng);
				diff.scale(SHOT_DISTANCE_MAX 
					+ 10f
					* (1f - diffAng)/1f
				);
				Vector2f.sub(lastProj.getLocation(), diff, proj.getLocation());
			}
		} else if (lastFiredTime <= 0f) {
			Global.getSoundPlayer().playSound(
				"espc_amflamer_burst", 
				1.0f,
				0.8f,
				weapon.getLocation(),
				ship.getVelocity()
			);
		}

		Vector2f muzzleVel = new Vector2f(
			(float) FastTrig.cos(Math.toRadians(proj.getFacing())),
			(float) FastTrig.sin(Math.toRadians(proj.getFacing()))
		);
		
		muzzleVel.scale(weapon.getProjectileSpeed() * 0.8f);
        engine.addNebulaParticle(
        	weapon.getFirePoint(0),
        	Vector2f.add(muzzleVel, ship.getVelocity(), new Vector2f()),
        	12f, 
        	25f + 10f * systemLevel, 0f, 0f, 1.5f,
        	new Color(255, 25, 10 + (int)(30f * systemLevel), 80 + (int)(40f * systemLevel))
        );
        if (!isSolo) {
    		muzzleVel.scale(0.7f + systemLevel * 0.2f);
            engine.addNebulaParticle(
               	weapon.getFirePoint(0),
               	Vector2f.add(muzzleVel, ship.getVelocity(), new Vector2f()),
               	12f, 
               	12f + 12f * systemLevel, 0f, 0f, 0.5f + systemLevel * 0.15f,
               	new Color(85, 25, 255, 180)
            );	
        }
		projs.addLast(proj);
    	lastFiredTime = 0.15f;
    }

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
    	if (delayTime > 0f) {
    		delayTime = Math.max(delayTime - amount, 0f);
			weapon.setRemainingCooldownTo(weapon.getCooldown() * delayTime / BURST_HALT_COOLDOWN_FLUX);
    	}
    	
    	lastFiredTime = Math.max(0f, lastFiredTime - amount);
    	if (lastFiredTime > 0f) {
    		if (!isSolo && ship.getSystem() != null && ship.getSystem().isActive()) {
    			Global.getSoundPlayer().playLoop(
    				"espc_amflamer_loop",
    				weapon,
    				0.8f,
    				1.1f,
    				weapon.getLocation(),
   					weapon.getShip().getVelocity()
    			);
    		} else {
    			Global.getSoundPlayer().playLoop(
    					"espc_amflamer_loop",
    					weapon,
    					1.0f,
    					0.7f,
    					weapon.getLocation(),
    					weapon.getShip().getVelocity()
    			);
    		}
    	}
		
		if (projs.size() > 0) {
			explosionLast += amount;
			// float damMult = (explosionLast > PROXIMITY_EXPLOSION_INTERVAL) ? 
			// 	ship.getMutableStats().getEnergyWeaponDamageMult().getMult() : 0f;
			
			Iterator<DamagingProjectileAPI> projIterator = projs.descendingIterator();
			DamagingProjectileAPI lastProj = null;
			float elapsedLast = 0f;
			Vector2f explosionLastPos = new Vector2f();
			while (projIterator.hasNext()) {
				if (lastProj == null) {
					lastProj = (DamagingProjectileAPI) projIterator.next();
					if (lastProj == null || lastProj.isExpired() || lastProj.didDamage() ||
						!Global.getCombatEngine().isEntityInPlay(lastProj)) {
						projIterator.remove();
						lastProj = null;
					} else {
						elapsedLast = lastProj.getElapsed();
						if (explosionLast > PROXIMITY_EXPLOSION_INTERVAL) {
							explosionLastPos = lastProj.getLocation();
							engine.spawnDamagingExplosion(
								proxExplosion(1f), 
								ship, explosionLastPos
							);
						}
					}
					continue;
				}
				DamagingProjectileAPI proj = (DamagingProjectileAPI) projIterator.next();
				if (proj == null || proj.isExpired() || proj.didDamage() ||
					!Global.getCombatEngine().isEntityInPlay(proj)) {
					projIterator.remove();
				} else {
					if (proj.getElapsed() < 0.33f) {
						// Vector2f velocity = proj.getVelocity();
						Vector2f.add(new Vector2f(), weapon.getFirePoint(0), proj.getTailEnd());
						// proj.setFacing(facing);
						// proj.setFacing(VectorUtils.getAngle(proj.getLocation(), lastProj.getLocation()) * 0.06f + proj.getFacing() * 0.94f);
						// Vector2f.add(velocity, new Vector2f(), proj.getVelocity());
					}
					if (proj.getElapsed() - lastProj.getElapsed() < SHOT_DELAY_MAX &&
						MathUtils.getDistanceSquared(proj.getLocation(), lastProj.getLocation()) > SHOT_DISTANCE_MAX * SHOT_DISTANCE_MAX) {
						float facingOrig = proj.getFacing();
						Vector2f diff = new Vector2f();
						Vector2f.sub(lastProj.getLocation(), proj.getLocation(), diff);
						diff.normalise();
						float diffAng = Vector2f.angle(
							new Vector2f(
								(float) FastTrig.cos(Math.toRadians(lastProj.getFacing())), 
								(float) FastTrig.sin(Math.toRadians(lastProj.getFacing()))
							), 
							diff
						);
						if (diffAng > 1.3f)
							diffAng = MathUtils.FPI - diffAng;
		    			// Global.getLogger(espc_AMFlamerEffect.class).info(diffAng);
						diff.scale(SHOT_DISTANCE_MAX 
							+ 10f
							* (1f - diffAng)/1f
						);
						Vector2f.sub(lastProj.getLocation(), diff, proj.getLocation());
						proj.setFacing(facingOrig);
					}
					
					if (explosionLast > PROXIMITY_EXPLOSION_INTERVAL &&
						MathUtils.getDistanceSquared(explosionLastPos, proj.getLocation()) > PROXIMITY_RADIUS * PROXIMITY_RADIUS) {
						explosionLastPos = proj.getLocation();
						engine.spawnDamagingExplosion(
							proxExplosion(1f), 
							ship, proj.getLocation()
						);
					}
					
					lastProj = proj;
				}
			}
			
			explosionLast = explosionLast % PROXIMITY_EXPLOSION_INTERVAL;
			
			if (elapsedLast > BURST_HALT_TIME_THRESHOLD && elapsedLast < BURST_HALT_TIME_THRESHOLD * 2f && delayTime <= 0f) {
				delayTime = BURST_HALT_COOLDOWN_TIME;
				/*
				Global.getSoundPlayer().playSound(
					"espc_amflamer_burst", 
					1.1f,
					0.7f,
					weapon.getLocation(),
					ship.getVelocity()
				);*/
				spurt(engine, weapon.getFirePoint(0), weapon.getCurrAngle(), 300f, 10, 30f, 2f);
				weapon.setRemainingCooldownTo(weapon.getCooldown());
			}
			
		} else {
			explosionLast = 0f;
		}
    }
    
    private void spurt(CombatEngineAPI engine, Vector2f loc, float ang, float speed, int count, float spread, float duration) {
    	int particles = Misc.random.nextInt(count/2) + count/2;
    	
    	for (int i = 0; i < particles; i++) {
    		// fuck ayn rand btw lmao
    		float angRand = (float) Math.toRadians((ang + Misc.random.nextFloat() * spread - spread/2f) % 360f);
    		Vector2f vel = new Vector2f(
    			(float) FastTrig.cos(angRand),
    			(float) FastTrig.sin(angRand)
    		);
    		vel.scale(Misc.random.nextFloat() * speed/2f + speed/2f);
    		Vector2f.add(ship.getVelocity(), vel, vel);
    		engine.addHitParticle(
    			loc,
    			vel,
    			Misc.random.nextFloat() * 3f + 5f,
    			Misc.random.nextFloat() * 1f + 4f,
    			Misc.random.nextFloat() * duration/2f + duration/2f,
    			new Color(255, Misc.random.nextInt(100) + 70, 10, Misc.random.nextInt(100) + 150)
    		);
    	}
    }
    
	public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
		Color color = projectile.getProjectileSpec().getFringeColor();
		
		Vector2f vel = new Vector2f();
		if (target instanceof ShipAPI) {
			vel.set(target.getVelocity());
		}
		
		engine.addNebulaParticle(
			point,
			vel,
			60f,
			4.5f,
			0f,
			0f,
			0.7f,
			Misc.scaleAlpha(color, projectile.getBrightness())
		);
		
		// most of this is my own code, but this *is* just ripped from cryoflamer lol.  i'm so tired, need to release this.
		/*
		Misc.playSound(damageResult, point, vel,
				"cryoflamer_hit_shield_light",
				"cryoflamer_hit_shield_solid",
				"cryoflamer_hit_shield_heavy",
				"cryoflamer_hit_light",
				"cryoflamer_hit_solid",
				"cryoflamer_hit_heavy");
		*/
	}
    
    private DamagingExplosionSpec proxExplosion(float damageMult) {
		DamagingExplosionSpec explosion = new DamagingExplosionSpec(
				0.5f, // duration
				PROXIMITY_RADIUS,
				PROXIMITY_RADIUS * 0.8f, // core radius, max -> min falloff
				PROXIMITY_DAMAGE * damageMult * (1f + 
					ship.getMutableStats().getEnergyWeaponDamageMult().getFlatMod() +
					ship.getMutableStats().getEnergyWeaponDamageMult().getPercentMod() / 100f)
					* ship.getMutableStats().getEnergyWeaponDamageMult().getMult(),
				PROXIMITY_DAMAGE * 0.3f * damageMult * (1f + 
					ship.getMutableStats().getEnergyWeaponDamageMult().getFlatMod() +
					ship.getMutableStats().getEnergyWeaponDamageMult().getPercentMod() / 100f)
					* ship.getMutableStats().getEnergyWeaponDamageMult().getMult(),
				CollisionClass.PROJECTILE_NO_FF,
				CollisionClass.PROJECTILE_FIGHTER,
				0f, // min particle size
				0f, // particle size range
				0f, // particle duration
				0, // particleCount
				new Color(0,0,255,0), // particleColor
				new Color(0,0,0,0)  // explosionColor
		);
		
		explosion.setDamageType(DamageType.ENERGY);
		explosion.setUseDetailedExplosion(false);
		explosion.setShowGraphic(false);
		return explosion;
    }
}