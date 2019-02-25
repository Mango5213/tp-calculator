package com.demo.tpcalculator.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yue.su
 * @date 2019年2月25日
 */
@ControllerAdvice(annotations = { RestController.class })
@Slf4j
public class ControllerAdvise {

	@ExceptionHandler(RuntimeException.class)
	public ResponseEntity<String> handleException(RuntimeException ex) {
		log.error(ex.getMessage(), ex);
		HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
		return new ResponseEntity<>(ex.getMessage(), status);
	}

}
