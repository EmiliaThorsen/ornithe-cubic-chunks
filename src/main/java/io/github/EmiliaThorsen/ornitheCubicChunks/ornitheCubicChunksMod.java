package io.github.EmiliaThorsen.ornitheCubicChunks;

import it.unimi.dsi.fastutil.bytes.ByteArrayFIFOQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ornitheCubicChunksMod {

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod name as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LogManager.getLogger("ornithe cubic chunks");

	public static class loadReturns {
		public byte[] outputs;
		public ByteArrayFIFOQueue inputs;

	}
}
