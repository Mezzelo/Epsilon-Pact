package data.scripts.weapons;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponEffectPluginWithInit;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

import java.awt.Color;
import java.util.ArrayList;

import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.plugins.MagicTrailPlugin;

public class espc_MKLEffect implements OnFireEffectPlugin, EveryFrameWeaponEffectPlugin, WeaponEffectPluginWithInit {

	// the percentage of flux from each shot to decay
	private static final float FLUX_DECAY_PERCENT_FRIGATE = 0.8f;
	private static final float FLUX_DECAY_PERCENT_DESTROYER = 0.7f;
	private static final float FLUX_DECAY_PERCENT_CRUISER = 0.6f;
	private static final float FLUX_DECAY_PERCENT_CAPITAL = 0.5f;
	
	/* original vals, scaled for half fire rate/double flux cost
	private static final float FLUX_DECAY_PERCENT_LIGHT = 0.76f;
	private static final float FLUX_DECAY_PERCENT_CRUISER = 0.68f;
	private static final float FLUX_DECAY_PERCENT_CAPITAL = 0.52f;
	
	and the original, FUNNY stats: might make this into a limited wep
	private static final float FLUX_DECAY_PERCENT_LIGHT = 0.7f;
	private static final float FLUX_DECAY_PERCENT_CRUISER = 0.6f;
	private static final float FLUX_DECAY_PERCENT_CAPITAL = 0.4f;
	*/
	
	// how long it takes for each shot's flux to dissipate
	private static final float secsPerShot = 4f; 
	
	private static final float BASE_FLUX_TO_FIRE = 1500f;
	
	// 
	private static final float MAX_VIS_RECOIL = 7f;
	private static final float VIS_RECOIL_PER_SHOT = 2f;
	private static final float VIS_RECOIL_DECAY = 7f;
	
	private ShipAPI ship;
    private SpriteAPI barrel;
	// we want to retain the onFire plugin to avoid refunding extra flux from additional modifiers.
	// the complication of potential deviations in flux expenditure per-shot also prevents us from a better optimized method here, but it's not a big deal.
	private float fluxPerShot = -1f;
	private float thisFluxPercent = 0.55f;
	private ArrayList<Float> fluxRemaining = new ArrayList<Float>();
	private float barrelBaseY = 0f;
	private float currRecoil = 0f;
	
	private float trailID1 = 0f;
	
	@Override
	public void init(WeaponAPI weapon) {
		fluxPerShot = weapon.getFluxCostToFire();
		// fluxPerShot = BASE_FLUX_TO_FIRE;
		ship = weapon.getShip();
		if (ship == null)
			return;
		if (ship.getHullSize() == HullSize.CAPITAL_SHIP)
			thisFluxPercent = FLUX_DECAY_PERCENT_CAPITAL;
		else if (ship.getHullSize() == HullSize.CRUISER)
			thisFluxPercent = FLUX_DECAY_PERCENT_CRUISER;
		else if (ship.getHullSize() == HullSize.DESTROYER)
			thisFluxPercent = FLUX_DECAY_PERCENT_DESTROYER;
		else
			thisFluxPercent = FLUX_DECAY_PERCENT_FRIGATE;
		
		barrel = weapon.getBarrelSpriteAPI();
		if (barrel != null)
			barrelBaseY = barrel.getCenterY();
		// weapon.getSlot().isTurret() - potential offset required based on hardpoint
		
	}
	
    @Override
    public void onFire(DamagingProjectileAPI proj, WeaponAPI weapon, CombatEngineAPI engine) {
    	if (MathUtils.getDistanceSquared(proj, weapon.getFirePoint(0)) > 1f || weapon.getShip() == null)
    		return;
		currRecoil = Math.min(currRecoil + VIS_RECOIL_PER_SHOT, MAX_VIS_RECOIL);
		for (int i = 0; i < 10 + Math.min(fluxRemaining.size(), 6) * 2; i++) {
			float dist = Misc.random.nextFloat();
			engine.addHitParticle(new Vector2f(
				(float) FastTrig.cos(Math.toRadians(weapon.getCurrAngle())) * (15f + Math.min(fluxRemaining.size() * 3, 15f)) * dist
					+ weapon.getFirePoint(0).x,
				(float) FastTrig.sin(Math.toRadians(weapon.getCurrAngle())) * (15f + Math.min(fluxRemaining.size() * 3, 15f)) * dist
					+ weapon.getFirePoint(0).y),
				ship.getVelocity(),
				(Misc.random.nextFloat() * 5f + 9f + Math.min(fluxRemaining.size() * 3f, 12f)) *
					(1f - dist * 0.65f),
				1f, 
				0f, 0.15f, new Color(250, 225, 150));
		}
		
		float fluxReduction = weapon.getFluxCostToFire() * thisFluxPercent + weapon.getFluxCostToFire() - BASE_FLUX_TO_FIRE;
		if (fluxReduction > 0f) {
			fluxRemaining.add(fluxReduction);
			if (fluxRemaining.size() == 1 && trailID1 == 0f) {
				trailID1 = MagicTrailPlugin.getUniqueID();
			}
		}
    }

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
		
        if (Global.getCurrentState() != GameState.COMBAT ||
        	weapon == null || engine.isPaused() || amount <= 0f || (fluxRemaining.size() == 0 && currRecoil <= 0f)) return;
		
		if (barrel != null) {
			currRecoil = Math.max(
				0f, 
				currRecoil - amount * VIS_RECOIL_DECAY * (currRecoil > MAX_VIS_RECOIL - VIS_RECOIL_PER_SHOT ? 2f : 1f)
			);
	        barrel.setCenterY(barrelBaseY + currRecoil);
		}
		
		// req. arraylist for random access set
		float fluxChange = (1f / secsPerShot * fluxPerShot * thisFluxPercent) * amount;
		float fluxGet;
		float fluxTotal = 0f;
		for (int i = fluxRemaining.size() - 1; i >= 0; i--) {
			fluxGet = (Float) fluxRemaining.get(i);
			ship.getFluxTracker().setCurrFlux(Math.max(ship.getCurrFlux() - (fluxChange > fluxGet ? fluxGet : fluxChange), 0f));
			if (fluxChange > fluxGet)
				fluxRemaining.remove(i);
			else {
				fluxRemaining.set(i, fluxGet - fluxChange);
				fluxTotal += fluxGet - fluxChange;
			}
		}
		if (fluxRemaining.size() == 0 && trailID1 != 0f) {
			trailID1 = 0f;
		} else {
			float intensity = Math.min(Math.max(fluxTotal / 2000f, 0f), 1f);
            MagicTrailPlugin.addTrailMemberAdvanced(
				ship,
				trailID1,	
				Global.getSettings().getSprite("fx", "espc_trail_wispy"),
				new Vector2f(
					weapon.getFirePoint(0).x + (float) FastTrig.cos(Math.toRadians(weapon.getCurrAngle())) * 3f, 
					weapon.getFirePoint(0).y + (float) FastTrig.sin(Math.toRadians(weapon.getCurrAngle())) * -7f),
				0f,
				0f,
				VectorUtils.getFacing(ship.getVelocity()),
				0f,
				0f,
				6,
				64,
            	new Color(185,185,185),
            	new Color(125,125,125),
            	intensity,
				0.0f,
				0.3f,
				0.45f,
				GL11.GL_SRC_ALPHA,
				GL11.GL_ONE_MINUS_SRC_ALPHA,
				128f,
				-64f - 64f * (6000f - Math.min(Math.max(fluxTotal / 6000f, 0f), 1f))/6000f,
				trailID1 * 128f,
				new Vector2f(
					(float) FastTrig.cos(Math.toRadians(weapon.getCurrAngle() - 90f)) * (100f * intensity + 50f),
					(float) FastTrig.sin(Math.toRadians(weapon.getCurrAngle() - 90f)) * (100f * intensity + 50f)),
				null,
            	CombatEngineLayers.ABOVE_SHIPS_LAYER,
				1f
            );
		}
    }
}