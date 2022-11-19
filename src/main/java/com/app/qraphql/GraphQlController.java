package com.app.qraphql;

import static org.apache.logging.log4j.LogManager.getLogger;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import javax.inject.Inject;

import org.apache.logging.log4j.Logger;

import com.app.dao.LinkResolver;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.jsoniter.JsonIterator;

import graphql.ExceptionWhileDataFetching;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLError;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.schema.idl.TypeRuntimeWiring;
import graphql.schema.idl.errors.SchemaProblem;
import spark.Request;
import spark.Response;

public class GraphQlController {

	private static final Logger logger = getLogger(GraphQlController.class);

	private GraphQL graphQL;

	@Inject
	private LinkResolver linkResolver;

	private static <T> DataFetcher<T> wrapExceptions(DataFetcher<T> wrapee) {
		return (env) -> {
			try {
				return wrapee.get(env);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				throw e;
			}
		};
	}

	private RuntimeWiring buildRuntimeWiring() {
		RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring()
				.type(TypeRuntimeWiring.newTypeWiring("Query")
				.dataFetcher("allLinks", wrapExceptions(linkResolver::getAllLinks)))
				.build();
		return runtimeWiring;

	}

	private GraphQLSchema buildSchema(String schemaName) throws SchemaProblem, URISyntaxException, IOException {
		SchemaParser schemaParser = new SchemaParser();
		SchemaGenerator schemaGenerator = new SchemaGenerator();
		TypeDefinitionRegistry typeRegistry = new TypeDefinitionRegistry();
		URL url = Resources.getResource(schemaName);
		String schemaContent = Resources.toString(url, Charsets.UTF_8);
		typeRegistry.merge(schemaParser.parse(schemaContent));
		return schemaGenerator.makeExecutableSchema(typeRegistry, buildRuntimeWiring());
	}

	@Inject
	private void initialize() throws IOException, SchemaProblem, URISyntaxException {
		GraphQLSchema graphQLSchema = buildSchema("schema.graphqls");
		this.graphQL = GraphQL.newGraphQL(graphQLSchema).build();
	}

	public String postQuery(Request request, Response response) {
		if (request.body().isEmpty()) {
			return "Request cannot be embty :(";
		}
		response.header("Content-Type", "application/json");
		GraphQLRequest graphQLRequest = JsonIterator.deserialize(request.body(), GraphQLRequest.class);
		ExecutionInput executionInput = ExecutionInput.newExecutionInput().query(graphQLRequest.getQuery())
				.variables(graphQLRequest.getVariables()).build();
		ExecutionResult result = graphQL.execute(executionInput);
		for (GraphQLError error : result.getErrors()) {
			if (error instanceof Exception) {
				Exception e = (Exception) error;
				logger.error(e.getMessage());
			} else if (error instanceof ExceptionWhileDataFetching) {
				ExceptionWhileDataFetching e = (ExceptionWhileDataFetching) error;
				logger.error(e.getMessage(), e.getException());
			}
			logger.error(String.format("error %s %s", error.getClass().getSimpleName(), error));
			logger.debug("query " + graphQLRequest.toString());
		}

		return JsonIterator.deserialize(result.toSpecification().toString()).toString();

	}

}
