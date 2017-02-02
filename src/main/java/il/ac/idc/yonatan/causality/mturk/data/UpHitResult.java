package il.ac.idc.yonatan.causality.mturk.data;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ygraber on 1/28/17.
 */
@Data
public class UpHitResult {
    private boolean hitDone;

    private String hitSummary;

    private Map<String, Integer> chosenChildrenSummaries = new HashMap<>();
}
