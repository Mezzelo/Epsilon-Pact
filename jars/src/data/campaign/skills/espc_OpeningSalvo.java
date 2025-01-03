package data.campaign.skills;


import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
// import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.AfterShipCreationSkillEffect;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.ShipSkillEffect;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

public class espc_OpeningSalvo {

	public static float DAMAGE_BONUS = 20f;
	public static float DAMAGE_REDUCTION = 30f;
	public static float SPEED_BONUS = 30f;
	public static float FULL_EFFECT_TIME = 60f;
	public static float DECAY_TIME = 60f;
	public static float CR_THRESHOLD = 50f;
	public static float VENT_RATE_BONUS = 25f;

	public static Object DAMAGE_BONUS_STATUS_KEY = new Object();
	public static Object SPEED_BONUS_STATUS_KEY = new Object();
	
	public static class OpeningSalvoEffectMod implements AdvanceableListener {
		protected ShipAPI ship;
		protected String id;
		protected float peakTime = 300f;
		public OpeningSalvoEffectMod(ShipAPI ship, String id) {
			this.ship = ship;
			this.id = id;
			this.peakTime = ship.getPeakTimeRemaining();
		}
		
		public void advance(float amount) {
			MutableShipStatsAPI stats = ship.getMutableStats();
			float effectiveness = (DECAY_TIME - Math.max(0, peakTime - ship.getPeakTimeRemaining() - FULL_EFFECT_TIME))/DECAY_TIME;
			if (effectiveness > 0f) {
				if (peakTime - ship.getPeakTimeRemaining() + amount > (FULL_EFFECT_TIME + DECAY_TIME)) {
					ship.getMutableStats().getArmorDamageTakenMult().unmodify(id);
					ship.getMutableStats().getShieldDamageTakenMult().unmodify(id);
					ship.getMutableStats().getHullDamageTakenMult().unmodify(id);
				} else {
					stats.getShieldDamageTakenMult().modifyPercent(id, DAMAGE_REDUCTION * effectiveness);
					stats.getArmorDamageTakenMult().modifyPercent(id, DAMAGE_REDUCTION * effectiveness);
					stats.getHullDamageTakenMult().modifyPercent(id, DAMAGE_REDUCTION * effectiveness);

					if (Global.getCurrentState() == GameState.COMBAT && 
						Global.getCombatEngine() != null && Global.getCombatEngine().getPlayerShip() == ship)
						Global.getCombatEngine().maintainStatusForPlayerShip(DAMAGE_BONUS_STATUS_KEY,
							Global.getSettings().getSpriteName("ui", "icon_energy"),
							"Opening salvo", 
							"-" + (int)(DAMAGE_REDUCTION * effectiveness) + "% damage taken", false);
				}
			}
			if (ship.getCurrentCR() < CR_THRESHOLD) {
				stats.getMaxSpeed().modifyPercent(id, SPEED_BONUS * (CR_THRESHOLD - ship.getCurrentCR())/CR_THRESHOLD);
				if (Global.getCurrentState() == GameState.COMBAT &&
					Global.getCombatEngine() != null && Global.getCombatEngine().getPlayerShip() == ship)
					Global.getCombatEngine().maintainStatusForPlayerShip(SPEED_BONUS_STATUS_KEY,
						Global.getSettings().getSpriteName("ui", "icon_energy"),
						"Opening salvo", 
						"+" + (int)(SPEED_BONUS * (CR_THRESHOLD - ship.getCurrentCR())/CR_THRESHOLD) + "% top speed", false);
			}

		}

	}
	
	public static class Level1 extends BaseSkillEffectDescription implements AfterShipCreationSkillEffect {
		public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
			ship.addListener(new OpeningSalvoEffectMod(ship, id));
		}
		public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
			ship.removeListenerOfClass(OpeningSalvoEffectMod.class);
			ship.getMutableStats().getArmorDamageTakenMult().unmodify(id);
			ship.getMutableStats().getShieldDamageTakenMult().unmodify(id);
			ship.getMutableStats().getHullDamageTakenMult().unmodify(id);
			ship.getMutableStats().getMaxSpeed().unmodify(id);
		}

		public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {}
		public void unapply(MutableShipStatsAPI stats, HullSize hullSize, String id)  {}
		
		public void createCustomDescription(MutableCharacterStatsAPI stats, SkillSpecAPI skill, 
			TooltipMakerAPI info, float width) {
		
			init(stats, skill);
			info.addPara("Up to +%s top speed for the first %s of peak operating time",
				0f, hc, hc, (int)SPEED_BONUS + "%", (int)(FULL_EFFECT_TIME + DECAY_TIME) + " seconds"
			);
			info.addPara("Up to -%s damage taken for the first %s of peak operating time",
					0f, hc, hc, (int)DAMAGE_REDUCTION + "%", (int)(FULL_EFFECT_TIME + DECAY_TIME) + " seconds"
				);
			info.addPara(indent + "This remains at max effectiveness for the first %s seconds of peak operating time, decaying after %s seconds",
				0f, tc, hc, (int)FULL_EFFECT_TIME + "", (int)(FULL_EFFECT_TIME + DECAY_TIME) + ""
			);
		}

		public String getEffectDescription(float level) {
			return null;
		}
		
		public ScopeDescription getScopeDescription() {
			return ScopeDescription.PILOTED_SHIP;
		}
	}
	
	public static class Level2 implements ShipSkillEffect {

		public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {
			stats.getVentRateMult().modifyPercent(id, VENT_RATE_BONUS);
		}
		
		public void unapply(MutableShipStatsAPI stats, HullSize hullSize, String id) {
			stats.getVentRateMult().unmodify(id);
		}
		
		public String getEffectDescription(float level) {
			return "+" + (int)(VENT_RATE_BONUS) + "% flux dissipation rate while venting";
		}
		
		public String getEffectPerLevelDescription() {
			return null;
		}

		public ScopeDescription getScopeDescription() {
			return ScopeDescription.PILOTED_SHIP;
		}
	}
}
