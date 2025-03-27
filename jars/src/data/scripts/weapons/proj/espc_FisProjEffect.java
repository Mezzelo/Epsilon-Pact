package data.scripts.weapons.proj;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;


import java.awt.Color;
import org.lwjgl.util.vector.Vector2f;
import org.lazywizard.lazylib.MathUtils;

import data.scripts.plugin.espc_DamageListener;
				
public class espc_FisProjEffect implements OnHitEffectPlugin, OnFireEffectPlugin {
	
	
	private static final float FIS_ACTIVATE_RADIUS = 550f;
	
    private static final float FIS_DURATION = 1f;
    private static final float FIS_EXPLOSION_DAMAGE = 1000f;
    private static final float FIS_EXPLOSION_RADIUS = 100f;
	
	public boolean didEntry = false;

	// particles handled via plugin, as we wish for the effects to persist past the projectile itself
	// (and to get the glViewport setup)
    @Override
    public void onFire(DamagingProjectileAPI proj, WeaponAPI weapon, CombatEngineAPI engine) {
        engine.addPlugin(new espc_FisProjVFX(proj));
		
		if (!weapon.getShip().hasListenerOfClass(espc_DamageListener.class))
			weapon.getShip().addListener(new espc_DamageListener());
    }
	
    @Override
    public void onHit(
       	DamagingProjectileAPI proj, 
        CombatEntityAPI target, 
        Vector2f point, 
        boolean shieldHit, 
        ApplyDamageResultAPI damage, 
        CombatEngineAPI engine
    ) {
		if (didEntry || target instanceof DamagingProjectileAPI && !proj.didDamage())
			return;
		
		int keyMax, keyMaxOld;
		boolean didFission = false;
		
		if (!engine.getCustomData().containsKey("espc_fisMax")) {
			engine.getCustomData().put("espc_fisMax", 0);
			keyMax = 0;
		}
		else
			keyMax = (Integer) engine.getCustomData().get("espc_fisMax");
		keyMaxOld = keyMax;
		
		for (int i = keyMaxOld - 1; i >= 0; i--) {
			if (engine.getTotalElapsedTime(false) - Math.abs((Float) engine.getCustomData().get("espc_fisTime" + i)) < FIS_DURATION) {
				if (MathUtils.isWithinRange(
					proj.getLocation(), 
					(Vector2f) engine.getCustomData().get(("espc_fisLoc" + i)), 
					FIS_ACTIVATE_RADIUS)) {
						
					didFission = true;
					didEntry = true;
					
					// if opposite sides both contribute to the explosion, it friendly fires everyone, including the shooters :)
					boolean clash = (Float) engine.getCustomData().get("espc_fisTime" + i) * (-0.5 + proj.getOwner()) < 0f;

					DamagingExplosionSpec fisExplosion = new DamagingExplosionSpec(
						0.5f, // duration
						FIS_EXPLOSION_RADIUS,
						FIS_EXPLOSION_RADIUS / 1.5f, // core radius, max -> min falloff
						FIS_EXPLOSION_DAMAGE * (1f +
							proj.getSource().getMutableStats().getEnergyWeaponDamageMult().getFlatMod() +
							proj.getSource().getMutableStats().getEnergyWeaponDamageMult().getPercentMod() / 100f)
							* proj.getSource().getMutableStats().getEnergyWeaponDamageMult().getMult(),
						FIS_EXPLOSION_DAMAGE / 1.5f * (1f +
							proj.getSource().getMutableStats().getEnergyWeaponDamageMult().getFlatMod() +
							proj.getSource().getMutableStats().getEnergyWeaponDamageMult().getPercentMod() / 100f)
							* proj.getSource().getMutableStats().getEnergyWeaponDamageMult().getMult(),
						clash ? CollisionClass.HITS_SHIPS_AND_ASTEROIDS : CollisionClass.PROJECTILE_FF,
						clash ? CollisionClass.HITS_SHIPS_AND_ASTEROIDS : CollisionClass.PROJECTILE_FIGHTER,
						7f, // min particle size
						4f, // particle size range
						2.0f, // particle duration
						32, // particleCount
						new Color(150,100,255,155), // particleColor
						new Color(150,100,255,155)  // explosionColor
					);

					fisExplosion.setDamageType(DamageType.ENERGY);
					fisExplosion.setSoundSetId("riftcascade_rift");
					fisExplosion.setUseDetailedExplosion(true);
					
					engine.spawnDamagingExplosion(
						fisExplosion, 
						proj.getSource(), MathUtils.getMidpoint(proj.getLocation(), (Vector2f) engine.getCustomData().get(("espc_fisLoc" + i))),
						clash
					);
					
					engine.getCustomData().remove("espc_fisTime" + i);
					engine.getCustomData().remove("espc_fisLoc" + i);
					keyMax--;
				}
			} else {
				engine.getCustomData().remove("espc_fisTime" + i);
				engine.getCustomData().remove("espc_fisLoc" + i);
				keyMax--;
			}
		}
		
		if (keyMax < keyMaxOld) {
			int low = 0, high = keyMaxOld - 1;
			while (low < high) {
				while (engine.getCustomData().containsKey("espc_fisTime" + low) && low < high) {
					++low;
				}
				while (!engine.getCustomData().containsKey("espc_fisTime" + high) && high > low) {
					--high;
				}
				if (low >= high)
					break;
				engine.getCustomData().put("espc_fisTime" + low, engine.getCustomData().get("espc_fisTime" + high));
				engine.getCustomData().put("espc_fisLoc" + low, engine.getCustomData().get("espc_fisLoc" + high));
				engine.getCustomData().remove("espc_fisTime" + high);
				engine.getCustomData().remove("espc_fisLoc" + high);
				++low;
				--high;
			}
		}
		
		if (!didFission) {
			engine.getCustomData().put("espc_fisTime" + keyMax, 
				// owner data is baked into the time's sign
				engine.getTotalElapsedTime(false) * (-1 + 2 * proj.getOwner())
			);
			engine.getCustomData().put("espc_fisLoc" + keyMax, proj.getLocation());
			didEntry = true;
			keyMax++;
		}
		
		engine.getCustomData().put("espc_fisMax", keyMax);

    }
	
}
