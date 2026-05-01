use serde_json::{Map, Value};
use std::io::{self, Read};

fn run(input: &Map<String, Value>) -> Map<String, Value> {
    let mut output = Map::new();
    output.insert("pong".to_string(), input["ping"].clone());
    output
}

fn main() {
    let mut buf = String::new();
    io::stdin().read_to_string(&mut buf).expect("failed to read stdin");

    let input: Map<String, Value> = serde_json::from_str(&buf).expect("failed to parse JSON");

    let output = run(&input);

    println!("{}", serde_json::to_string(&output).expect("failed to encode JSON"));
}
