function get(path) {
    return Data.exists(path) ? Data.get(path) : "0";
}

function set(path, value) {
    Data.set(path, value);
    return "";
}

function add(path, amount) {
    var current = parseFloat(get(path));
    var newValue = current + parseFloat(amount);
    Data.set(path, newValue.toString());
    return "";
}

function getDuration(startPath, endPath) {
    var start = parseInt(get(startPath));
    var end = parseInt(get(endPath));

    var diff = end - start;
    if (diff < 0) diff = 0;

    var hours = Math.floor(diff / (1000 * 60 * 60));
    var minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
    var seconds = Math.floor((diff % (1000 * 60)) / 1000);

    if (hours > 0) {
        return hours + "h " + minutes + "m " + seconds + "s";
    } else if (minutes > 0) {
        return minutes + "m " + seconds + "s";
    } else {
        return seconds + "s";
    }
}
function compareBestTime() {
    var bestStr = get("best_time");
    var startStr = get("new_world_time");
    var endStr = get("world_time");

    var best = parseFloat(bestStr) || 0;
    var start = parseFloat(startStr) || 0;
    var end = parseFloat(endStr) || 0;

    var currentRunDuration = end - start;

    // Logic: If best is 0 (no record) OR current is faster than best
    if (best <= 0 || currentRunDuration < best) {
        Data.set("best_time", currentRunDuration.toString());
        return "New Record Saved";
    }

    return "No New Record";
}

function runPlaceholder() {
    // Minimum check: we need at least the 'type' (args[0])
    if (args.length < 1) return "false";

    var type = args[0];

    // Functions that DON'T need a path argument
    if (type === "compare_best") {
        return compareBestTime();
    }

    // Functions that DO need a path (args[1])
    if (args.length < 2) return "false";
    var path = args[1];

    if (type === "get") {
        return get(path);
    } else if (type === "set") {
        return set(path, args[2]);
    } else if (type === "add") {
        return add(path, args[2]);
    } else if (type === "set_time") {
        Data.set(path, Date.now().toString());
        return "set";
    } else if (type === "within_time") {
        if (!Data.exists(path)) return "false";
        var lastTime = parseInt(Data.get(path));
        var duration = parseInt(args[2]) * 1000;
        return (Date.now() - lastTime <= duration) ? "true" : "false";
    } else if (type === "get_duration") {
        return getDuration(args[1], args[2]);
    }

    return "false";
}

runPlaceholder();