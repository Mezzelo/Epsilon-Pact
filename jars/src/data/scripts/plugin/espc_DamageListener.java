package data.scripts.plugin;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import org.lwjgl.util.vector.Vector2f;
import com.fs.starfarer.api.util.Misc;


public class espc_DamageListener implements DamageDealtModifier {
	

	private static final float MINIMIR_IGNORE_PERCENT = 0.5f;
	private static final float MINIMIR_IGNORE_SMALL_DIRECT = 0.5f;
	// private static final float MINIMIR_ARMOR_PERCENT_SMALL = 1.0f;
	private static final float MINIMIR_ARMOR_PERCENT_MED = 1.0f;
	
    public static final float FIS_PROJ_DAMAGE_FRACTION = 3f;

    @Override
    public String modifyDamageDealt(Object proj, CombatEntityAPI targ, DamageAPI damage, Vector2f point, boolean shieldHit) {

		if (damage.getDamage() <= 50) // helps filter out frequent checks from weapons like PD.
			return null;
		// if the base damage is reduced to this amount from what this listener is checking for (100+ dmg)
		// the change in effects for dam values this low will probably be negligible. /cope 
		// i MUST have my performance *does shit that doesn't matter and will probably result in screwy behaviours*
			
        if (!(proj instanceof DamagingProjectileAPI))
			return null;
		
		DamagingProjectileAPI dProj = (DamagingProjectileAPI) proj;
				
		if (dProj.getProjectileSpecId() == null)
			return null;
				
		if (dProj.getProjectileSpecId().equals("espc_minimir_shot") ||
			dProj.getProjectileSpecId().equals("espc_minimirdual_shot")) {
				
			if (!(targ instanceof ShipAPI))
				return null;
				
			ShipAPI shipTarg = (ShipAPI) targ;
			float ignorePortion = MINIMIR_IGNORE_PERCENT;
			if (!shieldHit && (dProj.getProjectileSpecId().equals("espc_minimir_shot")))
				ignorePortion = MINIMIR_IGNORE_SMALL_DIRECT;
			float ignoreDamageAmount = damage.getDamage() * ignorePortion;
			// Global.getLogger(espc_MinimirEffect.class).info(ignoreDamageAmount);
			
			if (dProj.getSource() != null) {
				damage.setDamage(
					damage.getDamage() /
					dProj.getSource().getMutableStats().getBallisticWeaponDamageMult().getMult() / (1f +
					dProj.getSource().getMutableStats().getBallisticWeaponDamageMult().getFlatMod() +
					dProj.getSource().getMutableStats().getBallisticWeaponDamageMult().getPercentMod() / 100f)
				);
			}
			else
				damage.setDamage(damage.getDamage() - ignoreDamageAmount);
			
			MutableShipStatsAPI stats = shipTarg.getMutableStats();
			CombatEngineAPI engine = Global.getCombatEngine();
				
			if (shieldHit) {
				float shieldMult = stats.getShieldDamageTakenMult().getMult();
				if (shieldMult < 1f)
					shieldMult = 2f - shieldMult;
				shipTarg.getFluxTracker().increaseFlux(ignoreDamageAmount * shieldMult, true);
				// Global.getLogger(espc_DamageListener.class).info("dam: " + damage.getDamage());
				// Global.getLogger(espc_DamageListener.class).info("bonus: " + ignoreDamageAmount * shieldMult);
				// Global.getLogger(espc_MinimirEffect.class).info(ignoreDamageAmount);
				if (Misc.shouldShowDamageFloaty(dProj.getSource(), shipTarg))
					engine.addFloatingDamageText(point, ignoreDamageAmount * shieldMult, 
					Misc.FLOATY_SHIELD_DAMAGE_COLOR, shipTarg, dProj.getSource());
				
				return null;
			}
			
			if (shipTarg.isHulk())
				return null;

			float armorMult = stats.getArmorDamageTakenMult().getMult();
			if (armorMult < 1f)
				armorMult = 2f - armorMult;
			
			ArmorGridAPI grid = shipTarg.getArmorGrid();
			int[] cell = grid.getCellAtLocation(point);
			if (cell == null)
				return null;
			
			int gridWidth = grid.getGrid().length;
			int gridHeight = grid.getGrid()[0].length;
			float damageDealt = 0f;
			float hullDamageDealt = 0f;
			
			for (int i = -2; i < 3; i++) {
				for (int j = -2; j < 3; j++) {
					if (i * j == -4 || i * j == 4)
						continue;
					
					int x = cell[0] + i;
					int y = cell[1] + j;

					if (x < 0 || x >= gridWidth || y < 0 || y >= gridHeight)
						continue;
					
					float cellMult = 1/15f;
					// edges
					if (i == -2 || i == 2 || j == -2 || j == 2)
						cellMult = 1/30f;
					
					float armorInCell = grid.getArmorValue(x, y);
					float armorDamage = ignoreDamageAmount * cellMult * armorMult * MINIMIR_ARMOR_PERCENT_MED;
					//	((dProj.getProjectileSpecId().equals("espc_minimirdual_shot")) ? MINIMIR_ARMOR_PERCENT_MED : MINIMIR_ARMOR_PERCENT_SMALL);
					if (armorDamage > armorInCell) {
						shipTarg.setHitpoints(Math.max(shipTarg.getHitpoints() - (armorDamage - armorInCell), 1f));
					// ship disappears otherwise
					if (shipTarg.getHitpoints() <= 1)
						engine.applyDamage(shipTarg, point, 100, DamageType.ENERGY, 0, true, false, dProj.getSource(), false);
						hullDamageDealt += (armorDamage - armorInCell);
					}
					
					damageDealt += Math.min(armorInCell, armorDamage);
					shipTarg.getArmorGrid().setArmorValue(x, y, Math.max(0, armorInCell - armorDamage));
				}
			}
					
			if (hullDamageDealt > 0) {
				if (Misc.shouldShowDamageFloaty(dProj.getSource(), shipTarg))
					engine.addFloatingDamageText(point, hullDamageDealt, 
					Misc.FLOATY_HULL_DAMAGE_COLOR, shipTarg, dProj.getSource());
			}
			
			if (damageDealt > 0) {
				if (Misc.shouldShowDamageFloaty(dProj.getSource(), shipTarg))
					engine.addFloatingDamageText(point, damageDealt, 
					Misc.FLOATY_ARMOR_DAMAGE_COLOR, shipTarg, dProj.getSource());
				shipTarg.syncWithArmorGridState();
			}
			
			return null;
		} else if (dProj.getProjectileSpecId().equals("espc_fission_shot")) {
			if (dProj.getSource() != null) {
				damage.setDamage(
					damage.getDamage() / FIS_PROJ_DAMAGE_FRACTION /
					dProj.getSource().getMutableStats().getEnergyWeaponDamageMult().getMult() / (1f +
					dProj.getSource().getMutableStats().getEnergyWeaponDamageMult().getFlatMod() +
					dProj.getSource().getMutableStats().getEnergyWeaponDamageMult().getPercentMod() / 100f)
				);
			}
			return null;
		} else if (((dProj.getProjectileSpecId().equals("espc_riftpike_shot") ||
			dProj.getProjectileSpecId().equals("espc_riftspear_shot"))) && shieldHit) {
			// if something ends up reducing rift pike damage to a 10th of its original value im making it deal hard flux
			// fuck you
			damage.setSoftFlux(true);
			return null;
		}
		
        return null;
    }
}
