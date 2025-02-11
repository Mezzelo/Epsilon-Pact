package data.campaign.skills;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
// import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.AfterShipCreationSkillEffect;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

public class espc_SecondWind {
	
	public static float SYSTEM_COOLDOWN = 20f;
	public static float OVERLOAD_COOLDOWN = 90f;
	public static float DISSIPATION_BONUS_OVERLOAD = 3f;
	public static float OVERLOAD_DURATION = 0.25f;
	
	public static class SecondWindEffectMod implements AdvanceableListener {
		protected ShipAPI ship;
		protected String id;
		protected float cooldown = 0f;
		protected boolean systemOn = false;
		public SecondWindEffectMod(ShipAPI ship, String id) {
			this.ship = ship;
			this.id = id;
		}
		
		public void advance(float amount) {
			if (Global.getCurrentState() != GameState.COMBAT)
				return;
			if (ship == null || ship.getSystem() == null)
				return;
			if (ship.getSystem().isActive() && !systemOn && cooldown <= 0f &&
				(ship.getSystem().getMaxAmmo() > 10000 ||
				ship.getSystem().getMaxAmmo() > 0 && ship.getSystem().getAmmo() <= 0)) {
				systemOn = true;
			} else if (systemOn && !ship.getSystem().isActive() && !ship.getSystem().isChargedown() &&
					cooldown <= 0f) {
				if (ship.getSystem().getCooldownRemaining() > 0f)
					ship.getSystem().setCooldownRemaining(Math.max(0f, 
						ship.getSystem().getCooldownRemaining() - ship.getSystem().getCooldown() * amount * 5f));
				
				if (ship.getSystem().getCooldownRemaining() <= 0f) {
					systemOn = false;
					cooldown = SYSTEM_COOLDOWN;
					if (ship.getSystem().getMaxAmmo() < 10000)
						ship.getSystem().setAmmo(Math.min(ship.getSystem().getMaxAmmo(), 
							ship.getSystem().getAmmo() + ship.getSystem().getMaxAmmo()/2));
				}
			} else if (cooldown > 0f)
				cooldown = Math.max(0f, cooldown - amount);
		}

	}

	public static class SecondWindEffectModElite implements AdvanceableListener {
		protected ShipAPI ship;
		protected String id;
		protected float cooldown = 0f;
		protected boolean wasOverloaded = false;
		public SecondWindEffectModElite(ShipAPI ship, String id) {
			this.ship = ship;
			this.id = id;
		}
		
		public void advance(float amount) {
			if (Global.getCurrentState() != GameState.COMBAT)
				return;
			if (ship.getFluxTracker().isOverloaded() && cooldown <= 0f &&
				ship.getFluxTracker().getOverloadTimeRemaining() > 1.5f) {
				wasOverloaded = true;
				cooldown = OVERLOAD_COOLDOWN;
	 			ship.getFluxTracker().setOverloadDuration(ship.getFluxTracker().getOverloadTimeRemaining() / 2f);
	 			ship.getMutableStats().getFluxDissipation().modifyMult(id, DISSIPATION_BONUS_OVERLOAD);
			} else if (wasOverloaded && !ship.getFluxTracker().isOverloaded()) {
	 			ship.getMutableStats().getFluxDissipation().unmodify(id);
	 			wasOverloaded = false;
			} else if (cooldown > 0f) {
				cooldown = Math.max(0f, cooldown - amount);
			}
		}

	}
	
	public static class Level1 extends BaseSkillEffectDescription implements AfterShipCreationSkillEffect {
		public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
			ship.addListener(new SecondWindEffectMod(ship, id));
		}
		public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
			ship.removeListenerOfClass(SecondWindEffectMod.class);
		}

		public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {}
		public void unapply(MutableShipStatsAPI stats, HullSize hullSize, String id)  {}
		
		public String getEffectDescription(float level) {
			return null;
		}
		
		// return "";
		
		public void createCustomDescription(MutableCharacterStatsAPI stats, SkillSpecAPI skill, 
											TooltipMakerAPI info, float width) {

			init(stats, skill);
			info.addPara("Once per " + (int)Math.round(SYSTEM_COOLDOWN) + " seconds, instantly refreshes ship system cooldown" + " after it elapses.",
				0f, hc, hc
			);
			info.addPara(indent + "If the ship's system has charges, activates when depleted, restoring half the system's maximum charges",
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
	
	
	public static class Level2 implements AfterShipCreationSkillEffect {
		public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
			ship.addListener(new SecondWindEffectModElite(ship, id));
		}
		public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
			ship.removeListenerOfClass(SecondWindEffectModElite.class);
			ship.getMutableStats().getFluxDissipation().unmodify(id);
		}
		
		public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {}
		public void unapply(MutableShipStatsAPI stats, HullSize hullSize, String id) {}
		
		public String getEffectDescription(float level) {
			return "Once per " + (int)Math.round(OVERLOAD_COOLDOWN) + " seconds on overload, "
				+ "quarters overload duration and triples flux dissipation while overloaded";
		}
		public String getEffectPerLevelDescription() {
			return null;
		}
		public ScopeDescription getScopeDescription() {
			return ScopeDescription.PILOTED_SHIP;
		}
	}
}
