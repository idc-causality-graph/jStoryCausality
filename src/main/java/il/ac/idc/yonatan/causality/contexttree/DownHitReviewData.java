package il.ac.idc.yonatan.causality.contexttree;

import lombok.Data;
import org.apache.commons.lang3.tuple.Triple;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by ygraber on 2/2/17.
 */
@Data
public class DownHitReviewData {
    private boolean hitDone;
    private String hitId;
    private String nodeId;
    private List<String> parentsSummaries;
    private Map<String, String> childIdToSummary;
    private List<Triple<String, Integer, String>> idsAndScoresAndEvents;

    /**
     * Encodes the triples to list of base64, delimited by ':'.
     * Each base64 contains a triple, also delimited by ':'
     * The event (the right part of the triple) might contain ':', but the decoder should ignore that.
     * @return
     */
    public String getEncodedData(){
        if (idsAndScoresAndEvents==null){
            return null;
        }
        Base64.Encoder base64Encoder = Base64.getEncoder();
        return idsAndScoresAndEvents.stream()
                .map(triple -> triple.getLeft() + ":" + triple.getMiddle() + ":" + triple.getRight())
                .map(String::getBytes)
                .map(bytes -> base64Encoder.encodeToString(bytes))
                .collect(Collectors.joining(":"));
    }

}
