package com.tfg.example.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tfg.example.service.PractitionerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/practitioner")
@Tag(name = "Practitioner", description = "Endpoints relacionados con los Profesionales")
public class PractitionerController {

	private final PractitionerService service;

	public PractitionerController(PractitionerService service) {
		this.service = service;
	}
	
	@GetMapping("/init")
	@Operation(summary = "Inicia una instancia del proceso y devuelve su ID")
	public Long initProcess() {
		return this.service.newProcessInstance();
	}
	
	@GetMapping("/process/{id}")
	@Operation(summary = "Inicia la tarea pasando como parámetro del identificador de la instancia del proceso")
	public void initTask(@PathVariable("id") String id) {
		this.service.initTask(id);
	}
}
