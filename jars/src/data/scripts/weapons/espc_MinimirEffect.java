// note: initial damage reduction doesn't take effect for projectiles that are spawned in.
// as damagingprojectileapi is a subinterface of combatentityapi, we can write custom data to solve this.

package data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import data.scripts.plugin.espc_DamageListener;

public class espc_MinimirEffect implements OnFireEffectPlugin {
	
	boolean hasListener = false;
	
	// no way to only have an init without an every frame effect, as far as i can tell.
    @Override
    public void onFire(DamagingProjectileAPI proj, WeaponAPI weapon, CombatEngineAPI engine) {
		
		if (hasListener)
			return;
		
		if (!weapon.getShip().hasListenerOfClass(espc_DamageListener.class)){
			weapon.getShip().addListener(new espc_DamageListener());
			hasListener = true;
		} else
			hasListener = true;
		
		// setDamageAmount appears to reapply mutable stats, so we have to manually reverse it here.
        // proj.setDamageAmount(proj.getDamageAmount() * (1f - ignorePercent) / 
		// 	weapon.getShip().getMutableStats().getBallisticWeaponDamageMult().getMult());
    }

	/*
	// percentage of damage that ignores shield/armor
	private static final float ignorePercent = 0.5f;
	private static final float armorPercentSmall = 0.5f;
	private static final float armorPercentMed = 1.0f;
    @Override
    public void onHit(DamagingProjectileAPI proj, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damage, CombatEngineAPI engine)
    {
		
		if (target instanceof MissileAPI) {
			target.setHitpoints(Math.max(target.getHitpoints() - proj.getDamageAmount() / (1f - ignorePercent) * ignorePercent, 1f));
			// ship disappears otherwise
			if (target.getHitpoints() <= 1)
				engine.applyDamage(target, point, 10, DamageType.ENERGY, 0, true, false, proj.getSource(), false);
		}
		
        if (!(target instanceof ShipAPI))
			return;
        if (proj.getDamageAmount() <= 0) // modded funny business, if anyone has the absolute gall
			return;
			
		ShipAPI shipTarg = (ShipAPI) target;
		float ignoreDamageAmount = proj.getDamageAmount() / (1f - ignorePercent) * ignorePercent;
		// Global.getLogger(espc_MinimirEffect.class).info(proj.getDamageAmount());
		// Global.getLogger(espc_MinimirEffect.class).info(ignoreDamageAmount);
		
		MutableShipStatsAPI stats = shipTarg.getMutableStats();
		
		if (shieldHit) {
			float shieldMult = stats.getShieldDamageTakenMult().getMult();
			if (shieldMult < 1f)
				shieldMult = 2f - shieldMult;
			shipTarg.getFluxTracker().increaseFlux(ignoreDamageAmount * shieldMult, true);
			// Global.getLogger(espc_MinimirEffect.class).info(ignoreDamageAmount);
			if (Misc.shouldShowDamageFloaty(proj.getSource(), shipTarg))
				engine.addFloatingDamageText(point, ignoreDamageAmount * shieldMult, 
				Misc.FLOATY_SHIELD_DAMAGE_COLOR, shipTarg, proj.getSource());
		}
		else if (!shipTarg.isHulk()) {
			// i've found this scripted armor damage in multiple mods, so assuming it's alex's work.  if i have infringed on your code without permission please notify me.

			float armorMult = stats.getArmorDamageTakenMult().getMult();
			if (armorMult < 1f)
				armorMult = 2f - armorMult;
			
			ArmorGridAPI grid = shipTarg.getArmorGrid();
			int[] cell = grid.getCellAtLocation(point);
			if (cell == null)
				return;
			
			int gridWidth = grid.getGrid().length;
			int gridHeight = grid.getGrid()[0].length;

			float damageDealt = 0f;
			float hullDamageDealt = 0f;
			for (int i = -2; i <= 2; i++) {
				for (int j = -2; j <= 2; j++) {
					if ((i == 2 || i == -2) && (j == 2 || j == -2)) continue; // corners
						int cx = cell[0] + i;
						int cy = cell[1] + j;

					if (cx < 0 || cx >= gridWidth || cy < 0 || cy >= gridHeight) continue;

					float damMult = 1/30f;
					if (i == 0 && j == 0) {
						damMult = 1/15f;
					} else if (i <= 1 && i >= -1 && j <= 1 && j >= -1) {
						damMult = 1/15f;
					} else {
						damMult = 1/30f;
					}

					float armorInCell = grid.getArmorValue(cx, cy);
					float armorDamage = ignoreDamageAmount * damMult * armorMult *
						((proj.getProjectileSpecId().equals("espc_minimirdual_shot")) ? armorPercentMed : armorPercentSmall);
					if (armorDamage > armorInCell) {
						shipTarg.setHitpoints(Math.max(shipTarg.getHitpoints() - (armorDamage - armorInCell), 1f));
						// ship disappears otherwise
						if (shipTarg.getHitpoints() <= 1)
							engine.applyDamage(shipTarg, point, 100, DamageType.ENERGY, 0, true, false, proj.getSource(), false);
						hullDamageDealt += (armorDamage - armorInCell);
					}
					damageDealt += Math.min(armorInCell, armorDamage);
					shipTarg.getArmorGrid().setArmorValue(cx, cy, Math.max(0, armorInCell - armorDamage));
				}
			}

			if (hullDamageDealt > 0) {
				if (Misc.shouldShowDamageFloaty(proj.getSource(), shipTarg))
					engine.addFloatingDamageText(point, hullDamageDealt, 
					Misc.FLOATY_HULL_DAMAGE_COLOR, shipTarg, proj.getSource());
			}
			
			if (damageDealt > 0) {
				if (Misc.shouldShowDamageFloaty(proj.getSource(), shipTarg))
					engine.addFloatingDamageText(point, damageDealt, 
					Misc.FLOATY_ARMOR_DAMAGE_COLOR, shipTarg, proj.getSource());
			}
        }
    }
	*/
}