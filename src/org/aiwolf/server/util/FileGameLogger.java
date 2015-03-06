package org.aiwolf.server.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;

import org.aiwolf.common.net.GameInfo;

/**
 * GameLogger using File
 * @author tori
 *
 */
public class FileGameLogger implements GameLogger {

	File logFile;
	BufferedWriter bw;
	public FileGameLogger(File logFile) throws IOException {
		super();
		this.logFile = logFile;

		if(logFile != null){
			logFile.getParentFile().mkdirs();
			bw = new BufferedWriter(new FileWriter(logFile));
		}
		else{
			bw = new BufferedWriter(new OutputStreamWriter(System.out));
		}
	}

	/**
	 * Save log
	 * @param text
	 * @param gameInfo
	 */
	public void log(String text){
		try{
			bw.append(text);
			bw.append("\n");
		}catch(IOException e){
		}
//		System.out.println(text);
	}
	
	public void flush(){
		try {
			bw.flush();
		} catch (IOException e) {
		}
	}
	
	public void close(){
		try {
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
