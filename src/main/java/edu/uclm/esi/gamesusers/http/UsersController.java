package edu.uclm.esi.gamesusers.http;

import java.text.DecimalFormat;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import com.github.openjson.JSONObject;

import edu.uclm.esi.gamesusers.entities.User;
import edu.uclm.esi.gamesusers.services.UsersService;
import edu.uclm.esi.gamesusers.services.ValidatorData;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("users")
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true") //Esto se pone al ser un método POST y PUT
public class UsersController {
	@Autowired
	private UsersService usersService;
	
	@Autowired
	private ValidatorData validatorData;
	
	@PostMapping("/register")
	public ResponseEntity<String> register(@RequestBody Map<String, Object> info) {
		String name = info.get("name").toString();
		String email = info.get("email").toString();
		String pwd1 = info.get("pwd1").toString();
		String pwd2 = info.get("pwd2").toString();
		
		if (!(validatorData.patternMatches(email, "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$"))) {
			throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "El correo no tiene el formato requerido");
		}
		
		if (!pwd1.equals(pwd2)) {
			throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Las contraseñas no coinciden");
		}
		
		try {
			this.usersService.register(name, email, pwd1);
		} catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Cosas");
		}
		
		JSONObject response = new JSONObject();
	    response.put("message", "Registro exitoso");
		return new ResponseEntity<>(response.toString(), HttpStatus.OK);
		
	}
	
	@GetMapping("/confirm/{tokenId}")
	public void confirm(HttpServletResponse response, @PathVariable String tokenId) {
		try {
			this.usersService.confirm(tokenId);
			response.sendRedirect("http://localhost:4200/");
		} catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
		}
	}
	
	@PutMapping("/login")
	public ResponseEntity<String> login(HttpSession session, @RequestBody Map<String, Object> info) {
		String name = info.get("name").toString();
		String pwd = info.get("pwd").toString();
		User user;
		try {
			user = this.usersService.login(name, pwd);
			session.setAttribute("userId", user.getId());
		} catch (Exception e) {
			throw e;
		}
		
		JSONObject response = new JSONObject();
	    response.put("message", "Login exitoso");
	    response.put("userId", user.getId());
	    response.put("userName", user.getName());
	    response.put("userBalance", user.getSaldo());
		return new ResponseEntity<>(response.toString(), HttpStatus.OK);
		
	}
	
	@GetMapping("/balance/{userId}")
	public ResponseEntity<String> getBalance(HttpSession session,  @PathVariable String userId) {
		User user;
		try {
			user = this.usersService.getUser(userId);
			
		} catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "No se ha podido consultar el saldo");
		}
		
		DecimalFormat df = new DecimalFormat("#.##");
		//String saldo = df.format(user.getSaldo());
		
		JSONObject response = new JSONObject();
	    response.put("message", "consulta exitosa");
	    response.put("userBalance", user.getSaldo());
	    response.put("userName", user.getName());
		return new ResponseEntity<>(response.toString(), HttpStatus.OK);
	}
	
	@PostMapping("/payGame")
	public ResponseEntity<String> restarSaldo(@RequestBody Map<String, Object> requestBody) {
	    String idPlayer = (String) requestBody.get("idPlayer");
	    double amount = Double.parseDouble(requestBody.get("amount").toString());
	    

	    // Obtener el usuario con el idPlayer y restar el saldo
	    try {
			this.usersService.payGame(idPlayer, amount);
		} catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "No se ha podido restar el saldo");
		}

	    // Devolver la respuesta JSON con la operación realizada con éxito
	    JSONObject responseBody = new JSONObject();
	    responseBody.put("success", true);
	    return ResponseEntity.ok(responseBody.toString());
	}

}
