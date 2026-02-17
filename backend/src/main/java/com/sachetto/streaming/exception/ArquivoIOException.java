package com.sachetto.streaming.exception;

public class ArquivoIOException extends RuntimeException {

	private static final long serialVersionUID = 4525087246851207240L;

	public ArquivoIOException() {
		super("Falha na entrada ou saida de manipulação do arquivo");
	}
}
