package data.scripts.hullmods;

import java.util.List;

import org.lazywizard.lazylib.FastTrig;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.input.InputEventAPI;
				
public class espc_RecoilModPlugin extends BaseEveryFrameCombatPlugin {
	
	// could work with multiple weps & detecting the largest, but becomes unnecessarily expensive
	// the hardcoded single large mount should suffice - anything more would be considerably goofier than what we're already allowing
	
	// private List<WeaponAPI> recoilWeps;
	// private float[] cooldownLast;
	
	private WeaponAPI recoilWep;
	private float cooldownLast = 0f;
	
	private static final float KNOCKBACK_PER_DAMAGE_OVER_WEIGHT = 30f;
	private static final float BEAM_KNOCKBACK_MULT = 0.65f;
	private static final float HEAVY_ARMOR_MODIFIER = -0.2f;
	private float knockbackMod;
	private boolean didInit = false;
	private boolean isBeam;
	
	private ShipAPI ship;
	
	// sim appears to sometimes fail to call the init method, typically on immediately setting autopilot?
	void doInit() {
		// recoilWeps = new ArrayList<WeaponAPI>();
		for (WeaponAPI weapon : ship.getAllWeapons()) {
			if (weapon.getSize() == WeaponSize.LARGE || weapon.getSize() == WeaponSize.MEDIUM) {
				recoilWep = weapon;
				didInit = true;
				break;
			}
				// recoilWeps.add(weapon);
		}
		// cooldownLast = new float[recoilWeps.size()];
		if (didInit) {
			isBeam = recoilWep.isBeam();
			if (isBeam)
				knockbackMod = recoilWep.getDamage().getDamage() 
					// * Math.max(recoilWep.getSpec().getBurstSize(), 0f) 
					/ ship.getMass() * KNOCKBACK_PER_DAMAGE_OVER_WEIGHT * BEAM_KNOCKBACK_MULT;
			else
				knockbackMod = recoilWep.getDerivedStats().getDamagePerShot() 
					// * Math.max(recoilWep.getSpec().getBurstSize(), 0f) 
					/ ship.getMass() * KNOCKBACK_PER_DAMAGE_OVER_WEIGHT;

			if (ship.getVariant().hasHullMod("heavyarmor")) {
				knockbackMod *= (1f + HEAVY_ARMOR_MODIFIER);
			}
		}
	}
	
	
	public espc_RecoilModPlugin(ShipAPI ship) {
		this.ship = ship;
		doInit();
	}
	
    @Override
	public void advance(float amount, List<InputEventAPI> events) {
    	if (ship == null || ship.isHulk()) {
    		Global.getCombatEngine().removePlugin(this);
    		return;
    	}
		// if (!didInit)
		// 	doInit();
		// if (recoilWep == null)
		// 	doInit();
		if (didInit) {
			if (isBeam && recoilWep.getChargeLevel() > 0f && recoilWep.isFiring()) {
				float damageMod = ship.getMutableStats().getBallisticWeaponDamageMult().getMult() * recoilWep.getChargeLevel();
				Vector2f.add(new Vector2f((float) FastTrig.cos(Math.toRadians(recoilWep.getCurrAngle())) * -knockbackMod * damageMod * amount, 
					(float) FastTrig.sin(Math.toRadians(recoilWep.getCurrAngle())) * -knockbackMod * damageMod * amount), ship.getVelocity(), ship.getVelocity());
			} else if (recoilWep.getCooldownRemaining() > cooldownLast) {
				float damageMod = ship.getMutableStats().getBallisticWeaponDamageMult().getMult();
				Vector2f.add(new Vector2f((float) FastTrig.cos(Math.toRadians(recoilWep.getCurrAngle())) * -knockbackMod * damageMod, 
					(float) FastTrig.sin(Math.toRadians(recoilWep.getCurrAngle())) * -knockbackMod * damageMod), ship.getVelocity(), ship.getVelocity());
			}
			cooldownLast = recoilWep.getCooldownRemaining();
		}
	}
}