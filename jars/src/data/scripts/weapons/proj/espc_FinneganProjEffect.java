package data.scripts.weapons.proj;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;

import com.fs.starfarer.api.util.Misc;

import org.lwjgl.util.vector.Vector2f;
import org.lazywizard.lazylib.combat.CombatUtils;
				
public class espc_FinneganProjEffect implements OnHitEffectPlugin {
	
	private static final float MIN_VEL = 50f;
	private static final float BASE_VEL = 500f;
	private static final float MAX_VEL = 1000f;
	private static final float BASE_DAMAGE = 1000f;
	private static final float KNOCKBACK_FORCE = 1500f;
	
	
    @Override
    public void onHit(
        	DamagingProjectileAPI proj, 
        	CombatEntityAPI target, 
        	Vector2f point, 
        	boolean shieldHit, 
        	ApplyDamageResultAPI damage, 
        	CombatEngineAPI engine
	) {
		if (!(target instanceof ShipAPI))
			return;
		float speed = Math.min(MAX_VEL, proj.getVelocity().length());
		Global.getLogger(espc_FinneganProjEffect.class).info("speed finonHit: " + speed);
		if (speed < MIN_VEL)
			return;
		
		engine.applyDamage(
			target,
			point,
			speed / BASE_VEL * BASE_DAMAGE,
			DamageType.KINETIC,
			0f,
			false,
			false,
			proj.getWeapon() != null ? proj.getWeapon() : engine
		);
        CombatUtils.applyForce(target, proj.getVelocity(), speed / BASE_VEL * KNOCKBACK_FORCE);
        
		Global.getSoundPlayer().playSound(
			"espc_finnegan_impact",
			1.0f, 0.5f * speed / BASE_VEL * 0.2f, point, Misc.ZERO);
    }
	
}
