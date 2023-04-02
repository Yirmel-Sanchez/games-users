package edu.uclm.esi.gamesusers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.UnsupportedEncodingException;

import org.json.JSONObject;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import edu.uclm.esi.gamesusers.dao.UserDAO;


@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@TestMethodOrder(OrderAnnotation.class)
public class TestUser {
	@Autowired
	private MockMvc server;
	@Autowired
	private UserDAO userDAO;
	@Test @Order(1)
	void testRegister() throws Exception {
		this.userDAO.deleteAll();
		//Registro exitoso
		ResultActions result = this.sendRequest("Pepe", "pepe@pepe.com", "pepe1234", "pepe1234");
		result.andExpect(status().isOk()).andReturn();
		//Ya existe el usuario
		result = this.sendRequest("Pepe", "pepe@pepe.com", "pepe1234", "pepe1234");
		result.andExpect(status().isConflict()).andReturn();
		//Correo invalido
		result = this.sendRequest("Pepe", "pepepepe.com", "pepe1234", "pepe1234");
		result.andExpect(status().isNotAcceptable()).andReturn();
		//Contraseñas desiguales
		result = this.sendRequest("Ana", "ana@ana.com", "ana123", "ana1234");
		result.andExpect(status().isNotAcceptable()).andReturn();
		
		result = this.sendRequest("Ana", "ana@ana.com", "ana123", "ana1234");
		result.andExpect(status().is(406)).andReturn();
		//Registro exitoso
		result = this.sendRequest("Ana", "ana@ana.com", "ana1234", "ana1234");
		result.andExpect(status().isOk()).andReturn();
	}
	
	
	@Test @Order(2)
	void testLogin() throws Exception {
		ResultActions result = this.sendLogin("Pepe", "pepe1234");
		result.andExpect(status().is(200)).andReturn();
		//No es la contraseña
		result = this.sendLogin("Ana", "******");
		result.andExpect(status().is(403)).andReturn();
		//Nombre no encontrado en la base de datos
		result = this.sendLogin("NoExiste", "ana1234");
		result.andExpect(status().is(403)).andReturn();
		
		result = this.sendLogin("Ana", "ana1234");
		result.andExpect(status().is(200)).andReturn();
	}
	
	private ResultActions sendLogin(String name, String pwd) throws Exception, UnsupportedEncodingException {
		JSONObject jsoUser = new JSONObject()
				.put("name", name)
				.put("pwd", pwd);
		RequestBuilder request = MockMvcRequestBuilders.
				put("/users/login").
				contentType("application/json").
				content(jsoUser.toString());
		
		ResultActions resultActions = this.server.perform(request);
		return resultActions;
	}


	private ResultActions sendRequest(String name, String email, String pwd1, String pwd2) throws Exception, UnsupportedEncodingException {
		JSONObject jsoUser = new JSONObject()
				.put("name", name)
				.put("email", email)
				.put("pwd1", pwd1)
				.put("pwd2", pwd2);
		RequestBuilder request = MockMvcRequestBuilders.
				post("/users/register").
				contentType("application/json").
				content(jsoUser.toString());
		
		ResultActions resultActions = this.server.perform(request);
		return resultActions;
	}
	
	
}

