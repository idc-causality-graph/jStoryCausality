package il.ac.idc.yonatan.causality.contexttree;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

import java.util.Set;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

@Data
@JsonAutoDetect(getterVisibility = NONE, setterVisibility = NONE, isGetterVisibility = NONE, fieldVisibility = ANY)
public class CausalityData {

    /**
     * A potentially important node is a node that is a leaf and marked as important, or it is not a leaf but has at
     * least one of his child is marked as important
     */
    private boolean potentiallyImportant;

    /**
     * A list of node ids (query nodes) that this node is a cause for.
     */
    @Setter(AccessLevel.NONE)
    private Multiset<String> targetNodeIds = HashMultiset.create();

    public Set<Multiset.Entry<String>> getTargetNodeIdEntrySet() {
        return targetNodeIds.entrySet();
    }

}
