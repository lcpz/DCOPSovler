package edu.cqu.benchmark;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import edu.cqu.benchmark.graphcoloring.GraphColoringGenerator;
import edu.cqu.benchmark.graphcoloring.WeightedGraphColoringGenerator;
import edu.cqu.benchmark.meetingscheduling.MeetingScheduling;
import edu.cqu.benchmark.randomdcops.AMaxDCSPGenerator;
import edu.cqu.benchmark.randomdcops.RandomADCOPGenerator;
import edu.cqu.benchmark.randomdcops.RandomDCOPGenerator;
import edu.cqu.benchmark.scalefreenetworks.ScaleFreeNetworkGenerator;

/**
 * Created by YanChenDeng on 2016/4/18.
 */
public class ContentWriter {

    public static final String PROBLEM_SCALE_FREE_NETWORK = "SCALE_FREE_NETWORK";
    public static final String PROBLEM_RANDOM_DCOP = "RANDOM_DCOP";
    public static final String PROBLEM_GRAPH_COLORING = "GRAPH_COLORING";
    public static final String PROBLEM_WEIGHTED_GRAPH_COLORING = "WEIGHTED_GRAPH_COLORING";
    public static final String PROBLEM_RANDOM_ADCOP = "RANDOM_ADCOP";
    public static final String PROBLEM_ASYMETIRC_MAX_DCSP = "RANDOM_A_MAX_DCSP";
    public static final String PROBLEM_MEETING_SCHEDULING = "MEETING_SCHEDULING";

    private int nbInstance;
    private String dirPath;
    private int nbAgent;
    private int domainSize;
    private int minCost;
    private int maxCost;
    private String problemType;
    private Map<String,Object> extraParameter;

    public ContentWriter(int nbInstance, String dirPath, int nbAgent, int domainSize, int minCost, int maxCost, String problemType,Map<String,Object> extraParameter) {
        this.nbInstance = nbInstance;
        this.dirPath = dirPath;
        this.nbAgent = nbAgent;
        this.domainSize = domainSize;
        this.minCost = minCost;
        this.maxCost = maxCost;
        this.problemType = problemType;
        this.extraParameter = extraParameter;
    }

    public void setExtraParameter(Map<String, Object> extraParameter) {
        this.extraParameter = extraParameter;
    }

    public void generate() throws Exception{
        Format format = Format.getPrettyFormat();
        format.setEncoding("UTF-8");
        XMLOutputter outputter = new XMLOutputter(format);
        int base = 0;
        File f=new File(dirPath);
		if(!f.exists())
		{
			f.mkdirs();
		}
        String filenameBase =dirPath + "\\" + problemType + "_" + nbAgent + "_" + domainSize + "_";
        for (String key : extraParameter.keySet()){
            filenameBase += key + "_";
            filenameBase += extraParameter.get(key) + "_";
        }
        while (true){
            String fileName = filenameBase + base + ".xml";
            if (!new File(fileName).exists())
                break;
            base++;
        }
        for (int i = 0; i < nbInstance; i++){
            FileOutputStream stream = new FileOutputStream(filenameBase+ (base + i) + ".xml");
            Element root = new Element("instance");
            AbstractGraph graph = null;
            if (problemType.equals(PROBLEM_SCALE_FREE_NETWORK)) {
                graph = new ScaleFreeNetworkGenerator("instance" + i,nbAgent,domainSize,minCost,maxCost,(Integer)extraParameter.get("m1"),(Integer)extraParameter.get("m2"));
            }
            else if (problemType.equals(PROBLEM_RANDOM_DCOP)){
                graph = new RandomDCOPGenerator("instance" + i,nbAgent,domainSize,minCost,maxCost,(double)extraParameter.get("density"));
            } else if (problemType.equals(PROBLEM_GRAPH_COLORING)) {
                graph = new GraphColoringGenerator("instance" + i,nbAgent,domainSize,(double)extraParameter.get("density"));
            }
            else if (problemType.equals(PROBLEM_WEIGHTED_GRAPH_COLORING)){
                graph = new WeightedGraphColoringGenerator("instance" + i,nbAgent,domainSize,minCost,maxCost,(double)extraParameter.get("density"));
            }
            else if (problemType.equals(PROBLEM_RANDOM_ADCOP)){
                graph = new RandomADCOPGenerator("instance" + i,nbAgent,domainSize,minCost,maxCost,(double)extraParameter.get("density"));
            }
            else if (problemType.equals(PROBLEM_ASYMETIRC_MAX_DCSP)){
                graph = new AMaxDCSPGenerator("instance" + i,nbAgent,domainSize,minCost,maxCost,(double)extraParameter.get("density"),(double) extraParameter.get("p2"));
            }
            else if (problemType.equals(PROBLEM_MEETING_SCHEDULING)){
                int nbMeeting = (int) extraParameter.get("nbm");
                int meetingPerAgent = (int) extraParameter.get("mpa");
                int maxTt = (int) extraParameter.get("maxtt");
                int minTt = (int) extraParameter.get("mintt");
                graph = new MeetingScheduling(nbAgent,nbMeeting,meetingPerAgent,maxTt,minTt,domainSize);
            }
            graph.generateConstraint();
            root.addContent(graph.getPresentation());
            root.addContent(graph.getAgents());
            root.addContent(graph.getDomains());
            root.addContent(graph.getVariables());
            root.addContent(graph.getConstraints());
            root.addContent(graph.getRelations());
            root.addContent(graph.getGuiPresentation());
            outputter.output(root,stream);
            stream.close();
        }
    }

    public static void main(String[] args) throws Exception{
        Map<String,Object> para = new HashMap<String, Object>();
        double p1 = 0.3;
        para.put("density",p1);
        int agentNum = 90;
        int domainsize = 20;
//        double p2 = 0.9;
//        para.put("p2",p2);
        para.put("nbm",20);
        para.put("mpa",2);
        para.put("maxtt",10);
        para.put("mintt",6);
        String path = "problem/ms/"+agentNum + "/"+domainsize;

        String problemType = PROBLEM_MEETING_SCHEDULING;
        ContentWriter writer = new ContentWriter(10,path,agentNum,domainsize,1,100,problemType,para);
        writer.generate();
    }
}
