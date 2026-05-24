package data.scripts.campaign.skills;

import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

// import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.AfterShipCreationSkillEffect;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import data.scripts.util.MezzUtils;

public class espc_Underdog {
	
	public static float DAMAGE_BONUS_PER_DP = 1f;
	public static float DAMAGE_REDUCTION_PER_DP = 1f;
	public static float DAMAGE_REDUCTION_SOFT_THRESHOLD = 30f;
	public static float DAMAGE_REDUCTION_SOFT_MULT = 0.5f;
	public static float DAMAGE_REDUCTION_CAP = 80f;
	public static float DAMAGE_REDUCTION_DEG_MAX = 30f;
	public static float DAMAGE_REDUCTION_DEG_MIN = 90f;
	public static float DAMAGE_REDUCTION_OTHER_CAP = 40f;
	public static float DAMAGE_REDUCTION_PER_DP_CARRIER = 0.5f;
	public static float DAMAGE_REDUCTION_CARRIER_CAP = 50f;
	public static float DAMAGE_REDUCTION_CARRIER_SOFT_THRESHOLD = 35f;
	public static float DAMAGE_REDUCTION_CARRIER_SOFT_MULT = 0.5f;
	
	public static boolean isFrigateAndOfficer(MutableShipStatsAPI stats) {
		if (stats.getEntity() instanceof ShipAPI) {
			ShipAPI ship = (ShipAPI) stats.getEntity();
			if (!ship.isFrigate()) return false;
			return !ship.getCaptain().isDefault();
		} else {
			FleetMemberAPI member = stats.getFleetMember();
			if (member == null) return false;
			if (!member.isFrigate()) return false;
			return !member.getCaptain().isDefault();
		}
	}
	
	public static class UDDamageDealtMod implements DamageDealtModifier, DamageTakenModifier {
		protected ShipAPI ship;
		protected String id;
		public UDDamageDealtMod(ShipAPI ship, String id) {
			this.ship = ship;
			this.id = id;
		}
		public String modifyDamageDealt(Object param,
			CombatEntityAPI target, DamageAPI damage,
			Vector2f point, boolean shieldHit) {
			
			if (ship == null) return null;
			if (ship.getParentStation() != null)
				ship = ship.getParentStation();
			
			if (ship.getFleetMember() == null)
				return null;
			
			if (!(target instanceof ShipAPI))
				return null;
			
			ShipAPI targ = (ShipAPI) target;
			if (targ.getParentStation() != null)
				targ = targ.getParentStation();
			if (targ.isFighter() || targ.getFleetMember() == null)
				return null;
			float diff = 
				Math.max(targ.getFleetMember().getUnmodifiedDeploymentPointsCost(), targ.getFleetMember().getDeploymentPointsCost())
				- Math.max(ship.getFleetMember().getUnmodifiedDeploymentPointsCost(), ship.getFleetMember().getDeploymentPointsCost());
			if (diff <= 0f)
				return null;
			diff *= DAMAGE_BONUS_PER_DP;
			if (diff > DAMAGE_REDUCTION_SOFT_THRESHOLD)
				diff = DAMAGE_REDUCTION_SOFT_THRESHOLD + (diff - DAMAGE_REDUCTION_SOFT_THRESHOLD) * DAMAGE_REDUCTION_SOFT_MULT;
			
			float angleMod = Math.abs(MathUtils.getShortestRotation(VectorUtils.getAngle(targ.getLocation(), ship.getLocation()),
        		targ.getFacing()));
			if (angleMod >= DAMAGE_REDUCTION_DEG_MIN)
				return null;
			else if (angleMod > DAMAGE_REDUCTION_DEG_MAX)
				diff *= (1f - (angleMod - DAMAGE_REDUCTION_DEG_MAX)/(DAMAGE_REDUCTION_DEG_MIN - DAMAGE_REDUCTION_DEG_MAX));
			
			damage.getModifier().modifyPercent(id + "_given", diff);
			return id + "_given";
		}
		@Override
		public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point,
				boolean shieldHit) {
			if (ship == null) return null;
			if (ship.getParentStation() != null)
				ship = ship.getParentStation();
			
			if (ship.getFleetMember() == null)
				return null;
			
			if (ship.getMutableStats().getShieldDamageTakenMult().getModifiedValue() * 100f < 100f - DAMAGE_REDUCTION_OTHER_CAP ||
				ship.getMutableStats().getArmorDamageTakenMult().getModifiedValue() * 100f < 100f - DAMAGE_REDUCTION_OTHER_CAP ||
				ship.getMutableStats().getHullDamageTakenMult().getModifiedValue() * 100f < 100f - DAMAGE_REDUCTION_OTHER_CAP)
				return null;
			
			ShipAPI source = null;
			
			if (param instanceof DamagingProjectileAPI)
				source = ((DamagingProjectileAPI) param).getSource();
			else if (param instanceof BeamAPI)
				source = ((BeamAPI) param).getSource();
			
			if (source == null)
				return null;
			if (source.getParentStation() != null)
				source = source.getParentStation();
			if (source.isFighter() || source.getFleetMember() == null)
				return null;
			float diff = 
				Math.max(source.getFleetMember().getUnmodifiedDeploymentPointsCost(), source.getFleetMember().getDeploymentPointsCost())
				- Math.max(ship.getFleetMember().getUnmodifiedDeploymentPointsCost(), ship.getFleetMember().getDeploymentPointsCost());
			if (diff <= 0f)
				return null;
			diff *= DAMAGE_REDUCTION_PER_DP;
			if (diff > DAMAGE_REDUCTION_SOFT_THRESHOLD)
				diff = DAMAGE_REDUCTION_SOFT_THRESHOLD + (diff - DAMAGE_REDUCTION_SOFT_THRESHOLD) * DAMAGE_REDUCTION_SOFT_MULT;
			
			float angleMod = Math.abs(MathUtils.getShortestRotation(VectorUtils.getAngle(source.getLocation(), ship.getLocation()),
				source.getFacing()));
			if (angleMod >= DAMAGE_REDUCTION_DEG_MIN)
				return null;
			else if (angleMod > DAMAGE_REDUCTION_DEG_MAX)
				diff *= (1f - (angleMod - DAMAGE_REDUCTION_DEG_MAX)/(DAMAGE_REDUCTION_DEG_MIN - DAMAGE_REDUCTION_DEG_MAX));
			
			damage.getModifier().modifyPercent(id + "_taken", Math.max(-diff, -DAMAGE_REDUCTION_CAP));
			return id + "_taken";
		}
	}
	
	public static class UDDamageTakenModFighters implements DamageTakenModifier {
		protected ShipAPI ship;
		protected String id;
		public UDDamageTakenModFighters(ShipAPI ship, String id) {
			this.ship = ship;
			this.id = id;
		}
		@Override
		public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point,
				boolean shieldHit) {
			if (ship == null) return null;
			
			ShipAPI source = null;
			if (ship.getMutableStats().getShieldDamageTakenMult().getModifiedValue() * 100f < 100f - DAMAGE_REDUCTION_OTHER_CAP ||
				ship.getMutableStats().getArmorDamageTakenMult().getModifiedValue() * 100f < 100f - DAMAGE_REDUCTION_OTHER_CAP ||
				ship.getMutableStats().getHullDamageTakenMult().getModifiedValue() * 100f < 100f - DAMAGE_REDUCTION_OTHER_CAP)
				return null;
			
			if (param instanceof DamagingProjectileAPI)
				source = ((DamagingProjectileAPI) param).getSource();
			else if (param instanceof BeamAPI)
				source = ((BeamAPI) param).getSource();
			
			if (source == null)
				return null;
			if (!source.isFighter())
				return null;
			
			if (source.isFighter() && source.getWing() != null && source.getWing().getLeader() != null && source.getWing().getSourceShip().getFleetMember() != null) {
				float diff =
					Math.max(source.getWing().getSourceShip().getFleetMember().getUnmodifiedDeploymentPointsCost(), 
						source.getWing().getSourceShip().getFleetMember().getDeploymentPointsCost())
					- Math.max(ship.getFleetMember().getUnmodifiedDeploymentPointsCost(), ship.getFleetMember().getDeploymentPointsCost());
				if (diff <= 0f)
					return null;
				diff *= DAMAGE_REDUCTION_PER_DP_CARRIER;
				if (diff > DAMAGE_REDUCTION_CARRIER_SOFT_THRESHOLD)
					diff = DAMAGE_REDUCTION_CARRIER_SOFT_THRESHOLD + (diff - DAMAGE_REDUCTION_CARRIER_SOFT_THRESHOLD) * DAMAGE_REDUCTION_CARRIER_SOFT_MULT;
				// Global.getLogger(espc_Underdog.class).info("dp diff: " + diff);
				damage.getModifier().modifyPercent(id + "_taken_fighter", Math.max(-diff, -DAMAGE_REDUCTION_CARRIER_CAP));
				return id + "_taken_fighter";
			}
			return null;
		}
	}
	
	public static class Level1 extends BaseSkillEffectDescription implements AfterShipCreationSkillEffect {
		public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
			ship.addListener(new UDDamageDealtMod(ship, id));
		}
		public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
			ship.removeListenerOfClass(UDDamageDealtMod.class);
		}
		public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {}
		public void unapply(MutableShipStatsAPI stats, HullSize hullSize, String id) {}
		
		public void createCustomDescription(MutableCharacterStatsAPI stats, SkillSpecAPI skill, 
			TooltipMakerAPI info, float width) {
		
			init(stats, skill);
			info.addPara(MezzUtils.getString("espc_skills", "underdog1-1"),
				0f, hc, hc, (int)DAMAGE_BONUS_PER_DP + "%"
			);
			info.addPara(MezzUtils.getString("espc_skills", "underdog1-2"),
				0f, hc, hc, (int)DAMAGE_REDUCTION_PER_DP + "%", (int)DAMAGE_REDUCTION_CAP + "%"
			);
			info.addPara(indent + MezzUtils.getString("espc_skills", "underdog1-3"),
				0f, tc, hc
			);
			info.addPara(indent + MezzUtils.getString("espc_skills", "underdog1-4"),
				0f, tc, hc, (int)DAMAGE_REDUCTION_SOFT_THRESHOLD + "%"
			);
			info.addPara(indent + MezzUtils.getString("espc_skills", "underdog1-5"),
				0f, tc, hc, "" + (int) DAMAGE_REDUCTION_DEG_MAX, "" + (int) DAMAGE_REDUCTION_DEG_MIN
			);
			info.addPara(indent + MezzUtils.getString("espc_skills", "underdog1-6"),
				0f, tc, hc, (int)DAMAGE_REDUCTION_OTHER_CAP + "%"
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
	
	public static class Level2 extends BaseSkillEffectDescription implements AfterShipCreationSkillEffect {
		public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
			ship.addListener(new UDDamageTakenModFighters(ship, id));
		}
		public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
			ship.removeListenerOfClass(UDDamageTakenModFighters.class);
		}
		public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {}
		public void unapply(MutableShipStatsAPI stats, HullSize hullSize, String id) {}
		
		public void createCustomDescription(MutableCharacterStatsAPI stats, SkillSpecAPI skill, 
			TooltipMakerAPI info, float width) {
		
			init(stats, skill);
			info.addPara(MezzUtils.getString("espc_skills", "underdog2-1"),
				0f, stats.getSkillLevel(skill.getId()) > 1 ? hc : dhc, stats.getSkillLevel(skill.getId()) > 1? hc : dhc,
				DAMAGE_REDUCTION_PER_DP_CARRIER + "%",
				(int)DAMAGE_REDUCTION_CARRIER_CAP + "%"
			);
			info.addPara(indent + MezzUtils.getString("espc_skills", "underdog2-2"),
				0f, stats.getSkillLevel(skill.getId()) > 1 ? tc : dtc, stats.getSkillLevel(skill.getId()) > 1? hc : dhc
			);
			info.addPara(indent + MezzUtils.getString("espc_skills", "underdog2-3"),
				0f, stats.getSkillLevel(skill.getId()) > 1 ? tc : dtc, stats.getSkillLevel(skill.getId()) > 1? hc : dhc,
				(int)DAMAGE_REDUCTION_CARRIER_SOFT_THRESHOLD + "%"
			);
			info.addPara(indent + MezzUtils.getString("espc_skills", "underdog2-4"),
				0f, stats.getSkillLevel(skill.getId()) > 1 ? tc : dtc, stats.getSkillLevel(skill.getId()) > 1? hc : dhc,
				(int)DAMAGE_REDUCTION_OTHER_CAP + "%"
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
	
