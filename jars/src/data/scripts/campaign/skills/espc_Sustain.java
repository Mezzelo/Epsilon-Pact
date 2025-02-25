package data.scripts.campaign.skills;

import java.util.List;

import com.fs.starfarer.api.characters.AfterShipCreationSkillEffect;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;

public class espc_Sustain {
	
	public static float CAPACITY_PER_OP = 80f;
	public static float DISSIPATION_PER_OP = 8f;
	
	public static class Level1 implements AfterShipCreationSkillEffect {
		public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
			MutableShipStatsAPI stats = ship.getMutableStats();
			int mounted = 0;
 			
 			List<WeaponAPI> weapons = ship.getAllWeapons();
 			for (WeaponAPI wep : weapons) {
 				if (wep.getType() != WeaponType.MISSILE && 
 					(wep.getSlot().getWeaponType() == WeaponType.UNIVERSAL || 
 					wep.getSlot().getWeaponType() == WeaponType.COMPOSITE ||
 					wep.getSlot().getWeaponType() == WeaponType.SYNERGY)) {
 					mounted += wep.getSpec().getOrdnancePointCost(null, stats);
 				}
 			}
 			stats.getFluxDissipation().modifyFlat(id, DISSIPATION_PER_OP * mounted);
		}
		public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
			MutableShipStatsAPI stats = ship.getMutableStats();
			stats.getFluxDissipation().unmodify(id);
		}

		public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {}
		public void unapply(MutableShipStatsAPI stats, HullSize hullSize, String id)  {}
		
		public String getEffectDescription(float level) {
			return "+" + (int)(DISSIPATION_PER_OP) + " flux dissipation per ordnance point spent on "
				+ "non-missile weapons in composite, synergy or universal slots.";
		}
		
		public String getEffectPerLevelDescription() {
			return null;
		}
		
		public ScopeDescription getScopeDescription() {
			return ScopeDescription.PILOTED_SHIP;
		}
	}
	
	public static class Level2 implements AfterShipCreationSkillEffect {
		public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
			MutableShipStatsAPI stats = ship.getMutableStats();
			int mounted = 0;
 			
 			List<WeaponAPI> weapons = ship.getAllWeapons();
 			for (WeaponAPI wep : weapons) {
 				if (wep.getType() != WeaponType.MISSILE && 
 					(wep.getSlot().getWeaponType() == WeaponType.UNIVERSAL || 
 					wep.getSlot().getWeaponType() == WeaponType.COMPOSITE ||
 					wep.getSlot().getWeaponType() == WeaponType.SYNERGY)) {
 					mounted += wep.getSpec().getOrdnancePointCost(null, stats);
 				}
 			}
 			stats.getFluxCapacity().modifyFlat(id, CAPACITY_PER_OP * mounted);
		}
		public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
			MutableShipStatsAPI stats = ship.getMutableStats();
			stats.getFluxCapacity().unmodify(id);
		}
		
		public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {}
		
		public void unapply(MutableShipStatsAPI stats, HullSize hullSize, String id) {}
		
		public String getEffectDescription(float level) {
			return "+" + (int)(CAPACITY_PER_OP) + " flux capacity per ordnance point spent on "
				+ "non-missile weapons in composite, synergy or universal slots.";
		}
		
		public String getEffectPerLevelDescription() {
			return null;
		}
		
		public ScopeDescription getScopeDescription() {
			return ScopeDescription.PILOTED_SHIP;
		}
	}
}
