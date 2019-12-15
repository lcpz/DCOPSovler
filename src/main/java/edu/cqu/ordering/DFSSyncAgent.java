package edu.cqu.ordering;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cqu.core.Message;
import edu.cqu.core.SyncAgent;
import edu.cqu.core.SyncMailer;

public abstract class DFSSyncAgent extends SyncAgent{

    private static final int MSG_DEGREE = 0XFFFF0;
    private static final int MSG_DFS = 0XFFFF1;
    private static final int MSG_DFS_BACKTRACK = 0XFFFF2;
    private static final int MSG_START = 0XFFFF3;

    protected Map<Integer,Integer> degreeView;
    protected List<Map.Entry<Integer,Integer>> orderedDegree;
    protected int parent;
    protected List<Integer> children;
    private int currentChildIndex;
    protected List<Integer> pseudoParents;
    protected int level;
    protected int height;
    private int maxSubHeight;

    public DFSSyncAgent(int id, int[] domain, int[] neighbours, Map<Integer, int[][]> constraintCosts, Map<Integer, int[]> neighbourDomains, SyncMailer mailer) {
        super(id, domain, neighbours, constraintCosts, neighbourDomains, mailer);
        degreeView = new HashMap<>();
        children = new LinkedList<>();
        pseudoParents = new LinkedList<>();
        parent = -1;
    }

    protected boolean isRootAgent(){
        return parent <= 0;
    }

    protected boolean isLeafAgent(){
        return children.size() == 0;
    }

    @Override
    protected void initRun() {
        for (int neighbourId : neighbours){
            sendMessage(new Message(id,neighbourId,MSG_DEGREE,neighbours.length));
        }
    }

    @Override
    public void runFinished() {

    }

    @Override
    public void disposeMessage(Message message) {
        switch (message.getType()){
            case MSG_DEGREE:
                degreeView.put(message.getIdSender(),(int)message.getValue());
                if (degreeView.size() == neighbours.length){
                    orderedDegree = new ArrayList<>(degreeView.entrySet());
                    orderedDegree.sort(new Comparator<Map.Entry<Integer, Integer>>() {
                        @Override
                        public int compare(Map.Entry<Integer, Integer> o1, Map.Entry<Integer, Integer> o2) {
                            return o2.getValue().compareTo(o1.getValue());
                        }
                    });
                    if (id == 1){
                        HashSet<Integer> visited = new HashSet<>();
                        visited.add(id);
                        parent = -1;
                        level = 0;
                        children.add(orderedDegree.get(0).getKey());

                        sendMessage(new Message(id,orderedDegree.get(0).getKey(),MSG_DFS,new DFSMessageContent(visited,level)));
                    }
                }
                break;
            case MSG_DFS: {
//                System.out.println(message);
                DFSMessageContent content = (DFSMessageContent) message.getValue();
                Set<Integer> visited = content.visited;
                level = content.level + 1;
                visited.add(id);
                parent = message.getIdSender();
                int selectedChild = 0;
                for (int i = 0; i < orderedDegree.size(); i++) {
                    if (visited.contains(orderedDegree.get(i).getKey())) {
                        if (orderedDegree.get(i).getKey() != parent){
                            pseudoParents.add(orderedDegree.get(i).getKey());
                        }
                    }
                }
                for (int i = 0; i < orderedDegree.size(); i++) {
                    if (visited.contains(orderedDegree.get(i).getKey())) {
                        continue;
                    }
                    selectedChild = orderedDegree.get(i).getKey();
                    currentChildIndex = i;
                    break;
                }
                if (selectedChild != 0) {
                    children.add(selectedChild);
                    sendMessage(new Message(id, selectedChild, MSG_DFS, new DFSMessageContent(visited,level)));
                }
                else {
                    height = 0;
                    sendMessage(new Message(id, parent, MSG_DFS_BACKTRACK, new BacktrackMessageContent(visited,height)));
                }

                break;
            }
            case MSG_DFS_BACKTRACK:
                BacktrackMessageContent content = (BacktrackMessageContent) message.getValue();
                Set<Integer> visited = content.visited;
                if (content.height > maxSubHeight){
                    maxSubHeight = content.height;
                }
                int selectedChild = 0;
                for (int i = currentChildIndex + 1; i < orderedDegree.size(); i++){
                    if (visited.contains(orderedDegree.get(i).getKey())) {
                        continue;
                    }
                    selectedChild = orderedDegree.get(i).getKey();
                    currentChildIndex = i;
                    break;
                }
                if (selectedChild != 0){
                    children.add(selectedChild);
                    sendMessage(new Message(id, selectedChild, MSG_DFS, new DFSMessageContent(visited,level)));
                }
                else {
                     height = maxSubHeight + 1;
                    if (id != 1) {
                        sendMessage(new Message(id, parent, MSG_DFS_BACKTRACK, new BacktrackMessageContent(visited,height)));
                    }
                    else {
                        for (int childId : children){
                            sendMessage(new Message(id,childId,MSG_START,null));
                        }
                        pseudoTreeCreated();
                    }
                }
                break;
            case MSG_START:
                for (int childId : children){
                    sendMessage(new Message(id,childId,MSG_START,null));
                }
                pseudoTreeCreated();
                break;
        }
    }

    protected abstract void pseudoTreeCreated();
    private class DFSMessageContent{
        Set<Integer> visited;
        int level;

        public DFSMessageContent(Set<Integer> visited, int level) {
            this.visited = visited;
            this.level = level;
        }
    }

    private class BacktrackMessageContent{
        Set<Integer> visited;
        int height;

        public BacktrackMessageContent(Set<Integer> visited, int height) {
            this.visited = visited;
            this.height = height;
        }
    }

    public String toDOTString(){
        StringBuilder stringBuilder = new StringBuilder();
        if (parent > 0){
            stringBuilder.append("X" + parent + "->X" + id + ";\n");
        }
        for (int pp : pseudoParents){
            stringBuilder.append("X" + pp + "->X" + id + " [style=dotted];\n");
        }
        return stringBuilder.toString();
    }
}
