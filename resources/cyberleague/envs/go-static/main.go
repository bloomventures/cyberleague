package main

import (
	"encoding/json"
	"fmt"
	"io"
	"os"
)

func run(input map[string]any) map[string]any {
	return map[string]any{
		"pong": input["ping"],
	}
}

func main() {
	data, err := io.ReadAll(os.Stdin)
	if err != nil {
		fmt.Fprintf(os.Stderr, "error reading stdin: %v\n", err)
		os.Exit(1)
	}

	var input map[string]any
	if err := json.Unmarshal(data, &input); err != nil {
		fmt.Fprintf(os.Stderr, "error parsing JSON: %v\n", err)
		os.Exit(1)
	}

	output, err := json.Marshal(run(input))
	if err != nil {
		fmt.Fprintf(os.Stderr, "error encoding JSON: %v\n", err)
		os.Exit(1)
	}

	fmt.Println(string(output))
}
