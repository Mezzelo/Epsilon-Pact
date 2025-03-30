package data.scripts.campaign.skills;


import java.util.Iterator;
import java.util.List;

import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
// import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.AfterShipCreationSkillEffect;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
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

public class espc_DancingSteps {

	public static float SPEED_BONUS = 50f;
	public static float MANEUVERABILITY_BONUS = 50f;
	public static float SPEED_BONUS_NO_SHIELD = 30f;
	public static float SHIELD_DECAY_TIME = 3f;
	public static float SHIELD_TIME_GAIN_MULT = 2f;
	public static float HULL_STATIC_MAX = 2000f;
	public static float HULL_PORTION_MAX = 50f;
	public static float HOSTILE_RANGE = 350f;
	public static float SHIELD_RAISE_BONUS = 75f;
	public static float ARMOR_DAMAGE_PENALTY = 20f;
	public static float FIGHTER_DAMAGE_REDUCTION = 25f;
	
	public static float DAMAGE_REDUCTION_SHIELD_MAX = 80f;
	public static float DAMAGE_REDUCTION_SPEED_MAX = 70f;
	
	public static int BEAM_UPDATE_RATE = 8;

	public static Object SHIELD_DROP_STATUS_KEY = new Object();
	
	public static class DancingStepsEffectMod implements AdvanceableListener {
		protected ShipAPI ship;
		protected String id;
		protected float beamDamageLast = 0f;
		protected int beamTicks = 0;
		protected float shieldTimer = 0f;
		public DancingStepsEffectMod(ShipAPI ship, String id) {
			this.ship = ship;
			this.id = id;
			this.beamDamageLast = 0f;
			this.beamTicks = 0;
			this.shieldTimer = 0f;
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
			float beamDamage = 0f;
			List<BeamAPI> beams = combatEngine.getBeams();
			for (BeamAPI beam : beams) {
				if (beam.getSource().getOwner() != ship.getOwner() && 
					MathUtils.getDistanceSquared(beam.getTo(), ship.getLocation()) < 
					Math.pow(ship.getShieldRadiusEvenIfNoShield() + HOSTILE_RANGE, 2f))
					beamDamage += beam.getDamage().getDamage();
			}
			if (beamDamage > 0f) {
				beamTicks = BEAM_UPDATE_RATE;
				beamDamageLast = beamDamage;
			}
			else if (beamTicks > 0){
				beamTicks--;
				if (beamTicks == 0)
					beamDamageLast = 0f;
				else
					beamDamage = beamDamageLast;
				
			}
			damage += beamDamage;
			if (ship.getShield() != null && ship.getShield().isOff() && ship.getMaxSpeed() > 0f) {
				if (shieldTimer > 0f) {
					shieldTimer = Math.max(shieldTimer - amount, 0f);
					ship.getMutableStats().getMaxSpeed().modifyFlat(id + "noShield", SPEED_BONUS_NO_SHIELD * shieldTimer/SHIELD_DECAY_TIME);
					if (Global.getCombatEngine() != null && Global.getCombatEngine().getPlayerShip() == ship) {
						Global.getCombatEngine().maintainStatusForPlayerShip(SHIELD_DROP_STATUS_KEY,
							Global.getSettings().getSpriteName("ui", "icon_tactical_engine_boost"),
							"Dancing steps", 
							"+" + (int)(SPEED_BONUS_NO_SHIELD * shieldTimer/SHIELD_DECAY_TIME) + " top speed", false);
					}
				} else {
					ship.getMutableStats().getMaxSpeed().unmodify(id + "noShield");
				}
			}
			else {
				ship.getMutableStats().getMaxSpeed().unmodify(id + "noShield");
				if (ship.getShield() != null && ship.getShield().isOn() && ship.getMaxSpeed() > 0f)
					shieldTimer = Math.min(shieldTimer + amount * SHIELD_TIME_GAIN_MULT, SHIELD_DECAY_TIME);
			}
			if (damage > 0f && !ship.isPhased()) {
				float mult = Math.min(damage / Math.min(HULL_STATIC_MAX, ship.getHitpoints() * HULL_PORTION_MAX/100f), 1f);
				MutableShipStatsAPI stats = ship.getMutableStats();
				stats.getMaxSpeed().modifyPercent(id, SPEED_BONUS * mult);
				stats.getAcceleration().modifyPercent(id, MANEUVERABILITY_BONUS * mult);
				stats.getDeceleration().modifyPercent(id, MANEUVERABILITY_BONUS * mult);
				stats.getTurnAcceleration().modifyPercent(id, MANEUVERABILITY_BONUS * mult * 2f);
				stats.getMaxTurnRate().modifyPercent(id, MANEUVERABILITY_BONUS * mult);
			} else {
				MutableShipStatsAPI stats = ship.getMutableStats();
				stats.getMaxSpeed().unmodify(id);
				stats.getAcceleration().unmodify(id);
				stats.getDeceleration().unmodify(id);
				stats.getTurnAcceleration().unmodify(id);
				stats.getMaxTurnRate().unmodify(id);
			}
		}

	}
	
	public static class DancingStepsEffectModShield implements DamageTakenModifier {
		protected ShipAPI ship;
		protected String id;
		public DancingStepsEffectModShield(ShipAPI ship, String id) {
			this.ship = ship;
			this.id = id;
		}
		@Override
		public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point,
				boolean shieldHit) {
			if (!shieldHit || ship == null) return null;
			
			float angle = 0f;
			if (param instanceof DamagingProjectileAPI)
				angle = (float) Math.toDegrees(FastTrig.atan2(
					((DamagingProjectileAPI) param).getVelocity().y, ((DamagingProjectileAPI) param).getVelocity().x
					));
			else if (param instanceof BeamAPI)
				angle = VectorUtils.getAngle(((BeamAPI) param).getFrom(), point);
			else
				return null;
			if (Float.isNaN(angle))
				return null;
			
			float angleShip = Math.abs(MathUtils.getShortestRotation((float) Math.toDegrees(FastTrig.atan2(
				ship.getVelocity().y, ship.getVelocity().x
			)), angle));
			
			angle = Math.abs(MathUtils.getShortestRotation(VectorUtils.getAngle(point, ship.getShieldCenterEvenIfNoShield()), angle));
			
			// something's thrown NaN here very rarely, either an error in the math or a return on one of the many calls here
			// don't have time to properly debug before release so it's a gross fix for now lol
			angle = (90f - Math.abs(angle - 90f))/90f
					* Math.min(DAMAGE_REDUCTION_SPEED_MAX, ship.getVelocity().length()) / DAMAGE_REDUCTION_SPEED_MAX *
					(90f - Math.abs(angleShip - 90f))/90f
					* -DAMAGE_REDUCTION_SHIELD_MAX;
			if (Float.isNaN(angle) || angle <= -95f)
				return null;
			damage.getModifier().modifyPercent(id + "_taken_shield", angle);
			/*
			damage.getModifier().modifyPercent(id + "_taken_shield", (90f - Math.abs(angle - 90f))/90f
				* Math.min(DAMAGE_REDUCTION_SPEED_MAX, ship.getVelocity().length()) / DAMAGE_REDUCTION_SPEED_MAX *
				(90f - Math.abs(angleShip - 90f))/90f
				* -DAMAGE_REDUCTION_SHIELD_MAX); */
			return id + "_taken_shield";
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
			
			damage.getModifier().modifyPercent(id, -FIGHTER_DAMAGE_REDUCTION *
				Math.min(1f, ship.getVelocity().length() / DAMAGE_REDUCTION_SPEED_MAX));
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
			info.addPara("Up to +%s top speed and maneuverability, based on nearby incoming fire",
				0f, hc, hc, (int)SPEED_BONUS + "%", (int)MANEUVERABILITY_BONUS + "%"
			);
			info.addPara(indent + "Max effect when there is half of current hull's worth of enemy fire within %s. No effect while phased.",
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
	
	public static class Level2 extends BaseSkillEffectDescription implements AfterShipCreationSkillEffect {
		public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
			ship.addListener(new DancingStepsEffectModShield(ship, id));
		}
		public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
			ship.removeListenerOfClass(DancingStepsEffectModShield.class);
		}

		public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {}
		public void unapply(MutableShipStatsAPI stats, HullSize hullSize, String id) {}

		public void createCustomDescription(MutableCharacterStatsAPI stats, SkillSpecAPI skill, 
			TooltipMakerAPI info, float width) {
			init(stats, skill);
			info.addPara("Up to -%s damage taken by shields, based on current speed and impact angle against shield",
					0f, hc, hc,
					(int) DAMAGE_REDUCTION_SHIELD_MAX + "%"
				);
			info.addPara(indent + "Max damage reduction when moving perpendicular to an attack at %s, and it lands parallel to shield",
				0f, tc, hc, (int) DAMAGE_REDUCTION_SPEED_MAX + " su/second"
			);
			info.addPara("%s su/second to top speed after dropping shields, decaying over %s",
				0f, hc, hc, "+" + (int)SPEED_BONUS_NO_SHIELD, (int)SHIELD_DECAY_TIME + " seconds"
			);
		}
		
		public String getEffectPerLevelDescription() {
			return null;
		}

		public ScopeDescription getScopeDescription() {
			return ScopeDescription.PILOTED_SHIP;
		}
	}
	
	public static class Level3 extends BaseSkillEffectDescription implements AfterShipCreationSkillEffect {
		public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
			ship.addListener(new DancingStepsEffectModElite(ship, id));
		}
		public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
			ship.removeListenerOfClass(DancingStepsEffectModElite.class);
		}

		public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {}
		public void unapply(MutableShipStatsAPI stats, HullSize hullSize, String id) {}

		public void createCustomDescription(MutableCharacterStatsAPI stats, SkillSpecAPI skill, 
			TooltipMakerAPI info, float width) {
			init(stats, skill);
			info.addPara("Up to -%s armor and hull damage taken from fighters",
					0f, stats.getSkillLevel(skill.getId()) > 1 ? hc : dhc, stats.getSkillLevel(skill.getId()) > 1? hc : dhc,
					(int) FIGHTER_DAMAGE_REDUCTION + "%"
				);
			info.addPara(indent + "Max effect when moving at %s. Does not apply to missiles",
				0f, stats.getSkillLevel(skill.getId()) > 1 ? tc : dtc, stats.getSkillLevel(skill.getId()) > 1? hc : dhc,
					(int) DAMAGE_REDUCTION_SPEED_MAX + " su/second"
			);
		}
		
		public String getEffectPerLevelDescription() {
			return null;
		}

		public ScopeDescription getScopeDescription() {
			return ScopeDescription.PILOTED_SHIP;
		}
	}
}
