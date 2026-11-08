<#
deck_inject.ps1 - director only. Type a task into an agy agent's OPEN window, without using the mouse
or keyboard focus: attach to that window's console and write key events into its input buffer.

  powershell -File deck_inject.ps1 -Agent agy_director -Text "[director] Task A1: ..."

The agent name is looked up in AGENTS.md ("- agy_director: `<agy session id>`"), then the running agy.exe
started with "--conversation <id>" is found. Fails if that window is not open.

Do not type into the target window while this runs, or the two inputs mix.
#>
param(
    [string]$Agent,
    [int]$TargetPid,
    [Parameter(Mandatory)] [string]$Text,
    [switch]$NoEnter,
    [string]$ResultFile   # internal: set when the script re-runs itself in a hidden process
)

$ErrorActionPreference = 'Stop'
$Root = $PSScriptRoot

function Find-AgentPid([string]$name) {
    $agentMd = Get-Content (Join-Path $Root 'AGENTS.md') -Raw -Encoding UTF8
    $m = [regex]::Match($agentMd, "(?m)^-\s*$([regex]::Escape($name)):\s*``([0-9a-fA-F-]{36})``")
    if (-not $m.Success) { throw "No agy session id for '$name' in AGENTS.md." }
    $id = $m.Groups[1].Value

    $proc = Get-CimInstance Win32_Process -Filter "Name='agy.exe'" |
        Where-Object { $_.CommandLine -like "*$id*" } | Select-Object -First 1
    if (-not $proc) { throw "'$name' (session $id) has no open agy window. Open it with: agy --conversation $id" }
    return [int]$proc.ProcessId
}

# ---- outer call: resolve the target, then re-run hidden (attaching needs a process with its own console) ----
if (-not $ResultFile) {
    if (-not $TargetPid) {
        if (-not $Agent) { throw 'Give -Agent <name> (or -TargetPid <pid>).' }
        $TargetPid = Find-AgentPid $Agent
    }

    $out = [System.IO.Path]::GetTempFileName()
    $argList = @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', "`"$PSCommandPath`"",
                 '-TargetPid', $TargetPid, '-Text', "`"$($Text -replace '"', '\"')`"", '-ResultFile', "`"$out`"")
    if ($NoEnter) { $argList += '-NoEnter' }

    Start-Process powershell -ArgumentList $argList -WindowStyle Hidden -Wait
    $result = Get-Content $out -Raw -Encoding UTF8
    Remove-Item $out -ErrorAction SilentlyContinue
    Write-Host "$(if ($Agent) { $Agent } else { "pid $TargetPid" }): $result"
    if ($result -notlike 'OK*') { exit 1 }
    return
}

# ---- inner call: attach to the target console and write the keys ----
Add-Type -TypeDefinition @'
using System;
using System.Runtime.InteropServices;

public static class ConsoleInject
{
    [StructLayout(LayoutKind.Explicit, CharSet = CharSet.Unicode)]
    struct INPUT_RECORD
    {
        [FieldOffset(0)] public ushort EventType;
        [FieldOffset(4)] public int bKeyDown;
        [FieldOffset(8)] public ushort wRepeatCount;
        [FieldOffset(10)] public ushort wVirtualKeyCode;
        [FieldOffset(12)] public ushort wVirtualScanCode;
        [FieldOffset(14)] public char UnicodeChar;
        [FieldOffset(16)] public uint dwControlKeyState;
    }

    [DllImport("kernel32.dll", SetLastError = true)] static extern bool FreeConsole();
    [DllImport("kernel32.dll", SetLastError = true)] static extern bool AttachConsole(uint pid);
    [DllImport("kernel32.dll", SetLastError = true, CharSet = CharSet.Unicode)]
    static extern IntPtr CreateFile(string name, uint access, uint share, IntPtr sec, uint disposition, uint flags, IntPtr template);
    [DllImport("kernel32.dll", SetLastError = true, CharSet = CharSet.Unicode)]
    static extern bool WriteConsoleInput(IntPtr h, INPUT_RECORD[] records, uint count, out uint written);
    [DllImport("kernel32.dll")] static extern bool CloseHandle(IntPtr h);

    static INPUT_RECORD Key(char c, ushort vk, bool down)
    {
        return new INPUT_RECORD { EventType = 1, bKeyDown = down ? 1 : 0, wRepeatCount = 1, wVirtualKeyCode = vk, UnicodeChar = c };
    }

    public static string Send(uint pid, string text, bool enter)
    {
        FreeConsole();
        if (!AttachConsole(pid)) return "AttachConsole failed, error " + Marshal.GetLastWin32Error();

        // GENERIC_READ | GENERIC_WRITE, FILE_SHARE_READ | FILE_SHARE_WRITE, OPEN_EXISTING
        IntPtr h = CreateFile("CONIN$", 0xC0000000, 3, IntPtr.Zero, 3, 0, IntPtr.Zero);
        if (h == new IntPtr(-1)) return "CreateFile CONIN$ failed, error " + Marshal.GetLastWin32Error();

        var list = new System.Collections.Generic.List<INPUT_RECORD>();
        foreach (char c in text) { list.Add(Key(c, 0, true)); list.Add(Key(c, 0, false)); }
        if (enter) { list.Add(Key('\r', 0x0D, true)); list.Add(Key('\r', 0x0D, false)); }

        uint written;
        bool ok = WriteConsoleInput(h, list.ToArray(), (uint)list.Count, out written);
        int err = Marshal.GetLastWin32Error();
        CloseHandle(h);
        FreeConsole();
        return ok ? "OK wrote " + written + " of " + list.Count + " events" : "WriteConsoleInput failed, error " + err;
    }
}
'@

$result = [ConsoleInject]::Send([uint32]$TargetPid, $Text, -not $NoEnter)
Set-Content -Path $ResultFile -Value $result -Encoding UTF8
