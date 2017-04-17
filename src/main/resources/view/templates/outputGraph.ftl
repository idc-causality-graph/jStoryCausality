<html>

<head>
    <script src="https://code.jquery.com/jquery-3.2.1.min.js"
            integrity="sha384-xBuQ/xzmlsLoJpyjoggmTEz8OWUFM0/RC5BsqQBDX2v5cMvDHcMakNTNrHIW2I5f"
            crossorigin="anonymous"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/vis/4.19.1/vis.min.js"
            integrity="sha384-hBrV4jNV5IXr7qI1FRqoXh1CcuGKG7pUkHEzGdc3CPNfsBqZXsMHFob790BH3Rdd"
            crossorigin="anonymous"></script>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/vis/4.19.1/vis.min.css"
          integrity="sha384-ZAt6HD4d7/ihTBxD8xtsI0We2gW8Qtr3657zMpcGmTg4A+VJydp4L2dbT+JaYS8Z" crossorigin="anonymous">
    <script src="https://cdnjs.cloudflare.com/ajax/libs/micromustache/4.1.1/micromustache.min.js"
            integrity="sha384-8TpUV/Xw6Z899iNaIwlUghCAtR3JnqLxrR5PziHRMidPTR7PWlMx/nU9GW8GIkfT"
            crossorigin="anonymous"></script>

    <script type="application/javascript">
        $(function () {

            // TEMPLATE STUFF
            var MAX_EDGES = ${causalityReplicaFactor};

            //rawData contains all (pseaduo) leaf nodes, in the chronological order, as appears in the upper nodeLevels
            var rawData = ${rawData};

            // END OF TEMPLATE STUFF

            var SHORTEN_LENGTH = 50;
            var MOST_IMPORTANT_COLOR = [0, 204, 0];
            var LEST_IMPORTANT_COLOR = [255, 51, 0];

            var datasetNodes = [];
            var edgeNodes = [];
            var datasetMap = {};

            function pickHex(color1, color2, weight) {
                var w1 = weight;
                var w2 = 1 - w1;
                var rgb = [Math.round(color1[0] * w1 + color2[0] * w2),
                    Math.round(color1[1] * w1 + color2[1] * w2),
                    Math.round(color1[2] * w1 + color2[2] * w2)];
                return "rgb(" + rgb.join(',') + ")";
            }

            function shorten(str) {
                if (str && str.length > SHORTEN_LENGTH) {
                    return str.substr(0, SHORTEN_LENGTH) + "...";
                }
                return str;
            }

            var minScore = 1;
            var maxScore = 0;
            rawData.forEach(function (item) {
                minScore = Math.min(item.importanceScore, minScore);
                maxScore = Math.max(item.importanceScore, maxScore);
            });
            rawData.forEach(function (item, i) {
                datasetMap[item.id] = item;
                var normScore = (item.importanceScore - minScore) / (maxScore - minScore);
                datasetNodes.push($.extend(
                        {
                            y: i,
                            label: 'Part ' + (i + 1),
                            color: pickHex(MOST_IMPORTANT_COLOR, LEST_IMPORTANT_COLOR, normScore),
                            title: shorten(item.summary)
                        }, item));
                var itemId = item.id;
                item.causeTo.forEach(function (toItemId) {
                    var newEdge = {
                        from: itemId,
                        to: toItemId,
                        width: 1,
                        votes: 1,
                        color: pickHex(MOST_IMPORTANT_COLOR, LEST_IMPORTANT_COLOR, 1 / MAX_EDGES)
                    };
                    var existingEdge = edgeNodes.find(function (it) {
                        return it.from == newEdge.from && it.to == newEdge.to;
                    });
                    if (!existingEdge) {
                        edgeNodes.push(newEdge);
                    } else {
                        existingEdge.votes += 1;
                        existingEdge.width += 2;
                        existingEdge.color = pickHex(MOST_IMPORTANT_COLOR, LEST_IMPORTANT_COLOR, existingEdge.votes / MAX_EDGES);

                    }
                });
            });
            var nodes = new vis.DataSet(datasetNodes);
            var edges = new vis.DataSet(edgeNodes);

            // create a network
            var container = document.getElementById('mynetwork');

            // provide the data in the vis format
            var data = {
                nodes: nodes,
                edges: edges
            };
            var options = {
                layout: {
                    randomSeed: 280481
                },
                nodes: {
                    shape: "box"
                },

                edges: {
                    arrows: 'to'
                }
            };

            // initialize your network!
            var network = new vis.Network(container, data, options);
            var highlightItem = function (item) {
                $(".item").removeClass("selected_item");
                var selectedItem = $("#data_item_" + item.id);
                selectedItem.addClass("selected_item");
                return selectedItem;
            };
            var clickEvent = function (event) {
                if (!event.nodes || event.nodes.length != 1) {
                    return;
                }
                var item = datasetMap[event.nodes[0]];
                highlightItem(item).get(0).scrollIntoView();
            };

            network.on('click', clickEvent);

            var container = $("#items_container");
            var item_template = $("#item_template").html();
            var handleNewlines = function (str) {
                return str.split("\n").join("<br>");
            };
            rawData.forEach(function (item, i) {
                var data = {
                    id: item.id,
                    item_number: i + 1,
                    score: item.importanceScore,
                    summary: handleNewlines(item.summary)
                };
                var itemDiv = $("<div>").html(micromustache.render(item_template, data));
                itemDiv.click(function () {
                    highlightItem(item);
                    network.selectNodes([item.id]);
                });
                container.append(itemDiv);
            });
            setTimeout(function () {
                network.fit(rawData.map(function (item) {
                    return item.id;
                }));
                network.redraw();
            }, 10);
        });
    </script>

    <script type="text/x-mustache" id="item_template">
    <div class="item" id="data_item_{{id}}">
<p>Part # {{item_number}}</p>
<p>Score: {{score}}</p>
<p>{{summary}}</p>
</div>

    </script>

    <style>
        #items_container {
            vertical-align: top;
            height: 100%;
            width: 25%;
            display: inline-block;
            overflow-y: scroll;
        }

        .item {
            padding: 5px;
            border: solid black 2px;
            margin-bottom: 3px;
            background-color: #adadad;
        }

        #mynetwork {
            vertical-align: top;
            height: 100%;
            width: 65%;
            display: inline-block;
        }

        .selected_item {
            background-color: cornsilk;
        }
    </style>
</head>

<body>
<div id="items_container" style=""></div>
<div id="mynetwork" style="">Network</div>
</body>
</html>