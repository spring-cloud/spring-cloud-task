/*
 *  Copyright 2016 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.cloud.task.batch.listener.support;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.springframework.batch.core.JobParameter;

/**
 * This is a JobParametersEvent DTO created so that a {@link org.springframework.batch.core.JobParameters} can be
 * serialized into Json without having to add mixins to an ObjectMapper.
 *
 * @author Glenn Renfro
 */
public class JobParametersEvent {
	private final Map<String,JobParameterEvent> parameters;

	public JobParametersEvent() {
		this.parameters = new LinkedHashMap<>();
	}

	public JobParametersEvent(Map<String,JobParameter> jobParameters) {
		this.parameters = new LinkedHashMap<>();
		for(Map.Entry<String, JobParameter> entry: jobParameters.entrySet()){
			if(entry.getValue().getValue() instanceof String){
				parameters.put(entry.getKey(), new JobParameterEvent(entry.getValue()));
			}
			else if(entry.getValue().getValue() instanceof Long){
				parameters.put(entry.getKey(), new JobParameterEvent(entry.getValue()));
			}
			else if(entry.getValue().getValue() instanceof Date){
				parameters.put(entry.getKey(), new JobParameterEvent(entry.getValue()));
			}
			else if(entry.getValue().getValue() instanceof Double){
				parameters.put(entry.getKey(), new JobParameterEvent(entry.getValue()));
			}
		}
	}


	/**
	 * Typesafe Getter for the Long represented by the provided key.
	 *
	 * @param key The key to get a value for
	 * @return The <code>Long</code> value
	 */
	public Long getLong(String key){
		if (!parameters.containsKey(key)) {
			return 0L;
		}
		Object value = parameters.get(key).getValue();
		return value==null ? 0L : ((Long)value).longValue();
	}

	/**
	 * Typesafe Getter for the Long represented by the provided key.  If the
	 * key does not exist, the default value will be returned.
	 *
	 * @param key to return the value for
	 * @param defaultValue to return if the value doesn't exist
	 * @return the parameter represented by the provided key, defaultValue
	 * otherwise.
	 */
	public Long getLong(String key, long defaultValue){
		if(parameters.containsKey(key)){
			return getLong(key);
		}
		else{
			return defaultValue;
		}
	}

	/**
	 * Typesafe Getter for the String represented by the provided key.
	 *
	 * @param key The key to get a value for
	 * @return The <code>String</code> value
	 */
	public String getString(String key){
		JobParameterEvent value = parameters.get(key);
		return value==null ? null : value.toString();
	}

	/**
	 * Typesafe Getter for the String represented by the provided key.  If the
	 * key does not exist, the default value will be returned.
	 *
	 * @param key to return the value for
	 * @param defaultValue to return if the value doesn't exist
	 * @return the parameter represented by the provided key, defaultValue
	 * otherwise.
	 */
	public String getString(String key, String defaultValue){
		if(parameters.containsKey(key)){
			return getString(key);
		}
		else{
			return defaultValue;
		}
	}

	/**
	 * Typesafe Getter for the Long represented by the provided key.
	 *
	 * @param key The key to get a value for
	 * @return The <code>Double</code> value
	 */
	public Double getDouble(String key){
		if (!parameters.containsKey(key)) {
			return 0.0;
		}
		Double value = (Double)parameters.get(key).getValue();
		return value==null ? 0.0 : value.doubleValue();
	}

	/**
	 * Typesafe Getter for the Double represented by the provided key.  If the
	 * key does not exist, the default value will be returned.
	 *
	 * @param key to return the value for
	 * @param defaultValue to return if the value doesn't exist
	 * @return the parameter represented by the provided key, defaultValue
	 * otherwise.
	 */
	public Double getDouble(String key, double defaultValue){
		if(parameters.containsKey(key)){
			return getDouble(key);
		}
		else{
			return defaultValue;
		}
	}

	/**
	 * Typesafe Getter for the Date represented by the provided key.
	 *
	 * @param key The key to get a value for
	 * @return The <code>java.util.Date</code> value
	 */
	public Date getDate(String key){
		return this.getDate(key,null);
	}

	/**
	 * Typesafe Getter for the Date represented by the provided key.  If the
	 * key does not exist, the default value will be returned.
	 *
	 * @param key to return the value for
	 * @param defaultValue to return if the value doesn't exist
	 * @return the parameter represented by the provided key, defaultValue
	 * otherwise.
	 */
	public Date getDate(String key, Date defaultValue){
		if(parameters.containsKey(key)){
			return (Date)parameters.get(key).getValue();
		}
		else{
			return defaultValue;
		}
	}

	/**
	 * Get a map of all parameters, including string, long, and date.
	 *
	 * @return an unmodifiable map containing all parameters.
	 */
	public Map<String, JobParameterEvent> getParameters(){
		return new LinkedHashMap<String, JobParameterEvent>(parameters);
	}

	/**
	 * @return true if the parameters is empty, false otherwise.
	 */
	public boolean isEmpty(){
		return parameters.isEmpty();
	}

	@Override
	public boolean equals(Object obj) {
		if(obj instanceof JobParametersEvent == false){
			return false;
		}

		if(obj == this){
			return true;
		}

		JobParametersEvent rhs = (JobParametersEvent)obj;
		return this.parameters.equals(rhs.parameters);
	}

	@Override
	public int hashCode() {
		return 17 + 23 * parameters.hashCode();
	}

	@Override
	public String toString() {
		return parameters.toString();
	}

	public Properties toProperties() {
		Properties props = new Properties();

		for (Map.Entry<String, JobParameterEvent> param : parameters.entrySet()) {
			if(param.getValue() != null) {
				props.put(param.getKey(), param.getValue().toString());
			}
		}

		return props;
	}
}
