package il.ac.idc.yonatan.causality.contexttree;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * Created by ygraber on 1/28/17.
 */
@Data
public class NodeLevel {
    static ContextTree contextTree;
    private List<String> nodeIds = new ArrayList<>();

    public List<Node> getNodes() {
        return nodeIds.stream().map(this::getNode).collect(toList());
    }

    private Node getNode(String nodeId) {
        return contextTree.getNodeRepository().get(nodeId);
    }
}
