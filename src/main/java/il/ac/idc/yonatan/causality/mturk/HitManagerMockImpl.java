package il.ac.idc.yonatan.causality.mturk;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;
import il.ac.idc.yonatan.causality.config.AppConfig;
import il.ac.idc.yonatan.causality.mturk.data.CausalityHitResult;
import il.ac.idc.yonatan.causality.mturk.data.CausalityQuestion;
import il.ac.idc.yonatan.causality.mturk.data.CauseAndAffect;
import il.ac.idc.yonatan.causality.mturk.data.DownHitResult;
import il.ac.idc.yonatan.causality.mturk.data.IdScoreAndEvent;
import il.ac.idc.yonatan.causality.mturk.data.UpHitResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import mturk.wsdl.AssignmentStatus;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

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

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/**
 * Created by ygraber on 2/25/17.
 */
@Profile("mockAws")
@Service
@Controller
public class HitManagerMockImpl implements HitManager {

    private static final String DOWN_HIT_PREFIX = "DOWNHIT-";
    private static final String UP_HIT_PREFIX = "DOWNHIT-";
    private static final String CAUS_HIT_PREFIX = "CAUSHIT-";

    private static final File DB = new File("./hit-storage.json");
    private final AppConfig appConfig;
    private final ObjectMapper objectMapper;

    @Data
    public static class HitStorage {
        private Map<String, UpHitStorage> upHitStorage = new HashMap<>();
        private Map<String, DownHitStorage> downHitStorage = new HashMap<>();
        private Map<String, CausalityHitStorage> causalityHitStorage = new HashMap<>();
    }

    @Data
    public static class UpHitStorage {
        // assignments
        private List<UpAssignment> upAssignments = new ArrayList<>();
        private LinkedHashMap<String, List<String>> childIdToSummaries;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonAutoDetect(getterVisibility = NONE, setterVisibility = NONE, isGetterVisibility = NONE, fieldVisibility = ANY)
    public static class UpAssignment {
        private UpHitResult upHitResult;
        private AssignmentStatus assignmentStatus;

        public boolean isDone() {
            return assignmentStatus == AssignmentStatus.SUBMITTED;
        }

        public boolean isClosed() {
            return assignmentStatus == AssignmentStatus.APPROVED || assignmentStatus == AssignmentStatus.REJECTED;
        }
    }

    @Data
    public static class DownHitStorage {
        private List<DownAssignment> downAssignments = new ArrayList<>();
        private List<String> parentsSummaries;
        private List<Pair<String, String>> childrenIdsAndSummaries;
        private boolean isLeaf;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonAutoDetect(getterVisibility = NONE, setterVisibility = NONE, isGetterVisibility = NONE, fieldVisibility = ANY)
    public static class DownAssignment {
        private DownHitResult downHitResult;
        private AssignmentStatus assignmentStatus;

        public boolean isDone() {
            return assignmentStatus == AssignmentStatus.SUBMITTED;
        }

        public boolean isClosed() {
            return assignmentStatus == AssignmentStatus.APPROVED || assignmentStatus == AssignmentStatus.REJECTED;
        }

    }

    @Data
    public static class CausalityHitStorage {
        private String globalSummary;
        private List<CausalityQuestion> causalityHitQuestions;
        private List<CausalityAssignment> causalityAssignments = new ArrayList<>();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonAutoDetect(getterVisibility = NONE, setterVisibility = NONE, isGetterVisibility = NONE, fieldVisibility = ANY)
    public static class CausalityAssignment {
        private CausalityHitResult causalityHitResult;
        private AssignmentStatus assignmentStatus;

        public boolean isDone() {
            return assignmentStatus == AssignmentStatus.SUBMITTED;
        }

        public boolean isClosed() {
            return assignmentStatus == AssignmentStatus.APPROVED || assignmentStatus == AssignmentStatus.REJECTED;
        }

    }

    @Autowired
    public HitManagerMockImpl(ObjectMapper objectMapper, AppConfig appConfig) {
        this.appConfig = appConfig;
        this.objectMapper = objectMapper;
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

    private UpAssignment createUpAssignment() {
        String assignmentId = "AID-" + RandomStringUtils.randomAlphanumeric(8);
        UpHitResult upHitResult = new UpHitResult();
        upHitResult.setAssignmentId(assignmentId);
        return new UpAssignment(upHitResult, null);
    }

    @Override
    public String createUpHit(LinkedHashMap<String, List<String>> childIdToSummaries) {
        HitStorage db = readDb();
        String hitId = UP_HIT_PREFIX + RandomStringUtils.randomAlphanumeric(8);
        UpHitStorage upHitStorage = new UpHitStorage();
        upHitStorage.setChildIdToSummaries(childIdToSummaries);
        db.getUpHitStorage().put(hitId, upHitStorage);
        for (int i = 0; i < appConfig.getReplicationFactor(); i++) {
            upHitStorage.getUpAssignments().add(createUpAssignment());
        }
        saveDb(db);
        return hitId;
    }

    @Override
    public List<UpHitResult> getUpHitForReview(String hitId) {
        HitStorage db = readDb();
        UpHitStorage upHitStorage = db.getUpHitStorage().get(hitId);
        List<UpAssignment> upAssignments = upHitStorage.getUpAssignments();
        return upAssignments.stream()
                .filter(x -> x.getAssignmentStatus() == AssignmentStatus.SUBMITTED)
                .map(UpAssignment::getUpHitResult)
                .collect(toList());
    }

    @Override
    public void submitReviewUpHit(String hitId, String assignmentId, boolean hitApproved, String reason) {
        HitStorage db = readDb();
        UpHitStorage upHitStorage = db.getUpHitStorage().get(hitId);
        List<UpAssignment> upAssignments = upHitStorage.getUpAssignments();
        UpAssignment upAssignment = upAssignments.stream().filter(x -> x.getUpHitResult().getAssignmentId().equals(assignmentId))
                .findFirst().get();
        if (hitApproved) {
            upAssignment.setAssignmentStatus(AssignmentStatus.APPROVED);
        } else {
            upAssignment.setAssignmentStatus(AssignmentStatus.REJECTED);
            upAssignments.add(createUpAssignment());
        }
        saveDb(db);
    }

    private DownAssignment createDownAssignment() {
        String assignmentId = "AID-" + RandomStringUtils.randomAlphanumeric(8);
        DownHitResult downHitResult = new DownHitResult();
        downHitResult.setAssignmentId(assignmentId);
        return new DownAssignment(downHitResult, null);
    }

    @Override
    public String createDownHit(List<String> parentsSummaries, List<Pair<String, String>> childrenIdsAndSummaries, boolean isLeaf) {
        HitStorage db = readDb();
        String hitId = DOWN_HIT_PREFIX + RandomStringUtils.randomAlphanumeric(8);

        DownHitStorage downHitStorage = new DownHitStorage();
        db.getDownHitStorage().put(hitId, downHitStorage);
        downHitStorage.setChildrenIdsAndSummaries(childrenIdsAndSummaries);
        downHitStorage.setLeaf(isLeaf);
        downHitStorage.setParentsSummaries(parentsSummaries);

        for (int i = 0; i < appConfig.getReplicationFactor(); i++) {
            downHitStorage.getDownAssignments().add(createDownAssignment());
        }
        saveDb(db);
        return hitId;
    }

    @Override
    public List<DownHitResult> getDownHitForReview(String hitId) {
        HitStorage db = readDb();
        DownHitStorage downHitStorage = db.getDownHitStorage().get(hitId);
        List<DownAssignment> downAssignments = downHitStorage.getDownAssignments();
        return downAssignments.stream()
                .filter(x -> x.getAssignmentStatus() == AssignmentStatus.SUBMITTED)
                .map(DownAssignment::getDownHitResult)
                .collect(toList());
    }

    @Override
    public void submitDownHitReview(String hitId, String assignmentId, boolean hitApproved, String reason) {
        HitStorage db = readDb();
        DownHitStorage downHitStorage = db.getDownHitStorage().get(hitId);
        List<DownAssignment> downAssignments = downHitStorage.getDownAssignments();
        DownAssignment downAssignment = downAssignments.stream().filter(x -> x.getDownHitResult().getAssignmentId().equals(assignmentId))
                .findFirst().get();
        if (hitApproved) {
            downAssignment.setAssignmentStatus(AssignmentStatus.APPROVED);
        } else {
            downAssignment.setAssignmentStatus(AssignmentStatus.REJECTED);
            downAssignments.add(createDownAssignment());
        }
        saveDb(db);
    }

    private CausalityAssignment createCausalityAssignment() {
        String assignmentId = "AID-" + RandomStringUtils.randomAlphanumeric(8);
        CausalityHitResult causalityHitResult = new CausalityHitResult();
        causalityHitResult.setAssignmentId(assignmentId);
        return new CausalityAssignment(causalityHitResult, null);
    }

    @Override
    public String createCausalityHit(String globalSummary, List<CausalityQuestion> causalityHitQuestions) {
        HitStorage db = readDb();
        String hitId = CAUS_HIT_PREFIX + RandomStringUtils.randomAlphanumeric(8);

        CausalityHitStorage causalityHitStorage = new CausalityHitStorage();
        db.getCausalityHitStorage().put(hitId, causalityHitStorage);
        causalityHitStorage.setCausalityHitQuestions(causalityHitQuestions);
        causalityHitStorage.setGlobalSummary(globalSummary);

        for (int i = 0; i < appConfig.getCausalityReplicaFactor(); i++) {
            causalityHitStorage.getCausalityAssignments().add(createCausalityAssignment());
        }
        saveDb(db);
        return hitId;
    }

    @Override
    public void submitCausalityHitReview(String hitId, String assignmentId, boolean hitApproved, String reason) {
        HitStorage db = readDb();
        CausalityHitStorage causalityHitStorage = db.getCausalityHitStorage().get(hitId);
        List<CausalityAssignment> causalityAssignments = causalityHitStorage.getCausalityAssignments();
        CausalityAssignment causalityAssignment = causalityAssignments.stream()
                .filter(x -> x.getCausalityHitResult().getAssignmentId().equals(assignmentId))
                .findFirst().get();
        if (hitApproved) {
            causalityAssignment.setAssignmentStatus(AssignmentStatus.APPROVED);
        } else {
            causalityAssignment.setAssignmentStatus(AssignmentStatus.REJECTED);
            causalityAssignments.add(createCausalityAssignment());
        }
        saveDb(db);
    }

    @Override
    public List<CausalityHitResult> getCausalityHitForReview(String hitId) {
        HitStorage db = readDb();
        CausalityHitStorage causalityHitStorage = db.getCausalityHitStorage().get(hitId);
        List<CausalityAssignment> causalityAssignments = causalityHitStorage.getCausalityAssignments();
        return causalityAssignments.stream()
                .filter(x -> x.getAssignmentStatus() == AssignmentStatus.SUBMITTED)
                .map(CausalityAssignment::getCausalityHitResult)
                .collect(toList());
    }

    @Override
    public void reset() {
        DB.delete();
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
        Set<String> hitAssignmentIds = result.keySet().stream()
                .filter(x -> x.startsWith(UP_HIT_PREFIX))
                .filter(x -> x.endsWith("_hitsummary"))
                .map(x -> StringUtils.substringBeforeLast(x, "_hitsummary"))
                .collect(toSet());

        for (String hitAssignmentId : hitAssignmentIds) {

            String hitId = StringUtils.substringBefore(hitAssignmentId, ":");
            String assignmentId = StringUtils.substringAfter(hitAssignmentId, ":");

            UpHitStorage upHitStorage = hitStorage.getUpHitStorage().get(hitId);
            List<UpAssignment> assignments = upHitStorage.getUpAssignments();
            UpAssignment upAssignment = assignments.stream()
                    .filter(x -> x.getUpHitResult().getAssignmentId().equals(assignmentId))
                    .findFirst().get();
            String hitSummary = result.get(hitAssignmentId + "_hitsummary");
            boolean good = true;
            if (StringUtils.isEmpty(hitSummary)) {
                continue;
            }
            UpHitResult upHitResult = upAssignment.getUpHitResult();
            upHitResult.setHitSummary(hitSummary);
            Set<String> childIds = upHitStorage.getChildIdToSummaries().keySet();
            for (String childId : childIds) {

                String idx = result.get(hitAssignmentId + "_" + childId);
                if (idx != null) {
                    upHitResult.getChosenChildrenSummaries().put(childId, Integer.valueOf(idx));
                } else {
                    good = false;
                }
            }
            if (good) {
                upAssignment.setAssignmentStatus(AssignmentStatus.SUBMITTED);
            }
        }
    }


    private static String findHitIdForHitDownFieldKey(String key) {
        key = StringUtils.substringAfter(key, DOWN_HIT_PREFIX);
        key = StringUtils.substringBeforeLast(key, "_");
        key = StringUtils.substringBeforeLast(key, "_");
        return key;
    }

    private void processDownHits(HitStorage hitStorage, Map<String, String> result) {
        Set<String> hitAssignmentIds = result.keySet().stream()
                .filter(x -> x.startsWith(DOWN_HIT_PREFIX))
                .map(HitManagerMockImpl::findHitIdForHitDownFieldKey)
//                .map(s -> StringUtils.substringAfter(s, "DOWN_"))
//                .map(s -> StringUtils.substringBeforeLast(s, "_"))
                .collect(toSet());

        for (String hitAssignmentId : hitAssignmentIds) {
            String hitId = StringUtils.substringBefore(hitAssignmentId, ":");
            String assignmentId = StringUtils.substringAfter(hitAssignmentId, ":");

            DownHitStorage downHitStorage = hitStorage.getDownHitStorage().get(hitId);

            List<String> scoreFields = result.keySet().stream()
                    .filter(name -> name.startsWith(DOWN_HIT_PREFIX + hitAssignmentId + "_score_"))
                    .collect(toList());

            List<IdScoreAndEvent> idScoreAndEvents = new ArrayList<>();
            boolean completed = true;
            for (String scoreField : scoreFields) {
                String idx = StringUtils.substringAfterLast(scoreField, "_");
                Integer score = NumberUtils.createInteger(result.get(scoreField));
                String nodeId = result.get(DOWN_HIT_PREFIX + hitId + "_nodeid_" + idx);
                String event = result.get(DOWN_HIT_PREFIX + hitId + "_event_" + idx);
                if (score != null && nodeId != null && StringUtils.isNotEmpty(event)) {
                    idScoreAndEvents.add(new IdScoreAndEvent(nodeId, score, event));
                } else {
                    completed = false;
                }
            }
            if (completed && !idScoreAndEvents.isEmpty()) {
                DownAssignment downAssignment = downHitStorage.getDownAssignments().stream()
                        .filter(x -> x.getDownHitResult().getAssignmentId().equals(assignmentId))
                        .findFirst().get();
                downAssignment.getDownHitResult().setIdsAndScoresAndEvents(idScoreAndEvents);
                downAssignment.setAssignmentStatus(AssignmentStatus.SUBMITTED);
            }
        }
    }

    private void processCausalityHits(HitStorage hitStorage, Map<String, String> result) {
        Set<String> causalityInputs = result.keySet().stream()
                .filter(x -> x.startsWith(CAUS_HIT_PREFIX))
                .collect(toSet());
        Set<String> resetHits = new HashSet<>();

        Set<String> seenQueryNodeIds = new HashSet<>();
        for (String causalityInputId : causalityInputs) {
            String hitAssignmentId = StringUtils.substringBefore(
                    StringUtils.substringAfter(causalityInputId, CAUS_HIT_PREFIX),
                    ";");
            String hitId = StringUtils.substringBefore(hitAssignmentId, ":");
            String assignmentId = StringUtils.substringAfter(hitAssignmentId, ":");
//            log.debug("Processing causality hit {}", hitId);
            CausalityHitStorage causalityHitStorage = hitStorage.getCausalityHitStorage().get(hitId);


            String response = result.get(causalityInputId);
            boolean causality = BooleanUtils.toBoolean(response);

            String causeAndAffectIds = StringUtils.substringAfter(causalityInputId, ":");
            String queryNodeId = StringUtils.substringBefore(causeAndAffectIds, ":");

            String causeId = StringUtils.substringAfter(causeAndAffectIds, ":");
            CauseAndAffect causeAndAffect = new CauseAndAffect(causeId, queryNodeId);
            CausalityAssignment causalityAssignment = causalityHitStorage.getCausalityAssignments().stream()
                    .filter(x -> x.getCausalityHitResult().getAssignmentId().endsWith(assignmentId))
                    .findFirst().get();
            CausalityHitResult causalityHitResult = causalityAssignment.getCausalityHitResult();
//            CausalityHitResult causalityHitResult = causalityResultStorage.getCausalityHitResult();
            if (!resetHits.contains(hitAssignmentId)) {
                // Clear the hit result before re-save it
                causalityHitResult.getCauseAndAffects().clear();
                causalityHitResult.getNonCauseAndAffects().clear();
                resetHits.add(hitAssignmentId);
            }
            if (causality) {
//                log.debug("Adding cause {}", causeAndAffect);
                causalityHitResult.getCauseAndAffects().add(causeAndAffect);
            } else {
//                log.debug("Adding noncause {}", causeAndAffect);
                causalityHitResult.getNonCauseAndAffects().add(causeAndAffect);
            }
            causalityAssignment.setAssignmentStatus(AssignmentStatus.SUBMITTED);
        }
//        saveDb(hitStorage);
    }

}
