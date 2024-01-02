const tableLog = document.getElementById("table-logging");
const filterFinest = document.getElementById("FINEST");
const filterFiner = document.getElementById("FINER");
const filterFine = document.getElementById("FINE");
const filterInfo = document.getElementById("INFO");
const filterWarning = document.getElementById("WARNING");
const filterSevere = document.getElementById("SEVERE");

function splitMessage(lines) {
	const messages = lines.split("\n");
	messages.pop(); // Last is empty
	let results = [];
	messages.forEach(function(item) {
		let row = [];
		row.push(item.substring(0, 23)); // Get timestamp
		row.push(item.substring(24, 31).trimEnd()); // Get level
		let firstIndex = item.indexOf(" ", 36);
		if (firstIndex == -1) { console.log(`1:parse failure ${item}`); return; }
		let tmp = item.substring(31, firstIndex).trimEnd(); // Get class
		tmp = tmp.substring(tmp.lastIndexOf(".") + 1); // Remove package name 
		row.push(tmp);
		let lastIndex = item.indexOf(" ", firstIndex + 1);
		if (lastIndex == -1) { console.log(`2:parse failure ${item}`); return; }
		row.push(item.substring(firstIndex, lastIndex)); // Get function
		firstIndex = item.indexOf("|", lastIndex);
		if (firstIndex == -1) { console.log(`3:parse failure ${item}`); return; }
		row.push(item.substring(firstIndex + 2)); // Get message
		results.push(row);
	});
	return results;
}
function addTableMessage(lines) {
	const processedMsg = splitMessage(lines);
	if (lines.length > 128) tableLog.style.display = "none";
	processedMsg.forEach(function(line) {
		const row = tableLog.insertRow();
		row.insertCell().innerText = line[0];
		const cell1 = row.insertCell();
		cell1.classList.add(line[1]);
		cell1.innerText = line[1];
		switch (line[1]) {
			case "FINEST": if (!filterFinest.checked) row.style.display = "none"; break;
			case "FINER": if (!filterFiner.checked) row.style.display = "none"; break;
			case "FINE": if (!filterFine.checked) row.style.display = "none"; break;
			case "INFO": if (!filterInfo.checked) row.style.display = "none"; break;
			case "WARNING": if (!filterWarning.checked) row.style.display = "none"; break;
			case "SEVERE": if (!filterSevere.checked) row.style.display = "none"; break;
		}
		row.insertCell().innerText = line[2];
		row.insertCell().innerText = line[3];
		row.insertCell().innerText = line[4];
	});
	if (lines.length > 128) tableLog.style.display = "initial";
}
function scrollToBottom() {
	const cont = document.getElementById("main-content-wrapper");
	cont.scrollTo({top: cont.children[0].scrollHeight, behavior: "smooth"});
}
new ResizeObserver(scrollToBottom).observe(document.getElementById("main-content-wrapper").children[0]);

const tableWS = openWS("/ws/logging",
(/*event*/) => { // Open
	tableWS.send("all");
},
(event) => { // Close
	alert("closed");
	console.log(event);
},
(error) => { // Error
	alert("error");
	console.log(error);
},
(event) => { // RX
	//console.log(event.data);
	addTableMessage(event.data);
});