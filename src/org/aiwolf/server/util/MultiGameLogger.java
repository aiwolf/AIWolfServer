package org.aiwolf.server.util;

import java.util.HashSet;
import java.util.Set;

import org.aiwolf.common.net.GameInfo;


/**
 * 
 * @author tori
 *
 */
public class MultiGameLogger implements GameLogger {

	protected Set<GameLogger> gameLoggerSet;

	public MultiGameLogger(){
		gameLoggerSet = new HashSet<GameLogger>();
	}

	public MultiGameLogger(GameLogger... loggers){
		gameLoggerSet = new HashSet<GameLogger>();
		for(GameLogger gl:loggers){
			gameLoggerSet.add(gl);
		}
	}

	
	public void add(GameLogger gameLogger){
		gameLoggerSet.add(gameLogger);
	}

	public void remove(GameLogger gameLogger){
		gameLoggerSet.remove(gameLogger);
	}

	
	@Override
	public void log(String log) {
		for(GameLogger gl:gameLoggerSet){
			gl.log(log);
		}

	}

	@Override
	public void flush() {
		for(GameLogger gl:gameLoggerSet){
			gl.flush();
		}
	}

	@Override
	public void close() {
		for(GameLogger gl:gameLoggerSet){
			gl.close();
		}
	}

}
