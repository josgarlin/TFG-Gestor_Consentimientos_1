package com.tfg.example.mapper;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.MaskFormatter;

import org.hl7.fhir.r5.model.Questionnaire;
import org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemAnswerOptionComponent;

@org.springframework.stereotype.Component
public class QuestToForm implements IMapper<Questionnaire, Map<String, Object>> {

	private JFrame frame;
	private JPanel panel;
	private JButton btnEnviar;
	
	private Map<String, Object> posicion = new HashMap<String, Object>();
	private Map<String, Object> result;
	
	@Override
	public Map<String, Object> map(Questionnaire in) {
		return createForm(in);
	}
	
	public Map<String, Object> getResult() {
		return result;
	}

	private Map<String, Object> createForm(Questionnaire questionnaire) {
		Map<String, Object> fillResult = null;
		
		frame = new JFrame();
		frame.setTitle("Formulario FHIR");
		
		panel = new JPanel();
		
		for (Questionnaire.QuestionnaireItemComponent item : questionnaire.getItem()) {
			generateFormComponents(item);
		}
		
		btnEnviar = new JButton("Enviar");
		btnEnviar.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				result = onClick(questionnaire);
			}
		});
		panel.add(btnEnviar);
		frame.setContentPane(panel);
		frame.pack();
		frame.setVisible(true);
		
		while (fillResult == null) {
			fillResult = getResult();
		}
		
		return fillResult;
	}

	private void generateFormComponents(Questionnaire.QuestionnaireItemComponent item) {
		switch (item.getType()) {
		case BOOLEAN:
			createBooleanComponent(item, posicion);
			break;
		case INTEGER:
			createIntegerComponent(item, posicion);
			break;
		case STRING:
			createStringComponent(item, posicion);
			break;
		case DATE:
			createDateComponent(item, posicion);
			break;
		case CHOICE:
			createChoiceComponent(item, posicion);
			break;
		case GROUP:
			posicion.put(item.getLinkId(), createGroupComponent(item));
			break;
		default:
			throw new UnsupportedOperationException("Tipo de componente no soportado: " + item.getType().getDisplay());
		}
	}
	
	private Map<String, Object> createGroupComponent(Questionnaire.QuestionnaireItemComponent item) {
		Map<String, Object> posicion = new HashMap<String, Object>();
		
		for (Questionnaire.QuestionnaireItemComponent it : item.getItem()) {
			switch (it.getType()) {
			case BOOLEAN:
				createBooleanComponent(it, posicion);
				break;
			case INTEGER:
				createIntegerComponent(it, posicion);
				break;
			case STRING:
				createStringComponent(it, posicion);
				break;
			case DATE:
				createDateComponent(it, posicion);
				break;
			case CHOICE:
				createChoiceComponent(it, posicion);
				break;
			case GROUP:
				posicion.put(it.getLinkId(), createGroupComponent(it));
				break;
			default:
				throw new UnsupportedOperationException("Tipo de componente no soportado: " + item.getType().getDisplay());
			}
		}
		
//		System.out.println(posicion);
		return posicion;
	}
	
	private void createBooleanComponent(Questionnaire.QuestionnaireItemComponent item, Map<String, Object> posicion) {
		JLabel label = new JLabel(item.getText());
		JCheckBox box1 = new JCheckBox("Sí");
		JCheckBox box2 = new JCheckBox("No");
		
		if (!(item.getAnswerOption().isEmpty())) {
			boolean valor = Boolean.parseBoolean(item.getAnswerOption().get(0).getValue().toString());
			if (valor) {
				box1.setSelected(true);
			} else {
				box2.setSelected(true);
			}
		}
		
		panel.add(label);
		panel.add(box1);
		panel.add(box2);
		
		List<Integer> indices = new ArrayList<Integer>();
		indices.add(panel.getComponentZOrder(box1));
		indices.add(panel.getComponentZOrder(box2));
		
//		System.out.println(indices);
		posicion.put(item.getLinkId(), indices);
	}
	
	private void createIntegerComponent(Questionnaire.QuestionnaireItemComponent item, Map<String, Object> posicion) {
		JLabel label = new JLabel(item.getText());
		JTextField textField = new JTextField(5);
		
		if (!(item.getAnswerOption().isEmpty())) {
			textField.setText(item.getAnswerOption().get(0).getValue().toString());
		}
		
		panel.add(label);
		panel.add(textField);
		
		int indice = panel.getComponentZOrder(textField);
		
		posicion.put(item.getLinkId(), indice);
	}
	
	private void createStringComponent(Questionnaire.QuestionnaireItemComponent item, Map<String, Object> posicion) {
		JLabel label = new JLabel(item.getText());
		JTextField textField = new JTextField(20);
	
		if (!(item.getAnswerOption().isEmpty())) {
			textField.setText(item.getAnswerOption().get(0).getValue().toString());
		}
		
		panel.add(label);
		panel.add(textField);
		
		int indice = panel.getComponentZOrder(textField);
		
		posicion.put(item.getLinkId(), indice);
	}
	
	private void createDateComponent(Questionnaire.QuestionnaireItemComponent item, Map<String, Object> posicion) {
		JLabel label = new JLabel(item.getText());
		JFormattedTextField dateField;
		MaskFormatter dateMask = null;
	    try {
	      dateMask = new MaskFormatter("##/##/####");
	      dateMask.setPlaceholderCharacter('_');
	    } catch (ParseException e) {
	      e.printStackTrace();
	    }
	    
	    // Crea el campo de fecha formateado con el formato de la máscara
	    dateField = new JFormattedTextField(new SimpleDateFormat("dd/MM/yyyy"));
	    dateField.setFormatterFactory(new DefaultFormatterFactory(dateMask));
		
	    if (!(item.getAnswerOption().isEmpty())) {
	    	SimpleDateFormat formatoActual = new SimpleDateFormat("yyyy-MM-dd");
			SimpleDateFormat formatoNuevo = new SimpleDateFormat("dd/MM/yyyy");
			
			String fecha = item.getAnswerOption().get(0).getValueDateType().asStringValue();
			String fechaNueva = null;
			try {
				fechaNueva = formatoNuevo.format(formatoActual.parse(fecha));
			} catch (ParseException e) {
				e.printStackTrace();
			}
	    	
			dateField.setText(fechaNueva);
		}
	    
		panel.add(label);
		panel.add(dateField);
		
		int indice = panel.getComponentZOrder(dateField);
		
		posicion.put(item.getLinkId(), indice);
	}
	
	private void createChoiceComponent(Questionnaire.QuestionnaireItemComponent item, Map<String, Object> posicion) {
		JLabel label = new JLabel(item.getText());
		JComboBox<String> elementos = new JComboBox<String>();
		
		List<String> list = new ArrayList<String>();
		for (int i=0; i<item.getAnswerOption().size(); i++) {
			list.add(item.getAnswerOption().get(i).getValueCoding().getCode());
		}
		
		List<QuestionnaireItemAnswerOptionComponent> options = item.getAnswerOption();
		String texto = null;
		if (findDuplicate(list)) {
			String help = list.get(list.size()-1);
			list.remove(list.size()-1);
			options.remove(options.size()-1);
			
			int id = list.indexOf(help);
			if (options.get(id).getValueCoding().getDisplay() == null) {
				texto = options.get(id).getValueCoding().getCode();
			} else {
				texto = options.get(id).getValueCoding().getDisplay();
			}
		}
		
		for (int i=0; i<options.size(); i++) {
			if (options.get(i).getValueCoding().getDisplay() == null) {
				elementos.addItem(options.get(i).getValueCoding().getCode());
			} else {
				elementos.addItem(options.get(i).getValueCoding().getDisplay());
			}
		}
		
		if (texto != null) {
			elementos.setSelectedItem(texto);
		}
		
		panel.add(label);
		panel.add(elementos);
		
		int indice = panel.getComponentZOrder(elementos);
		
		posicion.put(item.getLinkId(), indice);
	}
	
	private Boolean findDuplicate(List<String> lista) {
		Boolean result = false;
		
		for (int i = 0; i < lista.size(); i++) {
			for (int j = i+1; j < lista.size(); j++) {
				if (lista.get(i).equals(lista.get(j))) {
					result = true;
				}
			}
		}
		
		return result;
	}
	
	private Map<String, Object> onClick(Questionnaire questionnaire){
		Map<String, Object> respuestas = new HashMap<String, Object>();
		
		for (Questionnaire.QuestionnaireItemComponent item : questionnaire.getItem()) {
//			System.out.println(posicion.get(item.getLinkId()));
			switch (item.getType()) {
			case BOOLEAN:
				respuestas.put(item.getLinkId(), respuestaBoolean((List<Integer>) posicion.get(item.getLinkId())));
				break;
			case INTEGER:
				respuestas.put(item.getLinkId(), respuestaInteger((Integer) posicion.get(item.getLinkId())));
				break;
			case STRING:
				respuestas.put(item.getLinkId(), respuestaString((Integer) posicion.get(item.getLinkId())));
				break;
			case DATE:
				respuestas.put(item.getLinkId(), respuestaDate((Integer) posicion.get(item.getLinkId())));
				break;
			case CHOICE:
				respuestas.put(item.getLinkId(), respuestaChoice(item, (Integer) posicion.get(item.getLinkId())));
				break;
			case GROUP:
				respuestas.put(item.getLinkId(), respuestaGroup(item));
				break;
			default:
				throw new UnsupportedOperationException("Tipo de componente no soportado: " + item.getType().getDisplay());
			}
		}
		
		frame.setVisible(false);
		
		return respuestas;
	}
	
	private Map<String, Object> respuestaGroup(Questionnaire.QuestionnaireItemComponent item) {
		Map<String, Object> result = new HashMap<String, Object>();
		Map<String, Object> pos = (Map<String, Object>) posicion.get(item.getLinkId());
		
		for (Questionnaire.QuestionnaireItemComponent it : item.getItem()) {
			switch (it.getType()) {
			case BOOLEAN:
				result.put(it.getLinkId(), respuestaBoolean((List<Integer>) pos.get(it.getLinkId())));
				break;
			case INTEGER:
				result.put(it.getLinkId(), respuestaInteger((Integer) pos.get(it.getLinkId())));
				break;
			case STRING:
				result.put(it.getLinkId(), respuestaString((Integer) pos.get(it.getLinkId())));
				break;
			case DATE:
				result.put(it.getLinkId(), respuestaDate((Integer) pos.get(it.getLinkId())));
				break;
			case CHOICE:
				result.put(it.getLinkId(), respuestaChoice(it, (Integer) pos.get(it.getLinkId())));
				break;
			case GROUP:
				result.put(it.getLinkId(), respuestaGroup(it));
				break;
			default:
				throw new UnsupportedOperationException("Tipo de componente no soportado: " + item.getType().getDisplay());
			}
		}
		
		return result;
	}
	
	private Boolean respuestaBoolean(List<Integer> pos) {
		Boolean result = null;
		
		for (int i = 0; i < pos.size(); i++) {
			Component component = panel.getComponent(pos.get(i));
			JCheckBox checkBox = (JCheckBox) component;
			
			if (checkBox.isSelected()) {
				if (checkBox.getText().equals("Sí")) {
					result = true;
				} else if (checkBox.getText().equals("No")) {
					result = false;
				}
			}
		}
		
		return result;
	}
	
	private Integer respuestaInteger(Integer pos) {
		Integer result = null;
		
		Component component = panel.getComponent(pos);
		JTextField textField = (JTextField) component;
		result = Integer.parseInt(textField.getText());
		
		return result;
	}
	
	private String respuestaString(Integer pos) {
		String result = null;
		
		Component component = panel.getComponent(pos);
		JTextField textField = (JTextField) component;
		result = textField.getText();
		
		if (result.equals("")) {
			result = null;
		}
		
		return result;
	}
	
	private String respuestaDate(Integer pos) {
		String result = null;
		
		Component component = panel.getComponent(pos);
		JFormattedTextField textField = (JFormattedTextField) component;
		result = textField.getText();
		
		return result;
	}
	
	private String respuestaChoice(Questionnaire.QuestionnaireItemComponent item, Integer pos) {
		String text = null;
		
		Component component = panel.getComponent(pos);
		JComboBox<String> elementos = (JComboBox<String>) component;
		text = (String) elementos.getSelectedItem();
		
		String result = code(item, text);
		
		return result;
	}
	
	private String code(Questionnaire.QuestionnaireItemComponent item, String texto) {
		String result = null;
		
		for (int i=0; i<item.getAnswerOption().size(); i++) {
			if (item.getAnswerOption().get(i).getValueCoding().getDisplay() == null) {
				if (item.getAnswerOption().get(i).getValueCoding().getCode().equals(texto)) {
					result = item.getAnswerOption().get(i).getValueCoding().getCode();
				}
			} else {
				if (item.getAnswerOption().get(i).getValueCoding().getDisplay().equals(texto)) {
					result = item.getAnswerOption().get(i).getValueCoding().getCode();
				}
			}
		}
		
		return result;
	}

}
