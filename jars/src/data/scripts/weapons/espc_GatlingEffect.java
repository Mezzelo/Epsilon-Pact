package data.scripts.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponEffectPluginWithInit;

public class espc_GatlingEffect implements OnFireEffectPlugin, EveryFrameWeaponEffectPlugin, WeaponEffectPluginWithInit {

	public static final float minCooldown = 0.075f;
	public static final float rateDecayPerSecond = 0.25f;
	public static final float rateIncreasePerShot = 0.05f;
	
	private float cCooldown = -1f;
	private float cooldownLast = 0f;
	private ShipAPI ship;
	
    @Override
    public void onFire(DamagingProjectileAPI proj, WeaponAPI weapon, CombatEngineAPI engine) {
		// cCooldown = cCooldown - rateIncreasePerShot;
    }
	
	@Override
	public void init(WeaponAPI weapon) {
		ship = weapon.getShip();
	}

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (weapon == null || engine.isPaused() || amount <= 0f) return;
		// Global.getLogger(espc_GatlingEffect.class).info(weapon.getCooldown() + ", " + weapon.getCooldownRemaining());
		
		if (cCooldown <= 0f)
			cCooldown = weapon.getCooldown();
		
		if (weapon.getCooldownRemaining() > cooldownLast) {
			cCooldown = Math.max(cCooldown - rateIncreasePerShot, minCooldown);
			weapon.setRemainingCooldownTo(cCooldown);
		}
		else if (weapon.getCooldownRemaining() <= 0f) {
			cCooldown = Math.min(weapon.getCooldown(), cCooldown + rateDecayPerSecond * amount * Math.min(ship.getMutableStats().getBallisticRoFMult().getMult(), 1f));
		}
		
		cooldownLast = weapon.getCooldownRemaining();
		
    }
}