// deterministic sim of single frame renders to achieve more complex particle behaviour
// applying easing curves is instrumental to achieving the specific visuals of this effect,
// and don't appear to be available in the utilities i've found.  attempting to use as little memory as possible here.
// direct render calls has this very performant - 50x fire rate with minimal impact on weaker hardware,
// although lmk if you run into issues.

// you could almost certainly do this faster on the gpu with shaders
// don't have time to study em rn.  there's worse performance hogs.
package data.scripts.weapons.proj;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.lwjgl.util.vector.Vector2f;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.FastTrig;

import org.lwjgl.opengl.GL11;

import data.scripts.util.MezzUtils;
				
public class espc_AutocannonVFX extends BaseEveryFrameCombatPlugin {
	
	
	private static final int PARTICLE_COUNT = 128;
	private static final float PARTICLE_DUR = 0.7f;
	private static final float PARTICLE_DUR_BIG = 1.0f;
	private static final float PARTICLE_DUR_MIN = 0.2f;
	private static final float PARTICLE_DIST_MIN = 50f;
	private static final float PARTICLE_DIST_MAX = 300f;
	private static final float PARTICLE_DIST_BIG = 400f;
	private static final float DIST_MULT_MIN = 0.6f;
	private static final float DIST_MULT_MAX = 1.0f;
	private static final float PARTICLE_LENGTH_MAX = 22f;
	private static final float PARTICLE_LENGTH_MIN = 4.5f;
	private static final float PARTICLE_WIDTH_MAX = 3f;
	private static final float PARTICLE_WIDTH_MIN = 1.5f;
	private static final float PARTICLE_DIST_FORWARDS = 150f;
	private static final Color COLOR_START = new Color(255, 215, 80);
	private static final Color COLOR_END = new Color(255, 85, 10);
	private static final float BIG_CHANCE = 0.1f;
	
	private final ShipAPI ship;
	
	private ArrayDeque<AutocannonProj> projs;
	
    public espc_AutocannonVFX(ShipAPI ship) {
    	this.ship = ship;
    	projs = new ArrayDeque<AutocannonProj>();
    }
    
    public void addProj(Vector2f point, float ang) {
    	projs.addLast(new AutocannonProj(point, ang));
    }
    
    private class AutocannonProj {
    	private final Vector2f startPos;
    	private final float startAng;
    	private final float effectStart;
    	private final float angSinY;
    	private final float angCosY;
    	private final float distMax;
    	private final float durMax;
    	private final int particleCount;
    	
    	public AutocannonProj(Vector2f pos, float angle) {
    		boolean isBig = Misc.random.nextFloat() < BIG_CHANCE;
            effectStart = Global.getCombatEngine().getTotalElapsedTime(false);
    		startPos = pos;
    		startAng = angle;
    		angSinY = (float) FastTrig.sin(Math.toRadians(startAng));
    		angCosY = (float) FastTrig.cos(Math.toRadians(startAng));
    		distMax = (isBig ? PARTICLE_DIST_BIG : PARTICLE_DIST_MAX) * 
    			(Misc.random.nextFloat() * (DIST_MULT_MAX - DIST_MULT_MIN) + DIST_MULT_MIN);
    		durMax = (isBig ? PARTICLE_DUR_BIG : PARTICLE_DUR);
    		particleCount = isBig ? PARTICLE_COUNT * 2 : PARTICLE_COUNT;
    		
    	}
    }

	
    @Override
    public void advance(float amount, List<InputEventAPI> events) {
    	if (amount == 0)
    		return;
		CombatEngineAPI engine = Global.getCombatEngine();
		if (projs.size() == 0) {
			if (ship == null || !engine.isInPlay(ship) || ship.isHulk() || !ship.isAlive()) {
				if (ship != null)
					ship.removeCustomData("espc_AutocannonPlugin");
				engine.removePlugin(this);
			}
			return;
		}
		Iterator<AutocannonProj> projIter = projs.iterator();
		while (projIter.hasNext()) {
			AutocannonProj proj = projIter.next();
			if (engine.getTotalElapsedTime(false) > proj.effectStart + proj.durMax)
				projIter.remove();
		}
	}
	
    @Override
    public void renderInWorldCoords(ViewportAPI viewport) {
		if (projs.size() == 0)
			return;
		CombatEngineAPI engine = Global.getCombatEngine();
		float cTime = engine.getTotalElapsedTime(false);
		
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
		SpriteAPI sprite = Global.getSettings().getSprite("systemMap", "radar_entity");
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, sprite.getTextureId());
		GL11.glBegin(GL11.GL_QUADS);
		
		for (AutocannonProj proj: projs) {
			if (!engine.getViewport().isNearViewport(proj.startPos, 500f))
				continue;
			
			Random rand = new Random((long) (proj.effectStart + proj.startPos.x * 10f + proj.startPos.y * 10f));
			
			for (int i = 0; i < proj.particleCount; i ++) {
				float dur = MezzUtils.halfSineIn(rand.nextFloat()) * (proj.durMax - PARTICLE_DUR_MIN) + PARTICLE_DUR_MIN;
				float ang = rand.nextFloat() * 360f;
				float distRand = rand.nextFloat();
				float width = (PARTICLE_WIDTH_MAX - PARTICLE_WIDTH_MIN) * rand.nextFloat() + PARTICLE_WIDTH_MIN;
				float dist = distRand * (proj.distMax - PARTICLE_DIST_MIN) + PARTICLE_DIST_MIN;
				if (cTime - proj.effectStart > dur)
					continue;
				float randCos = (float) FastTrig.cos((ang + proj.startAng) * MathUtils.FPI / 180f);
				float randSin = (float) FastTrig.sin((ang + proj.startAng) * MathUtils.FPI / 180f);
				float locSin = (float) FastTrig.sin(ang * MathUtils.FPI / 180f);
				float interp = Math.min((cTime - proj.effectStart)/dur, 1f);
				float scale = 1f - 
					Math.min(Math.max((cTime - proj.effectStart - dur / 2f) * 2f / dur, 0f), 1f);
				
				float x = proj.startPos.x + 
					MezzUtils.halfSineOut(interp) * randCos * dist * (0.65f + Math.abs(locSin) * 0.35f) +
					MezzUtils.halfSineIn(interp) * proj.angCosY * PARTICLE_DIST_FORWARDS * (distRand * 0.7f + 0.3f);
				float y = proj.startPos.y +
					MezzUtils.halfSineOut(interp) * randSin * dist * (0.65f + Math.abs(locSin) * 0.35f) +
					MezzUtils.halfSineIn(interp) * proj.angSinY * PARTICLE_DIST_FORWARDS * (distRand * 0.7f + 0.3f);

				float length = (PARTICLE_LENGTH_MAX - PARTICLE_LENGTH_MIN) * MezzUtils.halfSineIn(1f - interp) + PARTICLE_LENGTH_MIN;
				MezzUtils.colorHSBLerp4UB(COLOR_START, COLOR_END, 255, 255, MezzUtils.halfSineOut(interp));
				GL11.glTexCoord2f(0, 0);
				GL11.glVertex2f(
					x - length * randCos * scale -
					width * randSin * scale,
					y + width * randCos * scale -
					length * randSin * scale
				);
				GL11.glTexCoord2f(1, 0);
				GL11.glVertex2f(
					x + length * randCos * scale -
					width * randSin * scale,
					y + width * randCos * scale +
					length * randSin * scale
				);
				GL11.glTexCoord2f(1, 1);
				GL11.glVertex2f(
					x + length * randCos * scale +
					width * randSin * scale,
					y - width * randCos * scale +
					length * randSin * scale
				);
				GL11.glTexCoord2f(0, 1);
				GL11.glVertex2f(
					x - length * randCos * scale +
					width * randSin * scale,
					y - width * randCos * scale -
					length * randSin * scale
				);
				
				length *= 2f;
				width *= 3f;
				MezzUtils.colorHSBLerp4UB(COLOR_START, COLOR_END, 150, 50, interp);
				GL11.glTexCoord2f(0, 0);
				GL11.glVertex2f(
					x - length * randCos * scale -
					width * randSin * scale,
					y + width * randCos * scale -
					length * randSin * scale
				);
				GL11.glTexCoord2f(1, 0);
				GL11.glVertex2f(
					x + length * randCos * scale -
					width * randSin * scale,
					y + width * randCos * scale +
					length * randSin * scale
				);
				GL11.glTexCoord2f(1, 1);
				GL11.glVertex2f(
					x + length * randCos * scale +
					width * randSin * scale,
					y - width * randCos * scale +
					length * randSin * scale
				);
				GL11.glTexCoord2f(0, 1);
				GL11.glVertex2f(
					x - length * randCos * scale +
					width * randSin * scale,
					y - width * randCos * scale -
					length * randSin * scale
				);
				
			}
		}
		
		GL11.glEnd();
		
	}
}
