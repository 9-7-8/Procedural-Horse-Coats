// PreToolUse guard for Bash and PowerShell: refuse every process-killing command.
//
// On 2026-10-01 a session stopped a dev server by command-line pattern and killed
// the LIVE Minecraft server (C:\Users\Isabel\26.1.2-neoforge-server) with players on
// it. Its command line carries no path and its PID changes on every restart, so no
// filter written in the moment can be trusted to miss it. The one sanctioned way to
// stop a process is tools/stop-dev-java.ps1, an allowlist of this repo's own
// processes; its invocation contains none of the words below, so it passes.
// Gradle daemons stop with ./gradlew --stop; background shells with TaskStop.
//
// Deliberately broad. A false positive costs a reworded command; a false negative
// cost three players their session.

let raw = '';
process.stdin.setEncoding('utf8');
process.stdin.on('data', (c) => { raw += c; });
process.stdin.on('end', () => {
    let command = '';
    try {
        command = String(JSON.parse(raw)?.tool_input?.command ?? '');
    } catch {
        return; // not a tool call we can read - let the harness decide
    }
    const verdict = check(command);
    if (verdict) {
        process.stdout.write(JSON.stringify({
            hookSpecificOutput: {
                hookEventName: 'PreToolUse',
                permissionDecision: 'deny',
                permissionDecisionReason:
                    `Blocked: ${verdict}. Killing processes by hand is not allowed in this repo - `
                    + 'it once took down the live server. Use `pwsh -File tools/stop-dev-java.ps1` '
                    + '(add -WhatIf first, or -Id <pid>), `./gradlew --stop` for Gradle daemons, '
                    + 'or TaskStop for a background shell. Never stop anything under '
                    + '26.1.2-neoforge-server; ask the owner.',
            },
        }));
    }
});

// Words that kill, wherever they appear as a command. PowerShell cmdlets and .NET
// calls are matched anywhere: they have no innocent use in this repo's commands.
const ANYWHERE = [
    [/\bStop-Process\b/i, 'Stop-Process'],
    [/\bspps\b/i, 'spps (Stop-Process alias)'],
    [/\.Kill\s*\(/i, '.Kill()'],
    [/\bTerminate\b/i, 'a Terminate call'],
    [/\btaskkill\b/i, 'taskkill'],
    [/\btskill\b/i, 'tskill'],
    [/\bwmic\b[^\n]*\b(delete|terminate)\b/i, 'wmic delete/terminate'],
    [/\bpkill\b/i, 'pkill'],
    [/\bkillall\b/i, 'killall'],
];
// `kill` is also an ordinary English word (commit messages, grep patterns), so only
// as a command: at the start of the line or after ; & | ( or xargs/sudo/exec.
const KILL_AS_COMMAND = /(^|[;&|(\n`]|\b(xargs|sudo|exec|nohup)\s+)\s*kill\b(?!-)/i;

function check(command) {
    for (const [re, name] of ANYWHERE) {
        if (re.test(command)) return name;
    }
    if (KILL_AS_COMMAND.test(command)) return 'kill';
    return null;
}
