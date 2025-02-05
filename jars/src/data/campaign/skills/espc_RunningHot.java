package data.campaign.skills;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.AfterShipCreationSkillEffect;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShieldAPI.ShieldType;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

public class espc_RunningHot {
	
	public static float FIRE_RATE_BONUS = 40f;
	public static float FLUX_USAGE_BONUS = 20f;
	public static float FLUX_MIN_THRESHOLD = 30f;
	public static float DISSIPATION_BONUS_FLUX = 50f;
	public static float DISSIPATION_BONUS_OVERLOAD = 100f;
	
//	public static float FLAGSHIP_SPEED_BONUS = 25f;
//	public static float FLAGSHIP_CP_BONUS = 100f;

	public static Object DISSIPATION_BONUS_STATUS_KEY = new Object();
	public static Object DAM_BONUS_STATUS_KEY = new Object();
	
	public static class RunningHotEffectMod implements AdvanceableListener {
		protected ShipAPI ship;
		protected String id;
		protected boolean hasShield;
		public RunningHotEffectMod(ShipAPI ship, String id) {
			this.ship = ship;
			this.id = id;
			this.hasShield = ship.getShield() != null && ship.getShield().getType() != ShieldType.PHASE &&
				(ship.getVariant() == null || ship.getVariant() != null && !ship.getVariant().hasHullMod("safetyoverrides"));
		}
		
		public void advance(float amount) {
			if (Global.getCurrentState() != GameState.COMBAT)
				return;
 			MutableShipStatsAPI stats = ship.getMutableStats();
			
			float fluxLevel = ship.getFluxLevel();
			if (Float.isNaN(fluxLevel))
				fluxLevel = 0f;
			fluxLevel = Math.max(0f, fluxLevel - FLUX_MIN_THRESHOLD/100f) * 100f/(100f - FLUX_MIN_THRESHOLD);
			stats.getBallisticRoFMult().modifyPercent(id, fluxLevel * FIRE_RATE_BONUS);
			stats.getEnergyRoFMult().modifyPercent(id, fluxLevel * FIRE_RATE_BONUS);
			stats.getFluxDissipation().modifyPercent(id, fluxLevel * DISSIPATION_BONUS_FLUX * (hasShield ? 1f : 0.5f) +
				(ship.getFluxTracker().isOverloaded() ? (DISSIPATION_BONUS_OVERLOAD) : 0f));
			
			if (Global.getCombatEngine() != null && Global.getCombatEngine().getPlayerShip() == ship) {

				if ((int)(fluxLevel * FIRE_RATE_BONUS) > 0) {
					Global.getCombatEngine().maintainStatusForPlayerShip(DISSIPATION_BONUS_STATUS_KEY,
						Global.getSettings().getSpriteName("ui", "icon_energy"),
						"Running hot", 
						"+" + (int)(fluxLevel * DISSIPATION_BONUS_FLUX * (hasShield ? 1f : 0.5f)) + "% flux dissipation", false);
					Global.getCombatEngine().maintainStatusForPlayerShip(DAM_BONUS_STATUS_KEY,
						Global.getSettings().getSpriteName("ui", "icon_energy"),
						"Running hot", 
						"+" + (int)(fluxLevel * FIRE_RATE_BONUS) + "% non-missile fire rate", false);
				}
				
			}
		}

	}
	
	public static class RunningHotEffectModElite implements AdvanceableListener {
		protected ShipAPI ship;
		protected String id;
		public RunningHotEffectModElite(ShipAPI ship, String id) {
			this.ship = ship;
			this.id = id + "elite";
		}
		
		public void advance(float amount) {
			if (Global.getCurrentState() != GameState.COMBAT)
				return;
 			ship.getMutableStats().getFluxDissipation().modifyPercent(id, 
				ship.getFluxTracker().isOverloaded() ? (DISSIPATION_BONUS_OVERLOAD) : 0f);
		}

	}
	
	public static class Level1 extends BaseSkillEffectDescription implements AfterShipCreationSkillEffect {
		public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
			ship.addListener(new RunningHotEffectMod(ship, id));
		}
		public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
			ship.removeListenerOfClass(RunningHotEffectMod.class);
			ship.getMutableStats().getBallisticRoFMult().unmodify(id);
			ship.getMutableStats().getEnergyRoFMult().unmodify(id);
			ship.getMutableStats().getFluxDissipation().unmodify(id);
		}

		public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {}
		public void unapply(MutableShipStatsAPI stats, HullSize hullSize, String id)  {}

		public void createCustomDescription(MutableCharacterStatsAPI stats, SkillSpecAPI skill, 
			TooltipMakerAPI info, float width) {
			init(stats, skill);
			info.addPara("Up to +%s ballistic and energy fire rate based on current flux level, "
					+ "starting at %s of flux capacity",
					0f, hc, hc,
					(int)Math.round(FIRE_RATE_BONUS) + "%", (int)FLUX_MIN_THRESHOLD + "%"
				);
			info.addPara("Up to +%s flux dissipation based on current flux level, "
				+ "starting at %s of flux capacity",
				0f, hc, hc,
				(int)Math.round(DISSIPATION_BONUS_FLUX) + "%", (int)FLUX_MIN_THRESHOLD + "%"
			);
			info.addPara(indent + "Dissipation bonus is %s for ships with safety overrides or without shields",
				0f, tc, hc, "halved"
			);
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
			ship.addListener(new RunningHotEffectModElite(ship, id));
		}
		public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
			ship.removeListenerOfClass(RunningHotEffectModElite.class);
			ship.getMutableStats().getFluxDissipation().unmodify(id + "elite");
		}
		public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {}
		public void unapply(MutableShipStatsAPI stats, HullSize hullSize, String id) {}
		
		public String getEffectDescription(float level) {
			return "+" + (int)Math.round(DISSIPATION_BONUS_OVERLOAD) + "% flux dissipation while overloaded";
		}
		public String getEffectPerLevelDescription() {
			return null;
		}
		public ScopeDescription getScopeDescription() {
			return ScopeDescription.PILOTED_SHIP;
		}
	}
}
