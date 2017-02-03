package il.ac.idc.yonatan.causality.contexttree;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by ygraber on 1/28/17.
 */
@Data
public class ContextTree {

    private String rootNodeId;
    private List<String> textFragments = new ArrayList<>();
    private Map<String, Node> nodeRepository = new HashMap<>();
    private LinkedList<NodeLevel> nodeLevels = new LinkedList<>();
    private int upLevelStep;
    private Phases phase;

}
