$ErrorActionPreference = 'Stop'

$source = @'
using System;
using System.Runtime.InteropServices;

[ComImport]
[Guid("56FDF342-FD6D-11d0-958A-006097C9A090")]
[InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
public interface ITaskbarList {
    [PreserveSig] int HrInit();
    [PreserveSig] int AddTab(IntPtr hwnd);
    [PreserveSig] int DeleteTab(IntPtr hwnd);
    [PreserveSig] int ActivateTab(IntPtr hwnd);
    [PreserveSig] int SetActiveAlt(IntPtr hwnd);
}

public static class TaskbarButton {
    public static int SetVisible(IntPtr hwnd, bool visible) {
        var type = Type.GetTypeFromCLSID(new Guid("56FDF344-FD6D-11d0-958A-006097C9A090"));
        var list = (ITaskbarList)Activator.CreateInstance(type);
        try {
            int result = list.HrInit();
            return result == 0 ? (visible ? list.AddTab(hwnd) : list.DeleteTab(hwnd)) : result;
        } finally {
            Marshal.ReleaseComObject(list);
        }
    }
}
'@

Add-Type -TypeDefinition $source
$targetProcessId = __PID__
$visible = __VISIBLE__
for ($attempt = 0; $attempt -lt 40; $attempt++) {
    $process = Get-Process -Id $targetProcessId -ErrorAction SilentlyContinue
    if ($null -eq $process) { exit 1 }
    if ($process.MainWindowHandle -ne [IntPtr]::Zero) {
        $result = [TaskbarButton]::SetVisible($process.MainWindowHandle, $visible)
        if ($result -eq 0) { exit 0 }
    }
    Start-Sleep -Milliseconds 500
}
exit 1
