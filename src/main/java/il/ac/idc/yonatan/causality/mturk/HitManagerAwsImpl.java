package il.ac.idc.yonatan.causality.mturk;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import il.ac.idc.yonatan.causality.config.AppConfig;
import il.ac.idc.yonatan.causality.mturk.data.CausalityHitResult;
import il.ac.idc.yonatan.causality.mturk.data.CausalityQuestion;
import il.ac.idc.yonatan.causality.mturk.data.CauseAndAffect;
import il.ac.idc.yonatan.causality.mturk.data.DownHitResult;
import il.ac.idc.yonatan.causality.mturk.data.IdScoreAndEvent;
import il.ac.idc.yonatan.causality.mturk.data.UpHitResult;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import mturk.wsdl.Assignment;
import mturk.wsdl.AssignmentStatus;
import mturk.wsdl.Comparator;
import mturk.wsdl.CreateHIT;
import mturk.wsdl.CreateHITRequest;
import mturk.wsdl.CreateHITResponse;
import mturk.wsdl.Errors;
import mturk.wsdl.GetAssignmentsForHIT;
import mturk.wsdl.GetAssignmentsForHITRequest;
import mturk.wsdl.GetAssignmentsForHITResponse;
import mturk.wsdl.GetAssignmentsForHITResult;
import mturk.wsdl.HIT;
import mturk.wsdl.Price;
import mturk.wsdl.QualificationRequirement;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactory;
import org.springframework.ws.client.core.support.WebServiceGatewaySupport;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.security.SignatureException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

@Service
@Slf4j
@Profile("aws")
public class HitManagerAwsImpl extends WebServiceGatewaySupport implements HitManager {

    private static final Namespace ANSWER_NS = Namespace.getNamespace("http://mechanicalturk.amazonaws.com/AWSMechanicalTurkDataSchemas/2005-10-01/QuestionFormAnswers.xsd");

    private final AppConfig appConfig;
    private final Template htmlQuestionTemplate;
    private final Configuration freeMarkerConfig;
    private final String submitUrl;
    private final SAXBuilder builder = new SAXBuilder();

    @Autowired
    public HitManagerAwsImpl(Configuration freeMarkerConfig,
                             AppConfig appConfig) throws IOException {
        this.appConfig = appConfig;
        this.freeMarkerConfig = freeMarkerConfig;
        htmlQuestionTemplate = freeMarkerConfig.getTemplate("htmlQuestion.xml.ftl");
        submitUrl = appConfig.isSandbox() ? "https://workersandbox.mturk.com/mturk/externalSubmit" :
                "https://www.mturk.com/mturk/externalSubmit";
        Jaxb2Marshaller marshaller = marshaller();
        setMarshaller(marshaller);
        setUnmarshaller(marshaller);
    }

    @Override
    public String createDownHit(List<String> parentsSummaries, List<Pair<String, String>> childrenIdsAndSummaries, boolean isLeaf) {
        String downHitHtml = renderHitTemplate("downHit.ftl",
                ImmutableMap.of("parentsSummaries", parentsSummaries, "childrenIdsAndSummaries", childrenIdsAndSummaries));
        return createHit(downHitHtml, "Decide what is the most important event",
                "After reading a summary, you should decide what is the most important event in it",
                appConfig.getDownHitReward(),
                newArrayList("summary", "vote"));
    }

    @Override
    public String createCausalityHit(String globalSummary, List<CausalityQuestion> causalityQuestions) {
        String causalityHitHtml = renderHitTemplate("causalityHit.ftl",
                ImmutableMap.of("globalSummary", globalSummary, "causalityQuestions", causalityQuestions));
        return createHit(causalityHitHtml, "Decide what is the direct cause of an event",
                "Read a description of an event, and decide which of the following event are direct cause of it",
                appConfig.getCausalityHitReward(),
                newArrayList("summary", "vote"));
    }

    @Override
    public String createUpHit(LinkedHashMap<String, List<String>> childIdToSummaries) {
        String upHitHtml = renderHitTemplate("upHit.ftl",
                ImmutableMap.of("childIdToSummaries", childIdToSummaries));
        return createHit(upHitHtml, "Make a summery of text and vote for a summary",
                "Choose between summaries, and then make a summary of your own",
                appConfig.getUpHitReward(),
                newArrayList("writing", "summary", "vote"));
    }

    @SneakyThrows
    String createHit(String questionHtml, String title, String description,
                     double reward, List<String> keywords) {
        CreateHIT createHIT = new CreateHIT();
        long durationInMinutes = appConfig.getHitDurationInMinutes();
        long lifetimeInMinutes = appConfig.getHitLifetimeInMinutes();

        setAwsRequestHeaders(createHIT);

        CreateHITRequest chr = new CreateHITRequest();
        chr.setTitle(title);
        chr.setDescription(description);
        chr.setAssignmentDurationInSeconds(TimeUnit.MINUTES.toSeconds(durationInMinutes));
        chr.setLifetimeInSeconds(TimeUnit.MINUTES.toSeconds(lifetimeInMinutes));

        Price price = new Price();
        price.setAmount(BigDecimal.valueOf(reward));
        price.setCurrencyCode("USD");
        chr.setReward(price);
        if (keywords != null) {
            chr.setKeywords(Joiner.on(',').join(keywords));
        }

        addQualifications(chr);

        StringWriter sw = new StringWriter();
        htmlQuestionTemplate.process(ImmutableMap.of("htmlContent", questionHtml, "frameHeight", 600), sw);
        chr.setQuestion(sw.toString());
        createHIT.getRequest().add(chr);

        CreateHITResponse result = mturkSubmit(createHIT);
        if (result.getHIT().isEmpty()) {
            throw mturkException("Cannot create hit", result.getOperationRequest().getErrors());
        }
        HIT hit = result.getHIT().get(0);
        if (StringUtils.equalsIgnoreCase("True", hit.getRequest().getIsValid())) {
            return hit.getHITId();
        }
        throw mturkException("Cannot create hit", hit.getRequest().getErrors());
    }

    @SneakyThrows({IOException.class, TemplateException.class})
    private String renderHitTemplate(String templateName, Map<String, Object> inputDataModel) {
        Template template = freeMarkerConfig.getTemplate(templateName);
        Map<String, Object> dataModel = new HashMap<>(inputDataModel);
        dataModel.put("submitUrl", submitUrl);
        StringWriter stringWriter = new StringWriter();
        template.process(dataModel, stringWriter);
        String output = stringWriter.toString();
        FileUtils.write(new File("./tmp/" + templateName + ".html"), output, Charset.defaultCharset());
        return output;
    }

    private <T> T mturkSubmit(Object request) {
        String url = appConfig.isSandbox() ?
                "https://mechanicalturk.sandbox.amazonaws.com" : "https://not-working-mechanicalturk.amazonaws.com";
        return (T) getWebServiceTemplate().marshalSendAndReceive(url, request);
    }

    private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";

    private String calculateRFC2104HMAC(String data, String key) throws java.security.SignatureException {
        String result;
        try {
            // get an hmac_sha1 key from the raw key bytes
            SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(),
                    HMAC_SHA1_ALGORITHM);

            // get an hmac_sha1 Mac instance and initialize with the signing key
            Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
            mac.init(signingKey);

            // compute the hmac on input data bytes
            byte[] rawHmac = mac.doFinal(data.getBytes());


            // base64-encode the hmac
            result = Base64.getEncoder().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new SignatureException("Failed to generate HMAC : " + e.getMessage());
        }
        return result;
    }

    @SneakyThrows
    private void setAwsRequestHeaders(Object request) {
        String operation = request.getClass().getSimpleName();
        BeanUtils.setProperty(request, "AWSAccessKeyId", appConfig.getAwsKey());

        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
        String timestamp = StringUtils.substringBefore(now.format(DateTimeFormatter.ISO_DATE_TIME), "[");
        System.out.println(timestamp);
        BeanUtils.setProperty(request, "timestamp", new XMLGregorianCalendarImpl(GregorianCalendar.from(now)));

        String signature = calculateRFC2104HMAC("AWSMechanicalTurkRequester" + operation + timestamp,
                appConfig.getAwsSecretKey());
        BeanUtils.setProperty(request, "signature", signature);
    }

    private void addQualifications(CreateHITRequest chr) {
        if (appConfig.isSandbox()) {
            return;
        }
        QualificationRequirement percentAssignmentsApprovedRequirement = new QualificationRequirement();
        percentAssignmentsApprovedRequirement.setComparator(Comparator.GREATER_THAN_OR_EQUAL_TO);
        percentAssignmentsApprovedRequirement.setQualificationTypeId("000000000000000000L0");
        percentAssignmentsApprovedRequirement.getIntegerValue().add(95);
        chr.getQualificationRequirement().add(percentAssignmentsApprovedRequirement);

        QualificationRequirement numberHitsApprovedRequirement = new QualificationRequirement();
        numberHitsApprovedRequirement.setComparator(Comparator.GREATER_THAN_OR_EQUAL_TO);
        numberHitsApprovedRequirement.setQualificationTypeId("00000000000000000040");
        numberHitsApprovedRequirement.getIntegerValue().add(1000);
        chr.getQualificationRequirement().add(numberHitsApprovedRequirement);

        QualificationRequirement mastersRequirement = new QualificationRequirement();
        mastersRequirement.setComparator(Comparator.EXISTS);
        mastersRequirement.setQualificationTypeId(appConfig.isSandbox() ?
                "2ARFPLSP75KLA8M8DH1HTEQVJT3SY6" : "2F1QJWKUDD8XADTFD2Q0G6UTO95ALH");
        chr.getQualificationRequirement().add(mastersRequirement);

    }

    private RuntimeException mturkException(String message, Errors errors) {
        String errorsString = "";
        if (errors != null) {
            errorsString = errors.getError().stream()
                    .map(error -> error.getCode() + ": " + error.getMessage())
                    .collect(Collectors.joining(", "));
        }
        throw new RuntimeException(message + ": " + errorsString);
    }


    private Map<String, String> getHitAnswers(String hitId) {
        GetAssignmentsForHIT getAssignmentsForHIT = new GetAssignmentsForHIT();
        setAwsRequestHeaders(getAssignmentsForHIT);

        GetAssignmentsForHITRequest request = new GetAssignmentsForHITRequest();
        getAssignmentsForHIT.getRequest().add(request);

        request.getAssignmentStatus().add(AssignmentStatus.SUBMITTED);
        request.setPageSize(100);
        request.setHITId(hitId);

        GetAssignmentsForHITResponse getAssignmentsForHITResponse = mturkSubmit(getAssignmentsForHIT);
        if (getAssignmentsForHITResponse.getGetAssignmentsForHITResult().isEmpty()) {
            throw mturkException("Cannot get hit " + hitId, getAssignmentsForHITResponse.getOperationRequest().getErrors());
        }
        GetAssignmentsForHITResult getAssignmentsForHITResult = getAssignmentsForHITResponse.getGetAssignmentsForHITResult().get(0);
        if (!"true".equalsIgnoreCase(getAssignmentsForHITResult.getRequest().getIsValid())) {
            throw mturkException("Cannot get hit " + hitId, getAssignmentsForHITResult.getRequest().getErrors());
        }

        if (getAssignmentsForHITResult.getNumResults() == 0) {
            return null; //not completed yet
        }

        Assignment assignment = getAssignmentsForHITResult.getAssignment().get(0);
        String rawAnswer = assignment.getAnswer();
        try {
            Document doc = builder.build(IOUtils.toInputStream(rawAnswer, "UTF-8"));
            Map<String, String> questionsAndAnswers = new HashMap<>();
            for (Element child : doc.getRootElement().getChildren()) {
                Element qid = child.getChild("QuestionIdentifier", ANSWER_NS);
                Element ft = child.getChild("FreeText", ANSWER_NS);
                String question = qid.getTextTrim();
                String answer = ft.getTextTrim();
                questionsAndAnswers.put(question, answer);
            }
            return questionsAndAnswers;
        } catch (Exception e) {
            throw new RuntimeException("Cannot parse XML for " + rawAnswer, e);
        }
    }

    @Override
    public UpHitResult getUpHitForReview(String hitId) {
        Map<String, String> hitAnswers = getHitAnswers(hitId);
        UpHitResult upHitResult = new UpHitResult();
        if (hitAnswers == null) {
            upHitResult.setHitDone(false);
        } else {
            upHitResult.setHitDone(true);

            String hitSummary = hitAnswers.get("hitsummary");
            upHitResult.setHitSummary(hitSummary);

            hitAnswers.keySet().stream()
                    .filter(key -> !key.equalsIgnoreCase("hitsummary"))
                    .forEach(key ->
                            upHitResult.getChosenChildrenSummaries().put(key,
                                    Integer.parseInt(hitAnswers.get(key))));
        }
        return upHitResult;
    }


    @Override
    public DownHitResult getDownHitForReview(String hitId) {
        DownHitResult downHitResult = new DownHitResult();
        Map<String, String> hitAnswers = getHitAnswers(hitId);
        if (hitAnswers == null) {
            downHitResult.setHitDone(false);
            return downHitResult;
        } else {
            downHitResult.setHitDone(true);
            Set<String> ids = hitAnswers.keySet().stream()
                    .filter(key -> key.endsWith("_event"))
                    .collect(toSet());

            for (String id : ids) {
                String event = hitAnswers.get(id + "_event");
                String score = hitAnswers.get(id + "_score");
                IdScoreAndEvent idScoreAndEvent = new IdScoreAndEvent(id, Integer.parseInt(score), event);
                downHitResult.getIdsAndScoresAndEvents().add(idScoreAndEvent);
            }
        }
        return downHitResult;
    }

    private List<CauseAndAffect> createCauseAndAffects(String eventKey, String causeIds) {
        return Splitter.on(":").splitToList(causeIds)
                .stream()
                .map(causeId -> new CauseAndAffect(causeId, eventKey))
                .collect(toList());
    }

    @Override
    public CausalityHitResult getCausalityHitForReview(String hitId) {
        CausalityHitResult causalityHitResult = new CausalityHitResult();
        Map<String, String> hitAnswers = getHitAnswers(hitId);
        if (hitAnswers == null) {
            causalityHitResult.setHitDone(false);
        } else {
            causalityHitResult.setHitDone(true);

            Set<String> keys = hitAnswers.keySet()
                    .stream()
                    .filter(key -> key.endsWith("_causes"))
                    .map(key -> StringUtils.substringBeforeLast(key, "_causes"))
                    .collect(toSet());

            for (String key : keys) {
                String causes = hitAnswers.get(key + "_causes");
                String noncauses = hitAnswers.get(key + "+noncauses");
                causalityHitResult.getCauseAndAffects().addAll(createCauseAndAffects(key, causes));
                causalityHitResult.getNonCauseAndAffects().addAll(createCauseAndAffects(key, noncauses));
            }
        }
        return causalityHitResult;
    }

    private void submitHitReview(String hitId, boolean hitApproved, String reason) {
        // if not hitApproved, allow more hits
        // otherwise, approve
    }

    @Override
    public void submitReviewUpHit(String hitId, boolean hitApproved, String reason) {
        submitHitReview(hitId, hitApproved, reason);
    }

    @Override
    public void submitDownHitReview(String hitId, boolean hitApproved, String reason) {
        submitHitReview(hitId, hitApproved, reason);
    }

    @Override
    public void submitCausalityHitReview(String hitId, boolean hitApproved, String reason) {
        submitHitReview(hitId, hitApproved, reason);
    }


    private static Jaxb2Marshaller marshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        // this package must match the package in the <generatePackage> specified in
        // pom.xml
        marshaller.setContextPath("mturk.wsdl");
        return marshaller;
    }

    @Override
    public void reset() {
        throw new UnsupportedOperationException("reset is not supported");

    }

}
