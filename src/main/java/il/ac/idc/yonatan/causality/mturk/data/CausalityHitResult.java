package il.ac.idc.yonatan.causality.mturk.data;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

@Data
public class CausalityHitResult {


    private String assignmentId;
    /**
     * Contains the cause node id->query node id pairs that are a causality edge in the graph
     */
    @Setter(AccessLevel.NONE)
    private Set<CauseAndAffect> causeAndAffects = new HashSet<>();

    /**
     * Return a set of QUERY_NODE_ID:CAUSE_NODE_ID strings (i.e. ["1234:5646","1234:5222"])
     *
     * @return
     */
    public Set<String> getCauseNodeIds() {
        return causeAndAffects.stream()
                .map(caf -> caf.getAffectNodeId() + ":" + caf.getCauseNodeId())
                .collect(toSet());
    }

    /**
     * Contains the cause node id->query node id pairs that are not an edge in the causality graph
     */
    @Setter(AccessLevel.NONE)
    private Set<CauseAndAffect> nonCauseAndAffects = new HashSet<>();
}
