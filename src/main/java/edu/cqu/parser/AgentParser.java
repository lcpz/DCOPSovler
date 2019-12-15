package edu.cqu.parser;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import edu.cqu.core.AgentDescriptor;

/**
 * Created by dyc on 2017/6/26.
 */
public class AgentParser {
	private Element root;

    public AgentParser(String agentsPath) {
		try {
			Document document = new SAXBuilder().build(new File(agentsPath));
			root = document.getRootElement();
		} catch (JDOMException | IOException e) {
			e.printStackTrace();
		}
    }

    public Map<String,AgentDescriptor> getAgents() {
        Map<String, AgentDescriptor> map = new HashMap<>();

		String name;
		AgentDescriptor descriptor;

		for (Element var : (List<Element>) root.getChild("agents").getChildren()) {
			name = var.getAttributeValue("name");
            descriptor = new AgentDescriptor(var.getAttributeValue("class"), var.getAttributeValue("method").toUpperCase());
            map.put(name.toUpperCase(), descriptor);
		}

        return map;
    }

    public Map<String,String> getConfigurations(){
        List<Element> configurations = root.getChildren("configurations");

        if (configurations.size() == 0)
        	return null;

        Map<String, String> m = new HashMap<>();

		for (Element var : (List<Element>) root.getChild("configurations").getChildren())
            m.put(var.getAttributeValue("name").toUpperCase(), var.getAttributeValue("value").toUpperCase());

        return m;
    }
}
