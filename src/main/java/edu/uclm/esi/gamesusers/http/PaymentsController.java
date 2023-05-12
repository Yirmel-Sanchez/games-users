package edu.uclm.esi.gamesusers.http;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.github.openjson.JSONObject;
import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentIntentUpdateParams;
import com.stripe.param.PaymentMethodCreateParams;
import com.stripe.exception.StripeException;
import com.stripe.param.PaymentIntentConfirmParams;
import com.stripe.param.PaymentMethodCreateParams.CardDetails;

import edu.uclm.esi.gamesusers.services.UsersService;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("payments")
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class PaymentsController {
	
	@Autowired
	private UsersService usersService;

	@Value("${stripe.apiKey}")
	private String apiKey;

	@GetMapping("/prepay")
	public String prepay(HttpSession session, @RequestParam int cant) {
		try {
			if (session.getAttribute("userId") == null)
				throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Debes iniciar sesión para recargar el saldo");
			if (cant != 5 && cant != 10 && cant != 20 && cant != 30 && cant != 40 && cant != 50)
				throw new ResponseStatusException(HttpStatus.CONFLICT, "Cantidad solicitada incorrecta");
			long total = cant;
			total = total * 100;
			Stripe.apiKey = apiKey;
			PaymentIntentCreateParams params = new PaymentIntentCreateParams.Builder().setCurrency("eur")
					.setAmount(total).build();
			PaymentIntent intent = PaymentIntent.create(params);
			JSONObject jso = new JSONObject(intent.toJson());
			String clientSecret = jso.getString("client_secret");
			session.setAttribute("client_secret", clientSecret);
			session.setAttribute("cantidad", cant);
			session.setAttribute("payment_intent_id", intent.getId());
			return clientSecret;
		} catch (Exception e) {
			// e.printStackTrace();
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
		}
	}

	@GetMapping("/confirm")
	public ResponseEntity<String> confirm(HttpSession session) {
		try {
			if (session.getAttribute("client_secret") == null || session.getAttribute("cantidad") == null
				|| session.getAttribute("userId") == null || session.getAttribute("payment_intent_id") == null)
				throw new ResponseStatusException(HttpStatus.FORBIDDEN);
			
			PaymentIntent paymentIntent = PaymentIntent.retrieve(session.getAttribute("payment_intent_id").toString());
			String paymentIntentId = paymentIntent.getId();
			
			paymentIntent = PaymentIntent.retrieve(paymentIntentId);

			if ("succeeded".equals(paymentIntent.getStatus())) {
				String userId = session.getAttribute("userId").toString();
				this.usersService.updateBalance(userId, (Integer) session.getAttribute("cantidad"));
				session.removeAttribute("client_secret");
				session.removeAttribute("cantidad");
			} else {
				throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "El pago no ha sido confirmado");
			}
		}catch(IllegalArgumentException iae) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encuentra el usuario con el id solicitado");
		}catch(Exception e) {
			//e.printStackTrace();
			//throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al confirmar el pago");
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
		}
		
		JSONObject response = new JSONObject();
	    response.put("message", "Actualizacion de saldo exitosa");
		return new ResponseEntity<>(response.toString(), HttpStatus.OK);
	}
}
