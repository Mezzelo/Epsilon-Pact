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

import org.magiclib.plugins.MagicTrailPlugin;

import java.awt.Color;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

// import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;
				
public class espc_SlamfireVFX extends BaseEveryFrameCombatPlugin {
	
	private float startTime;
	private Vector2f velOffset;
	private float smokeLast;
	
	private static final float TRAIL_DURATION = 0.4f;
	private static final float SMOKE_DURATION = 2.4f;
	private static final float SMOKE_INTERVAL = 0.03f;
	
	private ShipAPI ship;
	private LinkedList<DamagingProjectileAPI> projs;
	private LinkedList<Float> trailIds;
	private LinkedList<WeaponAPI> weapons;
	
	// private int barrelCount = 0;
	
	public espc_SlamfireVFX(ShipAPI ship, Vector2f velOffset, LinkedList<WeaponAPI> weapons) {
		this.ship = ship;
		this.startTime = Global.getCombatEngine().getTotalElapsedTime(false);
		this.smokeLast = 0f;
		this.velOffset = velOffset;
		this.weapons = weapons;
		projs = new LinkedList<DamagingProjectileAPI>();
		trailIds = new LinkedList<Float>();
		
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
		
		float elapsed = combatEngine.getTotalElapsedTime(false) - startTime;
		if (elapsed >= SMOKE_DURATION) {
			combatEngine.removePlugin(this);
			return;
		} else if (elapsed > smokeLast + SMOKE_INTERVAL) {
			smokeLast += SMOKE_INTERVAL;
			/*
	        Iterator<WeaponAPI> wepIterator = weapons.iterator();
	        
	        while (wepIterator.hasNext()) {
	        	WeaponAPI currWep = wepIterator.next();
	        	for (int i = 0; i < currWep.getSpec().getHardpointAngleOffsets().size(); i++) {
	        		combatEngine.addSmokeParticle(
	                	currWep.getFirePoint(i),
	                	MathUtils.getRandomPointInCircle(null, Misc.random.nextFloat() * 8f + 8f),
	                	(17f - elapsed / SMOKE_DURATION * 10f + Misc.random.nextFloat() * 8f) 
	                		/ currWep.getSpec().getHardpointAngleOffsets().size(),
	                	0.6f - elapsed / SMOKE_DURATION * 0.6f, // does opacity not work w/ smoke lol
	                	1f,
	                	new Color(
	                		100 + (int) (elapsed / SMOKE_DURATION * 40f),
	                		100 + (int) (elapsed / SMOKE_DURATION * 40f),
	                		100 + (int) (elapsed / SMOKE_DURATION * 40f),
	                		160 - (int) (elapsed / SMOKE_DURATION * 160f)
	                	)
	        		);
	        	}
	        }*/
		}
        Iterator<Float> idIterator = trailIds.iterator();
        
        Iterator<WeaponAPI> wepIterator = weapons.iterator();
        while (wepIterator.hasNext()) {
        	WeaponAPI currWep = wepIterator.next();
        	for (int i = 0; i < currWep.getSpec().getHardpointAngleOffsets().size(); i++) {
            	float trailId = idIterator.next();
                MagicTrailPlugin.addTrailMemberAdvanced(
    				null,
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
                	0.8f * (SMOKE_DURATION - elapsed)/SMOKE_DURATION,
    				0.0f,
    				0.3f,
    				0.2f,
    				GL11.GL_SRC_ALPHA,
    				GL11.GL_ONE_MINUS_SRC_ALPHA,
    				128f,
    				-128f,
    				trailId * 128f,
    				Misc.ZERO,
    				null,
                	CombatEngineLayers.ABOVE_SHIPS_LAYER,
    				1f
                );
        	}
        }
		
		if (elapsed > TRAIL_DURATION)
			return;

        Iterator<DamagingProjectileAPI> projIterator = projs.iterator();
        
        while (projIterator.hasNext()) {
        	DamagingProjectileAPI proj = (DamagingProjectileAPI) projIterator.next();
        	float trailId = (Float) idIterator.next();
        	if (proj == null || !combatEngine.isInPlay(proj) || proj.didDamage() || proj.isExpired()) {
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