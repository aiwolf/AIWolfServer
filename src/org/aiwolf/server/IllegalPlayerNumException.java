package org.aiwolf.server;

import org.aiwolf.common.AIWolfRuntimeException;

public class IllegalPlayerNumException extends AIWolfRuntimeException {

	public IllegalPlayerNumException() {
		super();
	}

	public IllegalPlayerNumException(String arg0, Throwable arg1, boolean arg2,
			boolean arg3) {
		super(arg0, arg1, arg2, arg3);
	}

	public IllegalPlayerNumException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public IllegalPlayerNumException(String arg0) {
		super(arg0);
	}

	public IllegalPlayerNumException(Throwable arg0) {
		super(arg0);
	}

}
