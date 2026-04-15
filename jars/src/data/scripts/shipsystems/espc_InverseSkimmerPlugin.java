package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;
import com.fs.starfarer.api.combat.WeaponAPI.AIHints;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.MissileSpecAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.util.MezzUtils;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.json.JSONException;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;
				
public class espc_InverseSkimmerPlugin extends BaseEveryFrameCombatPlugin {

	private static final float START_DURATION = 1.5f;
	private static final float START_DURATION_CAPITAL = 2.5f;
	private static final float DURATION = 5f + START_DURATION;
	private static final float DURATION_CAPITAL = 8f + START_DURATION_CAPITAL;
	private static final float AI_FORCEFIRE_DELAY = 0.55f;
	private static final float INTERCEPT_RANGE = 150f;

    private static final float ALLY_FLUX_THRESHOLD = 0.8f;
    private static final float ALLY_HARDFLUX_THRESHOLD = 0.5f;

	private static final float PORTAL_LENGTH = 25f;
	private static final float PORTAL_HEIGHT = 35f;
	private static final float FIGHTER_DELAY_INTERVAL = 0.2f;
	
	private ShipAPI ship;
	private CombatEngineAPI engine;
	private boolean isCapital = false;
	
	private boolean toRemove = false;
	float timeLast = 0f;
	float fighterDelay = 0f;
	
	private ArrayDeque<SkimmerSummonInstance> allies;
	private ArrayDeque<String> fighterIds;
	
	private class SkimmerSummonInstance {
		ShipAPI ally;
		public float startTime = 0f;
		public float delay = 1f;
		public boolean shouldUseSystem = false;
		public boolean pdOnly = false;
		public ArrayDeque<WeaponAPI> weapons;
		public ArrayDeque<WeaponAPI> missiles;
		public LinkedList<String> weaponIds;
		public LinkedList<Vector2f> portals;
		public LinkedList<Float> portalRots;
		public Long randSeed;
		
		private SkimmerSummonInstance(ShipAPI allyShip) {
			this.ally = allyShip;
			this.startTime = engine.getTotalElapsedTime(false);
			this.delay = AI_FORCEFIRE_DELAY + Misc.random.nextFloat() * AI_FORCEFIRE_DELAY;
			this.weapons = new ArrayDeque<WeaponAPI>();
			this.missiles = new ArrayDeque<WeaponAPI>();
			this.weaponIds = new LinkedList<String>();
			this.portals = new LinkedList<Vector2f>();
			this.portalRots = new LinkedList<Float>();
			this.randSeed = Misc.random.nextLong();
			
			// TODO: test w/ ships that would have no portals.  missiles only included
			// test w/ slamfire ai
			for (WeaponAPI wep : ally.getAllWeapons()) {
				if (!wep.isDecorative() && !wep.isBeam() && !wep.getSlot().isSystemSlot())
					if (wep.getType().equals(WeaponType.MISSILE) && wep.getDamageType() != DamageType.KINETIC
						&& wep.getDamageType() != DamageType.OTHER)
						missiles.add(wep);
					else if (!(wep.hasAIHint(AIHints.PD) && !wep.hasAIHint(AIHints.PD_ALSO))) {
						if (wep.getSize().equals(WeaponSize.LARGE))
							weaponIds.add(wep.getSlot().getId());
						else if (!weaponIds.contains(wep.getSpec().getWeaponId()))
							weaponIds.add(wep.getSpec().getWeaponId());
						weapons.add(wep);
					}
			}
			if (weapons.size() == 0) {
				pdOnly = true;
				for (WeaponAPI wep : ally.getAllWeapons()) {
					if (!wep.isDecorative() && !wep.isBeam() && !wep.getSlot().isSystemSlot())
						if (wep.getType().equals(WeaponType.MISSILE) && !missiles.contains(wep)
							&& wep.getDamageType() != DamageType.KINETIC
							&& wep.getDamageType() != DamageType.OTHER)
							missiles.add(wep);
						else if (!wep.getType().equals(WeaponType.MISSILE)) {
							if (wep.getSize().equals(WeaponSize.LARGE))
								weaponIds.add(wep.getId());
							else if (!weaponIds.contains(wep.getSpec().getWeaponId()))
								weaponIds.add(wep.getSpec().getWeaponId());
							weapons.add(wep);
						}
				}
			}
			
			if (ally.getSystem() != null) {
				String systemAI = "";
				try {
					systemAI = ship.getSystem().getSpecAPI().getSpecJson().getString("aiType");
				} catch (JSONException e) {
					systemAI = "null";
				}
				if ((systemAI.equals("WEAPON_BOOST") && !ship.getSystem().getDisplayName().equals("Shieldwall")) ||
					ship.getSystem().getDisplayName().equals("Slamfire")) {
					shouldUseSystem = true;
				}
			}	
		}
	}
	
	public espc_InverseSkimmerPlugin(ShipAPI ship) {
		this.ship = ship;
		this.engine = Global.getCombatEngine();
		this.allies = new ArrayDeque<SkimmerSummonInstance>();
		this.fighterIds = new ArrayDeque<String>();
		this.isCapital = ship.getHullSize().equals(HullSize.CAPITAL_SHIP);
		timeLast = engine.getTotalElapsedTime(false);
	}
	
	public void addAlly(ShipAPI ally) {
		fighterIds.clear();
		SkimmerSummonInstance summon = new SkimmerSummonInstance(ally);
		if (summon.weapons.size() > 0)
			allies.add(summon);
	    	ally.setCustomData("espc_InverseSkimmer_Ally", true);
	}
	
	public ShipAPI getBestTarget() {
		ShipAPI targ = ship.getShipTarget();
		if (targ != null) {
    		if (ship.getOwner() == targ.getOwner() || targ.isFighter()
    			|| !targ.isAlive() || targ.isHulk() || targ.isPhased())
        		targ = null;
    		else {
    			Vector2f ray = new Vector2f(
    				(float) FastTrig.cos(Math.toRadians(ship.getFacing())),
    				(float) FastTrig.sin(Math.toRadians(ship.getFacing())));
    			ray.scale(MathUtils.getDistanceSquared(ship.getLocation(), targ.getLocation()));
    			if (!CollisionUtils.getCollides(ship.getLocation(), 
    				Vector2f.add(ship.getLocation(), ray, new Vector2f()),
        			targ.getLocation(), targ.getShieldRadiusEvenIfNoShield()))
    				targ = null;
    		}
		}


		if (targ == null) {
			if (ship.getShipAI() != null) {
				Object test = ship.getAIFlags().getCustom(AIFlags.MANEUVER_TARGET);
				if (test instanceof ShipAPI) {
					targ = (ShipAPI) test;
		    		if (ship.getOwner() == targ.getOwner() || targ.isFighter()
		    			|| !targ.isAlive() || targ.isHulk() || targ.isPhased())
		        		targ = null;
		    		else {
		    			Vector2f ray = new Vector2f(
		    				(float) FastTrig.cos(Math.toRadians(ship.getFacing())),
		    				(float) FastTrig.sin(Math.toRadians(ship.getFacing())));
		    			ray.scale(MathUtils.getDistanceSquared(ship.getLocation(), targ.getLocation()));
		    			if (!CollisionUtils.getCollides(ship.getLocation(), 
		    				Vector2f.add(ship.getLocation(), ray, new Vector2f()),
		        			targ.getLocation(), targ.getShieldRadiusEvenIfNoShield()))
		    				targ = null;
		    		}
				}
			} else {
				float closest = 1800f;
				// find enemy target for acceptable autofire, closest target.  raycast time!!!  wooo!!!
    			Vector2f ray = new Vector2f(
	    			(float) FastTrig.cos(Math.toRadians(ship.getFacing())),
	    			(float) FastTrig.sin(Math.toRadians(ship.getFacing())));
    			ray.scale(1800f);
        		Iterator<Object> shipGridIterator = (Iterator<Object>) (engine.getAiGridShips().getCheckIterator(
            		new Vector2f(ship.getLocation().x + ray.x/2f, ship.getLocation().y + ray.y/2f),
            		Math.abs(ray.x),
            		Math.abs(ray.y))
            	);
        		while (shipGridIterator.hasNext()) {
        			ShipAPI currShip = (ShipAPI) shipGridIterator.next();
        			if (currShip.isShuttlePod() || currShip == ship || currShip.hasTag(Tags.VARIANT_FX_DRONE)
        				|| currShip.isFighter()
        				|| (currShip.getHullSize().equals(HullSize.FRIGATE) && !isCapital)
        				|| currShip.isPhased())
        				continue;
        			if (CollisionUtils.getCollides(ship.getLocation(), 
        				new Vector2f(ship.getLocation().x + ray.x, ship.getLocation().y + ray.y),
                		currShip.getLocation(), currShip.getShieldRadiusEvenIfNoShield() * 1.1f) &&
        				MathUtils.getDistanceSquared(ship, currShip) < closest * closest) {
        				if (currShip.isHulk() || !currShip.isAlive()) {
        					if (!isCapital) {
                				closest = MathUtils.getDistance(ship, currShip);
        						targ = null;
        					}
        					continue;
        				}
    					targ = (ship.getOwner() != currShip.getOwner()) ? currShip : null;
        			}
        		}
			}
		}
		return targ;
	}
	
	public void calculatePortals(float time) {
		// max width: 320
		int portalCount = 0;
		LinkedList<Integer> portalIndexes = new LinkedList<Integer>();
		for (SkimmerSummonInstance ally : allies)
			if (ally.startTime == time)
				portalCount += ally.weaponIds.size();
		
		for (int i = 0; i < portalCount; i++)
			portalIndexes.add(i);
		
		for (SkimmerSummonInstance ally : allies)
			if (ally.startTime == time)
				for (int i = 0; i < ally.weaponIds.size(); i++) {
					int index = portalIndexes.remove(Misc.random.nextInt(portalIndexes.size()));
					float x = (index - 
						(portalCount - 1f) / 2f) * (portalCount > 9 ? 15f : 25f) +
						Misc.random.nextFloat() * 10f - 5f;
					ally.portals.add(
						new Vector2f(ship.getCollisionRadius() * 1.1f + Misc.random.nextFloat() * 10f +
							MezzUtils.halfSineIn(
								(portalCount > 4 ? Math.abs((float) index - portalCount/2f) / (float) portalCount : 0f)
							) * 100f + (portalCount > 9 && index % 2 == 1 ? 20f : 0f),
							x));
					ally.portalRots.add(x * 0.04f);
				}
	}
	
	public void cancelSystem() {
	    ship.removeCustomData("espc_InverseSkimmer_Ally");
	    boolean anyRemove = false;
    	for (SkimmerSummonInstance ally : allies) {
    		anyRemove = true;
    		ally.ally.removeCustomData("espc_InverseSkimmer_Ally");
    		for (Vector2f pos : ally.portals) {
				engine.addHitParticle(
					new Vector2f(ship.getLocation().x + 
						(float) FastTrig.cos(Math.toRadians(ship.getFacing())) * 
						pos.x +
						(float) FastTrig.sin(Math.toRadians(ship.getFacing())) * 
						pos.y,
						ship.getLocation().y - 
						(float) FastTrig.cos(Math.toRadians(ship.getFacing())) * 
						pos.y +
						(float) FastTrig.sin(Math.toRadians(ship.getFacing())) * 
						pos.x),
					ship.getVelocity(),
					80f,
					0.45f,
					new Color(50, 100, 255, 100)
					);
	   			engine.addHitParticle(
	       			new Vector2f(ship.getLocation().x + 
	       				(float) FastTrig.cos(Math.toRadians(ship.getFacing())) * 
	       				pos.x +
	        				(float) FastTrig.sin(Math.toRadians(ship.getFacing())) * 
	       				pos.y,
	       				ship.getLocation().y - 
	       				(float) FastTrig.cos(Math.toRadians(ship.getFacing())) * 
	        				pos.y +
	       				(float) FastTrig.sin(Math.toRadians(ship.getFacing())) * 
	       				pos.x),
	       			ship.getVelocity(),
	       			50f,
	       			0.8f,
	        			new Color(120, 160, 255, 100)
	       		);
    	    }
    	}
    	if (anyRemove) {
    		Global.getSoundPlayer().playSound("system_phase_skimmer", 1f, 1f, ship.getLocation(), ship.getVelocity());
    		allies.clear();
    	}
	}
	
    @Override
	public void advance(float amount, List<InputEventAPI> events) {
    	
    	if (ship == null || !engine.isInPlay(ship)) {
			engine.removePlugin(this);
			return;
    	}
    	if (ship.isHulk() || !ship.isAlive())
    		toRemove = true;

    	boolean cancelSystem = toRemove ||
    		ship.getFluxTracker().isOverloadedOrVenting();
    	
    	ShipAPI targ = getBestTarget();
    	
    	if (fighterDelay > 0f)
    		fighterDelay -= amount;
    	
    	boolean shouldAutofire = targ != null;
    	if (shouldAutofire) {
    		Vector2f ray = Vector2f.sub(targ.getLocation(), ship.getLocation(), new Vector2f());
    		Iterator<Object> shipGridIterator = (Iterator<Object>) (engine.getAiGridShips().getCheckIterator(
        		new Vector2f(ship.getLocation().x + ray.x/2f, ship.getLocation().y + ray.y/2f),
        		Math.abs(ray.x),
        		Math.abs(ray.y))
        	);

    		while (shipGridIterator.hasNext() && shouldAutofire) {
    			ShipAPI currShip = (ShipAPI) shipGridIterator.next();
    			
    			if (currShip.isShuttlePod() || currShip == ship || currShip.hasTag(Tags.VARIANT_FX_DRONE)
    				|| currShip.getOwner() != ship.getOwner()
    				|| currShip.isFighter()
    				|| (currShip.getHullSize().equals(HullSize.FRIGATE) && !isCapital)
    				// we don't really want to waste flux on shooting hulks.  probably.
    				|| currShip.isHulk() || !currShip.isAlive()
    				|| currShip.isPhased())
    				continue;
    			if (CollisionUtils.getCollides(ship.getLocation(), 
        			targ.getLocation(),
            		currShip.getLocation(), currShip.getShieldRadiusEvenIfNoShield() * 1.1f))
    				shouldAutofire = false;
    		}
    	}
    	
    	boolean anyRemove = false;
    	Iterator<SkimmerSummonInstance> allyIter = allies.iterator();
    	while (allyIter.hasNext()) {
    		SkimmerSummonInstance ally = allyIter.next();

        	if (cancelSystem || engine.getTotalElapsedTime(false) - ally.startTime > 
        	(isCapital ? DURATION_CAPITAL : DURATION)) {
    		    ally.ally.removeCustomData("espc_InverseSkimmer_Ally");
    		    for (Vector2f pos : ally.portals) {
					engine.addHitParticle(
						new Vector2f(ship.getLocation().x + 
							(float) FastTrig.cos(Math.toRadians(ship.getFacing())) * 
							pos.x +
							(float) FastTrig.sin(Math.toRadians(ship.getFacing())) * 
							pos.y,
							ship.getLocation().y - 
							(float) FastTrig.cos(Math.toRadians(ship.getFacing())) * 
							pos.y +
							(float) FastTrig.sin(Math.toRadians(ship.getFacing())) * 
							pos.x),
						ship.getVelocity(),
						80f,
						0.45f,
						new Color(50, 100, 255, 100)
					);
	    			engine.addHitParticle(
	        			new Vector2f(ship.getLocation().x + 
	        				(float) FastTrig.cos(Math.toRadians(ship.getFacing())) * 
	        				pos.x +
	        				(float) FastTrig.sin(Math.toRadians(ship.getFacing())) * 
	        				pos.y,
	        				ship.getLocation().y - 
	        				(float) FastTrig.cos(Math.toRadians(ship.getFacing())) * 
	        				pos.y +
	        				(float) FastTrig.sin(Math.toRadians(ship.getFacing())) * 
	        				pos.x),
	        			ship.getVelocity(),
	        			50f,
	        			0.8f,
	        			new Color(120, 160, 255, 100)
	        		);
    		    }
    		    allyIter.remove();
    		    anyRemove = true;
    		    if (allies.size() == 0)
    			    ship.removeCustomData("espc_InverseSkimmer_Ally");
    		    continue;
        	}
        	
    		ally.ally.setJitterUnder(this, new Color(50, 100, 255, 170), 1f, 2, 20f);
    		
    		if (ally.ally.getFluxTracker().isOverloadedOrVenting() || ally.ally.isPhased())
    			return;
        	
        	if (ally.shouldUseSystem && engine.getTotalElapsedTime(false) >= ally.startTime + ally.delay &&
        		ally.ally.getSystem().canBeActivated()) {
        		ally.ally.useSystem();
        		ally.shouldUseSystem = false;
        	}
        	
        	if (engine.getTotalElapsedTime(false) < ally.startTime + (isCapital ? START_DURATION_CAPITAL : START_DURATION))
        		continue;
        	
    		Iterator<Object> entityIterator = engine.getAllObjectGrid().getCheckIterator(
    			ally.ally.getLocation(), 
    			(INTERCEPT_RANGE + ally.ally.getShieldRadiusEvenIfNoShield()) * 2f,
    			(INTERCEPT_RANGE + ally.ally.getShieldRadiusEvenIfNoShield()) * 2f
    		);
    		while (entityIterator.hasNext()) {
    			CombatEntityAPI entity = (CombatEntityAPI) entityIterator.next();
    			if (!(entity instanceof DamagingProjectileAPI)) {
    				if (entity instanceof ShipAPI && fighterDelay <= 0f) {
    					ShipAPI checkShip = (ShipAPI) entity;
    					if (checkShip.isFighter() && 
    					(checkShip.getOwner() == ship.getOwner() || isCapital) && MathUtils.getDistanceSquared(ship, entity) >
    					(ship.getShieldRadiusEvenIfNoShield() + 250f) * 
    					(ship.getShieldRadiusEvenIfNoShield() + 250f) &&
    					!fighterIds.contains(checkShip.getId())) {
    						int portalIndex = Misc.random.nextInt(ally.portals.size());
    						Vector2f spawnPos = new Vector2f(ship.getLocation().x + 
        	    				(float) FastTrig.cos(Math.toRadians(ship.getFacing())) * 
        	    				ally.portals.get(portalIndex).x +
        	    				(float) FastTrig.sin(Math.toRadians(ship.getFacing())) * 
        	    				ally.portals.get(portalIndex).y,
        	    				ship.getLocation().y - 
        	    				(float) FastTrig.cos(Math.toRadians(ship.getFacing())) * 
        	    				ally.portals.get(portalIndex).y +
        	    				(float) FastTrig.sin(Math.toRadians(ship.getFacing())) * 
        	    				ally.portals.get(portalIndex).x);
    						engine.addHitParticle(
    							checkShip.getLocation(),
								ship.getVelocity(),
								110f,
								0.45f,
								0.3f,
								new Color(50, 100, 255, 120
								)
        					);
        			        engine.addHitParticle(
        			        	checkShip.getLocation(),
								ship.getVelocity(),
								80f,
								0.45f,
								0.3f,
								new Color(200, 235, 255, 200
								)
        			        );
        					engine.addHitParticle(
								spawnPos,
								ship.getVelocity(),
								110f,
								0.45f,
								0.3f,
								new Color(50, 100, 255, 120
								)
        					);
        					engine.addHitParticle(
								spawnPos,
								ship.getVelocity(),
								80f,
								0.45f,
								0.3f,
								new Color(200, 235, 255, 200
								)
        					);
    						Vector2f.add(checkShip.getLocation(),
    							new Vector2f(spawnPos.x - checkShip.getLocation().x,
    								spawnPos.y - checkShip.getLocation().y), 
    								checkShip.getLocation());

        					Global.getSoundPlayer().playSound("system_phase_skimmer", 
        						1f, 0.25f, checkShip.getLocation(), checkShip.getVelocity());
        					
    						if (checkShip.getOwner() == ship.getOwner())
    							fighterIds.add(checkShip.getId());
    						else if (checkShip.getEngineController() != null)
    								checkShip.getEngineController().forceFlameout();
    						
    						fighterDelay = Misc.random.nextFloat() * FIGHTER_DELAY_INTERVAL - 0.04f;
    					}
    				}
    				continue;
    			}
    			
    			DamagingProjectileAPI thisProj = (DamagingProjectileAPI) entity;
    			if ((thisProj.getElapsed() < 0.05f &&
    				thisProj.getVelocity().lengthSquared() < 4500 * 4500) ||
    				thisProj.getElapsed() > 0.2f)
    				continue;
    			if (engine.isInPlay(thisProj) && 
    				thisProj.getSource().equals(ally.ally) &&
    				!thisProj.didDamage() && !thisProj.isExpired() && thisProj.getWeapon() != null &&
    				!thisProj.getCustomData().containsKey("espc_InverseSkimmer_Proj")) {
    				if (MathUtils.isWithinRange(ally.ally.getLocation(), thisProj.getLocation(), 
    					INTERCEPT_RANGE + ally.ally.getShieldRadiusEvenIfNoShield())) {
    					if (ally.pdOnly ||
    						thisProj.getWeapon().hasAIHint(AIHints.PD_ALSO) || !thisProj.getWeapon().hasAIHint(AIHints.PD)) {
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
    						
    						float projDamage = thisProj.getDamageAmount();
    						
    						int portalIndex = isMissile ? Misc.random.nextInt(ally.portals.size()) :
    							(thisProj.getWeapon().getSize().equals(WeaponSize.LARGE) ? 
    								ally.weaponIds.indexOf(thisProj.getWeapon().getSlot().getId()) :
    								ally.weaponIds.indexOf(thisProj.getWeapon().getSpec().getWeaponId()));
    							
    						Vector2f spawnPos = new Vector2f(ship.getLocation().x + 
    							(float) FastTrig.cos(Math.toRadians(ship.getFacing())) * (ship.getCollisionRadius() + 1f),
    							ship.getLocation().y + 
    							(float) FastTrig.sin(Math.toRadians(ship.getFacing())) * (ship.getCollisionRadius() + 1f));
    						float facing = ship.getFacing() +
    							thisProj.getFacing() - thisProj.getWeapon().getCurrAngle();
    						
    						if (portalIndex > -1) {
    							spawnPos = new Vector2f(ship.getLocation().x + 
    	    						(float) FastTrig.cos(Math.toRadians(ship.getFacing())) * 
    	    						ally.portals.get(portalIndex).x +
    	    						(float) FastTrig.sin(Math.toRadians(ship.getFacing())) * 
    	    						ally.portals.get(portalIndex).y,
    	    						ship.getLocation().y - 
    	    						(float) FastTrig.cos(Math.toRadians(ship.getFacing())) * 
    	    						ally.portals.get(portalIndex).y +
    	    						(float) FastTrig.sin(Math.toRadians(ship.getFacing())) * 
    	    						ally.portals.get(portalIndex).x);
    							facing += ally.portalRots.get(portalIndex);
    						}
    						
    						DamagingProjectileAPI spawnProj = (DamagingProjectileAPI) engine.spawnProjectile(
    							ally.ally, 
    							thisProj.getWeapon(),
    							thisProj.getWeapon().getId(),
    							spawnPos, 
    							facing, 
    							ship.getVelocity());
    						
    						spawnProj.setDamageAmount(thisProj.getDamageAmount());
    						spawnProj.setHitpoints(thisProj.getHitpoints());
    						spawnProj.setCustomData("espc_InverseSkimmer_Proj", true);
    						
    						if (thisProj.getWeapon().getEffectPlugin() != null &&
    							thisProj.getWeapon().getEffectPlugin() instanceof OnFireEffectPlugin)
    							((OnFireEffectPlugin) thisProj.getWeapon().getEffectPlugin()).onFire(
    								spawnProj, thisProj.getWeapon(), engine);
    						
    						if (isMissile) {
    							if (((MissileSpecAPI) thisProj.getWeapon().getSpec().getProjectileSpec()).getOnFireEffect() != null)
    								((MissileSpecAPI) thisProj.getWeapon().getSpec().getProjectileSpec()).getOnFireEffect().onFire(
    									spawnProj, thisProj.getWeapon(), engine);
    						} else {
    							if (thisProj.getProjectileSpec().getOnFireEffect() != null)
    								thisProj.getProjectileSpec().getOnFireEffect().onFire(
    									spawnProj, thisProj.getWeapon(), engine);
    						}
    						
    						if (thisProj.getCustomData().size() > 0)
    							for (String key : thisProj.getCustomData().keySet())
    								spawnProj.setCustomData(key, thisProj.getCustomData().get(key));
    						
    						
    						projDamage = Math.min(projDamage, 2000f);
    						engine.addHitParticle(
    			            	thisProj.getLocation(),
    			               	Misc.ZERO,
    			               	50 + projDamage / 30,
    			               	0.6f,
    			               	0.25f + projDamage / 10000f,
    			               	new Color(50, 100, 255, 
    			               		(int) (projDamage / 30f + 60f)
    			               	)
    						);
    			            engine.addHitParticle(
    			               	thisProj.getLocation(),
    			               	Misc.ZERO,
    			               	30 + projDamage / 35,
    			               	1.5f + projDamage / 2000f,
    			               	0.1f + projDamage / 15000f,
    			               	new Color(200, 235, 255, 
    			               		(int) (projDamage / 15f + 120f)
    			               	)
    			            );
    						engine.addHitParticle(
    							spawnPos,
    				           	ship.getVelocity(),
    				           	50 + projDamage / 30,
    				           	0.8f,
    				           	0.25f + projDamage / 10000f,
    				           	new Color(50, 100, 255, 
    				           		(int) (projDamage / 30f + 60f)
    				           		)
    						);
    						engine.addHitParticle(
    							spawnPos,
    							ship.getVelocity(),
    							30 + projDamage / 35,
    							1.5f + projDamage / 2000f,
    							0.1f + projDamage / 15000f,
    							new Color(200, 235, 255, 
    								(int) (projDamage / 15f + 120f)
    							)
    						);
    						engine.removeEntity(thisProj);
    						
    					}
    				}
    			}
    		}
        	
    		// a lot of these autofire checks are already done/implicit to targ != null
        	if (engine.getTotalElapsedTime(false) >= ally.startTime + (isCapital ? START_DURATION_CAPITAL : START_DURATION) &&
        		ally.ally.getShipAI() != null && targ != null &&
        		!targ.isPhased() && targ.isAlive() && !targ.isHulk()) {
        		if (ally.ally.getFluxLevel() < ALLY_FLUX_THRESHOLD && 
        		ally.ally.getHardFluxLevel() < ALLY_HARDFLUX_THRESHOLD &&
        		shouldAutofire)
	    	    	for (WeaponAPI wep : ally.weapons)
	    	    		if (MathUtils.isWithinRange(ship, targ, wep.getRange()))
	    	    			wep.setForceFireOneFrame(true);
        		// only consider for missiles it wouldn't bother firing itself, due to being out of range
        		if (targ.getFluxTracker().isOverloadedOrVenting())
	    	    	for (WeaponAPI wep : ally.missiles)
	    	    		if (!targ.getHullSize().equals(HullSize.FRIGATE) &&
	    	    			!MathUtils.isWithinRange(ally.ally, targ, wep.getRange()) &&
	    	    			(shouldAutofire || wep.getProjectileCollisionClass() != CollisionClass.MISSILE_FF))
	    	    			wep.setForceFireOneFrame(true);
	    	    		
        		
        	}
        	
    	}
    	if (anyRemove && ship != null && engine.isInPlay(ship))
			Global.getSoundPlayer().playSound("system_phase_skimmer", 1f, 1f, ship.getLocation(), ship.getVelocity());
    	if (toRemove)
			engine.removePlugin(this);
    }
    	
    	
    @Override
    public void renderInWorldCoords(ViewportAPI viewport) {
        if (ship == null || !engine.isInPlay(ship))
        	return;
        
        float time = engine.getTotalElapsedTime(false);
		Random jitterRand = new Random((long) (time * 600f));
		
        GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		/*
		SpriteAPI sprite = Global.getSettings().getSprite("graphics/missiles/missile_sabot_warhead.png");
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, sprite.getTextureId());
		GL11.glBegin(GL11.GL_QUADS); */
		
		SpriteAPI sprite2 = Global.getSettings().getSprite("systemMap", "radar_entity");
		
		for (SkimmerSummonInstance ally : allies) {
			float duration = (isCapital ? START_DURATION_CAPITAL : START_DURATION);
			
			float pTime = time - ally.startTime;
			Random particleRand = new Random(ally.randSeed);
			
			for (int i = 0; i < ally.portals.size(); i++) {
				Vector2f pos = 
					new Vector2f(ship.getLocation().x + 
					(float) FastTrig.cos(Math.toRadians(ship.getFacing())) * 
					ally.portals.get(i).x +
					(float) FastTrig.sin(Math.toRadians(ship.getFacing())) * 
					ally.portals.get(i).y,
					ship.getLocation().y - 
					(float) FastTrig.cos(Math.toRadians(ship.getFacing())) * 
					ally.portals.get(i).y +
					(float) FastTrig.sin(Math.toRadians(ship.getFacing())) * 
					ally.portals.get(i).x);
				

				float early = particleRand.nextFloat() * duration * 0.3f;
				if (pTime < duration) {
					GL11.glBindTexture(GL11.GL_TEXTURE_2D, sprite2.getTextureId());
					GL11.glBegin(GL11.GL_QUADS);
					float pDelay = particleRand.nextFloat() * duration * 0.2f;
					
					if (pTime > duration - early &&
						timeLast < ally.startTime + duration - early) {
						Global.getSoundPlayer().playSound("system_phase_skimmer", 
							Misc.random.nextFloat() * 0.2f + 0.9f, 0.3f, pos, ship.getVelocity());
						engine.addHitParticle(
							pos,
							ship.getVelocity(),
							80f,
							0.45f,
							new Color(50, 100, 255, 100)
						);
						engine.addHitParticle(
							pos,
							ship.getVelocity(),
							50f,
							0.8f,
							new Color(120, 160, 255, 100)
						);
					}
					
					for (int g = 0; g < 15; g++) {
						float pVelocity = particleRand.nextFloat() * 6f + 1f;
						float ang = particleRand.nextFloat() * (float) Math.PI * 2f
							+ pTime / duration * pVelocity;
						// 0 -> 1
						// total duration: (duration - (start delay - g * .01) - end)
						float lerp = (pTime - g * 0.03f - pDelay)/
							(duration - pDelay - g * 0.03f - early);
						float radius = (particleRand.nextFloat() * 100f + 80f + 8f * (15 - g))
							* (1f - MezzUtils.halfSineOut(
								lerp
						));
						
						if (lerp <= 0f || lerp >= 1f)
							continue;
						
						GL11.glColor4ub(
							(byte) 50, (byte) 100, (byte) 255, 
							(byte) (230f * lerp));
						
						MezzUtils.glSquare(pos.x + (float) FastTrig.cos(ang) * radius, 
						pos.y + (float) FastTrig.sin(ang * (ally.portals.get(i).y < 0f ? -1f : 1f)) * radius,
						6.5f);
					}

					GL11.glEnd();
				}
				
				if (pTime < duration - early)
					continue;
				SpriteAPI spritePortal = Global.getSettings().getSprite("fx", "espc_inverseportal");
				GL11.glBindTexture(GL11.GL_TEXTURE_2D, spritePortal.getTextureId());
				GL11.glBegin(GL11.GL_QUADS);
				
				float rot = ally.portalRots.get(i);
				
				GL11.glColor4ub(
					(byte) 120, (byte) 160, (byte) 255, (byte) 225);
				
				GL11.glTexCoord2f(0, 0);
				float fCos = (float) FastTrig.cos(Math.toRadians(ship.getFacing() + rot));
				float fSin = (float) FastTrig.sin(Math.toRadians(ship.getFacing() + rot));
				float divisor = jitterRand.nextFloat() * 0.3f + 1.8f;
				GL11.glVertex2f(
					pos.x - PORTAL_LENGTH/divisor * fCos -
					PORTAL_HEIGHT/divisor * fSin,
					pos.y + PORTAL_HEIGHT/divisor * fCos -
					PORTAL_LENGTH/divisor * fSin
				);
				GL11.glTexCoord2f(1, 0);
				GL11.glVertex2f(
					pos.x + PORTAL_LENGTH/divisor * fCos -
					PORTAL_HEIGHT/divisor * fSin,
					pos.y + PORTAL_HEIGHT/divisor * fCos +
					PORTAL_LENGTH/divisor * fSin
				);
				GL11.glTexCoord2f(1, 1);
				GL11.glVertex2f(
					pos.x + PORTAL_LENGTH/divisor * fCos +
					PORTAL_HEIGHT/divisor * fSin,
					pos.y - PORTAL_HEIGHT/divisor * fCos +
					PORTAL_LENGTH/divisor * fSin
				);
				GL11.glTexCoord2f(0, 1);
				GL11.glVertex2f(
					pos.x - PORTAL_LENGTH/divisor * fCos +
					PORTAL_HEIGHT/divisor * fSin,
					pos.y - PORTAL_HEIGHT/divisor * fCos -
					PORTAL_LENGTH/divisor * fSin
				);
				Vector2f.add(pos, new Vector2f(jitterRand.nextFloat() * 8f - 4f, jitterRand.nextFloat() * 8f - 4f), pos);
				divisor = jitterRand.nextFloat() * 0.5f + 1.3f;
				GL11.glEnd();
				SpriteAPI spritePortal2 = Global.getSettings().getSprite("systemMap", "radar_entity");
				GL11.glBindTexture(GL11.GL_TEXTURE_2D, spritePortal2.getTextureId());
				GL11.glBegin(GL11.GL_QUADS);

				GL11.glColor4ub(
						(byte) 25, (byte) 75, (byte) 255, (byte) 100);
				
				GL11.glTexCoord2f(0, 0);
				GL11.glVertex2f(
					pos.x - PORTAL_LENGTH/2f * fCos -
					PORTAL_HEIGHT/divisor * fSin,
					pos.y + PORTAL_HEIGHT/2f * fCos -
					PORTAL_LENGTH/divisor * fSin
				);
				GL11.glTexCoord2f(1, 0);
				GL11.glVertex2f(
					pos.x + PORTAL_LENGTH/2f * fCos -
					PORTAL_HEIGHT/divisor * fSin,
					pos.y + PORTAL_HEIGHT/2f * fCos +
					PORTAL_LENGTH/divisor * fSin
				);
				GL11.glTexCoord2f(1, 1);
				GL11.glVertex2f(
					pos.x + PORTAL_LENGTH/2f * fCos +
					PORTAL_HEIGHT/divisor * fSin,
					pos.y - PORTAL_HEIGHT/2f * fCos +
					PORTAL_LENGTH/divisor * fSin
				);
				GL11.glTexCoord2f(0, 1);
				GL11.glVertex2f(
					pos.x - PORTAL_LENGTH/2f * fCos +
					PORTAL_HEIGHT/divisor * fSin,
					pos.y - PORTAL_HEIGHT/2f * fCos -
					PORTAL_LENGTH/divisor * fSin
				);
				
				GL11.glEnd();
			}
		}
		timeLast = time;
	}
}