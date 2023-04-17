package edu.uclm.esi.gamesusers.services;

import java.io.IOException;
import java.util.Optional;

import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import edu.uclm.esi.gamesusers.entities.Token;
import edu.uclm.esi.gamesusers.entities.User;
import edu.uclm.esi.gamesusers.servicesAux.HttpClient;
import edu.uclm.esi.gamesusers.servicesAux.Manager;
import edu.uclm.esi.gamesusers.dao.TokenDAO;
import edu.uclm.esi.gamesusers.dao.UserDAO;

@Service
public class UsersService {
	@Autowired
	private UserDAO userDAO;
	
	@Autowired
	private TokenDAO tokenDAO;
	
	@Transactional
	public void register(String name, String email, String pwd) throws IOException {
		User user = new User();
		user.setName(name);
		user.setEmail(email);
		user.setPwd(pwd);
		
		Token token = new Token();
		token.setUser(user);
		System.out.println("Antes de guardar");
		//Caso de uso de volvernos a registrar con un nombre o un correo que ya existe
		// pero la cuenta está aún sin confirmar
		if (this.userDAO.findByEmail(email)!= null)  {
			User userFoundByEmail = this.userDAO.findByEmail(email);
			this.tokenDAO.deleteByUserId(userFoundByEmail.getId());
			if(userFoundByEmail.getValidationDate()==null) {
				this.userDAO.deleteByUserId(userFoundByEmail.getId());
			}
		}
		if (this.userDAO.findByName(name)!= null)  {
			User userFoundByName = this.userDAO.findByName(name);
			this.tokenDAO.deleteByUserId(userFoundByName.getId());
			if(userFoundByName.getValidationDate()==null) {
				this.userDAO.deleteByUserId(userFoundByName.getId());
			}
		}
		this.userDAO.save(user);
		this.tokenDAO.save(token);
		System.out.println("despues de guardar");
		
		try {
			this.sendEmail(user, token);
		} catch(Exception e) {
			e.printStackTrace();
			throw e;
		}
		System.out.println("despues del email");
		
	}
	

	private void sendEmail(User user, Token token) throws IOException {
		
		String body = Manager.get().readFile("welcome.html.txt");
		body = body.replace("#TOKEN#", token.getId());
		JSONObject emailParameters =
				new JSONObject(Manager.get().readFile("sendinblue.parameters.txt"));
		
		String endPoint = emailParameters.getString("endpoint");
		JSONArray headers = emailParameters.getJSONArray("headers");
		JSONObject payload = new JSONObject();
		
		payload.put("sender", emailParameters.getJSONObject("sender"));
		JSONArray to = new JSONArray();
		to.put(new JSONObject().put("email", user.getEmail()).put("name", user.getName()));
		
		payload.put("to", to);
		payload.put("subject", emailParameters.getString("subject"));
		payload.put("htmlContent", body);
		
		HttpClient client = new HttpClient();
		
		client.sendPost(endPoint, headers, payload);
	}
	
	public void confirm(String tokenId) {
		
		Optional<Token> optToken = this.tokenDAO.findById(tokenId); //No se si es esta bilbio para el Optional
		
		if (!optToken.isPresent())
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encuentra el token o ha caducado");
		
		Token token = optToken.get();
		long time = System.currentTimeMillis();
		
		if (time-token.getTime()>24*60*60*1000)
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encuentra el token o ha caducado");
		
		User user = token.getUser();
		user.setValidationDate(time);
		this.userDAO.save(user);
		this.tokenDAO.delete(token);
		
	}

	public User login(String name, String pwd) {
		
		User user = this.userDAO.findByName(name);
		
		if (user==null )
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Credenciales inválidas");
		
		String pwdEncripted = org.apache.commons.codec.digest.DigestUtils.sha512Hex(pwd);
		if (!user.getPwd().equals(pwdEncripted))
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Credenciales inválidas");
		
		if (user.getValidationDate()==null)
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuario sin activar");
		
		return user;
		
	}


	public void updateBalance(String userId, int increase) {
		Optional<User> userOptional = this.userDAO.findById(userId);
		if (userOptional.isPresent()) {
	        User user = userOptional.get();
	        user.setSaldo(user.getSaldo() + increase); // Update balance with the provided attribute
	        this.userDAO.save(user); // Save the updated user entity to the database
	    } else {
	        throw new IllegalArgumentException("No se ha encontrado el usuario con id: " + userId);
	    }
		
	}


	public User getUser(String userId) {
		Optional<User> userOptional = this.userDAO.findById(userId);
		if (userOptional.isPresent()) {
	        User user = userOptional.get();
	        return user;
	    } else {
	        throw new IllegalArgumentException("No se ha encontrado el usuario con id: " + userId);
	    }
	}


	public void payGame(String idPlayer, double amount) {
		Optional<User> userOptional = this.userDAO.findById(idPlayer);
		if (userOptional.isPresent()) {
	        User user = userOptional.get();
	        user.setSaldo(user.getSaldo() - amount); // Update balance with the provided attribute
	        this.userDAO.save(user); // Save the updated user entity to the database
	    } else {
	        throw new IllegalArgumentException("No se ha encontrado el usuario con id: " + idPlayer);
	    }
	}

}
