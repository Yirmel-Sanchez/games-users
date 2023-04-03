package edu.uclm.esi.gamesusers.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.uclm.esi.gamesusers.entities.Token;

public interface TokenDAO extends JpaRepository<Token, String>{

}
