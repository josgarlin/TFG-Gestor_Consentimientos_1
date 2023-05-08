package com.tfg.example.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import org.hl7.fhir.r5.model.Questionnaire;
import org.hl7.fhir.r5.model.QuestionnaireResponse;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import com.tfg.example.mapper.FormToResp;
import com.tfg.example.mapper.QuestResponseToQuest;
import com.tfg.example.mapper.QuestToForm;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

@Service
public class PatientService {
	private final String username = "wbadmin";
	private final String password = "wbadmin";
	
	private final QuestResponseToQuest questResponseToQuest = new QuestResponseToQuest();
	private final QuestToForm questToForm = new QuestToForm();
	private FormToResp formToResp;
	
	private FhirContext ctx = FhirContext.forR5();
	private IParser parser = ctx.newJsonParser().setPrettyPrint(true);
	
	public Long newProcessInstance(String id_response) {
		Long id = null;

		try {
			URL url = new URL("http://localhost:8080/kie-server/services/rest/server/containers/business-application-kjar_1.0-SNAPSHOT/processes/business-application-kjar.patientProcess/instances");
			
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Authorization", getAuthoritationHeader(username, password));
			conn.setRequestProperty("Content-Type", "application/json");
			
			String body = "{ \"id_response\": \""+id_response+"\" }";
			
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
	
	public void initTask(String id_process) {
		Long id_task = getIdTask(id_process);
		String questionnaireResponse = null;
		
		try {
			URL url = new URL("http://localhost:8080/kie-server/services/rest/server/containers/business-application-kjar_1.0-SNAPSHOT/tasks/"+id_task.toString()+"/states/started");
			
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("PUT");
			conn.setRequestProperty("Authorization", getAuthoritationHeader(username, password));
			conn.setRequestProperty("Content-Type", "application/json");
			conn.connect();
			
			if (conn.getResponseCode() == 201) {
				questionnaireResponse = getQuestionnaireResponse(id_task.toString());
				completeTask(id_task.toString(), questionnaireResponse);
				
				// Obtener id de la respuesta (NO ES MUY CORRECTO PERO FUNCIONA)
				String result = getVariableValue(id_process, "result");
				String id = getIdResponse(result);
				System.out.println(id);
			} else {
				
			}
			
			conn.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void completeTask(String id, String questResponse) {
		try {
			URL url = new URL("http://localhost:8080/kie-server/services/rest/server/containers/business-application-kjar_1.0-SNAPSHOT/tasks/"+id+"/states/completed");
			
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("PUT");
			conn.setRequestProperty("Authorization", getAuthoritationHeader(username, password));
			conn.setRequestProperty("Content-Type", "application/json");
			
			String body = "{ \"out\": \""+example(questResponse)+"\"}";
			
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
	
	private String example(String questResponse) {
		String response = null;
		
		QuestionnaireResponse questionnaireResponse = parser.parseResource(QuestionnaireResponse.class, questResponse);
		Questionnaire questionnaire = questResponseToQuest.map(questionnaireResponse);
		
		Map<String, Object> result = questToForm.map(questionnaire);
		
		formToResp = new FormToResp(questionnaire);
		QuestionnaireResponse resp = formToResp.map(result);
		response = parser.encodeResourceToString(resp);
		response = response.replace("\n", "");
		response = response.replace("\"", "\\\"");
		
		return response;
	}
	
	private String getQuestionnaireResponse(String id) {
		String questionnaireResponse = null;
		
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
			questionnaireResponse = json.getString("in");
			
			conn.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return questionnaireResponse;
	}
	
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
	
	private Long getIdTask(String id_process) {
		Long id = null;
		
		try {
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
	
	private String getAuthoritationHeader(String username, String password) {
		String auth = username + ":" + password;
		byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
		return "Basic " + new String(encodedAuth);
	}
	
	private String getIdResponse(String in) {
		String newIn = in.substring(0, in.lastIndexOf(",")).concat("\n}");
		JSONObject json = new JSONObject(newIn);
		String id = json.getString("id");

		return id;
	}
}
