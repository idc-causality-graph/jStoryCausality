package il.ac.idc.yonatan.causality.mturk;

import il.ac.idc.yonatan.causality.mturk.data.DownHitResult;
import il.ac.idc.yonatan.causality.mturk.data.UpHitResult;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created by ygraber on 1/28/17.
 */
public interface HitManager {
    String createUpHit(LinkedHashMap<String, List<String>> childIdToSummaries);

    String createDownHit(String summary, List<String> childrenSummaries);

    UpHitResult getUpHitForReview(String hitId);

    DownHitResult getDownHitForReview(String hitId);

    void submitReviewUpHit(String hitId, boolean hitApproved, String reason);

    void submitDownHitReview(String hitId, boolean hitApproved, String reason);

}
