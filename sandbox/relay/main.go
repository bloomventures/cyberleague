package main

import (
	"bytes"
	"fmt"
	"net"
	"os"
	"os/exec"
	"time"

	"github.com/fxamacker/cbor/v2"
	"github.com/mdlayher/vsock"
)

const (
	vsockPort      = 52525
	execTimeoutSec = 30
)

var debug = true

func logf(format string, args ...any) {
	if debug {
		fmt.Fprintf(os.Stderr, "relay: "+format+"\n", args...)
	}
}

type request struct {
	Artifact []byte   `cbor:"artifact"`
	Stdin    []byte   `cbor:"stdin"`
	Argv     []string `cbor:"argv"`
}

type response struct {
	Exit   int    `cbor:"exit"`
	Stdout string `cbor:"stdout"`
	Stderr string `cbor:"stderr"`
}

func sendErr(conn net.Conn, msg string) {
	logf("error: %s", msg)
	cbor.NewEncoder(conn).Encode(response{Exit: -1, Stderr: msg})
}

func handleConn(conn net.Conn) {
	defer conn.Close()

	logf("parsing request")
	var req request
	if err := cbor.NewDecoder(conn).Decode(&req); err != nil {
		sendErr(conn, "decode: "+err.Error())
		return
	}

	logf("creating temp file")
	tmp, err := os.CreateTemp("", "agent-*.bin")
	if err != nil {
		sendErr(conn, "temp file: "+err.Error())
		return
	}
	defer os.Remove(tmp.Name())

	logf("writing artifact")
	if _, err = tmp.Write(req.Artifact); err != nil {
		sendErr(conn, "write artifact: "+err.Error())
		return
	}
	tmp.Close()

	logf("changing artifact permissions")
	if err = os.Chmod(tmp.Name(), 0o755); err != nil {
		sendErr(conn, "chmod: "+err.Error())
		return
	}

	logf("preparing command")
	if len(req.Argv) == 0 {
		sendErr(conn, "empty argv")
		return
	}
	argv := make([]string, len(req.Argv))
	for i, s := range req.Argv {
		if s == "$ARTIFACT" {
			argv[i] = tmp.Name()
		} else {
			argv[i] = s
		}
	}
	cmd := exec.Command(argv[0], argv[1:]...)
	cmd.Env = os.Environ()
	cmd.Stdin = bytes.NewReader(req.Stdin)

	var stdout, stderr bytes.Buffer
	cmd.Stdout = &stdout
	cmd.Stderr = &stderr

	logf("executing: %v", argv)
	if err = cmd.Start(); err != nil {
		sendErr(conn, "start: "+err.Error())
		return
	}

	done := make(chan error, 1)
	go func() { done <- cmd.Wait() }()

	select {
	case err = <-done:
	case <-time.After(execTimeoutSec * time.Second):
		cmd.Process.Kill()
		sendErr(conn, "timeout")
		return
	}

	exitCode := 0
	if err != nil {
		if exitErr, ok := err.(*exec.ExitError); ok {
			exitCode = exitErr.ExitCode()
		}
	}

	logf("result: exit=%d stdout=%q stderr=%q", exitCode, stdout.String(), stderr.String())
	cbor.NewEncoder(conn).Encode(response{
		Exit:   exitCode,
		Stdout: stdout.String(),
		Stderr: stderr.String(),
	})
}

func main() {
	ln, err := vsock.Listen(vsockPort, nil)
	if err != nil {
		logf("listen: %v", err)
		os.Exit(1)
	}
	defer ln.Close()

	logf("started, listening on vsock port %d", vsockPort)

	for {
		conn, err := ln.Accept()
		if err != nil {
			logf("accept: %v", err)
			os.Exit(1)
		}
		go handleConn(conn)
	}
}
