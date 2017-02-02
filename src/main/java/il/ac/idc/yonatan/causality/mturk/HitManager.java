package il.ac.idc.yonatan.causality.mturk;

import com.google.common.collect.ListMultimap;
import il.ac.idc.yonatan.causality.mturk.data.UpHitResult;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created by ygraber on 1/28/17.
 */
public interface HitManager {
    String createUpHit(LinkedHashMap<String, List<String>> childIdToSummaries);

    UpHitResult getUpHitForReview(String hitId);

    void submitReviewUpHit(String hitId, boolean hitApproved, String reason);
}
