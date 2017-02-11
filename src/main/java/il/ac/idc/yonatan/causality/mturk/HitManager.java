package il.ac.idc.yonatan.causality.mturk;

import il.ac.idc.yonatan.causality.mturk.data.CausalityHitResult;
import il.ac.idc.yonatan.causality.mturk.data.CausalityQuestion;
import il.ac.idc.yonatan.causality.mturk.data.DownHitResult;
import il.ac.idc.yonatan.causality.mturk.data.UpHitResult;

import java.util.LinkedHashMap;
import java.util.List;

public interface HitManager {
    String createUpHit(LinkedHashMap<String, List<String>> childIdToSummaries);

    UpHitResult getUpHitForReview(String hitId);

    DownHitResult getDownHitForReview(String hitId);

    CausalityHitResult getCausalityHitForReview(String hitId);

    void submitReviewUpHit(String hitId, boolean hitApproved, String reason);

    void submitDownHitReview(String hitId, boolean hitApproved, String reason);

    void submitCausalityHitReview(String hitId, boolean hitApproved, String reason);

    String createDownHit(List<String> allRootSummaries, List<String> nodeSummaries, boolean isLeaf);

    void reset();

    String createCausalityHit(String globalSummary, List<CausalityQuestion> causalityHitQuestions);

}
