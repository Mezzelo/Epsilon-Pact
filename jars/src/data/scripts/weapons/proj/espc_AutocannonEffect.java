package data.scripts.weapons.proj;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.util.Misc;

import java.awt.Color;
import org.lwjgl.util.vector.Vector2f;
				
public class espc_AutocannonEffect implements OnHitEffectPlugin {
	
    private static final float EXPLOSION_DAMAGE = 200f;
    private static final float PROJECTILE_DAMAGE_BASE = 300f;
    private static final float EXPLOSION_RADIUS = 150f;
	
    @Override
    public void onHit(
       	DamagingProjectileAPI proj, 
        CombatEntityAPI target, 
        Vector2f point, 
        boolean shieldHit, 
        ApplyDamageResultAPI damage, 
        CombatEngineAPI engine
    ) {
    	if (proj.getWeapon() != null && proj.getWeapon().getShip() != null) {
    		if  (!proj.getWeapon().getShip().getCustomData().containsKey("espc_AutocannonPlugin")) {
        		espc_AutocannonVFX plugin = new espc_AutocannonVFX(proj.getWeapon().getShip());
        		engine.addPlugin(plugin);
        		plugin.addProj(point, proj.getFacing());
        		proj.getWeapon().getShip().setCustomData("espc_AutocannonPlugin", plugin);
        	} else
        		((espc_AutocannonVFX) proj.getWeapon().getShip().getCustomData().get("espc_AutocannonPlugin")).addProj(
        			point, proj.getFacing());
    	}
    	float mult = proj.getDamageAmount() / PROJECTILE_DAMAGE_BASE;
        
        engine.addHitParticle(point, Misc.ZERO, EXPLOSION_RADIUS * 2.75f, 0.2f, 0.35f, new Color(255,70,0,70));
		
		DamagingExplosionSpec ShellExplosion = new DamagingExplosionSpec(
				0.5f, // duration
				EXPLOSION_RADIUS,
				EXPLOSION_RADIUS / 1.5f, // core radius, max -> min falloff
				EXPLOSION_DAMAGE * mult,
				EXPLOSION_DAMAGE * mult,
				CollisionClass.PROJECTILE_FF,
				CollisionClass.PROJECTILE_FIGHTER,
				5f, // min particle size
				3f, // particle size range
				0.5f, // particle duration
				0, // particleCount
				new Color(255,125,0,0), // particleColor
				new Color(255,70,0,0)  // explosionColor
			);

			ShellExplosion.setDamageType(DamageType.FRAGMENTATION);
			ShellExplosion.setSoundSetId("hit_hull_heavy");
			ShellExplosion.setSoundVolume(0.5f);
			ShellExplosion.setUseDetailedExplosion(false);
			ShellExplosion.setShowGraphic(false);
			
			engine.spawnDamagingExplosion(
				ShellExplosion, 
				proj.getSource(), 
				point,
				true
			);
    }
	
}
