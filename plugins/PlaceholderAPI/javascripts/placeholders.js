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
    if (!Data.exists(startPath) || !Data.exists(endPath)) return "0m";

    var start = parseInt(Data.get(startPath));
    var end = parseInt(Data.get(endPath));
    var diff = end - start;

    if (diff < 0) diff = 0;

    var hours = Math.floor(diff / (1000 * 60 * 60));
    var minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));

    if (hours > 0) {
        return hours + "h " + minutes + "m";
    } else {
        return minutes + "m";
    }
}

function runPlaceholder() {
    if (args.length < 2) return "false";

    var type = args[0];
    var path = args[1];

    if (type === "get") {
        return get(path);
    } else if (type === "set") {
        return set(path, args[2]);
    } else if (type === "add") {
        return add(path, args[2]);
    }
    else if (type === "set_time") {
        Data.set(path, Date.now().toString());
        return "set"; // Return a string to confirm execution
    }
    else if (type === "within_time") {
        if (!Data.exists(path)) return "false";
        var lastTime = parseInt(Data.get(path));
        var duration = parseInt(args[2]) * 1000;
        var now = Date.now();
        return (now - lastTime <= duration) ? "true" : "false";
    }
    else if (type === "get_duration") {
        return getDuration(args[1], args[2]);
    }

    return "false";
}

runPlaceholder();