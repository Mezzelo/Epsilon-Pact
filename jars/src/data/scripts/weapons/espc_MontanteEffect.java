package data.scripts.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponEffectPluginWithInit;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;

public class espc_MontanteEffect implements EveryFrameWeaponEffectPlugin, WeaponEffectPluginWithInit {

	// at this angle, the weapon's behaviour converts from normal beam to sweeping PD beam
	private static final float stabAngleMax = 50f;
	// private static final float beamDuration = 2f;
	private static final float sweepDuration = 3f;
	
	
	// 0 = beam, 1 = sweep
	private int fireMode = -1;
	
	private float arc;
	
	private float turnRateStore;
	private float sweepStart = -1f;
	
	private ShipAPI ship;
	// private 
	
	@Override
	public void init(WeaponAPI weapon) {
		arc = weapon.getArc();
		fireMode = (arc > stabAngleMax) ? 1 : 0;
		if (fireMode == 1) {
			weapon.setPDAlso(true);
		}
		ship = weapon.getShip();
	}
	
    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
		if (weapon == null || engine.isPaused() || amount <= 0f) return;
		// i'm noting getarcfacing returns 0, and can't find any other uses of it - assuming it's deprecated?  should ask about this later.
		// Global.getLogger(espc_MontanteEffect.class).info("facing: " + weapon.getArcFacing());
		if (weapon.getChargeLevel() > 0f) {
			if (fireMode == 1) {
				float cTime = engine.getTotalElapsedTime(false);
				if (sweepStart <= 0f) {
					sweepStart = cTime;
					turnRateStore = weapon.getTurnRate();
					weapon.setTurnRateOverride(0f);
				}
				if (cTime - sweepStart <= sweepDuration) {
					weapon.setCurrAngle(
						(weapon.getSlot().computeMidArcAngle(ship) -
						arc * ((float) FastTrig.cos(MathUtils.FTAU * (cTime - sweepStart)/sweepDuration)) / 2f + 360f) % 360f
					);
				}
			}
		} else if (sweepStart > 0f && fireMode == 1) {
			weapon.setTurnRateOverride(turnRateStore);
			sweepStart = -1f;
		}
		
			
		// Global.getLogger(espc_MontanteEffect.class).info("angle: " + weapon.getCurrAngle());
    }
}