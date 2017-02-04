package il.ac.idc.yonatan.causality.contexttree;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static java.util.stream.Collectors.toList;

/**
 * Created by ygraber on 1/28/17.
 */
@Data
@JsonAutoDetect(getterVisibility = NONE, setterVisibility = NONE, isGetterVisibility = NONE, fieldVisibility = ANY)
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
