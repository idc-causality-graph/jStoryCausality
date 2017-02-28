<#import "common.ftl" as c>
<!DOCTYPE html>
<html>
<head>
    <title>Assignment</title>
    <meta http-equiv='Content-Type' content='text/html; charset=UTF-8'/>
<@c.external_scripts/>
<@c.bootup_script />
    <script type="application/javascript">
        function disable_form() {
            $("input").attr("disabled", true);
            $("textarea").attr("disabled", true);
        }
    </script>
</head>
<body>
<@c.unaccept_banner />
<form name='mturk_form' method='post' id='mturk_form' action='${submitUrl}'>
<@c.assignment_hidden />
    <section class="container" id="other">
        <div class="row col-xs-12 col-md-12"><!-- Instructions -->

            <div class="panel panel-primary">
                <div class="panel-heading"><strong>Instructions</strong></div>

                <div class="panel-body">

                    <p>
                        In this task, you will be shown several alternative summaries of an entire story.
                        In each section you will be shown a portion of the original, unabridged story. Your job is to
                        describe the most important <i>event</i> in the portion of each of the sections, and then score
                        the importance of that event to the full-text summaries.
                    </p>
                    <hr/>
                    <p>Guidelines:</p>
                    <ul>
                        <li>A score of <b>1</b> means that the event you described is not important at all for
                            understanding the full-text summaries. (While it is the most important event in the portion
                            of unabridged text, it may not be important at all for the overall story.)
                        </li>
                        <li>A score of <b>7</b> means that the event you described is crucial for understanding the
                            summaries of the entire story. If this event were removed from the summaries, then the
                            summaries would be partial at
                            best.
                        </li>
                    </ul>
                    <p>Your writing must be original and can not simply be a copy of part of the text. We strive to
                        award all assignments so please write in your own words using good grammar and spelling.</p>
                </div>
            </div>
            <!-- End Instructions -->

            <!-- Content Body -->
            <section>
                <div class="panel panel-default">
                    <div class="panel-heading">
                        <strong>Read the list of summaries and describe the most important event</strong>
                    </div>
                    <div class="panel-body">
                        <p>
                            Read the list of summaries, and for each section read the story portion and answer the
                            questions.
                        </p>

                        <label>Summaries</label>
                        <ul>
                        <#list parentsSummaries as parentSummary>
                            <li>${parentSummary}</li>
                        </#list>
                        </ul>
                        <hr/>


                    <#list childrenIdsAndSummaries as childIdAndSummary>
                        <div class="form-group">
                            <label class="form-control-label">Portion of the story</label>
                            <div class="form-control-static">${childIdAndSummary.right}</div>
                        </div>
                        <div class="form-group">
                            <label class="form-control-label">Please describe the most important event in the
                                portion of the story</label>
                        <#--<div class="col-sm-10">-->
                            <textarea name="${childIdAndSummary.left}_event"
                                      data-error="#${childIdAndSummary.left}_event_err"
                                      autocomplete="off"
                                      class="form-control"
                                      required></textarea>
                            <div id="${childIdAndSummary.left}_event_err"
                                 class="text-danger"
                                 style="display: none"></div>
                        <#--</div>-->
                        </div>
                        <div class="form-group">
                            <label class="form-control-label">How important is the event you just described to
                                understanding the following summaries of the entire story? </label>
                        <#--<div class="col-sm-10 ">-->
                            <div class="form-control form-check form-check-inline">
                                <span>Not important</span>
                                <#list 1..7 as rate>
                                    <label class="form-check-label">
                                        <input class="form-check-input" type="radio"
                                               name="${childIdAndSummary.left}_score"
                                               required
                                               data-error="#${childIdAndSummary.left}_score_err"
                                               value="${rate}">
                                    ${rate}
                                    </label>
                                </#list>
                                <span>Most important</span>
                            </div>
                            <div class="help-block">Please provide a score
                                between 1 (not important at all) to 7 (the most important event in the full-text
                                summary).
                            </div>
                            <div id="${childIdAndSummary.left}_score_err"
                                 class="text-danger"
                                 style="display: none"></div>
                        <#--</div>-->
                        </div>
                        <#sep >
                            <hr/>
                    </#list>
                    </div>
                </div>
            </section>
        </div>
    </section>
<@c.submit_button />
</form>
<@c.aws_script />
</body>
</html>