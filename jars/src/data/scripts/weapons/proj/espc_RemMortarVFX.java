package data.scripts.weapons.proj;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.List;

import org.lwjgl.opengl.GL11;

import data.scripts.util.MezzUtils;
				
public class espc_RemMortarVFX extends BaseEveryFrameCombatPlugin {
	
	private static final float BLINK_FREQUENCY = 1f;
	private static final float BLINK_DURATION = 0.4f;
	
	private final ShipAPI ship;
	
	private ArrayDeque<DamagingProjectileAPI> projs;
	
    public espc_RemMortarVFX(ShipAPI ship) {
    	this.ship = ship;
		projs = new ArrayDeque<DamagingProjectileAPI>();
    }
	
	public void addProj(DamagingProjectileAPI proj) {
		projs.add(proj);
	}
	
    @Override
    public void advance(float amount, List<InputEventAPI> events) {
    	if (amount == 0f)
    		return;
		CombatEngineAPI engine = Global.getCombatEngine();
		if (projs.size() == 0) {
			if (ship == null || !engine.isInPlay(ship) || ship.isHulk() || !ship.isAlive()) {
				if (ship != null)
					ship.removeCustomData("espc_RemMortarPlugin");
				engine.removePlugin(this);
			}
			return;
		}
		Iterator<DamagingProjectileAPI> projIter = projs.iterator();
		while (projIter.hasNext()) {
			DamagingProjectileAPI proj = projIter.next();
			if (proj == null || !engine.isInPlay(proj) || proj.isExpired() || proj.isFading())
				projIter.remove();
		}
	}
	
    @Override
    public void renderInWorldCoords(ViewportAPI viewport) {
		if (projs.size() == 0)
			return;
		CombatEngineAPI engine = Global.getCombatEngine();
		float cTime = (engine.getTotalElapsedTime(false) + 0.3f) % BLINK_FREQUENCY;
		if (cTime > BLINK_DURATION)
			return;
		cTime = (BLINK_DURATION - cTime) / BLINK_DURATION;
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
		SpriteAPI sprite = Global.getSettings().getSprite("systemMap", "radar_entity");
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, sprite.getTextureId());
		GL11.glBegin(GL11.GL_QUADS);

		for (DamagingProjectileAPI proj : projs) {
			if (!engine.getViewport().isNearViewport(proj.getLocation(), 500f))
				continue;

			
			GL11.glColor4ub(
				(byte) 255, (byte) 60, (byte) 0, 
				(byte) (150f * cTime));
			
			MezzUtils.glSquare(proj.getLocation(), 50f * cTime);
			
			GL11.glColor4ub(
					(byte) 255, (byte) 255, (byte) 255,
					(byte) (150f * cTime));
			
			MezzUtils.glSquare(proj.getLocation(), 20f * cTime);
		}
		GL11.glEnd();
	}
}
