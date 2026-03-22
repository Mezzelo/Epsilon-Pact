package data.scripts.weapons;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.WeaponEffectPluginWithInit;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.OnHitEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

import java.awt.Color;
import java.util.ArrayList;

import org.lazywizard.lazylib.FastTrig;
import org.lwjgl.util.vector.Vector2f;

public class espc_IonResonatorEffectOld implements EveryFrameWeaponEffectPlugin, WeaponEffectPluginWithInit,
	OnHitEffectPlugin{
	
	private ShipAPI ship;
	private final static float ARC_DELAY_SMALL = 0.8f;
	private final static float ARC_DELAY_MED = 0.4f;
	private final static int MIN_ARCS_SMALL = 3;
	private final static int MIN_ARCS_MED = 6;
	private final static float SHIELD_MULT = 2f;
	private final static float ARC_DAMAGE_MULT = 3f;
	private final static float DAMAGE_SMALL = 40f;
	private final static float DAMAGE_MED = 45f;
	private final static float EMP_SMALL = 200f;
	private final static float EMP_MED = 300f;
	
	
	private ArrayList<ShipAPI> targets;
	private ArrayList<Float> lastHit;
	// store hits in polar coordinates, to account for targets rotating
	private ArrayList<ArrayList<Float>> hitDists;
	private ArrayList<ArrayList<Float>> bearings;
	
	private int wepSize = 0;
	
	private void doArc(CombatEngineAPI engine, ShipAPI target,
		float dist, float bearing, float damage, float emp) {
		Vector2f hitLoc = new Vector2f(
			(float) FastTrig.cos(bearing) * dist + target.getLocation().x, 
			(float) FastTrig.sin(bearing) * dist + target.getLocation().y
		);
		engine.spawnEmpArc(ship, hitLoc,
			target, target,
			DamageType.ENERGY, 
			damage,
			emp,
			100000f, // max range 
			"tachyon_lance_emp_impact",
			20f, // thickness
			new Color(25,100,155,255),
			new Color(255,255,255,255)
		);
	}
	
	@Override
	public void init(WeaponAPI weapon) {
		ship = weapon.getShip();
		if (ship == null)
			return;
		if (weapon.getId().equals("espc_heavyionresonator"))
			wepSize = 1;
		
		targets = new ArrayList<ShipAPI>();
		lastHit = new ArrayList<Float>();
		// store hits in polar coordinates, to account for targets rotating
		hitDists = new ArrayList<ArrayList<Float>>();
		bearings = new ArrayList<ArrayList<Float>>();
	}

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (Global.getCurrentState() != GameState.COMBAT ||
        	weapon == null || engine.isPaused() || amount <= 0f || targets.size() <= 0) return;
        for (int i = 0; i < targets.size(); i++) {
        	if (engine.getTotalElapsedTime(false) > lastHit.get(i) +
        		(wepSize == 0 ? ARC_DELAY_SMALL : ARC_DELAY_MED)) {
        		for (int g = 0; g < bearings.get(i).size(); g++) 
					doArc(engine, targets.get(i),
						hitDists.get(i).get(g), 
						bearings.get(i).get(g), 
						(wepSize == 0 ? DAMAGE_SMALL : DAMAGE_MED) * 
							(bearings.get(i).size() >= (wepSize == 0 ? MIN_ARCS_SMALL : MIN_ARCS_MED) 
							? ARC_DAMAGE_MULT : 1f), 
						(wepSize == 0 ? EMP_SMALL : EMP_MED) * 
							(bearings.get(i).size() >= (wepSize == 0 ? MIN_ARCS_SMALL : MIN_ARCS_MED) 
							? ARC_DAMAGE_MULT : 1f)
					);
        		targets.remove(i);
        		lastHit.remove(i);
        		hitDists.remove(i);
        		bearings.remove(i);
        		i--;
        	}
        }
		
    }

	@Override
	public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit,
		ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
		if (projectile.getWeapon() == null)
			return;
		if (target instanceof MissileAPI ||
			(target instanceof ShipAPI && ((ShipAPI) target).getHullSize().equals(HullSize.FIGHTER))
			) {
			engine.spawnEmpArc(projectile.getSource(), point, target, target,
				DamageType.ENERGY, 
				DAMAGE_SMALL * ARC_DAMAGE_MULT,
				EMP_SMALL * ARC_DAMAGE_MULT,
				100000f, // max range 
				"tachyon_lance_emp_impact",
				20f, // thickness
				new Color(25,100,155,255),
				new Color(255,255,255,255)
			);
			return;
		}
		if (shieldHit) {
			engine.applyDamage(target, point, 
			(projectile.getWeapon().getId().equals("espc_ionresonator") ? DAMAGE_SMALL : DAMAGE_MED) * SHIELD_MULT,
			DamageType.ENERGY, 
			(projectile.getWeapon().getId().equals("espc_ionresonator") ? EMP_SMALL : EMP_MED) * SHIELD_MULT,
			false, false, ship);
			return;
		}
		((espc_IonResonatorEffect) projectile.getWeapon().getEffectPlugin()).onHitLocal(
			projectile, target, point, damageResult, engine);
	}
    
    public void onHitLocal(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point,
    	ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
		if (target instanceof ShipAPI) {
			ShipAPI targ = (ShipAPI) target;
			if (targets.contains(targ)) {
				int index = targets.indexOf(targ);
				float angle = (float) FastTrig.atan2(
						point.y - targ.getLocation().y,
						point.x - targ.getLocation().x
					);
					if (Float.isNaN(angle))
						angle = targ.getFacing();
				lastHit.set(index, engine.getTotalElapsedTime(false));
				hitDists.get(index).add(Misc.getDistance(targ.getLocation(), point));
				bearings.get(index).add(angle);
			} else {
				targets.add(targ);
				lastHit.add(engine.getTotalElapsedTime(false));
				hitDists.add(new ArrayList<Float>());
				bearings.add(new ArrayList<Float>());
				hitDists.get(targets.size() - 1).add(Misc.getDistance(targ.getLocation(), point));
				float angle = (float) FastTrig.atan2(
					point.y - targ.getLocation().y,
					point.x - targ.getLocation().x
				);
				if (Float.isNaN(angle))
					angle = targ.getFacing();
				bearings.get(targets.size() - 1).add(angle);
			}
		}
    }
}