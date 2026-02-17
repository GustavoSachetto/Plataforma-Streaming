package com.sachetto.streaming.exception;

public class ChecksumException extends RuntimeException {

	private static final long serialVersionUID = -8062160901743947268L;

	public ChecksumException() {
		super("Falha ao validar checksum do arquivo");
	}
}
