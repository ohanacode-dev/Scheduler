
function Component()
{
    // default constructor
}

Component.prototype.createOperations = function()
{
    // This actually installs the files
    component.createOperations();

    if (systemInfo.productType == "windows") {
       // This could be changed to use @RunProgram@ and other variables
       component.addOperation("CreateShortcut", "@TargetDir@/OC-Scheduler.exe", "@StartMenuDir@/OC-Scheduler.lnk", "workingDirectory=@TargetDir@", "iconPath=@TargetDir@/oc_logo.ico");      
    }

    if (systemInfo.kernelType == "linux") {
       component.addOperation("CreateDesktopEntry", "@HomeDir@/.local/share/applications/OC-Scheduler.desktop", 
	"Version=1.0\nType=Application\nTerminal=false\nExec=@TargetDir@/OC-Scheduler\nName=OC-Scheduler\nIcon=@TargetDir@/oc_logo.png\nName=OC-Scheduler\nCategories=Utility");
	component.addOperation("Copy", "@HomeDir@/.local/share/applications/OC-Scheduler.desktop", "@HomeDir@/Desktop/OC-Scheduler.desktop");
	component.addOperation("Execute", "sudo", "apt", "install", "libqt5x11extras5");
    }
}

