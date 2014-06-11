package pl.edu.agh.jobshop;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Writer;

public class FileWriter {

	private String filename;

	public FileWriter(String filename) {
		this.filename = filename;
		
		File file = new File(filename);
		if (!file.exists())
		{
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else
		{
			file.delete();
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void write(String text) {
		Writer output;
		try {
			output = new BufferedWriter(new java.io.FileWriter(filename, true));
			output.append(text);
			output.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
