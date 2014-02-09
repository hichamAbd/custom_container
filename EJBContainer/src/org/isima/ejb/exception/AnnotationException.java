package org.isima.ejb.exception;

public class AnnotationException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String message;
	private Throwable cause;
	
	public AnnotationException(String message){
		super(message);
		this.message = message;
	}
	
	public AnnotationException(Throwable cause){
		super(cause);
		this.cause = cause;
	}
	
	public AnnotationException(String message,Throwable cause){
		super(message,cause);
		this.message = message;
		this.cause = cause;
	}

	public String getMessage() {
		return message;
	}

	public Throwable getCause() {
		return cause;
	}
	
	

}
