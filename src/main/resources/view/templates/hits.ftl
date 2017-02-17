<!DOCTYPE html>
<html>
<head>
    <title>HIT Worker UI</title>
    <link rel="stylesheet" href="/css/bootstrap.min.css">
    <link rel="stylesheet" href="/css/bootstrap-grid.min.css">
    <script src="/js/jquery-3.1.1.min.js"></script>
    <script src="/js/bootstrap.min.js"></script>
    <script type="application/javascript">
        $(function(){
           $("#theform").submit(function(){
               $("input[name*='CAUS_'][type='checkbox']:checked").prev().attr('disabled',true);
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
                <label>Root node summaries</label>
                <ul>
                    <#list downHit.allRootSummaries as rootNodeSummary>
                        <li>${rootNodeSummary}</li>
                    </#list>
                </ul>
                <label>Node summaries</label>
                <ul>
                    <#list downHit.nodeSummaries as nodeSummary>
                        <li>${nodeSummary}</li>
                    </#list>
                </ul>

                <div class="form-group row">
                    <label class="col-form-label col-sm-2">Most important event</label>
                    <textarea name="DOWN_${hitId}_event"
                              class="form-control col-sm-10">${downHit.downHitResult.mostImportantEvent!""}</textarea>
                </div>

                <div class="form-group row">
                    <label class="col-form-label col-sm-2">Importance score</label>
                    <div class="form-check form-check-inline">
                        <#list 1..7 as rate>
                            <label class="form-check-label">
                                <input class="form-check-input" type="radio"
                                    <#if downHit.downHitResult.importanceScore??>${(downHit.downHitResult.importanceScore==rate)?then('checked','')}</#if>
                                       name="DOWN_${hitId}_score" value="${rate}">
                            ${rate}
                            </label>
                        </#list>
                    </div>
                </div>
            </div>
        </div>
    </#list>

    <#list hitStorage.causalityHits as hitId, causalityHit>
        <div class="card mb-5">
            <div class="card-header card-${causalityHit.causalityHitResult.hitDone?then('secondary','primary card-inverse')} ">
                HIT Id ${hitId}
            </div>
            <div class="card-block">
                <label>Root node summaries</label>
                <div>${causalityHit.globalSummary}</div>
                <#list causalityHit.causalityQuestions as causalityQuestion>
                    <#assign questionNodeId=causalityQuestion.questionNodeId>
                    <div class="form-group row">
                        <label class="col-form-label col-sm-2">Event</label>
                        <span class="form-control-static col-10">${causalityQuestion.question}</span>
                    </div>

                    <label>Causes: (${causalityQuestion.causes?size}) check all that apply</label>
                    <ul>
                        <#list causalityQuestion.causes as cause>
                            <li>
                                <label>
                                    <#assign inputName='CAUS_'+hitId+':'+questionNodeId+':'+cause.nodeId>
                                    <input type="hidden" value="off" name="${inputName}">
                                    <input type="checkbox"
                                           ${causalityHit.causalityHitResult.causeNodeIds?seq_contains(questionNodeId+':'+cause.nodeId)?then('checked','')}
                                           name="${inputName}">
                                ${cause.text}
                                </label>
                            </li>
                        </#list>
                    </ul>
                    <#sep ><hr/></#sep>
                </#list>
            </div>
        </div>

    </#list>
    </form>
</div>
</body>
</html>