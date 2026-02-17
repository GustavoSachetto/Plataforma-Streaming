package com.sachetto.streaming.exception;

public class ComandoFFMpegException extends RuntimeException {

	private static final long serialVersionUID = -3489624839792972146L;

	public ComandoFFMpegException() {
		super("Falha ao processar comando do FFMpeg");
	}
}
