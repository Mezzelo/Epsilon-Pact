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

public class espc_RailFletEffect implements OnFireEffectPlugin, EveryFrameWeaponEffectPlugin, WeaponEffectPluginWithInit {
	
	public static final int shotsPerBurst = 5;
	public static final float burstSpread = 6.5f;
	
	private int currentShot = 0;
	private float spreadFacing = 0f;
	// private Vector2f spreadLocation;
	private float spreadVel;
	
	private ShipAPI thisShip;
	private Vector2f shipVel;
	
	@Override
	public void init(WeaponAPI weapon) {
		thisShip = weapon.getShip();
	}
	
    @Override
    public void onFire(DamagingProjectileAPI proj, WeaponAPI weapon, CombatEngineAPI engine) {
        if (currentShot == 0) {
			// spreadLocation = proj.getLocation();
			spreadFacing = proj.getFacing();
			shipVel = thisShip.getVelocity();
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
		
    }
}