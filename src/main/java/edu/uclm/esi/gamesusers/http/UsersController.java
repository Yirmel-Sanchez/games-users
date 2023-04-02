package edu.uclm.esi.gamesusers.http;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import edu.uclm.esi.gamesusers.services.UsersService;
import edu.uclm.esi.gamesusers.services.ValidatorData;

@RestController
@RequestMapping("users")
@CrossOrigin("*") //Esto se pone al ser un método POST y PUT
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
			throw new ResponseStatusException(HttpStatus.CONFLICT);
		}
		
		return new ResponseEntity<>("Registro exitoso", HttpStatus.OK);
		
	}
	@PutMapping("/login")
	public ResponseEntity<String> login(@RequestBody Map<String, Object> info) {
		String name = info.get("name").toString();
		String pwd = info.get("pwd").toString();
		
		try {
			this.usersService.login(name, pwd);
		} catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}
		
		return new ResponseEntity<>("Login exitoso", HttpStatus.OK);
		
	}
}
