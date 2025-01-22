package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.util.Misc;

import data.scripts.plugin.espc_DamageListener;

import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Iterator;

import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;

public class espc_FlakEffect implements OnFireEffectPlugin, EveryFrameWeaponEffectPlugin {

	public static final int shotsPerBurst = 5;
	public static final float burstSpread = 24f;
	public static final float minDamageForTarget = 250f;
	
	private int currentShot = 0;
	// private Vector2f spreadLocation;
	private float spreadVel;
	
	private ShipAPI thisShip;
	private Vector2f shipVel;
	
	private class PDThr {
		public PDThr(CombatEntityAPI entity, DamagingProjectileAPI proj, Vector2f initShipVel, float distSquared) {
			this.entity = entity;
			this.proj = proj;
			this.initShipVel = initShipVel;
			this.distSquared = distSquared;
		}
		CombatEntityAPI entity;
		DamagingProjectileAPI proj;
		Vector2f initShipVel;
		float distSquared;
	}
	
	private ArrayList<PDThr> pdThreats;
	private float longestDist = 0f;
	
    @Override
    public void onFire(DamagingProjectileAPI proj, WeaponAPI weapon, CombatEngineAPI engine) {
    	if (MathUtils.getDistanceSquared(proj, weapon.getFirePoint(0)) > 1f)
    		return;
        if (currentShot == 0) {
			shipVel = thisShip.getVelocity();
			spreadVel = weapon.getProjectileSpeed();
			
			longestDist = 0f;
			pdThreats = new ArrayList<PDThr>();
			CombatEngineAPI combatEngine = Global.getCombatEngine();
            
			Iterator<Object> entityIterator = combatEngine.getAiGridShips().getCheckIterator(
				Vector2f.add(weapon.getLocation(), 
					new Vector2f((float) FastTrig.cos(Math.toRadians(weapon.getCurrAngle())) * weapon.getRange()/2f, 
					(float) FastTrig.sin(Math.toRadians(weapon.getCurrAngle())) * weapon.getRange()/2f), 
					new Vector2f()),
				weapon.getRange(),
				weapon.getRange()
			);
			while (entityIterator.hasNext()) {
				CombatEntityAPI entity = (CombatEntityAPI) entityIterator.next();
				if (entity.getOwner() == thisShip.getOwner())
					continue;
				if (((ShipAPI) entity).isFighter() && Math.abs(MathUtils.getShortestRotation(
					(float) Math.toDegrees(FastTrig.atan2(entity.getLocation().y - proj.getLocation().y, 
					entity.getLocation().x - proj.getLocation().x)),
					weapon.getCurrAngle())) < burstSpread/2f) {
					float thisDist = MathUtils.getDistanceSquared(proj, entity);
					if (pdThreats.size() < shotsPerBurst && thisDist < weapon.getRange() * weapon.getRange()) {
						pdThreats.add(new PDThr(
							entity, 
							proj,
							shipVel,
							thisDist
						));
						if (thisDist > longestDist)
							longestDist = thisDist;
					} else if (thisDist < longestDist) {
						for (int i = 0; i < pdThreats.size(); i++) {
							if (pdThreats.get(i).distSquared > thisDist) {
								pdThreats.remove(pdThreats.size() - 1);
								pdThreats.add(i, new PDThr(
									entity,
									proj,
									shipVel,
									thisDist
								));
								longestDist = pdThreats.get(pdThreats.size() - 1).distSquared;
								break;
							}
						}
					}
				}
			}
			entityIterator = combatEngine.getAiGridMissiles().getCheckIterator(
					Vector2f.add(weapon.getLocation(), 
						new Vector2f((float) FastTrig.cos(Math.toRadians(weapon.getCurrAngle())) * weapon.getRange()/2f, 
						(float) FastTrig.sin(Math.toRadians(weapon.getCurrAngle())) * weapon.getRange()/2f), 
						new Vector2f()),
					weapon.getRange(),
					weapon.getRange()
				);
				while (entityIterator.hasNext()) {
					CombatEntityAPI entity = (CombatEntityAPI) entityIterator.next();
					if (entity.getOwner() == thisShip.getOwner())
						continue;
					if (Math.abs(MathUtils.getShortestRotation(
						(float) Math.toDegrees(FastTrig.atan2(entity.getLocation().y - proj.getLocation().y, 
						entity.getLocation().x - proj.getLocation().x)),
						weapon.getCurrAngle())) < burstSpread/2f) {
						float thisDist = MathUtils.getDistanceSquared(proj, entity);
						if (pdThreats.size() < shotsPerBurst && thisDist < weapon.getRange() * weapon.getRange()) {
							pdThreats.add(new PDThr(
								entity, 
								proj,
								shipVel,
								thisDist
							));
							if (thisDist > longestDist)
								longestDist = thisDist;
						} else if (thisDist < longestDist) {
							for (int i = 0; i < pdThreats.size(); i++) {
								if (pdThreats.get(i).distSquared > thisDist) {
									pdThreats.remove(pdThreats.size() - 1);
									pdThreats.add(i, new PDThr(
										entity,
										proj,
										shipVel,
										thisDist
									));
									longestDist = pdThreats.get(pdThreats.size() - 1).distSquared;
									break;
								}
							}
						}
					}
				}
			
			
			float speedMod = Misc.random.nextFloat();
			if (pdThreats.size() > 0) {
				speedMod = 1f;
				float travel = MathUtils.getDistance(pdThreats.get(0).entity, proj) / proj.getVelocity().length();
				proj.setFacing((float) Math.toDegrees(FastTrig.atan2(
					pdThreats.get(0).entity.getLocation().y
					+ (pdThreats.get(0).entity.getVelocity().y - thisShip.getVelocity().y) * travel
					- proj.getLocation().y, 
					pdThreats.get(0).entity.getLocation().x
					+ (pdThreats.get(0).entity.getVelocity().x - thisShip.getVelocity().x) * travel
					- proj.getLocation().x)));
			}
			else
				proj.setFacing(weapon.getCurrAngle() + Misc.random.nextFloat() * burstSpread - (burstSpread / 2.0f));
			Vector2f.add(new Vector2f((float) FastTrig.cos(Math.toRadians(proj.getFacing())) * spreadVel * (0.85f + speedMod * 0.15f) + shipVel.x, 
				(float) FastTrig.sin(Math.toRadians(proj.getFacing())) * spreadVel * (0.85f + speedMod * 0.15f) + shipVel.y), 
				new Vector2f(), proj.getVelocity()
			);
		} else {
			float speedMod = Misc.random.nextFloat();
			if (currentShot < pdThreats.size()) {
				speedMod = 1f;
				float travel = MathUtils.getDistance(pdThreats.get(currentShot).entity, proj) / proj.getVelocity().length();
				proj.setFacing((float) Math.toDegrees(FastTrig.atan2(
					pdThreats.get(currentShot).entity.getLocation().y
					+ (pdThreats.get(currentShot).entity.getVelocity().y - thisShip.getVelocity().y) * travel
					- proj.getLocation().y, 
					pdThreats.get(currentShot).entity.getLocation().x
					+ (pdThreats.get(currentShot).entity.getVelocity().x - thisShip.getVelocity().x) * travel
					- proj.getLocation().x)));
			}
			else
				proj.setFacing(weapon.getCurrAngle() + Misc.random.nextFloat() * burstSpread - (burstSpread / 2.0f));
			Vector2f.add(new Vector2f((float) FastTrig.cos(Math.toRadians(proj.getFacing())) * spreadVel * (0.85f + speedMod * 0.15f) + shipVel.x, 
				(float) FastTrig.sin(Math.toRadians(proj.getFacing())) * spreadVel * (0.85f + speedMod * 0.15f) + shipVel.y),
				new Vector2f(), proj.getVelocity()
			);
		}
        proj.setCollisionRadius(proj.getCollisionRadius() * 10f);
		currentShot++;
		if (currentShot >= shotsPerBurst) {
			currentShot = 0;
		}
    }
	
    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
		if (thisShip == null)
			thisShip = weapon.getShip();
		if (!thisShip.hasListenerOfClass(espc_DamageListener.class))
			thisShip.addListener(new espc_DamageListener());
		if (pdThreats != null && pdThreats.size() > 0) {
			CombatEngineAPI combatEngine = Global.getCombatEngine();
			if (combatEngine.getElapsedInLastFrame() <= 0f)
				return;
			for (int i = pdThreats.size() - 1; i >= 0; i--) {
				if (combatEngine.isInPlay(pdThreats.get(i).proj) && combatEngine.isInPlay(pdThreats.get(i).entity)) {
					DamagingProjectileAPI proj = pdThreats.get(i).proj;
					Vector2f.sub(proj.getVelocity(), pdThreats.get(i).initShipVel, proj.getVelocity());
					float travel = MathUtils.getDistance(pdThreats.get(i).entity, proj) / proj.getVelocity().length();
					float ang = (float) Math.toDegrees(FastTrig.atan2(
						pdThreats.get(i).entity.getLocation().y
						+ (pdThreats.get(i).entity.getVelocity().y - pdThreats.get(i).initShipVel.y) * travel
						- proj.getLocation().y, 
						pdThreats.get(i).entity.getLocation().x
						+ (pdThreats.get(i).entity.getVelocity().x - pdThreats.get(i).initShipVel.x) * travel
						- proj.getLocation().x));
					if (Math.abs(MathUtils.getShortestRotation(ang, proj.getFacing())) > 2.5f)
						pdThreats.remove(i);
					else {
						proj.setFacing(ang);
						float vel = proj.getVelocity().length();
						Vector2f.add(new Vector2f(
							(float) FastTrig.cos(Math.toRadians(proj.getFacing())) * vel + pdThreats.get(i).initShipVel.x, 
							(float) FastTrig.sin(Math.toRadians(proj.getFacing())) * vel + pdThreats.get(i).initShipVel.y),
							new Vector2f(), proj.getVelocity()
						);
					}
				}
				else
					pdThreats.remove(i);
			}
		}
    }
}