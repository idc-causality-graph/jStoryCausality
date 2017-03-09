package il.ac.idc.yonatan.causality.contexttree;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.google.common.collect.Sets;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

@Data
@JsonAutoDetect(getterVisibility = NONE, setterVisibility = NONE, isGetterVisibility = NONE, fieldVisibility = ANY)
public class ContextTree {

    public ContextTree() {
        Node.contextTree = this;
        NodeLevel.contextTree = this;
    }

    private String rootNodeId;
    @Setter(AccessLevel.NONE)
    private List<String> textFragments = new ArrayList<>();
    @Setter(AccessLevel.NONE)
    private Map<String, Node> nodeRepository = new HashMap<>();
    @Setter(AccessLevel.NONE)
    private LinkedList<NodeLevel> nodeLevels = new LinkedList<>();
    @Setter(AccessLevel.NONE)
    private List<NodeLevel> prunedNodeLevels = new ArrayList<>();
    @Setter(AccessLevel.NONE)
    private Set<String> causalityHits = new HashSet<>();
    @Setter(AccessLevel.NONE)
    private Set<String> completedCausalityHits = new HashSet<>();
    @Setter(AccessLevel.NONE)
    private Map<String,Set<String>> completedCausalityAssignmentsByHit = new HashMap<>();
    private int upLevelStep;
    private Phases phase;
    private int causalityLevelStep;

    public Node getNode(String nodeId) {
        return nodeRepository.get(nodeId);
    }

    public Node getRootNode() {
        return getNode(rootNodeId);
    }

    public NodeLevel getLeafNodeLevel() {
        return nodeLevels.getFirst();
    }

    public NodeLevel getMetaLeafNodeLevel(){
        return nodeLevels.get(1);
    }


    public Collection<Node> getAllNodes() {
        return getNodeRepository().values();
    }

    public NodeLevel getPrunedLeafNodeLevel() {
        if (prunedNodeLevels.isEmpty()) {
            return null;
        }
        return prunedNodeLevels.get(0);
    }

    public Set<String> getUncompletedCausalityHits() {
        return Sets.difference(causalityHits, completedCausalityHits).immutableCopy();
    }

    public boolean areDownPhaseHitCreated(){
        return nodeRepository.values().stream().anyMatch(node->node.getDownHitId()!=null);
    }

}
