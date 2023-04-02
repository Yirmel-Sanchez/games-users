package edu.uclm.esi.gamesusers.services;

import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

@Service
public class ValidatorData {
	
	public boolean patternMatches(String emailAddress, String regexPattern) {
		return Pattern.compile(regexPattern)
	      .matcher(emailAddress)
	      .matches();
	}
	

}
