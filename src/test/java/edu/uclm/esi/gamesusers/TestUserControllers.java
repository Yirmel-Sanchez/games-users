package edu.uclm.esi.gamesusers;

import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.OngoingStubbing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentIntentUpdateParams;

import edu.uclm.esi.gamesusers.dao.TokenDAO;
import edu.uclm.esi.gamesusers.dao.UserDAO;
import edu.uclm.esi.gamesusers.entities.Token;
import edu.uclm.esi.gamesusers.entities.User;
import edu.uclm.esi.gamesusers.services.UsersService;
import jakarta.servlet.http.HttpSession;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@TestMethodOrder(OrderAnnotation.class)
public class TestUserControllers {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private UsersService usersService;
	
	@Value("${stripe.apiKey}")
	private String apiKey;

	@Test
	void testRegister() throws Exception {
		JSONObject jsonRequest = new JSONObject().put("name", "Pepe").put("email", "pepe@pepe.com")
				.put("pwd1", "pepe1234").put("pwd2", "pepe1234");

		// Configurar el comportamiento del servicio
		doAnswer(invocation -> {
			return true;
		}).when(usersService).register(anyString(), anyString(), anyString());

		// Realizar la petición
		MvcResult result = mockMvc
				.perform(
						post("/users/register").contentType(MediaType.APPLICATION_JSON).content(jsonRequest.toString()))
				.andExpect(status().isOk()).andReturn();

		JSONObject jsonResponse = new JSONObject(result.getResponse().getContentAsString());

		// Comprobar la respuesta
		assertEquals("Registro exitoso", jsonResponse.getString("message"));
		verify(usersService, times(1)).register(anyString(), anyString(), anyString());

		// Realizar otra petición para comprobar el error de correo inválido
		jsonRequest.put("email", "pepepepe.com");
		result = mockMvc
				.perform(
						post("/users/register").contentType(MediaType.APPLICATION_JSON).content(jsonRequest.toString()))
				.andExpect(status().isNotAcceptable()).andReturn();

		// Comprobar la respuesta
		int statusCode = result.getResponse().getStatus();
		assertEquals(HttpStatus.NOT_ACCEPTABLE.value(), statusCode);

		// Realizar otra petición para comprobar el error de contraseñas no coincidentes
		jsonRequest.put("email", "pepe@pepe.com");
		jsonRequest.put("pwd2", "pepe4321");
		result = mockMvc
				.perform(
						post("/users/register").contentType(MediaType.APPLICATION_JSON).content(jsonRequest.toString()))
				.andExpect(status().isNotAcceptable()).andReturn();

		// Comprobar la respuesta
		statusCode = result.getResponse().getStatus();
		assertEquals(HttpStatus.NOT_ACCEPTABLE.value(), statusCode);

		// Configurar el comportamiento del servicio para lanzar una excepción
		doAnswer(invocation -> {
			throw new Exception("Cosas");
		}).when(usersService).register(anyString(), anyString(), anyString());

		// Realizar otra petición para comprobar el error de excepción en el servicio
		jsonRequest.put("pwd2", "pepe1234");
		result = mockMvc
				.perform(
						post("/users/register").contentType(MediaType.APPLICATION_JSON).content(jsonRequest.toString()))
				.andExpect(status().isConflict()).andReturn();

		// Comprobar la respuesta
		statusCode = result.getResponse().getStatus();
		assertEquals(HttpStatus.CONFLICT.value(), statusCode);
	}

	@Test
	void testConfirm() throws Exception {
		// Configurar el comportamiento del servicio
		doNothing().when(usersService).confirm(anyString());

		// Realizar la petición
		mockMvc.perform(get("/users/confirm/{tokenId}", "token123")).andExpect(status().isFound())
				.andExpect(redirectedUrl("http://localhost:4200/")).andReturn();

		// Comprobar que se llama al método del servicio
		verify(usersService, times(1)).confirm("token123");

		// Configurar el comportamiento del servicio para lanzar una excepción
		doAnswer(invocation -> {
			throw new Exception("Error");
		}).when(usersService).confirm(anyString());

		// Realizar la petición GET a /confirm/{tokenId}
		String tokenId = "abc123";
		MvcResult result = mockMvc.perform(get("/users/confirm/{tokenId}", tokenId)).andExpect(status().isConflict())
				.andReturn();
		int statusCode = result.getResponse().getStatus();
		assertEquals(HttpStatus.CONFLICT.value(), statusCode);

	}

	@Test
	public void testLogin() throws Exception {
		// Configurar el comportamiento del servicio para devolver un usuario
		User user = new User();
		user.setId("1");
		user.setName("Pepe");
		user.setEmail("pepe@pepe.com");
		user.setPwd("pepe1234");
		user.setSaldo(1000);

		doAnswer(invocation -> {
			return user;
		}).when(usersService).login("Pepe", "pepe1234");

		// Realizar la petición
		JSONObject info = new JSONObject().put("name", "Pepe").put("pwd", "pepe1234");

		MvcResult result = mockMvc
				.perform(put("/users/login").contentType(MediaType.APPLICATION_JSON).content(info.toString()))
				.andExpect(status().isOk()).andReturn();

		// Comprobar la respuesta
		JSONObject jsonResponse = new JSONObject(result.getResponse().getContentAsString());
		assertEquals("Login exitoso", jsonResponse.getString("message"));
		assertEquals(user.getId(), jsonResponse.getString("userId"));
		assertEquals(user.getName(), jsonResponse.getString("userName"));
		assertEquals(user.getSaldo(), jsonResponse.getInt("userBalance"));

		// Comprobar que se ha guardado el usuario en la sesión
		HttpSession session = result.getRequest().getSession();
		assertEquals(user.getId(), session.getAttribute("userId"));
	}

	@Test
	public void testGetBalance() throws Exception {
		// Configurar el comportamiento del servicio para devolver un usuario
		User user = new User();
		user.setId("1");
		user.setName("Pepe");
		user.setEmail("pepe@pepe.com");
		user.setPwd("pepe1234");
		user.setSaldo(1000);

		doAnswer(invocation -> {
			return user;
		}).when(usersService).getUser("1");

		// Crear sesión simulada
		MockHttpSession session = new MockHttpSession();
		session.setAttribute("userId", user.getId());

		MvcResult result = mockMvc.perform(get("/users/balance/1").session(session)).andExpect(status().isOk())
				.andReturn();

		// Comprobar la respuesta
		JSONObject jsonResponse = new JSONObject(result.getResponse().getContentAsString());
		assertEquals("consulta exitosa", jsonResponse.getString("message"));
		assertEquals(user.getName(), jsonResponse.getString("userName"));
		assertEquals(user.getSaldo(), jsonResponse.getDouble("userBalance"), 0.001);
	}
	
	@Test
	public void testRestarSaldo() throws Exception {
	    // Configurar el comportamiento del servicio para devolver el usuario creado
	    doAnswer(invocation -> {
			return true;
		}).when(usersService).payGame("1", 500);

	    // Realizar la petición
	    JSONObject requestBody = new JSONObject()
	            .put("idPlayer", "1")
	            .put("amount", 500.0);
	    
	    MvcResult result = mockMvc.perform(post("/users/payGame")
	            .contentType(MediaType.APPLICATION_JSON)
	            .content(requestBody.toString()))
	            .andExpect(status().isOk())
	            .andReturn();

	    // Comprobar la respuesta
	    JSONObject jsonResponse = new JSONObject(result.getResponse().getContentAsString());
	    assertTrue(jsonResponse.getBoolean("success"));
	    
	    int statusCode = result.getResponse().getStatus();
		assertEquals(HttpStatus.OK.value(), statusCode);

	    // Comprobar que se llama al método del servicio
	 	verify(usersService, times(1)).payGame("1", 500);
	 	
	 	// Configurar el comportamiento del servicio para lanzar una excepción
	 	doAnswer(invocation -> { 
	 		throw new Exception("Error");
	 	}).when(usersService).payGame("1", 500);
	 	 
	 	// Realizar otra petición para comprobar el error de excepción en el servicio
	 	requestBody.put("idPlayer", "1"); 
	 	requestBody.put("amount", 500.0); 
	 	result = mockMvc.perform(post("/users/payGame")
	 			.contentType(MediaType.APPLICATION_JSON)
	 			.content(requestBody.toString()))
	 			.andExpect(status().isConflict())
	 			.andReturn();
	 	 
	 	// Comprobar la respuesta 
	 	statusCode = result.getResponse().getStatus();
	 	assertEquals(HttpStatus.CONFLICT.value(), statusCode);
	}
	
	@Test
	public void testPrepay() throws Exception {
		
		String clientSecret = "client_secret_1234";
		Stripe.apiKey = apiKey;
		
		long amount = 1000;
	    String currency = "usd";
	    
	    PaymentIntent paymentIntent = Mockito.mock(PaymentIntent.class);
	    when(paymentIntent.getClientSecret()).thenReturn(clientSecret);
	    when(paymentIntent.getAmount()).thenReturn(amount);
	    when(paymentIntent.getCurrency()).thenReturn(currency);

	    //when(PaymentIntent.create(createParams)).thenReturn(paymentIntent);
	    
	    // Realizar la petición
	    int cant = 10;
	    MvcResult result = mockMvc.perform(get("/payments/prepay")
	            .sessionAttr("userId", "1")
	            .param("cant", String.valueOf(cant)))
	            .andExpect(status().isOk())
	            .andReturn();

	    // Comprobar la respuesta
	    int statusCode = result.getResponse().getStatus();
	 	assertEquals(HttpStatus.OK.value(), statusCode);
	 	
	 	// Realizar la petición cuando no se ha logeado
	    result = mockMvc.perform(get("/payments/prepay")
	    		.sessionAttr("key", "key")
	            .param("cant", String.valueOf(cant)))
	            .andExpect(status().isBadRequest())
	            .andReturn();

	    // Comprobar la respuesta
	    statusCode = result.getResponse().getStatus();
	 	assertEquals(HttpStatus.BAD_REQUEST.value(), statusCode);
	 	assertTrue(result.getResponse().getErrorMessage().contains("Debes iniciar sesión para recargar el saldo"));
	 	
	 // Realizar la petición con una cantidad incorrecta
	 	cant = 4;
	    result = mockMvc.perform(get("/payments/prepay")
	    		.sessionAttr("userId", "1")
	            .param("cant", String.valueOf(cant)))
	            .andExpect(status().isBadRequest())
	            .andReturn();

	    // Comprobar la respuesta
	    statusCode = result.getResponse().getStatus();
	 	assertEquals(HttpStatus.BAD_REQUEST.value(), statusCode);
	 	assertTrue(result.getResponse().getErrorMessage().contains("Cantidad solicitada incorrecta"));
	 	
	 	
	}

	@Test
	public void testConfirmPay() throws Exception {
		Stripe.apiKey = apiKey;
	    // Mockear la respuesta de la API de Stripe
	    PaymentIntent paymentIntent = Mockito.mock(PaymentIntent.class);
	    when(paymentIntent.getId()).thenReturn("pi_1234");
	    when(paymentIntent.getStatus()).thenReturn("succeeded");
	    
	    doAnswer(invocation -> {
			return true;
		}).when(usersService).updateBalance("1", 1000);
	    
	 // Realizar la petición
	    MvcResult result = mockMvc.perform(get("/payments/confirm")
	    		.sessionAttr("client_secret", "client_secret_1234")
	    		.sessionAttr("userId", "1")
	    		.sessionAttr("payment_intent_id", "pi_1234"))
	            .andExpect(status().isInternalServerError())
	            .andReturn();

	    // Comprobar la respuesta
	    int statusCode = result.getResponse().getStatus();
	    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), statusCode);
	}
}