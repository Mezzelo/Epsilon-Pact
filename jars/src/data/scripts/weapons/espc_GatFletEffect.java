package data.scripts.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponEffectPluginWithInit;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import org.lazywizard.lazylib.FastTrig;

public class espc_GatFletEffect implements OnFireEffectPlugin, EveryFrameWeaponEffectPlugin, WeaponEffectPluginWithInit {

	public static final int shotsPerBurst = 5;
	public static final float burstSpread = 6f;
	
	public static final float minCooldown = 0.2f;
	public static final float rateDecayPerSecond = 0.25f;
	public static final float rateIncreasePerShot = 0.1f;
	
	private float cCooldown = -1f;
	private float cooldownLast = 0f;
	
	private int currentShot = 0;
	private float spreadFacing = 0f;
	// private Vector2f spreadLocation;
	private float spreadVel;
	
	private ShipAPI ship;
	private Vector2f shipVel;
	
	@Override
	public void init(WeaponAPI weapon) {
		ship = weapon.getShip();
	}
	
    @Override
    public void onFire(DamagingProjectileAPI proj, WeaponAPI weapon, CombatEngineAPI engine) {
        if (currentShot == 0) {
			// spreadLocation = proj.getLocation();
			spreadFacing = proj.getFacing();
			shipVel = ship.getVelocity();
			spreadVel = (float) Math.hypot(proj.getVelocity().x - shipVel.x, proj.getVelocity().y - shipVel.y);
		} else {
			// Vector2f.add(spreadLocation, new Vector2f(), proj.getLocation());
			proj.setFacing(spreadFacing + Misc.random.nextFloat() * burstSpread - (burstSpread / 2.0f));
			Vector2f.add(new Vector2f((float) FastTrig.cos(Math.toRadians(proj.getFacing())) * spreadVel + shipVel.x, 
				(float) FastTrig.sin(Math.toRadians(proj.getFacing())) * spreadVel + shipVel.y), new Vector2f(), proj.getVelocity());
			
		}
		currentShot++;
		if (currentShot >= shotsPerBurst)
			currentShot = 0;
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