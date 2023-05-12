package edu.uclm.esi.gamesusers.servicesAux;

import java.io.IOException;
import java.io.InputStream;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class Manager {
	
	private Manager() {}
	
	private static class AManagerHolder {
		
		static Manager singleton=new Manager();
	}
	
	@Bean
	public static Manager get() {
		
		return AManagerHolder.singleton;
	}
	
	public String readFile(String fileName) throws IOException {
		
		ClassLoader classLoader = getClass().getClassLoader();
		
		try (InputStream fis = classLoader.getResourceAsStream(fileName)) {
			byte[] b = new byte[fis.available()];
			fis.read(b);
			return new String(b);
		}
	}
}