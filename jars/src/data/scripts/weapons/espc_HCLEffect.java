package data.scripts.weapons;

import java.awt.Color;

import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.BeamEffectPlugin;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;

public class espc_HCLEffect implements BeamEffectPlugin {

	// time between visual explosions at the end of the beam.
	private static final float EXPLOSION_INTERVAL = 1.25f;
	private static final float EXPLOSION_DISTANCE_INTERVAL = 140f;
	private float explosionTimer = 0f;
	
	private static final float EXPLOSION_DAMAGE = 300f;
	private static final float EXPLOSION_RADIUS = 90f;
	
	
	public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
		
        if (beam == null || engine.isPaused() || amount <= 0f) return;
		explosionTimer += amount;
		
		if (explosionTimer > EXPLOSION_INTERVAL) {
			explosionTimer -= EXPLOSION_INTERVAL;
			// DamagingProjectileAPI explosion;
			for (int i = 0; i < beam.getLength(); i += EXPLOSION_DISTANCE_INTERVAL) {
				Vector2f thisScale = (Vector2f) ((Vector2f.sub(beam.getFrom(), beam.getTo(), new Vector2f())).scale(i / beam.getLength()));
				engine.spawnDamagingExplosion(
					HCLExplosion(1f, beam.getSource()), 
					beam.getSource(), Vector2f.add(beam.getTo(), thisScale, new Vector2f())
				);
			}
		}
		

	}

	private DamagingExplosionSpec HCLExplosion(float damageMult, ShipAPI ship) {
		
		// oh boy.
		float damageFinal = EXPLOSION_DAMAGE * damageMult * (1f +
			ship.getMutableStats().getEnergyWeaponDamageMult().getPercentMod() / 100f +
			ship.getMutableStats().getBeamWeaponDamageMult().getPercentMod() / 100f +
			ship.getMutableStats().getEnergyWeaponDamageMult().getFlatMod() +
			ship.getMutableStats().getBeamWeaponDamageMult().getFlatMod()) *
			ship.getMutableStats().getEnergyWeaponDamageMult().getMult() *
			ship.getMutableStats().getBeamWeaponDamageMult().getMult();
		DamagingExplosionSpec beamExplosion = new DamagingExplosionSpec(
				0.5f, // duration
				EXPLOSION_RADIUS,
				EXPLOSION_RADIUS / 2f, // core radius, max -> min falloff
				damageFinal,
				// > final
				damageFinal / 2f,
				CollisionClass.PROJECTILE_FF,
				CollisionClass.PROJECTILE_FIGHTER,
				7f, // min particle size
				4f, // particle size range
				2.0f, // particle duration
				8, // particleCount
				new Color(255,135,105,155), // particleColor
				new Color(255,135,105,0)  // explosionColor
		);

		beamExplosion.setDamageType(DamageType.HIGH_EXPLOSIVE);
		// beamExplosion.setSoundSetId("devastator_explosion");
		beamExplosion.setUseDetailedExplosion(false);
		return beamExplosion;		
    }
	
	/*
	private DamagingExplosionSpec HCLExplosionVis() {
		DamagingExplosionSpec beamExplosion = new DamagingExplosionSpec(
				0.5f, // duration
				EXPLOSION_RADIUS * 0.5f,
				EXPLOSION_RADIUS * 0.5f, // core radius, max -> min falloff
				0f,
				0f,
				CollisionClass.NONE,
				CollisionClass.NONE,
				4f, // min particle size
				3f, // particle size range
				1.0f, // particle duration
				5, // particleCount
				new Color(255,135,105,105), // particleColor
				new Color(255,135,105,55)  // explosionColor
		);

		beamExplosion.setDamageType(DamageType.HIGH_EXPLOSIVE);
		beamExplosion.setUseDetailedExplosion(false);
		return beamExplosion;		
    }*/
	
}