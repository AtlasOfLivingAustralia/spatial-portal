package org.ala.layers.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;

/**
 * Utility class for parsing JSON maps provided to webservice handling methods.
 *
 * @author ChrisF
 */
public class JSONRequestBodyParser {

    List<String> _parameterNames;

    /**
     * Holds parsed values for request parameters
     */
    Map<String, Object> _parsedParameterValues;

    /**
     * Holds types (classes) for request parameters
     */
    Map<String, Class<?>> _parameterClasses;

    /**
     * Holds booleans values indicating whether or not parameters are optional
     */
    Map<String, Boolean> _parametersOptionalStatus;

    List<String> _errorMessages;

    public JSONRequestBodyParser() {
        _parameterNames = new ArrayList<String>();
        _parsedParameterValues = new HashMap<String, Object>();
        _parameterClasses = new HashMap<String, Class<?>>();
        _parametersOptionalStatus = new HashMap<String, Boolean>();
        _errorMessages = new ArrayList<String>();
    }

    public void addParameter(String name, Class<?> clazz, boolean optional) {
        if (_parameterNames.contains(name)) {
            throw new IllegalArgumentException("Parameter with this name has already been defined");
        }

        _parameterNames.add(name);
        _parameterClasses.put(name, clazz);
        _parametersOptionalStatus.put(name, optional);

    }

    public boolean parseJSON(String json) {
        boolean parseSuccessful = true;
        ObjectMapper mapper = new ObjectMapper();

        try {
            Map<String, Object> parsedJSON = mapper.readValue(json, Map.class);

            for (String parameterName : _parameterNames) {
                if (parsedJSON.containsKey(parameterName)) {
                    Object parsedParameterValue = parsedJSON.get(parameterName);
                    if (_parameterClasses.get(parameterName).isInstance(parsedParameterValue)) {
                        _parsedParameterValues.put(parameterName, parsedParameterValue);
                    } else {
                        _errorMessages.add("Invalid value for " + parameterName + " expecting a value of type " + _parameterClasses.get(parameterName).getSimpleName());
                    }
                } else {
                    if (!_parametersOptionalStatus.get(parameterName)) {
                        _errorMessages.add("Missing compulsory parameter " + parameterName);
                        parseSuccessful = false;
                    }
                }
            }
        } catch (Exception ex) {
            _errorMessages.add("Malformed JSON map");
        }

        return parseSuccessful;
    }

    public List<String> getErrorMessages() {
        return _errorMessages;
    }

    public Object getParsedValue(String parameterName) {
        return _parsedParameterValues.get(parameterName);
    }

}
