package data.scripts.shipsystems;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.loading.MissileSpecAPI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.WeaponAPI.AIHints;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
// import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import java.util.List;

import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

public class espc_SalvoStats extends BaseShipSystemScript {
	
	private static class mFighter {
		public WeaponAPI missileWep;
		public ShipAPI fighter;
		public int remaining = 0;
		public int cumulative = 0;
		public float timePerShot = 0.1f;
		public float burstTime = 0.1f;
		public boolean burstStarted = false;
		public float baseDamage = 100f;
		public OnFireEffectPlugin projEffectPlugin = null;
		public mFighter(ShipAPI fighter, WeaponAPI missileWep, int remaining, int cumulative, boolean override, float timePerShot, OnFireEffectPlugin plugin) {
			this.fighter = fighter;
			this.missileWep = missileWep;
			this.remaining = remaining;
			this.cumulative = cumulative;
			if (this.remaining == 1)
				this.timePerShot = 0.1f;
			else
				this.timePerShot = timePerShot;
			this.burstTime = this.timePerShot;
			this.projEffectPlugin = plugin;
			this.burstStarted = override;
			this.baseDamage = ((MissileSpecAPI) missileWep.getSpec().getProjectileSpec()).getDamage().getBaseDamage();
			
			// if (((MissileSpecAPI) missileWep.getSpec().getProjectileSpec()).getOnFireEffect() != null)
			// 	this.projEffectPlugin = ((MissileSpecAPI) missileWep.getSpec().getProjectileSpec()).getOnFireEffect();
		}
		public mFighter(ShipAPI fighter) {
			this.fighter = fighter;
			this.remaining = 0;
			this.burstTime = 0.0f;
			this.burstStarted = true;
		}
	}
	
	// if OP*ammo/missile is greater than this value, it is split between fighters.
	// i.e. a reaper (2 OP/missile) is split between two fighters, 5 OP/2 missiles would see a similar split.
	// private static final float MISSILE_OP_MAX = 1f;
	// private static final int BURST_MAX = 25;
	private static final int OP_PER_WING = 3;
	private static final int OP_PER_WING_SMALL = 3;
	private static final int BURSTS_PER_FIGHTER = 3;
	private static final float MIN_RANGE = 150f;
	private static final float UNGUIDED_RADIUS_THRESHOLD = 0.7f;
	// public static final float BURST_INTERVAL_DEFAULT = 0.5f;
	// 0 = initialize, 1 = still bursting missiles, 2 = finished
	private int useState = 0;
	private int remainingOP = 0;
	
	private List<WeaponAPI> shipMissiles;
	private List<ShipAPI> shipFighters;
	private LinkedList<mFighter> mFighters;
	
	private ShipAPI ship;
	
	
	private boolean fireMissileFighter(WeaponAPI spawnMissile, ShipAPI fighter, int barrel, float baseDamage, 
		OnFireEffectPlugin plugin, CombatEngineAPI combatEngine, boolean override) {
		if (!fighter.getFluxTracker().isOverloaded() && (override ||
			// support fighters never take a target.  in this case we always fire regardless of range, or
			((fighter.getShipTarget() == null
			// ensure a ship target when using dumbfire missiles
			&& (ship.getShipTarget() != null
			|| spawnMissile.getProjectileCollisionClass() != CollisionClass.MISSILE_FF)
			)
				|| (fighter.getShipTarget() != null &&
					(fighter.getWing().getRange() <= 0f || (
						(MathUtils.isWithinRange(
							fighter, 
							fighter.getShipTarget(), 
							spawnMissile.getRange()
						) && 
						!MathUtils.isWithinRange(
							fighter, 
							fighter.getShipTarget(), 
							MIN_RANGE
						))
					))
				) || spawnMissile.hasAIHint(AIHints.ANTI_FTR)
			// if using anti-fighter missiles, we can just look for nearby fighters.  in fact, we should prioritize that
			))
		) {
			// just gonna assume friendly fire missiles are dumbfire.
			// i'm not writing an exception for guided friendly fire missiles.  like, wyd???
			
			// as much as i'm for shooting dumbfire missiles haphazardly when without a target, this is probably a bad idea.
			if (!override && spawnMissile.getProjectileCollisionClass() == CollisionClass.MISSILE_FF) {
				ShipAPI target = fighter.getShipTarget();
				if (target == null)
					target = ship.getShipTarget();
				// don't fire dumbfires at other fighters, that is a horrendous idea.
				if (target == null || target.getOwner() == ship.getOwner() || target.isFighter())
					return false;
				Vector2f ray = new Vector2f(
					(float) FastTrig.cos(Math.toRadians(fighter.getFacing())), 
					(float) FastTrig.sin(Math.toRadians(fighter.getFacing())));
				ray.scale(spawnMissile.getRange());
				// no travel time/speed compensation: can give wild results w/ mobility systems,
				// & getting actual missile speed (i.e. acceleration) is gonna be a pain in the ass.
				if (!CollisionUtils.getCollides(
					fighter.getLocation(), 
					Vector2f.add(fighter.getLocation(), ray, new Vector2f()),
					target.getLocation(),
					target.getShieldRadiusEvenIfNoShield() * UNGUIDED_RADIUS_THRESHOLD
				))
					return false;
				// collision grid check for ships
				Iterator<Object> shipGridIterator = combatEngine.getAiGridShips().getCheckIterator(
					Vector2f.add(fighter.getLocation(), (Vector2f) ray.normalise().scale(MathUtils.getDistance(
						fighter.getLocation(), target.getLocation()) * 0.5f
					), new Vector2f()),
					Math.abs(target.getLocation().x - fighter.getLocation().x), 
					Math.abs(target.getLocation().y - fighter.getLocation().y)
				);
				ray.scale(2f);
				Vector2f.add(fighter.getLocation(), ray, ray);
				while (shipGridIterator.hasNext()) {
					ShipAPI currShip = (ShipAPI) shipGridIterator.next();
					if (currShip.getHullSize() != HullSize.FIGHTER && 
						currShip.getOwner() == ship.getOwner() &&
						CollisionUtils.getCollides(
						fighter.getLocation(), 
						ray,
						currShip.getLocation(),
						currShip.getCollisionRadius()
					))
						return false;
				}
			}

			if (baseDamage <= 150)
				Global.getSoundPlayer().playSound("swarmer_fire", 1.05f, 0.5f, fighter.getLocation(), fighter.getVelocity());
			else if (baseDamage <= 400)
				Global.getSoundPlayer().playSound("annihilator_fire", 1.05f, 0.65f, fighter.getLocation(), fighter.getVelocity());
			else if (baseDamage < 1000)
				Global.getSoundPlayer().playSound("harpoon_fire", 1.05f, 0.85f, fighter.getLocation(), fighter.getVelocity());
			else if (baseDamage < 1500)
				Global.getSoundPlayer().playSound("atropos_fire", 1.05f, 1.0f, fighter.getLocation(), fighter.getVelocity());
			else if (baseDamage < 2500)
				Global.getSoundPlayer().playSound("hammer_fire", 1.0f, 1.0f, fighter.getLocation(), fighter.getVelocity());
			else 
				Global.getSoundPlayer().playSound("reaper_fire", 1.0f, 1.0f, fighter.getLocation(), fighter.getVelocity());
			
			// due to multi-shot missiles, we'll want to apply and reapply this for every missile launched, unfortunately
			// ship.getMutableStats().getMissileWeaponRangeBonus().modifyPercent(id, 35f);
			CombatEntityAPI missile = combatEngine.spawnProjectile(
				fighter,
				spawnMissile,
				spawnMissile.getId(), 
				fighter.getLocation(), 
				fighter.getFacing() + (Misc.random.nextFloat() * 2f - 1f) * spawnMissile.getSpec().getMaxSpread() +
				spawnMissile.getSpec().getHardpointAngleOffsets().get(
					barrel % spawnMissile.getSpec().getHardpointAngleOffsets().size()), 
				fighter.getVelocity()
			);
			// honestly i'd be the sort of fucker to require this check.
			if (missile instanceof MissileAPI) {
				// this is a lot of effort to makes swarmers work tbh.  saving grace is that this doesn't need to fire ALL the time
				if (spawnMissile.hasAIHint(AIHints.ANTI_FTR) &&
					((MissileAPI) missile).getMissileAI() instanceof GuidedMissileAI) {
					Iterator<Object> entityIterator = combatEngine.getAiGridShips().getCheckIterator(
						fighter.getLocation(), 
						spawnMissile.getRange() * 2f,
						spawnMissile.getRange() * 2f
					);
					ShipAPI target = null;
					float dist = spawnMissile.getRange() * spawnMissile.getRange() * 1.6f;
					while (entityIterator.hasNext()) {
						ShipAPI currShip = (ShipAPI) entityIterator.next();
						if (currShip.getOwner() != ship.getOwner() &&
							currShip.isFighter() && 
							MathUtils.getDistanceSquared(fighter.getLocation(), currShip.getLocation()) <= dist) {
							target = currShip;
							dist = MathUtils.getDistanceSquared(fighter.getLocation(), currShip.getLocation());
						}
					}
					if (target != null) {
						((GuidedMissileAI)((MissileAPI) missile).getMissileAI()).setTarget(target);
					}
				}
				((MissileAPI) missile).setMaxFlightTime(((MissileAPI) missile).getMaxFlightTime() * 1.25f);
				((MissileAPI) missile).setMaxRange(((MissileAPI) missile).getMaxRange() * 1.25f);
			}
			// ship.getMutableStats().getMissileWeaponRangeBonus().unmodify("espc_salvo");
			if (plugin != null)
				plugin.onFire((DamagingProjectileAPI) missile, spawnMissile, combatEngine);
			return true;
		}
		return false;
	}
	
					
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		
		if (stats.getEntity() == null)
			return;
		
		if (useState == 2) {
			ship.useSystem();
			return;
		}
		
		if (useState == 0) {
			if (ship == null)
				ship = (ShipAPI) stats.getEntity();
			
			shipMissiles = new ArrayList<WeaponAPI>();
			List<Integer> fightersPerMissile = new ArrayList<Integer>();
			for (WeaponAPI weapon : ship.getAllWeapons()) {
				if (weapon.getType() == WeaponType.MISSILE) {
					// If your missile is worth more than 5 OP per shot, I'm not firing it.  Fuck you.
					if (weapon.getSpec().getOrdnancePointCost(null) / weapon.getSpec().getMaxAmmo() > 5)
						continue;
					shipMissiles.add(weapon);
					if (weapon.usesAmmo() && weapon.getAmmoPerSecond() <= 0f)
						/*
						fightersPerMissile.add(Math.max((int)Math.floor(((float)weapon.getSpec().getOrdnancePointCost(null)
							/(float)weapon.getMaxAmmo() /(float)weapon.getSpec().getBurstSize())), 1));*/
						fightersPerMissile.add(Math.max(Math.min((int)Math.floor(((float)weapon.getSpec().getOrdnancePointCost(null)
							/(float)weapon.getMaxAmmo())), weapon.getSpec().getBurstSize() * BURSTS_PER_FIGHTER), 1));
					else {
						// base shot count on shots/minute?
						// fightersPerMissile.add(2);
						// salamander: 2.4 shots/min, 5 OP
						// salamander mrm: 4.8 shots/min, 10 OP
						// pilum: 6 shots/min, 7 OP
						// pilum lrm: 20 shots/min, 14 OP
						fightersPerMissile.add(
							(int) (weapon.getSpec().getAmmoPerSecond() * 60f * weapon.getSpec().getOrdnancePointCost(null)) /
							weapon.getSpec().getBurstSize());
					}
				}
			}
			if (shipMissiles.size() < 1) {
				useState = 2;
				return;
			}
			
			int missileIndex = 0;
			// 0 = check for split or add missile, 1 = add missile, 2 = split
			int fighterSplit = 0;
			shipFighters = new ArrayList<ShipAPI>();
			
			for (FighterWingAPI thisWing : ship.getAllWings()) {
				remainingOP += ship.getHullSize() == HullSize.CRUISER ? OP_PER_WING : OP_PER_WING_SMALL;
				for (ShipAPI thisWingFighter : thisWing.getWingMembers()) {
					if (thisWingFighter.isAlive() && !thisWingFighter.isHulk()){
						shipFighters.add(thisWingFighter);
					}
				}
			}
			
			CombatEngineAPI combatEngine = Global.getCombatEngine();
			
			if (shipFighters.size() > 0) {
				mFighters = new LinkedList<mFighter>();
			} else
				useState = 2;
			
			// weaponIndex.size() = amount of fighters left still bursting, as we add an entry for each fighter.
			while (useState != 2 && shipFighters.size() > 0 && remainingOP > 0) {
				remainingOP--;
				if (fighterSplit > 0) {
					--fighterSplit;
					shipFighters.remove(0);
					continue;
				}
				if ((Integer) fightersPerMissile.get(missileIndex) > 1 && fighterSplit == 0)
					fighterSplit = (Integer) fightersPerMissile.get(missileIndex) - 1;
				
				OnFireEffectPlugin thisPlugin = null;
				if (((MissileSpecAPI) ((WeaponAPI) shipMissiles.get(missileIndex)).getSpec().getProjectileSpec()).getOnFireEffect() != null)
					thisPlugin = ((MissileSpecAPI) ((WeaponAPI) shipMissiles.get(missileIndex)).getSpec().getProjectileSpec()).getOnFireEffect();
				boolean fired = fireMissileFighter(
					(WeaponAPI) shipMissiles.get(missileIndex),
					(ShipAPI) shipFighters.get(0),
					0,
					((MissileSpecAPI) ((WeaponAPI) shipMissiles.get(missileIndex)).getSpec().getProjectileSpec())
						.getDamage().getBaseDamage(),
					thisPlugin, combatEngine,
					false
				);
				if (((WeaponAPI) shipMissiles.get(missileIndex)).getSpec().getBurstSize() > 1 || !fired) {
					mFighters.add(new mFighter(
						(ShipAPI) shipFighters.get(0),
						(WeaponAPI) shipMissiles.get(missileIndex),
						((WeaponAPI) shipMissiles.get(missileIndex)).getSpec().getBurstSize() > 1 ?
							// Math.min(
								Math.min(
									((WeaponAPI) shipMissiles.get(missileIndex)).getSpec().getBurstSize() * BURSTS_PER_FIGHTER,
									(int) (
										((WeaponAPI) shipMissiles.get(missileIndex)).getMaxAmmo() /
										((WeaponAPI) shipMissiles.get(missileIndex)).getSpec().getOrdnancePointCost(null)
									)
							// 	), BURST_MAX
							) - (fired ? 1 : 0)
							: 1,
						fired ? 1 : 0,
						fired,
						((WeaponAPI) shipMissiles.get(missileIndex)).getDerivedStats().getBurstFireDuration() /
						((WeaponAPI) shipMissiles.get(missileIndex)).getSpec().getBurstSize() / 2f,
						thisPlugin
					));
				} else { // used to be optimized to run on lowest burst interval & throw out
					// probably needlessly aggressive.  want to add an entry here anyway to get the visual flash
					// not really a perf concern
					mFighters.add(new mFighter((ShipAPI) shipFighters.get(0)));
				}
				shipFighters.remove(0);
				missileIndex = (missileIndex + 1) % shipMissiles.size();
			}
			if (useState == 0 && mFighters.size() > 0) {
				useState = 1;
			} else
				useState = 2;
		} else if (useState == 1) {
			CombatEngineAPI combatEngine = Global.getCombatEngine();
			
			Iterator<mFighter> fighters = mFighters.iterator();
			while (fighters.hasNext()) {
				mFighter fighter = (mFighter) fighters.next();
				boolean setForRemove = false;

				if (fighter.fighter == null ||
					!fighter.fighter.isAlive() ||
					fighter.fighter.isHulk() ||
					!combatEngine.isInPlay(fighter.fighter)
				) {
					fighters.remove();
					if (mFighters.size() == 0)
						useState = 2;
				}
				else {
					if (!fighter.burstStarted) {
						fighter.burstStarted = fireMissileFighter(
							fighter.missileWep, 
							fighter.fighter, 
							fighter.cumulative,
							fighter.baseDamage, fighter.projEffectPlugin, combatEngine, false);
						if (fighter.burstStarted) {
							fighter.remaining -= 1;
							fighter.cumulative+= 1;
						}
					} else {
						fighter.burstTime -= combatEngine.getElapsedInLastFrame();
						if (fighter.burstTime <= 0f && fighter.remaining > 0) {
							boolean fired = fireMissileFighter(
								fighter.missileWep, 
								fighter.fighter, 
								fighter.cumulative,
								fighter.baseDamage, fighter.projEffectPlugin, combatEngine, true);
							if (fired) {
								fighter.remaining -= 1;
								fighter.cumulative+= 1;
							}
							else
								fighter.burstTime = 0.01f;
							if (fighter.remaining > 0)
								fighter.burstTime = Math.max(fighter.burstTime + fighter.timePerShot, 0f);
						}
					}
					
					float fadeOut = fighter.remaining == 0 ? 2f + fighter.burstTime * 2f : effectLevel;
					if (effectLevel < fadeOut && state == State.OUT)
						fadeOut = effectLevel;
					setForRemove = fadeOut <= 0f;
					
					if (!setForRemove) {
						fighter.fighter.setJitterUnder(this, new Color(155, 255, 0, 130),
							1f * fadeOut, 5, 0f, 4f);
						fighter.fighter.setJitter(this, new Color(155, 255, 0, 130),
							1f * fadeOut, 2, 0f, 4f);
						Global.getSoundPlayer().playLoop("system_targeting_feed_loop", ship, 1f, 1f,
							fighter.fighter.getLocation(), fighter.fighter.getVelocity());
					} else {
						fighters.remove();
						if (mFighters.size() <= 0)
							useState = 2;
					}
				}
				
			}
			
		}
	}

	public void unapply(MutableShipStatsAPI stats, String id) {
		useState = 0;
		remainingOP = 0;
		shipMissiles = null;
		shipFighters = null;
		mFighters = null;
	}
	

	/*
	private List<ShipAPI> getFighters(ShipAPI carrier) {
		List<ShipAPI> result = new ArrayList<ShipAPI>();
		
//		this didn't catch fighters returning for refit		
//		for (FighterLaunchBayAPI bay : carrier.getLaunchBaysCopy()) {
//			if (bay.getWing() == null) continue;
//			result.addAll(bay.getWing().getWingMembers());
//		}
		
		for (ShipAPI ship : Global.getCombatEngine().getShips()) {
			if (!ship.isFighter()) continue;
			if (ship.getWing() == null) continue;
			if (ship.getWing().getSourceShip() == carrier) {
				result.add(ship);
			}
		}
		
		return result;
	} */
	
}
