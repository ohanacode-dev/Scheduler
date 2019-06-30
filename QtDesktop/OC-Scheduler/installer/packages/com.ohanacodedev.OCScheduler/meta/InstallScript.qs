
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
}
