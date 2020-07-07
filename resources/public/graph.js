
var bot_graph = function(el, data){

  var margin = {top: 20, right: 0, bottom: 30, left: 50},
      width = 300 - margin.left - margin.right,
      height = 200 - margin.top - margin.bottom;

  var x = d3.time.scale()
    .range([0, width])
    .domain(d3.extent(data, function(d) { return d.inst; }));

  var xi = d3.scale.linear()
    .range([0, width])
    .domain(d3.extent(data, function(d,i) { return i; }));

  var y = d3.scale.linear()
    .range([height,0])
    .domain([0, 2500]);

  var tornadoArea = d3.svg.area()
    .interpolate('step')
    .x(function(d,i) { return xi(i); x(d.inst);})
    .y1(function(d) { return y(d.rating + d['rating-dev']);})
    .y0(function(d) { return y(d.rating - d['rating-dev']);})

  var middleLine = d3.svg.line()
    .x(function(d,i){return xi(i); x(d.inst);})
    .y(function(d){return y(d.rating);})
    .interpolate("step")

  var yAxis = d3.svg.axis()
    .scale(y)
    .tickValues([0, 500, 1000, 1500, 2000, 2500])
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

  graph.append('path')
    .datum(data)
    .attr("class", "line")
    .attr("d", middleLine)

  graph.append("g")
  .attr("class", "y axis")
  .call(yAxis)

}
