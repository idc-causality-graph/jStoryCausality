<!DOCTYPE html>
<html>
<head>
    <title>Context Tree</title>
    <link rel="stylesheet" href="/css/bootstrap.min.css">
    <link rel="stylesheet" href="/css/bootstrap-grid.min.css">
    <script src="/js/jquery-3.1.1.min.js"></script>
    <script src="/js/bootstrap.min.js"></script>
    <style>
        .hit_id_done {
            text-decoration: line-through;
        }

        .hdn {
            display: none;
        }

        ul.tree {
            list-style-type: none;
            margin: 0;
            padding: 0;
        }

        ul.tree ul {
            list-style-type: none;
            /*list-style-type: none;*/
            background: url(http://odyniec.net/articles/turning-lists-into-trees/vline.png) repeat-y;
            margin: 0;
            padding: 0;
            margin-left: 20px;
            padding-top: 10px;
        }

        ul.tree li {
            margin: 0;
            padding: 0 12px;
            line-height: 20px;
            background: url(http://odyniec.net/articles/turning-lists-into-trees/node.png) no-repeat;
        }

        ul.tree > li {
            background: none;

        }

        ul.tree .last {
            background: url(http://odyniec.net/articles/turning-lists-into-trees/vline.png) no-repeat;
        }

        ul.tree li.last {
            background: #fff url(http://odyniec.net/articles/turning-lists-into-trees/lastnode.png) no-repeat;
        }
    </style>
    <script language="JavaScript" type="application/javascript">
        $(function () {
            var $inputs = $('#root_node_form input');
            $inputs.click(function () {
                var $this = $(this);
                if ($this.attr("readonly")) {
                    return false;
                }
                var idx = $this.val();
                $.post("/contextTree/rootNode/choseUpResult", {'chosenResult': idx},
                        function (result) {
                            location.reload();
                        });
            });
            $(".ro_input").dblclick(function () {
                $inputs.show();
                $inputs.removeAttr('readonly');
                $(".ro_input").hide();
                $("#dblclick_message").hide();
            });

            $("#reset_btn").click(function () {
                var result = confirm("This will delete your context tree (and mock hit storage)! Are you sure?");
                return result;
            });
            $("#reset_btn").removeAttr("disabled");
        });
    </script>

</head>
<body>
<div class="container-fluid">
<#if errors??>
    <div class="alert alert-danger alert-dismissible" role="alert">
        <button type="button" class="close" data-dismiss="alert" aria-label="Close">
            <span aria-hidden="true">&times;</span>
        </button>
        <ul>
            <#list errors as error>
                <li>
                ${error}
                </li>
            </#list>
        </ul>
    </div>
</#if>
    <nav class="navbar navbar-toggleable-md navbar-light bg-faded">
        <div class="collapse navbar-collapse" id="navbarText">
            <ui class="navbar-nav">
                <li class="nav-item active">
                    <a class="nav-link" target="_blank" href="/contextTree/reviewsUpPhase">Review up HITs</a>
                </li>
                <li class="nav-item active">
                    <a class="nav-link" target="_blank" href="/contextTree/reviewsDownPhase">Review down HITs</a>
                </li>
                <li class="nav-item active">
                    <a class="nav-link" target="_blank" href="/contextTree/reviewsCausalityPhase">Review causality
                        HITs</a>
                </li>
                <li class="nav-item active">
                    <a class="nav-link" target="_blank" href="/hits">HIT Worker UI</a>
                </li>
            </ui>
        </div>
    </nav>
    <div class="btn-group sticky-top" role="group">
        <div class="btn-toolbar" role="toolbar" aria-label="Toolbar with button groups">

            <form action="/contextTree/progressUp" method="post">
                <button class="btn btn-secondary" type="submit" ${(phase!='UP_PHASE')?then('disabled','')}>
                    Progress up hits
                </button>
            </form>
            <form action="/contextTree/progressDown" method="post">
                <button class="btn btn-secondary"
                        type="submit" ${(phase!='DOWN_PHASE')?then('disabled','')}>Progress down hits
                </button>
            </form>
            <form action="/contextTree/progressCausality" method="post">
                <button class="btn btn-secondary"
                        type="submit" ${(phase!='CAUSALITY_PHASE')?then('disabled','')}>Progress causality hits
                </button>
            </form>
            <span class="ml-4">&nbsp;</span>
            <form action="/contextTree/reset" method="post">
                <button disabled id="reset_btn" class="btn btn-danger" type="submit">Reset</button>
            </form>

        </div>
    </div>
<#if phase=='DONE'><h1>Stick a fork in me, I'm done.</h1></#if>
    <div>&nbsp;</div>
${htmlTree}
    <br>
    <label>Causality HITs:</label>
    <div>
    <#list causalityHitIds as causalityHitId>
        <span class="<#if completedCausalityHitIds?seq_contains(causalityHitId)>hit_id_done</#if>">
        ${causalityHitId}
        </span>
        <#sep><span>,  </span></#sep>
    </#list>
    </div>
</div>
</body>
</html>