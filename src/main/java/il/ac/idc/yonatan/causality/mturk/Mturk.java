package il.ac.idc.yonatan.causality.mturk;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import il.ac.idc.yonatan.causality.config.AppConfig;
import il.ac.idc.yonatan.causality.mturk.data.CreateHitData;
import il.ac.idc.yonatan.causality.mturk.data.CreateHitResponse;
import il.ac.idc.yonatan.causality.mturk.data.GetHitResponse;
import il.ac.idc.yonatan.causality.mturk.data.HitIdData;
import il.ac.idc.yonatan.causality.mturk.data.PyAwsData;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;

/**
 * Created by ygraber on 1/28/17.
 */
@Service
@Slf4j
public class Mturk {

    @Autowired
    private AppConfig appConfig;

    private File tempMturkFile;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ResourceLoader resourceLoader;

    @PostConstruct
    void createTempFile() throws IOException {
        tempMturkFile = File.createTempFile("mturk", ".py");
        // tempMturkFile.deleteOnExit();
        Resource resource = resourceLoader.getResource("classpath:python/mturk.py");
        FileUtils.copyToFile(resource.getInputStream(), tempMturkFile);
    }

    @PreDestroy
    void clearTempFile() {

        //tempMturkFile.delete();
    }

    @Value("classpath*:/python/mturk.py")
    private FileSystemResource mturkPythonScript;

    @SneakyThrows(JsonProcessingException.class)
    private String runProcess(PyAwsData pyAwsData) throws IOException, InterruptedException {
        String scriptFile = tempMturkFile.getAbsolutePath();
        String pythonRunner = "/usr/bin/python";
        log.debug("Running {} {}", pythonRunner, scriptFile);
        File tempInput = new File("/var/folders/qh/4g4cyct54j97h5s1yn65j6l80000gp/T/in.tmp");
        File tempOutput = new File("/var/folders/qh/4g4cyct54j97h5s1yn65j6l80000gp/T/out.tmp");

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(pythonRunner, scriptFile,
                    tempInput.getAbsolutePath(), tempOutput.getAbsolutePath());
            log.debug("Running processbuilder {}", processBuilder.command());

            String input = objectMapper.writeValueAsString(pyAwsData);
            FileUtils.write(tempInput, input, Charset.defaultCharset());

            Process process = processBuilder.start();
            int rc = process.waitFor();
            System.out.println(rc);
            String errString = IOUtils.toString(process.getErrorStream(), Charset.defaultCharset());
            if (rc != 0) {
                throw new RuntimeException("Python error:\n" + errString);
            }
            return FileUtils.readFileToString(tempOutput, Charset.defaultCharset());
        } finally {
            tempInput.delete();
            tempOutput.delete();
        }

    }

    private PyAwsData createAwsData() {
        PyAwsData pyAwsData = new PyAwsData();
        pyAwsData.setAwsKey(appConfig.getAwsKey());
        pyAwsData.setAwsSecretKey(appConfig.getAwsSecretKey());
        pyAwsData.setSandbox(appConfig.isSandbox());
        return pyAwsData;
    }

    public CreateHitResponse createHit(String title, String description, String questionHtml, double reward, int numOfDuplications,
                                       int durationMinutes,
                                       int lifetimeMinutes, List<String> keywords, String annotation) {
        PyAwsData awsData = createAwsData();
        awsData.setRequestType(RequestTypes.CREATE_HIT);
        CreateHitData createHitData = new CreateHitData();
        createHitData.setTitle(title);
        createHitData.setDescription(description);
        createHitData.setQuestionHtml(questionHtml);
        createHitData.setReward(reward);
        createHitData.setDurationMinutes(durationMinutes);
        createHitData.setLifetimeMinutes(lifetimeMinutes);
        createHitData.setNumberOfDuplications(numOfDuplications);
        createHitData.setKeywords(keywords != null ? keywords : Collections.<String>emptyList());
        createHitData.setAnnotation(annotation);
        awsData.setRequestData(createHitData);
        try {
            String rawResponse = runProcess(awsData);
            CreateHitResponse createHitResponse = objectMapper.readValue(rawResponse, CreateHitResponse.class);
            return createHitResponse;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public void dodo() throws IOException {
        System.out.println(mturkPythonScript.getURL().getFile());

        System.out.println(mturkPythonScript.getFile());
    }

    private <T> T getHitData(String hitId, RequestTypes requestType, Class<T> responseClass) {
        try {
            PyAwsData awsData = createAwsData();
            awsData.setRequestType(requestType);
            HitIdData hitIdData = new HitIdData();
            hitIdData.setHitId(hitId);
            awsData.setRequestData(hitIdData);
            String result = runProcess(awsData);
            return objectMapper.readValue(result, responseClass);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public GetHitResponse getHit(String hitId) {
        return getHitData(hitId, RequestTypes.GET_HIT, GetHitResponse.class);


    }
}
