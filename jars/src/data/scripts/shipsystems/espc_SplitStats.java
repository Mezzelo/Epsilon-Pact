package data.scripts.shipsystems;

import java.awt.Color;
import java.util.Iterator;
import java.util.LinkedList;

import org.lazywizard.lazylib.MathUtils;

import com.fs.starfarer.api.Global;
// import com.fs.starfarer.api.SoundPlayerAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.loading.ProjectileSpecAPI;
import com.fs.starfarer.api.util.Misc;

public class espc_SplitStats extends BaseShipSystemScript {

	public static final float DAMAGE_MALUS = -0.4f;
	public static final int PROJ_COUNT = 4;
	public static final float SPREAD = 10f;

	private ShipAPI ship;
	private LinkedList<DamagingProjectileAPI> projs;
	
	
	
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		if (stats.getEntity() == null)
			return;
		
		if (ship == null)
			ship = (ShipAPI) stats.getEntity();
			
		if (projs == null)
			projs = new LinkedList<DamagingProjectileAPI>();

		stats.getBallisticWeaponDamageMult().modifyMult(id, 1f + DAMAGE_MALUS);
		stats.getEnergyWeaponDamageMult().modifyMult(id, 1f + DAMAGE_MALUS);
		
		CombatEngineAPI combatEngine = Global.getCombatEngine();
		
		Iterator<DamagingProjectileAPI> projIterator = projs.iterator();
		while (projIterator.hasNext()) {
			DamagingProjectileAPI proj = (DamagingProjectileAPI) projIterator.next();
			if (proj.isExpired() || proj.didDamage() || !combatEngine.isInPlay(proj))
				projIterator.remove();
		}
		
		Iterator<Object> entityIterator = combatEngine.getAllObjectGrid().getCheckIterator(
				ship.getLocation(), 
				(ship.getShieldRadiusEvenIfNoShield() + 60f) * 2f,
				(ship.getShieldRadiusEvenIfNoShield() + 60f) * 2f
			);
		while (entityIterator.hasNext()) {
			CombatEntityAPI entity = (CombatEntityAPI) entityIterator.next();
			if (!(entity instanceof DamagingProjectileAPI))
				continue;
			
			if (entity instanceof MissileAPI)
				continue;
					
			DamagingProjectileAPI proj = (DamagingProjectileAPI) entity;
            // for (DamagingProjectileAPI proj : combatEngine.getProjectiles()) {
            if (proj.getSource() != ship)
            	continue;
            if (proj.getProjectileSpecId() == null || proj.didDamage() || proj.getWeapon() == null)
            	continue;
            if (proj.getElapsed() > combatEngine.getElapsedInLastFrame() + 0.05f)
            	continue;
            if (proj.getWeapon().getType() != WeaponType.BALLISTIC && proj.getWeapon().getType() != WeaponType.ENERGY)
            	continue;
            if (proj.getWeapon().getSpec() != null &&
            	proj.getWeapon().getSpec().getProjectileSpec() != null &&
            	!proj.getWeapon().getSpec().getProjectileSpec().equals(proj.getProjectileSpec()))
            	continue;
            if (projs.contains(proj))
            	continue;
            
            projs.add(proj);
            
            WeaponAPI currWeapon = proj.getWeapon();

        	boolean getWepPlugin = currWeapon.getEffectPlugin() != null &&
        		currWeapon.getEffectPlugin().getClass().isAssignableFrom(OnFireEffectPlugin.class);
        	boolean getProjPlugin = (((ProjectileSpecAPI) currWeapon.getSpec().getProjectileSpec()).getOnFireEffect() != null);
            for (int i = 1; i < PROJ_COUNT; i++) {
				stats.getBallisticProjectileSpeedMult().modifyMult(id, Misc.random.nextFloat() * 0.2f + 0.9f);
				stats.getBallisticWeaponRangeBonus().modifyMult(id, Misc.random.nextFloat()* 0.05f + 1.0f);
				stats.getEnergyProjectileSpeedMult().modifyMult(id, Misc.random.nextFloat() * 0.2f + 0.9f);
				stats.getEnergyWeaponRangeBonus().modifyMult(id, Misc.random.nextFloat()* 0.05f + 1.0f);
    			DamagingProjectileAPI cloneProj = (DamagingProjectileAPI) combatEngine.spawnProjectile(
    				ship,
    				currWeapon,
    				currWeapon.getId(),
    				// currWeapon.getSpec().getProjectileSpec().getId(),
    				proj.getLocation(),
    				//proj.getFacing() + Misc.random.nextFloat() * SPREAD / PROJ_COUNT +
    				//	SPREAD / PROJ_COUNT * (i - PROJ_COUNT / 2f),
    				proj.getFacing() + (Misc.random.nextFloat() - 0.5f) * SPREAD ,
    				ship.getVelocity()
    			);

				if (getWepPlugin)
					((OnFireEffectPlugin) currWeapon.getEffectPlugin()).onFire(cloneProj, currWeapon, combatEngine);
				if (getProjPlugin)
					((OnFireEffectPlugin) ((ProjectileSpecAPI) currWeapon.getSpec().getProjectileSpec()).getOnFireEffect())
						.onFire(cloneProj, currWeapon, combatEngine);
				
    			projs.add(cloneProj);
            }
            
            float projDamage = proj.getDamageAmount();
        	
			Global.getSoundPlayer().playSound(
				"espc_overpressure_fire",
				1.35f - projDamage / 1300f,
				(0.25f + projDamage / 700f),
				proj.getLocation(),
				ship.getVelocity()
			);
        	
            combatEngine.addHitParticle(
            	proj.getLocation(),
            	ship.getVelocity(),
            	50 + projDamage / 6,
            	0.6f,
            	0.15f + projDamage / 2500f,
            	new Color(75, 255, 85, 
            		(int) Math.min((projDamage / 25f), 255f)
            	)
            );
            combatEngine.addHitParticle(
            	proj.getLocation(),
            	ship.getVelocity(),
            	15 + projDamage / 16,
            	1.5f + projDamage / 500f,
            	0.05f + projDamage / 5000f,
            	new Color(200, 255, 200, 
            		(int) Math.min((projDamage / 2f), 255f)
            	)
            );
            for (int i = 0; i < projDamage / 50 + 4; i++) {
            	combatEngine.addHitParticle(
            		MathUtils.getRandomPointInCone(
            			proj.getLocation(),
            			(45f + projDamage / 25f) / (projDamage / 50f + 4f) * i,
            			proj.getFacing() - projDamage/50f - 6f,
            			proj.getFacing() + projDamage/50f + 6f
            		),
					ship.getVelocity(),
					Misc.random.nextFloat() * 35f + projDamage/20f - i,
					1,
					0.2f + 0.02f * i,
					new Color(150, (int) (Misc.random.nextFloat() * 40) + 180, 150, 
						(int) (200)
					)
            	);
            }
            
		}

		stats.getBallisticProjectileSpeedMult().unmodify(id);
		stats.getBallisticWeaponRangeBonus().unmodify(id);
		stats.getEnergyProjectileSpeedMult().unmodify(id);
		stats.getEnergyWeaponRangeBonus().unmodify(id);
	}
	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getBallisticWeaponDamageMult().unmodify(id);
		stats.getEnergyWeaponDamageMult().unmodify(id);
		stats.getBallisticProjectileSpeedMult().unmodify(id);
		stats.getBallisticWeaponRangeBonus().unmodify(id);
		stats.getEnergyProjectileSpeedMult().unmodify(id);
		stats.getEnergyWeaponRangeBonus().unmodify(id);
		
		if (projs != null) {
			projs.clear();
			projs = null;
		}
	}
	
	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 1) {
			return new StatusData("non-missile damage -" + ((int) (DAMAGE_MALUS * -100)) + "%", true);
		} else if (index == 0) {
			return new StatusData("quadrupling all non-missile projectiles", false);
		}
		return null;
	}
}
