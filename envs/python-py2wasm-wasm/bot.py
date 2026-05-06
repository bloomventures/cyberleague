import json
import sys


def run(input):
    return {"pong": input["ping"]}


def main():
    data = sys.stdin.read()
    input = json.loads(data)
    output = run(input)
    print(json.dumps(output))


main()
