function main() {
	var nodeRef = args.nodeRef;
	var folderNode = search.findNode(nodeRef);
	if (!folderNode) {
		throw new Error("QR Code browseFolder | nodeRef is invalid | nodeRef:"+nodeRef);
	}
	
	if (userHasPermission(folderNode, username)) { //username: public username from java GetTicket class
		model.fileFolders = getFileFolders(folderNode);
	}
	
	model.folderPath = (folderNode.displayPath == "/Company Home") ? folderNode.name : folderNode.displayPath.replace("/Company Home/", "")+"/"+folderNode.name;
}

function getFileFolders(folderNode) {
	var folders = [];
	var files = [];
	var childs = folderNode.childFileFolders(true, true);

	childs.forEach(function(child){
		if(child.isContainer){
			folders.push(child);
		} else {
			files.push(child);
		}
	});

	folders.sort(function(a, b){
		return a.name.localeCompare(b.name, "en", { sensitivity: "base" });
	});

	files.sort(function(a, b){
		return a.name.localeCompare(b.name, "en", { sensitivity: "base" });
	});

	return folders.concat(files);
}

function userHasPermission(folderNode, userName) {
	return folderNode.permissions.toString().indexOf(userName) >= 0
}

main();