param([Parameter(Mandatory = $true)][string]$Workspace)

# Scope window changes to the explicitly marked client for this workspace.
Add-Type @'
using System;
using System.Runtime.InteropServices;
public static class FactoryQaWindows {
    public delegate bool Visitor(IntPtr window, IntPtr data);
    [DllImport("user32.dll")] public static extern bool EnumWindows(Visitor visitor, IntPtr data);
    [DllImport("user32.dll")] public static extern uint GetWindowThreadProcessId(IntPtr window, out uint process);
    [DllImport("user32.dll")] public static extern int GetWindowLong(IntPtr window, int index);
    [DllImport("user32.dll")] public static extern int SetWindowLong(IntPtr window, int index, int value);
    [DllImport("user32.dll")] public static extern bool SetLayeredWindowAttributes(IntPtr window, uint color, byte alpha, uint flags);
    [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr window, int command);
}
'@

$deadline = [DateTime]::UtcNow.AddMinutes(10)
$seenClient = $false
$changed = [Collections.Generic.HashSet[long]]::new()
while ([DateTime]::UtcNow -lt $deadline) {
    $clients = @(Get-CimInstance Win32_Process -Filter "Name = 'java.exe' OR Name = 'javaw.exe'" |
        Where-Object { $_.CommandLine -and $_.CommandLine.Contains('-Dgtng.factory.qa=true') -and $_.CommandLine.Contains($Workspace) })
    if ($clients.Count -eq 0 -and $seenClient) { break }
    if ($clients.Count -gt 0) { $seenClient = $true }
    $clientIds = @($clients | ForEach-Object { [uint32]$_.ProcessId })
    [FactoryQaWindows]::EnumWindows({
        param($window, $data)
        [uint32]$ownerId = 0
        [void][FactoryQaWindows]::GetWindowThreadProcessId($window, [ref]$ownerId)
        if ($clientIds -contains $ownerId -and $changed.Add($window.ToInt64())) {
            [void][FactoryQaWindows]::ShowWindow($window, 0)
            $style = [FactoryQaWindows]::GetWindowLong($window, -20)
            # Tool window, layered opacity zero and click-through; remove the taskbar app-window flag.
            $style = ($style -band (-bnot 0x40000)) -bor 0x800A0
            [void][FactoryQaWindows]::SetWindowLong($window, -20, $style)
            [void][FactoryQaWindows]::SetLayeredWindowAttributes($window, 0, 0, 2)
            [void][FactoryQaWindows]::ShowWindow($window, 4)
            [Console]::WriteLine("Hidden QA window for process " + $ownerId)
        }
        return $true
    }, [IntPtr]::Zero) | Out-Null
    Start-Sleep -Milliseconds 250
}
