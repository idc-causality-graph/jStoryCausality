package il.ac.idc.yonatan.causality.contexttree;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

/**
 * Created by ygraber on 1/28/17.
 */
@Data
@JsonAutoDetect(getterVisibility = NONE,setterVisibility = NONE,isGetterVisibility = NONE,fieldVisibility = ANY)
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
    private int upLevelStep;
    private Phases phase;

    public Node getNode(String nodeId) {
        return nodeRepository.get(nodeId);
    }

    public Node getRootNode() {
        return getNode(rootNodeId);
    }

    public Collection<Node> getAllNodes() {
        return getNodeRepository().values();
    }


}
