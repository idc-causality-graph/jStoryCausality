package il.ac.idc.yonatan.causality.mturk;

import il.ac.idc.yonatan.causality.mturk.data.CausalityHitResult;
import il.ac.idc.yonatan.causality.mturk.data.CausalityQuestion;
import il.ac.idc.yonatan.causality.mturk.data.DownHitResult;
import il.ac.idc.yonatan.causality.mturk.data.UpHitResult;
import org.apache.commons.lang3.tuple.Pair;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;

public interface HitManager {
    boolean isSandbox();

    String createUpHit(LinkedHashMap<String, List<String>> childIdToSummaries, int replicas);

    List<UpHitResult> getUpHitForReview(String hitId);

    List<DownHitResult> getDownHitForReview(String hitId);

    List<CausalityHitResult> getCausalityHitForReview(String hitId);

    void submitReviewUpHit(String hitId, String assignmentId, boolean hitApproved, String reason);

    void submitDownHitReview(String hitId, String assignmentId, boolean hitApproved, String reason);

    void submitCausalityHitReview(String hitId, String assignmentId, boolean hitApproved, String reason);

    String createDownHit(List<String> parentsSummaries, List<Pair<String, String>> childrenIdsAndSummaries,
                         boolean isLeaf, int replicas);

    void reset();

    String createCausalityHit(String globalSummary, List<CausalityQuestion> causalityHitQuestions);

    BigDecimal getAccountBalance();

}
