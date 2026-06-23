package data.scripts.util;


import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.WaveDistortion;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.util.Misc;

// for distortion instances that need per-frame control
public class espc_DistWrapper {
	
	private WaveDistortion wave;
	private CombatEntityAPI entity = null;
	
	public espc_DistWrapper (boolean flipped, 
	    Vector2f loc, Vector2f vel, float intensity, float size, float dur, float in, float out, int inMode, int outMode)
    {
       wave = new WaveDistortion(loc, vel == null ? Misc.ZERO : vel);
        wave.setIntensity(intensity);
        wave.setSize(size);
        wave.setLifetime(dur);
        if (flipped)
        	wave.flip(flipped);
        if (inMode >= 0) {
        	if (in > 0f) {
            	if (inMode == 0 || inMode == 2)
            		wave.fadeInSize(in);
            	else if (inMode == 1 || inMode == 2)
            		wave.fadeInIntensity(in);
        	}
        }
        if (outMode >= 0) {
            if (outMode == 0 || outMode == 2)
            	wave.fadeOutSize(out);
            else if (outMode == 1 || outMode == 2)
            	wave.fadeOutIntensity(out);
        }
        DistortionShader.addDistortion(wave);
    }
	
	public void setEntity(CombatEntityAPI entity) {
		this.entity = entity;
	}
	
	public CombatEntityAPI getEntity() {
		return entity;
	}

	public void setLocation(Vector2f loc) {
		wave.setLocation(loc);
	}

	public void setVelocity(Vector2f vel) {
		wave.setVelocity(vel);
	}
	
	public void setSize(float size) {
		wave.setSize(size);
	}
	
	public void setIntensity(float intensity) {
		wave.setIntensity(intensity);
	}
	
	public void setLifetime(float dur) {
		wave.setLifetime(dur);
	}
	
	public void setFade(float dur, int mode) {
		if (mode == 0 || mode == 2)
			wave.fadeOutSize(dur);
		if (mode == 1 || mode == 2)
			wave.fadeOutIntensity(dur);
	}
	
	public void remove() {
		DistortionShader.removeDistortion(wave);
	}
}
