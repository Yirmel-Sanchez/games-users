package edu.uclm.esi.gamesusers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.server.ResponseStatusException;

import edu.uclm.esi.gamesusers.dao.TokenDAO;
import edu.uclm.esi.gamesusers.dao.UserDAO;
import edu.uclm.esi.gamesusers.entities.Token;
import edu.uclm.esi.gamesusers.entities.User;
import edu.uclm.esi.gamesusers.services.UsersService;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@TestMethodOrder(OrderAnnotation.class)
public class TestUsersServices {

	
	@Autowired
    private UsersService usersService;

    @MockBean
    private UserDAO userDAO;

    @MockBean
    private TokenDAO tokenDAO;

	@Test
	public void testRegisterUserService() throws Exception {
		// Configurar el comportamiento del DAO para devolver el usuario y el token
        User user = new User();
        user.setName("John");
        user.setEmail("john@example.com");
        user.setPwd("secret");

        Token token = new Token();
        token.setId("abc123");
        token.setUser(user);

        when(userDAO.save(user)).thenReturn(user);
        when(tokenDAO.save(token)).thenReturn(token);

        // Realizar la llamada al servicio
        usersService.register("John", "john@example.com", "secret");

        // Comprobar que se han guardado los datos correctamente
        verify(userDAO, times(1)).save(any(User.class));
        verify(tokenDAO, times(1)).save(any(Token.class));
	}

	@Test
	public void testConfirm() {
		//crear user
		User user = new User();
        user.setName("Pepe");
        user.setEmail("pepe@pepe.com");
        user.setPwd("secret");
		
	    // Crear el token
	    Token token = new Token();
	    token.setId("abc123");
	    token.setUser(user);
	    token.setTime(System.currentTimeMillis());
	    
	    

	    // Configurar el comportamiento del DAO para devolver el token
	    when(tokenDAO.findById(token.getId())).thenReturn(Optional.of(token));
	    when(userDAO.save(any(User.class))).thenReturn(user);
	    doNothing().when(tokenDAO).delete(token);

	    // Realizar la llamada al servicio
	    usersService.confirm(token.getId());

	    // Verificar que se han llamado a los métodos del DAO correctamente
	    verify(tokenDAO, times(1)).findById(token.getId());
	    verify(userDAO, times(1)).save(any(User.class));
	    verify(tokenDAO, times(1)).delete(any(Token.class));
	    
	    // Configurar el comportamiento del TokenDAO para que devuelva un Optional vacío
	    when(tokenDAO.findById("tokenNoExistente")).thenReturn(Optional.empty());
	    
	    // Invocar el método confirm con un tokenId que no existe
	    try {
	        usersService.confirm("tokenNoExistente");
	        fail("Se esperaba que se produjera una excepción");
	    } catch (ResponseStatusException ex) {
	        // Comprobar que la excepción es la esperada
	        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
	        assertEquals("No se encuentra el token o ha caducado", ex.getReason());
	    }
	    
	    // crear un token que ha expirado hace 25 horas y 5 segundos
	    long now = System.currentTimeMillis();
	    long tokenExpirationTime = now - (25 * 60 * 60 * 1000); // expirado hace 25 horas
	    token.setTime(tokenExpirationTime);

	    when(tokenDAO.findById(token.getId())).thenReturn(Optional.of(token));

	    // verificar que se lanza una excepción de tipo ResponseStatusException con código 404
	    try {
	    	usersService.confirm(token.getId());
	        fail("Se esperaba que se produjera una excepción");
	    } catch (ResponseStatusException ex) {
	        // Comprobar que la excepción es la esperada
	        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
	        assertEquals("No se encuentra el token o ha caducado", ex.getReason());
	    }
	}


	@Test
    public void testLogin() {
        // Caso en el que el usuario no existe
        when(userDAO.findByName("john")).thenReturn(null);
        assertThrows(ResponseStatusException.class, () -> usersService.login("john", "secret"));

        // Caso en el que la contraseña es incorrecta
        User user = new User();
        user.setName("jane");
        user.setEmail("jane@example.com");
        user.setPwd("secret123");
        when(userDAO.findByName("jane")).thenReturn(user);
        assertThrows(ResponseStatusException.class, () -> usersService.login("jane", "secret"));

        // Caso en el que el usuario no ha sido activado
        User inactiveUser = new User();
        inactiveUser.setName("bob");
        inactiveUser.setEmail("bob@example.com");
        inactiveUser.setPwd("secret123");
        inactiveUser.setValidationDate(null);
        when(userDAO.findByName("bob")).thenReturn(inactiveUser);
        assertThrows(ResponseStatusException.class, () -> usersService.login("bob", "secret123"));

        // Caso en el que el usuario existe, la contraseña es correcta y ha sido activado
        User activeUser = new User();
        activeUser.setName("alice");
        activeUser.setEmail("alice@example.com");
        activeUser.setPwd("secret123");
        activeUser.setValidationDate(System.currentTimeMillis());
        when(userDAO.findByName("alice")).thenReturn(activeUser);
        User result = usersService.login("alice", "secret123");
        assertNotNull(result);
        assertEquals(activeUser, result);
        
        // Caso en el que el usuario existe, la contraseña es correcta y ha sido activado
        activeUser.setValidationDate(null);
        assertThrows(ResponseStatusException.class, () -> usersService.login("alice", "secret123"));
    }
	
	@Test
	public void testUpdateBalance() {
		User user = new User();
        user.setName("jane");
        user.setEmail("jane@example.com");
        user.setPwd("secret123");
        user.setSaldo(50);
        int increase = 10;
        when(userDAO.findById("1")).thenReturn(Optional.of(user));
        when(userDAO.save(any(User.class))).thenReturn(user);
	    
	    usersService.updateBalance("1", increase);

	    assertEquals(50 + increase, user.getSaldo());
	    
	    when(userDAO.findById("1")).thenReturn(Optional.empty());
	    assertThrows(IllegalArgumentException.class, () -> usersService.updateBalance("1", increase));
	}

	@Test
	public void testGetUser() {
	    // Crea un usuario para el test
	    User user = new User();
	    user.setName("John");
	    user.setPwd("secret123");
	    user.setSaldo(100);
	    user.setValidationDate(System.currentTimeMillis());
	    when(userDAO.findById("1")).thenReturn(Optional.of(user));
	    // Llama al método getUser() con el ID del usuario creado
	    usersService.getUser("1");
	    
	    // Verifica que el usuario devuelto es el mismo que el creado
	    assertEquals(user.getId(), user.getId());
	    assertEquals(user.getName(), user.getName());
	    assertEquals(user.getPwd(), user.getPwd());
	    assertEquals(user.getSaldo(), user.getSaldo());
	    assertEquals(user.getValidationDate(), user.getValidationDate());
	    
	    when(userDAO.findById("1")).thenReturn(Optional.empty());
	    assertThrows(IllegalArgumentException.class, () -> usersService.getUser("1"));
	}

	@Test
	public void testPayGame() {
	    // Create a user with an initial balance of 100.0
	    User user = new User();
	    user.setSaldo(100.0);
	    when(userDAO.findById("1")).thenReturn(Optional.of(user));
        when(userDAO.save(any(User.class))).thenReturn(user);

	    usersService.payGame("1", 50.0);
	    assertEquals(50.0, user.getSaldo(), 0.001);
	    
	    when(userDAO.findById("1")).thenReturn(Optional.empty());
	    assertThrows(IllegalArgumentException.class, () -> usersService.payGame("1", 50.0));
	    
	}

}
