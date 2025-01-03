package data.campaign.skills;


import java.util.Iterator;
import java.util.List;

import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
// import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.AfterShipCreationSkillEffect;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.ShipSkillEffect;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

public class espc_DancingSteps {

	public static float SPEED_BONUS = 50f;
	public static float MANEUVERABILITY_BONUS = 80f;
	public static float SPEED_BONUS_NO_SHIELD = 25f;
	public static float HULL_STATIC_MAX = 2000f;
	public static float HULL_PORTION_MAX = 50f;
	public static float HOSTILE_RANGE = 350f;
	public static float SHIELD_RAISE_BONUS = 75f;
	public static float ARMOR_DAMAGE_PENALTY = 20f;
	public static float FIGHTER_DAMAGE_REDUCTION = 15f;
	
	public static class DancingStepsEffectMod implements AdvanceableListener {
		protected ShipAPI ship;
		protected String id;
		// beams appear to update once every 6 ticks
		protected float beamDamageLast = 0f;
		protected int beamTicks = 0;
		public DancingStepsEffectMod(ShipAPI ship, String id) {
			this.ship = ship;
			this.id = id;
			this.beamDamageLast = 0f;
			this.beamTicks = 0;
		}
		
		public void advance(float amount) {
			if (Global.getCurrentState() != GameState.COMBAT)
				return;
			CombatEngineAPI combatEngine = Global.getCombatEngine();
			if (combatEngine == null)
				return;
			Iterator<Object> entityIterator = combatEngine.getAllObjectGrid().getCheckIterator(
				ship.getLocation(), 
				(HOSTILE_RANGE + ship.getShieldRadiusEvenIfNoShield()) * 2f,
				(HOSTILE_RANGE + ship.getShieldRadiusEvenIfNoShield()) * 2f
			);
			float damage = 0f;
			while (entityIterator.hasNext()) {
				Object entity = entityIterator.next();
				if (entity instanceof DamagingProjectileAPI) {
					DamagingProjectileAPI check = (DamagingProjectileAPI) entity;
					if (check.getOwner() != ship.getOwner())
						damage += check.getDamageAmount() * (check.getDamageType() == DamageType.FRAGMENTATION ? 0.5f : 1f);
				}
			}
			List<BeamAPI> beams = combatEngine.getBeams();
			for (BeamAPI beam : beams) {
				if (beam.getSource().getOwner() != ship.getOwner() && 
					MathUtils.getDistanceSquared(beam.getTo(), ship.getLocation()) < 
					Math.pow(ship.getShieldRadiusEvenIfNoShield() + HOSTILE_RANGE, 2f))
					damage += beam.getDamage().getDamage();
			}
			float damageFinal = damage;
			damage = beamDamageLast > damage ? beamDamageLast : damage;
			if (damage > 0f) {
				float mult = Math.min(damage / Math.min(HULL_STATIC_MAX, ship.getHitpoints() * HULL_PORTION_MAX/100f), 1f);
				MutableShipStatsAPI stats = ship.getMutableStats();
				stats.getMaxSpeed().modifyPercent(id, SPEED_BONUS * mult);
				if (ship.getShield() != null && ship.getShield().isOff())
					stats.getMaxSpeed().modifyFlat(id + "noShield", SPEED_BONUS_NO_SHIELD * mult);
				stats.getAcceleration().modifyPercent(id, MANEUVERABILITY_BONUS * mult);
				stats.getDeceleration().modifyPercent(id, MANEUVERABILITY_BONUS * mult);
				stats.getTurnAcceleration().modifyPercent(id, MANEUVERABILITY_BONUS * mult * 2f);
				stats.getMaxTurnRate().modifyPercent(id, MANEUVERABILITY_BONUS * mult);
			} else {
				ship.getMutableStats().getMaxSpeed().unmodify(id);
				ship.getMutableStats().getMaxSpeed().unmodify(id + "noShield");
				ship.getMutableStats().getAcceleration().unmodify(id);
				ship.getMutableStats().getDeceleration().unmodify(id);
				ship.getMutableStats().getTurnAcceleration().unmodify(id);
				ship.getMutableStats().getMaxTurnRate().unmodify(id);
			}
			beamDamageLast = damageFinal;

		}

	}

	public static class DancingStepsEffectModElite implements DamageTakenModifier {
		protected ShipAPI ship;
		protected String id;
		public DancingStepsEffectModElite(ShipAPI ship, String id) {
			this.ship = ship;
			this.id = id;
		}
		@Override
		public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point,
			boolean shieldHit) {
			if (ship == null || shieldHit) return null;
			
			ShipAPI source = null;
			if (param instanceof DamagingProjectileAPI) {
				if (param instanceof MissileAPI)
					return null;
				source = ((DamagingProjectileAPI) param).getSource();
			}
			else if (param instanceof BeamAPI)
				source = ((BeamAPI) param).getSource();
			if (source == null)
				return null;
			if (!source.isFighter())
				return null;
			
			damage.getModifier().modifyPercent(id, -FIGHTER_DAMAGE_REDUCTION);
			return id;
		}
	}
	
	public static class Level1 extends BaseSkillEffectDescription implements AfterShipCreationSkillEffect {
		public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
			ship.addListener(new DancingStepsEffectMod(ship, id));
		}
		public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
			ship.removeListenerOfClass(DancingStepsEffectMod.class);
			ship.getMutableStats().getMaxSpeed().unmodify(id);
			ship.getMutableStats().getMaxSpeed().unmodify(id + "noShield");
			ship.getMutableStats().getAcceleration().unmodify(id);
			ship.getMutableStats().getDeceleration().unmodify(id);
			ship.getMutableStats().getTurnAcceleration().unmodify(id);
			ship.getMutableStats().getMaxTurnRate().unmodify(id);
		}

		public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {
			// stats.getArmorDamageTakenMult().modifyPercent(id, ARMOR_DAMAGE_PENALTY);
			stats.getShieldUnfoldRateMult().modifyPercent(id, SHIELD_RAISE_BONUS);
		}
		public void unapply(MutableShipStatsAPI stats, HullSize hullSize, String id)  {
			// stats.getArmorDamageTakenMult().unmodify(id);
			stats.getShieldUnfoldRateMult().unmodify(id);
		}
		
		public void createCustomDescription(MutableCharacterStatsAPI stats, SkillSpecAPI skill, 
			TooltipMakerAPI info, float width) {
		
			init(stats, skill);
			info.addPara("Up to +%s top speed and +%s maneuverability, based on nearby incoming fire",
				0f, hc, hc, (int)SPEED_BONUS + "%", (int)MANEUVERABILITY_BONUS + "%"
			);
			info.addPara("Up to %s su/second to top speed when shields are down, on ships with shields",
				0f, hc, hc, "+" + (int)SPEED_BONUS_NO_SHIELD
			);
			info.addPara(indent + "Max effect when there is half of current hull's worth of enemy fire within %s",
				0f, tc, hc, (int)HOSTILE_RANGE + " su"
			);
			/*
			info.addPara("+%s armor damage taken",
				0f, Misc.getNegativeHighlightColor(), Misc.getNegativeHighlightColor(), (int)ARMOR_DAMAGE_PENALTY + "%"
			);*/
		}

		public String getEffectDescription(float level) {
			return null;
		}
		
		public ScopeDescription getScopeDescription() {
			return ScopeDescription.PILOTED_SHIP;
		}
	}
	
	public static class Level2 implements AfterShipCreationSkillEffect {
		public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
			ship.addListener(new DancingStepsEffectModElite(ship, id));
		}
		public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
			ship.removeListenerOfClass(DancingStepsEffectModElite.class);
		}

		public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {}
		public void unapply(MutableShipStatsAPI stats, HullSize hullSize, String id) {}
		
		public String getEffectDescription(float level) {
			return "-" + (int)FIGHTER_DAMAGE_REDUCTION + "% armor and hull damage taken from fighters";
		}
		
		public String getEffectPerLevelDescription() {
			return null;
		}

		public ScopeDescription getScopeDescription() {
			return ScopeDescription.PILOTED_SHIP;
		}
	}
	public static class Level3 extends BaseSkillEffectDescription implements ShipSkillEffect {

		public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {}
		public void unapply(MutableShipStatsAPI stats, HullSize hullSize, String id) {}
		
		public void createCustomDescription(MutableCharacterStatsAPI stats, SkillSpecAPI skill, 
			TooltipMakerAPI info, float width) {
		
			init(stats, skill);
			info.addPara(indent + "Does not apply to missiles",
				0f, tc, hc
			);
		}
		
		public String getEffectDescription(float level) {
			return null;
		}
		
		public String getEffectPerLevelDescription() {
			return null;
		}

		public ScopeDescription getScopeDescription() {
			return ScopeDescription.PILOTED_SHIP;
		}
	}
}
