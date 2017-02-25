package il.ac.idc.yonatan.causality.mturk;

import com.fasterxml.jackson.databind.ObjectMapper;
import il.ac.idc.yonatan.causality.config.AppConfig;
import il.ac.idc.yonatan.causality.mturk.data.CausalityHitResult;
import il.ac.idc.yonatan.causality.mturk.data.CausalityQuestion;
import il.ac.idc.yonatan.causality.mturk.data.DownHitResult;
import il.ac.idc.yonatan.causality.mturk.data.UpHitResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import mturk.wsdl.AssignmentStatus;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

/**
 * Created by ygraber on 2/25/17.
 */
public class HitManagerMock2Impl implements HitManager {

    private static final File DB = new File("./hit-storage.json");
    private final AppConfig appConfig;
    private final ObjectMapper objectMapper;


    @Data
    static class HitStorage {
        private Map<String, UpHitStorage> upHitStorage = new HashMap<>();
        private Map<String, DownHitStorage> downHitStorage = new HashMap<>();
        private Map<String, CausalityHitStorage> causalityHitStorage = new HashMap<>();
    }

    @Data
    static class UpHitStorage {
        // assignments
        private List<UpAssignment> upAssignments = new ArrayList<>();
        private LinkedHashMap<String, List<String>> childIdToSummaries;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class UpAssignment {
        private UpHitResult upHitResult;
        private AssignmentStatus assignmentStatus;
    }

    @Data
    static class DownHitStorage {
        private List<DownAssignment> downAssignments = new ArrayList<>();
        private List<String> parentsSummaries;
        private List<Pair<String, String>> childrenIdsAndSummaries;
        private boolean isLeaf;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class DownAssignment {
        private DownHitResult downHitResult;
        private AssignmentStatus assignmentStatus;
    }

    @Data
    static class CausalityHitStorage {
        private String globalSummary;
        private List<CausalityQuestion> causalityHitQuestions;
        private List<CausalityAssignment> causalityAssignments = new ArrayList<>();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class CausalityAssignment {
        private CausalityHitResult causalityHitResult;
        private AssignmentStatus assignmentStatus;
    }

    @Autowired
    public HitManagerMock2Impl(ObjectMapper objectMapper, AppConfig appConfig) {
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
        String hitId = "HIT-" + RandomStringUtils.randomAlphanumeric(8);
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
        String hitId = "HIT_D-" + RandomStringUtils.randomAlphanumeric(8);

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
        String hitId = "C_HIT-" + RandomStringUtils.randomAlphanumeric(8);

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

    //TODO add controller and UI

}
