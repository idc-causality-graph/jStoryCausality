<!DOCTYPE html>
<html>
<head>
    <title>Review Down Hits</title>
    <link rel="stylesheet" href="/css/bootstrap.min.css">
    <link rel="stylesheet" href="/css/bootstrap-grid.min.css">
    <script src="/js/jquery-3.1.1.min.js"></script>
    <script src="/js/bootstrap.min.js"></script>
    <script type="application/javascript">
        $(function () {
            $("input[type='radio']").click(function () {
                var $this = $(this);
                var hitId = $this.data().hitid;
                var approve = $this.val() == '1';
                $("#" + hitId + "_reason").toggle(!approve);
            });
        })
    </script>
</head>
<body>
<div class="container-fluid">
    <h1>Review DOWN hits</h1>
    <form method="post">
        <div class="sticky-top">
            <div class="d-flex flex-row">
                <button class="p-2 btn btn-info" type="submit">Submit</button>
                <a class="p-2 btn btn-info" href="">Reload</a>
            </div>
        </div>
    <#list hitsForReview?sort_by('hitDone')?reverse as hitForReview>
        <div>
            <div class="card mb-2">
                <div class="card-header ${hitForReview.hitDone?then('card-primary card-inverse','')}">
                    HitId: ${hitForReview.hitId}
                </div>
                <div class="card-block">
                    <div class="form-group row">
                        <label class="col-sm-3 form-control-label">Is done?</label>
                        <div class="col-sm-9">
                            <span class="form-control-static">${hitForReview.hitDone?c}</span>
                        </div>
                    </div>

                    <label>Parents summaries</label>
                    <ul>
                        <#list hitForReview.parentsSummaries as parentSaummary>
                            <li>${parentSaummary}</li>
                        </#list>
                    </ul>

                    <#if hitForReview.hitDone>
                        <label>Scores and events</label>
                        <ul>
                        <#list hitForReview.idsAndScoresAndEvents as idAndScoreAndEvent>
                            <div class="form-group row">
                                <label class="col-sm-2 form-control-label">Summary</label>
                                <div class="col-sm-10">
                                    <span class="form-control-static">${hitForReview.childIdToSummary[idAndScoreAndEvent.left]}</span>
                                </div>
                            </div>
                            <div class="form-group row">
                                <label class="col-sm-2 form-control-label">Score (1-7)</label>
                                <div class="col-sm-10"><span class="form-control-static">
                                    ${idAndScoreAndEvent.middle}
                                    </span>
                                </div>
                            </div>
                            <div class="form-group row">
                                <label class="col-sm-2 form-control-label">Important event</label>
                                <div class="col-sm-10"><span class="form-control-static">
                                    ${idAndScoreAndEvent.right}
                                </span>
                                </div>
                            </div>
                            <#sep ><hr/>
                        </#list>
                        </ul>

                        <fieldset class="form-group row">
                            <legend class="col-form-legend col-sm-2">Approve</legend>
                            <div class="form-check form-check-inline">
                                <label class="form-check-label">
                                    <input type="radio" name="${hitForReview.hitId}_approve" value="1"
                                           data-hitid="${hitForReview.hitId}"> Yes
                                </label>
                                <label class="form-check-label">
                                    <input type="radio" name="${hitForReview.hitId}_approve" value="0"
                                           data-hitid="${hitForReview.hitId}"> No
                                </label>
                            </div>
                        </fieldset>
                        <div class="form-group row" id="${hitForReview.hitId}_reason" style="display: none">
                            <label class="col-sm-1 form-control-label">Reason</label>
                            <div class="col-sm-11">
                                <textarea name="${hitForReview.hitId}_reason" placeholder="Reason"
                                          autocomplete="off"
                                          class="form-control"></textarea>
                            </div>
                        </div>

                    </#if>
                    <input type="hidden" name="${hitForReview.hitId}_data" value="${hitForReview.encodedData!""}">
                    <input type="hidden" name="${hitForReview.hitId}_nodeid" value="${hitForReview.nodeId}">
                </div>
            </div>
        </div>
        <#sep><br></#sep>
    </#list>

    </form>
</div>
</body>
</html>