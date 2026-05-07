// all this effort for a few trails and a puff of smoke.  pretty, though :)

package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;

import data.scripts.util.MezzUtils;

import org.magiclib.plugins.MagicTrailPlugin;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.List;

// import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;
				
public class espc_SlamfireVFX extends BaseEveryFrameCombatPlugin {
	
	private float startTime = -1f;
	private Vector2f velOffset;
	private float smokeLast;
	
	private static final float TRAIL_DURATION = 0.4f;
	private static final float SMOKE_DURATION = 8f;
	private static final float SMOKE_INTERVAL = 0.03f;
	
	private ShipAPI ship;
	private boolean isHightech = false;
	private ArrayDeque<DamagingProjectileAPI> projs;
	private ArrayDeque<Float> trailIds;
	private ArrayDeque<WeaponAPI> weapons;
	
	// private int barrelCount = 0;
	
	public espc_SlamfireVFX(ShipAPI ship, boolean isHightech) {
		this.ship = ship;
		this.isHightech = isHightech;
		projs = new ArrayDeque<DamagingProjectileAPI>();
		trailIds = new ArrayDeque<Float>();
	}
	
	public void initUse(Vector2f velOffset, ArrayDeque<WeaponAPI> weapons) {
		this.startTime = Global.getCombatEngine().getTotalElapsedTime(false);
		this.smokeLast = 0f;
		this.velOffset = velOffset;
		this.weapons = weapons;
		
        Iterator<WeaponAPI> wepIterator = weapons.iterator();
        while (wepIterator.hasNext()) {
        	WeaponAPI currWep = wepIterator.next();
        	for (int i = 0; i < currWep.getSpec().getHardpointAngleOffsets().size(); i++) {
        		trailIds.addLast(MagicTrailPlugin.getUniqueID());
        		// barrelCount++;
        	}
        }
	}
	
	public void addProj(DamagingProjectileAPI proj) {
		projs.addLast(proj);
    	trailIds.addLast(MagicTrailPlugin.getUniqueID());
    	trailIds.addLast(MagicTrailPlugin.getUniqueID());
	}
	
    @Override
	public void advance(float amount, List<InputEventAPI> events) {
    	CombatEngineAPI combatEngine = Global.getCombatEngine();
    	
		if (combatEngine == null || combatEngine.getElapsedInLastFrame() <= 0f || combatEngine.isPaused())
			return;
		
		if (startTime < 0f) {
			if (ship == null || !combatEngine.isInPlay(ship) || !ship.isAlive() || ship.isHulk())
				combatEngine.removePlugin(this);
			return;
		}
		
		float elapsed = combatEngine.getTotalElapsedTime(false) - startTime;
		if (elapsed >= SMOKE_DURATION) {
			startTime = -1f;
			if (!isHightech) {
				projs.clear();
				trailIds.clear();
			}
			return;
		} else if (elapsed > smokeLast + SMOKE_INTERVAL) {
			smokeLast += SMOKE_INTERVAL;
		}
        Iterator<Float> idIterator = trailIds.iterator();
        
        Iterator<WeaponAPI> wepIterator = weapons.iterator();
        while (wepIterator.hasNext()) {
        	WeaponAPI currWep = wepIterator.next();
        	for (int i = 0; i < currWep.getSpec().getHardpointAngleOffsets().size(); i++) {
        		if (i == 1 && currWep.getSpec().getHardpointAngleOffsets().get(0).equals(
        			currWep.getSpec().getHardpointAngleOffsets().get(1)) &&
        			currWep.getSpec().getHardpointFireOffsets().get(0).equals(
        			currWep.getSpec().getHardpointFireOffsets().get(1)))
        			break;
            	float trailId = idIterator.next();
                MagicTrailPlugin.addTrailMemberAdvanced(
    				ship,
    				trailId,	
    				Global.getSettings().getSprite("fx", "espc_trail_wispy"),
    				currWep.getFirePoint(i),
    				0f,
    				0f,
    				VectorUtils.getFacing(ship.getVelocity()),
    				0f,
    				0f,
    				6,
    				64,
                	new Color(125,125,125),
                	new Color(85,85,85),
                	0.8f * MezzUtils.halfSineIn((SMOKE_DURATION - elapsed)/SMOKE_DURATION),
    				0.0f,
    				0.3f,
    				0.2f,
    				GL11.GL_SRC_ALPHA,
    				GL11.GL_ONE_MINUS_SRC_ALPHA,
    				128f,
    				-64f - 64f * (SMOKE_DURATION - elapsed)/SMOKE_DURATION,
    				trailId * 128f,
    				Misc.ZERO,
    				null,
                	CombatEngineLayers.ABOVE_SHIPS_LAYER,
    				1f
                );
        	}
        }
		
		if (elapsed > TRAIL_DURATION || isHightech)
			return;

        Iterator<DamagingProjectileAPI> projIterator = projs.iterator();
        
        while (projIterator.hasNext()) {
        	DamagingProjectileAPI proj = (DamagingProjectileAPI) projIterator.next();
        	float trailId = (Float) idIterator.next();
        	if (proj == null || !combatEngine.isEntityInPlay(proj) || proj.didDamage() || proj.isExpired()) {
        		projIterator.remove();
        		idIterator.remove();
        		idIterator.next();
        		idIterator.remove();
        		continue;
        	}
        	float trailId2 = (Float) idIterator.next();
        	
            MagicTrailPlugin.addTrailMemberAdvanced(
				proj,
				trailId2,	
				Global.getSettings().getSprite("fx", "base_trail_mild"),
				proj.getLocation(),
				0f,
				0f,
				proj.getFacing() - 180f,
				0f,
				0f,
				12,
				24,
				new Color(255,119,0),
            	new Color(225,60,0),
				0.55f * (TRAIL_DURATION - elapsed)/TRAIL_DURATION,
				0.01f,
				0.1f,
				0.05f,
				GL11.GL_SRC_ALPHA,
				GL11.GL_ONE,
				128f,
				-128f,
				0f,
				velOffset,
				null,
				CombatEngineLayers.ABOVE_SHIPS_LAYER,
				1f
            );
            
            MagicTrailPlugin.addTrailMemberAdvanced(
				proj,
				trailId,	
				Global.getSettings().getSprite("fx", "base_trail_fuzzy"),
				proj.getLocation(),
				0f,
				0f,
				proj.getFacing() - 180f,
				0f,
				0f,
				8,
				15,
            	new Color(125,115,105),
            	new Color(85,85,85),
				0.45f * (TRAIL_DURATION - elapsed)/TRAIL_DURATION,
				0.01f,
				0.3f,
				0.15f,
				GL11.GL_SRC_ALPHA,
				GL11.GL_ONE_MINUS_SRC_ALPHA,
				128f,
				-128f,
				0f,
				velOffset,
				null,
            	CombatEngineLayers.ABOVE_SHIPS_LAYER,
				1f
            );
        }
	}
}