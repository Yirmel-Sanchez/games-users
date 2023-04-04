package edu.uclm.esi.gamesusers.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.uclm.esi.gamesusers.entities.User;

public interface UserDAO extends JpaRepository<User, String> {

	User findByEmail(String name);
	User findByName(String name);

}
