package com.app.qraphql;

import java.util.Collections;
import java.util.Map;

public class GraphQLRequest {

	private String query;
	private Map<String, Object> variables;

	public String getQuery() {
		return query;
	}

	public Map<String, Object> getVariables() {
		if (variables == null) {
			return Collections.emptyMap();
		}
		return variables;
	}

}
