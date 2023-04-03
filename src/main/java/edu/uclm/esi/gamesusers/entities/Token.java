package edu.uclm.esi.gamesusers.entities;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class Token {
	
	@Id
	private String id;
	
	private long time;
	
	@ManyToOne
	private User user;
	
	public Token() {
		this.id = UUID.randomUUID().toString();
		this.time = System.currentTimeMillis();
	}
	
}
