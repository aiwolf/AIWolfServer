package org.aiwolf.server;

import org.aiwolf.common.AIWolfRuntimeException;

/**
 * throws when the cliend connection is lost
 * @author tori
 *
 */
public class LostClientException extends AIWolfRuntimeException {

	public LostClientException() {
	}

	public LostClientException(String arg0) {
		super(arg0);
	}

	public LostClientException(Throwable arg0) {
		super(arg0);
	}

	public LostClientException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public LostClientException(String arg0, Throwable arg1, boolean arg2,
			boolean arg3) {
		super(arg0, arg1, arg2, arg3);
	}

}
