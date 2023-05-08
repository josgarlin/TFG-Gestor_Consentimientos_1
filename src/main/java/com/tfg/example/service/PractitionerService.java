package com.tfg.example.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.hl7.fhir.r5.model.Questionnaire;
import org.hl7.fhir.r5.model.QuestionnaireResponse;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import com.tfg.example.mapper.FormToResp;
import com.tfg.example.mapper.QuestToForm;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

@Service
public class PractitionerService {
	private final String username = "wbadmin";
	private final String password = "wbadmin";
	
	private final QuestToForm questToForm = new QuestToForm();
	private FormToResp formToResp;
	
	private FhirContext ctx = FhirContext.forR5();
	private IParser parser = ctx.newJsonParser().setPrettyPrint(true);

	// Inicia una nueva instancia del proceso
	public Long newProcessInstance() {
		Long id = null;

		try {
//			URL url = new URL("http://localhost:8090/rest/server/containers/business-application-kjar/processes/business-application-kjar.process/instances");
			URL url = new URL("http://localhost:8080/kie-server/services/rest/server/containers/business-application-kjar_1.0-SNAPSHOT/processes/business-application-kjar.process/instances");
			
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Authorization", getAuthoritationHeader(username, password));
			conn.setRequestProperty("Content-Type", "application/json");
			
			String body = "{}";
			
			conn.setDoOutput(true);
			OutputStream os = conn.getOutputStream();
			os.write(body.getBytes(StandardCharsets.UTF_8));
			os.flush();
			os.close();
			
			BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			StringBuilder sb = new StringBuilder();
			String linea;
			while ((linea = br.readLine()) != null) {
				sb.append(linea);
			}
			br.close();
			
			id = Long.parseLong(sb.toString());
			
			conn.disconnect();
		}catch (Exception e) {
			e.printStackTrace();
		}
		
		return id;
	}
	
	// Inicia la tarea humana de la instancia del proceso pasada como parámetro
	public void initTask(String id_process) {
		Long id_task = getIdTask(id_process);
		String questionnaire = null;
		
		try {
			URL url = new URL("http://localhost:8080/kie-server/services/rest/server/containers/business-application-kjar_1.0-SNAPSHOT/tasks/"+id_task.toString()+"/states/started");
			
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("PUT");
			conn.setRequestProperty("Authorization", getAuthoritationHeader(username, password));
			conn.setRequestProperty("Content-Type", "application/json");
			conn.connect();
			
//			System.out.println(conn.getResponseCode());
			if (conn.getResponseCode() == 201) {
				questionnaire = getQuestionnaire(id_task.toString());
				completeTask(id_task.toString(), questionnaire);
				
				// Obtener id de la respuesta (NO ES MUY CORRECTO PERO FUNCIONA)
				String result = getVariableValue(id_process, "result");
				String id = getIdResponse(result);
				System.out.println(id);
				
				// Obtiene la lista de pacientes destinatarios
				String patients = getVariableValue(id_process, "patients");
				System.out.println(patients);
			} else {
				// Mostrar un error o algo asi
			}
			
			conn.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	// Obtiene la respuesta y completa la tarea humana
	private void completeTask(String id, String quest) {
		try {
			URL url = new URL("http://localhost:8080/kie-server/services/rest/server/containers/business-application-kjar_1.0-SNAPSHOT/tasks/"+id+"/states/completed");
			
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("PUT");
			conn.setRequestProperty("Authorization", getAuthoritationHeader(username, password));
			conn.setRequestProperty("Content-Type", "application/json");
			
			String body = "{ \"out\": \""+example(quest)+"\", \"patients\": \""+getList()+"\"}";
//			System.out.println(body);
			
			conn.setDoOutput(true);
			OutputStream os = conn.getOutputStream();
			os.write(body.getBytes(StandardCharsets.UTF_8));
			os.flush();
			os.close();

			System.out.println(conn.getResponseCode());  // TIENE QUE ESTAR PARA QUE FUNCIONE
			
			conn.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	// Realización de la tarea humana
	private String example(String quest) {
		String response = null;
		
		// Convierte a formulario y obtiene la respuesta
		Questionnaire questionnaire = parser.parseResource(Questionnaire.class, quest);
		Map<String, Object> result = questToForm.map(questionnaire);
		
		// La respuesta se convierte a recurso QuestionnaireResponse
		formToResp = new FormToResp(questionnaire);
		QuestionnaireResponse resp = formToResp.map(result);
		
		// Se convierte a String y modifica para cumplir formato
		response = parser.encodeResourceToString(resp);
		response = response.replace("\n", "");
		response = response.replace("\"", "\\\"");
		
		return response;
	}
	
	// Obtiene los parametros de entrada de la tarea humana, se pasa como parámetro el id de la tarea
	private String getQuestionnaire(String id) {
		String questionnaire = null;
		
		try {
			URL url = new URL("http://localhost:8080/kie-server/services/rest/server/containers/business-application-kjar_1.0-SNAPSHOT/tasks/"+id+"/contents/input");
			
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Authorization", getAuthoritationHeader(username, password));
			conn.setRequestProperty("accept", "application/json");
			
			BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			StringBuilder sb = new StringBuilder();
			String linea;
			while ((linea = br.readLine()) != null) {
				sb.append(linea);
			}
			br.close();
			
			JSONObject json = new JSONObject(sb.toString());
			questionnaire = json.getString("in");
			
			conn.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return questionnaire;
	}
	
	// Se obtiene el valor de la variable de la instancia pasado como parámetro
	private String getVariableValue(String id, String nameVar) {
		String result = null;
		
		try {
			URL url = new URL("http://localhost:8080/kie-server/services/rest/server/containers/business-application-kjar_1.0-SNAPSHOT/processes/instances/"+id+"/variables/instances/"+nameVar);
			
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Authorization", getAuthoritationHeader(username, password));
			conn.setRequestProperty("accept", "application/json");
			
			BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			StringBuilder sb = new StringBuilder();
			String linea;
			while ((linea = br.readLine()) != null) {
				sb.append(linea);
			}
			br.close();
			
			JSONObject json = new JSONObject(sb.toString());
			result = json.getJSONArray("variable-instance").getJSONObject(0).getString("value");
			
			conn.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
	// Se obtiene el id de la tarea humana
	private Long getIdTask(String id_process) {
		Long id = null;
		
		try {
//			URL url = new URL("http://localhost:8090/rest/server/containers/business-application-kjar/processes/instances/"+id_process);
			URL url = new URL("http://localhost:8080/kie-server/services/rest/server/containers/business-application-kjar_1.0-SNAPSHOT/processes/instances/"+id_process);
			
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Authorization", getAuthoritationHeader(username, password));
			conn.setRequestProperty("accept", "application/json");
			
			BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			StringBuilder sb = new StringBuilder();
			String linea;
			while ((linea = br.readLine()) != null) {
				sb.append(linea);
			}
			br.close();
			
			JSONObject json = new JSONObject(sb.toString());
			id = json.getJSONObject("active-user-tasks").getJSONArray("task-summary").getJSONObject(0).getLong("task-id");
			
			conn.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return id;
	}
	
	
	// Autenticación (Usado en las cabecera de las peticiones)
	private String getAuthoritationHeader(String username, String password) {
		String auth = username + ":" + password;
		byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
		return "Basic " + new String(encodedAuth);
	}
	
	// Obtener el id del recurso QuestionnaireResponse (no es una forma muy buena pero funciona)
	private String getIdResponse(String in) {
		String newIn = in.substring(0, in.lastIndexOf(",")).concat("\n}");
		JSONObject json = new JSONObject(newIn);
		String id = json.getString("id");

		return id;
	}
	
	// Obtener la lista de pacientes (habria que buscar la forma de hacer dicha selección)
	private List<String> getList(){
		List<String> lista = new ArrayList<String>();
		
		lista.add("Jose");
		lista.add("Antonio");
		
		return lista;
	}
}
