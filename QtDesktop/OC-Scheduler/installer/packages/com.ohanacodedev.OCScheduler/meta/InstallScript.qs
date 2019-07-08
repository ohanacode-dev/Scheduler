
function Component()
{
    installer.gainAdminRights();
    gui.pageById(QInstaller.TargetDirectory).entered.connect(this, this.TargetDirectoryPageEntered);

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


Component.prototype.TargetDirectoryPageEntered = function()
{
    installer.execute("notify-send", "TargetDirectoryPageEntered");

    var dir = installer.value("TargetDir");
    var uninstaller = dir + "/Uninstall OC_Scheduler";  
    var uninst_script = dir + "/auto_uninstall.qs";

    if (systemInfo.productType == "windows"){
 	uninstaller += ".exe";
    }

    if (installer.fileExists(dir)){
	if (installer.fileExists(uninstaller) && installer.fileExists(uninst_script)) {
	    var uninstResult = QMessageBox.question("overwrite.question", "Installer", "Target folder:" + dir + "\nalready has an instalation. Overwrite?",
				          QMessageBox.Yes | QMessageBox.No);
	    if (uninstResult == QMessageBox.Yes) {
	       installer.execute(uninstaller, "--script=" + dir + "/auto_uninstall.qs");
	    }        
	}else{
            var overwriteResult = QMessageBox.question("overwrite.question", "Installer", "Target folder:" + dir + "\nis not empty. Overwrite?",
				          QMessageBox.Yes | QMessageBox.No);
	    if (overwriteResult == QMessageBox.Yes) {
               if (systemInfo.kernelType == "linux"){
	           installer.execute("rm", new Array("-rf", dir));
               }

               if (systemInfo.productType == "windows"){
	           installer.execute("rmdir", new Array(dir, "/s", "/q"));
               }
	    }   
        }
    }
}