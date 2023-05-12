package edu.uclm.esi.gamesusers.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.uclm.esi.gamesusers.entities.Token;
import edu.uclm.esi.gamesusers.entities.User;

public interface TokenDAO extends JpaRepository<Token, String>{

	@Modifying
	@Query( value="delete from token where user_id=:id", nativeQuery = true)
	void deleteByUserId(@Param("id") String id);
	
}
