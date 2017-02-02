<!DOCTYPE html>
<html>
<head>
    <link rel="stylesheet" href="/css/bootstrap.min.css">
    <link rel="stylesheet" href="/css/bootstrap-grid.min.css">
    <script src="/jquery-3.1.1.min.js"></script>
    <script src="/js/bootstrap.min.js"></script>
    <style>
        ul.tree, ul.tree ul {
            list-style-type: none;
            background: url(http://odyniec.net/articles/turning-lists-into-trees/vline.png) repeat-y;
            margin: 0;
            padding: 0;
        }

        ul.tree ul {
            margin-left: 20px;
            padding-top: 10px;
        }

        ul.tree li {
            margin: 0;
            padding: 0 12px;
            line-height: 20px;
            background: url(http://odyniec.net/articles/turning-lists-into-trees/node.png) no-repeat;
        }

        ul.tree li:last-child {
            background: #fff url(http://odyniec.net/articles/turning-lists-into-trees/lastnode.png) no-repeat;
        }
    </style>

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
    <div class="btn-group sticky-top" role="group">
        <div class="btn-toolbar" role="toolbar" aria-label="Toolbar with button groups">
            <form action="/contextTree/save" method="post">
                <button class="btn btn-info" type="submit">Save</button>
            </form>
            <form action="/contextTree/progressUp" method="post">
                <button class="btn btn-secondary" type="submit">Progress up hits</button>
            </form>
            <form action="/contextTree/reload" method="post">
                <button class="btn btn-secondary" type="submit">
                    Reload from disk
                </button>
            </form>
        </div>
    </div>
   <div>&nbsp;</div>
${htmlTree}
</div>
</body>
</html>