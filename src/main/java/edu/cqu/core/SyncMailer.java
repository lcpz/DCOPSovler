package edu.cqu.core;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import edu.cqu.framework.ALSAgent;
import edu.cqu.result.ResultAls;
import edu.cqu.result.ResultCycle;
import edu.cqu.result.annotations.NotRecordCostInCycle;

/**
 * Created by dyc on 2017/6/16.
 */
public class SyncMailer extends Process{

    public static final int PHASE_AGENT = 1;
    public static final int PHASE_MAILER = 2;

    private Map<Integer,SyncAgent> agents;
    private Queue<Message> messageQueue;
    private int cycleCount;
    private int messageCount;
    private Set<Integer> agentReady;
    private AtomicInteger phase;
    private double[] costInCycle;
    private double[] bestCostInCyle;
    private int tail;
    private ResultCycle resultCycle;
    private long startTime;
    private FinishedListener listener;
    private Set<Agent> stoppedAgents;
    private boolean printCycle;
    private List<CycleListener> cycleListeners;
    private List<AgentIteratedOverListener> agentIteratedOverListeners;
    private double sumCost = 0;
    private int t;
    private int ncccs;
    private boolean recordCostInCycle;

    public SyncMailer(){
        super("mailer");
        agents = new HashMap<>();
        messageQueue = new LinkedList<>();
        agentReady = new HashSet<>();
        phase = new AtomicInteger(PHASE_AGENT);
        costInCycle = new double[2];
        bestCostInCyle = new double[2];
        stoppedAgents = new HashSet<>();
        cycleListeners = new LinkedList<>();
        agentIteratedOverListeners = new LinkedList<>();
        recordCostInCycle = true;
    }

    public SyncMailer(FinishedListener finishedListener){
        this();
        listener = finishedListener;
    }


    public void setPrintCycle(boolean printCycle) {
        this.printCycle = printCycle;
    }

    public void registerCycleListener(CycleListener listener){
        cycleListeners.add(listener);
    }

    public void registerAgentIteratedOverListener(AgentIteratedOverListener listener){
        agentIteratedOverListeners.add(listener);
    }

    private void expand(){
        double[] tmpCostInCycle = new double[costInCycle.length * 2];
        double[] tmpBestCostInCycle = new double[bestCostInCyle.length * 2];
        for (int i = 0 ; i < costInCycle.length; i++){
            tmpCostInCycle[i] = costInCycle[i];
            tmpBestCostInCycle[i] = bestCostInCyle[i];
        }
        costInCycle = tmpCostInCycle;
        bestCostInCyle = tmpBestCostInCycle;
    }

    public void register(SyncAgent agent){
        if (recordCostInCycle){
            recordCostInCycle = !agent.getClass().isAnnotationPresent(NotRecordCostInCycle.class);
        }
        agents.put(agent.id,agent);
    }

    public void addMessage(Message message) {
        synchronized (phase){
            while (phase.get() == PHASE_MAILER);
            synchronized (messageQueue){
                messageQueue.add(message);
            }
        }
    }

    @Override
    public void preExecution() {
        startTime = new Date().getTime();
    }

    @Override
    public void execution() {
        if (phase.get() == PHASE_MAILER){
            synchronized (messageQueue){
                while (!messageQueue.isEmpty()){
                    Message message = messageQueue.poll();
//                    System.out.println(message);
                    messageCount++;
                    if (agents.get(message.getIdReceiver()).isRunning()){
                        agents.get(message.getIdReceiver()).addMessage(message);
                        int senderNccc = agents.get(message.getIdSender()).ncccs;
                        int receiverNcccc = agents.get(message.getIdReceiver()).ncccs;
                        ncccs = Integer.max(ncccs,senderNccc);
                        ncccs = Integer.max(ncccs,receiverNcccc);
                        agents.get(message.getIdReceiver()).ncccs = Integer.max(senderNccc + t,receiverNcccc);
                    }

                }
                boolean canTerminate = true;
                for (SyncAgent syncAgent : agents.values()){
                    if (syncAgent.isRunning()){
                        canTerminate = false;
                    }
                    else {
                        stoppedAgents.add(syncAgent);
                    }
                }
                if (recordCostInCycle) {
                    if (tail == costInCycle.length - 1){
                        expand();
                    }
                    for (Agent agent : stoppedAgents) {
                        sumCost += agent.getLocalCost();
                    }
                    costInCycle[tail] = sumCost / 2;
                }
                sumCost = 0;
                if (recordCostInCycle) {
                    Agent rootAgent = agents.get(1);
                    if (rootAgent instanceof ALSAgent) {
                        bestCostInCyle[tail] = ((ALSAgent) rootAgent).getBestCost();
                    }
                }
                tail++;
                for (CycleListener listener : cycleListeners){
                    listener.onCycleChanged(tail);
                }
                for (AgentIteratedOverListener listener : agentIteratedOverListeners){
                    listener.agentIteratedOver(agents);
                }
//                printCycle = true;
                if (printCycle){
                    System.out.println("cycle " + tail);
                }
                if (canTerminate){
                    stopProcess();
                }
                else {
                    cycleCount++;
                    agentReady.clear();
                    phase.set(PHASE_AGENT);
                }
            }
        }
    }

    public int getMessageCount() {
        return messageCount;
    }



    @Override
    public void postExecution() {

    }

    public synchronized void agentDone(int id){
        synchronized (agentReady){
            agentReady.add(id);
            sumCost += agents.get(id).getLocalCost();
            if (agentReady.size() == agents.size() - stoppedAgents.size()){
                phase.set(PHASE_MAILER);
            }
        }
    }

    public synchronized boolean isDone(int id){
        return agentReady.contains(id);
    }

    public synchronized int getPhase() {
        return phase.get();
    }

    public ResultCycle getResultCycle() {
        return resultCycle;
    }

    public synchronized void setResultCycle(int id,ResultCycle resultCycle){
        if (this.resultCycle == null){
            this.resultCycle = resultCycle;
        }
        else {
            this.resultCycle.add(resultCycle);
            this.resultCycle.setAgentValues(id,resultCycle.getAgentValue(id));
        }
        if (this.resultCycle.getAgents().size() == agents.size()){
            this.resultCycle.setTotalTime(new Date().getTime() - startTime);
            this.resultCycle.setMessageQuantity(messageCount);
            if (recordCostInCycle)
                this.resultCycle.setCostInCycle(costInCycle,tail);
            if (agents.get(1) instanceof ALSAgent){
                ((ResultAls) this.resultCycle).setBestCostInCycle(bestCostInCyle,tail);
            }
            this.resultCycle.setNcccs(ncccs);
            if (listener != null){
                listener.onFinished(this.resultCycle);
            }
        }
    }

    public interface CycleListener{
        void onCycleChanged(int cycle);
    }
}
