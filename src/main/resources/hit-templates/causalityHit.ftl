<#import "common.ftl" as c>
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
        $(function () {
            $("input.none-of-the-above").click(function () {
                var $this = $(this);
                var groupName = $this.attr("name");
                $("input[name='" + groupName + "']:not(.none-of-the-above)").prop('checked', false);
            });
            $("input:not(.none-of-the-above)").click(function () {
                var $this = $(this);
                var groupName = $this.attr("name");
                $("input[name='" + groupName + "'].none-of-the-above").prop('checked', false);
            });

        })
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
                    <p>In this task you will first be shown a summary of a story. Then, you will be presented with
                        several
                        questions.</p>

                    <p>In each question an event from the story will be described. You will be asked to select possible
                        direct causes for the event in question. Your goal is to mark all of the possible direct causes
                        of
                        this event. Since you have only seen a summary of the original story, you may sometimes need to
                        guess or fill some gaps.</p>

                    <p>For consistency checking, each event will appear twice in the questionnaire in random order. If
                        you
                        are too inconsistent in your answers, we will reject them.</p>

                    <p><strong>For example:</strong></p>

                    <ul>
                        <li><u>Summary</u>: Hector and Qi went on a hike together. At the top of the mountain Hactor's
                            big backpack bumped Qi and he fell from the cliff.
                        </li>
                        <li><u>Event</u>: Hector and Qi met and became friends.</li>
                        <li><u>Possible causes</u>:
                            <ol>
                                <li>Qi fell of the cliff</li>
                                <li>Hector packed too many things</li>
                                <li>Hector and Qi were neighbors</li>
                                <li>None of the above</li>
                            </ol>
                        </li>
                    </ul>

                    <p>In this example, the only direct cause of the event is option 3 since, although not written in
                        the
                        summary, it is reasonable to guess that Hector and Qi met because they were neighbors.</p>

                </div>
                <section>
                    <div class="panel panel-default">
                        <div class="panel-heading">Questions</div>
                        <div class="panel-body">
                            <div class="form-group">
                                <label class="form-control-label">Summary</label>
                                <div class="form-control-static">${globalSummary}</div>
                            </div>
                            <hr/>
                            <ul>
                            <#list causalityQuestions as causalityQuestion>
                                <#assign questionNodeId=causalityQuestion.questionNodeId>
                                <div class="form-group">
                                    <label class="form-control-label">Event</label>
                                    <div class="form-control-static">
                                    ${causalityQuestion.question}
                                    </div>
                                </div>

                                <label>Possible causes: (check all that apply)</label>
                            <#--<ul>-->
                                <#list causalityQuestion.causes as cause>
                                    <div class="form-check">
                                        <label class="form-check-label">
                                            <input type="checkbox" <#if cause?is_first >required
                                                   data-error="#${questionNodeId}_err"</#if>
                                                   class="form-check-input"
                                                   name="${questionNodeId}[]"
                                                   value="${cause.nodeId}">
                                        ${cause.text}
                                        </label>
                                    </div>
                                </#list>
                                <div class="form-check">
                                    <label class="form-check-label">
                                        <input type="checkbox" class="form-check-input none-of-the-above"
                                               name="${questionNodeId}[]" value="none">
                                        None of the above
                                    </label>
                                </div>
                                <div id="${questionNodeId}_err"
                                     class="text-danger"
                                     style="display: none">
                                </div>
                            <#--</ul>-->
                                <#sep >
                                    <hr/></#sep>
                            </#list>
                            </ul>
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