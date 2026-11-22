package com.overloadsync;

import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;

/** A short, soft two-note ding built in memory, so there are no sound files. */
@Singleton
class PerfectSound
{
	private static final float SAMPLE_RATE = 44100f;
	private static final byte[] PCM = build();

	private final ScheduledExecutorService executor;

	@Inject
	PerfectSound(ScheduledExecutorService executor)
	{
		this.executor = executor;
	}

	void ding(int volumePercent)
	{
		if (volumePercent <= 0)
		{
			return;
		}
		executor.submit(() ->
		{
			try
			{
				Clip clip = AudioSystem.getClip();
				clip.addLineListener(event ->
				{
					if (event.getType() == LineEvent.Type.STOP)
					{
						clip.close();
					}
				});
				clip.open(new AudioFormat(SAMPLE_RATE, 16, 1, true, false), PCM, 0, PCM.length);
				if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN))
				{
					FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
					float db = (float) (20.0 * Math.log10(Math.max(1, Math.min(100, volumePercent)) / 100.0));
					gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), db)));
				}
				clip.start();
			}
			catch (Exception | LinkageError ignored)
			{
				// No audio device available: the chat message still shows.
			}
		});
	}

	void close()
	{
		// Clips close themselves when they finish.
	}

	/** Two notes (A5 then E6), each a quick decaying sine. */
	private static byte[] build()
	{
		int[] freqs = {880, 1319};
		int noteSamples = (int) (SAMPLE_RATE * 0.14);
		int step = (int) (SAMPLE_RATE * 0.07);
		int total = step + noteSamples;
		double[] mix = new double[total];
		for (int n = 0; n < freqs.length; n++)
		{
			int start = n * step;
			for (int i = 0; i < noteSamples; i++)
			{
				double attack = Math.min(1.0, i / (SAMPLE_RATE * 0.004));
				double decay = Math.exp(-i / (SAMPLE_RATE * 0.045));
				mix[start + i] += Math.sin(2 * Math.PI * freqs[n] * i / SAMPLE_RATE) * attack * decay * 0.5;
			}
		}
		byte[] pcm = new byte[total * 2];
		for (int i = 0; i < total; i++)
		{
			short v = (short) (Math.max(-1, Math.min(1, mix[i])) * Short.MAX_VALUE);
			pcm[2 * i] = (byte) (v & 0xff);
			pcm[2 * i + 1] = (byte) ((v >> 8) & 0xff);
		}
		return pcm;
	}
}
