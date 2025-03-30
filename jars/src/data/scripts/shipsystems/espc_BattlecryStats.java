package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.EmpArcEntityAPI.EmpArcParams;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.IntervalUtil;

import java.util.Iterator;
import java.awt.Color;
import com.fs.starfarer.api.util.Misc;

import org.lazywizard.lazylib.MathUtils;

public class espc_BattlecryStats extends BaseShipSystemScript {
	
	private static final float FLUX_PROJECT_MULT = 1.0f;
	private static final float FLUX_RANGE_BASE = 1000f;
	private static final float FLUX_RANGE_MAX_PENALTY = 0f;
	
	// public static final float FLUX_COST_MULT = 1f;
	private static final float FLUX_DISSIPATION_MULT = 2f;
	
	private static final float ARC_FIGHTER_CHANCE = 0.4f;
	
	private float fluxLast = -100f;
	private float hardFluxLast = 0f;
	private float arcFlux = 0f;
	
	private float jitterAmount = 0f;
	
	private IntervalUtil fluxPulseInterval = new IntervalUtil(0.2f, 0.4f);
	
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		
		// stats.getBallisticRoFMult().modifyMult(id, mult);
		// stats.getBallisticWeaponFluxCostMod().modifyMult(id, 1f - (FLUX_REDUCTION * 0.01f));

		if (stats.getEntity() == null)
			return;

		CombatEngineAPI combatEngine = Global.getCombatEngine();
		float amount = combatEngine.getElapsedInLastFrame();
		if (amount == 0f)
			return;

		stats.getFluxDissipation().modifyMult(id, FLUX_DISSIPATION_MULT * effectLevel);
		
		ShipAPI ship = (ShipAPI) stats.getEntity();
		
		jitterAmount = Math.max(0f, jitterAmount - amount * 1.5f);
		if (jitterAmount > 0f)
			ship.setJitterUnder(
				this,
				new Color(200, 40, 210, 170),
				effectLevel * jitterAmount,
				2,
				effectLevel * 8f
			);
		
		// debounce to ensure flux values are from previous tick on ability activate
		if (fluxLast > -50f) {
			float hardFluxChange = ship.getFluxTracker().getHardFlux() - hardFluxLast;
			float fluxChange = ship.getCurrFlux() - fluxLast - hardFluxChange;
			int changeFriendly = (hardFluxChange + fluxChange > 0.0f) ? 1 - ship.getOwner() : ship.getOwner();
			arcFlux += (fluxChange + hardFluxChange);
			
			fluxPulseInterval.advance(combatEngine.getElapsedInLastFrame());
			
			Iterator<Object> shipGridIterator = (Iterator<Object>) (combatEngine.getAiGridShips().getCheckIterator(
				ship.getLocation(),
				// FLUX_RANGE_BASE - FLUX_RANGE_MAX_PENALTY * ship.getFluxLevel()
				FLUX_RANGE_BASE * 2f,
				FLUX_RANGE_BASE * 2f));
			
			boolean didArc = false;
			
			while (shipGridIterator.hasNext()) {
				ShipAPI currShip = (ShipAPI) shipGridIterator.next();
				boolean setForArc = false;
				boolean setForOverload = false;
				if (currShip.isShuttlePod() || currShip == ship || currShip.hasTag(Tags.VARIANT_FX_DRONE) ||
					currShip.getMutableStats().getHullDamageTakenMult().getMult() <= 0f
					|| currShip.isHulk() || !currShip.isAlive() || currShip.getMaxFlux() <= 0f
					|| !MathUtils.isWithinRange(
						currShip, ship.getLocation(), FLUX_RANGE_BASE - FLUX_RANGE_MAX_PENALTY * ship.getFluxLevel()
					)
				)
					continue;
				
				if (fluxPulseInterval.intervalElapsed() && 
					(currShip.getOwner() - 0.5f) * arcFlux > 0.0f &&
					!(currShip.isFighter() && Misc.random.nextFloat() < ARC_FIGHTER_CHANCE)) {
					setForArc = true;
				}
				
				if (Math.abs(fluxChange + hardFluxChange) > 0.0 && currShip.getOwner() == changeFriendly) {
					if (!currShip.getFluxTracker().isOverloaded()) {
						setForOverload = currShip.getCurrFlux() + (fluxChange + hardFluxChange) * FLUX_PROJECT_MULT >
							currShip.getMaxFlux();
						float excess = currShip.getCurrFlux() + (fluxChange + hardFluxChange) - currShip.getMaxFlux();
						currShip.getFluxTracker().increaseFlux(fluxChange * FLUX_PROJECT_MULT, false);
						currShip.getFluxTracker().increaseFlux(hardFluxChange * FLUX_PROJECT_MULT, true);
						if (setForOverload && !Float.isNaN(currShip.getFluxLevel()) && 
							currShip.getFluxLevel() < 1f &&
							currShip.getCurrFlux() 	+ hardFluxChange * FLUX_PROJECT_MULT < currShip.getMaxFlux()) {
							// if it would be soft flux that overloads the target, add it as hard flux.
							// can potentially add a lot of hard flux that should be soft flux, but
							// this method allows for proportional overload times
							currShip.getFluxTracker().increaseFlux(currShip.getMaxFlux() - currShip.getCurrFlux(), false);
							currShip.getFluxTracker().increaseFlux(
								Math.min(excess, currShip.getMaxFlux() * 0.15f), true);
						}
					}
					else {
						currShip.getFluxTracker().setCurrFlux(
							Math.min(currShip.getCurrFlux() + (fluxChange + hardFluxChange) * FLUX_PROJECT_MULT,
							currShip.getMaxFlux())
						);
						currShip.getFluxTracker().setHardFlux(
							Math.min(currShip.getFluxTracker().getHardFlux() + hardFluxChange * FLUX_PROJECT_MULT,
							currShip.getMaxFlux())
						);
					}
					if (currShip.getOwner() == changeFriendly && !currShip.isFighter())
						combatEngine.addFloatingDamageText(
							currShip.getLocation(), Math.abs(fluxChange + hardFluxChange) * FLUX_PROJECT_MULT,
							new Color(215, 125, 215), currShip, ship
						);
				}
				if (setForArc || setForOverload) {
					if (setForOverload)
						Global.getSoundPlayer().playSound(
							"espc_spark",
							0.8f + Misc.random.nextFloat() * 0.2f,
							1f,
							currShip.getLocation(),
							currShip.getVelocity()
						);
					if (setForOverload || 
						Misc.random.nextFloat() > 0.7f - Math.min(0.4f, arcFlux / ship.getMaxFlux() * 6f)
						) {
						didArc = true;
						EmpArcParams params = new EmpArcParams();
						params.segmentLengthMult = 8f;
						params.zigZagReductionFactor = 0.15f;
						params.fadeOutDist = 800f;
						if (!setForOverload)
							params.minFadeOutMult = 3f;
						params.flickerRateMult = 0.7f;
						if (!setForOverload)
							params.movementDurOverride = Math.max(0.1f, 
								Misc.getDistance(ship.getLocation(), currShip.getLocation()) / 5000f);
						
						EmpArcEntityAPI arc = combatEngine.spawnEmpArcVisual(
							ship.getLocation(), ship, currShip.getLocation(), currShip,
							(setForOverload && !currShip.isFighter()) ? 55f : Math.min(Math.abs(arcFlux)/10f, 35f), 
							new Color(240, 0, 255), new Color(185, 0, 255),
							params
						);
						Global.getSoundPlayer().playSound(
							"system_emp_emitter_impact",
							0.8f + Misc.random.nextFloat() * 0.2f,
							setForOverload ? 1.15f : 1f,
							currShip.getLocation(),
							currShip.getVelocity()
						);
						arc.setSingleFlickerMode(!setForOverload);
						arc.setRenderGlowAtStart(setForOverload);
						arc.setFadedOutAtStart(!setForOverload);
					}
				}
				if (didArc)
					jitterAmount = Math.min(arcFlux / ship.getMaxFlux() * 5f, 2.5f);
			}
			
			if (fluxPulseInterval.intervalElapsed()) {
				fluxPulseInterval.setInterval(0.2f, 0.4f);
				fluxPulseInterval.setElapsed(0.0f);
				arcFlux = 0f;
			}
		} else {
			// playLoop(java.lang.String id, java.lang.Object playingEntity, float pitch, float volume, Vector2f loc, Vector2f vel, float fadeIn, float fadeOut) 
		}
		
		fluxLast = ship.getCurrFlux();
		hardFluxLast = ship.getFluxTracker().getHardFlux();
//		ship.blockCommandForOneFrame(ShipCommand.FIRE);
//		ship.setHoldFireOneFrame(true);
	}
	public void unapply(MutableShipStatsAPI stats, String id) {
		fluxLast = -100f;
		arcFlux = 0f;
		jitterAmount = 0f;
		fluxPulseInterval.setInterval(0.2f, 0.4f);
		fluxPulseInterval.setElapsed(0.0f);
		stats.getFluxDissipation().unmodify(id);
	}
	
	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0)
			return new StatusData("flux dissipation +" + (int) ((FLUX_DISSIPATION_MULT - 1f) * 100f) + "%", false);
		return null;
	}
}
