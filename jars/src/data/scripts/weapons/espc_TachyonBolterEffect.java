package data.scripts.weapons;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.WeaponEffectPluginWithInit;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.combat.OnHitEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.util.Misc;

import data.scripts.plugin.espc_DamageListener;
// import data.scripts.util.MezzUtils;
import data.scripts.weapons.proj.espc_TachyonBolterRenderPlugin;

import com.fs.starfarer.api.combat.ShipAPI;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.Iterator;

import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
// import org.magiclib.plugins.MagicTrailPlugin;

import com.fs.starfarer.api.combat.MissileAPI;
/*
import com.fs.starfarer.api.combat.EmpArcEntityAPI;
import com.fs.starfarer.api.combat.BattleObjectiveAPI;
import org.lazywizard.lazylib.combat.CombatUtils;
*/

public class espc_TachyonBolterEffect implements OnFireEffectPlugin, EveryFrameWeaponEffectPlugin, WeaponEffectPluginWithInit,
	OnHitEffectPlugin{
	
	private ShipAPI ship;
	public final static float BASE_DAMAGE = 150f;
	private final static float ARC_DAMAGE_MULT = 1.5f;
	private final static float ARC_VEL_HORIZONTAL_MAX = 350f;
	public final static float BASE_VELOCITY = 1000f;
	private final static float VEL_CURVE_MAGNITUDE = 250f;
	private static int BURST_SIZE = 9;
	private final static int ARCS_PER_BURST = 3;
	private final static float MAX_SHOT_DELAY = 0.3f;
	
	private ArrayDeque<DamagingProjectileAPI> projs = null;
	private WeaponAPI thisWep;
	private float firedLast = 0f;
	private int burstRemaining = 0;
	private int arcsRemaining = 0;
	
	// private boolean glib = false;
	

	espc_TachyonBolterRenderPlugin plugin = null;
	
	public WeaponAPI getWeapon() {
		return thisWep;
	}
    
	// TODO: among other things, make sure you add a damage listener
	@Override
	public void init(WeaponAPI weapon) {
		BURST_SIZE = weapon.getSpec().getBurstSize();
		thisWep = weapon;
		ship = weapon.getShip();
		// glib = MezzUtils.glibCheck();
		if (ship == null)
			return;
		
		if (!weapon.getShip().hasListenerOfClass(espc_DamageListener.class))
			weapon.getShip().addListener(new espc_DamageListener());
		
		if (Global.getCombatEngine().hasPluginOfClass(espc_TachyonBolterRenderPlugin.class))
			plugin = (espc_TachyonBolterRenderPlugin) 
				Global.getCombatEngine().getPluginOfClass(espc_TachyonBolterRenderPlugin.class);
		else {
			plugin = new espc_TachyonBolterRenderPlugin(this);
			Global.getCombatEngine().addPlugin(plugin);
		}
		plugin.addWeapon(weapon);
		
		projs = new ArrayDeque<DamagingProjectileAPI>();
		
	}

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (Global.getCurrentState() != GameState.COMBAT ||
        	weapon == null || engine.isPaused() || amount <= 0f) return;
        
		if (projs.size() == 0)
			return;
		
		float timescale = engine.getTimeMult().computeMultMod() /
			(ship.equals(engine.getPlayerShip()) ? 1f :
			ship.getMutableStats().getTimeMult().computeMultMod());
        Iterator<DamagingProjectileAPI> iter = projs.iterator();
        while (iter.hasNext()) {
        	DamagingProjectileAPI proj = iter.next();
        	if (proj == null || !engine.isInPlay(proj) || proj.didDamage() ||
        		!proj.getCustomData().containsKey("espc_tachyonboltervel")) {
        		iter.remove();
        		continue;
        	}
        	float vel = (float) proj.getCustomData().get("espc_tachyonboltervel");
        	// float life = 1f - proj.getElapsed() / weapon.getRange() * weapon.getProjectileSpeed() * 2f;
        	// float lifeLast = 1f - (proj.getElapsed() - amount) / weapon.getRange() * weapon.getProjectileSpeed() * 2f;
        	// 		(life - lifeLast)
        	float life = -amount / weapon.getRange() * weapon.getProjectileSpeed() * 2f;
        	float lifeMag = (float) FastTrig.cos(
                proj.getElapsed() / weapon.getRange() * weapon.getProjectileSpeed() * Math.PI * 2f);
            float lifeMagLast = (float) FastTrig.cos(
                (proj.getElapsed() - amount * timescale) / weapon.getRange() * weapon.getProjectileSpeed() * Math.PI * 2f);
    		Vector2f.add(proj.getVelocity(), new Vector2f(
        		(float) FastTrig.cos(Math.toRadians(proj.getFacing() - 90f))
        			* vel * life * timescale + 
        		(float) FastTrig.cos(Math.toRadians(proj.getFacing())) *
        			VEL_CURVE_MAGNITUDE * (lifeMag - lifeMagLast), 
        		(float) FastTrig.sin(Math.toRadians(proj.getFacing() - 90f))
        			* vel * life * timescale + 
        		(float) FastTrig.sin(Math.toRadians(proj.getFacing())) *
        			VEL_CURVE_MAGNITUDE *(lifeMag - lifeMagLast)
        	), proj.getVelocity());
        }
    }

    
    @Override
    public void onFire(DamagingProjectileAPI proj, WeaponAPI weapon, CombatEngineAPI engine) {
    	proj.getDamage().setSoftFlux(true);
    	
    	boolean naturalSpawn = false;
    	float vel = ARC_VEL_HORIZONTAL_MAX * (Misc.random.nextFloat() * 2f - 1f);
    	if (!proj.getCustomData().containsKey("espc_tachyonboltervel")) {
    		naturalSpawn = true;
    		proj.setCustomData("espc_tachyonboltervel", 
    			vel);
    		proj.setCustomData("espc_tachyonbolterspawntime", engine.getTotalElapsedTime(false));
    		naturalSpawn = true;
    	} else if (proj.getCustomData().containsKey("espc_tachyonbolterspawntime") &&
    		(float) proj.getCustomData().get("espc_tachyonbolterspawntime") - engine.getTotalElapsedTime(false) <= 0.1f) {
    		naturalSpawn = true;
    		vel = (float) proj.getCustomData().get("espc_tachyonboltervel");
    		proj.getVelocity().set((float) FastTrig.cos(Math.toRadians(proj.getFacing())) * BASE_VELOCITY,
    			(float) FastTrig.sin(Math.toRadians(proj.getFacing())) * BASE_VELOCITY);
    	}
    	if (!naturalSpawn && proj.getVelocity().lengthSquared() < BASE_VELOCITY * BASE_VELOCITY)
    		proj.getVelocity().scale(BASE_VELOCITY / proj.getVelocity().length());
   		plugin.addProj(proj);
    	if (naturalSpawn) {
    		if (engine.getTotalElapsedTime(false) - firedLast > MAX_SHOT_DELAY) {
    			burstRemaining = BURST_SIZE;
    			arcsRemaining = ARCS_PER_BURST;
    		}
    		if (burstRemaining > 0) {
        		if (arcsRemaining > 0 && Misc.random.nextFloat() < ((float) arcsRemaining) / burstRemaining) {
        			arcsRemaining--;
        			proj.setCustomData("espc_tachyonboltarc", true);
        		}
        		burstRemaining--;
    		}
	       	projs.add(proj);
	   		Vector2f.add(proj.getVelocity(), new Vector2f(
	   			(float) FastTrig.cos(Math.toRadians(proj.getFacing() - 90f))
	   			* vel + (float) FastTrig.cos(Math.toRadians(proj.getFacing())) * VEL_CURVE_MAGNITUDE, 
	   			(float) FastTrig.sin(Math.toRadians(proj.getFacing() - 90f))
	   			* vel + (float) FastTrig.sin(Math.toRadians(proj.getFacing())) * VEL_CURVE_MAGNITUDE
	   		), proj.getVelocity());
    	}
   		return;
    }

	@Override
	public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit,
		ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
		((espc_TachyonBolterRenderPlugin) 
			Global.getCombatEngine().getPluginOfClass(espc_TachyonBolterRenderPlugin.class)).addImpact(
	    	point, VectorUtils.getAngle(new Vector2f(1f, 0f), projectile.getVelocity()));
        if (!projectile.getCustomData().containsKey("espc_tachyonboltarc"))
        	return;
        
		if (target instanceof MissileAPI) {
			engine.applyDamage(target, target.getLocation(), 
				projectile.getDamageAmount() * ARC_DAMAGE_MULT, 
				DamageType.ENERGY, 
				projectile.getEmpAmount() * ARC_DAMAGE_MULT, false, true, projectile.getWeapon());
			return;
		}
		if (!(target instanceof ShipAPI))
			return;
		
		if (!shieldHit)
			engine.spawnEmpArc(projectile.getSource(), point, target, target,
				DamageType.ENERGY, 
				projectile.getDamageAmount() * ARC_DAMAGE_MULT,
				projectile.getEmpAmount() * ARC_DAMAGE_MULT,
				100000f, // max range 
				"tachyon_lance_emp_impact",
				20f, // thickness
				new Color(85,25,215,255),
				new Color(255,255,255,255)
			);
		else if (Misc.random.nextFloat() < ((ShipAPI) target).getHardFluxLevel() *
			((ShipAPI) target).getMutableStats().getDynamic().getValue(Stats.SHIELD_PIERCED_MULT))
			engine.spawnEmpArcPierceShields(projectile.getSource(), point, target, target,
				DamageType.ENERGY, 
				projectile.getDamageAmount() * ARC_DAMAGE_MULT,
				projectile.getEmpAmount() * ARC_DAMAGE_MULT,
				100000f, // max range 
				"tachyon_lance_emp_impact",
				20f, // thickness
				new Color(85,25,215,255),
				new Color(255,255,255,255)
			);
	}
}