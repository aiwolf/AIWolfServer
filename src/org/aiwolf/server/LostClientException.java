package org.aiwolf.server;

import org.aiwolf.common.AIWolfRuntimeException;
import org.aiwolf.common.data.Agent;

/**
 * throws when the cliend connection is lost
 * @author tori
 *
 */
public class LostClientException extends AIWolfRuntimeException {

	Agent agent;
	
//	public LostClientException() {
//	}
//
//	public LostClientException(String arg0) {
//		super(arg0);
//	}

	public LostClientException(Throwable arg0, Agent agent) {
		super(arg0);
		this.agent = agent;
	}
	
//	public LostClientException(Throwable arg0) {
//		super(arg0);
//	}

//	public LostClientException(String arg0, Throwable arg1) {
//		super(arg0, arg1);
//	}
//
//	public LostClientException(String arg0, Throwable arg1, boolean arg2,
//			boolean arg3) {
//		super(arg0, arg1, arg2, arg3);
//	}

	public LostClientException(String arg0, Throwable arg1, Agent agent) {
		super(arg0, arg1);
		this.agent = agent;
	}

	/**
	 * @return agent
	 */
	public Agent getAgent() {
		return agent;
	}

	/**
	 * @param agent セットする agent
	 */
	public void setAgent(Agent agent) {
		this.agent = agent;
	}

	
	
}
