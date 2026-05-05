
var bot_graph = function(el, data){
  if(el.children[0]) {
    el.children[0].remove();
  }

  var margin = {top: 20, right: 0, bottom: 30, left: 50},
      width = 400 - margin.left - margin.right,
      height = 200 - margin.top - margin.bottom;

  var x = d3.time.scale()
    .range([0, width])
    .domain(d3.extent(data, function(d) { return d.inst; }));

  var xi = d3.scale.linear()
    .range([0, width])
    .domain(d3.extent(data, function(d,i) { return i; }));

  var y = d3.scale.linear()
    .range([height,0])
    .domain([0, 3500]);

  var tornadoArea = d3.svg.area()
    .interpolate('step-after')
    .x(function(d,i) { return xi(i); x(d.inst);})
    .y1(function(d) { return y(d.rating + d['rating-dev']);})
    .y0(function(d) { return y(d.rating - d['rating-dev']);})


  var yAxis = d3.svg.axis()
    .scale(y)
    .tickValues([0, 500, 1000, 1500, 2000, 2500, 3000, 3500])
    .orient("left");

  var graph = d3.select(el).append('svg')
    .attr('height', height + margin.top + margin.bottom)
    .attr('width', width + margin.left + margin.right)
    .append("g")
      .attr("transform", "translate(" + margin.left + "," + margin.top + ")")

  graph.append('path')
    .datum(data)
    .attr("d", tornadoArea)
    .attr("class", "area")
    .style("fill", "#eee")

  graph.selectAll(".line-segment")
    .data(data.slice(0, data.length - 1))
    .enter()
    .append("path")
    .attr("class", "line")
    .attr("d", function(d, i) {
      return "M" + xi(i) + "," + y(d.rating)
           + "H" + xi(i + 1)
           + "V" + y(data[i + 1].rating);
    })
    .style("stroke", function(d) { return d.color; })
    .style("fill", "none")

  var axis = graph.append("g")
    .attr("class", "y axis")
    .call(yAxis);

  axis.selectAll("path, line")
    .style("fill", "none")
    .style("stroke", "#ccc")
    .style("shape-rendering", "crispEdges");

  axis.selectAll("text")
    .style("fill", "#ccc")
    .style("font-size", "10px");

  var seenDigests = {};
  var firstOccurrences = data.reduce(function(acc, d, i) {
    if (d.digest && !seenDigests[d.digest]) {
      seenDigests[d.digest] = true;
      acc.push({d: d, i: i});
    }
    return acc;
  }, []);

  var deployGroups = graph.selectAll(".deploy-group")
    .data(firstOccurrences)
    .enter()
    .append("g")
    .attr("class", "deploy-group");

  deployGroups.append("line")
    .attr("class", "deploy-line")
    .attr("x1", function(entry) { return xi(entry.i); })
    .attr("x2", function(entry) { return xi(entry.i); })
    .attr("y1", 0)
    .attr("y2", height)
    .attr("stroke", function(entry) { return entry.d.color; })
    .attr("stroke-width", 2)
    .append("title")
    .text(function(entry) { return "Artifact Digest: " + entry.d.digest; });

  var badgeWidth = 42;
  var badgeHeight = 14;

  deployGroups.append("rect")
    .attr("x", function(entry) { return xi(entry.i) - badgeWidth / 2; })
    .attr("y", 0)
    .attr("width", badgeWidth)
    .attr("height", badgeHeight)
    .attr("rx", 2)
    .attr("fill", function(entry) { return entry.d.color; });

  deployGroups.append("text")
    .attr("x", function(entry) { return xi(entry.i); })
    .attr("y", badgeHeight - 3)
    .attr("text-anchor", "middle")
    .attr("fill", "white")
    .attr("font-size", "9px")
    .attr("font-family", "monospace")
    .text(function(entry) { return entry.d.digest ? entry.d.digest.slice(0, 6) : ""; });

}
