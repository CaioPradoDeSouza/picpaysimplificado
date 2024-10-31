package com.picpaysimplificado.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.picpaysimplificado.domain.transaction.Transaction;
import com.picpaysimplificado.domain.user.User;
import com.picpaysimplificado.dtos.TransactionDTO;
import com.picpaysimplificado.repositories.TransactionRepository;

@Service
public class TransactionService {
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private TransactionRepository repository;
	
	@Autowired
	private RestTemplate restTemplate;
	
	
	public void createTransaction(TransactionDTO transaction) {
		User sender = this.userService.findUserById(transaction.senderId());
		User receiver = this.userService.findUserById(transaction.receiverId());
	
		userService.validateTransaction(sender ,transaction.value());
		
		boolean isAuthorized = this.authorizeTransaction(sender, transaction.value());
		
		if(!isAuthorized) {
			throw new Exception("transação não autorizada");
		}
		
		Transaction newtransaction = new Transaction();
		newtransaction.setAmount(transaction.value());
		newtransaction.setSender(sender);
		newtransaction.setReceiver(receiver);
		newtransaction.setLocalDateTime(LocalDateTime.now());
		
		sender.setBalance(sender.getBalance().subtract(transaction.value()));
		receiver.setBalance(receiver.getBalance().add(transaction.value()));
		
		this.repository.save(newtransaction);
		this.userService.saveUser(sender);
		this.userService.saveUser(receiver);
	}
	
	public boolean authorizeTransaction(User sender, BigDecimal value) {
		
		ResponseEntity<Map> authorizationResponse = restTemplate.getForEntity("https://util.devi.tools/api/v2/authorize", Map.class);
	
				if(authorizationResponse.getStatusCode() == HttpStatus.OK) {
					String message = (String) authorizationResponse.getBody().get("message"); 
					return "Autorizado" .equalsIgnoreCase(message);
				}else return false;
	}
}
