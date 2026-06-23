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
	private static final int PROJECTILES_PER_SHOT = 4;
	
	private float shotTime = 0f;
	private float spreadFacing = 0f;
	private float spreadVel;
	private int projs = 0;
	
    public void onFire(DamagingProjectileAPI proj, WeaponAPI weapon, CombatEngineAPI engine) {
    	if (MathUtils.getDistanceSquared(proj, weapon.getFirePoint(0)) > 1f || weapon.getShip() == null) {
    		proj.getVelocity().scale(Misc.random.nextFloat() * 0.15f + 0.95f);
    		return;
    	}
    	if (engine.getTotalElapsedTime(false) - shotTime > 0.02f || projs >= PROJECTILES_PER_SHOT) {
			projs = 1;
        	shotTime = engine.getTotalElapsedTime(false);
			spreadFacing = proj.getFacing();
			spreadVel = weapon.getProjectileSpeed();
		} else {
			projs++;
			proj.setFacing(spreadFacing + Misc.random.nextFloat() * burstSpread - (burstSpread / 2.0f));
			float speedMod = Misc.random.nextFloat();
			Vector2f.add(new Vector2f((float) FastTrig.cos(Math.toRadians(proj.getFacing())) * spreadVel * (0.95f + speedMod * 0.15f), 
				(float) FastTrig.sin(Math.toRadians(proj.getFacing())) * spreadVel * (0.95f + speedMod * 0.15f)), 
				weapon.getShip().getVelocity(), proj.getVelocity());
			
		}
    }
}