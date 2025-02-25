package data.scripts.weapons.proj;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;

import java.util.Iterator;
import java.util.List;

import org.lwjgl.util.vector.Vector2f;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.FastTrig;
				
public class espc_RemiseMortarProjEffect implements OnHitEffectPlugin {
	
	private static final float TARGET_RADIUS_MIN = 120f;
	private static final float RANGE_MULT = 1.15f;
	
    @Override
    public void onHit(
    	DamagingProjectileAPI proj, 
    	CombatEntityAPI target, 
    	Vector2f point, 
    	boolean shieldHit, 
    	ApplyDamageResultAPI damage, 
    	CombatEngineAPI engine
	) {
		if (target instanceof DamagingProjectileAPI || !proj.didDamage() || proj.getSource() == null)
			return;

		ShipAPI ship = proj.getSource();
		
		if (ship == null || ship.getFluxTracker().isOverloadedOrVenting() || ship.isHulk() || !ship.isAlive())
			return;
		
		List<WeaponAPI> weapons = ship.getAllWeapons();
		Iterator<WeaponAPI> weaponIterator = weapons.iterator();
		boolean hasDriver = false;
		while (weaponIterator.hasNext()) {
			WeaponAPI currWeapon = (WeaponAPI) weaponIterator.next();
			if (currWeapon.getId().equals("espc_remdriver")) {
				hasDriver = true;
				break;
			}
		}
		
		if (!hasDriver)
			return;
		
		weaponIterator = weapons.iterator();
		while (weaponIterator.hasNext()) {
			WeaponAPI currWeapon = (WeaponAPI) weaponIterator.next();
			if (!currWeapon.isDisabled() && currWeapon.getId().equals("espc_remdriver")) {
				Vector2f ray = new Vector2f(
					(float) FastTrig.cos(Math.toRadians(currWeapon.getCurrAngle())), 
					(float) FastTrig.sin(Math.toRadians(currWeapon.getCurrAngle())));
				ray.scale(currWeapon.getRange() * RANGE_MULT);
				if (CollisionUtils.getCollides(
					currWeapon.getLocation(), 
					Vector2f.add(currWeapon.getFirePoint(0), ray, new Vector2f()),
					target.getLocation(),
					Math.max(target.getCollisionRadius() * 1.15f, TARGET_RADIUS_MIN)
				)) {
					currWeapon.setRemainingCooldownTo(0f);
				}
			}
		}
    }
	
}
