package com.tfg.example.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tfg.example.service.PatientService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/patient")
@Tag(name = "Patient", description = "Endpoints relacionados con los Pacientes")
public class PatientController {

	private final PatientService service;

	public PatientController(PatientService service) {
		this.service = service;
	}
	
	@GetMapping("/init/{id}") // Id del Questionnaire Response
	@Operation(summary = "Inicia una instancia del proceso y devuelve su ID")
	public Long initProcess(@PathVariable("id") String id) {
		return this.service.newProcessInstance(id);
	}
	
	@GetMapping("/process/{id}")
	@Operation(summary = "Inicia la tarea pasando como parámetro del identificador de la instancia del proceso")
	public void initTask(@PathVariable("id") String id) {
		this.service.initTask(id);
	}
}
