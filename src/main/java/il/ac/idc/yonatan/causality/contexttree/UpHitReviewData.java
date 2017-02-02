package il.ac.idc.yonatan.causality.contexttree;

import lombok.Data;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ygraber on 1/29/17.
 */
@Data
public class UpHitReviewData {
    private String hitId;
    private String summary;
    private String taskText;
    private String nodeId;
    private String chosenChildrenSummariesJsonBase64;
    public String getSummaryBase64(){
        if (summary==null){
            return "";
        }
        return Base64.getEncoder().encodeToString(summary.getBytes());
    }

    /**
     * Is hit done, or still needed to be completed?
     */
    private boolean hitDone;
}
