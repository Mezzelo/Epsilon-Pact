package data.scripts.shipsystems;

import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.loading.MissileSpecAPI;
import com.fs.starfarer.api.loading.WingRole;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipSystemAPI.SystemState;
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
		public int count = 0;
		public int cumulative = 0;
		public float timePerShot = 0.1f;
		public float burstTime = 0.1f;
		public boolean burstStarted = false;
		public float baseDamage = 100f;
		public OnFireEffectPlugin projEffectPlugin = null;
		public mFighter(ShipAPI fighter, WeaponAPI missileWep, int count, int cumulative, boolean override, float timePerShot, OnFireEffectPlugin plugin) {
			this.fighter = fighter;
			this.missileWep = missileWep;
			this.count = count;
			this.cumulative = cumulative;
			if (this.cumulative == 0)
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
		
		// add entries for jitter fade out
		public mFighter(ShipAPI fighter) {
			this.fighter = fighter;
			this.count = 0;
			this.burstTime = 0.0f;
			this.burstStarted = true;
		}
	}
	
	public static class MissileToFire {
		public WeaponAPI missileWep;
		public int count;
		public int cost;
		public float refireDelay = 0.05f;
		public float missilesPerMinute = -1f;
		public MissileToFire(WeaponAPI missileWep, int count, int cost, float refireDelay) {
			this.missileWep = missileWep;
			this.count = count;
			this.cost = cost;
			this.refireDelay = refireDelay;
		}
		public MissileToFire(WeaponAPI missileWep, int count, int cost, float refireDelay,
			float missilesPerMinute) {
			this.missileWep = missileWep;
			this.count = count;
			this.cost = cost;
			this.refireDelay = refireDelay;
			this.missilesPerMinute = missilesPerMinute;
		}
	}
	
	// if OP*ammo/missile is greater than this value, it is split between fighters.
	// i.e. a reaper (2 OP/missile) is split between two fighters, 5 OP/2 missiles would see a similar split.
	// private static final float MISSILE_OP_MAX = 1f;
	// private static final int BURST_MAX = 25;
	public static final int OP_PER_WING = 3;
	public static final int OP_PER_WING_SMALL = 3;
	public static final int FIGHTERS_PER_WING = 3;
	public static final int BURSTS_PER_FIGHTER = 4;
	public static final int MAX_MISSILE_OP = 6;
	private static final float MIN_RANGE = 150f;
	private static final float UNGUIDED_RADIUS_THRESHOLD = 0.7f;
	// 0 = initialize, 1 = still bursting missiles, 2 = finished
	
	private static final boolean SHOULD_RECALL_AFTER = true;
	
	private int useState = 0;
	private int usableState = -1;
	
	private List<MissileToFire> shipMissiles;
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
				// don't fire dumbfires at other fighters, that is a horrendous idea.  except for modded pd shit idfk
				if (target == null || target.getOwner() == ship.getOwner() || target.isFighter())
					return false;
				// need to account for how fast many fighters travel.  the relative math here works out
				Vector2f ray = new Vector2f(
					(float) FastTrig.cos(Math.toRadians(fighter.getFacing())) * spawnMissile.getProjectileSpeed()
						+ fighter.getVelocity().x, 
					(float) FastTrig.sin(Math.toRadians(fighter.getFacing())) * spawnMissile.getProjectileSpeed()
						+ fighter.getVelocity().y);
				ray.normalise();
				ray.scale(spawnMissile.getRange() * 1.15f);
				// we could derive for local minima to get the actual travel time considering both velocities, but this isn't necessary smile.
				float travelTime = (Vector2f.sub(
					fighter.getLocation(), 
					target.getLocation(), new Vector2f()).length() - target.getShieldRadiusEvenIfNoShield() * UNGUIDED_RADIUS_THRESHOLD) / 
					spawnMissile.getProjectileSpeed();
				if (!CollisionUtils.getCollides(
					fighter.getLocation(), 
					Vector2f.add(fighter.getLocation(), ray, new Vector2f()),
					Vector2f.add(target.getLocation(), 
						new Vector2f(target.getVelocity().getX() * travelTime, target.getVelocity().getY() * travelTime), new Vector2f()),
					target.getShieldRadiusEvenIfNoShield() * UNGUIDED_RADIUS_THRESHOLD
				))
					return false;
				// collision grid check for ff considerations 
				Iterator<Object> shipGridIterator = combatEngine.getAiGridShips().getCheckIterator(
					Vector2f.add(fighter.getLocation(), (Vector2f) ray.normalise().scale(MathUtils.getDistance(
						fighter.getLocation(), target.getLocation()) * 0.5f
					), new Vector2f()),
					Math.abs(target.getLocation().x - fighter.getLocation().x), 
					Math.abs(target.getLocation().y - fighter.getLocation().y)
				);
				ray.scale(1.5f);
				Vector2f.add(fighter.getLocation(), ray, ray);
				while (shipGridIterator.hasNext()) {
					ShipAPI currShip = (ShipAPI) shipGridIterator.next();
					if (currShip.getHullSize() != HullSize.FIGHTER && 
						currShip.getOwner() == ship.getOwner()) {
						float ffTravel = Vector2f.sub(
							fighter.getLocation(), 
							currShip.getLocation(), new Vector2f()).length() / spawnMissile.getProjectileSpeed();
						if (CollisionUtils.getCollides(
							fighter.getLocation(), 
							ray,
							Vector2f.add(currShip.getLocation(), 
								new Vector2f(currShip.getVelocity().getX() * ffTravel, currShip.getVelocity().getY() * ffTravel), 
									new Vector2f()),
							currShip.getShieldRadiusEvenIfNoShield()
							))
							return false;
					}
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
	
	public static MissileToFire calcMissile(WeaponAPI weapon) {
		int fpm = 99;
		int missileCount = weapon.getSpec().getBurstSize();
		float refireDelay = 0f;
		float missilesPerMinute = -1f;
		
		if (weapon.usesAmmo() && weapon.getAmmoPerSecond() <= 0f) {
			fpm = Math.max(1,
				(int) Math.floor(weapon.getSpec().getOrdnancePointCost(null)
				/ weapon.getMaxAmmo() * weapon.getSpec().getBurstSize())
			);
			if (fpm == 1 && weapon.getSpec().getBurstSize() > 1)
				missileCount = Math.min(weapon.getSpec().getBurstSize() * BURSTS_PER_FIGHTER,
					Math.max((int) (weapon.getMaxAmmo() / weapon.getSpec().getOrdnancePointCost(null)),
					weapon.getSpec().getBurstSize())
				);
		} else if (weapon.usesAmmo() && weapon.getAmmoPerSecond() > 0f) {
			// pilum: 0.1 ammo/sec (6 shots/min), 7 OP, burst size 3
			// fpm: 
			// pilum lrm: 0.33 ammo/sec (20 shots/min), 14 OP, burst size 6
			// fpm: 
			// antimatter srm: 0.05 ammo/sec (3 shots/min), 7 op, burst size 1
				// fpm: 1.4
			// restonator: 0.4 ammo/sec (24 shots/min), 16 op, burst size 4
				// fpm: 1.6
			missilesPerMinute = weapon.getSpec().getAmmoPerSecond() / weapon.getSpec().getBurstSize() * 60f;
			float fpmf = 1f / 100f / weapon.getSpec().getAmmoPerSecond() * weapon.getSpec().getOrdnancePointCost(null)
				* weapon.getSpec().getBurstSize();
			if (fpmf < 1f) {
				missileCount = Math.max(weapon.getSpec().getBurstSize(),
					(int) (1f / fpmf * weapon.getSpec().getBurstSize())
				);
				fpm = 1;
			} else
				fpm = Math.round(fpmf);
		} else if (weapon.getRefireDelay() > 0f){
			// salamanders go here - just using their 25 second reload as a base to limit really obnoxious modded missiles
			fpm = Math.max(1, Math.round(weapon.getRefireDelay() / 25f));
			missilesPerMinute = 60f / weapon.getRefireDelay();
			refireDelay = weapon.getRefireDelay() / 2f;
			if (fpm == 1) {
				missileCount = Math.max(weapon.getSpec().getBurstSize(),
				Math.round(25f / weapon.getRefireDelay()));
			}
		}
		return new MissileToFire(weapon, missileCount, fpm, refireDelay, missilesPerMinute);
	}
					
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		
		if (stats.getEntity() == null)
			return;
		
		if (useState == 2 && state != State.OUT) {
			ship.useSystem();
			return;
		}
		
		if (useState == 0) {
			if (ship == null)
				ship = (ShipAPI) stats.getEntity();
			
			// slightly erroneous variables, but as missiles cannot cost less than 1 OP it works out
			int remainingOP = (ship.getHullSize() == HullSize.CRUISER ? OP_PER_WING : OP_PER_WING_SMALL) *
				ship.getHullSpec().getFighterBays();
			shipMissiles = new ArrayList<MissileToFire>();
			for (WeaponAPI weapon : ship.getAllWeapons()) {
				if (weapon.getType() == WeaponType.MISSILE && !weapon.isPermanentlyDisabled() &&
					!weapon.getSpec().hasTag(Tags.FRAGMENT)) {
					MissileToFire missile = calcMissile(weapon);
					if (missile.cost < Math.min(MAX_MISSILE_OP, OP_PER_WING * ship.getHullSpec().getFighterBays()) &&
						missile.cost < remainingOP) {
						shipMissiles.add(missile);
					}
				}
			}
			if (shipMissiles.size() < 1) {
				useState = 2;
				return;
			}
			
			int missileIndex = 0;
			int fightersMax = Math.min(shipMissiles.size(), FIGHTERS_PER_WING);
			int remainingMissiles = shipMissiles.size() * ship.getHullSpec().getFighterBays();
			shipFighters = new ArrayList<ShipAPI>();

			// get the first [num equipped missles] fighters of each wing, to alternate between missiles within wings
			for (FighterWingAPI thisWing : ship.getAllWings()) {
				for (int i = 0; i < fightersMax && i < thisWing.getWingMembers().size()
					&& remainingMissiles > 0; i++) {
					ShipAPI thisWingFighter = thisWing.getWingMembers().get(i);
					if (thisWingFighter != null &&
						thisWingFighter.isAlive() && !thisWingFighter.isHulk() &&
						!thisWing.isReturning(thisWingFighter)){
						shipFighters.add(thisWingFighter);
						remainingMissiles--;
					}
				}
			}
			
			// alternate between wings to assign remaining fighters
			if (shipFighters.size() < remainingOP && remainingMissiles > 0) {
				int maxSize = 0;
				for (FighterWingAPI thisWing : ship.getAllWings()) {
					if (thisWing.getWingMembers().size() > maxSize)
						maxSize = thisWing.getWingMembers().size();
				}
				for (int i = FIGHTERS_PER_WING; i < maxSize && shipFighters.size() < remainingOP
					&& remainingMissiles > 0; i++) {
					for (FighterWingAPI thisWing : ship.getAllWings()) {
						if (i < thisWing.getWingMembers().size() && shipFighters.size() < remainingOP && remainingMissiles > 0) {
							ShipAPI thisWingFighter = thisWing.getWingMembers().get(i);
							if (thisWingFighter != null &&
								thisWingFighter.isAlive() && !thisWingFighter.isHulk() &&
								!thisWing.isReturning(thisWingFighter)){
								shipFighters.add(thisWingFighter);
								remainingMissiles--;
							}
						}
					}
				}
			}
			
			CombatEngineAPI combatEngine = Global.getCombatEngine();
			
			if (shipFighters.size() > 0) {
				mFighters = new LinkedList<mFighter>();
			} else
				useState = 2;
			
			// weaponIndex.size() = amount of fighters left still bursting, as we add an entry for each fighter.
			while (useState != 2 && shipFighters.size() > 0 && remainingOP > 0 && shipMissiles.size() > 0) {
				if (remainingOP < shipMissiles.get(missileIndex).cost) {
					shipMissiles.remove(missileIndex);
					if (shipMissiles.size() == 0)
						break;
					else if (missileIndex >= shipMissiles.size())
						missileIndex = 0;
					continue;
				} else
					remainingOP -= shipMissiles.get(missileIndex).cost;
				for (int i = 1; i < shipMissiles.get(missileIndex).cost; i++) {
					shipFighters.add(shipFighters.remove(0));
				}
				
				OnFireEffectPlugin thisPlugin = null;
				if (((MissileSpecAPI) shipMissiles.get(missileIndex).missileWep.getSpec().getProjectileSpec()).getOnFireEffect() != null)
					thisPlugin = ((MissileSpecAPI) shipMissiles.get(missileIndex).missileWep.getSpec().getProjectileSpec()).getOnFireEffect();
				boolean fired = fireMissileFighter(
					shipMissiles.get(missileIndex).missileWep,
					(ShipAPI) shipFighters.get(0),
					0,
					((MissileSpecAPI) shipMissiles.get(missileIndex).missileWep.getSpec().getProjectileSpec())
						.getDamage().getBaseDamage(),
					thisPlugin, combatEngine,
					false
				);
				if (shipMissiles.get(missileIndex).count > 1 || !fired) {
					mFighters.add(new mFighter(
						(ShipAPI) shipFighters.get(0),
						shipMissiles.get(missileIndex).missileWep,
						shipMissiles.get(missileIndex).count,
						fired ? 1 : 0,
						fired,
						shipMissiles.get(missileIndex).refireDelay > 0f ? shipMissiles.get(missileIndex).refireDelay :
							shipMissiles.get(missileIndex).missileWep.getDerivedStats().getBurstFireDuration() /
							shipMissiles.get(missileIndex).missileWep.getSpec().getBurstSize() / 2f,
						thisPlugin
					));
				} else { // used to be optimized to run on lowest burst interval & throw out
					// probably needlessly aggressive.  want to add an entry here anyway to get the visual flash
					// not really a perf concern
					mFighters.add(new mFighter((ShipAPI) shipFighters.get(0)));
					if (SHOULD_RECALL_AFTER &&
						!((ShipAPI) shipFighters.get(0)).getWing().getRole().equals(WingRole.BOMBER) &&
						!((ShipAPI) shipFighters.get(0)).getWing().getRole().equals(WingRole.SUPPORT)) {
						((ShipAPI) shipFighters.get(0)).getWing().orderReturn(((ShipAPI) shipFighters.get(0)));
					}
				}
				shipFighters.remove(0);
				missileIndex++;
				if (missileIndex >= shipMissiles.size())
					missileIndex = 0;
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
							fighter.cumulative+= 1;
							if (fighter.count - fighter.cumulative == 0 && SHOULD_RECALL_AFTER &&
								!fighter.fighter.getWing().getRole().equals(WingRole.BOMBER)) {
								fighter.fighter.getWing().orderReturn(fighter.fighter);
							}
						}
					} else {
						fighter.burstTime -= combatEngine.getElapsedInLastFrame();
						if (fighter.burstTime <= 0f && fighter.count - fighter.cumulative > 0) {
							boolean fired = fireMissileFighter(
								fighter.missileWep, 
								fighter.fighter, 
								fighter.cumulative,
								fighter.baseDamage, fighter.projEffectPlugin, combatEngine, true);
							if (fired) {
								fighter.cumulative+= 1;
							}
							else
								fighter.burstTime = 0.1f;
							if (fighter.count - fighter.cumulative > 0)
								fighter.burstTime = Math.max(fighter.burstTime + fighter.timePerShot, 0f);
							else if (SHOULD_RECALL_AFTER &&
								!fighter.fighter.getWing().getRole().equals(WingRole.BOMBER)) {
								fighter.fighter.getWing().orderReturn(fighter.fighter);
							}
						}
					}
					
					float fadeOut = fighter.count - fighter.cumulative == 0 ? 2f + fighter.burstTime * 2f : effectLevel;
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
		shipMissiles = null;
		shipFighters = null;
		mFighters = null;
	}
	
	/*
	@Override
	public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
		if (usableState == 0)
			return "INVALID LOADOUT";
		
		if (system.getState().equals(SystemState.IDLE))
			return "READY";
		else if (system.getState().equals(SystemState.ACTIVE))
			return "ACTIVE";
		else
			return "";
	}
	
	@Override
	public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
		if (usableState == -1) {
			if (ship.getAllWings().size() <= 0) {
				usableState = 0;
				return false;
			}
			for (WeaponAPI wep : ship.getAllWeapons()) {
				if (wep.isPermanentlyDisabled() || wep.isDecorative() ||
					wep.getSpec().hasTag(Tags.FRAGMENT) ||
					!wep.getType().equals(WeaponType.MISSILE))
					continue;
				MissileToFire missile = calcMissile(wep);
				if (missile.cost < Math.min(MAX_MISSILE_OP, 
					(ship.getHullSize().ordinal() < HullSize.CRUISER.ordinal() ? OP_PER_WING :
						OP_PER_WING_SMALL)
						* ship.getAllWings().size())) {
					usableState = 1;
					return true;
				}
			}
		} else if (usableState == 1)
			return true;
		return false;
	}
	*/
	

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
