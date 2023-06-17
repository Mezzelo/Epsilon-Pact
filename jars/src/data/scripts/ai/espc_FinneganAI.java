package data.scripts.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.GuidedMissileAI;
import com.fs.starfarer.api.combat.MissileAIPlugin;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.util.Misc;

import java.awt.Color;
import java.util.Iterator;

import org.lazywizard.lazylib.combat.CombatUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

public class espc_FinneganAI implements MissileAIPlugin, GuidedMissileAI {
	
	private static final float COLLISION_RADIUS = 16f;
	
	private static final float MIN_VEL = 150f;
	private static final float BASE_VEL = 600f;
	private static final float MAX_VEL = 1050f;
	private static final float BASE_DAMAGE = 1000f;
	private static final float KNOCKBACK_FORCE = 400f;
	
	private static final float VEL_CHANGE_THRESHOLD = 0.9f;
	private static final float RESTITUTION = 0.25f;
	private static final float RESTITUTION_FIGHTER = 0.75f;
	private static final float DEBOUNCE_DUR = 0.1f;
	
	// private ShipAPI ship;
	private final MissileAPI missile;
	private CombatEntityAPI target;
	
	private Vector2f velocityLast = new Vector2f();
	private float debounce = 0f;

	public espc_FinneganAI(MissileAPI missile, ShipAPI ship) {
		// if (ship != null)
		// 	this.ship = ship;
		this.missile = missile;
		velocityLast = Vector2f.add(missile.getVelocity(), Misc.ZERO, velocityLast);
	}

	@Override
	public void advance(float amount) {		
		
		if (amount <= 0f || Global.getCombatEngine().isPaused() || missile.isFading())
			return;
		
		CombatEngineAPI combatEngine = Global.getCombatEngine();
		// Global.getLogger(espc_FinneganAI.class).info("speed: " + missile.getVelocity().length() + ", didBounce: " + debounce);
		// Global.getLogger(espc_FinneganAI.class).info("vs: " + velocityLast.length() * VEL_CHANGE_THRESHOLD);
		if (debounce <= 0f && velocityLast.length() > MIN_VEL && 
			missile.getVelocity().length() < velocityLast.length() * VEL_CHANGE_THRESHOLD) {
			// Global.getLogger(espc_FinneganAI.class).info("do check");
			
			Iterator<Object> entityIterator = combatEngine.getAllObjectGrid().getCheckIterator(
				missile.getLocation(), 
				COLLISION_RADIUS * 0.3f,
				COLLISION_RADIUS * 0.3f
			);
			
			CombatEntityAPI bestTarget = null;
			float bestDistance = 0f;
			
			while (entityIterator.hasNext()) {
				// Global.getLogger(espc_FinneganAI.class).info("iter");
				CombatEntityAPI entity = (CombatEntityAPI) entityIterator.next();
				if (!(entity instanceof DamagingProjectileAPI)) {
					if (bestTarget == null) {
						bestTarget = entity;
						bestDistance = MathUtils.getDistanceSquared(missile, entity);
						continue;
					}
					if (MathUtils.getDistanceSquared(missile, entity) < bestDistance) {
						bestTarget = entity;
						bestDistance = MathUtils.getDistanceSquared(missile, entity);
					}
				}
			}
			if (bestTarget != null) {
				float speed = Math.min(MAX_VEL, velocityLast.length());
				
				combatEngine.applyDamage(
					bestTarget,
					missile.getLocation(),
					speed / BASE_VEL * BASE_DAMAGE * (missile.getSource() == null ? 1f : ((1f + 
						missile.getSource().getMutableStats().getMissileWeaponDamageMult().getFlatMod() +
						missile.getSource().getMutableStats().getMissileWeaponDamageMult().getPercentMod() / 100f)
						* missile.getSource().getMutableStats().getMissileWeaponDamageMult().getMult())
					),
					DamageType.KINETIC,
					0f,
					false,
					false,
					(missile.getSource() == null ? combatEngine : missile.getSource())
				);
				CombatUtils.applyForce(bestTarget, missile.getVelocity(), speed / BASE_VEL * KNOCKBACK_FORCE);
			
				Vector2f newVel = Vector2f.sub(
					missile.getLocation(), 
					bestTarget.getLocation(),
					new Vector2f()
				);
				newVel.normalise();
				newVel.scale(Vector2f.dot(newVel, velocityLast) * -2f);
				
				if (bestTarget instanceof ShipAPI && ((ShipAPI) bestTarget).getHullSize() == HullSize.FIGHTER) {
					Vector2f.add(
						velocityLast, 
						newVel,
						newVel
					);
					newVel.scale(1f - RESTITUTION_FIGHTER);
					velocityLast.scale(RESTITUTION_FIGHTER);
					Vector2f.add(
						velocityLast, 
						newVel,
						missile.getVelocity()
					);
					debounce = DEBOUNCE_DUR * 0.5f;
				} else {
					Vector2f.add(
						velocityLast, 
						newVel, 
						missile.getVelocity()
					);
					missile.getVelocity().scale(RESTITUTION);
					debounce = DEBOUNCE_DUR;
				}
				
				Global.getSoundPlayer().playSound(
					"espc_finnegan_impact",
					1.0f, 1.0f * speed / BASE_VEL * 0.25f, missile.getLocation(), Misc.ZERO);
			}
		} else if (debounce > 0f)
			debounce -= amount;
		
		if (missile.isArmed()) {
			DamagingExplosionSpec explosion = new DamagingExplosionSpec(
				0.1f,
				225f,
				175f,
				missile.getDamageAmount(),
				missile.getDamageAmount() / 2f,
				CollisionClass.MISSILE_FF,
				CollisionClass.MISSILE_FF,
				3f,
				4f,
				0.8f,
				200,
				new Color(255,100,50,255),
				new Color(255,100,50,255)
			);
			explosion.setDamageType(DamageType.HIGH_EXPLOSIVE);
			explosion.setUseDetailedExplosion(true);
			explosion.setDetailedExplosionRadius(250f);
			explosion.setDetailedExplosionFlashRadius(500f);
			explosion.setShowGraphic(true);
			explosion.setSoundSetId("espc_finnegan_explode");
			combatEngine.spawnDamagingExplosion(explosion, missile.getSource(), missile.getLocation(), true);
			combatEngine.removeEntity(missile);
		}
		
		velocityLast = Vector2f.add(missile.getVelocity(), Misc.ZERO, velocityLast);
	}

	@Override
	public CombatEntityAPI getTarget() {
		return target;
	}

	@Override
	public void setTarget(CombatEntityAPI target) {
		this.target = target;
	}
}