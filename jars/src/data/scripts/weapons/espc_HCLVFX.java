// author: Mezzelo

// direct rendering of a deterministic particle sim along the beam's trail.  particles bounce inwards and outwards from the beam, requiring this bespoke implementation as opposed to going through default combat particle behaviour.
// as we're avoiding creating new data bar the seeds used, we can get relatively gratuitous here

package data.scripts.weapons;

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
				
public class espc_HCLVFX extends BaseEveryFrameCombatPlugin {
	
	private final DamagingProjectileAPI proj;
	private final Vector2f startPos;
	private final float startAng;
	private Vector2f startVel;
	
	private final float effectStart;
	private float effectEnd = -1f;
	
	private static final float particleDurBase = 1f;
	private static final float particleSpawnInterval = 0.005f;
	private static final Color colorInner = new Color(255, 235, 220);
	private static final Color colorOuter = new Color(85, 45, 255);
	
	private float angSin;
	private float angCos;
	private float angSinY;
	private float angCosY;
	
	private float rand;
	
    public espc_HCLVFX(DamagingProjectileAPI proj) {
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
			
		if (effectEnd > 0f && engine.getTotalElapsedTime(false) > effectEnd + particleDurBase)
			engine.removePlugin(this);
	}
	
    @Override
    public void renderInWorldCoords(ViewportAPI viewport) {
		
		CombatEngineAPI engine = Global.getCombatEngine();
		float cTime = engine.getTotalElapsedTime(false);

		float iMin = Math.max(cTime - particleDurBase - effectStart, 0f);
		iMin = iMin - iMin % particleSpawnInterval;
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
		
		for (float i = 0f; i < iMax; i += particleSpawnInterval) {
			// don't believe there's any other way to properly seed these random vals due to the sequential nature of multiplicative congruential gen
			// to java's credit the class is very lightweight
			float randX = particleNoiseX.nextFloat();
			float randY = particleNoiseY.nextFloat();
			float randZ = particleNoiseZ.nextFloat();
			if (i < iMin)
				continue;
			
			// random distribution is square instead of circular, but it's not noticeable
			float particleX = startPos.x + startVel.x * i + 
				(float) (FastTrig.sin(((i + rand * 5f) * (15f + rand * 20f))) * 50f + randX * 36f - 18f) 
				* angCos * MezzUtils.halfSineOut((cTime - i - effectStart)/particleDurBase)
				+ (float) (FastTrig.cos(((i + rand * 7f) * (20f + rand * 30f))) * 65f + randY * 36f - 18f) 
				* angCosY * MezzUtils.halfSineIn((cTime - i - effectStart)/particleDurBase);
			float particleY = startPos.y + startVel.y * i + 
				(float) (FastTrig.sin(((i + rand * 5f) * (15f + rand * 20f))) * 50f + randX * 36f - 18f) 
				* angSin * MezzUtils.halfSineOut((cTime - i - effectStart)/particleDurBase)
				+ (float) (FastTrig.cos(((i + rand * 7f) * (20f + rand * 30f))) * 65f + randY * 36f - 18f) 
				* angSinY * MezzUtils.halfSineIn((cTime - i - effectStart)/particleDurBase);
			float particleSize = 6f + randZ * 8f;
			
			/* basic time-based gradient
			color algorithms preserved for a/b testing
			if (cTime - i - effectStart < particleDurBase / 2f)
				MezzUtils.colorHSBLerp4UBRev(colorInner, colorMid, 255, 200,
					(cTime - i - effectStart)/particleDurBase * 2f);
			else
				MezzUtils.colorHSBLerp4UB(colorMid, colorOuter, 200, 0,
					(cTime - i - effectStart)/particleDurBase * 2f - 1f);
			
			*/
			
			/* offset-based colors
			if (Math.abs(randX - 0.5f) + Math.abs(randY - 0.5f) < 0.65f)
				MezzUtils.colorHSBLerp4UBRev(colorInner, colorMid, 
					(int)(MathUtils.clamp((particleDurBase * 2f + (i - cTime + effectStart) * 2f), 0f, particleDurBase)/particleDurBase * 255f),
					(Math.abs(randX - 0.5f) + Math.abs(randY - 0.5f)) / 0.65f
				);
			else
				MezzUtils.colorHSBLerp4UB(colorMid, colorOuter,
					(int)(MathUtils.clamp((particleDurBase * 2f + (i - cTime + effectStart) * 2f), 0f, particleDurBase)/particleDurBase * 255f),
					(Math.abs(randX - 0.5f) + Math.abs(randY - 0.5f) - 0.65f) / 0.35f
				);
			*/
			

			
			
			GL11.glTexCoord2f(0, 0);
			GL11.glVertex2f(particleX - particleSize/2f, particleY - particleSize/2f);
			GL11.glTexCoord2f(1, 0);
			GL11.glVertex2f(particleX + particleSize/2f, particleY - particleSize/2f);
			GL11.glTexCoord2f(1, 1);
			GL11.glVertex2f(particleX + particleSize/2f, particleY + particleSize/2f);
			GL11.glTexCoord2f(0, 1);
			GL11.glVertex2f(particleX - particleSize/2f, particleY + particleSize/2f);
		}
		
		if (effectEnd > 0f) {
			
			// no need to reset seed: we already iterate fully through it prior to this, for better or worse.
			
			for (float i = 0; i < 18; i ++) {
				// slightly different application of our noise here, but the same set of sequences suffices.
				float randX = particleNoiseX.nextFloat();
				float randY = particleNoiseY.nextFloat();
				float randZ = particleNoiseZ.nextFloat();
			
				float particleX = startPos.x + startVel.x * (effectEnd - effectStart) + startVel.x/(2f + randX * 7f) * (cTime - effectEnd) + 
					MezzUtils.halfSineOut((cTime - effectEnd)/particleDurBase) * (float) FastTrig.cos(Math.toRadians(startAng - 55f + 110f * randY)) * 25f;
				float particleY = startPos.y + startVel.y * (effectEnd - effectStart) + startVel.y/(2f + randX * 7f) * (cTime - effectEnd) + 
					MezzUtils.halfSineOut((cTime - effectEnd)/particleDurBase) * (float) FastTrig.sin(Math.toRadians(startAng - 55f + 110f * randY)) * 25f;
				float particleSize = 9f + randZ * 6f;
				

					
				GL11.glColor4ub((byte) 200,
						(byte) 155,
						(byte) 255,
						(byte) ((int)(MathUtils.clamp((effectEnd + particleDurBase - cTime - i/24f) * 2f, 0f, particleDurBase)/particleDurBase * 255f)));
						
				GL11.glTexCoord2f(0, 0);
				GL11.glVertex2f(particleX - particleSize/2f, particleY - particleSize/2f);
				GL11.glTexCoord2f(1, 0);
				GL11.glVertex2f(particleX + particleSize/2f, particleY - particleSize/2f);
				GL11.glTexCoord2f(1, 1);
				GL11.glVertex2f(particleX + particleSize/2f, particleY + particleSize/2f);
				GL11.glTexCoord2f(0, 1);
				GL11.glVertex2f(particleX - particleSize/2f, particleY + particleSize/2f);
			
			}
		}
		
		GL11.glEnd();
		
	}
}
