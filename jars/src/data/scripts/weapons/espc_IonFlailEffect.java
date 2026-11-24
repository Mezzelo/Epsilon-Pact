package data.scripts.weapons;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatAsteroidAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.WeaponEffectPluginWithInit;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.combat.OnHitEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.util.Misc;

import data.scripts.util.MezzUtils;
import data.scripts.weapons.proj.espc_IonFlailRenderPlugin;

import com.fs.starfarer.api.combat.ShipAPI;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.Iterator;

import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.plugins.MagicTrailPlugin;

import com.fs.starfarer.api.combat.MissileAPI;
/*
import com.fs.starfarer.api.combat.EmpArcEntityAPI;
import com.fs.starfarer.api.combat.BattleObjectiveAPI;
import org.lazywizard.lazylib.combat.CombatUtils;
*/

public class espc_IonFlailEffect implements OnFireEffectPlugin, EveryFrameWeaponEffectPlugin, WeaponEffectPluginWithInit,
	OnHitEffectPlugin{
	
	private ShipAPI ship;
	public final static float BASE_DAMAGE = 350f;
	private final static float ARC_PORTION = 0.25f;
	private final static int ARC_COUNT = 2;
	private final static float ARC_VEL_HORIZONTAL = 700f;
	public final static float BASE_VELOCITY = 800f;
	private final static float VEL_CURVE_MAGNITUDE = 400f;
	
	private final static float IMPULSE_ROTATION = 30000f;
	private final static float IMPULSE_FORCE = 150f;
	private final static float IMPULSE_FLAMED_MULT = 0.4f;
	/*
	private static final float IMPULSE_CORE_RADIUS = 75f;
	private static final float IMPULSE_RADIUS = 250f;
	private static final float FORCE_MULT_AOE = 1f;
	private static final float FORCE_MULT_MISSILE = 0.35f;
	*/
	
	
	
	public int arcDirection = 1;
	public float trailId = -100f;
	public boolean isCd = false;
	private int slotNeg = 1;
	private boolean queueChange = false;
	private float chargeLast = 0f;
	
	private ArrayDeque<DamagingProjectileAPI> projs = null;
	private WeaponAPI thisWep;
	private boolean glib = false;

	espc_IonFlailRenderPlugin plugin = null;
	
	public WeaponAPI getWeapon() {
		return thisWep;
	}
    
	
	@Override
	public void init(WeaponAPI weapon) {
		thisWep = weapon;
		trailId = MagicTrailPlugin.getUniqueID();
		slotNeg = weapon.getSlot().getLocation().y < 0f ? -1 : 1;
		ship = weapon.getShip();
		glib = MezzUtils.glibCheck();
		if (ship == null)
			return;
		
		if (Global.getCombatEngine().hasPluginOfClass(espc_IonFlailRenderPlugin.class))
			plugin = (espc_IonFlailRenderPlugin) Global.getCombatEngine().getPluginOfClass(espc_IonFlailRenderPlugin.class);
		else {
			plugin = new espc_IonFlailRenderPlugin(this);
			Global.getCombatEngine().addPlugin(plugin);
		}
		plugin.addWeapon(this);
		
		projs = new ArrayDeque<DamagingProjectileAPI>();
		
	}

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (Global.getCurrentState() != GameState.COMBAT ||
        	weapon == null || engine.isPaused() || amount <= 0f) return;
        
		if (queueChange) {
			isCd = true;
			queueChange = false;
    		arcDirection *= -1;
    		trailId = MagicTrailPlugin.getUniqueID();
		} else
	        isCd = weapon.getChargeLevel() <= chargeLast;
		chargeLast = weapon.getChargeLevel();
		
		if (weapon.getChargeLevel() > 0f && !isCd) {
			Global.getSoundPlayer().playLoop(
				"espc_ionflail_loop", weapon, 
				0.9f + 0.1f * weapon.getChargeLevel()
				 * (float) FastTrig.sin(weapon.getChargeLevel() * Math.PI * 4f),
				0.6f * weapon.getChargeLevel(),
				weapon.getLocation(), 
				weapon.getShip() == null ? Misc.ZERO : weapon.getShip().getVelocity(), 0.1f, 0.1f);
		}
        
		if (projs.size() == 0)
			return;

		float timescale = engine.getTimeMult().computeMultMod() /
			(ship.equals(engine.getPlayerShip()) ? 1f :
			ship.getMutableStats().getTimeMult().computeMultMod());
        Iterator<DamagingProjectileAPI> iter = projs.iterator();
        while (iter.hasNext()) {
        	DamagingProjectileAPI proj = iter.next();
        	if (proj == null || !engine.isInPlay(proj) || proj.didDamage() ||
        		!proj.getCustomData().containsKey("espc_ionflaildir")) {
        		iter.remove();
        		continue;
        	}
        	int dir = (int) proj.getCustomData().get("espc_ionflaildir");
        	float targetVel = proj.getCustomData().containsKey("espc_ionflailtargetvel") ? 
        		(float) proj.getCustomData().get("espc_ionflailtargetvel") : 1f;
        	// float life = 1f - proj.getElapsed() / weapon.getRange() * weapon.getProjectileSpeed() * 2f;
        	// float lifeLast = 1f - (proj.getElapsed() - amount) / weapon.getRange() * weapon.getProjectileSpeed() * 2f;
        	// 		(life - lifeLast)
        	float life = -amount / weapon.getRange() * weapon.getProjectileSpeed() * 2f;
        	float lifeMag = (float) FastTrig.cos(
            	proj.getElapsed() / weapon.getRange() * weapon.getProjectileSpeed() * Math.PI * 2f);
            float lifeMagLast = (float) FastTrig.cos(
            	(proj.getElapsed() - amount * timescale) / weapon.getRange() * weapon.getProjectileSpeed() * Math.PI * 2f);
    		Vector2f.add(proj.getVelocity(), new Vector2f(
    			(float) FastTrig.cos(Math.toRadians(proj.getFacing() - 90f * dir))
    			* ARC_VEL_HORIZONTAL * life * timescale * slotNeg + 
    			(float) FastTrig.cos(Math.toRadians(proj.getFacing())) * VEL_CURVE_MAGNITUDE *
    			(lifeMag - lifeMagLast) * targetVel, 
    			(float) FastTrig.sin(Math.toRadians(proj.getFacing() - 90f * dir))
    			* ARC_VEL_HORIZONTAL * life * timescale * slotNeg + 
    			(float) FastTrig.sin(Math.toRadians(proj.getFacing())) * VEL_CURVE_MAGNITUDE *
    			(lifeMag - lifeMagLast) * targetVel
    		), proj.getVelocity());
        }
    }

    
    @Override
    public void onFire(DamagingProjectileAPI proj, WeaponAPI weapon, CombatEngineAPI engine) {
    	boolean naturalSpawn = false;
    	int dir = arcDirection;
    	float targetVel = 1f;
    	if (!proj.getCustomData().containsKey("espc_ionflaildir")) {
    		naturalSpawn = true;
    		proj.setCustomData("espc_ionflaildir", arcDirection);
    		proj.setCustomData("espc_ionflailspawntime", engine.getTotalElapsedTime(false));
    		isCd = true;
    		queueChange = true;
    		chargeLast = 0f;
    		naturalSpawn = true;
    	} else if (proj.getCustomData().containsKey("espc_ionflailspawntime") &&
    		(float) proj.getCustomData().get("espc_ionflailspawntime") - engine.getTotalElapsedTime(false) <= 0.1f) {
    		naturalSpawn = true;
    		dir = (int) proj.getCustomData().get("espc_ionflaildir");
    		proj.getVelocity().set(
    			(float) FastTrig.cos(Math.toRadians(proj.getFacing())) * BASE_VELOCITY,
    			(float) FastTrig.sin(Math.toRadians(proj.getFacing())) * BASE_VELOCITY);
    	}
    	if (!naturalSpawn && proj.getVelocity().lengthSquared() < BASE_VELOCITY * BASE_VELOCITY)
    		proj.getVelocity().scale(BASE_VELOCITY / proj.getVelocity().length());
    	
    	// find target in trajectory, adjust velocity based on range
    	// default to max range trajectory if a friendly is closest
    	// override all other considerations if targeted ship is in trajectory
    	float bestRange = weapon.getRange() * 1.5f;
    	ShipAPI bestTarget = null;
    	boolean targIsFighter = true;

    	Iterator<Object> iter = engine.getShipGrid().getCheckIterator(
    		new Vector2f(proj.getLocation().x + (float) FastTrig.cos(Math.toRadians(proj.getFacing())) * weapon.getRange() / 2f, 
    			proj.getLocation().y + (float) FastTrig.sin(Math.toRadians(proj.getFacing())) * weapon.getRange() / 2f), 
    		weapon.getRange() * (float) Math.abs(FastTrig.cos(Math.toRadians(proj.getFacing()))) + 30f, 
    		weapon.getRange() * (float) Math.abs(FastTrig.sin(Math.toRadians(proj.getFacing()))) + 30f);
    	while (iter.hasNext()) {
    		Object next = iter.next();
    		if (next.equals(ship))
    			continue;
    		ShipAPI targ = (ShipAPI) next;
    		if (!targ.isAlive() || targ.isHulk() || targ.hasTag(Tags.VARIANT_FX_DRONE) || !targ.isTargetable())
    			continue;
    		if (CollisionUtils.getCollides(proj.getLocation(), 
    			new Vector2f(proj.getLocation().x + (float) FastTrig.cos(Math.toRadians(proj.getFacing())) * weapon.getRange(),
    			proj.getLocation().y + (float) FastTrig.sin(Math.toRadians(proj.getFacing())) * weapon.getRange()), 
    			targ.getLocation(), targ.getShieldRadiusEvenIfNoShield()) && 
    			(!targ.isFighter() || targIsFighter) &&
    			(MathUtils.getDistanceSquared(proj, targ.getLocation()) < bestRange * bestRange
    				|| (proj.getSource() != null && proj.getSource().getShipTarget() != null &&
    				proj.getSource().getShipTarget().equals(targ) && proj.getOwner() != targ.getOwner()))
    			) {
    			targIsFighter = targ.isFighter();
    			bestTarget = proj.getOwner() == targ.getOwner() ? null : targ;
    			bestRange = (MathUtils.getDistance(proj,  targ.getLocation()) + 
    				targ.getShieldRadiusEvenIfNoShield() * (targ.getVelocity().length() > 100f ? 0f : 0.5f));
    			if (proj.getSource() != null && proj.getSource().getShipTarget() != null &&
    				proj.getSource().getShipTarget().equals(targ) && proj.getOwner() != targ.getOwner())
    				break;
    		}
    	}
    	if (bestTarget != null) {
    		targetVel = bestRange / weapon.getRange();
	    	proj.getVelocity().scale(targetVel);
    		proj.setCustomData("espc_ionflailtargetvel", targetVel);
    	}
    	
   		plugin.addProj(proj);
    	if (naturalSpawn) {
	       	projs.add(proj);
	    	Vector2f.add(new Vector2f(
	    		espc_IonFlailRenderPlugin.FLAIL_RADIUS * (float) FastTrig.cos(
	    			Math.toRadians(weapon.getCurrAngle() + espc_IonFlailRenderPlugin.FLAIL_DEGS * -dir * slotNeg)), 
	    		espc_IonFlailRenderPlugin.FLAIL_RADIUS * (float) FastTrig.sin(
	        			Math.toRadians(weapon.getCurrAngle() + espc_IonFlailRenderPlugin.FLAIL_DEGS * -dir * slotNeg))),
	    		proj.getLocation(), proj.getLocation());
	   		Vector2f.add(proj.getVelocity(), new Vector2f(
	   			(float) FastTrig.cos(Math.toRadians(proj.getFacing() - 90f * dir * slotNeg))
	   			* ARC_VEL_HORIZONTAL + (float) FastTrig.cos(Math.toRadians(proj.getFacing())) *
	   		   	VEL_CURVE_MAGNITUDE * targetVel, 
	   			(float) FastTrig.sin(Math.toRadians(proj.getFacing() - 90f * dir * slotNeg))
	   			* ARC_VEL_HORIZONTAL + (float) FastTrig.sin(Math.toRadians(proj.getFacing())) *
	   		   	VEL_CURVE_MAGNITUDE * targetVel
	   		), proj.getVelocity());
    	}
   		return;
    }

	@Override
	public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit,
		ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
		((espc_IonFlailRenderPlugin) 
			Global.getCombatEngine().getPluginOfClass(espc_IonFlailRenderPlugin.class)).addImpact(
	    	point, VectorUtils.getAngle(new Vector2f(1f, 0f), projectile.getVelocity()));
        float dot = Vector2f.dot(projectile.getVelocity().normalise(new Vector2f()), 
        	new Vector2f(target.getLocation().x - point.x, target.getLocation().y - point.y).normalise(new Vector2f()));
        float impulseMult = 1f;
        if ((target instanceof ShipAPI || target instanceof CombatAsteroidAPI) && glib)
    		MezzUtils.addWaveDistortion(false, 
    			point, 
    			Misc.ZERO, 
    			4f, 75f, 0.55f, 0.35f, 0.55f, 0, 1);
        if (target instanceof ShipAPI && ((ShipAPI) target).getEngineController() != null &&
        	((ShipAPI) target).getEngineController().isFlamedOut())
        	impulseMult = IMPULSE_FLAMED_MULT;
        CombatUtils.applyForce(target, projectile.getVelocity(), 
        	projectile.getDamageAmount() / BASE_DAMAGE * IMPULSE_FORCE
        	* impulseMult * dot);
        dot = 1f - dot;
        if (target instanceof MissileAPI || (target instanceof ShipAPI && ((ShipAPI) target).isFighter())) {
        	dot = 1f - 2f * Misc.random.nextInt(2);
            if (target instanceof MissileAPI)
            	((MissileAPI) target).flameOut();
        }
        if (MathUtils.getShortestRotation(VectorUtils.getAngle(new Vector2f(1f, 0f), projectile.getVelocity()), 
        	VectorUtils.getAngle(new Vector2f(1f, 0f), 
        		new Vector2f(target.getLocation().x - point.x, target.getLocation().y - point.y))) < 0f)
        	dot *= -1f;
        target.setAngularVelocity(target.getAngularVelocity() + dot * impulseMult *
        	IMPULSE_ROTATION / Math.max(target.getMass(), 1f) * projectile.getDamageAmount() / BASE_DAMAGE *
        	(target instanceof ShipAPI && !((ShipAPI) target).isFighter() ? 1f : 0.2f));
        /*
        Iterator<Object> iter = engine.getAllObjectGrid().getCheckIterator(point, IMPULSE_RADIUS * 2f, IMPULSE_RADIUS * 2f);
        while (iter.hasNext()) {
        	Object obj = iter.next();
        	if (obj instanceof DamagingProjectileAPI && !(obj instanceof MissileAPI))
        		continue;
        	if (obj instanceof EmpArcEntityAPI || obj instanceof BattleObjectiveAPI)
        		continue;
        	if (obj.equals(target))
        		continue;
        	
        	CombatEntityAPI cObj = (CombatEntityAPI) obj;
        	Vector2f dir = new Vector2f(cObj.getLocation().x - point.x, cObj.getLocation().y - point.y);
        	float mag = (IMPULSE_RADIUS - Math.max(dir.length(), IMPULSE_CORE_RADIUS))
        		/ (IMPULSE_RADIUS - IMPULSE_CORE_RADIUS);
        	if (mag <= 0f)
        		continue;
        	if (cObj instanceof ShipAPI && ((ShipAPI) cObj).isFighter() && mag >= 1f && ((ShipAPI) cObj).getEngineController() != null
        		&& !((ShipAPI) cObj).isPhased())
        		((ShipAPI) cObj).getEngineController().forceFlameout();
        	else if (cObj instanceof MissileAPI && mag >= 1f)
        		((MissileAPI) cObj).flameOut();
            CombatUtils.applyForce(
            	cObj, dir, 
            	projectile.getDamageAmount() / BASE_DAMAGE * KNOCKBACK_FORCE * FORCE_MULT_AOE * mag *
            	(cObj instanceof MissileAPI ? FORCE_MULT_MISSILE : 1f));
        	
        }
        */
		if (!(target instanceof ShipAPI))
			return;
		if (!shieldHit)
			for (int i = 0; i < ARC_COUNT; i++)
				engine.spawnEmpArc(projectile.getSource(), point, target, target,
					DamageType.ENERGY, 
					projectile.getDamageAmount() * ARC_PORTION,
					projectile.getEmpAmount() * ARC_PORTION,
					100000f, // max range 
					"tachyon_lance_emp_impact",
					20f, // thickness
					new Color(25,100,155,255),
					new Color(255,255,255,255)
				);
	}
}