package com.overloadsync;

import java.io.ByteArrayInputStream;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.audio.AudioPlayer;

/** A short, soft two-note ding built in memory as a WAV, so there are no sound files. */
@Singleton
class PerfectSound
{
	private static final int SAMPLE_RATE = 44100;
	private static final byte[] WAV = build();

	private final AudioPlayer audioPlayer;
	private final ScheduledExecutorService executor;

	@Inject
	PerfectSound(AudioPlayer audioPlayer, ScheduledExecutorService executor)
	{
		this.audioPlayer = audioPlayer;
		this.executor = executor;
	}

	void ding(int volumePercent)
	{
		if (volumePercent <= 0)
		{
			return;
		}
		// Volume 100 is the full level of the ding (0 dB); lower values are quieter.
		float gain = (float) Math.max(-80.0, 20.0 * Math.log10(Math.min(100, volumePercent) / 100.0));
		executor.submit(() ->
		{
			try
			{
				audioPlayer.play(new ByteArrayInputStream(WAV), gain);
			}
			catch (Exception ignored)
			{
				// No audio device available: the chat message still shows.
			}
		});
	}

	void close()
	{
		// Nothing to release: the audio player closes each sound when it finishes.
	}

	/** Two notes (A5 then E6), each a quick decaying sine, as 16-bit mono PCM in a WAV container. */
	static byte[] build()
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

		int dataBytes = total * 2;
		byte[] wav = new byte[44 + dataBytes];
		putAscii(wav, 0, "RIFF");
		putInt(wav, 4, 36 + dataBytes);
		putAscii(wav, 8, "WAVE");
		putAscii(wav, 12, "fmt ");
		putInt(wav, 16, 16);
		putShort(wav, 20, 1); // PCM
		putShort(wav, 22, 1); // mono
		putInt(wav, 24, SAMPLE_RATE);
		putInt(wav, 28, SAMPLE_RATE * 2);
		putShort(wav, 32, 2);
		putShort(wav, 34, 16);
		putAscii(wav, 36, "data");
		putInt(wav, 40, dataBytes);
		for (int i = 0; i < total; i++)
		{
			putShort(wav, 44 + 2 * i, (short) (Math.max(-1, Math.min(1, mix[i])) * Short.MAX_VALUE));
		}
		return wav;
	}

	private static void putAscii(byte[] b, int at, String s)
	{
		for (int i = 0; i < s.length(); i++)
		{
			b[at + i] = (byte) s.charAt(i);
		}
	}

	private static void putShort(byte[] b, int at, int v)
	{
		b[at] = (byte) (v & 0xff);
		b[at + 1] = (byte) ((v >> 8) & 0xff);
	}

	private static void putInt(byte[] b, int at, int v)
	{
		putShort(b, at, v & 0xffff);
		putShort(b, at + 2, (v >>> 16) & 0xffff);
	}
}
