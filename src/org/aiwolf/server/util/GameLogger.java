package org.aiwolf.server.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class GameLogger {

	File logFile;
	BufferedWriter bw;
	public GameLogger(File logFile) throws IOException {
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
