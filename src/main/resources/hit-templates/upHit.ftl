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

                    <p>This task has two parts:</p>

                    <ol>
                        <li>Choose the best among several alternative summary texts.</li>
                        <li>Write a shorter summary.</li>
                    </ol>

                    <hr/>
                    <p><strong>Guidelines:</strong></p>

                    <p>A good summary is short, and consists of only the important events and details from the original
                        text.<br/>
                        For example, for the text: &quot;<i>Cupid looks down as Bill initiates a conversation with Tina.
                            Cupid flies above a roof top, hunched over before he notices something above him. Cupid's
                            scroll
                            floats around him. He then pushes it away with a sad look on his face.</i>&quot;<br/>
                        A good summary is: &quot;<i>Cupid watches Bill and Tina, then flies away. He is sad to see his
                            scroll.</i>&quot;</p>
                    <p>Your writing must be original and can not simply be a copy of part of the text. We strive to
                        award all assignments so please write in your own words using good grammar and spelling.</p>
                </div>
                </p>
            </div>
        </div>
        <!-- End Instructions -->

        <!-- Content Body -->
        <section>
            <div class="panel panel-default">
                <div class="panel-heading">
                    <strong>Choose the best summary in each section</strong>
                </div>
                <div class="panel-body">

                <#list childIdToSummaries as childId,summaries>
                    <p>
                        The following is a group of summaries. All summaries in this group are summaries of
                        the same original text. Please choose the best summary for this group.
                    </p>
                    <div class="panel panel-default">
                        <div class="panel-body">
                            <ul>
                                <#list summaries as summary>
                                    <div class="form-group row">
                                        <label class="form-check-label">
                                            <input class="form-check-input"
                                                   type="radio"
                                                   required
                                                   name="${childId}"
                                                   data-error="#${childId}_err"
                                                   value="${summary?index}">
                                        ${summary}
                                        </label>
                                    </div>
                                </#list>
                                <div id="${childId}_err"
                                     class="text-danger"
                                     style="display: none">
                                </div>
                            </ul>
                        </div>
                    </div>
                </#list>
                </div>
            </div>
        </section>

        <section>
            <div class="panel panel-default">
                <div class="panel-heading">
                    <strong>Write a summary</strong>
                </div>
                <div class="panel-body">
                    <p>
                        Write a shorter, combined summary for all the groups together. Base your summary off the
                        best summary in each group.<br/>
                        The groups appear in chronological order (i.e. all the events in the first group happen
                        before the events in the second group, etc...)<br/>
                        Your shorter summary should be approximately half as long, leaving out the less
                        important details.
                    </p>
                    <div class="form-group row">
                        <label class="form-control-label col-sm-2">Suggested summary</label>
                        <textarea class="form-control col-sm-10"
                                  required
                                  rows="10"
                                  autocomplete="off"
                                  data-error="#hitsummary_err"
                                  name="hitsummary"></textarea>
                    </div>
                    <div id="hitsummary_err"
                         class="text-danger"
                         style="display: none">
                    </div>
        </section>
        </div>
    </section>
<@c.submit_button />
</form>
<@c.aws_script />
</body>
</html>