package org.aiwolf.server.util;

import org.aiwolf.common.net.GameInfo;

public interface GameLogger {
	public void log(String log);
	public void flush();
	public void close();
}
