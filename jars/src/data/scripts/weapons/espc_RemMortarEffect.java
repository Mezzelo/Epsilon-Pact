package data.scripts.weapons;

import java.awt.Color;
import java.util.Iterator;
import java.util.List;

import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.FastTrig;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.OnHitEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponGroupAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.util.Misc;

import data.scripts.weapons.proj.espc_RemMortarVFX;

// force staggers alternating remise mortars, as the game should but doesn't smile
// will technically cause undesired behaviour with linked mortars, but wyd
public class espc_RemMortarEffect implements OnFireEffectPlugin, OnHitEffectPlugin {
	
	// could theoretically run into issues with a fast enough fire rate, but that needs to be like 10x to take effect
	private static final float CD_SPACING = 0.25f;
	
	private static final float TARGET_RADIUS_MIN = 120f;
	private static final float RANGE_MULT = 1.15f;
	
	private int count = 0;
	private WeaponGroupAPI group;
	
    public void onFire(DamagingProjectileAPI proj, WeaponAPI weapon, CombatEngineAPI engine) {
    	if (weapon.getShip() == null)
    		return;
    	
		if (!weapon.getShip().getCustomData().containsKey("espc_RemMortarPlugin")) {
			espc_RemMortarVFX plugin = new espc_RemMortarVFX(weapon.getShip());
	        engine.addPlugin(plugin);
	        weapon.getShip().setCustomData("espc_RemMortarPlugin", plugin);
	        plugin.addProj(proj);
		} else
    		((espc_RemMortarVFX) weapon.getShip().getCustomData().get("espc_RemMortarPlugin")).addProj(proj);
    	
    	if (count == -1)
    		return;
    	else if (group != null && group.equals(weapon.getShip().getSelectedGroupAPI()))
    		return;
    	else if (count == 0) {
        	ShipAPI ship = weapon.getShip();
        	
        	for (WeaponGroupAPI currGroup : ship.getWeaponGroupsCopy()) {
        		boolean foundSelf = false;
        		boolean allMortars = true;
        		count = 0;
        		for (WeaponAPI wep : currGroup.getWeaponsCopy()) {
        			if (!wep.getId().equals("espc_remmortar"))
        				allMortars = false;
        			else {
        				count++;
        			}
        			if (weapon.equals(wep))
        				foundSelf = true;
        		}
        		if (count > 1 && allMortars && foundSelf) {
        			group = currGroup;
        			break;
        		}
        	}
        	if (count <= 1) {	
        		count = -1;
        		return;
        	} else if (group.equals(weapon.getShip().getSelectedGroupAPI()))
        		return;
    	}
    	
    	for (WeaponAPI wep : group.getWeaponsCopy()) {
    		if (weapon.equals(wep))
    			continue;
    		if ((weapon.getCooldownRemaining() - wep.getCooldownRemaining() + CD_SPACING * 0.65f) % weapon.getCooldown() < CD_SPACING) {
    			wep.setRemainingCooldownTo(wep.getCooldownRemaining() + CD_SPACING);
    		}
    	}
    }
	
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
		
		boolean anyProcced = false;
		
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
					anyProcced = true;
				}
			}
		}
		if (anyProcced)
	        MagicRender.battlespace(
	    		Global.getSettings().getSprite("fx", "espc_mortarhit"),
	    		point,
	    		Misc.ZERO,
	    		new Vector2f(15f, 15f),
	    		new Vector2f(800f, 800f),
	    		0f,
	    		0f,
	    		new Color(255, 30, 0, 60),
	    		true,
	    		0.0f,
	    		0.04f,
	    		0.14f
	    	);
    }
}