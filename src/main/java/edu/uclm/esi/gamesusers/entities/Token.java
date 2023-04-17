package edu.uclm.esi.gamesusers.entities;

import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity

public class Token {
	
	@Id
	private String id;
	
	private long time;
	
	@ManyToOne //(cascade = CascadeType.ALL) No hacer caso pero tenerlo ahí por si acaso hace falta
	private User user;
	
	public Token() {
		this.id = UUID.randomUUID().toString();
		this.time = System.currentTimeMillis();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}
	
}
