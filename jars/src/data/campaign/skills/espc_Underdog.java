package data.campaign.skills;

import org.lwjgl.util.vector.Vector2f;

// import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.AfterShipCreationSkillEffect;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.ShipSkillEffect;
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

public class espc_Underdog {
	
	public static float DAMAGE_BONUS_PER_DP = 1f;
	public static float DAMAGE_REDUCTION_PER_DP = 1f;
	public static float DAMAGE_REDUCTION_SOFT_THRESHOLD = 20f;
	public static float DAMAGE_REDUCTION_SOFT_MULT = 0.5f;
	public static float DAMAGE_REDUCTION_CAP = 80f;
	public static float DAMAGE_REDUCTION_OTHER_CAP = 40f;
	public static float DAMAGE_REDUCTION_PER_DP_CARRIER = 0.5f;
	public static float DAMAGE_REDUCTION_CARRIER_CAP = 50f;
	public static float DAMAGE_REDUCTION_CARRIER_SOFT_THRESHOLD = 35f;
	public static float DAMAGE_REDUCTION_CARRIER_SOFT_MULT = 0.5f;
	
	
//	public static float FLAGSHIP_SPEED_BONUS = 25f;
//	public static float FLAGSHIP_CP_BONUS = 100f;
	
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
			
			if (!(target instanceof ShipAPI))
				return null;
			
			ShipAPI targ = (ShipAPI) target;
			if (targ.isFighter() || targ.getFleetMember() == null)
				return null;
			float diff = targ.getFleetMember().getUnmodifiedDeploymentPointsCost() - ship.getFleetMember().getUnmodifiedDeploymentPointsCost();
			if (diff <= 0f)
				return null;
			else if (diff > DAMAGE_REDUCTION_SOFT_THRESHOLD)
				diff = DAMAGE_REDUCTION_SOFT_THRESHOLD + (diff - DAMAGE_REDUCTION_SOFT_THRESHOLD) * DAMAGE_REDUCTION_SOFT_MULT;
			
			damage.getModifier().modifyPercent(id + "_given", diff * DAMAGE_BONUS_PER_DP);
			return id + "_given";
		}
		@Override
		public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point,
				boolean shieldHit) {
			if (ship == null) return null;
			
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
			if (source.isFighter() || source.getFleetMember() == null)
				return null;
			float diff = source.getFleetMember().getUnmodifiedDeploymentPointsCost() - ship.getFleetMember().getUnmodifiedDeploymentPointsCost();
			if (diff <= 0f)
				return null;
			else if (diff > DAMAGE_REDUCTION_SOFT_THRESHOLD)
				diff = DAMAGE_REDUCTION_SOFT_THRESHOLD + (diff - DAMAGE_REDUCTION_SOFT_THRESHOLD) * DAMAGE_REDUCTION_SOFT_MULT;
			
			damage.getModifier().modifyPercent(id + "_taken", Math.max(-diff * DAMAGE_REDUCTION_PER_DP, -DAMAGE_REDUCTION_CAP));
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
				float diff = source.getWing().getSourceShip().getFleetMember().getUnmodifiedDeploymentPointsCost() - 
					ship.getFleetMember().getUnmodifiedDeploymentPointsCost();
				if (diff > DAMAGE_REDUCTION_CARRIER_SOFT_THRESHOLD)
					diff = DAMAGE_REDUCTION_CARRIER_SOFT_THRESHOLD + (diff - DAMAGE_REDUCTION_CARRIER_SOFT_THRESHOLD) * DAMAGE_REDUCTION_CARRIER_SOFT_MULT;
				// Global.getLogger(espc_Underdog.class).info("dp diff: " + diff);
				damage.getModifier().modifyPercent(id + "_taken_fighter", Math.max(-diff * DAMAGE_REDUCTION_PER_DP_CARRIER, -DAMAGE_REDUCTION_CARRIER_CAP));
				return id + "_taken_fighter";
			}
			return null;
		}
	}
	
	public static class Level1 implements AfterShipCreationSkillEffect {
		public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
			ship.addListener(new UDDamageDealtMod(ship, id));
		}
		public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
			ship.removeListenerOfClass(UDDamageDealtMod.class);
		}
		public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {}
		public void unapply(MutableShipStatsAPI stats, HullSize hullSize, String id) {}
		
		public String getEffectDescription(float level) {
			return "+" + (int)DAMAGE_BONUS_PER_DP + "% damage dealt per DP deficit between ship and target";
		}
		
		public String getEffectPerLevelDescription() {
			return null;
		}
		
		public ScopeDescription getScopeDescription() {
			return ScopeDescription.PILOTED_SHIP;
		}
	}
	
	public static class Level2 extends BaseSkillEffectDescription implements ShipSkillEffect {
		public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {}
		public void unapply(MutableShipStatsAPI stats, HullSize hullSize, String id) {}
		
		public void createCustomDescription(MutableCharacterStatsAPI stats, SkillSpecAPI skill, 
			TooltipMakerAPI info, float width) {
		
			init(stats, skill);
			info.addPara("-%s damage taken per DP deficit between ship and attacker, up to %s reduction",
				0f, hc, hc, (int)DAMAGE_REDUCTION_PER_DP + "%", (int)DAMAGE_REDUCTION_CAP + "%"
			);
			info.addPara(indent + "Damage bonus and reduction past %s is half as effective",
				0f, tc, hc, (int)DAMAGE_REDUCTION_SOFT_THRESHOLD + "%"
			);
			info.addPara(indent + "Does not trigger while the ship has shield, armor or hull damage reduction over %s from another source",
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
	
	public static class Level3 implements AfterShipCreationSkillEffect {
		public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
			ship.addListener(new UDDamageTakenModFighters(ship, id));
		}
		public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
			ship.removeListenerOfClass(UDDamageTakenModFighters.class);
		}
		public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {}
		public void unapply(MutableShipStatsAPI stats, HullSize hullSize, String id) {}
		
		public String getEffectDescription(float level) {
			return "-" + DAMAGE_REDUCTION_PER_DP_CARRIER + "% damage taken from fighters per DP deficit between ship and carrier,"
				+ " up to " + (int)DAMAGE_REDUCTION_CARRIER_CAP + "% reduction";
		}
		
		public String getEffectPerLevelDescription() {
			return null;
		}
		
		public ScopeDescription getScopeDescription() {
			return ScopeDescription.PILOTED_SHIP;
		}
	}
	
	public static class Level4 extends BaseSkillEffectDescription implements ShipSkillEffect {
		public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {}
		public void unapply(MutableShipStatsAPI stats, HullSize hullSize, String id) {}
		
		public void createCustomDescription(MutableCharacterStatsAPI stats, SkillSpecAPI skill, 
			TooltipMakerAPI info, float width) {
		
			init(stats, skill);
			info.addPara(indent + "Damage reduction past %s is half as effective",
				0f, tc, hc, (int)DAMAGE_REDUCTION_CARRIER_SOFT_THRESHOLD + "%"
			);
			info.addPara(indent + "Does not trigger while the ship has shield, armor or hull damage reduction over %s from another source",
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
	

}
	
