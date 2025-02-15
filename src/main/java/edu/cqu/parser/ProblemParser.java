package edu.cqu.parser;

import java.io.File;

import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

import edu.cqu.core.Problem;

/**
 * Created by dyc on 2017/6/18.
 */
public class ProblemParser {

    private static final String BENCHMARK_RANDOM_DCOP = "RandomDCOP";
    private static final String TYPE_DCOP = "DCOP";
    private static final String TYPE_ADCOP = "ADCOP";

    protected Element rootElement;

    public ProblemParser(String path){
        try {
            rootElement = new SAXBuilder().build(new File(path)).getRootElement();
        } catch (Exception e) {
            throw new RuntimeException(String.format("file %s is not an XML, or not existent, or not readable", path));
        }
    }

    public Problem parse(){
        Problem problem = new Problem();
        Parser parser = null;
        if (getType().equals(TYPE_DCOP)){
            if (getBenchmark().equals(BENCHMARK_RANDOM_DCOP)){
                parser = new Parser(rootElement,problem);
            }
        }
        else if (getType().equals(TYPE_ADCOP)){
            if (getBenchmark().equals(BENCHMARK_RANDOM_DCOP)){
                parser = new ADCOPParser(rootElement,problem);
            }
        }
        parser.parseContent();
        return problem;
    }

    private String getBenchmark(){
        return rootElement.getChild("presentation").getAttributeValue("benchmark");
    }

    public String getType(){
        return rootElement.getChild("presentation").getAttributeValue("type");
    }


}
