package il.ac.idc.yonatan.causality.mturk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import il.ac.idc.yonatan.causality.mturk.data.CausalityHitResult;
import il.ac.idc.yonatan.causality.mturk.data.CausalityQuestion;
import il.ac.idc.yonatan.causality.mturk.data.CauseAndAffect;
import il.ac.idc.yonatan.causality.mturk.data.DownHitResult;
import il.ac.idc.yonatan.causality.mturk.data.UpHitResult;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

@Service
@Controller
@Slf4j
public class HitManagerImpl implements HitManager {

    private static final File DB = new File("./hit-storage.json");

    private ObjectMapper objectMapper;

    @Autowired
    public HitManagerImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        HashFunction hf = Hashing.murmur3_32();
        log.info("Using mock db in {}", DB);

    }

    public void reset() {
        DB.delete();
    }

    @SneakyThrows
    private HitStorage readDb() {
        if (!DB.exists()) {
            return new HitStorage();
        }
        try (InputStream is = new FileInputStream(DB)) {
            return objectMapper.readValue(is, HitStorage.class);
        }
    }

    @SneakyThrows
    private void saveDb(HitStorage hitStorage) {
        try (OutputStream os = new FileOutputStream(DB)) {
            objectMapper.writeValue(os, hitStorage);
        }
    }


    @Data
    public static class HitStorage {
        private Map<String, UpHitResultStorage> upHits = new HashMap<>();
        private Map<String, DownHitResultStorage> downHits = new HashMap<>();
        private Map<String, CausalityResultStorage> causalityHits = new HashMap<>();
    }

    @Data
    @AllArgsConstructor()
    @NoArgsConstructor
    public static class CausalityResultStorage {
        @Setter(AccessLevel.NONE)
        private List<CausalityQuestion> causalityQuestions = new ArrayList<>();
        private String globalSummary;
        @Setter(AccessLevel.NONE)
        private CausalityHitResult causalityHitResult;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UpHitResultStorage {
        private UpHitResult upHitResult;
        private LinkedHashMap<String, List<String>> childIdToSummaries;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DownHitResultStorage {
        private DownHitResult downHitResult;
        private boolean leaf;
        private List<String> allRootSummaries;
        private List<String> nodeSummaries;
    }

    public String createUpHit(LinkedHashMap<String, List<String>> childIdToSummaries) {
        String hitId = "HIT-" + RandomStringUtils.randomAlphanumeric(8);
        HitStorage hitStorage = readDb();
        UpHitResult upHitResult = new UpHitResult();
        upHitResult.setHitDone(false);
        UpHitResultStorage upHitResultStorage = new UpHitResultStorage(upHitResult, childIdToSummaries);
        hitStorage.getUpHits().put(hitId, upHitResultStorage);
        saveDb(hitStorage);
        return hitId;
    }

    @Override
    public String createCausalityHit(String globalSummary, List<CausalityQuestion> causalityHitQuestions) {
        // This will:
        //present the global summary first
        //than will ask all questions in hit question (in real world - twice, and random causes order)
        HitStorage db = readDb();
        String hitId = "C_HIT-" + RandomStringUtils.randomAlphanumeric(8);
        CausalityResultStorage crs = new CausalityResultStorage(causalityHitQuestions, globalSummary, new CausalityHitResult());
        db.getCausalityHits().put(hitId, crs);
        saveDb(db);
        return hitId;
    }

    @Override
    public void submitCausalityHitReview(String hitId, boolean hitApproved, String reason) {
        HitStorage db = readDb();
        if (hitApproved) {
            db.getCausalityHits().remove(hitId);
        } else {
            CausalityHitResult causalityHitResult = db.getCausalityHits().get(hitId).getCausalityHitResult();
            causalityHitResult.setHitDone(false);
            causalityHitResult.getCauseAndAffects().clear();
        }
        saveDb(db);
    }

    public CausalityHitResult getCausalityHitForReview(String hitId) {
        return readDb().getCausalityHits().get(hitId).getCausalityHitResult();
    }


    public UpHitResult getUpHitForReview(String hitId) {
        log.debug("getUpHitForReview({})", hitId);
        return readDb().getUpHits().get(hitId).getUpHitResult();
    }

    public void submitReviewUpHit(String hitId, boolean hitApproved, String reason) {
        log.info("HitId {} approved? {}", hitId, hitApproved);
        HitStorage db = readDb();
        if (hitApproved) {
            db.getUpHits().remove(hitId);
        } else {
            UpHitResult hitResult = db.getUpHits().get(hitId).getUpHitResult();
            hitResult.setHitDone(false);
            hitResult.setHitSummary(null);
        }
        saveDb(db);
    }

    public DownHitResult getDownHitForReview(String hitId) {
        return readDb().getDownHits().get(hitId).getDownHitResult();
    }

    public void submitDownHitReview(String hitId, boolean hitApproved, String reason) {
        HitStorage db = readDb();
        if (hitApproved) {
            db.getDownHits().remove(hitId);
        } else {
            DownHitResult hitResult = db.getDownHits().get(hitId).getDownHitResult();
            hitResult.setHitDone(false);
            hitResult.setImportanceScore(null);
            hitResult.setMostImportantEvent(null);
        }
        saveDb(db);
    }

    @Override
    public String createDownHit(List<String> allRootSummaries, List<String> nodeSummaries, boolean isLeaf) {
        String hitId = "HIT_D-" + RandomStringUtils.randomAlphanumeric(8);
        HitStorage db = readDb();
        DownHitResult downHitResult = new DownHitResult();
        downHitResult.setHitDone(false);
        DownHitResultStorage downHitResultStorage = new DownHitResultStorage(
                downHitResult, isLeaf, allRootSummaries, nodeSummaries);
        db.getDownHits().put(hitId, downHitResultStorage);
        saveDb(db);
        return hitId;
    }

    @GetMapping("/hits")
    public String generateHitsView(Model model) {
        HitStorage hitStorage = readDb();
        model.addAttribute("hitStorage", hitStorage);
        return "hits";
    }

    @PostMapping("/hits")
    public String saveHits(@RequestParam Map<String, String> result) throws IOException {
        HitStorage hitStorage = readDb();

        processUpHits(hitStorage, result);
        processDownHits(hitStorage, result);
        processCausalityHits(hitStorage, result);
        saveDb(hitStorage);
        return "redirect:/hits";
    }

    private void processUpHits(HitStorage hitStorage, @RequestParam Map<String, String> result) {
        Set<String> hitIds = result.keySet().stream().filter(x -> x.endsWith("_hitsummary"))
                .map(x -> StringUtils.substringBeforeLast(x, "_hitsummary"))
                .collect(toSet());

        for (String hitId : hitIds) {
            UpHitResultStorage upHitResultStorage = hitStorage.getUpHits().get(hitId);
            UpHitResult upHitResult = upHitResultStorage.getUpHitResult();
            Set<String> childIds = upHitResultStorage.childIdToSummaries.keySet();
            boolean good = true;
            String summary = result.get(hitId + "_hitsummary");
            if (StringUtils.isEmpty(summary)) {
                continue;
            }
            upHitResult.setHitSummary(summary);
            for (String childId : childIds) {
                String idx = result.get(hitId + "_" + childId);
                if (idx != null) {
                    upHitResult.getChosenChildrenSummaries().put(childId, Integer.valueOf(idx));
                } else {
                    good = false;
                }
            }
            if (good) {
                upHitResult.setHitDone(true);
            }
        }
    }

    private void processDownHits(HitStorage hitStorage, Map<String, String> result) {

        Set<String> hitIds = result.keySet().stream()
                .filter(x -> x.startsWith("DOWN_"))
                .map(s -> StringUtils.substringAfter(s, "DOWN_"))
                .map(s -> StringUtils.substringBeforeLast(s, "_"))
                .collect(toSet());

        for (String hitId : hitIds) {
            DownHitResultStorage downHitResultStorage = hitStorage.getDownHits().get(hitId);
            Integer importanceScore = NumberUtils.createInteger(result.get("DOWN_" + hitId + "_score"));
            String mostImportantEvent = result.get("DOWN_" + hitId + "_event");
            if (importanceScore != null && StringUtils.isNotEmpty(mostImportantEvent)) {
                DownHitResult downHitResult = downHitResultStorage.getDownHitResult();
                downHitResult.setHitDone(true);
                downHitResult.setMostImportantEvent(mostImportantEvent);
                downHitResult.setImportanceScore(importanceScore);
            }
        }
    }

    private void processCausalityHits(HitStorage hitStorage, Map<String, String> result) {
        Set<String> causalityInputs = result.keySet().stream()
                .filter(x -> x.startsWith("CAUS_"))
                .collect(toSet());
        Set<String> resetHits = new HashSet<>();

        Set<String> seenQueryNodeIds = new HashSet<>();
        for (String causalityInputId : causalityInputs) {
            String hitId = StringUtils.substringBefore(
                    StringUtils.substringAfter(causalityInputId, "CAUS_"),
                    ":");
            log.debug("Processing causality hit {}", hitId);
            CausalityResultStorage causalityResultStorage = hitStorage.getCausalityHits().get(hitId);

            String response = result.get(causalityInputId);
            boolean causality = BooleanUtils.toBoolean(response);

            String causeAndAffectIds = StringUtils.substringAfter(causalityInputId, ":");
            String queryNodeId = StringUtils.substringBefore(causeAndAffectIds, ":");

            String causeId = StringUtils.substringAfter(causeAndAffectIds, ":");
            CauseAndAffect causeAndAffect = new CauseAndAffect(causeId, queryNodeId);
            CausalityHitResult causalityHitResult = causalityResultStorage.getCausalityHitResult();
            if (!resetHits.contains(hitId)) {
                // Clear the hit result before re-save it
                causalityHitResult.getCauseAndAffects().clear();
                causalityHitResult.getNonCauseAndAffects().clear();
                resetHits.add(hitId);
            }
            if (causality) {
                log.debug("Adding cause {}", causeAndAffect);
                causalityHitResult.getCauseAndAffects().add(causeAndAffect);
            } else {
                log.debug("Adding noncause {}", causeAndAffect);
                causalityHitResult.getNonCauseAndAffects().add(causeAndAffect);
            }
            causalityHitResult.setHitDone(true);
        }
        saveDb(hitStorage);
    }


}
