package data.campaign.skills;

import java.util.List;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
// import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.AfterShipCreationSkillEffect;
import com.fs.starfarer.api.characters.ShipSkillEffect;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription;

public class espc_VoidEmbrace {

	public static float DAMAGE_BONUS_HULL = 35f;
	public static float DAMAGE_BONUS_CR = 25f;
	public static float SPEED_BONUS_CR = 35f;
	public static float CR_THRESHOLD = 50f;
	public static float REPAIR_BONUS = 200f;

	public static Object DAMAGE_BONUS_STATUS_KEY = new Object();
	public static Object SPEED_BONUS_STATUS_KEY = new Object();
	
	public static class VoidEmbraceEffectMod implements AdvanceableListener {
		protected ShipAPI ship;
		protected String id;
		public VoidEmbraceEffectMod(ShipAPI ship, String id) {
			this.ship = ship;
			this.id = id;
		}
		
		public void advance(float amount) {
			MutableShipStatsAPI stats = ship.getMutableStats();
			if (ship.getHullLevel() < 1f) {
				stats.getBallisticWeaponDamageMult().modifyPercent(id, DAMAGE_BONUS_HULL * (1f - ship.getHullLevel()));
				stats.getEnergyWeaponDamageMult().modifyPercent(id, DAMAGE_BONUS_HULL * (1f - ship.getHullLevel()));
				stats.getMissileWeaponDamageMult().modifyPercent(id, DAMAGE_BONUS_HULL * (1f - ship.getHullLevel()));
			}
			if (ship.getCurrentCR() < CR_THRESHOLD)
				stats.getMaxSpeed().modifyPercent(id, (CR_THRESHOLD - ship.getCurrentCR())/CR_THRESHOLD * SPEED_BONUS_CR);

			if (Global.getCurrentState() == GameState.COMBAT &&
					Global.getCombatEngine() != null && Global.getCombatEngine().getPlayerShip() == ship) {

				if (ship.getCurrentCR() < CR_THRESHOLD)
					Global.getCombatEngine().maintainStatusForPlayerShip(SPEED_BONUS_STATUS_KEY,
						Global.getSettings().getSpriteName("ui", "icon_energy"),
						"Void embrace", 
						"+" + (int)(((CR_THRESHOLD - ship.getCurrentCR())/CR_THRESHOLD * SPEED_BONUS_CR)/100f * ship.getMaxSpeed())
						+ " max speed", false);
				if ((int)(DAMAGE_BONUS_HULL * (1f - ship.getHullLevel())) > 0)
					Global.getCombatEngine().maintainStatusForPlayerShip(DAMAGE_BONUS_STATUS_KEY,
						Global.getSettings().getSpriteName("ui", "icon_energy"),
						"Void embrace", 
						"+" + (int)(DAMAGE_BONUS_HULL * (1f - ship.getHullLevel())) + "% weapon damage", false);
				
			}
		}

	}

	public static class VoidEmbraceEffectModElite implements AdvanceableListener {
		protected ShipAPI ship;
		protected String id;
		protected float cooldown = 0f;
		protected boolean wasOverloaded = false;
		public VoidEmbraceEffectModElite(ShipAPI ship, String id) {
			this.ship = ship;
			this.id = id;
		}
		
		public void advance(float amount) {
			int total = 0;
			int disabled = 0;
			List<WeaponAPI> weapons = ship.getAllWeapons();
			for (WeaponAPI wep : weapons) {
				if (!wep.isDecorative() && !wep.getSlot().isHidden() && (wep.getSlot().isWeaponSlot() || wep.getSlot().isBuiltIn()))
					total++;
					if ((wep.isDisabled() || wep.isPermanentlyDisabled()))
						disabled++;
			}
			if (disabled > total) {
				ship.getMutableStats().getCombatWeaponRepairTimeMult().modifyMult(id, 100f / (100f + REPAIR_BONUS * (float)disabled/total));
				ship.getMutableStats().getCombatEngineRepairTimeMult().modifyPercent(id, 100f / (100f + REPAIR_BONUS * (float)disabled/total));
			}
		}

	}
	
	public static class Level1 implements AfterShipCreationSkillEffect {
		public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
			ship.addListener(new VoidEmbraceEffectMod(ship, id));
		}
		public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
			ship.removeListenerOfClass(VoidEmbraceEffectMod.class);
			ship.getMutableStats().getBallisticWeaponDamageMult().unmodify(id);
			ship.getMutableStats().getEnergyWeaponDamageMult().unmodify(id);
			ship.getMutableStats().getMissileWeaponDamageMult().unmodify(id);
		}

		public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {}
		public void unapply(MutableShipStatsAPI stats, HullSize hullSize, String id)  {}
		
		public String getEffectDescription(float level) {
			return "Up to +" + (int)DAMAGE_BONUS_HULL + " weapon damage, scaling with missing hull.";
		}
		
		public String getEffectPerLevelDescription() {
			return null;
		}
		
		public ScopeDescription getScopeDescription() {
			return ScopeDescription.PILOTED_SHIP;
		}
	}
	
	
	public static class Level2 implements ShipSkillEffect {
		public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {}
		public void unapply(MutableShipStatsAPI stats, HullSize hullSize, String id) {}
		
		public String getEffectDescription(float level) {
			return "Up to " + (int)SPEED_BONUS_CR + "% speed as combat readiness decreases, beginning at 50 CR";
		}
		public String getEffectPerLevelDescription() {
			return null;
		}
		public ScopeDescription getScopeDescription() {
			return ScopeDescription.PILOTED_SHIP;
		}
	}
	
	public static class Level3 implements AfterShipCreationSkillEffect {
		public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
			ship.addListener(new VoidEmbraceEffectModElite(ship, id));
		}
		public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
			ship.removeListenerOfClass(VoidEmbraceEffectModElite.class);
			ship.getMutableStats().getCombatWeaponRepairTimeMult().unmodify(id);
			ship.getMutableStats().getCombatEngineRepairTimeMult().unmodify(id);
		}
		
		public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {}
		public void unapply(MutableShipStatsAPI stats, HullSize hullSize, String id) {}
		
		public String getEffectDescription(float level) {
			return "Up to " + (int)REPAIR_BONUS + "% faster in combat weapon and engine repair speed, based on the proportion of disabled weapons to the total.";
		}
		public String getEffectPerLevelDescription() {
			return null;
		}
		public ScopeDescription getScopeDescription() {
			return ScopeDescription.PILOTED_SHIP;
		}
	}
}
