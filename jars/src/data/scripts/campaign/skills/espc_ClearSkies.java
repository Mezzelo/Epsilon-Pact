package data.scripts.campaign.skills;

import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.characters.AfterShipCreationSkillEffect;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.ShipSkillEffect;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

public class espc_ClearSkies {
	
	public static float DAMAGE_BONUS = 100f;
	public static float MISSILE_PENALTY = 50f;
	public static float DAMAGE_RECEIVED_BONUS = 25f;
	
	public static class Level1 implements ShipSkillEffect {
		
		public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {
			stats.getDamageToFighters().modifyPercent(id, DAMAGE_BONUS);
		}
		
		public void unapply(MutableShipStatsAPI stats, HullSize hullSize, String id) {
			stats.getDamageToFighters().unmodify(id);
		}
		
		public String getEffectDescription(float level) {
			return "+" + (int)(DAMAGE_BONUS) + "% damage to fighters";
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
			stats.getDamageToMissiles().modifyPercent(id, DAMAGE_BONUS);
		}
		
		public void unapply(MutableShipStatsAPI stats, HullSize hullSize, String id) {
			stats.getDamageToMissiles().unmodify(id);
		}
		
		public String getEffectDescription(float level) {
			return "+" + (int)(DAMAGE_BONUS) + "% damage to missiles";
		}
		
		public String getEffectPerLevelDescription() {
			return null;
		}
		
		public ScopeDescription getScopeDescription() {
			return ScopeDescription.PILOTED_SHIP;
		}
	}
	
	public static class Level3 extends BaseSkillEffectDescription implements ShipSkillEffect {
		
		public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {
			stats.getMissileWeaponDamageMult().modifyPercent(id, -MISSILE_PENALTY);
		}
		
		public void unapply(MutableShipStatsAPI stats, HullSize hullSize, String id) {
			stats.getMissileWeaponDamageMult().unmodify(id);
		}

		public void createCustomDescription(MutableCharacterStatsAPI stats, SkillSpecAPI skill, 
			TooltipMakerAPI info, float width) {
		
			init(stats, skill);
			info.addPara("-%s missile damage dealt",
				0f, Misc.getNegativeHighlightColor(), Misc.getNegativeHighlightColor(), (int)(MISSILE_PENALTY) + "%"
			);
		}
		public String getEffectPerLevelDescription() {
			return null;
		}
		
		public ScopeDescription getScopeDescription() {
			return ScopeDescription.PILOTED_SHIP;
		}
	}
	
	public static class ClearSkiesEffectModElite implements DamageDealtModifier {
		protected ShipAPI ship;
		protected String id;
		public ClearSkiesEffectModElite(ShipAPI ship, String id) {
			this.ship = ship;
			this.id = id;
		}
		public String modifyDamageDealt(Object param,
			CombatEntityAPI target, DamageAPI damage,
			Vector2f point, boolean shieldHit) {
			
			if (ship == null) return null;
			
			if (!(param instanceof MissileAPI))
				return null;
			
			if (target instanceof MissileAPI) {
				damage.getModifier().modifyPercent(id, MISSILE_PENALTY);
				return id;
			} else if (target instanceof ShipAPI) {
				ShipAPI targ = (ShipAPI) target;
				if (targ.isFighter() || !shieldHit) {
					damage.getModifier().modifyPercent(id, MISSILE_PENALTY);
					return id;
				}
			}
			return null;
		}
	}
	
	public static class Level4 implements AfterShipCreationSkillEffect {
		public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
			ship.addListener(new ClearSkiesEffectModElite(ship, id));
		}
		public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
			ship.removeListenerOfClass(ClearSkiesEffectModElite.class);
		}
		
		public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {
		}
		
		public void unapply(MutableShipStatsAPI stats, HullSize hullSize, String id) {
		}
		
		public String getEffectDescription(float level) {
			return "Negates missile damage penalty against fighters and hull";
		}
		
		public String getEffectPerLevelDescription() {
			return null;
		}
		
		public ScopeDescription getScopeDescription() {
			return ScopeDescription.PILOTED_SHIP;
		}
	}
	
}
