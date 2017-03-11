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
    <#list hitsForReview as hitForReview>
        <#assign assignmentId = hitForReview.assignmentId>
        <#assign assignmentHitId = hitForReview.hitId + ':' + hitForReview.assignmentId>
        <div>
            <div class="card mb-2">
                <div class="card-header card-primary card-inverse">
                    HitId: ${assignmentHitId}
                </div>
                <div class="card-block">
                    <label>Parents summaries</label>
                    <ul>
                        <#list hitForReview.parentsSummaries as parentSaummary>
                            <li>${parentSaummary?html?replace('\n', '<br>')}</li>
                        </#list>
                    </ul>

                    <label>Scores and events</label>
                    <ul>
                        <#list hitForReview.idsAndScoresAndEvents as idAndScoreAndEvent>
                            <div class="form-group row">
                                <label class="col-sm-2 form-control-label">Summary</label>
                                <div class="col-sm-10">
                                    <span class="form-control-static">${hitForReview.childIdToSummary[idAndScoreAndEvent.left]?html?replace('\n', '<br>')}</span>
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
                                ${idAndScoreAndEvent.right?html?replace('\n', '<br>')}
                                </span>
                                </div>
                            </div>
                            <#sep >
                                <hr/>
                        </#list>
                    </ul>

                    <fieldset class="form-group row">
                        <legend class="col-form-legend col-sm-2">Approve</legend>
                        <div class="form-check form-check-inline">
                            <label class="form-check-label">
                                <input type="radio" name="${assignmentHitId}_approve" value="1"
                                       data-hitid="${assignmentId}"> Yes
                            </label>
                            <label class="form-check-label">
                                <input type="radio" name="${assignmentHitId}_approve" value="0"
                                       data-hitid="${assignmentId}"> No
                            </label>
                        </div>
                    </fieldset>
                    <div class="form-group row" id="${assignmentId}_reason" style="display: none">
                        <label class="col-sm-1 form-control-label">Reason</label>
                        <div class="col-sm-11">
                                <textarea
                                        name="${assignmentHitId}_reason" placeholder="Reason"
                                        autocomplete="off"
                                        class="form-control"></textarea>
                        </div>
                    </div>

                    <input type="hidden" name="${assignmentHitId}_data" value="${hitForReview.encodedData!""}">
                    <input type="hidden" name="${assignmentHitId}_nodeid" value="${hitForReview.nodeId}">
                </div>
            </div>
        </div>
        <#sep><br></#sep>
    </#list>

    </form>
</div>
</body>
</html>