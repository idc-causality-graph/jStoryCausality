<!DOCTYPE html>
<html>
<head>
    <title>HIT Worker UI</title>
    <link rel="stylesheet" href="/css/bootstrap.min.css">
    <link rel="stylesheet" href="/css/bootstrap-grid.min.css">
    <script src="/js/jquery-3.1.1.min.js"></script>
    <script src="/js/bootstrap.min.js"></script>
    <script type="application/javascript">
        $(function () {
            $("#theform").submit(function () {
                $("input[name*='CAUS_'][type='checkbox']:checked").prev().attr('disabled', true);
            });
        });
    </script>
</head>
<body>
<div class="container-fluid">

    <h1>HITs Worker UI</h1>
    <form id="theform" action="/hits" method="post">
        <div class="sticky-top">
            <div class="d-flex flex-row">
                <button class="p-2 btn btn-info" type="submit">Save</button>
                <a class="p-2 btn btn-info" href="">Reload</a>
            </div>
        </div>
    <#list hitStorage.upHits as hitId, upHit>
        <div class="card mb-5">
            <div class="card-header card-${upHit.upHitResult.hitDone?then('secondary','primary card-inverse')} ">
                HIT Id ${hitId}
            </div>
            <div class="card-block">
                <div class="form-group row">
                    <label class="col-form-label col-sm-2">Suggested summary</label>
                    <textarea class="form-control col-sm-10"
                              autocomplete="off"
                              name="${hitId}_hitsummary">${upHit.upHitResult.hitSummary!""}</textarea>
                </div>

                <label>Choose best summary</label>
                <#list upHit.childIdToSummaries as childId,summaries>
                    <div class="card mb-1">
                        <div class="card-header">
                            Child Id ${childId}
                        </div>
                        <fieldset class="form-group row card-block">
                            <div class="col-sm-10">
                                <#list summaries as summary>
                                    <div class="form-check">
                                        <label class="form-check-label">
                                            <input class="form-check-input"
                                                   type="radio"
                                                   name="${hitId}_${childId}"
                                                   value="${summary?index}"
                                                <#if upHit.upHitResult.chosenChildrenSummaries[childId]??>
                                                   <#if upHit.upHitResult.chosenChildrenSummaries[childId]==summary?index>checked</#if>
                                                </#if>>
                                        ${summary}
                                        </label>
                                    </div>
                                </#list>
                            </div>
                        </fieldset>
                    </div>
                </#list>
            </div>
        </div>
    </#list>

    <#list hitStorage.downHits as hitId, downHit>
        <div class="card mb-5">
            <div class="card-header card-${downHit.downHitResult.hitDone?then('secondary','primary card-inverse')} ">
                HIT Id ${hitId}
            </div>
            <div class="card-block">
                <label>Parents' summaries</label>
                <ul>
                    <#list downHit.parentsSummaries as parentSummary>
                        <li>${parentSummary}</li>
                    </#list>
                </ul>

                <label>Events and scores</label>
                <ul>
                    <#list downHit.childrenIdsAndSummaries as childIdAndSummary>
                        <#if downHit.downHitResult.idsAndScoresAndEvents??>
                            <#assign given_score = downHit.downHitResult.idsAndScoresAndEvents[childIdAndSummary?index].score>
                            <#assign event = downHit.downHitResult.idsAndScoresAndEvents[childIdAndSummary?index].mostImportantEvent>
                        <#else>
                            <#assign event = "">
                            <#assign given_score = -1>
                        </#if>

                        <div class="form-group row">
                            <label class="col-form-label col-sm-2">Summary for (${childIdAndSummary.left})</label>
                            <div class="col-sm-10 form-control-static">${childIdAndSummary.right}</div>
                        </div>
                        <div class="form-group row">
                            <label class="col-form-label col-sm-2">Most important event</label>
                            <div class="col-sm-10">
                                <textarea name="DOWN_${hitId}_event_${childIdAndSummary?index}"
                                          autocomplete="off" class="form-control "
                                >${event}</textarea>
                            </div>

                        </div>
                        <div class="form-group row">
                            <label class="col-form-label col-sm-2">Importance score</label>
                            <div class="col-sm-10 ">
                                <div class="form-control form-check form-check-inline">
                                    <#list 1..7 as rate>
                                        <label class="form-check-label">
                                            <input class="form-check-input" type="radio"
                                                <#if given_score??>${(given_score==rate)?then('checked','')}</#if>
                                                   name="DOWN_${hitId}_score_${childIdAndSummary?index}"
                                                   value="${rate}">
                                        ${rate}
                                        </label>
                                    </#list>
                                </div>
                            </div>
                        </div>
                        <input type="hidden" name="DOWN_${hitId}_nodeid_${childIdAndSummary?index}"
                               value="${childIdAndSummary.left}">
                        <#sep >
                            <hr/>
                    </#list>
                </ul>


            </div>
        </div>
    </#list>

    <#list hitStorage.causalityHits as hitId, causalityHit>
        <div class="card mb-5">
            <div class="card-header card-${causalityHit.causalityHitResult.hitDone?then('secondary','primary card-inverse')} ">
                HIT Id ${hitId}
            </div>
            <div class="card-block">
                <div class="form-group row">
                    <label class="form-control-label col-sm-2">Root node summaries</label>
                    <div class="col-sm-10">
                        <span class="form-control-static">${causalityHit.globalSummary}</span>
                    </div>
                </div>
                <ul>
                    <#list causalityHit.causalityQuestions as causalityQuestion>
                        <#assign questionNodeId=causalityQuestion.questionNodeId>
                        <div class="form-group row">
                            <label class="form-control-label col-sm-1">Event</label>
                            <div class="col-sm-11">
                                <span class="form-control-static">${causalityQuestion.question}</span>
                            </div>
                        </div>

                        <label>Causes: (${causalityQuestion.causes?size}) check all that apply</label>
                        <ul>
                            <#list causalityQuestion.causes as cause>
                                <div class="form-check">
                                    <label class="form-check-label">
                                        <#assign inputName='CAUS_'+hitId+':'+questionNodeId+':'+cause.nodeId>
                                        <input type="hidden" value="off" name="${inputName}">
                                        <input type="checkbox" class="form-check-input"
                                        ${causalityHit.causalityHitResult.causeNodeIds?seq_contains(questionNodeId+':'+cause.nodeId)?then('checked','')}
                                               name="${inputName}">
                                        ${cause.text}
                                    </label>
                                </div>
                            </#list>
                        </ul>
                        <#sep >
                            <hr/></#sep>
                    </#list>
                </ul>
            </div>
        </div>

    </#list>
    </form>
</div>
</body>
</html>