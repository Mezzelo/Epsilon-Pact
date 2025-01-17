package data.campaign.skills;

import java.awt.Color;
import java.util.Iterator;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
// import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.AfterShipCreationSkillEffect;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.ShipSkillEffect;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

public class espc_Unburdened {

	public static float HULL_PERCENT_PER_SECOND = 1.0f;
	public static float HEAL_RATIO = 2f;
	public static float HEAL_RANGE = 700f;
	public static float MAX_SPEND = 50f;
	public static float MIN_THRESHOLD = 25f;
	public static float MIN_TARGET_THRESHOLD = 90f;
	public static float HULL_DAMAGE_REDUCTION = 25f;
	
	public static class UnburdenedEffectMod implements AdvanceableListener {
		protected ShipAPI ship;
		protected String id;
		protected float hullRemaining = 0f;
		protected float healDuration = 0f;
		public UnburdenedEffectMod(ShipAPI ship, String id) {
			this.ship = ship;
			this.id = id;
			hullRemaining = ship.getMaxHitpoints() * (MAX_SPEND / 100f);
		}
		
		public void advance(float amount) {
			if (Global.getCurrentState() != GameState.COMBAT ||
				amount <= 0f || hullRemaining <= 0f || ship.getHullLevel() <= MIN_THRESHOLD/100f ||
				ship.isPhased() || ship.getFluxTracker().isOverloaded())
				return;
			CombatEngineAPI combatEngine = Global.getCombatEngine();
			if (combatEngine == null)
				return;
			Iterator<Object> entityIterator = combatEngine.getAllObjectGrid().getCheckIterator(
				ship.getLocation(), 
				(HEAL_RANGE + ship.getShieldRadiusEvenIfNoShield()) * 2f,
				(HEAL_RANGE + ship.getShieldRadiusEvenIfNoShield()) * 2f
			);
			ShipAPI target = null;
			float lowestHull = 999999f;
			while (entityIterator.hasNext()) {
				CombatEntityAPI entity = (CombatEntityAPI) entityIterator.next();
				if (entity instanceof ShipAPI) {
					if (entity == ship)
						continue;
					ShipAPI check = (ShipAPI) entity;
					if (check.getOwner() == ship.getOwner() && ship.getOwner() < 100 && !check.isFighter() && check.getFleetMember() != null &&
						!check.isPhased() && !check.getFluxTracker().isOverloaded() &&
						check.getHitpoints() < lowestHull && check.getHullLevel() < MIN_TARGET_THRESHOLD/100f) {
						target = check;
						lowestHull = check.getHitpoints();
					}
				}
			}
			if (target != null) {
				float heal = Math.min(hullRemaining, 
					Math.min(target.getMaxHitpoints() - target.getHitpoints(), ship.getMaxHitpoints() * amount * HULL_PERCENT_PER_SECOND / 100f));
				// min (remaining, target hull deficit, heal rate)
				// 
				hullRemaining -= heal;
				ship.setHitpoints(ship.getHitpoints() - heal);
				target.setHitpoints(target.getHitpoints() + heal);
				if (ship.getHullLevel() > MIN_THRESHOLD/100f &&
					target.getHullLevel() < MIN_TARGET_THRESHOLD/100f) {
					// ship.setJitterShields(true);
					target.setJitterShields(false);
					target.setJitterUnder(this, new Color(0, 1f, 0.4f, 0.4f), 1f, 2, 20f);
					// target.setJitterShields(true);
				}
				healDuration = Math.min(1f, healDuration + amount);
			} else if (healDuration > 0f)
				healDuration = Math.max(0f, healDuration - amount);
			if (healDuration > 0f) {
				ship.setJitterShields(false);
				ship.setJitterUnder(this, new Color(0, 1f, 0.4f, 
				Math.max(0, (1f - (combatEngine.getTotalElapsedTime(false) % 1f) * 1.4f) * healDuration)), 
				1f, 
				2,
				(combatEngine.getTotalElapsedTime(false) % 1f) * 50f,
				(combatEngine.getTotalElapsedTime(false) % 1f) * 50f);
			}

		}
	}
	
	public static class Level1 extends BaseSkillEffectDescription implements AfterShipCreationSkillEffect {
		public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
			ship.addListener(new UnburdenedEffectMod(ship, id));
		}
		public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
			ship.removeListenerOfClass(UnburdenedEffectMod.class);
		}

		public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {}
		public void unapply(MutableShipStatsAPI stats, HullSize hullSize, String id)  {}
		
		public String getEffectDescription(float level) {
			return null;
		}
		
		public void createCustomDescription(MutableCharacterStatsAPI stats, SkillSpecAPI skill, 
			TooltipMakerAPI info, float width) {

			init(stats, skill);
			info.addPara("Ship sacrifices its hull at %s of max hull per second to repair nearby allies for twice the hull spent.",
				0f, hc, hc, HULL_PERCENT_PER_SECOND + "%"
			);
			info.addPara("Affects allies below %s hull and within %s, prioritizing allies with the lowest hull remaining",
				0f, hc, hc, (int) MIN_TARGET_THRESHOLD + "%", (int)HEAL_RANGE + " su"
			);
			info.addPara(indent + "Up to %s of maximum hull can be used. This effect pauses when below %s of maximum hull",
				0f, tc, hc, (int)MAX_SPEND + "%", (int)MIN_THRESHOLD + "%"
			);
			info.addPara(indent + "Does not affect allies while either ship is overloaded or phased",
				0f, tc, hc
			);
		}
		
		public String getEffectPerLevelDescription() {
			return null;
		}
		
		public ScopeDescription getScopeDescription() {
			return ScopeDescription.PILOTED_SHIP;
		}
	}
	
	
	public static class Level2 implements ShipSkillEffect {
		
		public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {
			stats.getHullDamageTakenMult().modifyPercent(id, -HULL_DAMAGE_REDUCTION);
		}
		public void unapply(MutableShipStatsAPI stats, HullSize hullSize, String id) {
			stats.getHullDamageTakenMult().unmodify();
		}
		
		public String getEffectDescription(float level) {
			return "-" + (int)HULL_DAMAGE_REDUCTION + "% hull damage taken";
		}
		public String getEffectPerLevelDescription() {
			return null;
		}
		public ScopeDescription getScopeDescription() {
			return ScopeDescription.PILOTED_SHIP;
		}
	}
}
