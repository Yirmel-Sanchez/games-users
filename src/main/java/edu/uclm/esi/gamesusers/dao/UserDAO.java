package edu.uclm.esi.gamesusers.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.uclm.esi.gamesusers.entities.User;

public interface UserDAO extends JpaRepository<User, String> {

	User findByEmail(String name);

	User findByName(String name);
	
	Optional<User> findById(String id);
	
	@Modifying
	@Query( value="delete from users where id=:id", nativeQuery = true)
	void deleteByUserId(@Param("id") String id);

}
