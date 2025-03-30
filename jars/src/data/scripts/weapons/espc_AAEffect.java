package data.scripts.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;

import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;

public class espc_AAEffect implements OnFireEffectPlugin {

	public static final float burstSpread = 8f;
	
	private float shotTime = 0f;
	private float spreadFacing = 0f;
	// private Vector2f spreadLocation;
	private float spreadVel;
	
	private Vector2f shipVel;
	
    public void onFire(DamagingProjectileAPI proj, WeaponAPI weapon, CombatEngineAPI engine) {
    	if (MathUtils.getDistanceSquared(proj, weapon.getFirePoint(0)) > 1f || weapon.getShip() == null)
    		return;
    	if (engine.getTotalElapsedTime(false) - shotTime > 0.02f) {
        	shotTime = engine.getTotalElapsedTime(false);
			// spreadLocation = proj.getLocation();
			spreadFacing = proj.getFacing();
			shipVel = weapon.getShip().getVelocity();
			// spreadVel = (float) Math.hypot(proj.getVelocity().x - shipVel.x, proj.getVelocity().y - shipVel.y);
			spreadVel = weapon.getProjectileSpeed();
		} else {
			// Vector2f.add(spreadLocation, new Vector2f(), proj.getLocation());
			proj.setFacing(spreadFacing + Misc.random.nextFloat() * burstSpread - (burstSpread / 2.0f));
			float speedMod = Misc.random.nextFloat();
			Vector2f.add(new Vector2f((float) FastTrig.cos(Math.toRadians(proj.getFacing())) * spreadVel * (0.95f + speedMod * 0.15f) + shipVel.x, 
				(float) FastTrig.sin(Math.toRadians(proj.getFacing())) * spreadVel * (0.95f + speedMod * 0.15f) + shipVel.y), new Vector2f(), proj.getVelocity());
			
		}
    }
}