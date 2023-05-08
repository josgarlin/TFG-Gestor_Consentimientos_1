package com.tfg.example.mapper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hl7.fhir.r5.model.BooleanType;
import org.hl7.fhir.r5.model.Coding;
import org.hl7.fhir.r5.model.DateType;
import org.hl7.fhir.r5.model.IntegerType;
import org.hl7.fhir.r5.model.Questionnaire;
import org.hl7.fhir.r5.model.QuestionnaireResponse;
import org.hl7.fhir.r5.model.StringType;
import org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r5.model.QuestionnaireResponse.QuestionnaireResponseItemComponent;
import org.hl7.fhir.r5.model.QuestionnaireResponse.QuestionnaireResponseStatus;

public class FormToResp implements IMapper<Map<String, Object>, QuestionnaireResponse> {

	private Questionnaire questionnaire;

	public FormToResp(Questionnaire questionnaire) {
		this.questionnaire = questionnaire;
	}

	@Override
	public QuestionnaireResponse map(Map<String, Object> in) {
		QuestionnaireResponse response = new QuestionnaireResponse();
		
		response.setStatus(QuestionnaireResponseStatus.COMPLETED);
		response.setQuestionnaire(questionnaire.getId());
		for (Questionnaire.QuestionnaireItemComponent item : questionnaire.getItem()) {
			if (!(in.get(item.getLinkId()) == null)) {
				switch (item.getType()) {
				case BOOLEAN:
					response.addItem()
						.setLinkId(item.getLinkId())
						.setText(item.getText())
						.addAnswer()
							.setValue(new BooleanType((Boolean) in.get(item.getLinkId())));
					break;
				case INTEGER:
					response.addItem()
						.setLinkId(item.getLinkId())
						.setText(item.getText())
						.addAnswer()
							.setValue(new IntegerType((Integer) in.get(item.getLinkId())));
					break;
				case STRING:
					response.addItem()
						.setLinkId(item.getLinkId())
						.setText(item.getText())
						.addAnswer()
							.setValue(new StringType((String) in.get(item.getLinkId())));
					break;
				case DATE:
					responseDate(response, item, (String) in.get(item.getLinkId()), null);
					break;
				case CHOICE:
					responseChoice(response, item, (String) in.get(item.getLinkId()), null);
					break;
				case GROUP:
					responseGroup(response, item, item.getItem(), (Map<String, Object>) in.get(item.getLinkId()));
					break;
				default:
					throw new UnsupportedOperationException("Tipo de componente no soportado: " + item.getType().getDisplay());
				}
			}
		}
		
		return response;
	}
	
	private void responseGroup(QuestionnaireResponse response, QuestionnaireItemComponent item, List<QuestionnaireItemComponent> items, Map<String, Object> resultados) {
		List<QuestionnaireResponseItemComponent> example = new ArrayList<QuestionnaireResponse.QuestionnaireResponseItemComponent>();
		
		for (Questionnaire.QuestionnaireItemComponent it : items) {
			QuestionnaireResponseItemComponent t = new QuestionnaireResponseItemComponent();
			if (!(resultados.get(it.getLinkId()) == null)) {
				switch (it.getType()) {
				case BOOLEAN:
					t.setLinkId(it.getLinkId())
						.setText(it.getText())
						.addAnswer()
							.setValue(new BooleanType((Boolean) resultados.get(it.getLinkId())));
					example.add(t);
					break;
				case INTEGER:
					t.setLinkId(it.getLinkId())
						.setText(it.getText())
						.addAnswer()
							.setValue(new IntegerType((Integer) resultados.get(it.getLinkId())));
					example.add(t);
					break;
				case STRING:
					t.setLinkId(it.getLinkId())
						.setText(it.getText())
						.addAnswer()
							.setValue(new StringType((String) resultados.get(it.getLinkId())));
					example.add(t);
					break;
				case DATE:
					responseDate(response, it, (String) resultados.get(it.getLinkId()), t);
					example.add(t);
					break;
				case CHOICE:
					responseChoice(response, it, (String) resultados.get(it.getLinkId()), t);
					example.add(t);
					break;
				case GROUP:
					responseGroup(response, it, it.getItem(), (Map<String, Object>) resultados.get(it.getLinkId()));
					break;
				default:
					throw new UnsupportedOperationException("Tipo de componente no soportado: " + it.getType().getDisplay());
				}
			}
		}
		
		response.addItem()
			.setLinkId(item.getLinkId())
			.setText(item.getText())
			.setItem(example);
	}
	
	private void responseDate(QuestionnaireResponse response, QuestionnaireItemComponent item, String fecha, QuestionnaireResponseItemComponent t) {
		SimpleDateFormat formatoActual = new SimpleDateFormat("dd/MM/yyyy");
		SimpleDateFormat formatoNuevo = new SimpleDateFormat("yyyy-MM-dd");
		
		String fechaNueva = null;
		try {
			fechaNueva = formatoNuevo.format(formatoActual.parse(fecha));
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		if (t == null) {
			response.addItem()
				.setLinkId(item.getLinkId())
				.setText(item.getText())
				.addAnswer()
					.setValue(new DateType(fechaNueva));
		} else {
			t.setLinkId(item.getLinkId())
				.setText(item.getText())
				.addAnswer()
					.setValue(new DateType(fechaNueva));
		}
	}
	
	private void responseChoice(QuestionnaireResponse response, QuestionnaireItemComponent item, String choice, QuestionnaireResponseItemComponent t) {
		if (t == null) {
			response.addItem()
				.setLinkId(item.getLinkId())
				.setText(item.getText())
				.addAnswer()
					.setValue(new Coding().setCode(choice));
		} else {
			t.setLinkId(item.getLinkId())
				.setText(item.getText())
				.addAnswer()
					.setValue(new Coding().setCode(choice));
		}
	}

}
