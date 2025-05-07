package data.scripts.shipsystems;

// there's a lot of optimization that can be done here.
// i will fix this absolute goddamn frankenstein code when alex fixes the ability to spawn projectiles from spec id
// i'm assuming that's a thing that's broken and will be fixed right?  right????
// please god help me

// addendum: lol lmao

import com.fs.starfarer.api.Global;
// import com.fs.starfarer.api.SoundPlayerAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.loading.MissileSpecAPI;
// import com.fs.starfarer.api.loading.ProjectileSpecAPI;
// import com.fs.starfarer.api.plugins.ShipSystemStatsScript.State;
import com.fs.starfarer.api.combat.ShipSystemAPI.SystemState;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;

// import data.scripts.util.MagicLensFlare;
// import data.scripts.weapons.proj.espc_FisProjVFX;

import com.fs.starfarer.api.util.Misc;
import java.awt.Color;
// import java.util.List;
// import java.util.Queue;
import java.util.ArrayDeque;
// import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.lwjgl.util.vector.Vector2f;
import org.lazywizard.lazylib.MathUtils;

// import org.dark.shaders.distortion.RippleDistortion;

public class espc_CollapseStats extends BaseShipSystemScript {
	
	private static final float ABILITY_RANGE = 1750f;
	// additional radius around the given ship's shield bounds (or collision bounds, whichever is more generous as to mod-proof) 
	private static final float FREEZE_RADIUS = 150f;
	private static final float FREEZE_RADIUS_MIN = 50f;
	
	private ShipAPI target;
	// 0: initialize, 1: initialized, 2 = projectiles released
	private int useState = 0;
	
	private HashMap<String, StaticProjType> freezeTypes;
	
	private espc_CollapseVFX freezeRenderer;
	
	private class StaticProjType {
		// String projectileSprite;
		public WeaponAPI weapon;
		// public String weaponId;
		// public String projSpecId;	
		public OnFireEffectPlugin projEffectPlugin;
		public ArrayDeque<StaticProj> projs;
		public StaticProjType(DamagingProjectileAPI proj, boolean isMissile) {
			
			this.weapon = proj.getWeapon();
			
			if (isMissile) {
				if (((MissileSpecAPI) proj.getWeapon().getSpec().getProjectileSpec()).getOnFireEffect() != null)
					projEffectPlugin =  ((MissileSpecAPI) proj.getWeapon().getSpec().getProjectileSpec()).getOnFireEffect();
			} else {
				if (proj.getProjectileSpec().getOnFireEffect() != null)
					projEffectPlugin = proj.getProjectileSpec().getOnFireEffect();
			}
	
			projs = new ArrayDeque<StaticProj>();
			addProj(proj);
		}
		public void addProj(DamagingProjectileAPI proj) {
			projs.push(new StaticProj(proj));
		}
	}
	
	private class StaticProj {
		public float damage;
		public float angle;
		public Vector2f offset;
		public Vector2f velocity;
		public float angularVelocity;
		public StaticProj(DamagingProjectileAPI proj) {
			damage = proj.getDamageAmount() * proj.getBaseDamageAmount();
			angle = proj.getFacing();
			offset = Vector2f.sub(proj.getLocation(), target.getLocation(), new Vector2f());
			velocity = proj.getVelocity();
			angularVelocity = proj.getAngularVelocity();
		}
	}
	
	
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		if (stats.getEntity() == null)
			return;

		CombatEngineAPI combatEngine = Global.getCombatEngine();
		if (combatEngine.getElapsedInLastFrame() <= 0f || combatEngine.isPaused())
			return;
		
		ShipAPI ship = (ShipAPI) stats.getEntity();
		
		if (state == State.ACTIVE) {
			if (useState == 0) {
			
				if (target == null) {
					target = findTarget(ship);
					target.setCustomData("espc_collapse", true);
					// self-targeting.  i have decided this is too big-brained to allow.
					// if (target == null)
					// 	target = (ShipAPI) stats.getEntity();
					freezeTypes = new HashMap<String, StaticProjType>();
				}
				
		        freezeRenderer = new espc_CollapseVFX(target, ship, combatEngine.getTotalElapsedTime(false));
		        combatEngine.addPlugin(freezeRenderer);
				useState = 1;
				
			}
			
			if (target == null || !combatEngine.isInPlay(target) || target.isHulk() || !target.isAlive()) {
				ship.getSystem().forceState(SystemState.OUT, 0f);
				freezeRenderer.setToRemove(combatEngine);
				useState = 2;
				return;
			}
			target.setJitterUnder(this, new Color(50, 100, 255, 170), effectLevel, 2, effectLevel * 15f);
			
			Iterator<Object> entityIterator = combatEngine.getAllObjectGrid().getCheckIterator(
				target.getLocation(), 
				(FREEZE_RADIUS + target.getShieldRadiusEvenIfNoShield()) * 2f,
				(FREEZE_RADIUS + target.getShieldRadiusEvenIfNoShield()) * 2f
			);
			while (entityIterator.hasNext()) {
				CombatEntityAPI entity = (CombatEntityAPI) entityIterator.next();
				if (!(entity instanceof DamagingProjectileAPI))
					continue;
				
				DamagingProjectileAPI thisProj = (DamagingProjectileAPI) entity;
			// for (DamagingProjectileAPI thisProj : combatEngine.getProjectiles()) {
				if (combatEngine.isInPlay(thisProj) && !thisProj.didDamage() && !thisProj.isExpired() && thisProj.getWeapon() != null) {
					if (MathUtils.isWithinRange(target.getLocation(), thisProj.getLocation(), FREEZE_RADIUS + target.getShieldRadiusEvenIfNoShield())) {
						if (!MathUtils.isWithinRange(target.getLocation(), thisProj.getLocation(), FREEZE_RADIUS_MIN + target.getShieldRadiusEvenIfNoShield())
							) {
							boolean isMissile = false;
							if (thisProj instanceof MissileAPI) {
								isMissile = true;
								if (!(thisProj.getWeapon().getSpec().getProjectileSpec() instanceof MissileSpecAPI)) {
									continue;
								} else {
									if (!((MissileSpecAPI) thisProj.getWeapon().getSpec().getProjectileSpec()).equals(
										((MissileAPI) thisProj).getSpec()))
										continue;
								}
							} else 
								if (!thisProj.getProjectileSpec().equals(
									thisProj.getWeapon().getSpec().getProjectileSpec()))
									continue;
							
							freezeRenderer.FreezeProj(thisProj, isMissile);
							float projDamage = thisProj.getDamageAmount();
							if (projDamage > 175f) {
								projDamage = Math.min(projDamage, 2000f);
				            	
				                combatEngine.addHitParticle(
				                	thisProj.getLocation(),
				                	Misc.ZERO,
				                	50 + projDamage / 30,
				                	0.6f,
				                	0.25f + projDamage / 10000f,
				                	new Color(50, 100, 255, 
				                		(int) (projDamage / 30f + 60f)
				                	)
				                );
				                combatEngine.addHitParticle(
				                	thisProj.getLocation(),
				                	Misc.ZERO,
				                	40 + projDamage / 35,
				                	1.5f + projDamage / 2000f,
				                	0.1f + projDamage / 15000f,
				                	new Color(200, 235, 255, 
				                		(int) (projDamage / 15f + 120f)
				                	)
				                );
				                /*
								Global.getSoundPlayer().playSound(
									"espc_temporal_freeze", 
									1.05f - projDamage / 6000f,
									0.3f + projDamage / 8000f, 
									thisProj.getLocation(),
									Misc.ZERO
								); */
							}
							// Global.getLogger(espc_CollapseStats.class).info("weaponId: " + thisProj.getWeapon().getId());
							if (freezeTypes.containsKey(thisProj.getSource().getId() + thisProj.getWeapon().getId())) {
								((StaticProjType) freezeTypes.get(
									thisProj.getSource().getId() + thisProj.getWeapon().getId()
								)).addProj(thisProj);
							} else {
								StaticProjType newType = new StaticProjType(thisProj, isMissile);
								freezeTypes.put(thisProj.getSource().getId() + thisProj.getWeapon().getId(), newType);
							}
							
							combatEngine.removeEntity(thisProj);
							
							
						}
					}
				}
			}
		} else if (state == State.OUT && useState == 1) {
			freezeRenderer.setToRemove(combatEngine);
			useState = 2;
		}
		if (target != null) {
			Global.getSoundPlayer().playLoop(
				"espc_temporal_loop",
				ship,
				1f,
				// state == State.OUT ? 1.2f - effectLevel * 0.6f : effectLevel * 0.6f,
				effectLevel * 0.75f,
				target.getLocation(),
				target.getVelocity()
			);
		}
	}
	
	public void unapply(MutableShipStatsAPI stats, String id) {
		if (useState > 0) {
			
			// ShipAPI ship = (ShipAPI) stats.getEntity();
			CombatEngineAPI combatEngine = Global.getCombatEngine();
			if (useState == 1) {
				freezeRenderer.setToRemove(combatEngine);
			}
			freezeRenderer.releaseVisuals();
			if (target != null) {
				for (StaticProjType thisType : freezeTypes.values()) {
					while (thisType.projs.peek() != null) {
						StaticProj thisProj = (StaticProj) thisType.projs.pop();
	
						DamagingProjectileAPI spawnProj = (DamagingProjectileAPI) combatEngine.spawnProjectile(
							thisType.weapon.getShip(), 
							thisType.weapon,
							thisType.weapon.getId(),
							// alt projectile specid, deprecated apparently urgh 
							Vector2f.add(target.getLocation(), thisProj.offset, new Vector2f()), 
							thisProj.angle, 
							Misc.ZERO);
	
						if (thisType.projEffectPlugin != null)
							thisType.projEffectPlugin.onFire(spawnProj, thisType.weapon, combatEngine);
						spawnProj.setDamageAmount(thisProj.damage / spawnProj.getDamageAmount());
						
						float projDamage = thisProj.damage;
						if (projDamage > 175f) {
							projDamage = Math.min(projDamage, 2000f);
						
							combatEngine.addHitParticle(
			                	spawnProj.getLocation(),
			                	Misc.ZERO,
			                	50 + projDamage / 30,
			                	0.6f,
			                	0.15f + projDamage / 15000f,
			                	new Color(50, 100, 255, 
			                		(int) (projDamage / 30f + 60f)
			                	)
			                );
			                combatEngine.addHitParticle(
			                	spawnProj.getLocation(),
			                	Misc.ZERO,
			                	40 + projDamage / 35,
			                	1.5f + projDamage / 2000f,
			                	0.05f + projDamage / 20000f,
			                	new Color(200, 235, 255, 
			                		(int) (projDamage / 15f + 120f)
			                	)
			                );
						}
						
						spawnProj.setAngularVelocity(thisProj.angularVelocity);
						Vector2f.add(new Vector2f(), thisProj.velocity, spawnProj.getVelocity());
					}
				}
			}
			//*/
		}
		target = null;
		useState = 0;
		freezeTypes = null;
	}
	
	// ripped these from alex's code for entropy amplifier/disruptor, with minor alterations.
	// would prefer to use my own code, but the stock AI allows it to target fighters without these tweaks.  urgh.
	
	public static float getMaxRange(ShipAPI ship) {
		return ship.getMutableStats().getSystemRangeBonus().computeEffective(ABILITY_RANGE);
	}
	
	protected ShipAPI findTarget(ShipAPI ship) {
		float range = getMaxRange(ship);
		boolean player = ship == Global.getCombatEngine().getPlayerShip();
		ShipAPI target = ship.getShipTarget();
		if (target != null) {
			float dist = Misc.getDistance(ship.getLocation(), target.getLocation());
			float radSum = ship.getCollisionRadius() + target.getCollisionRadius();
			if (dist > range + radSum) target = null;
		} else {
			if (target == null || target.getOwner() == ship.getOwner()) {
				if (player) {
					target = Misc.findClosestShipEnemyOf(ship, ship.getMouseTarget(), HullSize.FRIGATE, range, true);
				} else {
					Object test = ship.getAIFlags().getCustom(AIFlags.MANEUVER_TARGET);
					if (test instanceof ShipAPI) {
						target = (ShipAPI) test;
						float dist = Misc.getDistance(ship.getLocation(), target.getLocation());
						float radSum = ship.getCollisionRadius() + target.getCollisionRadius();
						if (dist > range + radSum) target = null;
					}
				}
			}
			if (target == null) {
				target = Misc.findClosestShipEnemyOf(ship, ship.getLocation(), HullSize.FRIGATE, range, true);
			}
		}
		
		return target;
	}

	@Override
	public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
		if (system.getState() != SystemState.IDLE)
			return null;
		
		ShipAPI hasTarget = findTarget(ship);

		if (hasTarget != null && target != ship) {
			if (!hasTarget.getCustomData().containsKey("espc_collapse"))
				return "READY";
			else {
				if (((Boolean) hasTarget.getCustomData().get("espc_collapse"))) {
					return "ALREADY INTERCEPTED";
				} else
					return "READY";
			}
		}
		if ((hasTarget == null) && ship.getShipTarget() != null) {
			return "OUT OF RANGE";
		}
		return "NO TARGET";

		// return MathUtils.isWithinRange(hasTarget, ship, ABILITY_RANGE) ? "READY" : "OUT OF RANGE";
	}
	
	
	@Override
	public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
		ShipAPI hasTarget = findTarget(ship);
		if (hasTarget == null)
			return false;
		if (hasTarget == ship)
			return false;
		if (hasTarget.getHullSize() == HullSize.FIGHTER)
			return false;
		if (!(hasTarget.getCustomData().containsKey("espc_collapse")))
			return true;
		else {
			if (((Boolean) hasTarget.getCustomData().get("espc_collapse"))) {
				return false;
			} else
				return true;
		}
		// return MathUtils.isWithinRange(hasTarget, ship, ABILITY_RANGE);
	}
}