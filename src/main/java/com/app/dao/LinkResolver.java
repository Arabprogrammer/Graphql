package com.app.dao;

import java.util.ArrayList;
import java.util.List;

import com.app.domain.Link;

import graphql.schema.DataFetchingEnvironment;

public class LinkResolver {

	public List<Link> getAllLinks(DataFetchingEnvironment env) {
		List<Link> links = new ArrayList<>();
		links.add(new Link("http://localhost.com", "my favorite page"));
		return links;
	}

}
