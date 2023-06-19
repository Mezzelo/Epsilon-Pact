// deterministic sim of single frame renders to achieve more complex particle behaviour
// applying easing curves is instrumental to achieving the specific visuals of this effect,
// and don't appear to be available in the utilities i've found.  attempting to use as little memory as possible here.
// direct render calls has this very performant - 50x fire rate with minimal impact on weaker hardware,
// although lmk if you run into issues.

// the moment the conditions change (i.e. projectile changes trajectory or speed) we abort,
// rather than deal with desync at the cost of more complexity

// you could almost certainly do this faster on the gpu with shaders
// don't have time to study em rn.  there's worse performance hogs.

package data.scripts.weapons.proj;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.input.InputEventAPI;

import java.awt.Color;
import java.util.List;
import java.util.Random;
import com.fs.starfarer.api.util.Misc;

import org.lwjgl.util.vector.Vector2f;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.FastTrig;

import org.lwjgl.opengl.GL11;

import data.scripts.util.MezzUtils;
				
public class espc_FisProjVFX extends BaseEveryFrameCombatPlugin {
	
	private final DamagingProjectileAPI proj;
	private final Vector2f startPos;
	private final float startAng;
	private Vector2f startVel;
	
	private final float effectStart;
	private float effectEnd = -1f;
	
	private static final float PARTICLE_DUR_BASE = 1f;
	private static final float PARTICLE_INTERVAL = 0.005f;
	private static final Color COLOR_INNER = new Color(255, 235, 220);
	private static final Color COLOR_MID = new Color(200, 80, 255);
	private static final Color COLOR_OUTER = new Color(85, 45, 255);
	
	private float angSin;
	private float angCos;
	private float angSinY;
	private float angCosY;
	
	private float rand;
	
    public espc_FisProjVFX(DamagingProjectileAPI proj) {
        this.proj = proj;
        this.effectStart = Global.getCombatEngine().getTotalElapsedTime(false);
		this.startPos = new Vector2f();
		Vector2f.add(proj.getLocation(), new Vector2f(), this.startPos);
		this.startAng = proj.getFacing();
		this.startVel = new Vector2f();
		Vector2f.add(proj.getVelocity(), new Vector2f(), this.startVel);
		this.angSin = (float) FastTrig.sin(Math.toRadians(startAng - 90f));
		this.angCos = (float) FastTrig.cos(Math.toRadians(startAng - 90f));
		this.angSinY = (float) FastTrig.sin(Math.toRadians(startAng));
		this.angCosY = (float) FastTrig.cos(Math.toRadians(startAng));
		this.rand = Misc.random.nextFloat();
    }

	
    @Override
    public void advance(float amount, List<InputEventAPI> events) {
		CombatEngineAPI engine = Global.getCombatEngine();
		// if (engine.isPaused())
		// 	return;
		// initializes 0, very cool.
		if (startVel.length() == 0f) {
			Vector2f.add(proj.getVelocity(), new Vector2f(), this.startVel);
			return;
		}
		
		if (!engine.isEntityInPlay(proj) && effectEnd <= 0f)
			effectEnd = Global.getCombatEngine().getTotalElapsedTime(false);
		else if (effectEnd <= 0f) {
			Vector2f velChangeCheck = new Vector2f();
			Vector2f.sub(startVel, proj.getVelocity(), velChangeCheck);
			if (Math.abs(startAng - proj.getFacing()) > 1f || velChangeCheck.length() > 1f || proj.didDamage() 
				// || proj.isFading() 
				)
				effectEnd = Global.getCombatEngine().getTotalElapsedTime(false);
		}
			
		if (effectEnd > 0f && engine.getTotalElapsedTime(false) > effectEnd + PARTICLE_DUR_BASE)
			engine.removePlugin(this);
	}
	
    @Override
    public void renderInWorldCoords(ViewportAPI viewport) {
		
		CombatEngineAPI engine = Global.getCombatEngine();
		float cTime = engine.getTotalElapsedTime(false);

		float iMin = Math.max(cTime - PARTICLE_DUR_BASE - effectStart, 0f);
		iMin = iMin - iMin % PARTICLE_INTERVAL;
		float iMax = effectEnd <= 0f ? cTime - effectStart: effectEnd - effectStart;
		Random particleNoiseX = new Random((long) (effectStart + startPos.x * 10f + startPos.y * 10f));
		Random particleNoiseY = new Random((long) ((effectStart + startPos.x * 10f + startPos.y * 10f) * 2f));
		Random particleNoiseZ = new Random((long) ((effectStart + startPos.x * 10f + startPos.y * 10f) * 3f));

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
		SpriteAPI sprite = Global.getSettings().getSprite("systemMap", "radar_entity");
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, sprite.getTextureId());
		GL11.glBegin(GL11.GL_QUADS);
		
		for (float i = 0f; i < iMax; i += PARTICLE_INTERVAL) {
			// don't believe there's any other way to properly seed these random vals,
			// given the sequential nature of multiplicative congruential gen
			// the class is lightweight as can be.
			float randX = particleNoiseX.nextFloat();
			float randY = particleNoiseY.nextFloat();
			float randZ = particleNoiseZ.nextFloat();
			if (i < iMin)
				continue;
			
			// basically a layer of pseudo-random-enough bullshit noise over a chaotic trig func
			// grants a nice degree of uniformity to the patterns
			// random distribution is square instead of circular, but it's not really noticeable /cope
			float particleX = startPos.x + startVel.x * i + 
				(float) (FastTrig.sin(((i + rand * 5f) * (15f + rand * 20f))) * 30f + randX * 36f - 18f) 
				* angCos * MezzUtils.halfSineOut((cTime - i - effectStart)/PARTICLE_DUR_BASE)
				+ (float) (FastTrig.cos(((i + rand * 7f) * (20f + rand * 30f))) * 65f + randY * 36f - 18f) 
				* angCosY * MezzUtils.halfSineIn((cTime - i - effectStart)/PARTICLE_DUR_BASE);
			float particleY = startPos.y + startVel.y * i + 
				(float) (FastTrig.sin(((i + rand * 5f) * (15f + rand * 20f))) * 30f + randX * 36f - 18f) 
				* angSin * MezzUtils.halfSineOut((cTime - i - effectStart)/PARTICLE_DUR_BASE)
				+ (float) (FastTrig.cos(((i + rand * 7f) * (20f + rand * 30f))) * 65f + randY * 36f - 18f) 
				* angSinY * MezzUtils.halfSineIn((cTime - i - effectStart)/PARTICLE_DUR_BASE);
			float particleSize = 6f + randZ * 8f;
			
			/* basic time-based gradient
			color algorithms preserved for a/b testing
			if (cTime - i - effectStart < PARTICLE_DUR_BASE / 2f)
				MezzUtils.colorHSBLerp4UBRev(COLOR_INNER, COLOR_MID, 255, 200,
					(cTime - i - effectStart)/PARTICLE_DUR_BASE * 2f);
			else
				MezzUtils.colorHSBLerp4UB(COLOR_MID, COLOR_OUTER, 200, 0,
					(cTime - i - effectStart)/PARTICLE_DUR_BASE * 2f - 1f);
			
			offset-based colors
			if (Math.abs(randX - 0.5f) + Math.abs(randY - 0.5f) < 0.65f)
				MezzUtils.colorHSBLerp4UBRev(COLOR_INNER, COLOR_MID, 
					(int)(MathUtils.clamp((PARTICLE_DUR_BASE * 2f + (i - cTime + effectStart) * 2f), 0f, PARTICLE_DUR_BASE)/PARTICLE_DUR_BASE * 255f),
					(Math.abs(randX - 0.5f) + Math.abs(randY - 0.5f)) / 0.65f
				);
			else
				MezzUtils.colorHSBLerp4UB(COLOR_MID, COLOR_OUTER,
					(int)(MathUtils.clamp((PARTICLE_DUR_BASE * 2f + (i - cTime + effectStart) * 2f), 0f, PARTICLE_DUR_BASE)/PARTICLE_DUR_BASE * 255f),
					(Math.abs(randX - 0.5f) + Math.abs(randY - 0.5f) - 0.65f) / 0.35f
				);
			*/
			
			// hybrid time+displacement approach for color, creating a hotter core
			if ((cTime - i - effectStart) / PARTICLE_DUR_BASE * (Math.abs(randX - 0.5f) + Math.abs(randY - 0.5f)) * 2f < 0.7f)
				MezzUtils.colorHSBLerp4UBRev(COLOR_INNER, COLOR_MID, 
					(int)(MathUtils.clamp((PARTICLE_DUR_BASE * 2f + (i - cTime + effectStart) * 2f), 0f, PARTICLE_DUR_BASE)/PARTICLE_DUR_BASE * 255f),
					(cTime - i - effectStart) / PARTICLE_DUR_BASE * (Math.abs(randX - 0.5f) + Math.abs(randY - 0.5f)) * 2f / 0.7f
				);
			else
				MezzUtils.colorHSBLerp4UB(COLOR_MID, COLOR_OUTER,
					(int)(MathUtils.clamp((PARTICLE_DUR_BASE * 2f + (i - cTime + effectStart) * 2f), 0f, PARTICLE_DUR_BASE)/PARTICLE_DUR_BASE * 255f),
					Math.min(((cTime - i - effectStart) / PARTICLE_DUR_BASE * (Math.abs(randX - 0.5f) + Math.abs(randY - 0.5f)) * 2f - 0.7f) * 7f / 0.3f, 1f)
				);
			
			MezzUtils.glSquare(particleX, particleY, particleSize/2f);
			
		}
		
		if (effectEnd > 0f) {
			
			// no need to reset seed: we already iterate fully through it prior.
			
			for (float i = 0; i < 18; i ++) {
				// slightly different application of our noise here, but the same set of sequences suffices.
				float randX = particleNoiseX.nextFloat();
				float randY = particleNoiseY.nextFloat();
				float randZ = particleNoiseZ.nextFloat();
			
				float particleX = startPos.x + startVel.x * (effectEnd - effectStart) + startVel.x/(2f + randX * 7f) * (cTime - effectEnd) + 
					MezzUtils.halfSineOut((cTime - effectEnd)/PARTICLE_DUR_BASE) * (float) FastTrig.cos(Math.toRadians(startAng - 55f + 110f * randY)) * 25f;
				float particleY = startPos.y + startVel.y * (effectEnd - effectStart) + startVel.y/(2f + randX * 7f) * (cTime - effectEnd) + 
					MezzUtils.halfSineOut((cTime - effectEnd)/PARTICLE_DUR_BASE) * (float) FastTrig.sin(Math.toRadians(startAng - 55f + 110f * randY)) * 25f;
				float particleSize = 9f + randZ * 6f;
				
				if (Math.abs(randY - 0.5f) < 0.25f)
					MezzUtils.colorHSBLerp4UBRev(COLOR_INNER, COLOR_MID, 
						(int)(MathUtils.clamp((effectEnd + PARTICLE_DUR_BASE - cTime - i/24f) * 2f, 0f, PARTICLE_DUR_BASE)/PARTICLE_DUR_BASE * 255f),
						Math.abs(randY - 0.5f) * 4f
					);
				else
					MezzUtils.colorHSBLerp4UB(COLOR_MID, COLOR_OUTER, 
						(int)(MathUtils.clamp((effectEnd + PARTICLE_DUR_BASE - cTime - i/24f) * 2f, 0f, PARTICLE_DUR_BASE)/PARTICLE_DUR_BASE * 255f),
						(Math.abs(randY - 0.5f) - 0.25f) * 4f
					);

				MezzUtils.glSquare(particleX, particleY, particleSize/2f);
			
			}
		}
		
		GL11.glEnd();
		
	}
}
