package il.ac.idc.yonatan.causality.contexttree;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

/**
 * Created by ygraber on 2/5/17.
 */
@Data
@JsonAutoDetect(getterVisibility = NONE, setterVisibility = NONE, isGetterVisibility = NONE, fieldVisibility = ANY)
public class CausalityData {

    static ContextTree contextTree;

    private String nodeId;

    /**
     * A potentially important node is a node that is a leaf and marked as important, or it is not a leaf but has at
     * least one of his child is marked as important
     */
    private boolean potentiallyImportant;

    @Setter(AccessLevel.NONE)
    private List<String> hits = new ArrayList<>();

    @Setter(AccessLevel.NONE)
    private Set<String> completedHits = new HashSet<>();

    @Setter(AccessLevel.NONE)
    private Multiset<String> targetNodeIds = HashMultiset.create();

    public boolean isLevelCompleted() {
        return hits.size() == completedHits.size();
    }

    public Node getNode() {
        return contextTree.getNode(nodeId);
    }
}
