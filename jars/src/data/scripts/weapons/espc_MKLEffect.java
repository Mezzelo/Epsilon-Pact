package data.scripts.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponEffectPluginWithInit;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

import java.util.ArrayList;

public class espc_MKLEffect implements OnFireEffectPlugin, EveryFrameWeaponEffectPlugin, WeaponEffectPluginWithInit {

	// the percentage of flux from each shot to decay
	private static final float FLUX_DECAY_PERCENT_FRIGATE = 0.8f;
	private static final float FLUX_DECAY_PERCENT_DESTROYER = 0.75f;
	private static final float FLUX_DECAY_PERCENT_CRUISER = 0.7f;
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
		currRecoil = Math.min(currRecoil + VIS_RECOIL_PER_SHOT, MAX_VIS_RECOIL);
		
		float fluxReduction = weapon.getFluxCostToFire() * thisFluxPercent + weapon.getFluxCostToFire() - BASE_FLUX_TO_FIRE;
		if (fluxReduction > 0f)
			fluxRemaining.add(fluxReduction);
    }

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
		
        if (weapon == null || engine.isPaused() || amount <= 0f || (fluxRemaining.size() == 0 && currRecoil <= 0f)) return;
		
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
		for (int i = fluxRemaining.size() - 1; i >= 0; i--) {
			fluxGet = (Float) fluxRemaining.get(i);
			ship.getFluxTracker().setCurrFlux(Math.max(ship.getCurrFlux() - (fluxChange > fluxGet ? fluxGet : fluxChange), 0f));
			if (fluxChange > fluxGet)
				fluxRemaining.remove(i);
			else
				fluxRemaining.set(i, fluxGet - fluxChange);
		}
    }
}