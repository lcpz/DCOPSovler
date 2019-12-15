package edu.cqu.core;

/**
 * Created by dyc on 2017/6/26.
 */
public class AgentDescriptor {

	public String className;
	public String method;

	public AgentDescriptor(String className, String method) {
		this.className = className;
		this.method = method;
	}

}
